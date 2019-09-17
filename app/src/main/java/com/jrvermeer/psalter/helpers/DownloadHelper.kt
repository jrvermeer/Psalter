package com.jrvermeer.psalter.helpers

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.infrastructure.Logger
import com.jrvermeer.psalter.infrastructure.PsalterDb
import com.jrvermeer.psalter.models.Psalter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL


class DownloadHelper(private val context: Context) {
    val saveDir: File =
            if (Build.VERSION.SDK_INT >= 26 && context.packageManager.isInstantApp)
                context.filesDir
            else context.getExternalFilesDir(null) ?: throw Exception("Storage not mounted")

    private val baseUrl = context.getString(R.string.mediaBaseUrl)
    suspend fun downloadFile(path: String): File? = withContext(Dispatchers.IO) {
        val file = File(saveDir, path)
        val temp = File.createTempFile("prefix", "suffix")
        var input: BufferedInputStream? = null
        var output: FileOutputStream? = null
        try {
            val url = URL(baseUrl + path)
            val connection = url.openConnection()
            connection.connect()
            Logger.d("Downloading $path")

            // download the file
            input = BufferedInputStream(url.openStream(), 8192)
            output = FileOutputStream(temp.path)
            val data = ByteArray(1024)
            var count: Int
            do {
                if (!isActive) throw CancellationException()
                count = input.read(data)
                if (count == -1) break
                output.write(data, 0, count)
            } while (true)

            output.flush()
            temp.copyTo(file, true)
            Logger.d("Download Complete: $path")
            return@withContext file
        } catch (e: Exception) {
            if (e is CancellationException) Logger.d("Download Cancelled: $path")
            else if (e !is IOException) Logger.e("Error downloading $path", e)
            return@withContext null
        } finally {
            if (temp.exists()) temp.delete()
            output?.close()
            input?.close()
        }
    }

    suspend fun queueAllDownloads(psalterDb: PsalterDb) = withContext(Dispatchers.Default) {
        val dlManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        for (i in 0 until psalterDb.getCount()) {
            val psalter = psalterDb.getIndex(i)!!
            val audio = File(saveDir, psalter.audioPath)
            val score = File(saveDir, psalter.scorePath)

            if (!audio.exists()) {
                dlManager.enqueue(getDownloadRequest(psalter, PsalterMedia.Audio))
            }

            if (!score.exists()) {
                dlManager.enqueue(getDownloadRequest(psalter, PsalterMedia.Score))
            }
        }
    }

    private fun getDownloadRequest(psalter: Psalter, media: PsalterMedia): DownloadManager.Request {
        val path =  if(media == PsalterMedia.Audio) psalter.audioPath else psalter.scorePath
        return DownloadManager.Request(Uri.parse(baseUrl + path)).apply {
            setTitle("Downloading ${psalter.title} ${media.name}")
            setDestinationInExternalFilesDir(context, null, path)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        }
    }

    enum class PsalterMedia { Audio, Score }
}