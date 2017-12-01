package com.rover12421.gradle.wrapper.downloads.ui.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class TopBarProperty {
    val allVersionBtnDisable: BooleanProperty = SimpleBooleanProperty(false)
    val showType: StringProperty = SimpleStringProperty(ShowType.ShowAll.btnText)
    val downloadBtnDisable: BooleanProperty = SimpleBooleanProperty(true)
}