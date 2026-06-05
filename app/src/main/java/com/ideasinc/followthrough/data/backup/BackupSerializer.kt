package com.ideasinc.followthrough.data.backup

import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.QuestionLabel
import com.ideasinc.followthrough.notifications.GoalReminder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// ─── Format constants ──────────────────────────────────────────────────────

/** Marker stored at the top of every export so an import can recognise its own
 *  files and reject anything else. */
const val BACKUP_FORMAT = "followthru-export"

/**
 * Version of the export *file* shape (independent of the Room DB version). Bump
 * this whenever the on-disk JSON layout changes in a way [BackupSerializer.decode]
 * needs to branch on. The decoder reads older versions forward to the current
 * schema; it never assumes a field exists.
 */
const val BACKUP_SCHEMA_VERSION = 1

/** The Room DB version this build writes/reads. Stamped into exports for
 *  diagnostics and forward-migration decisions. */
const val BACKUP_DB_VERSION = 27

// ─── In-memory model ───────────────────────────────────────────────────────

/** A single goal's reminder paired with the goal it belongs to. */
data class GoalReminderEntry(val goalId: String, val reminder: GoalReminder)

/** App-level settings worth carrying across a restore. */
data class BackupSettings(
    val globalReminderEnabled: Boolean,
    val globalReminderHour: Int,
    val globalReminderMinute: Int,
    val globalReminderDays: Set<Int>,
    val themeMode: String,
    val biometricEnabled: Boolean
)

/** Everything a backup file holds, parsed into memory. */
data class BackupData(
    val schemaVersion: Int,
    val appVersionName: String,
    val appVersionCode: Int,
    val dbVersion: Int,
    val exportedAt: Long,
    val goals: List<Goal>,
    val checkIns: List<CheckIn>,
    val questionLabels: List<QuestionLabel>,
    val goalReminders: List<GoalReminderEntry>,
    val settings: BackupSettings
)

/** Thrown when a file isn't a FollowThru backup or is too corrupt to read. */
class BackupFormatException(message: String) : Exception(message)

// ─── Serialization ─────────────────────────────────────────────────────────

/**
 * Pure (no Android, no IO) JSON encode/decode for [BackupData]. Uses org.json,
 * which ships with Android — no extra dependency. The decoder is deliberately
 * tolerant: every field is read with a default so a backup written by an older
 * (or slightly newer) build still imports instead of crashing.
 */
object BackupSerializer {

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("format", BACKUP_FORMAT)
        root.put("schemaVersion", data.schemaVersion)
        root.put("appVersionName", data.appVersionName)
        root.put("appVersionCode", data.appVersionCode)
        root.put("dbVersion", data.dbVersion)
        root.put("exportedAt", data.exportedAt)

        root.put("goals", JSONArray().apply { data.goals.forEach { put(goalToJson(it)) } })
        root.put("checkIns", JSONArray().apply { data.checkIns.forEach { put(checkInToJson(it)) } })
        root.put(
            "questionLabels",
            JSONArray().apply { data.questionLabels.forEach { put(labelToJson(it)) } }
        )
        root.put(
            "goalReminders",
            JSONArray().apply { data.goalReminders.forEach { put(reminderToJson(it)) } }
        )
        root.put("settings", settingsToJson(data.settings))

        return root.toString(2)
    }

    fun decode(raw: String): BackupData {
        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw BackupFormatException("This file isn't a valid FollowThru backup.")
        }

        if (root.optString("format") != BACKUP_FORMAT) {
            throw BackupFormatException("This file isn't a FollowThru backup.")
        }

        val schemaVersion = root.optInt("schemaVersion", 1)

        return try {
            BackupData(
                schemaVersion = schemaVersion,
                appVersionName = root.optString("appVersionName", ""),
                appVersionCode = root.optInt("appVersionCode", 0),
                dbVersion = root.optInt("dbVersion", 0),
                exportedAt = root.optLong("exportedAt", 0L),
                goals = root.optJSONArray("goals").mapObjects { goalFromJson(it) },
                checkIns = root.optJSONArray("checkIns").mapObjects { checkInFromJson(it) },
                questionLabels = root.optJSONArray("questionLabels").mapObjects { labelFromJson(it) },
                goalReminders = root.optJSONArray("goalReminders").mapObjects { reminderFromJson(it) },
                settings = settingsFromJson(root.optJSONObject("settings"))
            )
        } catch (e: JSONException) {
            throw BackupFormatException("This backup is incomplete or corrupted.")
        }
    }

    // ── Goal ──

    private fun goalToJson(g: Goal) = JSONObject().apply {
        put("id", g.id)
        put("title", g.title)
        putNullable("accountableTo", g.accountableTo)
        put("createdAt", g.createdAt)
        put("updatedAt", g.updatedAt)
        if (g.priority != null) put("priority", g.priority) else put("priority", JSONObject.NULL)
        put("followedThrough", g.followedThrough)
        if (g.followedThroughAt != null) put("followedThroughAt", g.followedThroughAt)
        else put("followedThroughAt", JSONObject.NULL)
    }

    private fun goalFromJson(o: JSONObject) = Goal(
        id = o.getString("id"),
        title = o.optString("title", ""),
        accountableTo = o.optNullableString("accountableTo"),
        createdAt = o.optLong("createdAt", 0L),
        updatedAt = o.optLong("updatedAt", 0L),
        priority = o.optNullableInt("priority"),
        followedThrough = o.optBoolean("followedThrough", false),
        followedThroughAt = o.optNullableLong("followedThroughAt")
    )

    // ── CheckIn ──

    private fun checkInToJson(c: CheckIn) = JSONObject().apply {
        put("id", c.id)
        put("goalId", c.goalId)
        put("goalOrChange", c.goalOrChange)
        putNullable("madeProgress", c.madeProgress)
        putNullable("avoiding", c.avoiding)
        putNullable("confidence", c.confidence)
        putNullable("competingPriority", c.competingPriority)
        putNullable("implementationIntention", c.implementationIntention)
        putNullable("accountability", c.accountability)
        put("createdAt", c.createdAt)
        put("updatedAt", c.updatedAt)
    }

    private fun checkInFromJson(o: JSONObject) = CheckIn(
        id = o.getString("id"),
        goalId = o.getString("goalId"),
        goalOrChange = o.optString("goalOrChange", ""),
        madeProgress = o.optNullableString("madeProgress"),
        avoiding = o.optNullableString("avoiding"),
        confidence = o.optNullableString("confidence"),
        competingPriority = o.optNullableString("competingPriority"),
        implementationIntention = o.optNullableString("implementationIntention"),
        accountability = o.optNullableString("accountability"),
        createdAt = o.optLong("createdAt", 0L),
        updatedAt = o.optLong("updatedAt", 0L)
    )

    // ── QuestionLabel ──

    private fun labelToJson(q: QuestionLabel) = JSONObject().apply {
        put("id", q.id)
        put("questionKey", q.questionKey)
        put("customLabel", q.customLabel)
        putNullable("customPlaceholder", q.customPlaceholder)
        put("isEnabled", q.isEnabled)
    }

    private fun labelFromJson(o: JSONObject) = QuestionLabel(
        id = o.getString("id"),
        questionKey = o.getString("questionKey"),
        customLabel = o.optString("customLabel", ""),
        customPlaceholder = o.optNullableString("customPlaceholder"),
        isEnabled = o.optBoolean("isEnabled", true)
    )

    // ── Per-goal reminder ──

    private fun reminderToJson(e: GoalReminderEntry) = JSONObject().apply {
        put("goalId", e.goalId)
        put("enabled", e.reminder.enabled)
        put("hour", e.reminder.hour)
        put("minute", e.reminder.minute)
        put("days", JSONArray().apply { e.reminder.days.forEach { put(it) } })
        put("body", e.reminder.body)
    }

    private fun reminderFromJson(o: JSONObject) = GoalReminderEntry(
        goalId = o.getString("goalId"),
        reminder = GoalReminder(
            enabled = o.optBoolean("enabled", false),
            hour = o.optInt("hour", 9),
            minute = o.optInt("minute", 0),
            days = o.optJSONArray("days").toIntSet(),
            body = o.optString("body", "")
        )
    )

    // ── Settings ──

    private fun settingsToJson(s: BackupSettings) = JSONObject().apply {
        put("globalReminderEnabled", s.globalReminderEnabled)
        put("globalReminderHour", s.globalReminderHour)
        put("globalReminderMinute", s.globalReminderMinute)
        put("globalReminderDays", JSONArray().apply { s.globalReminderDays.forEach { put(it) } })
        put("themeMode", s.themeMode)
        put("biometricEnabled", s.biometricEnabled)
    }

    private fun settingsFromJson(o: JSONObject?): BackupSettings {
        if (o == null) {
            return BackupSettings(
                globalReminderEnabled = false,
                globalReminderHour = 9,
                globalReminderMinute = 0,
                globalReminderDays = emptySet(),
                themeMode = "SYSTEM",
                biometricEnabled = false
            )
        }
        return BackupSettings(
            globalReminderEnabled = o.optBoolean("globalReminderEnabled", false),
            globalReminderHour = o.optInt("globalReminderHour", 9),
            globalReminderMinute = o.optInt("globalReminderMinute", 0),
            globalReminderDays = o.optJSONArray("globalReminderDays").toIntSet(),
            themeMode = o.optString("themeMode", "SYSTEM"),
            biometricEnabled = o.optBoolean("biometricEnabled", false)
        )
    }
}

// ─── org.json helpers ──────────────────────────────────────────────────────

private fun JSONObject.putNullable(key: String, value: String?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.optNullableString(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONObject.optNullableInt(key: String): Int? =
    if (has(key) && !isNull(key)) getInt(key) else null

private fun JSONObject.optNullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) getLong(key) else null

private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) out.add(transform(getJSONObject(i)))
    return out
}

private fun JSONArray?.toIntSet(): Set<Int> {
    if (this == null) return emptySet()
    val out = HashSet<Int>(length())
    for (i in 0 until length()) out.add(getInt(i))
    return out
}
