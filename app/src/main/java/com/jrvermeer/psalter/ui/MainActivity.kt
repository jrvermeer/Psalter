package com.jrvermeer.psalter.ui

import kotlinx.android.synthetic.main.activity_main.*

import com.jrvermeer.psalter.models.*
import com.jrvermeer.psalter.infrastructure.*
import com.jrvermeer.psalter.ui.adaptors.*

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.IBinder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.core.widget.TextViewCompat
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.jrvermeer.psalter.*
import com.jrvermeer.psalter.helpers.IntentHelper
import com.jrvermeer.psalter.helpers.StorageHelper
import com.jrvermeer.psalter.helpers.TutorialHelper
import kotlinx.android.synthetic.main.psalter_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private var searchMode = SearchMode.Psalter
    private lateinit var storage: StorageHelper
    private lateinit var psalterDb: PsalterDb
    private lateinit var tutorials: TutorialHelper
    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView
    private var mediaService: MediaServiceBinder? = null

    private val selectedPsalter get() = psalterDb.getIndex(viewpager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init(this)
        Logger.d("MainActivity created")
        storage = StorageHelper(this)
        psalterDb = PsalterDb(this, this)

        // must initialize theme before calling setContentView
        setTheme(if (storage.isNightMode) R.style.AppTheme_Dark else R.style.AppTheme_Light)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar)

        fabToggleScore.setChecked(storage.isScoreShown)
        viewpager.adapter = PsalterPagerAdapter(this, psalterDb, storage.isScoreShown, storage.isNightMode)
                .onPageCreated { v, i -> pageCreated(v, i) }
        viewpager.currentItem = storage.pageIndex
        lvSearchResults.adapter = PsalterSearchAdapter(this, psalterDb)

        tutorials = TutorialHelper(this)
        tutorials.showShuffleTutorial(fab)
        tutorials.showScoreTutorial(fabToggleScore)

        setupEventHandlers()
    }

    override fun onResume() {
        super.onResume()
        // initialize media service
        bindService(Intent(this@MainActivity, MediaService::class.java), mConnection, Service.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(mConnection)
    }

    override fun onBackPressed() {
        if (lvSearchResults.isShown) {
            collapseSearchView()
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)

        menu.findItem(R.id.action_nightmode).isChecked = storage.isNightMode

        searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                when (searchMode) {
                    SearchMode.Lyrics -> performLyricSearch(query ?: "", true)
                    SearchMode.Psalm -> performPsalmSearch(query?.toInt() ?: 0)
                    SearchMode.Psalter -> performPsalterSearch(query?.toInt() ?: 0)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (searchMode == SearchMode.Lyrics && query != null && query.length > 1) {
                    performLyricSearch(query, false)
                    return true
                }
                return false
            }
        })

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                showSearchButtons()
                menu.hideExcept(searchMenuItem)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                hideSearchResultsScreen()
                hideSearchButtons()
                invalidateOptionsMenu()
                return true
            }
        })
        setSearchModePsalter()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_nightmode -> toggleNightMode()
            R.id.action_random -> goToRandom()
            R.id.action_shuffle -> shuffle(true)
            R.id.action_rate -> startActivity(IntentHelper.RateIntent)
            R.id.action_sendfeedback -> startActivity(IntentHelper.FeedbackIntent)
            else -> return false
        }
        return true
    }

    private fun goToRandom() {
        Logger.d("GoToRandom")
        if (mediaService?.isShuffling == true && mediaService?.isPlaying == true) {
            mediaService?.skipToNext()
        } else launch {
            val next = psalterDb.getRandom()
            Logger.d("Going to ${next.title}")
            viewpager.setCurrentItem(next.id, true)
        }
        Logger.event(LogEvent.GoToRandom)
    }

    private fun toggleNightMode() {
        storage.toggleNightMode()
        recreateSafe()
    }

    private fun setSearchModePsalm() {
        searchView.inputType = InputType.TYPE_CLASS_NUMBER
        searchView.queryHint = "Enter Psalm (1 - 150)"
        searchMode = SearchMode.Psalm
        searchBtn_Psalm.deselect(searchBtn_Psalter, searchBtn_Lyrics)
   }
    private fun setSearchModeLyrics() {
        searchView.inputType = InputType.TYPE_CLASS_TEXT
        searchView.queryHint = "Enter search query"
        searchMode = SearchMode.Lyrics
        searchBtn_Lyrics.deselect(searchBtn_Psalter, searchBtn_Psalm)
    }
    private fun setSearchModePsalter() {
        searchView.inputType = InputType.TYPE_CLASS_NUMBER
        searchView.queryHint = "Enter Psalter number (1 - 434)"
        searchMode = SearchMode.Psalter
        searchBtn_Psalter.deselect(searchBtn_Psalm, searchBtn_Lyrics)
    }

    private fun performPsalterSearch(psalterNumber: Int){
        if (psalterNumber in 1..434) {
            collapseSearchView()
            goToPsalter(psalterNumber)
            Logger.searchPsalter(psalterNumber)
        }
        else this.shortToast("Pick a number between 1 and 434")
    }
    private fun performLyricSearch(query: String, logEvent: Boolean) {
        showSearchResultsScreen()
        (lvSearchResults.adapter as PsalterSearchAdapter).queryPsalter(query)
        lvSearchResults.setSelectionAfterHeaderView()
        if(logEvent) Logger.searchLyrics(query)
    }
    private fun performPsalmSearch(psalm: Int) {
        if (psalm in 1..150) {
            showSearchResultsScreen()
            (lvSearchResults.adapter as PsalterSearchAdapter).getAllFromPsalm(psalm)
            lvSearchResults.setSelectionAfterHeaderView()
            Logger.searchPsalm(psalm)
        }
        else this.shortToast("Pick a number between 1 and 150")
    }

    private fun showSearchButtons() {
        tableButtons.show()
        fabToggleScore.hide()
        fab.hide()
    }
    private fun hideSearchButtons() {
        tableButtons.hide()
        fabToggleScore.show()
        fab.show()
    }

    private fun showSearchResultsScreen() {
        lvSearchResults.show()
        viewpager.hide()
        fab.hide()
    }
    private fun hideSearchResultsScreen() {
        lvSearchResults.hide()
        viewpager.show()
        fab.show()
    }

    private fun collapseSearchView() {
        if (searchMenuItem.isActionViewExpanded) searchMenuItem.collapseActionView()
    }

    private fun goToPsalter(psalterNumber: Int) {
        val psalter = psalterDb.getPsalter(psalterNumber)!!
        viewpager.setCurrentItem(psalter.id, true) //viewpager goes by index
    }

    private fun toggleScore() {
        val adapter = viewpager.adapter as PsalterPagerAdapter
        storage.toggleScore()
        adapter.toggleScore()
        fabToggleScore.setChecked(storage.isScoreShown)
    }

    private fun togglePlay() {
        if (mediaService?.isPlaying == true) mediaService?.stop()
        else {
            val psalter = selectedPsalter
            mediaService?.play(psalter!!, false)
            mediaService?.startService(this)
        }
    }

    private fun shuffle(showLongPressTutorial: Boolean = false): Boolean {
        if(showLongPressTutorial) tutorials.showShuffleReminderTutorial(fab)
        mediaService?.play(selectedPsalter!!, true)
        mediaService?.startService(this)
        return true
    }

    private fun onItemClick(view: View) {
        try {
            val tvNumber = (view.tag as PsalterSearchAdapter.ViewHolder).tvNumber
            val num = Integer.parseInt(tvNumber!!.text.toString())

            //log event before collapsing searchview, so we can log the query text
            Logger.searchEvent(searchMode, searchView.query.toString(), num)

            collapseSearchView()
            goToPsalter(num)
        }
        catch (ex: Exception) {
            Logger.e("Error in MainActivity.onItemClick", ex)
        }
    }

    private fun pageCreated(page: View, position: Int) {
        if (position == viewpager.currentItem) {
            tutorials.showGoToPsalmTutorial(page.tvPagerPsalm)
        }
    }
    private fun onPageSelected(index: Int) {
        Logger.d("Page selected: $index")
        storage.pageIndex = index
        if (mediaService?.isPlaying == true && mediaService?.currentMediaId != index) { // if we're playing audio of a different #, stop it
            mediaService?.stop()
        }
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Logger.d("MediaService bound")
            mediaService = iBinder as MediaServiceBinder
            mediaService?.registerCallback(callback)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Logger.d("MediaService unbound")
        }
    }

    private var callback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            if (mediaService?.isPlaying == true) fab.setImageResourceSafe(R.drawable.ic_stop_white_24dp)
            else fab.setImageResourceSafe(R.drawable.ic_play_arrow_white_24dp)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (mediaService?.isPlaying == true) viewpager.currentItem = metadata!!.description.mediaId!!.toInt()
        }
    }

    private fun setupEventHandlers() {
        fab.setOnClickListener { togglePlay() }
        fab.setOnLongClickListener { shuffle() }
        fabToggleScore.setOnClickListener { toggleScore() }
        searchBtn_Psalm.setOnClickListener { setSearchModePsalm() }
        searchBtn_Lyrics.setOnClickListener { setSearchModeLyrics() }
        searchBtn_Psalter.setOnClickListener { setSearchModePsalter() }

        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                this@MainActivity.onPageSelected(position)
            }
        })
        lvSearchResults.setOnItemClickListener { _, view, _, _ -> onItemClick(view) }
    }

    private fun FloatingActionButton.setChecked(checked: Boolean) {
        val color: Int = if (checked && storage.isNightMode) Color.WHITE
        else if (checked) ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
        else if (storage.isNightMode) ContextCompat.getColor(this@MainActivity, R.color.colorUnselectedInverse)
        else ContextCompat.getColor(this@MainActivity, R.color.colorUnselected)

        this.drawable.mutate().setTint(color)
    }

    private fun TextView.deselect(vararg deselect: TextView){
        deselect.forEach { tv -> TextViewCompat.setTextAppearance(tv, R.style.Button) }
        TextViewCompat.setTextAppearance(this, R.style.Button_Selected)
    }

    private fun Menu.hideExcept(exception: MenuItem) {
        for (i in 0 until this.size()) {
            val item = this.getItem(i)
            if (item != exception) item.isVisible = false
        }
    }
}
