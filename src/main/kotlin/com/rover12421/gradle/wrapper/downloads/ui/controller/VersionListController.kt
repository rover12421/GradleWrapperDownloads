package com.rover12421.gradle.wrapper.downloads.ui.controller

import com.rover12421.gradle.wrapper.downloads.MainApp
import com.rover12421.gradle.wrapper.downloads.controller.GradleWrapperDownload
import com.rover12421.gradle.wrapper.downloads.model.VersionInfo
import com.rover12421.gradle.wrapper.downloads.ui.model.VersionInfoProperty
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.util.Callback

/**
 * Created by rover12421 on 4/14/17.
 */
class VersionListController {
    @FXML
    private lateinit var versionInfoTable: TableView<VersionInfoProperty>
    @FXML
    private lateinit var versionColumn: TableColumn<VersionInfoProperty, String>
    @FXML
    private lateinit var typeColumn: TableColumn<VersionInfoProperty, String>
//    @FXML
//    private lateinit var buildColumn: TableColumn<VersionInfoProperty, String>
//    @FXML
//    private lateinit var sha256Column: TableColumn<VersionInfoProperty, String>
    @FXML
    private lateinit var statusColumn: TableColumn<VersionInfoProperty, String>
    @FXML
    private lateinit var selectColumn: TableColumn<VersionInfoProperty, Boolean>

    // Reference to the main application.
    private lateinit var mainApp: MainApp

    private val versionList = FXCollections.observableArrayList<VersionInfoProperty>()

//    private var selectVersionInfo: VersionInfo? = null

    fun disableVersionTable(disable: Boolean) {
        versionInfoTable.isDisable = disable

        versionInfoTable.items.forEach { versionInfo ->
            val version = versionInfo.toVersionInfo()
            versionInfo.status.value = GradleWrapperDownload.getVersionInfoIsOk(version).toString() + " - " + GradleWrapperDownload.getVersionInfoIsOk(VersionInfo(version.version, version.type, "bin"))
        }
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private fun initialize() {
        selectColumn.setCellValueFactory { it.value.select }
        versionColumn.setCellValueFactory { it.value.version }
        typeColumn.setCellValueFactory { it.value.type }
//        buildColumn.setCellValueFactory { it.value.build }
//        sha256Column.setCellValueFactory { it.value.sha256 }
        statusColumn.setCellValueFactory { it.value.status }

        versionInfoTable.rowFactory = Callback<TableView<VersionInfoProperty>, TableRow<VersionInfoProperty>> {
            val tableRow = TableRow<VersionInfoProperty>()
            tableRow.itemProperty().addListener { observable, oldValue, newValue ->
                newValue?.select?.addListener { observable, oldValue, newValue ->
                    tableRowChangedSelectProperty(tableRow, newValue)
                }

                if (newValue != null) {
                    if (newValue.select.value) {
                        tableRowChangedSelectProperty(tableRow, newValue.select.value)
                    }
                } else {
                    tableRow.styleProperty().set("")
                }
                //强制刷新,可以修复背景颜色错误
                refresh()
            }
            tableRow
        }

        versionInfoTable.setOnMouseClicked {
            val selectedCells = versionInfoTable.selectionModel.selectedCells
            if (selectedCells.size <= 0) {
                return@setOnMouseClicked
            }
            val pos = selectedCells[0]
            val row = pos.row
            val col = pos.column
            val cellData = versionInfoTable.items[row]

            when(col) {
                0 -> {

//                    if (!cellData.select.value) {
//                        //之前没有选择，先把全部选择去掉
//                        versionInfoTable.items.forEach {
//                            it.select.value = false
//                        }
//                    }
                    cellData.select.set(!cellData.select.value)
//                    selectVersionInfo = cellData.toVersionInfo()
//                    mainApp.enableDownloadBtn(cellData.select.value)

                    refresh()
                }
            }
        }

        versionInfoTable.items = versionList
        versionList.addListener(ListChangeListener {
            refresh()
        })
    }

    fun tableRowChangedSelectProperty(tableRow: TableRow<VersionInfoProperty>, select: Boolean) {
        if (select) {
            tableRow.styleProperty().set("-fx-background-color: lightslategray")
        } else {
            tableRow.styleProperty().set("")
        }
    }

    /**
     * Is called by the main application to give a reference back to itself.
     * @param mainApp
     */
    fun setMainApp(mainApp: MainApp) {
        this.mainApp = mainApp
    }

    fun refresh() {
        if (versionInfoTable.items.find { it.select.value == true } != null) {
            Platform.runLater{
                mainApp.enableDownloadBtn(true)
            }
        }
        versionInfoTable.refresh()
    }

    fun setVersionList(list: List<VersionInfo>) {
        versionList.clear()
        list.forEach { version->
            versionList.add(VersionInfoProperty(version))
        }
    }

    fun getSelectVersions(): List<VersionInfo> {
        return versionInfoTable.items.filter { it.select.value == true }.map { it.toVersionInfo() }
    }
}