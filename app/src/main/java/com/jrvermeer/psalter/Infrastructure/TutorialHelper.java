package com.jrvermeer.psalter.Infrastructure;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.jrvermeer.psalter.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Jonathan on 6/22/2018.
 */

public class TutorialHelper implements TapTargetSequence.Listener {
    private Activity context;
    private TapTargetSequence targetSequence;
    SimpleStorage storage = new SimpleStorage();


    public TutorialHelper(Activity context){
        this.context = context;
    }

    private TapTargetSequence getTargetSequence(){
        return new TapTargetSequence(context)
                .listener(this)
                .continueOnCancel(true)
                .considerOuterCircleCanceled(true);
    }

    public void showTutorial(View view, @StringRes int prefTutorialShown, @StringRes int title, @StringRes int description){
        showTutorial(view, prefTutorialShown, title, description, false);
    }
    public void showTutorial(View view, @StringRes int prefTutorialShown, @StringRes int title, @StringRes int description, boolean transparent){
        boolean shown = storage.getBoolean(prefTutorialShown);
        if(!shown && view != null){
            if(targetSequence == null) targetSequence = getTargetSequence();
            targetSequence.target(TapTarget.forView(view,
                    context.getString(title),
                    context.getString(description))
            .transparentTarget(transparent));
            targetSequence.start();
            storage.setBoolean(prefTutorialShown, true);
        }
    }

    public void showShuffleTutorial(View fab){
        showTutorial(fab,
                R.string.pref_tutorialshown_fablongpress,
                R.string.tutorial_fab_title,
                R.string.tutorial_fab_description, true);
    }

    public void showShuffleReminderTutorial(View fab){
        showTutorial(fab, R.string.pref_tutorialshown_fabreminder,
                R.string.tutorial_fabreminder_title,
                R.string.tutorial_fabreminder_description,
                true);
    }

    public void showShuffleRandomTutorial(View button){
        showTutorial(button,
                R.string.pref_tutorialshown_randomWhenShuffling,
                R.string.tutorial_randomWhenShuffling_title,
                R.string.tutorial_randomWhenShuffling_description);
    }

    public void showGoToPsalmTutorial(View view){
        showTutorial(view,
                R.string.pref_tutorialshown_gotopsalm,
                R.string.tutorial_gotopsalm_title,
                R.string.tutorial_gotopsalm_description);
    }
    public void showScoreTutorial(View view){
        showTutorial(view,
                R.string.pref_tutorialshown_showscore,
                R.string.tutorial_showscore_title,
                R.string.tutorial_showscore_description);
    }

    @Override
    public void onSequenceFinish() {
        targetSequence = null;
    }

    @Override
    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) { }

    @Override
    public void onSequenceCanceled(TapTarget lastTarget) { }
}
