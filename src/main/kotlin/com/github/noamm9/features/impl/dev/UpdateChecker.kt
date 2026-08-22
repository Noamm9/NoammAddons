package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.network.WebUtils
import gg.essential.universal.UDesktop
import kotlinx.serialization.Serializable
import java.net.URI
import java.util.*
import java.util.concurrent.*
import kotlin.time.Instant

object UpdateChecker: Feature(
    "Checks GitHub for new releases or Action builds of the mod.\n&c&lDOES NOT AUTO UPDATE",
    toggled = true,
) {
    val checkOnStartup by ToggleSetting("Check On Startup", true).withDescription("Automatically checks for updates a few seconds after the game starts.")
    val source by DropdownSetting("Update Source", 0, listOf("Releases", "Action Builds")).withDescription("Releases checks the latest published GitHub release. Action Builds checks the latest successful CI build.")
    val check by ButtonSetting("Check For Updates") { ThreadUtils.async { runCheck(true) } }.withDescription("Manually checks using the source selected above.")
    val openPage by ButtonSetting("Open Latest Build/Release") { openPage() }.withDescription("Opens the latest release or Actions run page in your browser.")

    private const val RELEASE_URL = "https://api.noamm.org/na/data/release"
    private const val ACTION_URL = "https://api.noamm.org/na/data/action"
    private val title = "$MOD_NAME $name"
    private var page: String? = null

    override fun init() {
        register<GameStartEvent> {
            if (enabled && checkOnStartup.value) ThreadUtils.setTimeout(5000) { runCheck() }
        }

        var firstRun = true
        ThreadUtils.loop(TimeUnit.HOURS.toMillis(1)) {
            if (firstRun) {
                firstRun = false
                return@loop
            }

            runCheck()
        }
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

        if (latestMillis > BuildInfo.builtAt) NotificationManager.push(title, "A new Action build (${latest.head_sha.take(7)}).", 6000L)
        else if (manual) NotificationManager.push(title, "You're already running the latest Action build.")
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
        UDesktop.browse(URI(page ?: (if (source.value == 0) releases else actions)))
    }

    @Serializable private data class Release(val tag_name: String, val html_url: String, val created_at: String)
    @Serializable private data class Action(val workflow_runs: List<Run>)
    @Serializable private data class Run(val head_sha: String, val html_url: String, val created_at: String)

    object BuildInfo {
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