package com.rover12421.gradle.wrapper.downloads.ui.view

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.util.Callback

/**
 * Created by rover12421 on 4/14/17.
 */
class CheckBoxTableCellFactory<S,T> : Callback<TableColumn<S, T>, TableCell<S, T>> {
    override fun call(param: TableColumn<S, T>?): CheckBoxTableCell<S, T> = CheckBoxTableCell()
}