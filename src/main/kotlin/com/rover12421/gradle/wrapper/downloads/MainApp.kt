package com.rover12421.gradle.wrapper.downloads

import com.rover12421.gradle.wrapper.downloads.controller.GradleWrapperDownload
import com.rover12421.gradle.wrapper.downloads.model.VersionInfo
import com.rover12421.gradle.wrapper.downloads.ui.controller.StatusBarController
import com.rover12421.gradle.wrapper.downloads.ui.controller.TopBarController
import com.rover12421.gradle.wrapper.downloads.ui.controller.VersionListController
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.io.IOException


class MainApp : Application() {
    lateinit var primaryStage: Stage
    private lateinit var rootLayout: BorderPane
    lateinit var versionListController: VersionListController
    lateinit var statusBarController: StatusBarController
    lateinit var topBarController: TopBarController

    val versions = mutableListOf<VersionInfo>()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        this.primaryStage.title = "Gradle Wrapper Downloads"

        primaryStage.icons.add(Image("file:resources/images/gradle512.png"))

        initRootLayout()

        loadViews()
    }

    fun exit() {
        Platform.exit()
    }

    fun initRootLayout() {
        try {
            val loader = FXMLLoader()
            loader.location = this.javaClass.getResource("/view/RootLayout.fxml")
            rootLayout = loader.load()

            val scene = Scene(rootLayout)
            primaryStage.scene = scene
            primaryStage.show()

            primaryStage.onCloseRequest = EventHandler {
                println("Close App!")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadViews() {
        try {
            val centerLoader = FXMLLoader()
            centerLoader.location = this.javaClass.getResource("/view/VersionList.fxml")
            val versionListView: AnchorPane = centerLoader.load()
            rootLayout.center = versionListView
            versionListController = centerLoader.getController<VersionListController>()
            versionListController.setMainApp(this)

            val topLoader = FXMLLoader()
            topLoader.location = this.javaClass.getResource("/view/TopBar.fxml")
            val topBarView: AnchorPane = topLoader.load()
            rootLayout.top = topBarView
            topBarController = topLoader.getController<TopBarController>()
            topBarController.setMainApp(this)

            val statusLoader = FXMLLoader()
            statusLoader.location = this.javaClass.getResource("/view/StatusBar.fxml")
            val statusBar: AnchorPane = statusLoader.load()
            rootLayout.bottom = statusBar

            statusBarController = statusLoader.getController<StatusBarController>()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun updateStatus(info: String) {
        statusBarController.updateStatus(info)
    }

    fun resetVersions(list : List<VersionInfo>, showAll: Boolean = false) {
        versions.addAll(list)
        refreshVerion(showAll)

        updateStatus("Refresh local all version status ...")

        GradleWrapperDownload.readLoclVersionStatus(list)
        updateStatus("Refresh local all version status finish!")

        refreshVerion(showAll)
    }

    fun refreshVerion(showAll: Boolean) {
        if (showAll) {
            versionListController.setVersionList(versions)
        } else {
            versionListController.setVersionList(versions.filter { it.type.isBlank() })
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }
    }

    fun getSelectVersions() = versionListController.getSelectVersions()

    fun enableDownloadBtn(enable: Boolean) {
        if (enable) {
            topBarController.enableDownloadBtn()
        } else {
            topBarController.disableDownloadBtn()
        }
    }

    fun disableVersionTable(disable: Boolean) {
        versionListController.disableVersionTable(disable)
    }
}