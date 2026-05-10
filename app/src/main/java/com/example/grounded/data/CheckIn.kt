package com.example.grounded.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_ins",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CheckIn(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val goalOrChange: String = "",
    val madeProgress: String? = null,
    val avoiding: String? = null,
    val confidence: String? = null,
    val competingPriority: String? = null,
    val implementationIntention: String? = null,
    val accountability: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
