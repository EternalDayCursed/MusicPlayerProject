package com.example.androidmusicplayer.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.ExcludedFoldersActivity
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.activity.TracksActivity
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.adapter.FoldersAdapter
import com.example.androidmusicplayer.databinding.FragmentFoldersBinding
import com.example.androidmusicplayer.dialog.ChangeSortingDialog
import com.example.androidmusicplayer.extension.areSystemAnimationsEnabled
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.beGoneIf
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.isVisible
import com.example.androidmusicplayer.extension.mediaScanner
import com.example.androidmusicplayer.extension.underlineText
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.FOLDER
import com.example.androidmusicplayer.helper.TAB_FOLDERS
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.Folder
import com.example.androidmusicplayer.models.sortSafely

class FoldersFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var folders = ArrayList<Folder>()
    private val binding by viewBinding(FragmentFoldersBinding::bind)

    override fun setupFragment(activity: BaseCustomActivity) {
        ensureBackgroundThread {
            val folders = context.audioHelper.getAllFolders()

            activity.runOnUiThread {
                val scanning = activity.mediaScanner.isScanning()
                binding.foldersPlaceholder.text = if (scanning) {
                    context.getString(R.string.loading_files)
                } else {
                    context.getString(R.string.no_items_found)
                }
                binding.foldersPlaceholder.beVisibleIf(folders.isEmpty())
                binding.foldersFastscroller.beGoneIf(binding.foldersPlaceholder.isVisible())
                binding.foldersPlaceholder2.beVisibleIf(folders.isEmpty() && context.config.excludedFolders.isNotEmpty() && !scanning)
                binding.foldersPlaceholder2.underlineText()

                binding.foldersPlaceholder2.setOnClickListener {
                    activity.startActivity(Intent(activity, ExcludedFoldersActivity::class.java))
                }

                this.folders = folders

                val adapter = binding.foldersList.adapter
                if (adapter == null) {
                    FoldersAdapter(activity, folders, binding.foldersList) {
                        activity.hideKeyboard()
                        Intent(activity, TracksActivity::class.java).apply {
                            putExtra(FOLDER, (it as Folder).title)
                            activity.startActivity(this)
                        }
                    }.apply {
                        binding.foldersList.adapter = this
                    }

                    if (context.areSystemAnimationsEnabled) {
                        binding.foldersList.scheduleLayoutAnimation()
                    }
                } else {
                    (adapter as FoldersAdapter).updateItems(folders)
                }
            }
        }
    }

    override fun finishActMode() {
        getAdapter()?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = folders.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Folder>
        getAdapter()?.updateItems(filtered, text)
        binding.foldersPlaceholder.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchClosed() {
        getAdapter()?.updateItems(folders)
        binding.foldersPlaceholder.beGoneIf(folders.isNotEmpty())
    }

    override fun onSortOpen(activity: CustomActivity) {
        ChangeSortingDialog(activity, TAB_FOLDERS) {
            val adapter = getAdapter() ?: return@ChangeSortingDialog
            folders.sortSafely(activity.config.folderSorting)
            adapter.updateItems(folders, forceUpdate = true)
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        binding.foldersPlaceholder.setTextColor(textColor)
        binding.foldersFastscroller.updateColors(adjustedPrimaryColor)
        binding.foldersPlaceholder2.setTextColor(adjustedPrimaryColor)
        getAdapter()?.updateColors(textColor)
    }

    private fun getAdapter() = binding.foldersList.adapter as? FoldersAdapter
}
