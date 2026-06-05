package com.ideasinc.followthrough.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ideasinc.followthrough.BuildConfig
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.PREFS_NAME
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.KEY_REMINDERS_ENABLED
import com.ideasinc.followthrough.notifications.KEY_REMINDER_DAYS
import com.ideasinc.followthrough.notifications.KEY_REMINDER_HOUR
import com.ideasinc.followthrough.notifications.KEY_REMINDER_MINUTE
import com.ideasinc.followthrough.notifications.PREFS_REMINDERS
import com.ideasinc.followthrough.notifications.ReminderScheduler
import com.ideasinc.followthrough.ui.theme.ThemeMode
import com.ideasinc.followthrough.ui.theme.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Orchestrates user-controlled, fully-local export and import of all FollowThru
 * data. Nothing here touches the network: export writes to a Storage Access
 * Framework [Uri] the user picked, and import reads from one. The actual JSON
 * shape lives in [BackupSerializer]; this object only gathers/applies the data.
 *
 * Import is *replace*, not merge: [applyImport] wipes existing goals, check-ins,
 * question customizations and reminders before writing the backup's, then
 * re-schedules every reminder through the live schedulers (alarms aren't stored
 * in the DB, so they must be re-armed).
 */
object BackupManager {

    /** Gathers every piece of restorable state into an in-memory [BackupData]. */
    suspend fun collect(context: Context, container: AppContainer): BackupData {
        val appContext = context.applicationContext

        val goals = container.goalDao.getAllGoals().first()
        val checkIns = container.checkInDao.getAllCheckIns().first()
        val labels = container.questionLabelDao.getAllLabels().first()

        // One reminder entry per goal that has a persisted reminder.
        val reminders = goals.mapNotNull { goal ->
            GoalReminderScheduler.read(appContext, goal.id)
                ?.let { GoalReminderEntry(goal.id, it) }
        }

        val remindersPrefs =
            appContext.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        val globalDays = (remindersPrefs.getStringSet(KEY_REMINDER_DAYS, emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        val groundedPrefs =
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val settings = BackupSettings(
            globalReminderEnabled = remindersPrefs.getBoolean(KEY_REMINDERS_ENABLED, false),
            globalReminderHour = remindersPrefs.getInt(KEY_REMINDER_HOUR, 9),
            globalReminderMinute = remindersPrefs.getInt(KEY_REMINDER_MINUTE, 0),
            globalReminderDays = globalDays,
            themeMode = ThemePreferences.mode.value.name,
            biometricEnabled = groundedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        )

        return BackupData(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            dbVersion = BACKUP_DB_VERSION,
            exportedAt = System.currentTimeMillis(),
            goals = goals,
            checkIns = checkIns,
            questionLabels = labels,
            goalReminders = reminders,
            settings = settings
        )
    }

    /** Serializes all data and writes it to [uri]. Runs off the main thread. */
    suspend fun export(context: Context, container: AppContainer, uri: Uri) {
        val data = collect(context, container)
        val json = BackupSerializer.encode(data)
        withContext(Dispatchers.IO) {
            val resolver = context.applicationContext.contentResolver
            val stream = resolver.openOutputStream(uri, "wt")
                ?: throw BackupFormatException("Couldn't open the chosen file for writing.")
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }
    }

    /** Reads [uri], parses and validates it. Throws [BackupFormatException] on a
     *  file that isn't a (readable) FollowThru backup. */
    suspend fun readAndDecode(context: Context, uri: Uri): BackupData =
        withContext(Dispatchers.IO) {
            val resolver = context.applicationContext.contentResolver
            val raw = resolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw BackupFormatException("Couldn't open the chosen file.")
            BackupSerializer.decode(raw)
        }

    /** True if the app already holds user data that an import would replace. */
    suspend fun hasExistingData(container: AppContainer): Boolean {
        if (container.goalDao.getAllGoals().first().isNotEmpty()) return true
        if (container.checkInDao.getAllCheckIns().first().isNotEmpty()) return true
        return false
    }

    /**
     * Replaces all current data with [data] and re-arms every reminder. Existing
     * goals/check-ins/labels/reminders are cleared first so this is a true
     * replace, never a merge. Reminders and the global reminder are re-scheduled
     * through their live schedulers because alarms live in AlarmManager, not the
     * DB.
     */
    suspend fun applyImport(context: Context, container: AppContainer, data: BackupData) {
        val appContext = context.applicationContext

        // 1. Replace database tables atomically. The wipe and every re-insert run
        //    inside a single Room transaction: if any step throws, the whole
        //    transaction rolls back and the user's existing data is left intact
        //    (nothing half-deleted, nothing half-written). Clear children before
        //    parents so the wipe doesn't depend on foreign-key pragma state;
        //    insert parents before children so each check-in's goal already
        //    exists. Reminders/settings are deliberately re-armed only *after*
        //    this commits — see step 2 — so a rolled-back import touches nothing.
        container.database.withTransaction {
            container.checkInDao.deleteAll()
            container.goalDao.deleteAll()
            container.questionLabelDao.deleteAll()

            for (goal in data.goals) container.goalDao.insertGoal(goal)
            for (checkIn in data.checkIns) container.checkInDao.insertCheckIn(checkIn)
            for (label in data.questionLabels) container.questionLabelDao.insertLabel(label)
        }

        // 2. Replace per-goal reminders, then re-schedule each through the
        //    existing scheduler (no-op alarm-wise if permission is missing).
        GoalReminderScheduler.clearAll(appContext)
        for (entry in data.goalReminders) {
            GoalReminderScheduler.restore(appContext, entry.goalId, entry.reminder)
        }

        // 3. Restore settings.
        val remindersPrefs =
            appContext.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        remindersPrefs.edit()
            .putBoolean(KEY_REMINDERS_ENABLED, data.settings.globalReminderEnabled)
            .putInt(KEY_REMINDER_HOUR, data.settings.globalReminderHour)
            .putInt(KEY_REMINDER_MINUTE, data.settings.globalReminderMinute)
            .putStringSet(
                KEY_REMINDER_DAYS,
                data.settings.globalReminderDays.map { it.toString() }.toSet()
            )
            .apply()
        // Re-arm the global reminder from the freshly-written prefs.
        ReminderScheduler.rescheduleAllFromPrefs(appContext)

        val groundedPrefs =
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        groundedPrefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, data.settings.biometricEnabled)
            .apply()

        runCatching { ThemeMode.valueOf(data.settings.themeMode) }
            .getOrNull()
            ?.let { ThemePreferences.setMode(appContext, it) }
    }
}
