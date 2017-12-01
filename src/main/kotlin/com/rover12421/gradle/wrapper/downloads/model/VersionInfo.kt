package com.rover12421.gradle.wrapper.downloads.model

data class VersionInfo(
        var version: String = "",
        var type: String = "",  // ""/rc?
        var buid: String = "",  // all/bin
        var sha256: String = "",
        var status: String = ""
)