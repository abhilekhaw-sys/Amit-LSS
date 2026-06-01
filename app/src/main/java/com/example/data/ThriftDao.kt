package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThriftDao {
    // Savings Operations
    @Query("SELECT * FROM savings_contributions ORDER BY date DESC")
    fun getAllSavings(): Flow<List<SavingsContribution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavings(contribution: SavingsContribution)

    @Query("DELETE FROM savings_contributions WHERE id = :id")
    suspend fun deleteSavingsById(id: Int)

    // Loan Operations
    @Query("SELECT * FROM loans ORDER BY disbursedDate DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Int): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    // Repayment Operations
    @Query("SELECT * FROM loan_repayments ORDER BY datePaid DESC")
    fun getAllRepayments(): Flow<List<LoanRepayment>>

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY datePaid DESC")
    fun getRepaymentsForLoan(loanId: Int): Flow<List<LoanRepayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepayment)

    @Query("DELETE FROM loan_repayments WHERE id = :id")
    suspend fun deleteRepaymentById(id: Int)
}
