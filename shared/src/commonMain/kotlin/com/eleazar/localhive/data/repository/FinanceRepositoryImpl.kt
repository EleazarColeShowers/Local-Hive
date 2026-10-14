package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.FinanceRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.ui.util.formatNaira
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val api: LocalHiveApi
) : FinanceRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)
    private fun levies(estateId: String) = estate(estateId).collection("levies")
    private fun payments(estateId: String, levyId: String) = levies(estateId).document(levyId).collection("payments")
    private fun expenses(estateId: String) = estate(estateId).collection("expenses")

    override fun observeLevies(estateId: String): Flow<List<Levy>> =
        levies(estateId)
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toLevy() } }

    override fun observeLevy(estateId: String, levyId: String): Flow<Levy?> =
        levies(estateId).document(levyId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.toLevy() }

    override fun observePayments(estateId: String, levyId: String): Flow<List<LevyPayment>> =
        payments(estateId, levyId).snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toPayment() } }

    override fun observeMyPayment(estateId: String, levyId: String, userId: String): Flow<LevyPayment?> =
        payments(estateId, levyId).document(userId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.toPayment() }

    override suspend fun createLevy(estateId: String, levy: Levy): Result<String> = runCatching {
        val ref = levies(estateId).document
        ref.set(
            mapOf(
                "title" to levy.title,
                "amount" to levy.amount,
                "dueDate" to levy.dueDate,
                "note" to levy.note,
                "createdBy" to levy.createdBy,
                "createdAt" to levy.createdAt
            )
        )
        ref.id
    }

    override suspend fun deleteLevy(estateId: String, levyId: String): Result<Unit> = runCatching {
        // Payments first: a deleted levy must not leave records behind that nothing points to.
        val recorded = payments(estateId, levyId).get()
        recorded.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit()
        }
        levies(estateId).document(levyId).delete()
    }

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
        existing: LevyPayment?
    ): Result<Unit> = runCatching {
        val now = nowMillis()
        payments(estateId, levy.id).document(userId).set(
            mapOf(
                "userId" to userId,
                "userName" to (existing?.userName?.takeIf { it.isNotBlank() } ?: userName),
                "levyId" to levy.id,
                "amount" to levy.amount,
                "status" to status.key,
                "proofUrl" to existing?.proofUrl,
                "submittedAt" to (existing?.submittedAt?.takeIf { it > 0 } ?: now),
                "reviewedAt" to now
            )
        )
        val (type, title, message) = when (status) {
            PaymentStatus.PAID -> Triple(
                NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed",
                "Your ${formatNaira(levy.amount)} for ${levy.title} has been received. Thank you!"
            )
            PaymentStatus.REJECTED -> Triple(
                NotificationType.PAYMENT_REJECTED,
                "Proof of payment not accepted",
                "The treasurer couldn't confirm your payment for ${levy.title}. Please check and send it again."
            )
            PaymentStatus.PENDING -> return@runCatching
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

    override suspend fun remindUnpaid(estateId: String, levyId: String): Result<Int> = runCatching {
        if (!api.isConfigured) error("Reminders aren't set up yet")
        api.notifyNow("type" to "levy_reminder", "estateId" to estateId, "levyId" to levyId)
    }

    override fun observeExpenses(estateId: String): Flow<List<Expense>> =
        expenses(estateId)
            .orderBy("spentAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toExpense() } }

    override suspend fun addExpense(estateId: String, expense: Expense): Result<Unit> = runCatching {
        expenses(estateId).document.set(
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
    }

    override suspend fun deleteExpense(estateId: String, expenseId: String): Result<Unit> = runCatching {
        expenses(estateId).document(expenseId).delete()
    }

    private fun DocumentSnapshot.toLevy(): Levy? = runCatching {
        Levy(
            id = id,
            title = string("title").orEmpty(),
            amount = long("amount") ?: 0L,
            dueDate = long("dueDate") ?: 0L,
            note = string("note").orEmpty(),
            createdBy = string("createdBy").orEmpty(),
            createdAt = long("createdAt") ?: 0L
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
            reviewedAt = long("reviewedAt") ?: 0L
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
