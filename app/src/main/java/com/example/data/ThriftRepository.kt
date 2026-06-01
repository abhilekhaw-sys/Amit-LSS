package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ThriftRepository(private val dao: ThriftDao) {

    val allSavings: Flow<List<SavingsContribution>> = dao.getAllSavings()
    val allLoans: Flow<List<Loan>> = dao.getAllLoans()
    val allRepayments: Flow<List<LoanRepayment>> = dao.getAllRepayments()

    suspend fun insertSavings(contribution: SavingsContribution) {
        dao.insertSavings(contribution)
    }

    suspend fun deleteSavings(id: Int) {
        dao.deleteSavingsById(id)
    }

    suspend fun insertLoan(loan: Loan): Long {
        return dao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: Loan) {
        dao.updateLoan(loan)
    }

    suspend fun getLoanById(loanId: Int): Loan? {
        return dao.getLoanById(loanId)
    }

    suspend fun insertRepayment(repayment: LoanRepayment) {
        // Insert the repayment first
        dao.insertRepayment(repayment)

        // Adjust or verify if the Loan is fully paid
        val loan = dao.getLoanById(repayment.loanId)
        if (loan != null && loan.status == "ACTIVE") {
            // Fetch all repayments for this loan
            val repayments = dao.getRepaymentsForLoan(repayment.loanId).first()
            val totalPrincipalRepaid = repayments.sumOf { it.principalPaid }
            if (totalPrincipalRepaid >= loan.loanAmount) {
                dao.updateLoan(loan.copy(status = "PAID"))
            }
        }
    }

    suspend fun deleteRepayment(id: Int) {
        dao.deleteRepaymentById(id)
    }
}
