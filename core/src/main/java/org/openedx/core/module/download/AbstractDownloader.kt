package org.openedx.core.module.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

abstract class AbstractDownloader {

    protected abstract val client: OkHttpClient

    private val downloadApi: DownloadApi by lazy {
        Retrofit.Builder()
            .baseUrl(org.openedx.core.BuildConfig.BASE_URL)
            .client(client)
            .build()
            .create(DownloadApi::class.java)
    }

    private var currentDownloadingFilePath: String? = null

    var isCanceled = false

    private var input: InputStream? = null
    private var fos: FileOutputStream? = null

    open suspend fun download(
        url: String,
        path: String
    ): Boolean {
        isCanceled = false
        return try {
            val response = downloadApi.downloadFile(url).body()
            if (response != null) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                input = response.byteStream()
                currentDownloadingFilePath = path
                fos = FileOutputStream(file)
                fos.use { output ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (input!!.read(buffer).also { read = it } != -1) {
                        output?.write(buffer, 0, read)
                    }
                    output?.flush()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            fos?.close()
            input?.close()
        }
    }


    suspend fun cancelDownloading() {
        isCanceled = true
        withContext(Dispatchers.IO) {
            try {
                fos?.close()
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        currentDownloadingFilePath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
    }

}