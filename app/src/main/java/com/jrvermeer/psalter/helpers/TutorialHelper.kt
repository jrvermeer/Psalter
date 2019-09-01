package com.jrvermeer.psalter.helpers

import android.app.Activity
import androidx.annotation.StringRes
import android.view.View

import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.jrvermeer.psalter.R

/**
 * Created by Jonathan on 6/22/2018.
 */

class TutorialHelper(private val context: Activity) : TapTargetSequence.Listener {
    private var targetSequence: TapTargetSequence? = null
    private var storage = StorageHelper(context)

    private fun getTargetSequence(): TapTargetSequence {
        return TapTargetSequence(context)
                .listener(this)
                .continueOnCancel(true)
                .considerOuterCircleCanceled(true)
    }

    @JvmOverloads
    fun showTutorial(view: View?, @StringRes prefTutorialShown: Int, @StringRes title: Int, @StringRes description: Int, transparent: Boolean = false) {
        val shown = storage.getBoolean(prefTutorialShown)
        if (!shown && view != null) {
            if (targetSequence == null) targetSequence = getTargetSequence()
            targetSequence!!.target(TapTarget.forView(view,
                    context.getString(title),
                    context.getString(description))
                    .transparentTarget(transparent))
            targetSequence!!.start()
            storage.setBoolean(prefTutorialShown, true)
        }
    }

    fun showShuffleTutorial(fab: View) {
        showTutorial(fab,
                R.string.pref_tutorialshown_fablongpress,
                R.string.tutorial_fab_title,
                R.string.tutorial_fab_description, true)
    }

    fun showShuffleReminderTutorial(fab: View) {
        showTutorial(fab, R.string.pref_tutorialshown_fabreminder,
                R.string.tutorial_fabreminder_title,
                R.string.tutorial_fabreminder_description,
                true)
    }

    fun showGoToPsalmTutorial(view: View) {
        showTutorial(view,
                R.string.pref_tutorialshown_gotopsalm,
                R.string.tutorial_gotopsalm_title,
                R.string.tutorial_gotopsalm_description)
    }

    fun showScoreTutorial(view: View) {
        showTutorial(view,
                R.string.pref_tutorialshown_showscore,
                R.string.tutorial_showscore_title,
                R.string.tutorial_showscore_description, true)
    }

    override fun onSequenceFinish() {
        targetSequence = null
    }

    override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {}

    override fun onSequenceCanceled(lastTarget: TapTarget) {}
}
