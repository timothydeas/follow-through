package com.ideasinc.followthrough.di

import android.content.Context
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.QuestionLabelDao

class AppContainer(context: Context) {
    // Exposed so multi-table operations (e.g. a data import's wipe + re-insert)
    // can run inside a single Room transaction via database.withTransaction { }.
    val database: GroundedDatabase = GroundedDatabase.getInstance(context)
    val goalDao: GoalDao = database.goalDao()
    val checkInDao: CheckInDao = database.checkInDao()
    val questionLabelDao: QuestionLabelDao = database.questionLabelDao()
}
