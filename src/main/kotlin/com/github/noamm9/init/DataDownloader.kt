package com.github.noamm9.init

import com.github.noamm9.utils.GsonUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.*
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.*

object DataDownloader {
    private const val DOWNLOAD_URL = "https://github.com/Noamm9/NoammAddons/archive/refs/heads/data.zip"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons/commits/data"
    private val SHA_REGEX = Regex(""""sha"\s*:\s*"([^"]+)"""")
    private const val MOD_NAME = "NoammAddons"
    private val LOGGER = LoggerFactory.getLogger("$MOD_NAME-DataDownloader")

    val modDataPath = File("config/$MOD_NAME/data").toPath().also {
        if (! it.exists()) it.createDirectories()
    }

    @JvmStatic
    fun downloadData() {
        val versionFile = modDataPath.resolve("version.txt")

        LOGGER.info("Checking for remote data updates...")

        val connection = URI.create(GITHUB_API_URL).toURL().openConnection() as HttpsURLConnection
        connection.setRequestProperty("User-Agent", "NoammAddons-DataDownloader")

        val apiResponse = connection.inputStream.bufferedReader().use { it.readText() }
        val remoteHash = SHA_REGEX.find(apiResponse)?.groups?.get(1)?.value ?: return LOGGER.error("Could not fetch remote version hash.")

        val localHash = if (versionFile.exists()) versionFile.readText().trim() else null

        if (remoteHash != localHash || ! modDataPath.exists()) {
            LOGGER.info("Update required. Remote: $remoteHash, Local: ${localHash ?: "None"}")
            update(versionFile, remoteHash)
        }
        else LOGGER.info("Data is up-to-date (Version: $localHash).")
    }

    private fun update(versionFile: Path, newHash: String) = runCatching {
        val tempZipFile = Files.createTempFile("data-download-", ".zip")
        val connection = URI.create(DOWNLOAD_URL).toURL().openConnection() as HttpsURLConnection
        connection.setRequestProperty("User-Agent", "NoammAddons-DataDownloader")

        connection.inputStream.use { input ->
            Files.copy(input, tempZipFile, StandardCopyOption.REPLACE_EXISTING)
        }

        if (modDataPath.exists()) modDataPath.toFile().deleteRecursively()
        modDataPath.createDirectories()

        unzip(tempZipFile)

        versionFile.writeText(newHash)
        tempZipFile.deleteIfExists()

        LOGGER.info("Data update successful.")
    }.onFailure {
        LOGGER.error("Error while updating $MOD_NAME-data", it)
    }

    private fun unzip(zipFilePath: Path) = ZipInputStream(zipFilePath.inputStream()).use { zis ->
        var rootDirName: String? = null

        while (true) {
            val entry = zis.nextEntry ?: break

            if (rootDirName == null) rootDirName = entry.name.substringBefore('/') + "/"

            val entryName = entry.name.removePrefix(rootDirName).ifEmpty { continue }
            val targetPath = modDataPath.resolve(entryName)

            if (entry.isDirectory) targetPath.createDirectories()
            else {
                targetPath.parent?.createDirectories()
                Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }

            zis.closeEntry()
        }
    }

    inline fun <reified T: Any> loadJson(fileName: String) = GsonUtils.decode<T>(modDataPath.resolve(fileName).readText())
}