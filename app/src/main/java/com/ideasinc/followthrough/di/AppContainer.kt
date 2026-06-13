package com.ideasinc.followthrough.di

import android.content.Context
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.GoalContentDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.PaletteDao
import com.ideasinc.followthrough.data.QuestionLabelDao
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEventDao

class AppContainer(context: Context) {
    private val database: GroundedDatabase = GroundedDatabase.getInstance(context)
    val goalDao: GoalDao = database.goalDao()
    val checkInDao: CheckInDao = database.checkInDao()
    val questionLabelDao: QuestionLabelDao = database.questionLabelDao()
    val reminderDao: ReminderDao = database.reminderDao()
    val reminderEventDao: ReminderEventDao = database.reminderEventDao()
    val paletteDao: PaletteDao = database.paletteDao()
    val goalContentDao: GoalContentDao = database.goalContentDao()
}
