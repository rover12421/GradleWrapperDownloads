package com.rover12421.gradle.wrapper.downloads.ui.controller

import com.rover12421.gradle.wrapper.downloads.MainApp
import com.rover12421.gradle.wrapper.downloads.controller.GradleWrapperDownload
import com.rover12421.gradle.wrapper.downloads.model.VersionInfo
import com.rover12421.gradle.wrapper.downloads.ui.model.ShowType
import com.rover12421.gradle.wrapper.downloads.ui.model.TopBarProperty
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.text.Text
import org.apache.commons.io.FileUtils
import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.concurrent.thread

class TopBarController {

    private lateinit var mainApp: MainApp

    @FXML
    private lateinit var allVersionBtn: Button

    @FXML
    private lateinit var releaseOnlyBtn: Button

    @FXML
    private lateinit var downloadBtn: Button

    @FXML
    private lateinit var gradleHomeText: Text

    private val controlProperty: TopBarProperty = TopBarProperty()

    /**
     * Is called by the main application to give a reference back to itself.

     * @param mainApp
     */
    fun setMainApp(mainApp: MainApp) {
        this.mainApp = mainApp
    }

    @FXML
    private fun initialize() {
        /**
         * 给Control绑定属性
         */
        allVersionBtn.disableProperty().bind(controlProperty.allVersionBtnDisable)
        downloadBtn.disableProperty().bind(controlProperty.downloadBtnDisable)
        releaseOnlyBtn.textProperty().bind(controlProperty.showType)

        gradleHomeText.text = GradleWrapperDownload.gradleUserHome()
    }

    fun disableAllVersionBtn() {
        controlProperty.allVersionBtnDisable.value = true
    }

    fun enableAllVersionBtn() {
        controlProperty.allVersionBtnDisable.value = false

    }
    fun disableDownloadBtn() {
        controlProperty.downloadBtnDisable.value = true
    }

    fun enableDownloadBtn() {
        controlProperty.downloadBtnDisable.value = false
    }

    fun disableAllBtn(disable: Boolean) {
        try {
            Platform.runLater {
                controlProperty.allVersionBtnDisable.set(disable)
                controlProperty.downloadBtnDisable.set(disable)
                releaseOnlyBtn.isDisable = disable
            }
        } catch (e : Throwable) {
            e.printStackTrace()
        }
    }

    fun allVersionBtnClick() {
        thread {
            try {
                println("Click ${allVersionBtn.text}")
                disableAllVersionBtn()
                mainApp.updateStatus("[Info] get remote all version info ...")
                val versions = GradleWrapperDownload.readAllVersionInfo()
                if (versions.isEmpty()) {
                    mainApp.updateStatus("[Error] get remote all version error !!!")
                } else {
                    mainApp.updateStatus("[OK] get remote all version size : ${versions.size}")
                    mainApp.resetVersions(versions, isShowAll())
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                mainApp.updateStatus("[Exception] ${e.message}")
            } finally {
                enableAllVersionBtn()
            }
        }
    }

    fun isShowAll(): Boolean {
        return controlProperty.showType.value == ShowType.ShowAll.btnText
    }

    fun releaseOnlyBtnClick() {
        println("Click ${releaseOnlyBtn.text}")
        if (isShowAll()) {
            controlProperty.showType.value = ShowType.ShowRelease.btnText
        } else {
            controlProperty.showType.value = ShowType.ShowAll.btnText
        }

        mainApp.refreshVerion(isShowAll())
    }

    fun link(dir: File, from: String, link: String): String {
        val result = ProcessExecutor()
                .readOutput(true)
                .directory(dir)
                .command("sh", "-c", "ln -s $from $link")
                .execute()
        return result.outputUTF8()
    }

    fun fixGradleWrapper(version: VersionInfo) {
        mainApp.updateStatus("fixGradleWrapper : $version")

        val fileNamePerfix = GradleWrapperDownload.getFileName(version)
        val zipFileName = "$fileNamePerfix.zip"
        val url = "${GradleWrapperDownload.gradleWrapperUriPrefix}$zipFileName"
        val hashDir = GradleWrapperDownload.distsPath.resolve("$fileNamePerfix/${GradleWrapperDownload.getHash(url)}")
        val zipFile = hashDir.resolve(zipFileName)

        if (!GradleWrapperDownload.getVersionInfoIsOk(version)) {
            unZipFile(zipFile)
            Files.deleteIfExists(zipFile)
            Files.createFile(Paths.get("$zipFile.ok"))
        }

        val binVersionInfo = VersionInfo(version.version, version.type, "bin")
        if (!GradleWrapperDownload.getVersionInfoIsOk(binVersionInfo)) {
            val binFileNamePerfix = GradleWrapperDownload.getFileName(binVersionInfo)
            val binZipFileName = "$binFileNamePerfix.zip"
            val binUrl = "${GradleWrapperDownload.gradleWrapperUriPrefix}$binZipFileName"
            val binHashDir = GradleWrapperDownload.distsPath.resolve("$binFileNamePerfix/${GradleWrapperDownload.getHash(binUrl)}")
            val binZipFile = binHashDir.resolve(binZipFileName)

            link(binHashDir.parent.parent.toFile(), hashDir.parent.fileName.toString(), binHashDir.parent.fileName.toString())
            link(binHashDir.parent.toFile(), hashDir.fileName.toString(), binHashDir.fileName.toString())
            Files.createFile(Paths.get("$binZipFile.ok"))
        }

        mainApp.updateStatus("fixGradleWrapper : $version finish!!!")
    }

    fun unZipFile(zipPath: Path) {
        val zipFile = ZipFile(zipPath.toFile())
        for (entry in zipFile.entries()) {
            val desPath = zipPath.resolveSibling(entry.name)
            mainApp.updateStatus("unzip : ${entry.name}")
            if (entry.isDirectory) {
                Files.createDirectories(desPath)
            } else {
                Files.createDirectories(desPath.parent)
                zipFile.getInputStream(entry).use { zis ->
                    Files.copy(zis, desPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    fun downloadBtnClick() {
        println("Click ${downloadBtn.text}")
        thread {
            val list = mainApp.getSelectVersions()

            disableAllBtn(true)
            mainApp.disableVersionTable(true)

            list.forEach { version ->
                mainApp.updateStatus("Download : $version")


                try {
                    if (!GradleWrapperDownload.getVersionInfoIsOk(version)) {
                        GradleWrapperDownload.download(version, mainApp)
                    }

                    fixGradleWrapper(version)
                    mainApp.updateStatus("Download $version is OK")
                } catch (e: Throwable) {
                    e.printStackTrace()
                    mainApp.updateStatus(e.localizedMessage)
                }

            }

            disableAllBtn(false)
            mainApp.disableVersionTable(false)

            mainApp.updateStatus("Download Select Finish!!!")
        }
    }
}