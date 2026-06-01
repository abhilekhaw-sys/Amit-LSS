package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Loan
import com.example.data.LoanRepayment
import com.example.data.SavingsContribution
import com.example.data.ThriftRepository
import com.example.ui.ThriftUiState
import com.example.ui.ThriftViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = ThriftRepository(database.thriftDao())
        
        setContent {
            MyApplicationTheme {
                // Handle Window Insets properly
                val safePadding = WindowInsets.safeDrawing.asPaddingValues()
                
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    ThriftApp(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ThriftApp(
    repository: ThriftRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: ThriftViewModel = viewModel(
        factory = ThriftViewModel.Factory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // UI state for Dialogs
    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var showApplyLoanDialog by remember { mutableStateOf(false) }
    var showRepayLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanForRepayment by remember { mutableStateOf<Loan?>(null) }
    
    // Tab indicator
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Savings", "My Loans", "Audit Ledger")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("thrift_dashboard")
    ) {
        // App Custom Banner & Header
        HeaderProfileSection()

        // Cooperative Wealth Board Overview
        WealthBoardCard(
            uiState = uiState,
            onAddSavingsClick = { showAddSavingsDialog = true },
            onApplyLoanClick = { showApplyLoanDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs using standard M3 TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content Window with Staggered Fade effect
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> SavingsTabContent(
                    uiState = uiState,
                    onDeleteSavings = { viewModel.deleteSavings(it) }
                )
                1 -> LoansTabContent(
                    uiState = uiState,
                    onMakeRepayment = { loan ->
                        selectedLoanForRepayment = loan
                        showRepayLoanDialog = true
                    },
                    onDeleteRepayment = { viewModel.deleteRepayment(it) }
                )
                2 -> AuditLedgerTabContent(uiState = uiState)
            }
        }

        // Action Floating Action Button at bottom center/right
        BottomActionBar(
            onAddSavings = { showAddSavingsDialog = true },
            onAddLoan = { showApplyLoanDialog = true },
            onRepay = {
                selectedLoanForRepayment = null
                showRepayLoanDialog = true
            },
            hasActiveLoans = uiState.activeLoans.isNotEmpty()
        )
    }

    // Modal Dialogs Section

    if (showAddSavingsDialog) {
        AddSavingsDialog(
            onDismiss = { showAddSavingsDialog = false },
            onConfirm = { amount, notes ->
                viewModel.addSavings(amount, notes)
                showAddSavingsDialog = false
            }
        )
    }

    if (showApplyLoanDialog) {
        ApplyLoanDialog(
            onDismiss = { showApplyLoanDialog = false },
            onConfirm = { amount, rate, tenor, purpose ->
                viewModel.applyForLoan(amount, rate, tenor, purpose)
                showApplyLoanDialog = false
            }
        )
    }

    if (showRepayLoanDialog) {
        RepayLoanDialog(
            activeLoans = uiState.activeLoans,
            preselectedLoan = selectedLoanForRepayment,
            onDismiss = { showRepayLoanDialog = false },
            onConfirm = { loanId, amount, notes ->
                viewModel.repayLoan(loanId, amount, notes)
                showRepayLoanDialog = false
            }
        )
    }
}

// Sub-Composables & Clean Layouts

@Composable
fun HeaderProfileSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD3E4FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Society Logo",
                    tint = Color(0xFF001D36),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Amit Cooperative Thrift & Credit Society",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191C1E),
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }
        
        // Beautiful initials Avatar representing personal member badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFD3E4FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AW",
                color = Color(0xFF001D36),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun WealthBoardCard(
    uiState: ThriftUiState,
    onAddSavingsClick: () -> Unit,
    onApplyLoanClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    val monthlyInterest = remember(uiState.loansList) {
        uiState.loansList.filter { it.status == "ACTIVE" }.sumOf {
            it.loanAmount * (it.interestRate / 100.0) / 12.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Total Savings Balance (Main Blue Card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFD3E4FF))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TOTAL SAVINGS BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = currencyFormatter.format(uiState.totalSavings),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36),
                    letterSpacing = (-0.5).sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Next Due: Jun 05",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF001D36)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF2E7D32), shape = CircleShape)
                    )
                }
            }
        }

        // 2. Multi-column grid (Outstanding Loan & Monthly Interest)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Card: Loan Due Amount
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E2EC))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Loan Due Amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A5568)
                )
                
                Text(
                    text = currencyFormatter.format(uiState.outstandingLoanBalance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            // Right Card: Monthly Interest
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF2E0FF))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Monthly Interest",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7E57C2)
                )
                
                Text(
                    text = currencyFormatter.format(monthlyInterest),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5E35B1)
                )
            }
        }
    }
}

@Composable
fun SavingsTabContent(
    uiState: ThriftUiState,
    onDeleteSavings: (Int) -> Unit
) {
    if (uiState.savingsList.isEmpty()) {
        EmptyBoxState(
            title = "No savings recorded yet",
            subtitle = "Start adding monthly contributions to see compound growth and boost your borrowing capacity!"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Savings & Monthly Deposits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(uiState.savingsList) { contribution ->
                SavingsRowCard(contribution = contribution, onDeleteClick = { onDeleteSavings(contribution.id) })
            }
        }
    }
}

@Composable
fun SavingsRowCard(
    contribution: SavingsContribution,
    onDeleteClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3C7CF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Deposit",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = contribution.monthYear,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF191C1E)
                    )
                    Text(
                        text = contribution.notes.ifBlank { "Monthly Contribution" },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormatter.format(Date(contribution.date)),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+" + currencyFormatter.format(contribution.amount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoansTabContent(
    uiState: ThriftUiState,
    onMakeRepayment: (Loan) -> Unit,
    onDeleteRepayment: (Int) -> Unit
) {
    if (uiState.loansList.isEmpty()) {
        EmptyBoxState(
            title = "No active loans",
            subtitle = "Need a credit boost? Apply for a thrift society loan with automatic EMI and low cooperative interest rates."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // First section: Active Loans
            val activeLoans = uiState.loansList.filter { it.status == "ACTIVE" }
            if (activeLoans.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Cooperative Loans",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(activeLoans) { loan ->
                    // Find repayment sum for this loan
                    val sumPaid = uiState.repaymentsList
                        .filter { it.loanId == loan.id }
                        .sumOf { it.principalPaid }
                    val outstanding = (loan.loanAmount - sumPaid).coerceAtLeast(0.0)

                    ActiveLoanProgressCard(
                        loan = loan,
                        totalPrincipalRepaid = sumPaid,
                        outstandingPrincipal = outstanding,
                        onRepayClick = { onMakeRepayment(loan) }
                    )
                }
            }

            // Second section: Repayment Transactions History
            if (uiState.repaymentsList.isNotEmpty()) {
                item {
                    Text(
                        text = "Repayment History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.repaymentsList) { repayment ->
                    // Find associated loan details
                    val associatedLoan = uiState.loansList.find { it.id == repayment.loanId }
                    RepaymentHistoryRow(
                        repayment = repayment,
                        loanPurpose = associatedLoan?.purpose ?: "General Loan",
                        onDeleteClick = { onDeleteRepayment(repayment.id) }
                    )
                }
            }

            // Third section: Paid Loans (archived)
            val paidLoans = uiState.loansList.filter { it.status == "PAID" }
            if (paidLoans.isNotEmpty()) {
                item {
                    Text(
                        text = "Fully Repaid Loans",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(paidLoans) { loan ->
                    PaidLoanCard(loan = loan)
                }
            }
        }
    }
}

@Composable
fun ActiveLoanProgressCard(
    loan: Loan,
    totalPrincipalRepaid: Double,
    outstandingPrincipal: Double,
    onRepayClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val percentPaid = if (loan.loanAmount > 0) (totalPrincipalRepaid / loan.loanAmount).coerceIn(0.0, 1.0) else 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3C7CF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = loan.purpose,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF191C1E)
                    )
                    Text(
                        text = "Interest: ${loan.interestRate}% P.A.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF991B1B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Amount Due", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        text = currencyFormatter.format(outstandingPrincipal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Monthly installment", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        text = currencyFormatter.format(loan.monthlyInstallment),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FAF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Elegant Custom Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentPaid.toFloat())
                        .fillPageSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF005FAF),
                                    Color(0xFF8C4F00)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(percentPaid * 100).toInt()}% Repaid",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF005FAF)
                )
                Text(
                    text = "Total Principal: ${currencyFormatter.format(loan.loanAmount)}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onRepayClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005FAF)
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Installment Payment",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Repay EMI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RepaymentHistoryRow(
    repayment: LoanRepayment,
    loanPurpose: String,
    onDeleteClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3C7CF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Payment Out",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Repaid to: $loanPurpose",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF191C1E)
                    )
                    Text(
                        text = "Principal: ${currencyFormatter.format(repayment.principalPaid)} | Interest: ${currencyFormatter.format(repayment.interestPaid)}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${repayment.paymentMonth} • ${dateFormatter.format(Date(repayment.datePaid))}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-" + currencyFormatter.format(repayment.amountPaid),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete repayment",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaidLoanCard(loan: Loan) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3C7CF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = loan.purpose,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Disbursed: ${dateFormatter.format(Date(loan.disbursedDate))}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FULLY PAID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormatter.format(loan.loanAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun AuditLedgerTabContent(uiState: ThriftUiState) {
    // Combine savings and repayment entries chronologically
    val ledgerItems = remember(uiState.savingsList, uiState.repaymentsList) {
        val list = mutableListOf<LedgerEntry>()
        uiState.savingsList.forEach {
            list.add(
                LedgerEntry(
                    id = it.id,
                    type = "SAVINGS",
                    title = "Monthly Savings Contribution",
                    amount = it.amount,
                    date = it.date,
                    notes = it.notes,
                    monthYear = it.monthYear
                )
            )
        }
        uiState.repaymentsList.forEach {
            list.add(
                LedgerEntry(
                    id = it.id,
                    type = "REPAYMENT",
                    title = "Loan Monthly Repayment",
                    amount = it.amountPaid,
                    date = it.datePaid,
                    notes = it.notes,
                    monthYear = it.paymentMonth
                )
            )
        }
        list.sortByDescending { it.date }
        list
    }

    if (ledgerItems.isEmpty()) {
        EmptyBoxState(
            title = "No ledger details found",
            subtitle = "All your cooperative transactions and savings receipts will appear in this audited ledger timeline."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audited General Ledger",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Audit Log",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ledgerItems) { entry ->
                    LedgerEntryCard(entry = entry)
                }
            }
        }
    }
}

data class LedgerEntry(
    val id: Int,
    val type: String, // "SAVINGS" or "REPAYMENT"
    val title: String,
    val amount: Double,
    val date: Long,
    val notes: String,
    val monthYear: String
)

@Composable
fun LedgerEntryCard(entry: LedgerEntry) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3C7CF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (entry.type == "SAVINGS") Color(0xFFE8F5E9)
                                else Color(0xFFFEE2E2)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (entry.type == "SAVINGS") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (entry.type == "SAVINGS") Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entry.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF191C1E)
                        )
                        Text(
                            text = dateFormatter.format(Date(entry.date)),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = (if (entry.type == "SAVINGS") "+" else "-") + currencyFormatter.format(entry.amount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (entry.type == "SAVINGS") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Notes icon",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBoxState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }
}

@Composable
fun BottomActionBar(
    onAddSavings: () -> Unit,
    onAddLoan: () -> Unit,
    onRepay: () -> Unit,
    hasActiveLoans: Boolean
) {
    // Compact Horizontal suggesting chips above bottom bar or structured buttons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High visibility Floating Action Button at bottom panel
        FloatingActionButton(
            onClick = onAddSavings,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Deposit")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Savings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (hasActiveLoans) {
            FloatingActionButton(
                onClick = onRepay,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = "Repay")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Repay Loan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            FloatingActionButton(
                onClick = onAddLoan,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Borrow")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Take Loan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// Dialog Layout Components (Validation-safe Outlined Styles)

@Composable
fun AddSavingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Save Monthly Contribution",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Deposit funds into your cooperative thrift plan to increase your dividends and society credit-score.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("Savings Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        "Please enter a valid deposit amount greater than 0.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Ledger note (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        onConfirm(amt, notesText)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ApplyLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, rate: Double, tenorMonths: Int, purpose: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("6.0") }
    var tenorMonthsText by remember { mutableStateOf("12") }
    var purposeText by remember { mutableStateOf("") }
    
    var isAmountError by remember { mutableStateOf(false) }
    var isRateError by remember { mutableStateOf(false) }
    var isTenorError by remember { mutableStateOf(false) }

    // Live EMI Calculation summary
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val rate = interestRateText.toDoubleOrNull() ?: 0.0
    val tenor = tenorMonthsText.toIntOrNull() ?: 0

    val liveEmi = remember(amount, rate, tenor) {
        if (amount > 0 && rate >= 0 && tenor > 0) {
            val totalInterest = amount * (rate / 100.0) * (tenor / 12.0)
            val totalPayback = amount + totalInterest
            totalPayback / tenor
        } else {
            0.0
        }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Apply for Cooperative Loan",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Calculate and record a loan disbursed by your thrift society. Interest rate is flat annual cooperative credit.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isAmountError = false
                    },
                    label = { Text("Loan Principal (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = {
                            interestRateText = it
                            isRateError = false
                        },
                        label = { Text("Rate % P.A.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isRateError,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = tenorMonthsText,
                        onValueChange = {
                            tenorMonthsText = it
                            isTenorError = false
                        },
                        label = { Text("Term (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isTenorError,
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                OutlinedTextField(
                    value = purposeText,
                    onValueChange = { purposeText = it },
                    label = { Text("Spent Purpose (e.g. Education, Dental)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // EMI real-time display card
                if (liveEmi > 0.0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Monthly Installment (EMI)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currencyFormatter.format(liveEmi),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total Repayable: ${currencyFormatter.format(liveEmi * tenor)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amountText.toDoubleOrNull()
                    val rateVal = interestRateText.toDoubleOrNull()
                    val tenorVal = tenorMonthsText.toIntOrNull()

                    val hasError = amtVal == null || amtVal <= 0.0 ||
                            rateVal == null || rateVal < 0.0 ||
                            tenorVal == null || tenorVal <= 0

                    if (!hasError) {
                        onConfirm(amtVal!!, rateVal!!, tenorVal!!, purposeText)
                    } else {
                        isAmountError = amtVal == null || amtVal <= 0.0
                        isRateError = rateVal == null || rateVal < 0.0
                        isTenorError = tenorVal == null || tenorVal <= 0
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Approve Credit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RepayLoanDialog(
    activeLoans: List<Loan>,
    preselectedLoan: Loan?,
    onDismiss: () -> Unit,
    onConfirm: (loanId: Int, amount: Double, notes: String) -> Unit
) {
    if (activeLoans.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Active Debt", fontWeight = FontWeight.ExtraBold) },
            text = { Text("You currently do not have any active cooperative loans. You do not need to make repayments.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    var selectedLoan by remember { mutableStateOf(preselectedLoan ?: activeLoans.first()) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isAmountError by remember { mutableStateOf(false) }

    // Dropdown selection layer (rendered as active chips for ease of use in visual Android screens to avoid raw spinner bugs)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Record Loan Repayment",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select active loan and enter amount of instalment paid. Auto-calculation divides principal & interest.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Render horizontal chip selector for active loans
                Text(
                    text = "Select Loan Account:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeLoans.take(3).forEach { loan ->
                        SuggestionChip(
                            onClick = { selectedLoan = loan },
                            label = { Text(loan.purpose, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (selectedLoan.id == loan.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (selectedLoan.id == loan.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (selectedLoan.id == loan.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isAmountError = false
                    },
                    label = { Text("Repayment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Instalment No. / Note (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amountText.toDoubleOrNull()
                    if (amtVal != null && amtVal > 0.0) {
                        onConfirm(selectedLoan.id, amtVal, notesText)
                    } else {
                        isAmountError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// Clean helper modifiers

fun Modifier.fillPageSize(): Modifier = this.then(Modifier.fillMaxSize())
