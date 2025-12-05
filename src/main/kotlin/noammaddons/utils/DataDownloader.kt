package noammaddons.utils

import com.google.gson.reflect.TypeToken
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.HttpURLConnection
import java.nio.file.*
import java.util.zip.ZipInputStream

object DataDownloader {
    private const val DOWNLOAD_URL = "https://github.com/Noamm9/NoammAddons/archive/refs/heads/data.zip"
    private const val RAW_URL = "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Noamm9/NoammAddons/commits/data"
    private val SHA_REGEX = Regex(""""sha"\s*:\s*"([^"]+)"""")
    private val modDataPath = File(File(System.getProperty("user.dir")), "config").resolve("@NAME@").resolve("data").toPath()
    private val LOGGER = LogManager.getLogger("@NAME@ - DataDownloader")

    fun downloadData() {
        val versionFile = modDataPath.resolve("version.txt")
        try {
            LOGGER.info("Checking for data update.")
            val remoteHash = getRemoteCommitHash() ?: return LOGGER.error("Could not get remote version hash. Aborting update check.")
            val localHash = getLocalCommitHash(versionFile)

            if (remoteHash != localHash || ! Files.exists(modDataPath)) {
                if (localHash == null) LOGGER.info("No local data found. Starting initial download.")
                else LOGGER.info("New version found (Remote: $remoteHash, Local: $localHash).")
                update(versionFile, remoteHash)
            }
            else LOGGER.info("Data is up-to-date (Version: $localHash).")
        }
        catch (e: Exception) {
            LOGGER.error("An unexpected error occurred during the update check.", e)
        }
    }

    private fun update(versionFile: Path, newHash: String) {
        try {
            val tempZipFile = Files.createTempFile("data-", ".zip")
            WebUtils.makeWebRequest(DOWNLOAD_URL).getInputStream().use { inputStream ->
                Files.copy(inputStream, tempZipFile, StandardCopyOption.REPLACE_EXISTING)
            }

            if (Files.exists(modDataPath)) Files.walk(modDataPath).sorted(Comparator.reverseOrder()).forEach(Files::delete)
            Files.createDirectories(modDataPath)

            unzip(tempZipFile)

            Files.write(versionFile, newHash.toByteArray())
            Files.delete(tempZipFile)
        }
        catch (e: IOException) {
            LOGGER.error("Failed to perform update.", e)
        }
    }

    private fun getRemoteCommitHash(): String? {
        return try {
            val connection = WebUtils.makeWebRequest(GITHUB_API_URL) as HttpURLConnection
            connection.setRequestProperty("User-Agent", "@NAME@ - @VER@")
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                SHA_REGEX.find(response)?.groups?.get(1)?.value
            }
            else null
        }
        catch (e: IOException) {
            LOGGER.error("Could not connect to GitHub API.", e)
            null
        }
    }

    private fun getLocalCommitHash(versionFile: Path) = runCatching {
        Files.readAllLines(versionFile).firstOrNull()?.trim()
    }.getOrNull()

    private fun unzip(zipFilePath: Path) {
        ZipInputStream(Files.newInputStream(zipFilePath)).use { zis ->
            var rootDir: String? = null
            generateSequence { zis.nextEntry }.forEach { entry ->
                if (rootDir == null) rootDir = entry.name.substringBefore('/') + "/"
                val entryName = entry.name.removePrefix(rootDir !!)

                if (entryName.isNotEmpty()) {
                    val newFilePath = modDataPath.resolve(entryName)
                    if (! newFilePath.toAbsolutePath().startsWith(modDataPath.toAbsolutePath())) {
                        throw IOException("Entry is outside of the target dir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(newFilePath)
                    }
                    else {
                        Files.createDirectories(newFilePath.parent)
                        Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    inline fun <reified T> loadJson(fileName: String): T {
        return getReader(fileName).use { reader ->
            JsonUtils.gsonBuilder.fromJson(reader, object: TypeToken<T>() {}.type)
        }
    }

    fun getReader(fileName: String): BufferedReader {
        modDataPath.resolve(fileName).takeIf(Files::exists)?.let {
            return Files.newBufferedReader(it)
        }

        LOGGER.info("Local file '$fileName' not found. Attempting to fetch from remote.")

        val connection = WebUtils.makeWebRequest(RAW_URL + fileName) as HttpURLConnection
        connection.setRequestProperty("Accept", "application/json")
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader()
    }
}