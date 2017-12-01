package com.rover12421.gradle.wrapper.downloads.ui.controller

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.fxml.FXML
import javafx.scene.control.Label

class StatusBarController {
    @FXML
    private lateinit var status: Label

    private val statusProperty: StringProperty = SimpleStringProperty("init...")

    @FXML
    private fun initialize() {
        status.textProperty().bind(statusProperty)
    }

    fun updateStatus(info: String) {
        Platform.runLater {
            statusProperty.value = info
        }
    }
}