package com.taskchain.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.taskchain.R
import com.taskchain.ui.designsystem.LocalUiComponents
import com.taskchain.ui.designsystem.TaskChainDesignSystem

/**
 * Renders a label with a button that opens the system sound picker and a Reset button.
 * Inputs: `label` — what the sound is for; `uri` — the picked sound, or null for the built-in sound;
 * `onPicked` — receives the new content URI, or null after Reset.
 */
@Composable
internal fun SoundPickerRow(label: String, uri: String?, onPicked: (String?) -> Unit) {
    val context = LocalContext.current
    val ui = LocalUiComponents.current
    val builtIn = stringResource(R.string.sound_built_in)
    val title = remember(uri, builtIn) {
        uri?.let { runCatching { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) }.getOrNull() }
            ?: builtIn
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onPicked(picked?.toString())
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = TaskChainDesignSystem.spacing().small))
        ui.button(
            {
                picker.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri?.let(Uri::parse)),
                )
            },
            Modifier,
            true,
        ) { Text(title) }
        if (uri != null) {
            ui.button({ onPicked(null) }, Modifier.padding(start = TaskChainDesignSystem.spacing().small), true) {
                Text(stringResource(R.string.sound_reset))
            }
        }
    }
}
