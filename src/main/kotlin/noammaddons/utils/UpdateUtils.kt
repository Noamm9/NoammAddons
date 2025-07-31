package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.essential.universal.UDesktop.browse
import kotlinx.coroutines.*
import noammaddons.NoammAddons.Companion.FULL_PREFIX
import noammaddons.NoammAddons.Companion.MOD_VERSION
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.Utils.remove
import java.net.HttpURLConnection
import java.net.URI

object UpdateUtils {
    private data class Release(val tag_name: String, val html_url: String, val prerelease: Boolean)

    private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons/releases"
    private lateinit var updateVersion: Release
    private var isMessageOnScreen = false
    private var lastCheck: Long = 0L
    private var removeCharsRegex = Regex("[^0-9.]")
    private var startup = false

    fun update() {
        if (System.currentTimeMillis() - lastCheck < 25_000 || isMessageOnScreen) return
        lastCheck = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val connection = WebUtils.makeWebRequest(GITHUB_API_URL) as HttpURLConnection
            val response = connection.inputStream.bufferedReader().readText()
            val type = object: TypeToken<List<Release>>() {}.type
            val releases: List<Release> = Gson().fromJson(response, type)

            if (releases.isEmpty()) return@launch modMessage("No releases found.")
            val latestRelease = releases.firstOrNull { ! it.prerelease } ?: return@launch
            val latestVersion = latestRelease.tag_name.remove(removeCharsRegex)
            if (MOD_VERSION == latestVersion) {
                if (! startup) {
                    Alert(message = "\n&a&lYou are running the latest release", duration = 3)
                    startup = true
                }
                return@launch
            }

            if (! isNewerVersion(latestVersion)) return@launch
            updateVersion = latestRelease
            isMessageOnScreen = true

            clickableChat(
                "&bNew version available: &d$FULL_PREFIX ($latestVersion)",
                "/na openlink ${updateVersion.html_url}",
                "&bNew version available, Click to Open the download link of the latest version"
            )

            Alert(
                message = "\n&a&lNew version available, \n&a&lClick to Open the download link of the latest version",
                closeFunction = {
                    browse(URI(updateVersion.html_url))
                    isMessageOnScreen = false
                },
                duration = - 1
            )
        }

    }

    // function from https://github.com/kiwidotzip/zen/blob/master/src/main/kotlin/meowing/zen/UpdateChecker.kt#L65
    private fun isNewerVersion(latest: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = MOD_VERSION.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }

            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        return false
    }
}
