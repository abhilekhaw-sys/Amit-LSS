package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_repayments")
data class LoanRepayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanId: Int,
    val amountPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val datePaid: Long = System.currentTimeMillis(),
    val paymentMonth: String, // e.g., "June 2026"
    val notes: String = ""
)
