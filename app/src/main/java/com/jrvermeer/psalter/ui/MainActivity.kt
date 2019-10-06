package com.jrvermeer.psalter.ui

import kotlinx.android.synthetic.main.activity_main.*

import com.jrvermeer.psalter.models.*
import com.jrvermeer.psalter.infrastructure.*
import com.jrvermeer.psalter.ui.adaptors.*

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.snackbar.Snackbar
import com.jrvermeer.psalter.*
import com.jrvermeer.psalter.helpers.*
import kotlinx.android.synthetic.main.psalter_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleOwner


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(), LifecycleOwner {

    private lateinit var storage: StorageHelper
    private lateinit var rateHelper: RateHelper
    private lateinit var instant: InstantHelper
    private lateinit var psalterDb: PsalterDb
    private lateinit var tutorials: TutorialHelper
    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView
    private var mediaService: MediaServiceBinder? = null

    private val selectedPsalter get() = psalterDb.getIndex(viewPager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        storage = StorageHelper(this)
        rateHelper = RateHelper(this, storage) { msg -> snack(msg) }
        tutorials = TutorialHelper(this)
        instant = InstantHelper(this)
        psalterDb = PsalterDb(this, this)

        // https://stackoverflow.com/a/28155638
//        if (BuildConfig.DEBUG) {
//            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .detectLeakedRegistrationObjects()
//                    .detectActivityLeaks()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build())
//        }

        lifecycle.addObserver(psalterDb)

        // must be done before super(), or onCreate() will be called twice and tutorials won't work
        AppCompatDelegate.setDefaultNightMode(if(storage.nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        Logger.init(this)
        setContentView(R.layout.activity_main)
        initViews()

        storage.launchCount++
        instant.transferInstantAppData()

        tutorials.showOfflineTutorial(toolbar as Toolbar)
        tutorials.showScoreTutorial(fabToggleScore)
        tutorials.showShuffleTutorial(fab)
    }

    override fun onStart() {
        super.onStart()
        // initialize media service
        bindService(Intent(this@MainActivity, MediaService::class.java), mConnection, Service.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        mediaService?.unregisterCallbacks()
        unbindService(mConnection)
        super.onStop()
    }
    override fun onBackPressed() {
        if (lvSearchResults.isShown) {
            collapseSearchView()
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.action_nightMode).isChecked = storage.nightMode
        initSearchView(menu)
        searchMode = SearchMode.Psalter
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_downloadAll)?.isVisible = !storage.allMediaDownloaded && !instant.isInstantApp
        if(storage.fabLongPressCount > 7) menu?.findItem(R.id.action_shuffle)?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_nightMode -> toggleNightMode()
            R.id.action_random -> goToRandom()
            R.id.action_shuffle -> shuffle(true)
            R.id.action_sendFeedback -> startActivity(IntentHelper.FeedbackIntent)
            R.id.action_downloadAll -> queueDownloads()
            else -> return false
        }
        return true
    }

    private fun queueDownloads(){
        snack("Check notification for progress.")
        launch {
            storage.allMediaDownloaded = true
            psalterDb.downloader.queueAllDownloads(psalterDb)
            Logger.event(LogEvent.EnableOffline)
        }
    }

    private fun goToRandom() {
        if (mediaService?.isShuffling == true && mediaService?.isPlaying == true) {
            Logger.skipToNext(selectedPsalter)
            mediaService?.skipToNext()
        } else {
            Logger.event(LogEvent.GoToRandom)
            val next = psalterDb.getRandom()
            viewPager.setCurrentItem(next.id, true)
        }
    }

    private fun toggleNightMode() {
        storage.nightMode = !storage.nightMode
        Logger.changeTheme(storage.nightMode)
        recreateSafe()
    }

    private fun performPsalterSearch(psalterNumber: Int){
        if (psalterNumber in 1..434) {
            collapseSearchView()
            goToPsalter(psalterNumber)
            Logger.searchPsalter(psalterNumber)
        }
        else snack("Pick a number between 1 and 434")
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
        else snack("Pick a number between 1 and 150")
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
        viewPager.hide()
        fab.hide()
    }
    private fun hideSearchResultsScreen() {
        lvSearchResults.hide()
        viewPager.show()
        fab.show()
    }

    private fun collapseSearchView() {
        if (searchMenuItem.isActionViewExpanded) searchMenuItem.collapseActionView()
    }

    private fun goToPsalter(psalterNumber: Int) {
        val psalter = psalterDb.getPsalter(psalterNumber)!!
        viewPager.setCurrentItem(psalter.id, true) //viewpager goes by index
    }

    private fun toggleScore() {
        val adapter = viewPager.adapter as PsalterPagerAdapter
        storage.scoreShown = !storage.scoreShown
        adapter.toggleScore()
        fabToggleScore.isSelected = storage.scoreShown
        Logger.changeScore(storage.scoreShown)
    }

    private fun togglePlay() {
        if (mediaService?.isPlaying == true) mediaService?.stop()
        else {
            val psalter = selectedPsalter
            mediaService?.play(psalter!!, false)
            mediaService?.startService(this)
            rateHelper.showRateDialogIfAppropriate()
        }
    }

    private fun shuffle(showLongPressTutorial: Boolean = false): Boolean {
        if(showLongPressTutorial) tutorials.showShuffleReminderTutorial(fab)
        else storage.fabLongPressCount++

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

    private fun onPageSelected(index: Int, view: View?) {
        Logger.d("Page selected: $index")
        storage.pageIndex = index
        storage.pageSelectionCount++
        if(storage.pageSelectionCount > 5 && view != null) tutorials.showGoToPsalmTutorial(view.tvPagerPsalm)
        if (mediaService?.isPlaying == true && mediaService?.currentMediaId != index) { // if we're playing audio of a different #, stop it
            mediaService?.stop()
        }
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Logger.d("MediaService bound")
            mediaService = iBinder as MediaServiceBinder
            mediaService?.registerCallbacks(callback)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            // this method IS NOT CALLED on unbindService(), only when service crashes or something. So it's pretty useless and misleading
        }
    }

    private var callback = object : MediaServiceCallbacks () {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            fab.isSelected = mediaService?.isPlaying ?: false
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (mediaService?.isPlaying == true) viewPager.currentItem = metadata!!.description.mediaId!!.toInt()
        }

        override fun onAudioUnavailable(psalter: Psalter) {
            snack("Audio unavailable for ${psalter.title}")
        }

        override fun onBeginShuffling() {
            snack("Shuffling", MessageLength.Short, "Skip") {
                mediaService?.skipToNext()
            }
        }
    }

    private lateinit var _searchMode: SearchMode
    private var searchMode
        get() = _searchMode
        set(mode) {
            _searchMode = mode
            searchView.setQuery("", false)
            when(mode) {
                SearchMode.Lyrics -> {
                    searchView.inputType = InputType.TYPE_CLASS_TEXT
                    searchView.queryHint = "Enter search query"
                    searchBtn_Lyrics.deselect(searchBtn_Psalter, searchBtn_Psalm)
                }
                SearchMode.Psalm -> {
                    searchView.inputType = InputType.TYPE_CLASS_NUMBER
                    searchView.queryHint = "Enter Psalm (1 - 150)"
                    searchBtn_Psalm.deselect(searchBtn_Psalter, searchBtn_Lyrics)
                }
                SearchMode.Psalter -> {
                    searchView.inputType = InputType.TYPE_CLASS_NUMBER
                    searchView.queryHint = "Enter Psalter number (1 - 434)"
                    searchBtn_Psalter.deselect(searchBtn_Psalm, searchBtn_Lyrics)
                }
            }
        }

    private fun initViews() {
        setSupportActionBar(toolbar as Toolbar)

        fab.setOnClickListener { togglePlay() }
        fab.setOnLongClickListener { shuffle() }

        fabToggleScore.isSelected = storage.scoreShown
        fabToggleScore.setOnClickListener { toggleScore() }

        searchBtn_Psalm.setOnClickListener { searchMode = SearchMode.Psalm }
        searchBtn_Lyrics.setOnClickListener { searchMode = SearchMode.Lyrics }
        searchBtn_Psalter.setOnClickListener { searchMode = SearchMode.Psalter }

        val viewPagerAdapter = PsalterPagerAdapter(this, this, psalterDb, storage.scoreShown, storage.nightMode)
        viewPager.adapter = viewPagerAdapter
        viewPager.currentItem = storage.pageIndex
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                this@MainActivity.onPageSelected(position, viewPagerAdapter.getView(position))
            }
        })

        lvSearchResults.adapter = PsalterSearchAdapter(this, psalterDb)
        lvSearchResults.setOnItemClickListener { _, view, _, _ -> onItemClick(view) }
    }
    private fun initSearchView(menu: Menu){
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
                invalidateOptionsMenu() // rebuild menu
                return true
            }
        })
    }

    private fun snack(msg: String, len: MessageLength = MessageLength.Long){
        Snackbar.make(mainCoordLayout, msg, len.forSnack()).show()
    }
    private fun snack(msg: String, len: MessageLength, action: String, onClick: () -> Unit){
        Snackbar.make(mainCoordLayout, msg, len.forSnack())
                .setAction(action) { onClick() }.show()
    }

    private fun AppCompatButton.deselect(vararg deselect: AppCompatButton){
        this.isSelected = true
        deselect.forEach { btn -> btn.isSelected = false }
    }

    private fun Menu.hideExcept(exception: MenuItem) {
        for (i in 0 until this.size()) {
            val item = this.getItem(i)
            if (item != exception) item.isVisible = false
        }
    }
}
