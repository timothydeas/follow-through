package com.ideasinc.followthrough.di

import android.content.Context
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEventDao

class AppContainer(context: Context) {
    private val database: GroundedDatabase = GroundedDatabase.getInstance(context)
    val goalDao: GoalDao = database.goalDao()
    val reminderDao: ReminderDao = database.reminderDao()
    val reminderEventDao: ReminderEventDao = database.reminderEventDao()
}
