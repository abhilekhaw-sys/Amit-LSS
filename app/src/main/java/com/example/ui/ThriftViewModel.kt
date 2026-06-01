package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Loan
import com.example.data.LoanRepayment
import com.example.data.SavingsContribution
import com.example.data.ThriftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ThriftUiState(
    val savingsList: List<SavingsContribution> = emptyList(),
    val loansList: List<Loan> = emptyList(),
    val repaymentsList: List<LoanRepayment> = emptyList(),
    // Summary Aggregates
    val totalSavings: Double = 0.0,
    val initialThriftBalance: Double = 1500.0, // Custom basic stake or starting share capital
    val currentShareCapital: Double = 2500.0, // Share Capital stake
    val totalActiveLoansDisbursed: Double = 0.0,
    val outstandingLoanBalance: Double = 0.0,
    val activeLoans: List<Loan> = emptyList(),
    val isDbEmpty: Boolean = false
)

class ThriftViewModel(private val repository: ThriftRepository) : ViewModel() {

    // Combine database streams into a single UI state
    val uiState: StateFlow<ThriftUiState> = combine(
        repository.allSavings,
        repository.allLoans,
        repository.allRepayments
    ) { savings, loans, repayments ->
        
        val totalSavingsSum = savings.sumOf { it.amount }
        val activeLoans = loans.filter { it.status == "ACTIVE" }
        
        // Calculate outstanding balance per loan
        // Outstanding = Loan Amount - Sum(principal paid for that loan)
        val outstandingAmount = activeLoans.sumOf { loan ->
            val repaidForThisLoan = repayments
                .filter { it.loanId == loan.id }
                .sumOf { it.principalPaid }
            (loan.loanAmount - repaidForThisLoan).coerceAtLeast(0.0)
        }

        val totalActiveDisbursed = activeLoans.sumOf { it.loanAmount }

        ThriftUiState(
            savingsList = savings,
            loansList = loans,
            repaymentsList = repayments,
            totalSavings = totalSavingsSum,
            totalActiveLoansDisbursed = totalActiveDisbursed,
            outstandingLoanBalance = outstandingAmount,
            activeLoans = activeLoans,
            isDbEmpty = savings.isEmpty() && loans.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThriftUiState()
    )

    init {
        // Prepopulate with gorgeous sample data if DB is empty
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isDbEmpty) {
                    prepopulateSampleData()
                }
            }
        }
    }

    private suspend fun prepopulateSampleData() {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMMM yyyy", Locale.US)

        // Add savings contributions for the last 4 months
        for (i in 3 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val monthYear = format.format(cal.time)
            repository.insertSavings(
                SavingsContribution(
                    amount = 250.0 + (i * 50.0), // $250 to $450
                    date = cal.timeInMillis,
                    monthYear = monthYear,
                    notes = if (i == 3) "First ledger entry" else "Monthly compulsory contribution"
                )
            )
        }

        // Add an active loan
        val loanCal = Calendar.getInstance()
        loanCal.add(Calendar.MONTH, -2) // Taken 2 months ago
        val loanId = repository.insertLoan(
            Loan(
                loanAmount = 5000.0,
                interestRate = 6.0, // 6% flat rate per annum
                monthlyInstallment = 441.67, // Calculated EMI
                disbursedDate = loanCal.timeInMillis,
                purpose = "Home Renovation Plan",
                status = "ACTIVE"
            )
        )

        // Add 2 repayments for that loan
        val repCal1 = Calendar.getInstance()
        repCal1.add(Calendar.MONTH, -1) // 1st repayment
        repCal1.set(Calendar.DAY_OF_MONTH, 15)
        repository.insertRepayment(
            LoanRepayment(
                loanId = loanId.toInt(),
                amountPaid = 441.67,
                principalPaid = 416.67,
                interestPaid = 25.0, // 5000 * 6%/12
                datePaid = repCal1.timeInMillis,
                paymentMonth = format.format(repCal1.time),
                notes = "Instalment #01"
            )
        )

        val repCal2 = Calendar.getInstance()
        repCal2.set(Calendar.DAY_OF_MONTH, 15) // 2nd repayment (current month)
        repository.insertRepayment(
            LoanRepayment(
                loanId = loanId.toInt(),
                amountPaid = 441.67,
                principalPaid = 418.75,
                interestPaid = 22.92, // (5000 - 416.67) * 6%/12
                datePaid = repCal2.timeInMillis,
                paymentMonth = format.format(repCal2.time),
                notes = "Instalment #02"
            )
        )
    }

    // Interactive Actions

    fun addSavings(amount: Double, notes: String) {
        viewModelScope.launch {
            val format = SimpleDateFormat("MMMM yyyy", Locale.US)
            val monthYear = format.format(Calendar.getInstance().time)
            repository.insertSavings(
                SavingsContribution(
                    amount = amount,
                    monthYear = monthYear,
                    notes = notes.ifBlank { "Monthly Contribution" }
                )
            )
        }
    }

    fun deleteSavings(id: Int) {
        viewModelScope.launch {
            repository.deleteSavings(id)
        }
    }

    fun applyForLoan(amount: Double, interestAnnualRate: Double, months: Int, purpose: String) {
        viewModelScope.launch {
            // Flat rate EMI calculation: Total Payback = Principal + (Principal * Rate * (Months/12)/100)
            // Monthly EMI = Total Payback / Months
            val totalInterest = amount * (interestAnnualRate / 100.0) * (months / 12.0)
            val totalPayback = amount + totalInterest
            val monthlyEmi = totalPayback / months

            repository.insertLoan(
                Loan(
                    loanAmount = amount,
                    interestRate = interestAnnualRate,
                    monthlyInstallment = Math.round(monthlyEmi * 100.0) / 100.0,
                    purpose = purpose.ifBlank { "Personal Loan" }
                )
            )
        }
    }

    fun repayLoan(loanId: Int, amountPaid: Double, notes: String) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId) ?: return@launch
            
            // Fetch previous repayments to find outstanding principal
            val repayments = repository.allRepayments.first().filter { it.loanId == loanId }
            val totalPrincipalPaid = repayments.sumOf { it.principalPaid }
            val outstandingPrincipal = (loan.loanAmount - totalPrincipalPaid).coerceAtLeast(0.0)

            // Monthly interest calculation (reducing rate based on outstanding principal)
            val monthlyRate = (loan.interestRate / 12.0) / 100.0
            val calculatedInterest = outstandingPrincipal * monthlyRate
            
            // Clean division
            val interestPaid = minOf(calculatedInterest, amountPaid)
            val principalPaid = minOf(amountPaid - interestPaid, outstandingPrincipal)

            val format = SimpleDateFormat("MMMM yyyy", Locale.US)
            val paymentMonth = format.format(Calendar.getInstance().time)

            repository.insertRepayment(
                LoanRepayment(
                    loanId = loanId,
                    amountPaid = amountPaid,
                    principalPaid = Math.round(principalPaid * 100.0) / 100.0,
                    interestPaid = Math.round(interestPaid * 100.0) / 100.0,
                    paymentMonth = paymentMonth,
                    notes = notes.ifBlank { "Manual repayment" }
                )
            )
        }
    }

    fun deleteRepayment(id: Int) {
        viewModelScope.launch {
            repository.deleteRepayment(id)
        }
    }

    companion object {
        fun Factory(repository: ThriftRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThriftViewModel(repository) as T
            }
        }
    }
}
