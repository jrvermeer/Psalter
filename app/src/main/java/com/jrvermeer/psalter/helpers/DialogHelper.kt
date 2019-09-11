package com.jrvermeer.psalter.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.jrvermeer.psalter.R

class DialogHelper(private val context: Context,
                   private val storage: StorageHelper,
                   private val sendMessage: (String) -> Unit) {

    fun showRateDialogIfAppropriate() {
        if (!shouldShowDialog()) return

        storage.ratePromptCount++
        storage.setLong(R.string.pref_lastRatePromptShownTime, System.currentTimeMillis())

        AlertDialog.Builder(context).run {
            setTitle("Are you enjoying the Psalter?")
            // Can't set handlers here, they need a reference to the dialog.
            // But we have to set buttons, bc we can't add them once built. We can only change them.
            setPositiveButton("Yes!", null)
            setNegativeButton("Could be better", null)
            setNeutralButton("Meh", null) // have to create it here, can't add new button once created
            setIcon(R.mipmap.ic_launcher)
            setCancelable(false)
            create()
        }.run {
            // have to override this so it doesn't close when a button is clicked
            setOnShowListener {
                this.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    userIsEnjoying(this)
                }
                this.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                    userNotEnjoying(this)
                }
                this.getButton(DialogInterface.BUTTON_NEUTRAL).visibility = View.INVISIBLE // Won't show when we want it if we use View.GONE
            }
            show()
        }
    }

    private fun userIsEnjoying(dialog: AlertDialog) {
        dialog.run {
            setTitle("Would you mind leaving a review?")
            getButton(DialogInterface.BUTTON_POSITIVE).run {
                text = "Ok"
                setOnClickListener {
                    dismiss()
                    context.startActivity(IntentHelper.RateIntent)
                    storage.doNotShowRatePrompt = true
                }
            }
            getButton(DialogInterface.BUTTON_NEGATIVE).run {
                text = "Maybe later"
                setOnClickListener {
                    dismiss()
                    resetRatePromptRequirements()
                }
            }
            getButton(DialogInterface.BUTTON_NEUTRAL).run {
                text = "Already did"
                visibility = View.VISIBLE
                setOnClickListener {
                    dismiss()
                    sendMessage("You. I like you.")
                    storage.doNotShowRatePrompt = true
                }
            }
        }
    }

    private fun userNotEnjoying(dialog: AlertDialog) {
        dialog.run {
            setTitle("Any suggestions for improvements?")
            getButton(DialogInterface.BUTTON_POSITIVE).run {
                text = "Yes"
                setOnClickListener {
                    dismiss()
                    context.startActivity(IntentHelper.FeedbackIntent)
                    storage.doNotShowRatePrompt = true
                }
            }
            getButton(DialogInterface.BUTTON_NEGATIVE).run {
                text = "Not right now"
                setOnClickListener {
                    dismiss()
                    resetRatePromptRequirements()
                }
            }
        }
    }

    private fun shouldShowDialog(): Boolean {
        //return true
        return !storage.doNotShowRatePrompt
                && storage.launchCount > 10
                && storage.playCount > 5
                && enoughTimeSinceLastShow()
    }
    private fun enoughTimeSinceLastShow(): Boolean {
        val lastShown = storage.getLong(R.string.pref_lastRatePromptShownTime, System.currentTimeMillis())
        val daysSinceShown = (System.currentTimeMillis() - lastShown) / 1000.0 / 60 / 60 / 24
        val daysToWait = 5 * storage.ratePromptCount + 5
        return return daysSinceShown >= daysToWait
    }
    private fun resetRatePromptRequirements(){
        storage.launchCount = 0
        storage.playCount = 0
    }
}