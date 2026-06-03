package com.ideasinc.followthrough.di

import android.content.Context
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.FollowThroughDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.NoteDao
import com.ideasinc.followthrough.data.QuestionLabelDao

class AppContainer(context: Context) {
    private val database: GroundedDatabase = GroundedDatabase.getInstance(context)
    val noteDao: NoteDao = database.noteDao()
    val followThroughDao: FollowThroughDao = database.followThroughDao()
    val goalDao: GoalDao = database.goalDao()
    val checkInDao: CheckInDao = database.checkInDao()
    val questionLabelDao: QuestionLabelDao = database.questionLabelDao()
}
