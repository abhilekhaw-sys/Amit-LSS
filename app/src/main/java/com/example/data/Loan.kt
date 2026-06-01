package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanAmount: Double,
    val interestRate: Double, // e.g. 6.0 representing 6% per annum
    val monthlyInstallment: Double,
    val disbursedDate: Long = System.currentTimeMillis(),
    val purpose: String,
    val status: String = "ACTIVE" // "ACTIVE" or "PAID"
)
