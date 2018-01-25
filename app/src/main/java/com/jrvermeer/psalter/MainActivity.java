package com.jrvermeer.psalter;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.jrvermeer.psalter.Adapters.PsalterPagerAdapter;
import com.jrvermeer.psalter.Adapters.PsalterSearchAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnLongClick;
import butterknife.OnPageChange;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Psalter";
    private SharedPreferences sPref;
    private Random rand = new Random();
    @BindView(R.id.viewpager) ViewPager viewPager;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.lvSearchResults) ListView lvSearchResults;
    @BindView(R.id.toolbar) Toolbar toolbar;
    MenuItem searchMenuItem;
    MediaControllerCompat mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize theme (must do this before calling setContentView())
        sPref = getSharedPreferences("settings", MODE_PRIVATE);
        boolean nightMode = sPref.getBoolean(getString(R.string.pref_nightmode), false);
        if(nightMode) setTheme(R.style.AppTheme_Dark);
        else setTheme(R.style.AppTheme_Light);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        lvSearchResults.setAdapter(new PsalterSearchAdapter(this));
        PsalterPagerAdapter pagerAdapter = new PsalterPagerAdapter((this));

        //need
        pagerAdapter.addCallbacks(new PsalterPagerAdapter.Callbacks() {
            @Override
            public void pageCreated(View page, int position) {
                if(position == viewPager.getCurrentItem()){
                    showTutorialIfNeeded(page);
                }
            }
        });
        viewPager.setAdapter(pagerAdapter);
        int savedPageIndex = sPref.getInt(getString(R.string.pref_lastindex), 0);
        viewPager.setCurrentItem(savedPageIndex);

        // initialize media service
        Intent intent = new Intent(this, MediaService.class);
        getApplicationContext().bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        // onSaveInstanceState is not called when using back button to close application
        saveState();
        if(isFinishing()){
            mediaController.getTransportControls().stop();
            getApplicationContext().unbindService(mConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        if(lvSearchResults.getVisibility() == View.VISIBLE){
            collapseSearchView();
        }
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveState();
        super.onSaveInstanceState(outState);
    }

    private void saveState(){
        sPref.edit().putInt(getString(R.string.pref_lastindex), viewPager.getCurrentItem()).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options, menu);

        boolean nightMode = sPref.getBoolean(getString(R.string.pref_nightmode), false);
        menu.findItem(R.id.action_nightmode).setChecked(nightMode);

        searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView)searchMenuItem.getActionView();

        final String[] hints = getResources().getStringArray(R.array.search_hints);
        final Handler handler = new Handler();
        final Runnable loopHints = new Runnable() {
            private int iteration = 0;
            @Override
            public void run() {
                searchView.setQueryHint(hints[iteration++ % hints.length]);
                handler.postDelayed(this, 2000);
            }
        };
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                handler.post(loopHints);
                return true;
            }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                handler.removeCallbacks(loopHints);
                hideSearchResultsScreen();
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    int number = Integer.parseInt(query);
                    collapseSearchView();
                    goToPsalter(number);
                    return true;
                } catch (NumberFormatException ex){
                    stringSearch(query);
                    return true;
                }
                finally {
                    searchView.clearFocus();
                }
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                try{
                    Integer.parseInt(newText);
                    return false;
                }
                catch (NumberFormatException ex){
                    if(newText.length() > 2){
                        stringSearch(newText);
                        return true;
                    }
                    return  false;
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_nightmode){
            boolean nightmode = !item.isChecked(); //checked property must be updated manually, so new value is opposite of old value
            sPref.edit().putBoolean(getString(R.string.pref_nightmode), nightmode).apply();
            item.setChecked(nightmode);
            if(Build.VERSION.SDK_INT == 23){ // framework bug in api 23 calling recreate inside onOptionsItemSelected.
                finish();
                startActivity(getIntent());
            }
            else recreate();
        }
        else if(id == R.id.action_random){
            int numberIndex = rand.nextInt(viewPager.getAdapter().getCount()); // random number between 0 and 433 (inclusive)
            viewPager.setCurrentItem(numberIndex, true);
        }
        else if(id == R.id.action_shuffle) shuffle();
        else if(id == R.id.action_rate){
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jrvermeer.psalter")));
        }
        else if(id == R.id.action_sendfeedback){
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto","jrvermeer.dev@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Psalter App");
            String body = "\n\n\n";
            body += "---------------------------\n";
            body += "App version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n";
            body += "Android version: " + Build.VERSION.RELEASE;
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
        else return false;

        return true;
    }

    private void stringSearch(String query){
        showSearchResultsScreen();
        ((PsalterSearchAdapter)lvSearchResults.getAdapter()).queryPsalter(query);
        lvSearchResults.setSelectionAfterHeaderView();
    }

    private void goToPsalter(int psalterNumber){
        viewPager.setCurrentItem(psalterNumber - 1, true); //viewpager goes by index
    }
    private void showSearchResultsScreen(){
        lvSearchResults.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
    }
    private void hideSearchResultsScreen(){
        lvSearchResults.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }
    private void collapseSearchView(){
        if(searchMenuItem != null && searchMenuItem.isActionViewExpanded()) searchMenuItem.collapseActionView();
    }

    @OnClick(R.id.fab)
    public void fabClick() {
        if(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
            mediaController.getTransportControls().stop();
        }
        else {
            mediaController.getTransportControls().setShuffleModeEnabled(false);
            mediaController.getTransportControls().playFromMediaId(getMediaId(), null);
        }
    }
    @OnLongClick(R.id.fab)
    public boolean shuffle(){
        mediaController.getTransportControls().setShuffleModeEnabled(true);
        mediaController.getTransportControls().playFromMediaId(getMediaId(), null);
        return true;
    }

    @OnItemClick(R.id.lvSearchResults)
        public void onItemClick(View view) {
        try {
            TextView tvNumber = ((PsalterSearchAdapter.ViewHolder)view.getTag()).tvNumber;
            int num = Integer.parseInt(tvNumber.getText().toString());
            collapseSearchView();
            goToPsalter(num);

        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @OnPageChange(value = R.id.viewpager)
    public void onPageSelected(int index) {
        if(mediaController != null && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING
                && !mediaController.getMetadata().getDescription().getMediaId().equals(String.valueOf(index + 1))){
            mediaController.getTransportControls().stop();
        }
    }

    private String getMediaId(){
        return String.valueOf(viewPager.getCurrentItem() + 1);
    }
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try{
                mediaController = new MediaControllerCompat(MainActivity.this, ((MediaService.MediaBinder) iBinder).getSessionToken());
                mediaController.registerCallback(callback);
                Log.d(TAG, "MediaService connected");
            }
            catch (RemoteException ex){
                Log.e(TAG, ex.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    };

    MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
                fab.setImageResource(R.drawable.ic_stop_white_24dp);
            }
            else fab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            int currentlyPlaying = Integer.parseInt(metadata.getDescription().getMediaId());
            goToPsalter(currentlyPlaying);
        }
    };

    public void showTutorialIfNeeded(View selectedPage){
        List<TapTarget> tutorialTargets = getTutorialTargets(selectedPage);
        if(tutorialTargets != null && tutorialTargets.size() > 0){
            new TapTargetSequence(this)
                    .targets(tutorialTargets)
                    .continueOnCancel(true)
                    .start();

            allTutorialsShown();
        }
    }

    public List<TapTarget> getTutorialTargets(View selectedPage){

        boolean goToPsalmTutorialShown = sPref.getBoolean(getString(R.string.pref_tutorialshown_gotopsalm), false);
        boolean fabLongPressTutorialShown = sPref.getBoolean(getString(R.string.pref_tutorialshown_fablongpress), false);
        if(goToPsalmTutorialShown && fabLongPressTutorialShown) return null;

        List<TapTarget> targets = new ArrayList<>();
        if(!fabLongPressTutorialShown){
            targets.add(TapTarget.forToolbarOverflow(toolbar, "New! Shuffle Audio", "Numbers will continue playing at random in the background"));
            targets.add(TapTarget.forView(fab, "Also start shuffling by pressing and holding the play button")
                    .transparentTarget(true));
        }

        if(!goToPsalmTutorialShown && selectedPage != null){
            targets.add(TapTarget.forView(selectedPage.findViewById(R.id.tvPagerPsalm), "Click this link to go to Psalm"));
        }

        return  targets;
    }
    private void allTutorialsShown(){
        sPref.edit()
                .putBoolean(getString(R.string.pref_tutorialshown_fablongpress), true)
                .putBoolean(getString(R.string.pref_tutorialshown_gotopsalm), true)
                .apply();
    }
}
