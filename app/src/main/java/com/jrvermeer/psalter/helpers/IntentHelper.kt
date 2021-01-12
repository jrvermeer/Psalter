package com.jrvermeer.psalter.helpers

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jrvermeer.psalter.BuildConfig

object IntentHelper {
    val RateIntent get() = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jrvermeer.psalter"))
    val FeedbackIntent: Intent get() {
        var body = "\n\n\n"
        body += "---------------------------\n"
        body += "App version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
        body += "Android version: " + Build.VERSION.RELEASE

        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("jrvermeer.dev@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Psalter App")
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
}