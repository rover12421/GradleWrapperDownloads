package com.rover12421.gradle.wrapper.downloads.util

import okhttp3.*

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit

object OkHttpUtils {
    val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

    /**
     * 下载
     * 异步（根据断点请求）
     *
     * @param url
     * @param start
     * @param end
     * @param callback
     * @return
     */
    fun initRequest(url: String, start: Long, end: Long, callback: Callback): Call {
        val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=$start-$end")
                .build()

        val call = builder.build().newCall(request)
        call.enqueue(callback)

        return call
    }

    fun readRangeData(url: String, start: Long, end: Long): ByteArray? {
        val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=$start-$end")
                .build()

        builder.build().newCall(request).execute().use {
            return it.body()?.bytes()
        }
    }

    fun readHtml(url: String): String? {
        val request = Request.Builder()
                .url(url)
                .build()

        builder.build().newCall(request).execute().use {
            return it.body()?.string()
        }
    }

    /**
     * 下载
     * 同步请求
     *
     * @param url
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun initRequest(url: String): Response {
        val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-")
                .build()

        return builder.build().newCall(request).execute()
    }

    /**
     * 下载
     * 文件存在的情况下可判断服务端文件是否已经更改
     *
     * @param url
     * @param lastModify
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun initRequest(url: String, lastModify: String): Response {
        val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-")
                .header("If-Range", lastModify)
                .build()

        return builder.build().newCall(request).execute()
    }

    @Throws(IOException::class)
    fun getSources(url: String): String? {
        val request = Request.Builder()
                .url(url)
                .build()

        return builder.build().newCall(request).execute().use {
            it.body().use {
                it?.string()
            }
        }
    }

    /**
     * 下载
     * https请求时初始化证书
     *
     * @param certificates
     * @return
     */
    fun setCertificates(vararg certificates: InputStream) {
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)
            for ((index, certificate) in certificates.withIndex()) {
                val certificateAlias = Integer.toString(index)
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate))
                try {
                    certificate.close()
                } catch (e: IOException) {
                }

            }

            val sslContext = SSLContext.getInstance("TLS")
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

            trustManagerFactory.init(keyStore)
            sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())

            builder.sslSocketFactory(sslContext.socketFactory)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 上传
     * 异步
     *
     * @param url
     * @param requestBody
     * @param headers
     * @param callback
     * @return
     */
    fun initRequest(url: String, requestBody: RequestBody, headers: Map<String, String>?, callback: Callback): Call {
        val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

        if (headers != null && headers.size > 0) {
            val headerBuilder = Headers.Builder()

            for (key in headers.keys) {
                headerBuilder.add(key, headers[key])
            }
            requestBuilder.headers(headerBuilder.build())
        }

        val call = builder.build().newCall(requestBuilder.build())
        call.enqueue(callback)

        return call
    }
}
