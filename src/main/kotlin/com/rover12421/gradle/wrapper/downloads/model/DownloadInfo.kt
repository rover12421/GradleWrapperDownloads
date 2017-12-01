package com.rover12421.gradle.wrapper.downloads.model

data class DownloadInfo(
        var url: String = "",
        var savaPath: String = "",
        var fileName: String = "",
        var threads: Int = Runtime.getRuntime().availableProcessors(),
        var reset:Boolean = false
)