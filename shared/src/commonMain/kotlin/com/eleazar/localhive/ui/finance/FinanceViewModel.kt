package com.eleazar.localhive.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.FinanceRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.ui.util.parseNaira
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.eleazar.localhive.ui.util.nowMillis

/**
 * Dues and expenses. One instance backs each finance screen; each loads only what it shows,
 * through [openLevy] or the lists that start on creation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val estateRepository: EstateRepository,
    private val financeRepository: FinanceRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState = _uiState.asStateFlow()

    private var me: User? = null
    private var treasurerId: String = ""
    private var levyJob: Job? = null

    /** Settles once the user and their estate are known; everything else waits on it. */
    private val estateId = MutableStateFlow<String?>(null)
    private val isTreasurer = MutableStateFlow(false)

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId) }
        if (userId != null) viewModelScope.launch { load(userId) }
    }

    private suspend fun load(userId: String) {
        val user = userRepository.getUser(userId, cacheFirst = true).getOrNull()
        val estate = user?.estateId
        if (user == null || estate == null) {
            _uiState.update { it.copy(isLoading = false, error = "Join an estate to see its dues") }
            return
        }
        me = user
        _uiState.update { it.copy(estateId = estate) }
        estateId.value = estate

        viewModelScope.launch {
            // Live, so being made (or unmade) treasurer shows straight away.
            estateRepository.observeEstate(estate).collect { e ->
                treasurerId = e?.treasurerId.orEmpty()
                isTreasurer.value = e?.isTreasurer(userId) == true
                _uiState.update { it.copy(isTreasurer = isTreasurer.value, memberCount = e?.memberCount ?: 0) }
                val name = treasurerId.takeIf { it.isNotBlank() }?.let { id ->
                    if (id == userId) "You" else userRepository.getUser(id, cacheFirst = true).getOrNull()?.displayLabel()
                }
                _uiState.update { it.copy(treasurerName = name) }
            }
        }
    }

    // ───────────── Lists ─────────────

    /** Starts the levy list. Each levy carries the user's own standing, or totals for the treasurer. */
    fun loadLevies() {
        viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            val userId = _uiState.value.currentUserId ?: return@launch
            combine(isTreasurer, financeRepository.observeLevies(estate)) { treasurer, levies -> treasurer to levies }
                .flatMapLatest { (treasurer, levies) ->
                    if (levies.isEmpty()) flowOf(emptyList())
                    else combine(levies.map { levyRow(estate, it, userId, treasurer) }) { it.toList() }
                }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { rows -> _uiState.update { it.copy(levies = rows, isLoading = false) } }
        }
    }

    private fun levyRow(estate: String, levy: Levy, userId: String, treasurer: Boolean): Flow<LevyRow> =
        if (treasurer) {
            financeRepository.observePayments(estate, levy.id).map { payments ->
                LevyRow(
                    levy = levy,
                    myStatus = payments.firstOrNull { it.userId == userId }?.paymentStatus,
                    paidCount = payments.count { it.paymentStatus == PaymentStatus.PAID },
                    toReview = payments.count { it.paymentStatus == PaymentStatus.PENDING }
                )
            }
        } else {
            financeRepository.observeMyPayment(estate, levy.id, userId).map { LevyRow(levy, it?.paymentStatus) }
        }.catch { emit(LevyRow(levy)) }

    fun loadExpenses() {
        viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            financeRepository.observeExpenses(estate)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { expenses -> _uiState.update { it.copy(expenses = expenses, isLoading = false) } }
        }
    }

    // ───────────── One levy ─────────────

    fun openLevy(levyId: String) {
        levyJob?.cancel()
        levyJob = viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            val userId = _uiState.value.currentUserId ?: return@launch
            launch {
                financeRepository.observeLevy(estate, levyId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { levy -> _uiState.update { it.copy(levy = levy, levyMissing = levy == null, isLoading = false) } }
            }
            launch {
                financeRepository.observeMyPayment(estate, levyId, userId)
                    .catch { /* Nothing to show yet. */ }
                    .collect { payment -> _uiState.update { it.copy(myPayment = payment) } }
            }
            launch {
                isTreasurer
                    .flatMapLatest { treasurer ->
                        if (!treasurer) flowOf(emptyList())
                        else combine(
                            userRepository.getEstateMembers(estate),
                            financeRepository.observePayments(estate, levyId)
                        ) { members, payments -> memberPayments(members, payments) }
                    }
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { rows -> _uiState.update { it.copy(memberPayments = rows) } }
            }
        }
    }

    /**
     * Every member with their payment, proof waiting to be checked first, then who still owes,
     * then who has paid. Someone who paid and then left the estate still shows, so the books add up.
     */
    private fun memberPayments(members: List<User>, payments: List<LevyPayment>): List<MemberPayment> {
        val byUser = payments.associateBy { it.userId }
        val rows = members.map { MemberPayment(it.id, it.displayLabel(), it.profileImageUrl, byUser[it.id]) } +
            payments.filter { p -> members.none { it.id == p.userId } }
                .map { MemberPayment(it.userId, it.userName.ifBlank { "Former member" }, null, it) }
        return rows.sortedWith(
            compareBy<MemberPayment>({
                when (it.status) {
                    PaymentStatus.PENDING -> 0
                    null, PaymentStatus.REJECTED -> 1
                    PaymentStatus.PAID -> 2
                }
            }, { it.name.lowercase() })
        )
    }

    fun sendProof(image: String) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        val user = me ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = storageRepository.uploadReceipt(user.id, image).mapCatching { url ->
                financeRepository.submitProof(estate, levy, user.id, user.displayLabel(), url, treasurerId).getOrThrow()
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message,
                    message = if (result.isSuccess) "Proof sent. The treasurer will confirm it." else null
                )
            }
        }
    }

    fun confirmPayment(member: MemberPayment) = setStatus(member, PaymentStatus.PAID, "Payment confirmed")
    fun rejectPayment(member: MemberPayment) = setStatus(member, PaymentStatus.REJECTED, "Proof sent back to ${member.name}")
    /** A cash payment, or a transfer the treasurer saw without proof being sent. */
    fun markPaid(member: MemberPayment) = setStatus(member, PaymentStatus.PAID, "Marked ${member.name} as paid")

    private fun setStatus(member: MemberPayment, status: PaymentStatus, done: String) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.setPaymentStatus(estate, levy, member.userId, member.name, status, member.payment)
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = done) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    fun remindUnpaid() {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.remindUnpaid(estate, levy.id)
                .onSuccess { sent ->
                    val message = when (sent) {
                        0 -> "Already reminded today, or everyone has paid"
                        1 -> "Reminder sent to 1 person"
                        else -> "Reminder sent to $sent people"
                    }
                    _uiState.update { it.copy(isSaving = false, message = message) }
                }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    fun deleteLevy() {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        levyJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.deleteLevy(estate, levy.id)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    // ───────────── Forms ─────────────

    fun createLevy(title: String, amountText: String, dueDate: Long, note: String) {
        val estate = _uiState.value.estateId ?: return
        val userId = _uiState.value.currentUserId ?: return
        val amount = parseNaira(amountText)
        if (title.isBlank() || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Give the levy a name and an amount") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val levy = Levy(
                title = title.trim(),
                amount = amount,
                dueDate = dueDate,
                note = note.trim(),
                createdBy = userId,
                createdAt = nowMillis()
            )
            financeRepository.createLevy(estate, levy)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "Couldn't create the levy") } }
        }
    }

    fun addExpense(title: String, amountText: String, spentAt: Long, note: String, receipt: String?) {
        val estate = _uiState.value.estateId ?: return
        val userId = _uiState.value.currentUserId ?: return
        val amount = parseNaira(amountText)
        if (title.isBlank() || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Say what it was for and how much it cost") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // Upload first: an expense pointing at a failed upload would show a broken receipt.
            val receiptUrl = receipt?.let { uri ->
                storageRepository.uploadReceipt(userId, uri).getOrElse { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message ?: "Couldn't upload the receipt") }
                    return@launch
                }
            }
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                spentAt = spentAt,
                note = note.trim(),
                receiptUrl = receiptUrl,
                createdBy = userId,
                createdAt = nowMillis()
            )
            financeRepository.addExpense(estate, expense)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "Couldn't save the expense") } }
        }
    }

    fun deleteExpense(expense: Expense) {
        val estate = _uiState.value.estateId ?: return
        viewModelScope.launch {
            financeRepository.deleteExpense(estate, expense.id)
                .onSuccess { _uiState.update { it.copy(message = "Expense deleted") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun consumeError() = _uiState.update { it.copy(error = null) }
    fun consumeSaved() = _uiState.update { it.copy(saved = false) }
}

private fun User.displayLabel(): String = displayName?.takeIf { it.isNotBlank() } ?: username ?: "Neighbour"
