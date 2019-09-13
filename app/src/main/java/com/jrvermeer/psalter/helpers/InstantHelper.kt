package com.jrvermeer.psalter.helpers

import android.content.Context
import android.os.Build
import java.io.File

class InstantHelper(val context: Context){
    val isInstantApp get() = Build.VERSION.SDK_INT >= 26 && context.packageManager.isInstantApp
    fun transferInstantAppData(){
        if(isInstantApp) return

        val instantAppFiles = context.filesDir.list()
        if(instantAppFiles.isNotEmpty()) {
            val externalDir = context.getExternalFilesDir(null) ?: return
            for(fileName in instantAppFiles){
                val sourceFile = File(context.filesDir, fileName)
                val destFile = File(externalDir, fileName)
                sourceFile.copyRecursively(destFile, true)
                sourceFile.deleteRecursively()
            }
        }
    }
}