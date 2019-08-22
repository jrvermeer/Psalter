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
import android.widget.TextView
import com.jrvermeer.psalter.Core.*
import kotlinx.android.synthetic.main.psalter_layout.view.*

class MainActivity : AppCompatActivity() {

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
        Logger.init(this)
        psalterRepository = PsalterDb(this)
        storage = SimpleStorage(this)

        // must initialize theme before calling setContentView
        setTheme(if (storage.isNightMode) R.style.AppTheme_Dark else R.style.AppTheme_Light)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar)

        fabToggleScore.setChecked(storage.isScoreShown)
        viewpager.adapter = PsalterPagerAdapter(this, psalterRepository, storage.isScoreShown, storage.isNightMode)
                .onPageCreated { v, i -> pageCreated(v, i) }
        viewpager.currentItem = storage.pageIndex
        lvSearchResults.adapter = PsalterSearchAdapter(this, psalterRepository)

        tutorials = TutorialHelper(this)
        tutorials.showShuffleTutorial(fab)
        tutorials.showScoreTutorial(fabToggleScore)

        fab.setOnLongClickListener { shuffle() }
        searchBtn_Psalm.setOnClickListener { setSearchModePsalm() }
        searchBtn_Lyrics.setOnClickListener { setSearchModeLyrics() }
        searchBtn_Psalter.setOnClickListener { setSearchModePsalter() }

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
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                when (searchMode) {
                    SearchMode.Lyrics -> performLyricSearch(query ?: "")
                    SearchMode.Psalm -> performPsalmSearch(query?.toInt() ?: 0)
                    SearchMode.Psalter -> performPsalterSearch(query?.toInt() ?: 0)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (searchMode == SearchMode.Lyrics && query != null && query.length > 1) {
                    performLyricSearch(query)
                    return true
                }
                return false
            }
        })

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
        setSearchModePsalter()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_nightmode -> {
                item.isChecked = storage.toggleNightMode()
                recreateSafe()
            }
            R.id.action_random -> {
                if (mediaService.isShuffling && mediaService.isPlaying) {
                    mediaService.skipToNext()
                }
                else viewpager.setCurrentItem(psalterRepository.getRandom().id, true)
                Logger.event(LogEvent.GoToRandom)
            }
            R.id.action_shuffle -> {
                tutorials.showShuffleReminderTutorial(fab)
                shuffle()
            }
            R.id.action_rate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jrvermeer.psalter")))
            }
            R.id.action_sendfeedback -> {
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
            else -> return false
        }
        return true
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
    private fun performLyricSearch(query: String) {
        showSearchResultsScreen()
        (lvSearchResults.adapter as PsalterSearchAdapter).queryPsalter(query)
        lvSearchResults.setSelectionAfterHeaderView()
        Logger.searchLyrics(query)
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
        if (searchMenuItem?.isActionViewExpanded) searchMenuItem.collapseActionView()
    }

    private fun goToPsalter(psalterNumber: Int) {
        val psalter = psalterRepository.getPsalter(psalterNumber)!!
        viewpager.setCurrentItem(psalter.id, true) //viewpager goes by index
    }

    fun toggleScore(view: View?) {
        val adapter = viewpager.adapter as PsalterPagerAdapter
        storage.toggleScore()
        adapter.toggleScore()
        fabToggleScore.setChecked(storage.isScoreShown)

        val currentPage = viewpager.currentItem
        viewpager.adapter = adapter
        viewpager.currentItem = currentPage
    }

    fun togglePlay(view: View?) {
        if (mediaService.isPlaying) mediaService.stop()
        else {
            val psalter = selectedPsalter
            mediaService.play(this, psalter!!, false)
        }
    }

    private fun shuffle(): Boolean {
        val psalter = selectedPsalter
        mediaService.play(this, psalter!!, true)

        tutorials.showShuffleRandomTutorial(toolbar.findViewById(R.id.action_random))
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
        storage.pageIndex = index
        if (mediaService?.isPlaying && mediaService?.currentMediaId != index) { // if we're playing audio of a different #, stop it
            mediaService.stop()
        }
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Logger.d("MediaService bound")
            mediaService = iBinder as MediaServiceBinder
            mediaService.registerCallback(callback)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Logger.d("MediaService unbound")
        }
    }

    private var callback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            if (mediaService.isPlaying) fab.setImageResourceSafe(R.drawable.ic_stop_white_24dp)
            else fab.setImageResourceSafe(R.drawable.ic_play_arrow_white_24dp)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (mediaService.isPlaying) viewpager.currentItem = metadata!!.description.mediaId!!.toInt()
        }
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
}
