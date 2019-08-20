package com.jrvermeer.psalter.UI

import kotlinx.android.synthetic.main.activity_main.*

import com.jrvermeer.psalter.BuildConfig
import com.jrvermeer.psalter.Core.Contracts.*
import com.jrvermeer.psalter.Core.Models.*
import com.jrvermeer.psalter.Infrastructure.*
import com.jrvermeer.psalter.UI.Adaptors.*
import com.jrvermeer.psalter.R

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.design.widget.FloatingActionButton
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener, IPagerCallbacks {

    private var searchMode = SearchMode.Psalter
    private lateinit var storage: SimpleStorage
    private lateinit var psalterRepository: IPsalterRepository
    private lateinit var tutorials: TutorialHelper
    private lateinit var mediaService: MediaServiceBinder
    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView

    private val selectedPsalter get() = psalterRepository.getIndex(viewpager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.Init(this)
        psalterRepository = PsalterDb(this)
        storage = SimpleStorage(this)

        // must initialize theme before calling setContentView
        if (storage.isNightMode) setTheme(R.style.AppTheme_Dark)
        else setTheme(R.style.AppTheme_Light)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar)

        setFabChecked(storage.isScoreShown, fabToggleScore, storage.isNightMode)
        viewpager.adapter = PsalterPagerAdapter(this, psalterRepository,this, storage.isScoreShown, storage.isNightMode)
        viewpager.currentItem = storage.pageIndex
        lvSearchResults.adapter = PsalterSearchAdapter(this, psalterRepository)

        tutorials = TutorialHelper(this)
        tutorials.showShuffleTutorial(fab!!)
        tutorials.showScoreTutorial(fabToggleScore!!)

        fab.setOnLongClickListener { shuffle() }
        searchBtn_Psalm.setOnClickListener { searchPsalm() }
        searchBtn_Lyrics.setOnClickListener { searchLyrics() }
        searchBtn_Psalter.setOnClickListener { searchPsalter() }

        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
            override fun onPageSelected(position: Int) { this@MainActivity.onPageSelected(position) }
        })
        lvSearchResults.setOnItemClickListener { _, view, _, _ -> onItemClick(view) }
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
        searchView.setOnQueryTextListener(this)

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                showSearchButtons()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                hideSearchResultsScreen()
                hideSearchButtons()
                return true
            }
        })
        searchPsalter()
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        if (searchMode == SearchMode.Lyrics) {
            performStringSearch(query)
        } else {
            try {
                val number = query.toInt()
                if (searchMode == SearchMode.Psalter) {
                    if (number in 1..434) {
                        collapseSearchView()
                        goToPsalter(number)
                        Logger.searchEvent(searchMode, query, null)
                    } else {
                        Toast.makeText(this, "Pick a number between 1 and 434", Toast.LENGTH_SHORT).show()
                        return false
                    }
                } else if (searchMode == SearchMode.Psalm) {
                    if ( number in 1..150) {
                        performPsalmSearch(number)
                    } else {
                        Toast.makeText(this, "Pick a number between 1 and 150", Toast.LENGTH_SHORT).show()
                        return false
                    }
                } else return false

            } catch (ex: NumberFormatException) {
                return false
            }
        }
        searchView.clearFocus()
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (searchMode == SearchMode.Lyrics && newText.length > 1) {
            performStringSearch(newText)
            return true
        }
        return false
    }

    fun searchPsalm() {
        searchView.inputType = InputType.TYPE_CLASS_NUMBER
        searchView.queryHint = "Enter Psalm (1 - 150)"
        searchMode = SearchMode.Psalm
        if (Build.VERSION.SDK_INT < 23) {
            searchBtn_Psalter!!.setTextAppearance(this, R.style.Button)
            searchBtn_Psalm!!.setTextAppearance(this, R.style.Button_Selected)
            searchBtn_Lyrics!!.setTextAppearance(this, R.style.Button)
        } else {
            searchBtn_Psalter!!.setTextAppearance(R.style.Button)
            searchBtn_Psalm!!.setTextAppearance(R.style.Button_Selected)
            searchBtn_Lyrics!!.setTextAppearance(R.style.Button)
        }
    }

    fun searchLyrics() {
        searchView.inputType = InputType.TYPE_CLASS_TEXT
        searchView.queryHint = "Enter search query"
        searchMode = SearchMode.Lyrics
        if (Build.VERSION.SDK_INT < 23) {
            searchBtn_Psalter!!.setTextAppearance(this, R.style.Button)
            searchBtn_Psalm!!.setTextAppearance(this, R.style.Button)
            searchBtn_Lyrics!!.setTextAppearance(this, R.style.Button_Selected)
        } else {
            searchBtn_Psalter!!.setTextAppearance(R.style.Button)
            searchBtn_Psalm!!.setTextAppearance(R.style.Button)
            searchBtn_Lyrics!!.setTextAppearance(R.style.Button_Selected)
        }
    }

    fun searchPsalter() {
        searchView.inputType = InputType.TYPE_CLASS_NUMBER
        searchView.queryHint = "Enter Psalter number (1 - 434)"
        searchMode = SearchMode.Psalter

        TextViewCompat.setTextAppearance(searchBtn_Psalter, R.style.Button_Selected)
        TextViewCompat.setTextAppearance(searchBtn_Psalm, R.style.Button)
        TextViewCompat.setTextAppearance(searchBtn_Lyrics, R.style.Button)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_nightmode) {
            val nightMode = storage.toggleNightMode()
            item.isChecked = nightMode
            Logger.changeTheme(nightMode)

            if (Build.VERSION.SDK_INT == 23) { // framework bug in api 23 calling recreate inside onOptionsItemSelected.
                finish()
                startActivity(intent)
            } else recreate()
        } else if (id == R.id.action_random) {
            if (mediaService.isShuffling && mediaService.isPlaying) {
                mediaService.skipToNext()
            }
            else viewpager.setCurrentItem(psalterRepository.getRandom().id, true)

            Logger.event(LogEvent.GoToRandom)
        }
        else if (id == R.id.action_shuffle) {
            tutorials.showShuffleReminderTutorial(fab)
            shuffle()
        }
        else if (id == R.id.action_rate) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jrvermeer.psalter")))
        }
        else if (id == R.id.action_sendfeedback) {
            val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "jrvermeer.dev@gmail.com", null))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Psalter App")
            var body = "\n\n\n"
            body += "---------------------------\n"
            body += "App version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
            body += "Android version: " + Build.VERSION.RELEASE
            emailIntent.putExtra(Intent.EXTRA_TEXT, body)
            startActivity(Intent.createChooser(emailIntent, "Send email..."))
        }
        else return false

        return true
    }

    private fun performStringSearch(query: String) {
        showSearchResultsScreen()
        (lvSearchResults.adapter as PsalterSearchAdapter).queryPsalter(query)
        lvSearchResults.setSelectionAfterHeaderView()
    }

    private fun performPsalmSearch(psalm: Int) {
        showSearchResultsScreen()
        (lvSearchResults.adapter as PsalterSearchAdapter).getAllFromPsalm(psalm)
        lvSearchResults.setSelectionAfterHeaderView()
    }

    private fun goToPsalter(psalterNumber: Int) {
        val psalter = psalterRepository.getPsalter(psalterNumber)!!
        viewpager.setCurrentItem(psalter.id, true) //viewpager goes by index
    }

    private fun showSearchResultsScreen() {
        lvSearchResults.visibility = View.VISIBLE
        viewpager.visibility = View.GONE
        fab.hide()
    }

    private fun hideSearchResultsScreen() {
        lvSearchResults!!.visibility = View.GONE
        viewpager.visibility = View.VISIBLE
        fab!!.show()
    }

    private fun collapseSearchView() {
        if (searchMenuItem?.isActionViewExpanded) searchMenuItem.collapseActionView()
    }

    private fun showSearchButtons() {
        tableButtons.visibility = View.VISIBLE
        fabToggleScore.hide()
        fab.hide()
    }

    private fun hideSearchButtons() {
        tableButtons.visibility = View.GONE
        fabToggleScore.show()
        fab.show()
    }

    fun toggleScore(view: View?) {
        val adapter = viewpager.adapter as PsalterPagerAdapter
        if (storage.toggleScore()) {
            adapter.showScore()
            setFabChecked(true, fabToggleScore, storage.isNightMode)
        }
        else {
            adapter.hideScore()
            setFabChecked(false, fabToggleScore, storage.isNightMode)
        }

        val currentPage = viewpager.currentItem
        viewpager.adapter = adapter
        viewpager.currentItem = currentPage

        Logger.changeScore(storage.isScoreShown)
    }

    fun fabClick(view: View?) {
        if (mediaService.isPlaying) mediaService.stop()
        else {
            val psalter = selectedPsalter
            mediaService.play(this, psalter!!, false)
            Logger.playbackStarted(psalter.title, false)
        }
    }

    fun shuffle(): Boolean {
        val psalter = selectedPsalter
        mediaService.play(this, psalter!!, true)
        Logger.playbackStarted(psalter.title, true)

        tutorials.showShuffleRandomTutorial(toolbar.findViewById(R.id.action_random))
        return true
    }

    fun onItemClick(view: View) {
        try {
            val tvNumber = (view.tag as PsalterSearchAdapter.ViewHolder).tvNumber
            val num = Integer.parseInt(tvNumber!!.text.toString())

            //log event before collapsing searchview, so we can log the query text
            Logger.searchEvent(searchMode, searchView.query.toString(), num.toString())

            collapseSearchView()
            goToPsalter(num)
        } catch (ex: Exception) {
            Toast.makeText(this@MainActivity, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun pageCreated(page: View, position: Int) {
        if (position == viewpager.currentItem) {
            tutorials.showGoToPsalmTutorial(page.findViewById(R.id.tvPagerPsalm))
        }
    }

    fun onPageSelected(index: Int) {
        storage.pageIndex = index
        if (mediaService?.isPlaying && mediaService?.currentMediaId != index) { // if we're playing audio of a different #, stop it
            mediaService.stop()
        }
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Logger.d("MediaService connected to Activity")
            mediaService = iBinder as MediaServiceBinder
            mediaService.registerCallback(callback)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Logger.d("MediaService disconnected from Activity")
        }
    }

    internal var callback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            Logger.d("Playback state changed: " + state.state)
            if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                fab.setImageResource(R.drawable.ic_stop_white_24dp)
            } else
                fab.setImageResource(R.drawable.ic_play_arrow_white_24dp)

            if (fab.isShown) {  // stupid ass bug, setting images fails after toggling night mode. https://stackoverflow.com/a/52158081
                fab.hide()
                fab.show()
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Logger.d("Metadata changed")
            if (mediaService.isPlaying) {
                val id = Integer.parseInt(metadata!!.description.mediaId!!)
                viewpager.currentItem = id
            }
        }
    }

    private fun setFabChecked(checked: Boolean, fab: FloatingActionButton, nightMode: Boolean) {
        val color: Int = if (checked && nightMode) Color.WHITE
        else if (checked) ContextCompat.getColor(this, R.color.colorAccent)
        else if (nightMode) ContextCompat.getColor(this, R.color.colorUnselectedInverse)
        else ContextCompat.getColor(this, R.color.colorUnselected)

        fab.drawable.mutate().setTint(color)
    }
}
