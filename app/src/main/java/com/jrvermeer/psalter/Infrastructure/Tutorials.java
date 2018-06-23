package com.jrvermeer.psalter.Infrastructure;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Jonathan on 6/22/2018.
 */

public class Tutorials {
    private Activity context;
    private TapTargetSequence targetSequence;
    SharedPreferences pref;

    public Tutorials(Activity context){
        this.context = context;
        pref = context.getSharedPreferences("settings", MODE_PRIVATE);
    }

    private TapTargetSequence getTargetSequence(){
        return new TapTargetSequence(context)
                .listener(listener)
                .continueOnCancel(true);
    }

    public void showTutorial(View view, @StringRes int prefTutorialShown, @StringRes int title, @StringRes int description){
        showTutorial(view, prefTutorialShown, title, description, false);
    }
    public void showTutorial(View view, @StringRes int prefTutorialShown, @StringRes int title, @StringRes int description, boolean transparent){
        boolean shown = pref.getBoolean(context.getResources().getString(prefTutorialShown), false);
        if(!shown && view != null){
            if(targetSequence == null) targetSequence = getTargetSequence();
            targetSequence.target(TapTarget.forView(view,
                    context.getResources().getString(title),
                    context.getResources().getString(description))
            .transparentTarget(transparent));
            targetSequence.start();
            pref.edit().putBoolean(context.getResources().getString(prefTutorialShown), true).apply();
        }
    }
    TapTargetSequence.Listener listener = new TapTargetSequence.Listener() {
        @Override
        public void onSequenceFinish() {
            targetSequence = null;
        }

        @Override
        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

        }

        @Override
        public void onSequenceCanceled(TapTarget lastTarget) {

        }
    };
}
