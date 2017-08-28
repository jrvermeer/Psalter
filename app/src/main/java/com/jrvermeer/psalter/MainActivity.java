package com.jrvermeer.psalter;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MediaService.IMediaCallbacks {
    private MediaService.MediaBinder service;
    private SharedPreferences sPref;
    private ViewPager viewPager;
    private FloatingActionButton fab;
    private Random rand = new Random();
    private LinearLayout llSearchResults;
    private ListView lvSearchResults;
    private MenuItem searchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize theme (must do this before calling setContentView())
        sPref = getSharedPreferences("settings", MODE_PRIVATE);
        boolean nightMode = sPref.getBoolean(getResources().getString(R.string.pref_nightmode_key), false);
        if(nightMode){
            setTheme(R.style.AppTheme_Dark);
        }
        else setTheme(R.style.AppTheme_Light);
        setContentView(R.layout.activity_main);

        // initialize search results
        PsalterDb db = new PsalterDb(this);
        llSearchResults = (LinearLayout) findViewById(R.id.llSearchResults);
        lvSearchResults = (ListView)findViewById(R.id.lvSearchResults);
        lvSearchResults.setAdapter(new PsalterSearchAdapter(this, db));
        lvSearchResults.setOnItemClickListener(searchItemClickListener);

        // initialize toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // initialize main psalter viewpager
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        final PsalterPagerAdapter adapter = new PsalterPagerAdapter(this, db);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(5);
        viewPager.addOnPageChangeListener(pageChangeListener);

        // initialize fab
        fab = ((FloatingActionButton)findViewById(R.id.fab));
        fab.setOnClickListener(fabListener);

        // initialize media service
        Intent intent = new Intent(this, MediaService.class);
        getApplicationContext().bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed(){
        if(llSearchResults.getVisibility() == View.VISIBLE){
            hideSearchResultsScreen();
        }
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(service != null && isFinishing()){
            service.stopMedia();
            getApplicationContext().unbindService(mConnection);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_options, menu);
        boolean nightMode = sPref.getBoolean(getResources().getString(R.string.pref_nightmode_key), false);
        if(nightMode){
            menu.getItem(3).setChecked(true);
        }
        searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    int number = Integer.parseInt(query);
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

    private void stringSearch(String query){
        showSearchResultsScreen();
        ((PsalterSearchAdapter)lvSearchResults.getAdapter()).queryPsalter(query);
    }

    private void goToPsalter(int psalterNumber){
        searchMenuItem.collapseActionView();
        hideSearchResultsScreen();
        viewPager.setCurrentItem(psalterNumber - 1, true); //viewpager goes by index
    }
    private void showSearchResultsScreen(){
        llSearchResults.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
    }
    private void hideSearchResultsScreen(){
        llSearchResults.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_nightmode){
            boolean nightmode = !item.isChecked(); //checked property must be updated manually, so new value is opposite of old value
            sPref.edit().putBoolean(getResources().getString(R.string.pref_nightmode_key), nightmode).commit();
            item.setChecked(nightmode);
            recreate();
        }
        else if(id == R.id.action_random){
            int number = rand.nextInt(viewPager.getAdapter().getCount());
            viewPager.setCurrentItem(number, true);
        }
        else if(id == R.id.action_goto_psalm){
            int psalm = ((PsalterPagerAdapter)viewPager.getAdapter()).getPsalter(viewPager.getCurrentItem()).getPsalm();
            String url = "https://www.biblegateway.com/passage?search=Psalm+" + psalm;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        else if(id == R.id.action_search){
            final SearchView searchView = (SearchView)item.getActionView();
            final Timer timer = new Timer();
            final Handler handler = new Handler();
            MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    final String[] hints = getResources().getStringArray(R.array.search_hints);
                    final TimerTask task = new TimerTask() {
                        private int iteration = 0;
                        @Override
                        public void run() {
                            run(iteration++);
                        }

                        public void run(final int iteration){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    searchView.setQueryHint(hints[iteration % hints.length]);
                                }
                            });
                        }
                    };
                    timer.schedule(task, 0, 2000);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    timer.cancel();
                    hideSearchResultsScreen();
                    return true;
                }
            });
        }
        else return false;

        return true;
    }

    //MediaCallbacks Interface
    @Override
    public void playerFinished() {
        fab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = (MediaService.MediaBinder) iBinder;
            service.setCallbacks(MainActivity.this);
            if(service.isPlaying()){
                fab.setImageResource(R.drawable.ic_stop_white_24dp);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    };

    private View.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(service == null) return;

            if(service.isPlaying()){
                service.stopMedia();
            }
            else {
                int psalterNumber = viewPager.getCurrentItem() + 1; // 0 based index
                if(service.playMedia(psalterNumber)) {
                    fab.setImageResource(R.drawable.ic_stop_white_24dp);
                }
                else{
                    Toast.makeText(MainActivity.this, "Audio not available", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        @Override
        public void onPageScrollStateChanged(int state) {}
        @Override
        public void onPageSelected(int position) {
            if(service != null) service.stopMedia();
        }
    };
    private AdapterView.OnItemClickListener searchItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                TextView tvNumber = (TextView) view.findViewById(R.id.tvSearchNumber);
                int num = Integer.parseInt((String) tvNumber.getText());
                goToPsalter(num);

            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };
}
