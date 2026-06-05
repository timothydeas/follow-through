package com.ideasinc.followthrough.ui.settings

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ideasinc.followthrough.data.backup.BackupData
import com.ideasinc.followthrough.data.backup.BackupFormatException
import com.ideasinc.followthrough.data.backup.BackupManager
import com.ideasinc.followthrough.di.AppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reusable, self-contained export/import controls. Each owns its Storage Access
// Framework launcher and its dialogs (unencrypted warning, replace confirmation,
// error), and renders the caller's [content] slot with a `launch` lambda so the
// visual (a Settings row, an empty-state button, …) is the caller's choice.
// Nothing here requires a storage permission and nothing leaves the device.

private const val TAG = "BackupUi"

private val backupFileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

/**
 * Export control. Tapping [content]'s launch first warns that the file is
 * unencrypted, then opens ACTION_CREATE_DOCUMENT so the user chooses where to
 * save a single JSON backup. [onResult] reports success/failure for a snackbar.
 */
@Composable
fun ExportData(
    container: AppContainer,
    onResult: (success: Boolean, message: String) -> Unit = { _, _ -> },
    content: @Composable (launch: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUnencryptedWarning by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                BackupManager.export(context, container, uri)
                onResult(true, "Your data was exported.")
            } catch (e: Exception) {
                Log.e(TAG, "Export write failed", e)
                onResult(false, "Export failed. Please try again.")
            }
        }
    }

    if (showUnencryptedWarning) {
        AlertDialog(
            onDismissRequest = { showUnencryptedWarning = false },
            title = { Text("Export your data", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "This saves all your goals, plans, check-ins, reminders and " +
                        "settings to a single file on your device. The file is NOT " +
                        "encrypted — anyone who opens it can read it, so keep it " +
                        "somewhere private. Nothing is uploaded.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnencryptedWarning = false
                    val name = "followthru-backup-${backupFileDateFormat.format(Date())}.json"
                    try {
                        createLauncher.launch(name)
                    } catch (e: Exception) {
                        Log.e(TAG, "CreateDocument launch failed", e)
                        onResult(false, "Couldn't open the file picker.")
                    }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showUnencryptedWarning = false }) { Text("Cancel") }
            }
        )
    }

    content { showUnencryptedWarning = true }
}

/**
 * Import control. Tapping [content]'s launch opens ACTION_OPEN_DOCUMENT to pick a
 * backup file. The file is read, validated and version-checked; if the app
 * already has data the user is asked to confirm a full replace before anything
 * is written. Corrupt/incompatible files surface a clear message and never touch
 * existing data. [onImported] fires after a successful replace.
 */
@Composable
fun ImportData(
    container: AppContainer,
    onImported: (message: String) -> Unit = {},
    content: @Composable (launch: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingReplace by remember { mutableStateOf<BackupData?>(null) }

    fun apply(data: BackupData) {
        scope.launch {
            try {
                BackupManager.applyImport(context, container, data)
                onImported("Your data was imported.")
            } catch (e: Exception) {
                Log.e(TAG, "applyImport failed", e)
                errorMessage = "Import failed. Your existing data was not changed."
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val data = try {
                BackupManager.readAndDecode(context, uri)
            } catch (e: BackupFormatException) {
                Log.e(TAG, "readAndDecode rejected file", e)
                errorMessage = e.message ?: "This file isn't a valid FollowThru backup."
                return@launch
            } catch (e: Exception) {
                Log.e(TAG, "readAndDecode failed", e)
                errorMessage = "Couldn't read that file. Please pick a FollowThru backup."
                return@launch
            }
            if (BackupManager.hasExistingData(container)) {
                pendingReplace = data
            } else {
                apply(data)
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Couldn't import", style = MaterialTheme.typography.titleMedium) },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }

    pendingReplace?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingReplace = null },
            title = { Text("Replace your data?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "This will replace your current data — all goals, plans, " +
                        "check-ins, reminders and settings — with the contents of " +
                        "this backup. This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingReplace = null
                    apply(data)
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReplace = null }) { Text("Cancel") }
            }
        )
    }

    content {
        try {
            // Use a single permissive "*/*" filter rather than an explicit
            // MIME-type set. ACTION_OPEN_DOCUMENT resolves against the *full*
            // EXTRA_MIME_TYPES set, and a backup file's resolved type is
            // unreliable: depending on the device/provider a file written by us
            // as "application/json" can come back typed "text/plain",
            // "application/octet-stream", or even an unrelated guess — and on
            // some devices an OPEN_DOCUMENT intent whose type set doesn't match
            // any servable type throws ActivityNotFoundException, which this
            // catch was silently turning into "Couldn't open the file picker."
            // "*/*" always resolves; we validate the file's *contents* in
            // BackupManager.readAndDecode regardless of its MIME label.
            openLauncher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            Log.e(TAG, "OpenDocument launch failed", e)
            errorMessage = "Couldn't open the file picker."
        }
    }
}
