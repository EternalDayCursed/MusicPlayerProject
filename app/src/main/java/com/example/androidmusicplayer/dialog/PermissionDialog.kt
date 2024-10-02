package com.example.androidmusicplayer.dialog

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.DialogMessageBinding
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.setupDialogStuff


class PermissionRequiredDialog(
    val activity: Activity,
    textId: Int,
    private val positiveActionCallback: () -> Unit,
    private val negativeActionCallback: (() -> Unit)? = null
) {
    private var dialog: AlertDialog? = null

    init {
        val view = DialogMessageBinding.inflate(activity.layoutInflater, null, false)
        view.message.text = activity.getString(textId)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.grant_permission) { _, _ -> positiveActionCallback() }
            .setNegativeButton(R.string.cancel) { _, _ -> negativeActionCallback?.invoke() }.apply {
                val title = activity.getString(R.string.permission_required)
                activity.setupDialogStuff(view.root, this, titleText = title) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }
}