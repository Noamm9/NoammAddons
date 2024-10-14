package noammaddons.utils

import gg.essential.universal.UDesktop
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.ThreadUtils.runEvery
import java.net.URI

object UpdateUtils {
	private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons/releases"
	private const val INFINITE_DURATION = Int.MAX_VALUE
	private lateinit var updateVersion: Release
	private var isMessageOnScreen = false
	private var lastTrigger: Long = 0L
	private var removeCharsRegex = Regex("[^0-9.]")
	
	
	data class Release(
		val html_url: String,
		val id: Int,
		val tag_name: String,
		val name: String?,
		val body: String?,
		val draft: Boolean,
		val prerelease: Boolean,
		val created_at: String,
		val published_at: String,
		val author: Author,
		val assets: List<Asset>
	)
	
	data class Author(val login: String, val id: Int, val avatar_url: String)
	
	data class Asset(
		val id: Int,
		val name: String,
		val label: String?,
		val content_type: String,
		val size: Int,
		val download_count: Int,
		val browser_download_url: String
	)
	
	
	init {
		runEvery(600_000) { update() }
	}
	
	fun update() {
		if ((System.currentTimeMillis() - lastTrigger) < 25_000) return
		lastTrigger = System.currentTimeMillis()
		
		if (isMessageOnScreen) return
		
		fetchJsonWithRetry<List<Release>>(GITHUB_API_URL) { releases ->
			if (releases.isNullOrEmpty()) {
				modMessage("&4Failed to get release version from GitHub")
				return@fetchJsonWithRetry
			}
			
			updateVersion = releases.firstOrNull { !it.prerelease } ?: return@fetchJsonWithRetry
			
			val latestVersion = updateVersion.tag_name.replace(removeCharsRegex, "")
			
			if (MOD_VERSION == latestVersion)  {
				Alert(message = "\n&a&lYour are running the latest release", duration = 3)
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
					UDesktop.browse(URI(updateVersion.html_url))
					isMessageOnScreen = false
				},
				duration = INFINITE_DURATION
			)
		}
	}
}
