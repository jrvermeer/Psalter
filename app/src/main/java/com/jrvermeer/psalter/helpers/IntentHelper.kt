package com.jrvermeer.psalter.helpers

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jrvermeer.psalter.BuildConfig

object IntentHelper {
    val RateIntent get() = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jrvermeer.psalter"))
    val FeedbackIntent: Intent get() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "jrvermeer.dev@gmail.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Psalter App")
        var body = "\n\n\n"
        body += "---------------------------\n"
        body += "App version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
        body += "Android version: " + Build.VERSION.RELEASE
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        return Intent.createChooser(emailIntent, "Send email...")
    }
}