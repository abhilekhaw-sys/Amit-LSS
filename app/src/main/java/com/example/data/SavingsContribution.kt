package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_contributions")
data class SavingsContribution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val monthYear: String, // e.g., "June 2026"
    val notes: String = ""
)
