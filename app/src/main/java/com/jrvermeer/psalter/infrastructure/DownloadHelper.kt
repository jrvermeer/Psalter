package com.jrvermeer.psalter.infrastructure

import android.content.Context
import com.jrvermeer.psalter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL


class DownloadHelper(private val storageDir: String, private val baseUrl: String) {
    //private val baseUrl = context.getString(R.string.mediaBaseUrl)
    suspend fun downloadFile(path: String): File? = withContext(Dispatchers.Default){
        val file = File(storageDir, path)
        val temp = File.createTempFile("prefix", "suffix")
        try {
            val url = URL(baseUrl + path)
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
        } catch(e: FileNotFoundException){
            if(temp.exists()) temp.delete()
            return@withContext null
        }
        catch (e: Exception) {
            Logger.e("Error: ", e)
            return@withContext null
        }
    }
}