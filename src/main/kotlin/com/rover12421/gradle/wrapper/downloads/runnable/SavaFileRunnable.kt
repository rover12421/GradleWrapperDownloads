package com.rover12421.gradle.wrapper.downloads.runnable

import com.google.gson.Gson
import com.rover12421.gradle.wrapper.downloads.MainApp
import com.rover12421.gradle.wrapper.downloads.model.DownloadBlockInfo
import com.rover12421.gradle.wrapper.downloads.model.DownloadConfig
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

class SavaFileRunnable(val config: DownloadConfig, val mainApp: MainApp) : Runnable {
    val writeConfig = DownloadConfig(config)
    val info = writeConfig.info
    val map: MutableMap<DownloadBlockInfo, ByteArray> = mutableMapOf()
    val gson = Gson()
    val dlFile = File(info.savaPath, info.fileName + ".dl")
    val cfile: RandomAccessFile
    init {
        dlFile.parentFile.mkdirs()
        cfile = RandomAccessFile(dlFile, "rws")
    }

    fun addBlock(block: DownloadBlockInfo) {
        synchronized(writeConfig) {
            writeConfig.blocks.add(block)
            sava()
        }
    }
    fun putData(block: DownloadBlockInfo, data: ByteArray) {
        synchronized(map) {
            map[block] = data
        }
    }

    private fun sava() {
        val cbytes = gson.toJson(writeConfig).toByteArray(StandardCharsets.UTF_8)
        cfile.write(cbytes)
        cfile.setLength(cbytes.size.toLong())
    }

    var finish = false

    override fun run() {
        println("SavaFileRunnable Thread start : ${Thread.currentThread().name}")
        val stime = System.currentTimeMillis()
        var count = 0

        val sfile = RandomAccessFile(File(info.savaPath, info.fileName), "rws")

        while (!finish) {
            sfile.setLength(writeConfig.filesize)
            if (map.isNotEmpty()) {
                val entry = synchronized(map) {
                   val first = map.entries.first()
                    map.remove(first.key)
                    first
                }
                val block = entry.key
                sfile.seek(block.start)
                sfile.write(entry.value)
                writeConfig.blocks.remove(block)

                writeConfig.fileOff = config.fileOff

                cfile.seek(0)
                sava()

                count += entry.value.size
                val time = System.currentTimeMillis() - stime
                val message = "Download : ${config.fileOff}/${config.filesize} = ${config.fileOff * 100f / config.filesize} speed : ${count * 1000f / time / 1024} kb/s , using time : ${time / 1000}s"
                println(message)
                mainApp.updateStatus(message)
            } else {
                Thread.sleep(10)
            }
        }

        sfile.close()
        cfile.close()

        dlFile.delete()


        val message = "Download Finish !!! time : ${System.currentTimeMillis() - stime}"
        println(message)
        mainApp.updateStatus(message)
    }
}