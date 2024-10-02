package com.example.androidmusicplayer.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.viewpager.widget.ViewPager
import com.example.androidmusicplayer.BuildConfig
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.ActivityMainBinding
import com.example.androidmusicplayer.databinding.BottomTablayoutItemBinding
import com.example.androidmusicplayer.dialog.FilePickerDialog
import com.example.androidmusicplayer.dialog.NewPlaylistDialog
import com.example.androidmusicplayer.dialog.RadioGroupDialog
import com.example.androidmusicplayer.dialog.SelectPlaylistDialog
import com.example.androidmusicplayer.dialog.SleepTimerCustomDialog
import com.example.androidmusicplayer.extension.adjustAlpha
import com.example.androidmusicplayer.extension.appLaunched
import com.example.androidmusicplayer.extension.applyColorFilter
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.beGoneIf
import com.example.androidmusicplayer.extension.beVisible
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.fadeIn
import com.example.androidmusicplayer.extension.fadeOut
import com.example.androidmusicplayer.extension.getBottomNavigationBackgroundColor
import com.example.androidmusicplayer.extension.getColoredDrawableWithColor
import com.example.androidmusicplayer.extension.getFilenameFromPath
import com.example.androidmusicplayer.extension.getFolderTracks
import com.example.androidmusicplayer.extension.getFormattedDuration
import com.example.androidmusicplayer.extension.getProperBackgroundColor
import com.example.androidmusicplayer.extension.getProperPrimaryColor
import com.example.androidmusicplayer.extension.getProperTextColor
import com.example.androidmusicplayer.extension.getTempFile
import com.example.androidmusicplayer.extension.getVisibleTabs
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.mediaScanner
import com.example.androidmusicplayer.extension.onTabSelectionChanged
import com.example.androidmusicplayer.extension.sendCommand
import com.example.androidmusicplayer.extension.showErrorToast
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.updateBottomTabItemColors
import com.example.androidmusicplayer.extension.updateTextColors
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.fragments.PlaylistsFragment
import com.example.androidmusicplayer.helper.LOWER_ALPHA
import com.example.androidmusicplayer.helper.M3uImporter
import com.example.androidmusicplayer.helper.MIME_TYPE_M3U
import com.example.androidmusicplayer.helper.PERMISSION_READ_STORAGE
import com.example.androidmusicplayer.helper.TAB_ALBUMS
import com.example.androidmusicplayer.helper.TAB_FOLDERS
import com.example.androidmusicplayer.helper.TAB_PLAYLISTS
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.helper.getPermissionToRequest
import com.example.androidmusicplayer.helper.isOreoPlus
import com.example.androidmusicplayer.helper.isQPlus
import com.example.androidmusicplayer.helper.tabsList
import com.example.androidmusicplayer.models.Events
import com.example.androidmusicplayer.myclasses.RadioItem
import com.example.androidmusicplayer.playback.CustomCommands
import com.simplemobiletools.musicplayer.adapters.ViewPagerAdapter
import me.grantland.widget.AutofitHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.FileOutputStream

class MainActivity : CustomMusicActivity() {
    private val PICK_IMPORT_SOURCE_INTENT = 1

    private var bus: EventBus? = null
    private var storedShowTabs = 0
    private var storedExcludedFolders = 0

    private val binding by viewBinding(ActivityMainBinding::inflate)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = true)
        storeStateVariables()
        setupTabs()
        setupCurrentTrackBar(binding.currentTrackBar.root)

        handlePermission(getPermissionToRequest()) {
            if (it) {
                initActivity()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }

        Log.d("RETROFIT","WILL START RETROFIT WITH CONFIG ${config.serverURL}")
        volumeControlStream = AudioManager.STREAM_MUSIC
        checkAppOnSDCard()
    }

    override fun onResume() {
        super.onResume()
        if (storedShowTabs != config.showTabs) {
            config.lastUsedViewPagerPage = 0
            System.exit(0)
            return
        }

        updateMenuColors()
        updateTextColors(binding.mainHolder)
        setupTabColors()
        val properTextColor = getProperTextColor()
        val properPrimaryColor = getProperPrimaryColor()
        binding.sleepTimerHolder.background = ColorDrawable(getProperBackgroundColor())
        binding.sleepTimerStop.applyColorFilter(properTextColor)
        binding.loadingProgressBar.setIndicatorColor(properPrimaryColor)
        binding.loadingProgressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)

        getAllFragments().forEach {
            it.setupColors(properTextColor, properPrimaryColor)
        }

        if (storedExcludedFolders != config.excludedFolders.hashCode()) {
            refreshAllFragments()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onBackPressed() {
        if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun refreshMenuItems() {
        binding.mainMenu.getToolbar().menu.apply {
            val isPlaylistFragment = getCurrentFragment() is PlaylistsFragment
            findItem(R.id.create_new_playlist).isVisible = isPlaylistFragment
            findItem(R.id.create_playlist_from_folder).isVisible = isPlaylistFragment
            findItem(R.id.import_playlist).isVisible = isPlaylistFragment && isOreoPlus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupOptionsMenu() {
        binding.mainMenu.getToolbar().inflateMenu(R.menu.menu_main)
        binding.mainMenu.toggleHideOnScroll(false)
        binding.mainMenu.setupMenu()

        binding.mainMenu.onSearchClosedListener = {
            getAllFragments().forEach {
                it.onSearchClosed()
            }
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            getCurrentFragment()?.onSearchQueryChanged(text)
        }

        binding.mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog()
                R.id.rescan_media -> refreshAllFragments(showProgress = true)
                R.id.sleep_timer -> showSleepTimer()
                R.id.create_new_playlist -> createNewPlaylist()
                R.id.create_playlist_from_folder -> createPlaylistFromFolder()
                R.id.import_playlist -> tryImportPlaylist()
                R.id.equalizer -> launchEqualizer()
                R.id.settings -> launchSettings()
                R.id.search_for_music -> launchSearch()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        updateStatusbarColor(getProperBackgroundColor())
        binding.mainMenu.updateColors()
    }

    private fun storeStateVariables() {
        config.apply {
            storedShowTabs = showTabs
            storedExcludedFolders = config.excludedFolders.hashCode()
        }
    }

    private fun initActivity() {
        bus = EventBus.getDefault()
        bus!!.register(this)
        // trigger a scan first so that the fragments will accurately reflect the scanning state
        mediaScanner.scan()
        initFragments()
        binding.sleepTimerStop.setOnClickListener { stopSleepTimer() }

        refreshAllFragments()
    }

    private fun refreshAllFragments(showProgress: Boolean = config.appRunCount == 1) {
        if (showProgress) {
            binding.loadingProgressBar.show()
        }

        handleNotificationPermission { granted ->
            mediaScanner.scan(progress = showProgress && granted) { complete ->
                runOnUiThread {
                    getAllFragments().forEach {
                        it.setupFragment(this)
                    }

                    if (complete) {
                        binding.loadingProgressBar.hide()
                        withPlayer {
                            if (currentMediaItem == null) {
                                maybePreparePlayer()
                            } else {
                                sendCommand(CustomCommands.RELOAD_CONTENT)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initFragments() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = tabsList.size - 1
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                binding.mainTabsHolder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it.finishActMode()
                }
                refreshMenuItems()
            }
        })
        binding.viewPager.currentItem = config.lastUsedViewPagerPage
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        getVisibleTabs().forEach { value ->
            binding.mainTabsHolder.newTab().setCustomView(R.layout.bottom_tablayout_item).apply {
                val tabItemBinding = BottomTablayoutItemBinding.bind(customView!!)
                tabItemBinding.tabItemIcon.setImageDrawable(getTabIcon(value))
                tabItemBinding.tabItemLabel.text = getTabLabel(value)
                AutofitHelper.create(tabItemBinding.tabItemLabel)
                binding.mainTabsHolder.addTab(this)
            }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false)
            },
            tabSelectedAction = {
                binding.mainMenu.closeSearch()
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true)
            }
        )

        binding.mainTabsHolder.beGoneIf(binding.mainTabsHolder.tabCount == 1)
    }

    private fun setupTabColors() {
        val activeView = binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true)

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.mainTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false)
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.mainTabsHolder.setBackgroundColor(bottomBarColor)
        updateNavigationBarColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until tabsList.size).filter { it != activeIndex }

    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            TAB_PLAYLISTS -> R.drawable.ic_playlist_vector
            TAB_FOLDERS -> R.drawable.ic_folders_vector
            TAB_ALBUMS -> R.drawable.ic_album_vector
            else -> R.drawable.ic_music_note_vector
        }

        return resources.getColoredDrawableWithColor(drawableId, getProperTextColor())
    }

    private fun getTabLabel(position: Int): String {
        val stringId = when (position) {
            TAB_PLAYLISTS -> R.string.playlists
            TAB_FOLDERS -> R.string.folders
            TAB_ALBUMS -> R.string.albums
            else -> R.string.tracks
        }

        return resources.getString(stringId)
    }

    private fun showSortingDialog() {
        getCurrentFragment()?.onSortOpen(this)
    }

    private fun createNewPlaylist() {
        NewPlaylistDialog(this) {
            EventBus.getDefault().post(Events.PlaylistsUpdated())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createPlaylistFromFolder() {
        FilePickerDialog(this, pickFile = false, enforceStorageRestrictions = false) {
            createPlaylistFrom(it)
        }
    }

    private fun createPlaylistFrom(path: String) {
        ensureBackgroundThread {
            getFolderTracks(path, true) { tracks ->
                runOnUiThread {
                    NewPlaylistDialog(this) { playlistId ->
                        tracks.forEach {
                            it.playListId = playlistId
                        }

                        ensureBackgroundThread {
                            audioHelper.insertTracks(tracks)
                            EventBus.getDefault().post(Events.PlaylistsUpdated())
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportPlaylistFromFile(resultData.data!!)
        }
    }

    private fun tryImportPlaylistFromFile(uri: Uri) {
        when {
            uri.scheme == "file" -> showImportPlaylistDialog(uri.path!!)
            uri.scheme == "content" -> {
                val tempFile = getTempFile("imports", uri.path!!.getFilenameFromPath())
                if (tempFile == null) {
                    toast(getString(R.string.unknown_error_occurred))
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)

                    showImportPlaylistDialog(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(getString(R.string.invalid_file_format))
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun tryImportPlaylist() {
        if (isQPlus()) {
            hideKeyboard()
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = MIME_TYPE_M3U

                try {
                    startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                } catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.system_service_disabled, Toast.LENGTH_LONG))
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) { granted ->
                if (granted) {
                    showFilePickerDialog()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showFilePickerDialog() {
        FilePickerDialog(this, enforceStorageRestrictions = false) { path ->
            SelectPlaylistDialog(this) { id ->
                importPlaylist(path, id)
            }
        }
    }

    private fun showImportPlaylistDialog(path: String) {
        SelectPlaylistDialog(this) { id ->
            importPlaylist(path, id)
        }
    }

    private fun importPlaylist(path: String, id: Int) {
        ensureBackgroundThread {
            M3uImporter(this) { result ->
                runOnUiThread {
                    toast(
                        when (result) {
                            M3uImporter.ImportResult.IMPORT_OK -> R.string.importing_successful
                            M3uImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                            else -> R.string.importing_failed
                        }
                    )

                    getAdapter()?.getPlaylistsFragment()?.setupFragment(this)
                }
            }.importPlaylist(path, id)
        }
    }

    private fun showSleepTimer() {
        val minutes = getString(R.string.minutes_raw)
        val hour = resources.getQuantityString(R.plurals.hours, 1, 1)

        val items = arrayListOf(
            RadioItem(5 * 60, "5 $minutes"),
            RadioItem(10 * 60, "10 $minutes"),
            RadioItem(20 * 60, "20 $minutes"),
            RadioItem(30 * 60, "30 $minutes"),
            RadioItem(60 * 60, hour)
        )

        if (items.none { it.id == config.lastSleepTimerSeconds }) {
            val lastSleepTimerMinutes = config.lastSleepTimerSeconds / 60
            val text = resources.getQuantityString(R.plurals.minutes, lastSleepTimerMinutes, lastSleepTimerMinutes)
            items.add(RadioItem(config.lastSleepTimerSeconds, text))
        }

        items.sortBy { it.id }
        items.add(RadioItem(-1, getString(R.string.custom)))

        RadioGroupDialog(this, items, config.lastSleepTimerSeconds) {
            if (it as Int == -1) {
                SleepTimerCustomDialog(this) {
                    if (it > 0) {
                        pickedSleepTimer(it)
                    }
                }
            } else if (it > 0) {
                pickedSleepTimer(it)
            }
        }
    }

    private fun pickedSleepTimer(seconds: Int) {
        config.lastSleepTimerSeconds = seconds
        config.sleepInTS = System.currentTimeMillis() + seconds * 1000
        startSleepTimer()
    }

    private fun startSleepTimer() {
        binding.sleepTimerHolder.fadeIn()
        withPlayer {
            sendCommand(CustomCommands.TOGGLE_SLEEP_TIMER)
        }
    }

    private fun stopSleepTimer() {
        binding.sleepTimerHolder.fadeOut()
        withPlayer {
            sendCommand(CustomCommands.TOGGLE_SLEEP_TIMER)
        }
    }

    private fun getAdapter() = binding.viewPager.adapter as? ViewPagerAdapter

    private fun getAllFragments() = getAdapter()?.getAllFragments().orEmpty()

    private fun getCurrentFragment() = getAdapter()?.getCurrentFragment()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun sleepTimerChanged(event: Events.SleepTimerChanged) {
        binding.sleepTimerValue.text = event.seconds.getFormattedDuration()
        binding.sleepTimerHolder.beVisible()

        if (event.seconds == 0) {
            finish()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun playlistsUpdated(event: Events.PlaylistsUpdated) {
        getAdapter()?.getPlaylistsFragment()?.setupFragment(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun tracksUpdated(event: Events.RefreshTracks) {
        getAdapter()?.getTracksFragment()?.setupFragment(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun shouldRefreshFragments(event: Events.RefreshFragments) {
        refreshAllFragments()
    }

    private fun launchEqualizer() {
        hideKeyboard()
        startActivity(Intent(applicationContext, EqualizerActivity::class.java))
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchSearch() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SearchActivity::class.java))
    }
}