package com.vermeer.jonathan.psalter;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MediaService.IMediaCallbacks {
    private MediaService.MediaBinder service;
    private boolean serviceConnected = false;
    private SharedPreferences sPref;
    private ViewPager viewPager;
    private FloatingActionButton fab;
    private Random rand;
    private LinearLayout llSearchResults;
    private ListView lvSearchResults;
    private MenuItem searchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up theme (must do this before calling setContentView())
        sPref = getSharedPreferences("settings", MODE_PRIVATE);
        boolean nightMode = sPref.getBoolean(getResources().getString(R.string.pref_nightmode_key), false);
        if(nightMode){
            setTheme(R.style.AppTheme_Dark);
        }
        else setTheme(R.style.AppTheme_Light);
        setContentView(R.layout.activity_main);

        PsalterDb db = new PsalterDb(this);

        rand = new Random();
        llSearchResults = (LinearLayout) findViewById(R.id.llSearchResults);
        lvSearchResults = (ListView)findViewById(R.id.lvSearchResults);
        lvSearchResults.setAdapter(new PsalterSearchAdapter(this, db));
        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try{
                    TextView tvNumber = (TextView)view.findViewById(R.id.tvSearchNumber);
                    int num = Integer.parseInt((String)tvNumber.getText());
                    goToPsalter(num);

                } catch (Exception ex){
                    Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //set up toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set up pager
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        final PsalterPagerAdapter adapter = new PsalterPagerAdapter(this, db);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(5);
        viewPager.addOnPageChangeListener(pageChangeListener);

        //set up fab
        fab = ((FloatingActionButton)findViewById(R.id.fab));
        fab.setOnClickListener(fabListener);

        //set up service
        Intent intent = new Intent(this, MediaService.class);
        getApplicationContext().bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceConnected && isFinishing()){
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
        searchView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
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
                finally{
                    searchMenuItem.collapseActionView();
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
        llSearchResults.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        ((PsalterSearchAdapter)lvSearchResults.getAdapter()).updateItems(query);
    }

    private void goToPsalter(int psalterNumber){
        llSearchResults.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        viewPager.setCurrentItem(psalterNumber - 1, true); //viewpager goes by index
        searchMenuItem.collapseActionView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_nightmode){
            boolean nightmode = !item.isChecked();
            sPref.edit().putBoolean(getResources().getString(R.string.pref_nightmode_key), nightmode).commit();
            item.setChecked(nightmode);
            recreate();
            return true;
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
                    timer.schedule(task, 0, 3000);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    timer.cancel();
                    return true;
                }
            });
        }

        return false;
    }

    @Override
    public void playerFinished() {
        fab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = (MediaService.MediaBinder) iBinder;
            service.setCallbacks(MainActivity.this);
            serviceConnected = true;
            if(service.isPlaying()){
                fab.setImageResource(R.drawable.ic_stop_white_24dp);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceConnected = false;
        }
    };

    private View.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!serviceConnected){
                Toast.makeText(MainActivity.this, "Media Service not connected", Toast.LENGTH_SHORT).show();
            }
            else if(service.isPlaying()){
                service.stopMedia();
            }
            else {
                //byte[] tune = ((PsalterPagerAdapter)viewPager.getAdapter()).getTune(viewPager.getCurrentItem());
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
            if(serviceConnected) service.stopMedia();
        }
    };
}
