package com.rover12421.gradle.wrapper.downloads.ui.model

import com.rover12421.gradle.wrapper.downloads.model.VersionInfo
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class VersionInfoProperty(
        versionInfo: VersionInfo
) {
    val version: StringProperty = SimpleStringProperty(versionInfo.version)
    val type: StringProperty = SimpleStringProperty(versionInfo.type)
    val build: StringProperty = SimpleStringProperty(versionInfo.buid)
    val sha256: StringProperty = SimpleStringProperty(versionInfo.sha256)

    val status: StringProperty = SimpleStringProperty(versionInfo.status)
    val select: BooleanProperty = SimpleBooleanProperty(false)

    fun toVersionInfo() = VersionInfo(version.value, type.value, build.value, sha256.value, status.value)
}