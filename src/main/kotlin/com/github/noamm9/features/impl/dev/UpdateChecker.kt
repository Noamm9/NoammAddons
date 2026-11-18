package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.isCheat
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.compat.openUri
import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.network.WebUtils
import io.ktor.client.plugins.timeout
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import net.fabricmc.loader.api.FabricLoader
import java.net.URI
import java.nio.file.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.*
import kotlin.io.path.*
import kotlin.time.Instant

object UpdateChecker: Feature(
    "Checks GitHub for new releases or Action builds of the mod.",
    toggled = true
) {
    val source by DropdownSetting("Update Source", 0, listOf("Releases", "Action Builds")).withDescription("Releases checks the latest published GitHub release. Action Builds checks the latest successful CI build.")
    val autoUpdate by ToggleSetting("Auto Update").withDescription("Downloads new releases or Action builds in the background and installs them when you close the game.")
    val check by ButtonSetting("Check For Updates") { ThreadUtils.async { runCheck(true) } }.withDescription("Manually checks using the source selected above.")
    val openPage by ButtonSetting("Open Latest Build/Release") { openPage() }.withDescription("Opens the latest release or Actions run page in your browser.")

    private const val RELEASE_URL = "https://api.noamm.org/na/data/release"
    private const val ACTION_URL = "https://api.noamm.org/na/data/action"
    private const val ARTIFACT_URL = "$ACTION_URL/artifact"
    private val title = "$MOD_NAME $name"
    private var page: String? = null
    @Volatile private var pendingUpdate: PendingUpdate? = null

    override fun init() {
        register<GameStartEvent> {
            ThreadUtils.setTimeout(5000) {
                ThreadUtils.loop(TimeUnit.HOURS.toMillis(1), block = ::runCheck)
            }
        }

        ThreadUtils.addShutdownHook(::installUpdate)
    }

    suspend fun runCheck(manual: Boolean = false) = runCatching {
        logger.info("Checking for updates...")
        if (source.value == 0) checkReleases(manual) else checkActionBuilds(manual)

    }.onFailure {
        logger.error("UpdateChecker: failed to check for updates", it)
        if (manual) NotificationManager.push(title, "Failed to check for updates, see logs.")
    }

    private suspend fun checkReleases(manual: Boolean) {
        val release = WebUtils.getAs<Release>(RELEASE_URL).getOrThrow()
        val releaseMillis = Instant.parse(release.created_at).toEpochMilliseconds()
        val remoteVersion = release.tag_name
        page = release.html_url

        when {
            (BuildInfo.isCiBuild && releaseMillis > BuildInfo.builtAt) || isNewerVersion(remoteVersion, MOD_VERSION) -> {
                if (autoUpdate.value && downloadUpdate(remoteVersion, release.assets)) return
                val suffix = if (BuildInfo.isCiBuild) " (old action build)" else ""
                NotificationManager.push(
                    title,
                    "NoammAddons $remoteVersion is out, you're on $MOD_VERSION.$suffix",
                    6000L
                )
            }

            manual -> NotificationManager.push(title, "You're already up to date ($MOD_VERSION).")
        }
    }

    private suspend fun checkActionBuilds(manual: Boolean) {
        val latest = WebUtils.getAs<Action>(ACTION_URL).getOrThrow().workflow_runs.firstOrNull() ?: return
        val latestMillis = Instant.parse(latest.created_at).toEpochMilliseconds()
        page = latest.html_url

        if (latestMillis <= BuildInfo.builtAt) {
            if (manual) NotificationManager.push(title, "You're already running the latest Action build.")
            return
        }

        val sha = latest.head_sha.take(7)
        val assets = latest.artifacts.map { Asset(it.name, "$ARTIFACT_URL/${it.id}", it.digest) }
        if (autoUpdate.value && downloadUpdate(sha, assets)) return

        NotificationManager.push(title, "A new Action build ($sha).", 6000L)
    }

    private suspend fun downloadUpdate(version: String, assets: List<Asset>): Boolean {
        if (FabricLoader.getInstance().isDevelopmentEnvironment) return false

        return runCatching {
            if (pendingUpdate?.version == version) return@runCatching
            val jar = BuildInfo.currentJar ?: error("Not running from a jar. is this dev env?")

            val assetSuffix = "-@MINECRAFT_VERSION@-${if (isCheat) "cheat" else "legit"}.jar"
            val asset = assets.find { it.name.endsWith(assetSuffix) } ?: error("No *$assetSuffix asset for $version")

            val response = WebUtils.get(asset.browser_download_url) { timeout { requestTimeoutMillis = 300_000 } }.getOrThrow()
            if (! response.status.isSuccess()) error("HTTP ${response.status.value}-${response.status.description} downloading ${asset.name}")
            val bytes = response.bodyAsBytes()

            asset.digest?.removePrefix("sha256:")?.let { expected ->
                val actual = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
                if (! actual.equals(expected, true)) error("Checksum mismatch for ${asset.name}: expected $expected, got $actual")
            }

            val download = Files.createTempFile("$MOD_ID-update-", ".jar").apply { writeBytes(bytes) }
            pendingUpdate?.download?.deleteIfExists()
            pendingUpdate = PendingUpdate(version, download, jar, jar.resolveSibling(asset.name))
        }.onFailure {
            logger.error("UpdateChecker: failed to download $version", it)
        }.onSuccess {
            NotificationManager.push(title, "Downloaded NoammAddons $version, it will be installed when you close your game.", 6000L)
        }.isSuccess
    }

    fun installUpdate() {
        val update = pendingUpdate ?: return
        logger.info("$MOD_NAME-UpdateChecker: installing ${update.version} to ${update.target}")

        // windows keeps the running jar locked until the jvm dies so a process finishes the swap
        if (System.getProperty("os.name").startsWith("Windows")) {
            fun quote(path: Path) = "'${path.absolutePathString().replace("'", "''")}'"

            val script = buildList {
                add("Wait-Process -Id ${ProcessHandle.current().pid()} -ErrorAction SilentlyContinue")
                add("for (${'$'}i = 0; ${'$'}i -lt 40 -and (Test-Path -LiteralPath ${quote(update.current)}); ${'$'}i++) { Remove-Item -LiteralPath ${quote(update.current)} -Force -ErrorAction SilentlyContinue")
                add("Start-Sleep -Milliseconds 250 }")
                add("if (-not (Test-Path -LiteralPath ${quote(update.current)})) { Move-Item -LiteralPath ${quote(update.download)} -Destination ${quote(update.target)} -Force }")
            }

            ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script.joinToString("; ")).start()
            return
        }

        Files.move(update.download, update.target, StandardCopyOption.REPLACE_EXISTING)
        if (update.current != update.target) update.current.deleteIfExists()
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").map(String::toInt)
        val l = local.split(".").map(String::toInt)

        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrNull(i) ?: 0
            val lv = l.getOrNull(i) ?: 0
            if (rv != lv) return rv > lv
        }

        return false
    }

    private fun openPage() {
        val repo = "Noamm9/NoammAddons"
        val releases = "https://github.com/$repo/releases/latest"
        val actions = "https://github.com/$repo/actions"
        openUri(URI(page ?: (if (source.value == 0) releases else actions)))
    }

    @Serializable private data class Release(val tag_name: String, val html_url: String, val created_at: String, val assets: List<Asset> = emptyList())
    @Serializable private data class Asset(val name: String, val browser_download_url: String, val digest: String? = null)
    @Serializable private data class Action(val workflow_runs: List<Run>)
    @Serializable private data class Run(val head_sha: String, val html_url: String, val created_at: String, val artifacts: List<Artifact> = emptyList())
    @Serializable private data class Artifact(val id: Long, val name: String, val digest: String? = null)
    private data class PendingUpdate(val version: String, val download: Path, val current: Path, val target: Path)

    object BuildInfo {
        val currentJar: Path?
            get() = FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null)
                ?.origin?.paths?.singleOrNull()?.takeIf { it.isRegularFile() && it.extension == "jar" }

        val isCiBuild: Boolean
        val builtAt: Long

        init {
            val props = Properties()
            BuildInfo::class.java.classLoader.getResourceAsStream("build-info.properties")?.use(props::load)
            isCiBuild = props.getProperty("ci")?.toBoolean() ?: false
            builtAt = if (isCiBuild) props.getProperty("built_at")?.toLongOrNull() ?: 0L else 0L
        }
    }
}