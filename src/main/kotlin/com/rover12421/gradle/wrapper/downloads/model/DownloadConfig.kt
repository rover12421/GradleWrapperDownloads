package com.rover12421.gradle.wrapper.downloads.model

import com.rover12421.gradle.wrapper.downloads.runnable.SavaFileRunnable

val BLOCK_SIZE = 1*1024*1024L //1M

class DownloadConfig() {
    var info: DownloadInfo = DownloadInfo()
    var blocks: MutableList<DownloadBlockInfo> = mutableListOf()
    var lastModify: String = ""
    var filesize: Long = 0L
    var fileOff: Long = 0L

    constructor(info: DownloadInfo,
                lastModify: String,
                filesize: Long,
                fileOff: Long): this() {
        this.info = info
        this.lastModify = lastModify
        this.filesize = filesize
        this.fileOff = fileOff
    }

    constructor(config: DownloadConfig) : this(
            config.info, config.lastModify,
            config.filesize, config.fileOff)

    fun resetFileOff() {
        synchronized(this) {
            fileOff = 0L
            blocks.forEach {
                if (it.start + it.size > fileOff) {
                    fileOff = it.start + it.size
                }
            }
        }
    }

    fun getBlock(sava: SavaFileRunnable): DownloadBlockInfo {
        synchronized(this) {
            if (blocks.size > 0) {
                return blocks.removeAt(0)
            } else {
                val blockInfo = DownloadBlockInfo()
                if (fileOff >= filesize) {
                    blockInfo.size = -1
                } else {
                    blockInfo.size = BLOCK_SIZE
                    blockInfo.start = fileOff
                    fileOff += BLOCK_SIZE
                    if (fileOff > filesize) {
                        fileOff = filesize
                        blockInfo.size = filesize - blockInfo.start
                    }
                    sava.addBlock(blockInfo)
                }
                return blockInfo
            }
        }
    }
}