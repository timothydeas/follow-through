package com.example.grounded.di

import android.content.Context
import com.example.grounded.data.CheckInDao
import com.example.grounded.data.FollowThroughDao
import com.example.grounded.data.GoalDao
import com.example.grounded.data.GroundedDatabase
import com.example.grounded.data.NoteDao
import com.example.grounded.data.QuestionLabelDao

class AppContainer(context: Context) {
    private val database: GroundedDatabase = GroundedDatabase.getInstance(context)
    val noteDao: NoteDao = database.noteDao()
    val followThroughDao: FollowThroughDao = database.followThroughDao()
    val goalDao: GoalDao = database.goalDao()
    val checkInDao: CheckInDao = database.checkInDao()
    val questionLabelDao: QuestionLabelDao = database.questionLabelDao()
}
