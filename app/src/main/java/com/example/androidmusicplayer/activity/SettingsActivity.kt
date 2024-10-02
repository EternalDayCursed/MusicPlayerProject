package com.example.androidmusicplayer.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.ActivitySettingsBinding
import com.example.androidmusicplayer.dialog.ManageVisibleTabsDialog
import com.example.androidmusicplayer.dialog.RadioGroupDialog
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getProperPrimaryColor
import com.example.androidmusicplayer.extension.sendCommand
import com.example.androidmusicplayer.extension.updateTextColors
import com.example.androidmusicplayer.extension.value
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.NavigationIcon
import com.example.androidmusicplayer.helper.SHOW_FILENAME_ALWAYS
import com.example.androidmusicplayer.helper.SHOW_FILENAME_IF_UNAVAILABLE
import com.example.androidmusicplayer.helper.SHOW_FILENAME_NEVER
import com.example.androidmusicplayer.helper.isQPlus
import com.example.androidmusicplayer.helper.isTiramisuPlus
import com.example.androidmusicplayer.myclasses.RadioItem
import com.example.androidmusicplayer.playback.CustomCommands
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : CustomControllerActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.serverUrl.setText(config.serverURL)
        binding.setUrl.setOnClickListener {
            setupServerSetting()
        }
        updateMaterialActivityViews(binding.settingsCoordinator, binding.settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupUseEnglish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupLanguage()
        }
        setupManageExcludedFolders()
        setupManageShownTabs()
        setupSwapPrevNext()
        setupReplaceTitle()
        setupGaplessPlayback()
        updateTextColors(binding.settingsNestedScrollview)

        arrayOf(binding.settingsColorCustomizationSectionLabel, binding.settingsGeneralSettingsLabel, binding.settingsPlaybackSectionLabel).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupSwapPrevNext() = binding.apply {
        settingsSwapPrevNext.isChecked = config.swapPrevNext
        settingsSwapPrevNextHolder.setOnClickListener {
            settingsSwapPrevNext.toggle()
            config.swapPrevNext = settingsSwapPrevNext.isChecked
        }
    }

    private fun setupReplaceTitle() = binding.apply {
        settingsShowFilename.text = getReplaceTitleText()
        settingsShowFilenameHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(SHOW_FILENAME_NEVER, getString(R.string.never)),
                RadioItem(SHOW_FILENAME_IF_UNAVAILABLE, "If title is not available"),
                RadioItem(SHOW_FILENAME_ALWAYS, getString(R.string.always))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.showFilename) {
                config.showFilename = it as Int
                settingsShowFilename.text = getReplaceTitleText()
                refreshQueueAndTracks()
            }
        }
    }

    private fun getReplaceTitleText() = getString(
        when (config.showFilename) {
            SHOW_FILENAME_NEVER -> R.string.never
            SHOW_FILENAME_IF_UNAVAILABLE -> R.string.title_is_not_available
            else -> R.string.always
        }
    )

    private fun setupManageShownTabs() = binding.apply {
        settingsManageShownTabsHolder.setOnClickListener {
            ManageVisibleTabsDialog(this@SettingsActivity) { result ->
                val tabsMask = config.showTabs
                if (tabsMask != result) {
                    config.showTabs = result
                    withPlayer {
                        sendCommand(CustomCommands.RELOAD_CONTENT)
                    }
                }
            }
        }
    }

    private fun setupManageExcludedFolders() {
        binding.settingsManageExcludedFoldersHolder.beVisibleIf(isQPlus())
        binding.settingsManageExcludedFoldersHolder.setOnClickListener {
            startActivity(Intent(this, ExcludedFoldersActivity::class.java))
        }
    }

    private fun setupGaplessPlayback() = binding.apply {
        settingsGaplessPlayback.isChecked = config.gaplessPlayback
        settingsGaplessPlaybackHolder.setOnClickListener {
            settingsGaplessPlayback.toggle()
            config.gaplessPlayback = settingsGaplessPlayback.isChecked
            withPlayer {
                sendCommand(CustomCommands.TOGGLE_SKIP_SILENCE)
            }
        }
    }
    private fun setupServerSetting() = binding.apply {
        config.serverURL = serverUrl.value
        Log.d("SETTINGS","${config.serverURL} ${serverUrl.value} is set")
    }
}
