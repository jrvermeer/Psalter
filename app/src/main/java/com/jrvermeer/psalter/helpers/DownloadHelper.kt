package com.jrvermeer.psalter.helpers

import android.content.Context
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.infrastructure.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL


class DownloadHelper(private val context: Context) {
    val storageDir: String get() = context.filesDir.path
    suspend fun downloadFile(path: String): File? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, path)
        val temp = File.createTempFile("prefix", "suffix")
        try {
            val url = URL(context.getString(R.string.mediaBaseUrl) + path)
            val connection = url.openConnection()
            connection.connect()
            Logger.d("Downloading $url")

            // download the file
            val input = BufferedInputStream(url.openStream(),8192)
            val output = FileOutputStream(temp.path)
            val data = ByteArray(1024)
            var count: Int
            do {
                count = input.read(data)
                if(count == -1) break
                output.write(data, 0, count)
            } while(true)

            output.flush()
            output.close()
            input.close()

            temp.copyTo(file, true)
            temp.delete()
            Logger.d("Download Complete: $url")
            return@withContext file
        }
        catch (e: Exception) {
            if(e !is FileNotFoundException) Logger.e("Error: ", e)
            return@withContext null
        }
        finally {
            if(temp.exists()) temp.delete()
        }
    }
}