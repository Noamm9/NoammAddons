package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.network.WebUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import kotlin.io.path.*

object DataDownloader {
    private const val DOWNLOAD_URL = "https://github.com/Noamm9/NoammAddons-1.21.10/archive/refs/heads/data.zip"
    private const val RAW_URL = "https://raw.githubusercontent.com/Noamm9/NoammAddons-1.21.10/refs/heads/data/"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons-1.21.10/commits/data"
    private val SHA_REGEX = Regex(""""sha"\s*:\s*"([^"]+)"""")
    val LOGGER = LoggerFactory.getLogger("${NoammAddons.MOD_NAME} - DataDownloader")

    private val modDataPath = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("data").also {
        if (! it.exists()) it.createDirectories()
    }

    fun downloadData() = runBlocking {
        withContext(WebUtils.networkDispatcher) {
            val versionFile = modDataPath.resolve("version.txt")

            try {
                LOGGER.info("Checking for remote data updates...")
                val remoteHash = SHA_REGEX.find(WebUtils.getString(GITHUB_API_URL).getOrThrow())?.groups?.get(1)?.value
                    ?: return@withContext LOGGER.error("Could not fetch remote version hash.")
                val localHash = if (versionFile.exists()) versionFile.readText().trim() else null

                if (remoteHash != localHash || ! modDataPath.exists()) {
                    LOGGER.info("Update required. Remote: $remoteHash, Local: ${localHash ?: "None"}")
                    update(versionFile, remoteHash)
                }
                else LOGGER.info("Data is up-to-date (Version: $localHash).")

            }
            catch (e: Exception) {
                LOGGER.error("Failed to check for data updates", e)
            }
        }
    }

    private fun update(versionFile: Path, newHash: String) {
        try {
            val tempZipFile = Files.createTempFile("data-download-", ".zip")

            URI.create(DOWNLOAD_URL).toURL().openStream().use { input ->
                Files.copy(input, tempZipFile, StandardCopyOption.REPLACE_EXISTING)
            }

            if (modDataPath.exists()) modDataPath.toFile().deleteRecursively()
            modDataPath.createDirectories()

            unzip(tempZipFile)

            versionFile.writeText(newHash)
            tempZipFile.deleteIfExists()

            LOGGER.info("Data update successful.")
        }
        catch (e: IOException) {
            LOGGER.error("Failed to update data files", e)
        }
    }

    private fun unzip(zipFilePath: Path) {
        ZipInputStream(zipFilePath.inputStream()).use { zis ->
            var rootDirName: String? = null

            while (true) {
                val entry = zis.nextEntry ?: break

                if (rootDirName == null) rootDirName = entry.name.substringBefore('/') + "/"

                val entryName = entry.name.removePrefix(rootDirName)
                if (entryName.isEmpty()) continue

                val targetPath = modDataPath.resolve(entryName)

                if (! targetPath.normalize().startsWith(modDataPath.normalize())) {
                    throw IOException("Zip entry attempted to write outside target: ${entry.name}")
                }

                if (entry.isDirectory) targetPath.createDirectories()
                else {
                    targetPath.parent?.createDirectories()
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
                zis.closeEntry()
            }
        }
    }

    inline fun <reified T> loadJson(fileName: String): T {
        return getReader(fileName).use { reader ->
            JsonUtils.gsonBuilder.fromJson(reader, object: TypeToken<T>() {}.type)
        }
    }

    fun getReader(fileName: String): BufferedReader {
        val localFile = modDataPath.resolve(fileName)
        return if (localFile.exists()) localFile.bufferedReader()
        else {
            LOGGER.warn("Local file '$fileName' missing. Fetching from RAW URL.")
            val connection = WebUtils.prepareConnection(RAW_URL + fileName)
            connection.setRequestProperty("Accept", "application/json")
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader()
        }
    }
}