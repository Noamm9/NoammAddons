package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.utils.GsonUtils.decode
import com.github.noamm9.utils.GsonUtils.encode
import com.github.noamm9.utils.ThreadUtils.setTimeout
import com.github.noamm9.utils.network.NoammAPI.BASE_URL
import com.github.noamm9.utils.network.WebUtils.client
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.Signature
import java.util.*
import kotlin.io.encoding.Base64

object ApiAuth {
    private const val AUTH_URL = "$BASE_URL/hypixel/auth"
    @Volatile private var tokenInfo: TokenResponse? = null
    val token get() = tokenInfo?.token

    fun init() {
        register<GameStartEvent> {
            scope.launch { updateToken() }
        }
    }

    private suspend fun updateToken() {
        try {
            val playerKeypairOpt = mc.profileKeyPairManager.prepareKeyPair().await()

            if (! playerKeypairOpt.isPresent) {
                logger.warn("[ApiAuth] No profile key pair available, retrying in 5 minutes")
                setTimeout(5 * 60 * 1000L) { updateToken() }
                return
            }

            val playerKeyPair = playerKeypairOpt.get()
            val publicKeyData = playerKeyPair.publicKey().data()
            if (publicKeyData.hasExpired()) {
                logger.warn("[ApiAuth] Profile key pair has expired, please restart the game")
                setTimeout(5 * 60 * 1000L) { updateToken() }
                return
            }

            val publicKey = Base64.encode(publicKeyData.key().encoded)
            val publicKeySignature = publicKeyData.keySignature()
            val expiresAt = publicKeyData.expiresAt().toEpochMilli()
            val uuid = mc.user.profileId

            val signedData = signRandomData(playerKeyPair.privateKey()) ?: run {
                logger.error("[ApiAuth] Failed to sign random data, retrying in 5 minutes")
                setTimeout(5 * 60 * 1000L) { updateToken() }
                return
            }

            val request = TokenRequest(
                keyPair = KeyPairInfo(
                    uuid = uuid.toString(),
                    publicKey = publicKey,
                    publicKeySignature = Base64.encode(publicKeySignature),
                    expiresAt = expiresAt
                ),
                signedData = signedData,
                mod = MOD_ID,
                minecraftVersion = "26.1.2",
                modVersion = MOD_VERSION
            )

            val response = client.post(AUTH_URL) {
                contentType(ContentType.Application.Json)
                setBody(encode(request))
            }

            if (! response.status.isSuccess()) {
                val error = response.bodyAsText()
                logger.error("[ApiAuth] Auth failed (${response.status}): $error, retrying in 15 minutes")
                setTimeout(15 * 60 * 1000L) { updateToken() }
                return
            }

            val tokenResponse = decode<TokenResponse>(response.bodyAsText())
            tokenInfo = tokenResponse

            val refreshAtMillis = (tokenResponse.expiresAt - tokenResponse.issuedAt) - 5 * 60 * 1000
            logger.info("[ApiAuth] Successfully authenticated, refreshing in ${refreshAtMillis / 1000}s")
            setTimeout(refreshAtMillis) { updateToken() }
        }
        catch (e: Exception) {
            logger.error("[ApiAuth] Unexpected error during auth, retrying in 15 minutes", e)
            setTimeout(15 * 60 * 1000L) { updateToken() }
        }
    }

    private fun signRandomData(privateKey: PrivateKey): SignedData? {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            val uuid = UUID.randomUUID()
            val buf = ByteBuffer.allocate(16)
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)

            sig.initSign(privateKey)
            sig.update(buf.array())
            val signed = sig.sign()

            SignedData(
                original = Base64.encode(buf.array()),
                signed = Base64.encode(signed)
            )
        }
        catch (e: Exception) {
            logger.error("[ApiAuth] Failed to sign random data", e)
            null
        }
    }

    private data class TokenRequest(val keyPair: KeyPairInfo, val signedData: SignedData, val mod: String, val minecraftVersion: String, val modVersion: String)
    private data class KeyPairInfo(val uuid: String, val publicKey: String, val publicKeySignature: String, val expiresAt: Long)
    private data class SignedData(val original: String, val signed: String)
    private data class TokenResponse(val token: String, val issuedAt: Long, val expiresAt: Long)
}
