package com.example.androidmusicplayer.activity

import android.os.Bundle
import com.example.androidmusicplayer.adapter.ExcludedFoldersAdapter
import com.example.androidmusicplayer.databinding.ActivityExcludedFoldersBinding
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getProperTextColor
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.NavigationIcon
import com.example.androidmusicplayer.interfaces.RefreshRecyclerViewListener


class ExcludedFoldersActivity : CustomActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityExcludedFoldersBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(binding.excludedFoldersCoordinator, binding.excludedFoldersList, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.excludedFoldersList, binding.excludedFoldersToolbar)
        updateFolders()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.excludedFoldersToolbar, NavigationIcon.Arrow)
    }

    private fun updateFolders() {
        val folders = config.excludedFolders.toMutableList() as ArrayList<String>
        binding.excludedFoldersPlaceholder.apply {
            beVisibleIf(folders.isEmpty())
            setTextColor(getProperTextColor())
        }

        val adapter = ExcludedFoldersAdapter(this, folders, this, binding.excludedFoldersList) {}
        binding.excludedFoldersList.adapter = adapter
    }

    override fun refreshItems() {
        updateFolders()
    }
}
