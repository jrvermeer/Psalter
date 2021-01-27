package com.jrvermeer.psalter.helpers

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.jrvermeer.psalter.infrastructure.Logger
import java.util.concurrent.TimeUnit

class RateHelper(private val activity: Activity,
                 private val storage: StorageHelper) {

    fun showRateDialogIfAppropriate() {
        if (!shouldShowDialog()) return

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                manager.launchReviewFlow(activity, reviewInfo)
                showRatePromptAttempted()
            }
        }
    }

    private fun shouldShowDialog(): Boolean {
        // if prompt hasn't been shown yet, set it to now for the sake of calculation
        if(storage.lastRatePromptTime <= 0) storage.lastRatePromptTime = System.currentTimeMillis()

        return storage.launchCount > 5
                && enoughTimeSinceLastShow()
    }
    private fun enoughTimeSinceLastShow(): Boolean {
        val daysSinceShown = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - storage.lastRatePromptTime)
        val daysToWait = if (storage.ratePromptCount == 0) 7 else 31
        return daysSinceShown >= daysToWait // no more than every month
    }
    private fun showRatePromptAttempted() {
        storage.ratePromptCount++
        storage.lastRatePromptTime = System.currentTimeMillis()
        storage.launchCount = 0 // reset requirements for re-showing
        Logger.ratePromptAttempt(storage.ratePromptCount)
    }
}