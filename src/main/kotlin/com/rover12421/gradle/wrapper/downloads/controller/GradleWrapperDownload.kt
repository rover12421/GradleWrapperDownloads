package com.rover12421.gradle.wrapper.downloads.controller

import com.google.gson.Gson
import com.rover12421.gradle.wrapper.downloads.MainApp
import com.rover12421.gradle.wrapper.downloads.model.VersionInfo
import com.rover12421.gradle.wrapper.downloads.model.DownloadConfig
import com.rover12421.gradle.wrapper.downloads.model.DownloadInfo
import com.rover12421.gradle.wrapper.downloads.runnable.DownloadRunnable
import com.rover12421.gradle.wrapper.downloads.runnable.SavaFileRunnable
import com.rover12421.gradle.wrapper.downloads.util.OkHttpUtils
import okhttp3.Response
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

object GradleWrapperDownload {
    val DEFAULT_GRADLE_USER_HOME: String = System.getProperty("user.home") + "/.gradle"
    val GRADLE_USER_HOME_PROPERTY_KEY: String = "gradle.user.home"
    val GRADLE_USER_HOME_ENV_KEY: String = "GRADLE_USER_HOME"

    val gradleWrapperUriPrefix = "https://services.gradle.org/distributions/"

    fun gradleUserHome(): String {
        var gradleUserHome: String? = System.getProperty(GRADLE_USER_HOME_PROPERTY_KEY)
        if (gradleUserHome != null) {
            return gradleUserHome
        }
        gradleUserHome = System.getenv(GRADLE_USER_HOME_ENV_KEY)
        if (gradleUserHome != null) {
            return gradleUserHome
        }
        return DEFAULT_GRADLE_USER_HOME
    }

    fun getHash(string: String): String {
        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val bytes = string.toByteArray()
            messageDigest.update(bytes)
            return BigInteger(1, messageDigest.digest()).toString(36)
        } catch (e: Exception) {
            throw RuntimeException("Could not hash input string.", e)
        }
    }

    val distsPath = Paths.get(gradleUserHome(), "wrapper/dists")

    val gson = Gson()

    fun readAllVersionInfo(): List<VersionInfo> {
        val versions = mutableListOf<VersionInfo>()
        val html = OkHttpUtils.readHtml("https://services.gradle.org/distributions/")
        if (html != null) {
//            println(html)

            val nameRegex = "<span class=\"name\">(.+?)</span>".toRegex()
            val versionInfoRegex = "gradle-(.+?)(-(.+?))?-(all|bin).zip".toRegex()

            nameRegex.findAll(html)
                    .map { it.groupValues[1] }
                    .filter {
                        it.endsWith("-all.zip")
//                        it.endsWith("-bin.zip") || it.endsWith("-all.zip")
                    }
                    .forEach {
//                        println("[Name] : $it")
                        versionInfoRegex.matchEntire(it)?.groupValues?.let {
                            val versionInfo = VersionInfo(
                                    version = it[1],
                                    type = it[3],
                                    buid = it[4]
                            )
                            versions.add(versionInfo)
//                            println(versionInfo)
                        }
                    }
//        } else {
//            println("read version error !!!")
        }
        return versions
    }

    fun getFileNameNoBuild(version: VersionInfo): String {
        var type = ""
        if (!version.type.isEmpty()) {
            type = "-${version.type}"
        }
        return "gradle-${version.version}$type"
    }

    fun getFileName(version: VersionInfo): String = "${getFileNameNoBuild(version)}-${version.buid}"

    /**
    gradle-4.3.1-src.zip 08-Nov-2017 09:11 +0000 22.35M
    gradle-4.3.1-src.zip.sha256 08-Nov-2017 09:11 +0000 64.00B
    gradle-4.3.1-bin.zip 08-Nov-2017 09:11 +0000 69.64M
    gradle-4.3.1-bin.zip.sha256 08-Nov-2017 09:11 +0000 64.00B
    gradle-4.3.1-all.zip 08-Nov-2017 09:11 +0000 91.83M
    gradle-4.3.1-all.zip.sha256 08-Nov-2017 09:11 +0000 64.00B
     */
    fun getDownloadZipUrl(version: VersionInfo) = "$gradleWrapperUriPrefix${getFileName(version)}.zip"
    fun getDownloadZipSha256Url(version: VersionInfo) = "${getDownloadZipUrl(version)}.sha256"

    /**
     * /Users/rover12421/.gradle/wrapper/dists/gradle-3.5-rc-2-all/c0k4lbl8pjtnoeqme5ekch4re
     */
    fun getLocalRootDir(version: VersionInfo): String {
        val hash = getHash(getDownloadZipUrl(version))
        val fileName = getFileName(version)
        return "$distsPath/$fileName/$hash/"
    }

    fun getVersionInfoIsOk(version: VersionInfo): Boolean {
        val localRootDir = getLocalRootDir(version)
        val unzipDir = "$localRootDir${getFileNameNoBuild(version)}"
        val okFile = "$localRootDir${getFileName(version)}.zip.ok"

        val okExists = Files.exists(Paths.get(okFile))
        val unzipExists = Files.exists(Paths.get(unzipDir))

        return okExists && unzipExists
    }

    fun readLoclVersionStatus(list: List<VersionInfo>) {
        /**
         * /Users/rover12421/.gradle/wrapper/dists/gradle-3.5-rc-2-all/c0k4lbl8pjtnoeqme5ekch4re/gradle-3.5-rc-2
         */
        list.forEach { version ->
            version.status = getVersionInfoIsOk(version).toString() + " - " + getVersionInfoIsOk(VersionInfo(version.version, version.type, "bin"))
        }
    }


    fun download(version: VersionInfo, mainApp: MainApp) {
        val fileNamePerfix = getFileName(version)
        val zipFileName = "$fileNamePerfix.zip"
        val url = "$gradleWrapperUriPrefix$zipFileName"

        val hashDir = distsPath.resolve("$fileNamePerfix/${getHash(url)}")

        println("hashDir : $hashDir")

        val downloadInfo = DownloadInfo(url, hashDir.toString(), zipFileName)

        if (Files.exists(hashDir.resolve(zipFileName))) {
            val sources = OkHttpUtils.getSources("$url.sha256").toString().toLowerCase()
            val hash256 = Files.newInputStream(Paths.get(zipFileName)).use {
                DigestUtils.sha256Hex(it).toLowerCase()
            }

             if (sources == hash256) {
                 Files.deleteIfExists(hashDir.resolve("$zipFileName.dl"))
                return
            }
        }

        downloadTask(downloadInfo, mainApp)
    }

    fun downloadTask(info: DownloadInfo, mainApp: MainApp) {
        val savePath = Paths.get(info.savaPath)
        if (Files.exists(savePath)) {
            Files.createDirectories(savePath)
        }

        val savaFile = savePath.resolve(info.fileName)

        val configFile = savePath.resolve(info.fileName+".dl")
        val okFile = savePath.resolve(info.fileName+".ok")

        if (info.reset) {
            println("reset download")
            Files.deleteIfExists(configFile)
            Files.deleteIfExists(okFile)
            Files.deleteIfExists(savaFile)
        }

        if (!Files.exists(savaFile) || !Files.exists(configFile)) {
            Files.deleteIfExists(configFile)
            Files.deleteIfExists(savaFile)
            info.reset = true
        }

        var congfig = DownloadConfig()
        if (Files.exists(configFile)) {
            try {
                congfig = gson.fromJson(Files.readAllBytes(configFile).toString(StandardCharsets.UTF_8), DownloadConfig::class.java)
                val response = OkHttpUtils.initRequest(info.url, congfig.lastModify)
                if (response.isSuccessful && !isNotServerFileChanged(response)) {
                    info.reset = true
                }
                prepareRangeFile(response, congfig)
            } catch (e: Throwable) {
                info.reset = true
            }
        } else {
            info.reset = true
            val response = OkHttpUtils.initRequest(info.url)
            prepareRangeFile(response, congfig)
        }

        if (info.reset) {
            println("Reset model!!!")
            congfig.blocks.clear()
            info.reset = false
            congfig.fileOff = 0
        }

        congfig.info = info
        congfig.resetFileOff()

        val savaFileRunnable = SavaFileRunnable(congfig, mainApp)

        Thread(savaFileRunnable).start()

        val dls = Array(info.threads) {
            index ->
            DownloadRunnable(congfig, savaFileRunnable)
        }

        dls.forEach { Thread(it).start() }

        while (!dls.all { it.finish }) {
            Thread.sleep(100)
        }
        savaFileRunnable.finish = true
    }

    fun prepareRangeFile(response: Response, config: DownloadConfig) {
        config.filesize = response.body()!!.contentLength()
        config.lastModify = getLastModify(response)
        response.close()
    }

    fun isSupportRange(response: Response): Boolean {
        val headers = response.headers()
        return headers.get("Content-Range").isNullOrBlank() || headers.get("Content-Length")?.toLong() != -1L
    }

    /**
     * 文件最后修改时间
     *
     * @param response
     * @return
     */
    fun getLastModify(response: Response): String {
        return response.headers().get("Last-Modified")!!
    }

    /**
     * 服务器文件是否已更改
     *
     * @param response
     * @return
     */
    fun isNotServerFileChanged(response: Response): Boolean {
        return response.code() == 206
    }
}