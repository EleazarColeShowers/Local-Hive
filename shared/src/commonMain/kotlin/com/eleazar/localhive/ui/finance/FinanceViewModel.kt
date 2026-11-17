package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.ui.util.friendlyMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EXPENSE_UNDO_WINDOW_MILLIS
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.FinanceRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.Residence
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.flatId
import com.eleazar.localhive.ui.util.parseNaira
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private var leviesJob: Job? = null
    private var expensesJob: Job? = null

    /** Settles once the user and their estate are known; everything else waits on it. */
    private val estateId = MutableStateFlow<String?>(null)
    private val isTreasurer = MutableStateFlow(false)
    /** Where the user lives, once known; per-flat levies follow it. */
    private val myResidence = MutableStateFlow<Residence?>(null)

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
                _uiState.update {
                    it.copy(
                        isTreasurer = isTreasurer.value,
                        isAdmin = e?.isAdmin(userId) == true,
                        memberCount = e?.memberCount ?: 0,
                        estateName = e?.name.orEmpty()
                    )
                }
                val name = treasurerId.takeIf { it.isNotBlank() }?.let { id ->
                    if (id == userId) "You" else userRepository.getUser(id, cacheFirst = true).getOrNull()?.displayLabel()
                }
                _uiState.update { it.copy(treasurerName = name) }
            }
        }
        viewModelScope.launch {
            financeRepository.observeBankDetails(estate)
                .catch { /* Not readable yet; there's nothing to show. */ }
                .collect { bank -> _uiState.update { it.copy(bank = bank) } }
        }
        viewModelScope.launch {
            financeRepository.observeBuildings(estate)
                .catch { emit(emptyList()) }
                .collect { buildings -> _uiState.update { it.copy(buildings = buildings) } }
        }
        viewModelScope.launch {
            // Only people still in the estate: someone who left on an older app may have left their pick behind.
            combine(
                financeRepository.observeResidents(estate).catch { emit(emptyList()) },
                userRepository.getEstateMembers(estate).catch { emit(emptyList()) }
            ) { residents, members ->
                val ids = members.map { it.id }.toSet()
                residents.filter { it.userId in ids }
            }.collect { residents ->
                val mine = residents.firstOrNull { it.userId == userId }
                myResidence.value = mine
                _uiState.update { it.copy(residents = residents, myResidence = mine) }
            }
        }
    }

    // ───────────── Lists ─────────────

    /** Starts the levy list. Each levy carries the user's own standing, or totals for the treasurer. */
    fun loadLevies() {
        leviesJob?.cancel()
        leviesJob = viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            val userId = _uiState.value.currentUserId ?: return@launch
            combine(
                isTreasurer,
                myResidence.map { it?.buildingId to it?.flatId }.distinctUntilChanged(),
                financeRepository.observeLevies(estate),
                financeRepository.observeBuildings(estate).catch { emit(emptyList()) }
            ) { treasurer, home, levies, buildings -> LevyInputs(treasurer, home.first, home.second, levies, buildings) }
                .flatMapLatest { input ->
                    if (input.levies.isEmpty()) flowOf(emptyList())
                    else combine(input.levies.map { levyRow(estate, it, userId, input) }) { it.toList() }
                }
                .catch { e -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.friendlyMessage()) } }
                .collect { rows -> _uiState.update { it.copy(levies = rows, isLoading = false, isRefreshing = false) } }
        }
    }

    /** What the levy list depends on, besides the payments themselves. */
    private data class LevyInputs(
        val treasurer: Boolean,
        val myBuildingId: String?,
        val myFlatId: String?,
        val levies: List<Levy>,
        val buildings: List<Building>
    )

    private fun levyRow(estate: String, levy: Levy, userId: String, input: LevyInputs): Flow<LevyRow> {
        val treasurer = input.treasurer
        val myBuildingId = input.myBuildingId
        val myFlatId = input.myFlatId
        val totalFlats = input.buildings.sumOf { it.flats }
        return when {
            levy.perFlat && treasurer -> financeRepository.observeFlatPayments(estate, levy.id).map { flats ->
                LevyRow(
                    levy = levy,
                    myStatus = flats.firstOrNull { it.flatId == myFlatId }?.paymentStatus,
                    paidCount = flats.count { it.paymentStatus == PaymentStatus.PAID },
                    toReview = flats.filter { it.paymentStatus == PaymentStatus.PENDING }.map { it.groupId }.distinct().size,
                    totalFlats = totalFlats,
                    needsFlat = myFlatId == null
                )
            }
            levy.perFlat && myBuildingId != null -> financeRepository.observeBuildingPayments(estate, levy.id, myBuildingId)
                .map { flats -> LevyRow(levy, flats.firstOrNull { it.flatId == myFlatId }?.paymentStatus, totalFlats = totalFlats) }
            levy.perFlat -> flowOf(LevyRow(levy, totalFlats = totalFlats, needsFlat = true))
            treasurer -> financeRepository.observePayments(estate, levy.id).map { payments ->
                LevyRow(
                    levy = levy,
                    myStatus = payments.firstOrNull { it.userId == userId }?.paymentStatus,
                    paidCount = payments.count { it.paymentStatus == PaymentStatus.PAID },
                    toReview = payments.count { it.paymentStatus == PaymentStatus.PENDING }
                )
            }
            else -> financeRepository.observeMyPayment(estate, levy.id, userId).map { LevyRow(levy, it?.paymentStatus) }
        }.catch { emit(LevyRow(levy, needsFlat = levy.perFlat && myFlatId == null)) }
    }

    fun loadExpenses() {
        expensesJob?.cancel()
        expensesJob = viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            financeRepository.observeExpenses(estate)
                .catch { e -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.friendlyMessage()) } }
                .collect { expenses -> _uiState.update { it.copy(expenses = expenses, isLoading = false, isRefreshing = false) } }
        }
    }

    /** Pull to refresh: reconnects the dues and expenses lists. */
    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.estateId == null) return
        _uiState.update { it.copy(isRefreshing = true) }
        loadLevies()
        loadExpenses()
    }

    /**
     * The statement: every confirmed payment on every levy, and every expense. Only the treasurer
     * can read every payment, so for anyone else it stays empty.
     */
    fun loadStatement() {
        viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            val income = combine(isTreasurer, financeRepository.observeLevies(estate)) { treasurer, levies -> treasurer to levies }
                .flatMapLatest { (treasurer, levies) ->
                    if (!treasurer || levies.isEmpty()) flowOf(emptyList())
                    else combine(levies.map { levy -> levyIncome(estate, levy) }) { it.toList().flatten() }
                }
            combine(income, financeRepository.observeExpenses(estate)) { paid, spent ->
                (paid + spent.map(StatementEntry::expense)).sortedByDescending { it.at }
            }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.friendlyMessage()) } }
                .collect { entries -> _uiState.update { it.copy(statement = entries, isLoading = false) } }
        }
    }

    private fun levyIncome(estate: String, levy: Levy): Flow<List<StatementEntry>> =
        if (levy.perFlat) {
            financeRepository.observeFlatPayments(estate, levy.id).map { flats ->
                flats.filter { it.paymentStatus == PaymentStatus.PAID }
                    .groupBy { it.groupId }
                    .map { (_, group) -> StatementEntry.flatIncome(levy, group) }
            }
        } else {
            financeRepository.observePayments(estate, levy.id).map { payments ->
                payments.filter { it.paymentStatus == PaymentStatus.PAID }.map { StatementEntry.income(levy, it) }
            }
        }.catch { emit(emptyList()) }

    /** The finance log: everything the treasurer has done to the books. */
    fun loadFinanceLog() {
        viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            financeRepository.observeFinanceLog(estate)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.friendlyMessage()) } }
                .collect { entries -> _uiState.update { it.copy(financeLog = entries, isLoading = false) } }
        }
    }

    // ───────────── Bank details ─────────────

    fun saveBankDetails(bankName: String, accountNumber: String, accountName: String) {
        val estate = _uiState.value.estateId ?: return
        val user = me ?: return
        val number = accountNumber.filter { it.isDigit() }
        if (bankName.isBlank() || accountName.isBlank() || number.length !in 6..20) {
            _uiState.update { it.copy(error = "Fill in the bank, the account number and the name on the account") }
            return
        }
        val details = BankDetails(
            bankName = bankName.trim(),
            accountNumber = number,
            accountName = accountName.trim(),
            updatedByName = user.displayLabel(),
            updatedAt = nowMillis()
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.setBankDetails(estate, details, user.id, user.displayLabel())
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = "Bank details saved. Every member can see them.") } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't save the bank details")) } }
        }
    }

    fun removeBankDetails() {
        val estate = _uiState.value.estateId ?: return
        val user = me ?: return
        viewModelScope.launch {
            financeRepository.setBankDetails(estate, null, user.id, user.displayLabel())
                .onSuccess { _uiState.update { it.copy(message = "Bank details removed") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
        }
    }

    // ───────────── Where people live ─────────────

    /** The user saying which flat they live in. */
    fun pickHome(building: Building, flat: Int) {
        val user = me ?: return
        setResidence(Residence(user.id, building.id, building.name, flat, user.displayLabel()), "Saved: ${building.name}, flat $flat")
    }

    /** The treasurer or an admin correcting where a member lives. */
    fun moveResident(residence: Residence, building: Building, flat: Int) =
        setResidence(residence.copy(buildingId = building.id, buildingName = building.name, flat = flat), "Moved ${residence.userName} to ${building.name}, flat $flat")

    private fun setResidence(residence: Residence, done: String) {
        val estate = _uiState.value.estateId ?: return
        val by = me?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.setResidence(estate, residence, by)
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = done) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't save where you live")) } }
        }
    }

    /** The treasurer or an admin taking off a wrong pick, so the member picks again. */
    fun clearResident(residence: Residence) {
        val estate = _uiState.value.estateId ?: return
        viewModelScope.launch {
            financeRepository.clearResidence(estate, residence.userId)
                .onSuccess { _uiState.update { it.copy(message = "${residence.userName} will be asked to pick their flat again") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
        }
    }

    fun saveBuilding(building: Building) {
        val estate = _uiState.value.estateId ?: return
        val by = me?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.saveBuilding(estate, building, by)
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = "${building.name.trim()} saved") } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't save the building")) } }
        }
    }

    fun deleteBuilding(building: Building) {
        val estate = _uiState.value.estateId ?: return
        if (_uiState.value.residents.any { it.buildingId == building.id }) {
            _uiState.update { it.copy(error = "People still live in ${building.name}. Move them first.") }
            return
        }
        viewModelScope.launch {
            financeRepository.deleteBuilding(estate, building.id)
                .onSuccess { _uiState.update { it.copy(message = "${building.name} removed") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
        }
    }

    // ───────────── One levy ─────────────

    fun openLevy(levyId: String) {
        levyJob?.cancel()
        levyJob = viewModelScope.launch {
            val estate = estateId.filterNotNull().first()
            val userId = _uiState.value.currentUserId ?: return@launch
            val levyFlow = financeRepository.observeLevy(estate, levyId)
            launch {
                levyFlow
                    .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                    .collect { levy -> _uiState.update { it.copy(levy = levy, levyMissing = levy == null, isLoading = false) } }
            }
            // Which kind of levy decides what else to load; it never changes.
            val perFlat = levyFlow.filterNotNull().first().perFlat
            if (perFlat) openFlatLevy(estate, levyId, userId) else openMemberLevy(estate, levyId, userId)
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.openMemberLevy(estate: String, levyId: String, userId: String) {
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
                .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                .collect { rows -> _uiState.update { it.copy(memberPayments = rows) } }
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.openFlatLevy(estate: String, levyId: String, userId: String) {
        launch {
            // The treasurer sees every flat; a resident, the flats in their own building.
            combine(isTreasurer, myResidence.map { it?.buildingId }.distinctUntilChanged()) { t, b -> t to b }
                .flatMapLatest { (treasurer, building) ->
                    when {
                        treasurer -> financeRepository.observeFlatPayments(estate, levyId)
                        building != null -> financeRepository.observeBuildingPayments(estate, levyId, building)
                        else -> flowOf(emptyList())
                    }
                }
                .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                .collect { flats ->
                    _uiState.update { state ->
                        val mine = state.myResidence ?: return@update state.copy(flatPayments = flats, selectedFlats = emptySet())
                        // Only flats nobody has paid or sent proof for can be ticked. The user's
                        // own starts ticked while it's still owed.
                        val open = (1..(state.myBuilding?.flats ?: 0)).filter { n ->
                            flats.none { it.flatId == flatId(mine.buildingId, n) && it.paymentStatus != PaymentStatus.REJECTED }
                        }.toSet()
                        val selected = (state.selectedFlats intersect open)
                            .ifEmpty { if (mine.flat in open) setOf(mine.flat) else emptySet() }
                        state.copy(flatPayments = flats, selectedFlats = selected)
                    }
                }
        }
        launch {
            isTreasurer
                .flatMapLatest { treasurer -> financeRepository.observeFlatProofs(estate, levyId, if (treasurer) null else userId) }
                .catch { emit(emptyList()) }
                .collect { proofs -> _uiState.update { it.copy(flatProofs = proofs) } }
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

    /** Ticks or unticks a flat in the user's building to pay for. */
    fun toggleFlat(flat: Int) = _uiState.update {
        it.copy(selectedFlats = if (flat in it.selectedFlats) it.selectedFlats - flat else it.selectedFlats + flat)
    }

    /** Sends a photo of the transfer as proof: for the user, or on a per-flat levy, for the ticked flats. */
    fun sendProof(image: String) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        val user = me ?: return
        val building = state.myBuilding
        if (levy.perFlat && (building == null || state.selectedFlats.isEmpty())) {
            _uiState.update { it.copy(error = "Pick the flats this payment covers") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = storageRepository.uploadReceipt(user.id, image).mapCatching { url ->
                if (levy.perFlat) {
                    financeRepository.submitFlatProof(estate, levy, building!!, state.selectedFlats.toList(), user.id, user.displayLabel(), url).getOrThrow()
                } else {
                    financeRepository.submitProof(estate, levy, user.id, user.displayLabel(), url, treasurerId).getOrThrow()
                }
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.friendlyMessage(),
                    message = if (result.isSuccess) "Proof sent. The treasurer will confirm it." else null,
                    selectedFlats = if (result.isSuccess) emptySet() else it.selectedFlats
                )
            }
        }
    }

    fun confirmPayment(member: MemberPayment) = setStatus(member, PaymentStatus.PAID, "Payment confirmed")
    fun rejectPayment(member: MemberPayment) = setStatus(member, PaymentStatus.REJECTED, "Proof sent back to ${member.name}")
    /** A cash payment, or a transfer the treasurer saw without proof being sent. */
    fun markPaid(member: MemberPayment) = setStatus(member, PaymentStatus.PAID, "Marked ${member.name} as paid")
    /** Taking back a confirmation made by mistake. The reason goes in the finance log. */
    fun undoPayment(member: MemberPayment, reason: String) =
        setStatus(member, PaymentStatus.PENDING, "${member.name}'s payment taken back", reason)

    private fun setStatus(member: MemberPayment, status: PaymentStatus, done: String, undoReason: String? = null) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val treasurerName = me?.displayLabel().orEmpty()
            financeRepository.setPaymentStatus(estate, levy, member.userId, member.name, status, member.payment, treasurerName, undoReason)
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = done) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage()) } }
        }
    }

    fun confirmProof(review: ProofToReview) = reviewFlats(review.payments, PaymentStatus.PAID, "Payment confirmed")
    fun rejectProof(review: ProofToReview) = reviewFlats(review.payments, PaymentStatus.REJECTED, "Proof sent back to ${review.payerName}")
    /** Takes back a confirmed per-flat payment, every flat its proof covered. */
    fun undoFlatPayment(payment: FlatPayment, reason: String) {
        val group = _uiState.value.flatPayments.filter { it.groupId == payment.groupId }.ifEmpty { listOf(payment) }
        reviewFlats(group, PaymentStatus.PENDING, "Payment taken back", reason)
    }

    fun recordFlatCash(row: FlatRow) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.recordFlatCash(estate, levy, row.building, row.flat, me?.displayLabel().orEmpty())
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = "${row.building.name}, flat ${row.flat} marked as paid") } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage()) } }
        }
    }

    private fun reviewFlats(payments: List<FlatPayment>, status: PaymentStatus, done: String, undoReason: String? = null) {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.reviewFlatPayments(estate, levy, payments, status, me?.displayLabel().orEmpty(), undoReason)
                .onSuccess { _uiState.update { it.copy(isSaving = false, message = done) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage()) } }
        }
    }

    fun remindUnpaid() {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.remindUnpaid(estate, levy)
                .onSuccess { sent ->
                    val message = when (sent) {
                        0 -> "Already reminded today, or everyone has paid"
                        1 -> "Reminder sent to 1 person"
                        else -> "Reminder sent to $sent people"
                    }
                    _uiState.update { it.copy(isSaving = false, message = message) }
                }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage()) } }
        }
    }

    fun deleteLevy() {
        val state = _uiState.value
        val levy = state.levy ?: return
        val estate = state.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            financeRepository.deleteLevy(estate, levy, me?.displayLabel().orEmpty())
                .onSuccess {
                    levyJob?.cancel()
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: e.friendlyMessage()) } }
        }
    }

    // ───────────── Forms ─────────────

    fun createLevy(title: String, amountText: String, dueDate: Long, note: String, perFlat: Boolean) {
        val estate = _uiState.value.estateId ?: return
        val userId = _uiState.value.currentUserId ?: return
        val amount = parseNaira(amountText)
        if (title.isBlank() || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Give the levy a name and an amount") }
            return
        }
        if (perFlat && _uiState.value.buildings.isEmpty()) {
            _uiState.update { it.copy(error = "Add the estate's buildings first, so each flat can be charged") }
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
                createdAt = nowMillis(),
                perFlat = perFlat
            )
            financeRepository.createLevy(estate, levy, me?.displayLabel().orEmpty())
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't create the levy")) } }
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
                    _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't upload the receipt")) }
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
            financeRepository.addExpense(estate, expense, me?.displayLabel().orEmpty())
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.friendlyMessage("Couldn't save the expense")) } }
        }
    }

    fun deleteExpense(expense: Expense) {
        val estate = _uiState.value.estateId ?: return
        if (!expense.canStillDelete()) {
            _uiState.update { it.copy(error = "Expenses stay on the books once they're a day old") }
            return
        }
        viewModelScope.launch {
            financeRepository.deleteExpense(estate, expense, me?.displayLabel().orEmpty())
                .onSuccess { _uiState.update { it.copy(message = "Expense deleted") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun consumeError() = _uiState.update { it.copy(error = null) }
    fun consumeSaved() = _uiState.update { it.copy(saved = false) }
}

/** Within a day of adding it, an expense can still be taken back. The rules allow the same. */
fun Expense.canStillDelete(now: Long = nowMillis()): Boolean = now < createdAt + EXPENSE_UNDO_WINDOW_MILLIS

private fun User.displayLabel(): String = displayName?.takeIf { it.isNotBlank() } ?: username ?: "Neighbour"
