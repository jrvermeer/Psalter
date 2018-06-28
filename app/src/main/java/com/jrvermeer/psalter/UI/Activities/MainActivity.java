package com.jrvermeer.psalter.UI.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.flurry.android.FlurryAgent;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IStub;
import com.jrvermeer.psalter.BuildConfig;
import com.jrvermeer.psalter.Core.Contracts.IPagerCallbacks;
import com.jrvermeer.psalter.Core.Models.Psalter;
import com.jrvermeer.psalter.Infrastructure.Expansion.ExpansionHelper;
import com.jrvermeer.psalter.Infrastructure.Expansion.PsalterDownloaderService;
import com.jrvermeer.psalter.Infrastructure.Tutorials;
import com.jrvermeer.psalter.R;
import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository;
import com.jrvermeer.psalter.Infrastructure.MediaService;
import com.jrvermeer.psalter.Infrastructure.PsalterDb;
import com.jrvermeer.psalter.UI.Adaptors.PsalterPagerAdapter;
import com.jrvermeer.psalter.UI.Adaptors.PsalterSearchAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnLongClick;
import butterknife.OnPageChange;

public class MainActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        AHBottomNavigation.OnTabSelectedListener,
        IPagerCallbacks,
        IDownloaderClient {
    private final String TAG = "Psalter";
    private SharedPreferences sPref;
    private IPsalterRepository psalterRepository;
    private MediaControllerCompat mediaController;
    private IStub downloaderClient;
    private Tutorials tutorials;
    private ExpansionHelper expHelper;

    @BindView(R.id.viewpager) ViewPager viewPager;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.lvSearchResults) ListView lvSearchResults;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.tableButtons) TableLayout tableButtons;
    @BindView(R.id.searchBtn_Lyrics) Button  searchBtn_Lyrics;
    @BindView(R.id.searchBtn_Psalm) Button searchBtn_Psalm;
    @BindView(R.id.searchBtn_Psalter) Button searchBtn_Psalter;
    @BindView(R.id.bottom_navigation) AHBottomNavigation bottomNavigation;
    MenuItem searchMenuItem;

    SearchView searchView;

    private static final int SEARCH_MODE_PSALTER = 256;
    private static final int SEARCH_MODE_PSALM = 257;
    private static final int SEARCH_MODE_LYRICS = 258;
    private int SEARCH_MODE = SEARCH_MODE_PSALTER;

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

        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(this, getString(R.string.secret_flurry));

        psalterRepository = new PsalterDb(this);

        //initialize viewpager
        PsalterPagerAdapter pagerAdapter = new PsalterPagerAdapter(this, psalterRepository,this, false, nightMode);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(sPref.getInt(getString(R.string.pref_lastindex), 0));

        //initialize bottom nav
        AHBottomNavigationItem bottomNav_noScore = new AHBottomNavigationItem("", R.drawable.ic_no_score);
        AHBottomNavigationItem bottomNav_score = new AHBottomNavigationItem("", R.drawable.ic_score);
        bottomNavigation.addItem(bottomNav_noScore);
        bottomNavigation.addItem(bottomNav_score);
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_HIDE);
        bottomNavigation.setDefaultBackgroundColor(getResources().getColor(R.color.colorPrimaryInverse));
        bottomNavigation.setAccentColor(Color.WHITE);
        bottomNavigation.setOnTabSelectedListener(this);
        bottomNavigation.setCurrentItem(sPref.getInt(getString(R.string.pref_bottomnav), 0), true);

        lvSearchResults.setAdapter(new PsalterSearchAdapter(this, psalterRepository));

        // initialize media service
        Intent intent = new Intent(this, MediaService.class);
        getApplicationContext().bindService(intent, mConnection, Service.BIND_AUTO_CREATE);

        tutorials = new Tutorials(this);
        tutorials.showTutorial(fab,
                R.string.pref_tutorialshown_fablongpress,
                R.string.tutorial_fab_title,
                R.string.tutorial_fab_description, true);

        //bottomNavigation adds child views extremely late, meaning it's difficult to get a reference to the view.
        // Use this to tap into after that event
        bottomNavigation.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                tutorials.showTutorial(bottomNavigation.getViewAtPosition(1),
                        R.string.pref_tutorialshown_showscore,
                        R.string.tutorial_showscore_title,
                        R.string.tutorial_showscore_description);
                bottomNavigation.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        //ensure expansion files exist
        expHelper = new ExpansionHelper(this);
        if(!expHelper.expansionFilesDownloaded()){
            downloadExpansionFiles();
        }
    }



    @Override
    protected void onResume() {
        if(downloaderClient != null) downloaderClient.connect(this);
        super.onResume();
    }

    @Override
    protected void onStop() {
        if(downloaderClient != null) downloaderClient.disconnect(this);
        super.onStop();
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
        sPref.edit()
                .putInt(getString(R.string.pref_lastindex), viewPager.getCurrentItem())
                .putInt(getString(R.string.pref_bottomnav), bottomNavigation.getCurrentItem())
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options, menu);

        boolean nightMode = sPref.getBoolean(getString(R.string.pref_nightmode), false);
        menu.findItem(R.id.action_nightmode).setChecked(nightMode);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(this);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                showSearchButtons();
                return true;
            }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                hideSearchResultsScreen();
                hideSearchButtons();
                return true;
            }
        });
        searchPsalter();
        return true;
    }

    @Override
    public boolean onTabSelected(int position, boolean wasSelected) {
        PsalterPagerAdapter adapter = (PsalterPagerAdapter)viewPager.getAdapter();
        if(position == 1) adapter.showScore();
        else adapter.hideScore();
        int currentPage = viewPager.getCurrentItem();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPage);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(SEARCH_MODE == SEARCH_MODE_LYRICS){
            performStringSearch(query);
        }
        else {
            try {
                int number = Integer.parseInt(query);
                if(SEARCH_MODE == SEARCH_MODE_PSALTER){
                    collapseSearchView();
                    goToPsalter(number);
                }
                else if (SEARCH_MODE == SEARCH_MODE_PSALM && 1 <= number && number <= 150){
                    performPsalmSearch(number);
                }
                else return false;

            } catch (NumberFormatException ex){
                return false;
            }
        }
        searchView.clearFocus();
        return true;
    }
    @Override
    public boolean onQueryTextChange(String newText) {
        if(SEARCH_MODE == SEARCH_MODE_LYRICS && newText.length() > 1){
            performStringSearch(newText);
            return true;
        }
        return false;
    }
    @OnClick(R.id.searchBtn_Psalm)
    public void searchPsalm(){
        searchView.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchView.setQueryHint("Enter Psalm (1 - 150)");
        SEARCH_MODE = SEARCH_MODE_PSALM;
        if(Build.VERSION.SDK_INT < 23){
            searchBtn_Psalter.setTextAppearance(this, R.style.Button);
            searchBtn_Psalm.setTextAppearance(this, R.style.Button_Selected);
            searchBtn_Lyrics.setTextAppearance(this, R.style.Button);
        }
        else{
            searchBtn_Psalter.setTextAppearance(R.style.Button);
            searchBtn_Psalm.setTextAppearance(R.style.Button_Selected);
            searchBtn_Lyrics.setTextAppearance(R.style.Button);
        }
    }
    @OnClick(R.id.searchBtn_Lyrics)
    public void searchLyrics() {
        searchView.setInputType(InputType.TYPE_CLASS_TEXT);
        searchView.setQueryHint("Enter search query");
        SEARCH_MODE = SEARCH_MODE_LYRICS;
        if(Build.VERSION.SDK_INT < 23){
            searchBtn_Psalter.setTextAppearance(this, R.style.Button);
            searchBtn_Psalm.setTextAppearance(this, R.style.Button);
            searchBtn_Lyrics.setTextAppearance(this, R.style.Button_Selected);
        }
        else {
            searchBtn_Psalter.setTextAppearance(R.style.Button);
            searchBtn_Psalm.setTextAppearance(R.style.Button);
            searchBtn_Lyrics.setTextAppearance(R.style.Button_Selected);
        }
    }
    @OnClick(R.id.searchBtn_Psalter)
    public void searchPsalter(){
        searchView.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchView.setQueryHint("Enter Psalter number (1 - 434)");
        SEARCH_MODE = SEARCH_MODE_PSALTER;
        if(Build.VERSION.SDK_INT < 23){
            searchBtn_Psalter.setTextAppearance(this, R.style.Button_Selected);
            searchBtn_Psalm.setTextAppearance(this, R.style.Button);
            searchBtn_Lyrics.setTextAppearance(this, R.style.Button);
        }
        else{
            searchBtn_Psalter.setTextAppearance(R.style.Button_Selected);
            searchBtn_Psalm.setTextAppearance(R.style.Button);
            searchBtn_Lyrics.setTextAppearance(R.style.Button);
        }
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
            if(mediaController.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL
                    && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                mediaController.getTransportControls().skipToNext();
            }
            else {
                Psalter psalter = psalterRepository.getRandom();
                viewPager.setCurrentItem(psalter.getId(), true);
            }

        }
        else if(id == R.id.action_shuffle) {
            tutorials.showTutorial(fab, R.string.pref_tutorialshown_fabreminder,
                    R.string.tutorial_fabreminder_title,
                    R.string.tutorial_fabreminder_description,
                    true);
            shuffle();
        }
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

    private void performStringSearch(String query){
        showSearchResultsScreen();
        ((PsalterSearchAdapter)lvSearchResults.getAdapter()).queryPsalter(query);
        lvSearchResults.setSelectionAfterHeaderView();
    }
    private void performPsalmSearch(int psalm){
        showSearchResultsScreen();
        ((PsalterSearchAdapter)lvSearchResults.getAdapter()).getAllFromPsalm(psalm);
        lvSearchResults.setSelectionAfterHeaderView();
    }

    private void goToPsalter(int psalterNumber){
        Psalter psalter = psalterRepository.getPsalter(psalterNumber);
        viewPager.setCurrentItem(psalter.getId(), true); //viewpager goes by index
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
    private void showSearchButtons(){
        tableButtons.setVisibility(View.VISIBLE);
        bottomNavigation.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
    }
    private void hideSearchButtons(){
        tableButtons.setVisibility(View.GONE);
        bottomNavigation.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.fab)
    public void fabClick() {
        if(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
            mediaController.getTransportControls().stop();
        }
        else {
            mediaController.getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
            mediaController.getTransportControls().playFromMediaId(getMediaId(), null);
        }
    }
    @OnLongClick(R.id.fab)
    public boolean shuffle(){
        mediaController.getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
        mediaController.getTransportControls().playFromMediaId(getMediaId(), null);

        FlurryAgent.logEvent("shuffle_audio");
        tutorials.showTutorial(toolbar.findViewById(R.id.action_random),
                R.string.pref_tutorialshown_randomWhenShuffling,
                R.string.tutorial_randomWhenShuffling_title,
                R.string.tutorial_randomWhenShuffling_description);
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

    @Override
    public void pageCreated(View page, int position) {
        if(position == viewPager.getCurrentItem()){
            tutorials.showTutorial(page.findViewById(R.id.tvPagerPsalm),
                    R.string.pref_tutorialshown_gotopsalm,
                    R.string.tutorial_gotopsalm_title,
                    R.string.tutorial_gotopsalm_description);
            //showTutorialsOnStart(page.findViewById(R.id.tvPagerPsalm), bottomNavigation.getViewAtPosition(1));
        }
    }

    @OnPageChange(value = R.id.viewpager)
    public void onPageSelected(int index) {
        if(mediaController != null && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING //if media is playing
                && !mediaController.getMetadata().getDescription().getMediaId().equals(String.valueOf(index))){ // and it's not the media for current page
            mediaController.getTransportControls().stop();
        }
    }

    private String getMediaId(){
        return String.valueOf(viewPager.getCurrentItem());
    }
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try{
                mediaController = new MediaControllerCompat(MainActivity.this, ((MediaService.MediaBinder) iBinder).getSessionToken());
                mediaController.registerCallback(callback);
                callback.onPlaybackStateChanged(mediaController.getPlaybackState());

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
            if(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                int id = Integer.parseInt(metadata.getDescription().getMediaId());
                viewPager.setCurrentItem(id);
            }
        }
    };

    private static int WRITE_STORAGE = 1;
    private void downloadExpansionFiles() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder;
            if(Build.VERSION.SDK_INT >= 21){
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog);
            }
            else builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog);

            builder.setMessage(R.string.request_writestorage_message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    WRITE_STORAGE);
                        }
                    })
                .setCancelable(false)
                .show();
        }
        else{
            performDownloadExpansionFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == WRITE_STORAGE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                performDownloadExpansionFiles();
            }
            else {
                Toast.makeText(this, "Fine, enjoy your featureless app!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void performDownloadExpansionFiles(){
        try{
            FlurryAgent.logEvent("Expansion_Manual_Download");
            Intent notifierIntent = new Intent(this, MainActivity.class);
            notifierIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notifierIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(this,
                    pendingIntent, PsalterDownloaderService.class);
            if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                downloaderClient = DownloaderClientMarshaller.CreateStub(this, PsalterDownloaderService.class);
            }
        }
        catch (Exception ex){
            FlurryAgent.onError("downloading_files", "", ex);
            Toast.makeText(this, "Error downloading music and audio files", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onServiceConnected(Messenger m) {

    }

    @Override
    public void onDownloadStateChanged(int newState) {
        if(newState == IDownloaderClient.STATE_COMPLETED){
            AlertDialog.Builder builder;
            if(Build.VERSION.SDK_INT >= 21){
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog);
            }
            else builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog);

            builder.setMessage(R.string.download_complete_message)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) { }
                    })
                    .show();
        }
    }

    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {

    }
}