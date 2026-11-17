package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.FinanceRepository
import com.eleazar.localhive.domain.LevyHasPaymentsException
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.FinanceLogEntry
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.FlatProof
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.Residence
import com.eleazar.localhive.domain.model.flatId
import com.eleazar.localhive.domain.model.flatSummary
import com.eleazar.localhive.ui.util.formatNaira
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val api: LocalHiveApi
) : FinanceRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)
    private fun levies(estateId: String) = estate(estateId).collection("levies")
    /** Per-flat levies live apart, so the 1.06 app, which can't show them properly, never sees them. */
    private fun flatLevies(estateId: String) = estate(estateId).collection("flatLevies")
    private fun levyRef(estateId: String, levy: Levy) =
        (if (levy.perFlat) flatLevies(estateId) else levies(estateId)).document(levy.id)
    private fun payments(estateId: String, levyId: String) = levies(estateId).document(levyId).collection("payments")
    private fun flats(estateId: String, levyId: String) = flatLevies(estateId).document(levyId).collection("flats")
    private fun proofs(estateId: String, levyId: String) = flatLevies(estateId).document(levyId).collection("proofs")
    private fun expenses(estateId: String) = estate(estateId).collection("expenses")
    private fun buildings(estateId: String) = estate(estateId).collection("buildings")
    private fun residents(estateId: String) = estate(estateId).collection("residents")
    private fun financeLog(estateId: String) = estate(estateId).collection("financeLog")
    private fun bankRef(estateId: String) = estate(estateId).collection("private").document("bank")

    /**
     * Adds the log entry for a change to the books to [this] batch. The id is fixed by what was
     * changed, which is how the security rules find the entry that has to come with each change.
     */
    private fun WriteBatch.log(
        estateId: String,
        entryId: String,
        action: String,
        summary: String,
        amount: Long,
        by: String,
        byName: String,
        at: Long = nowMillis()
    ): WriteBatch = set(
        financeLog(estateId).document(entryId),
        mapOf(
            "action" to action,
            "summary" to summary.take(300),
            "amount" to amount,
            "by" to by,
            "byName" to byName.take(100),
            "at" to at
        )
    )

    // ───── Levies ─────

    override fun observeLevies(estateId: String): Flow<List<Levy>> = combine(
        levies(estateId).orderBy("createdAt", Direction.DESCENDING).snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toLevy(perFlat = false) } },
        flatLevies(estateId).orderBy("createdAt", Direction.DESCENDING).snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toLevy(perFlat = true) } }
    ) { perMember, perFlat -> (perMember + perFlat).sortedByDescending { it.createdAt } }

    override fun observeLevy(estateId: String, levyId: String): Flow<Levy?> = combine(
        levies(estateId).document(levyId).snapshots.map { it.takeIf { s -> s.exists }?.toLevy(perFlat = false) },
        flatLevies(estateId).document(levyId).snapshots.map { it.takeIf { s -> s.exists }?.toLevy(perFlat = true) }
    ) { perMember, perFlat -> perMember ?: perFlat }

    override fun observePayments(estateId: String, levyId: String): Flow<List<LevyPayment>> =
        payments(estateId, levyId).snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toPayment() } }

    override fun observeMyPayment(estateId: String, levyId: String, userId: String): Flow<LevyPayment?> =
        payments(estateId, levyId).document(userId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.toPayment() }

    override suspend fun createLevy(estateId: String, levy: Levy, byName: String): Result<String> = runCatching {
        val ref = (if (levy.perFlat) flatLevies(estateId) else levies(estateId)).document
        val fields = mutableMapOf<String, Any?>(
            "title" to levy.title,
            "amount" to levy.amount,
            "dueDate" to levy.dueDate,
            "note" to levy.note,
            "createdBy" to levy.createdBy,
            "createdAt" to levy.createdAt,
            "hasPayments" to false
        )
        val each = if (levy.perFlat) " per flat" else " per member"
        firestore.batch()
            .set(ref, fields)
            .log(estateId, "levy_${ref.id}_created", "levy_created", "New levy: ${levy.title}, ${formatNaira(levy.amount)}$each", levy.amount, levy.createdBy, byName)
            .commit()
        ref.id
    }

    override suspend fun deleteLevy(estateId: String, levy: Levy, byName: String): Result<Unit> = runCatching {
        if (levy.hasPayments) throw LevyHasPaymentsException()
        val ref = levyRef(estateId, levy)
        val treasurer = levy.createdBy
        if (levy.perFlat) {
            // Flat payments are never deleted; a levy with any waiting proof keeps it on screen.
            if (flats(estateId, levy.id).get().documents.isNotEmpty()) {
                throw Exception("Someone has sent proof for this levy. Reject it first, or keep the levy.")
            }
        } else {
            // Levies from before hasPayments existed: mark one that has been paid, and keep it.
            val recorded = payments(estateId, levy.id).get().documents
            if (recorded.any { it.string("status") == PaymentStatus.PAID.key }) {
                ref.update("hasPayments" to true)
                throw LevyHasPaymentsException()
            }
            recorded.chunked(400).forEach { chunk ->
                firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit()
            }
        }
        firestore.batch()
            .delete(ref)
            .log(estateId, "levy_${levy.id}_deleted", "levy_deleted", "Deleted levy: ${levy.title}", 0, currentTreasurer(estateId, treasurer), byName)
            .commit()
    }

    /** The treasurer's uid, as the log records it; the rules check it's the caller. */
    private suspend fun currentTreasurer(estateId: String, fallback: String): String =
        estate(estateId).get().string("treasurerId")?.takeIf { it.isNotBlank() } ?: fallback

    override suspend fun submitProof(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        proofUrl: String,
        treasurerId: String
    ): Result<Unit> = runCatching {
        payments(estateId, levy.id).document(userId).set(
            mapOf(
                "userId" to userId,
                "userName" to userName,
                "levyId" to levy.id,
                "amount" to levy.amount,
                "status" to PaymentStatus.PENDING.key,
                "proofUrl" to proofUrl,
                "submittedAt" to nowMillis()
            )
        )
        if (treasurerId.isNotBlank() && treasurerId != userId) {
            notificationRepository.createNotification(
                AppNotification(
                    recipientId = treasurerId,
                    type = NotificationType.PAYMENT_SUBMITTED,
                    title = "$userName sent proof of payment",
                    message = "${levy.title} · ${formatNaira(levy.amount)}. Check it and confirm.",
                    estateId = estateId,
                    relatedUserId = userId,
                    levyId = levy.id
                )
            )
        }
    }

    override suspend fun setPaymentStatus(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        status: PaymentStatus,
        existing: LevyPayment?,
        byName: String,
        undoReason: String?
    ): Result<Unit> = runCatching {
        val now = nowMillis()
        val treasurer = currentTreasurer(estateId, "")
        val name = existing?.userName?.takeIf { it.isNotBlank() } ?: userName
        val undoing = existing?.paymentStatus == PaymentStatus.PAID && status != PaymentStatus.PAID
        if (undoing && undoReason.isNullOrBlank()) throw Exception("Say why you're taking this payment back")
        val fields = mutableMapOf<String, Any?>(
            "userId" to userId,
            "userName" to name,
            "levyId" to levy.id,
            "amount" to levy.amount,
            "status" to status.key,
            "proofUrl" to existing?.proofUrl,
            "submittedAt" to (existing?.submittedAt?.takeIf { it > 0 } ?: now),
            "reviewedAt" to now,
            "confirmedByName" to if (status == PaymentStatus.PAID) byName else ""
        )
        if (undoing) fields["undoReason"] = undoReason!!.trim().take(200)
        val (action, summary) = when {
            undoing -> "payment_undone" to "Took back $name's ${formatNaira(levy.amount)} for ${levy.title}: ${undoReason!!.trim()}"
            status == PaymentStatus.PAID && existing?.proofUrl == null -> "payment_recorded" to "Recorded $name's ${formatNaira(levy.amount)} for ${levy.title} (cash or seen transfer)"
            status == PaymentStatus.PAID -> "payment_confirmed" to "Confirmed $name's ${formatNaira(levy.amount)} for ${levy.title}"
            else -> "payment_rejected" to "Didn't accept $name's proof for ${levy.title}"
        }
        firestore.batch()
            .set(payments(estateId, levy.id).document(userId), fields)
            .log(estateId, "pay_${levy.id}_${userId}_$now", action, summary, if (status == PaymentStatus.PAID) levy.amount else 0, treasurer, byName, now)
            .apply { if (status == PaymentStatus.PAID && !levy.hasPayments) update(levies(estateId).document(levy.id), "hasPayments" to true) }
            .commit()
        val (type, title, message) = when {
            status == PaymentStatus.PAID -> Triple(
                NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed",
                "Your ${formatNaira(levy.amount)} for ${levy.title} has been received. Your receipt is on the levy. Thank you!"
            )
            status == PaymentStatus.REJECTED -> Triple(
                NotificationType.PAYMENT_REJECTED,
                "Proof of payment not accepted",
                "The treasurer couldn't confirm your payment for ${levy.title}. Please check and send it again."
            )
            else -> return@runCatching
        }
        notificationRepository.createNotification(
            AppNotification(
                recipientId = userId,
                type = type,
                title = title,
                message = message,
                estateId = estateId,
                levyId = levy.id
            )
        )
    }

    override suspend fun remindUnpaid(estateId: String, levy: Levy): Result<Int> = runCatching {
        if (!api.isConfigured) error("Reminders aren't set up yet")
        api.notifyNow(
            "type" to "levy_reminder", "estateId" to estateId, "levyId" to levy.id,
            "flat" to levy.perFlat.toString()
        )
    }

    // ───── Buildings and flats ─────

    override fun observeBuildings(estateId: String): Flow<List<Building>> =
        buildings(estateId).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    Building(id = doc.id, name = doc.string("name").orEmpty(), flats = (doc.long("flats") ?: 1L).toInt())
                }.getOrNull()
            }.sortedWith(compareBy(naturalOrder()) { it.name })
        }

    override suspend fun saveBuilding(estateId: String, building: Building, userId: String): Result<Unit> = runCatching {
        val name = building.name.trim()
        if (name.isEmpty()) throw Exception("Give the building a name, like No. 12")
        if (building.flats !in 1..200) throw Exception("A building has between 1 and 200 flats")
        val ref = if (building.id.isBlank()) buildings(estateId).document else buildings(estateId).document(building.id)
        val existing = if (building.id.isBlank()) null else ref.get().takeIf { it.exists }
        ref.set(
            mapOf(
                "name" to name.take(80),
                "flats" to building.flats,
                "createdBy" to (existing?.string("createdBy") ?: userId),
                "createdAt" to (existing?.long("createdAt") ?: nowMillis())
            )
        )
    }

    override suspend fun deleteBuilding(estateId: String, buildingId: String): Result<Unit> = runCatching {
        buildings(estateId).document(buildingId).delete()
    }

    override fun observeResidents(estateId: String): Flow<List<Residence>> =
        residents(estateId).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    Residence(
                        userId = doc.id,
                        buildingId = doc.string("buildingId").orEmpty(),
                        buildingName = doc.string("buildingName").orEmpty(),
                        flat = (doc.long("flat") ?: 0L).toInt(),
                        userName = doc.string("userName").orEmpty()
                    )
                }.getOrNull()
            }
        }

    override suspend fun setResidence(estateId: String, residence: Residence, setBy: String): Result<Unit> = runCatching {
        residents(estateId).document(residence.userId).set(
            mapOf(
                "buildingId" to residence.buildingId,
                "buildingName" to residence.buildingName.take(80),
                "flat" to residence.flat,
                "userName" to residence.userName.take(100),
                "setBy" to setBy,
                "at" to nowMillis()
            )
        )
    }

    override suspend fun clearResidence(estateId: String, userId: String): Result<Unit> = runCatching {
        residents(estateId).document(userId).delete()
    }

    override fun observeFlatPayments(estateId: String, levyId: String): Flow<List<FlatPayment>> =
        flats(estateId, levyId).snapshots.map { snapshot -> snapshot.documents.mapNotNull { it.toFlatPayment() } }

    override fun observeBuildingPayments(estateId: String, levyId: String, buildingId: String): Flow<List<FlatPayment>> =
        flats(estateId, levyId).where { "buildingId" equalTo buildingId }.snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toFlatPayment() } }

    override fun observeFlatProofs(estateId: String, levyId: String, payerId: String?): Flow<List<FlatProof>> =
        (if (payerId == null) proofs(estateId, levyId) else proofs(estateId, levyId).where { "payerId" equalTo payerId })
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        FlatProof(
                            groupId = doc.id,
                            payerId = doc.string("payerId").orEmpty(),
                            payerName = doc.string("payerName").orEmpty(),
                            buildingId = doc.string("buildingId").orEmpty(),
                            flatIds = doc.stringList("flats").orEmpty(),
                            amount = doc.long("amount") ?: 0L,
                            proofUrl = doc.string("proofUrl").orEmpty(),
                            submittedAt = doc.long("submittedAt") ?: 0L
                        )
                    }.getOrNull()
                }
            }

    override suspend fun submitFlatProof(
        estateId: String,
        levy: Levy,
        building: Building,
        flats: List<Int>,
        payerId: String,
        payerName: String,
        proofUrl: String
    ): Result<Unit> = runCatching {
        val covered = flats.distinct().sorted()
        if (covered.isEmpty()) throw Exception("Pick at least one flat")
        val now = nowMillis()
        val proof = proofs(estateId, levy.id).document
        val batch = firestore.batch().set(
            proof,
            mapOf(
                "payerId" to payerId,
                "payerName" to payerName,
                "buildingId" to building.id,
                "flats" to covered.map { flatId(building.id, it) },
                "amount" to levy.amount * covered.size,
                "proofUrl" to proofUrl,
                "submittedAt" to now
            )
        )
        covered.forEach { flat ->
            batch.set(
                flats(estateId, levy.id).document(flatId(building.id, flat)),
                mapOf(
                    "buildingId" to building.id,
                    "buildingName" to building.name,
                    "flat" to flat,
                    "levyId" to levy.id,
                    "amount" to levy.amount,
                    "status" to PaymentStatus.PENDING.key,
                    "payerId" to payerId,
                    "payerName" to payerName,
                    "groupId" to proof.id,
                    "submittedAt" to now
                )
            )
        }
        batch.commit()
        api.notifyInBackground("type" to "flat_proof", "estateId" to estateId, "levyId" to levy.id, "groupId" to proof.id)
    }

    override suspend fun reviewFlatPayments(
        estateId: String,
        levy: Levy,
        payments: List<FlatPayment>,
        status: PaymentStatus,
        byName: String,
        undoReason: String?
    ): Result<Unit> = runCatching {
        val first = payments.firstOrNull() ?: return@runCatching
        val groupId = first.groupId
        val undoing = payments.any { it.paymentStatus == PaymentStatus.PAID } && status != PaymentStatus.PAID
        if (undoing && undoReason.isNullOrBlank()) throw Exception("Say why you're taking this payment back")
        val now = nowMillis()
        val treasurer = currentTreasurer(estateId, "")
        val total = payments.sumOf { it.amount }
        val who = first.payerName.ifBlank { "cash" }
        val which = flatSummary(payments)
        val (action, summary) = when {
            undoing -> "payment_undone" to "Took back ${formatNaira(total)} for ${levy.title} ($which): ${undoReason!!.trim()}"
            status == PaymentStatus.PAID -> "payment_confirmed" to "Confirmed ${formatNaira(total)} from $who for ${levy.title} ($which)"
            else -> "payment_rejected" to "Didn't accept $who's proof for ${levy.title} ($which)"
        }
        val batch = firestore.batch()
        payments.forEach { p ->
            val fields = mutableMapOf<String, Any?>(
                "buildingId" to p.buildingId,
                "buildingName" to p.buildingName,
                "flat" to p.flat,
                "levyId" to levy.id,
                "amount" to levy.amount,
                "status" to status.key,
                "payerId" to p.payerId,
                "payerName" to p.payerName,
                "groupId" to groupId,
                "submittedAt" to p.submittedAt,
                "reviewedAt" to now,
                "confirmedByName" to if (status == PaymentStatus.PAID) byName else ""
            )
            if (undoing) fields["undoReason"] = undoReason!!.trim().take(200)
            batch.set(flats(estateId, levy.id).document(p.flatId), fields)
        }
        batch.log(estateId, "flat_${levy.id}_${groupId}_$now", action, summary, if (status == PaymentStatus.PAID) total else 0, treasurer, byName, now)
        if (status == PaymentStatus.PAID && !levy.hasPayments) batch.update(flatLevies(estateId).document(levy.id), "hasPayments" to true)
        batch.commit()
        if (!undoing) {
            api.notifyInBackground("type" to "flat_reviewed", "estateId" to estateId, "levyId" to levy.id, "groupId" to groupId)
        }
    }

    override suspend fun recordFlatCash(estateId: String, levy: Levy, building: Building, flat: Int, byName: String): Result<Unit> {
        // Its own group, so it's confirmed, logged and receipted like any other payment.
        val groupId = "cash-" + flats(estateId, levy.id).document.id
        val payment = FlatPayment(
            buildingId = building.id,
            buildingName = building.name,
            flat = flat,
            levyId = levy.id,
            amount = levy.amount,
            groupId = groupId,
            submittedAt = nowMillis()
        )
        return reviewFlatPayments(estateId, levy, listOf(payment), PaymentStatus.PAID, byName)
    }

    // ───── Spending, bank details and the log ─────

    override fun observeExpenses(estateId: String): Flow<List<Expense>> =
        expenses(estateId)
            .orderBy("spentAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toExpense() } }

    override suspend fun addExpense(estateId: String, expense: Expense, byName: String): Result<Unit> = runCatching {
        val ref = expenses(estateId).document
        firestore.batch()
            .set(
                ref,
                mapOf(
                    "title" to expense.title,
                    "amount" to expense.amount,
                    "spentAt" to expense.spentAt,
                    "note" to expense.note,
                    "receiptUrl" to expense.receiptUrl,
                    "createdBy" to expense.createdBy,
                    "createdAt" to expense.createdAt
                )
            )
            .log(estateId, "expense_${ref.id}_added", "expense_added", "Spent ${formatNaira(expense.amount)} on ${expense.title}", expense.amount, expense.createdBy, byName)
            .commit()
    }

    override suspend fun deleteExpense(estateId: String, expense: Expense, byName: String): Result<Unit> = runCatching {
        val treasurer = currentTreasurer(estateId, expense.createdBy)
        firestore.batch()
            .delete(expenses(estateId).document(expense.id))
            .log(estateId, "expense_${expense.id}_deleted", "expense_deleted", "Deleted expense: ${expense.title}, ${formatNaira(expense.amount)}", expense.amount, treasurer, byName)
            .commit()
    }

    override fun observeBankDetails(estateId: String): Flow<BankDetails?> =
        bankRef(estateId).snapshots
            .map { snapshot ->
                snapshot.takeIf { it.exists }?.let {
                    BankDetails(
                        bankName = it.string("bankName").orEmpty(),
                        accountNumber = it.string("accountNumber").orEmpty(),
                        accountName = it.string("accountName").orEmpty(),
                        updatedByName = it.string("updatedByName").orEmpty(),
                        updatedAt = it.long("updatedAt") ?: 0L
                    )
                }
            }

    override suspend fun setBankDetails(estateId: String, details: BankDetails?, updatedBy: String, byName: String): Result<Unit> = runCatching {
        if (details == null) {
            val now = nowMillis()
            firestore.batch()
                .delete(bankRef(estateId))
                .log(estateId, "bank_removed_$now", "bank_removed", "Removed the bank details", 0, updatedBy, byName, now)
                .commit()
            return@runCatching
        }
        firestore.batch()
            .set(
                bankRef(estateId),
                mapOf(
                    "bankName" to details.bankName,
                    "accountNumber" to details.accountNumber,
                    "accountName" to details.accountName,
                    "updatedBy" to updatedBy,
                    "updatedByName" to details.updatedByName,
                    "updatedAt" to details.updatedAt
                )
            )
            .log(
                estateId, "bank_${details.updatedAt}", "bank_changed",
                "Dues now go to ${details.bankName}, account ending ${details.accountNumber.takeLast(4)} (${details.accountName})",
                0, updatedBy, byName, details.updatedAt
            )
            .commit()
        // Every member hears about it, so a quietly swapped account number can't go unnoticed.
        api.notifyInBackground("type" to "bank_changed", "estateId" to estateId)
    }

    override fun observeFinanceLog(estateId: String): Flow<List<FinanceLogEntry>> =
        financeLog(estateId)
            .orderBy("at", Direction.DESCENDING)
            .limit(200)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        FinanceLogEntry(
                            id = doc.id,
                            action = doc.string("action").orEmpty(),
                            summary = doc.string("summary").orEmpty(),
                            amount = doc.long("amount") ?: 0L,
                            byName = doc.string("byName").orEmpty(),
                            at = doc.long("at") ?: 0L
                        )
                    }.getOrNull()
                }
            }

    // ───── Mapping ─────

    private fun DocumentSnapshot.toLevy(perFlat: Boolean): Levy? = runCatching {
        Levy(
            id = id,
            title = string("title").orEmpty(),
            amount = long("amount") ?: 0L,
            dueDate = long("dueDate") ?: 0L,
            note = string("note").orEmpty(),
            createdBy = string("createdBy").orEmpty(),
            createdAt = long("createdAt") ?: 0L,
            perFlat = perFlat,
            hasPayments = boolean("hasPayments") ?: false
        )
    }.getOrNull()

    private fun DocumentSnapshot.toPayment(): LevyPayment? = runCatching {
        LevyPayment(
            userId = string("userId") ?: id,
            userName = string("userName").orEmpty(),
            levyId = string("levyId").orEmpty(),
            amount = long("amount") ?: 0L,
            status = string("status").orEmpty(),
            proofUrl = string("proofUrl"),
            submittedAt = long("submittedAt") ?: 0L,
            reviewedAt = long("reviewedAt") ?: 0L,
            confirmedByName = string("confirmedByName").orEmpty(),
            undoReason = string("undoReason").orEmpty()
        )
    }.getOrNull()

    private fun DocumentSnapshot.toFlatPayment(): FlatPayment? = runCatching {
        FlatPayment(
            buildingId = string("buildingId").orEmpty(),
            buildingName = string("buildingName").orEmpty(),
            flat = (long("flat") ?: 0L).toInt(),
            levyId = string("levyId").orEmpty(),
            amount = long("amount") ?: 0L,
            status = string("status").orEmpty(),
            payerId = string("payerId").orEmpty(),
            payerName = string("payerName").orEmpty(),
            groupId = string("groupId").orEmpty(),
            submittedAt = long("submittedAt") ?: 0L,
            reviewedAt = long("reviewedAt") ?: 0L,
            confirmedByName = string("confirmedByName").orEmpty(),
            undoReason = string("undoReason").orEmpty()
        )
    }.getOrNull()

    private fun DocumentSnapshot.toExpense(): Expense? = runCatching {
        Expense(
            id = id,
            title = string("title").orEmpty(),
            amount = long("amount") ?: 0L,
            spentAt = long("spentAt") ?: 0L,
            note = string("note").orEmpty(),
            receiptUrl = string("receiptUrl"),
            createdBy = string("createdBy").orEmpty(),
            createdAt = long("createdAt") ?: 0L
        )
    }.getOrNull()
}
