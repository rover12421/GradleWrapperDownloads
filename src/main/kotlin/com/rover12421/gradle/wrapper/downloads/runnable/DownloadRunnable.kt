package com.rover12421.gradle.wrapper.downloads.runnable

import com.rover12421.gradle.wrapper.downloads.model.DownloadConfig
import com.rover12421.gradle.wrapper.downloads.util.OkHttpUtils
import okhttp3.Request

class DownloadRunnable(val congfig: DownloadConfig, val savaFileRunnable: SavaFileRunnable) : Runnable {
    val info = congfig.info

    var finish = false

    override fun run() {
        println("DownloadThread ${Thread.currentThread().name} start ...")
        var block = congfig.getBlock(savaFileRunnable)
        val client = OkHttpUtils.builder.build()
        while (block.size > 0L) {
            println("${Thread.currentThread().name} $block")
            try {
                val start = block.start
                val end = block.start + block.size
                val request = Request.Builder()
                        .url(info.url)
                        .header("Range", "bytes=$start-$end")
                        .build()

                val response = client.newCall(request).execute()
                val bytes = response.body()?.bytes()
                response.close()
//                val bytes = OkHttpUtils.initRequest(model.url, block.start, block.start + block.size)
                if (bytes != null) {
                    savaFileRunnable.putData(block, bytes)
                    block = congfig.getBlock(savaFileRunnable)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        finish = true
    }
}