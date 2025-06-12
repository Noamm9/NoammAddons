package noammaddons.utils

import gg.essential.universal.UDesktop.browse
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.Utils.remove
import java.net.URI

object UpdateUtils {
    private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons/releases"
    private lateinit var updateVersion: DataClasses.Release
    private var isMessageOnScreen = false
    private var lastCheck: Long = 0L
    private var removeCharsRegex = Regex("[^0-9.]")
    private var startup = false


    fun update() {
        if ((System.currentTimeMillis() - lastCheck) < 25_000) return
        lastCheck = System.currentTimeMillis()

        if (isMessageOnScreen) return

        WebUtils.fetchJsonWithRetry<List<DataClasses.Release>>(GITHUB_API_URL) { releases ->
            if (releases.isEmpty()) return@fetchJsonWithRetry modMessage("&4Failed to get release version from GitHub")
            updateVersion = releases.firstOrNull { ! it.prerelease } ?: return@fetchJsonWithRetry

            val latestVersion = updateVersion.tag_name.remove(removeCharsRegex)

            if (MOD_VERSION == latestVersion) {
                if (! startup) {
                    Alert(message = "\n&a&lYour are running the latest release", duration = 3)
                    startup = true
                }
                return@fetchJsonWithRetry
            }

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
}
