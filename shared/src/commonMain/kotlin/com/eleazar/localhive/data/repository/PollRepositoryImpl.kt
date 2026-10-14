package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.PollRepository
import com.eleazar.localhive.domain.model.Poll
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PollRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : PollRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)
    private fun polls(estateId: String) = estate(estateId).collection("polls")
    // Kept apart from the polls, keyed "<pollId>_<userId>", so the rules can let each member read
    // only their own votes while the tallies on the poll stay public.
    private fun votes(estateId: String) = estate(estateId).collection("pollVotes")

    override fun observePolls(estateId: String): Flow<List<Poll>> =
        polls(estateId)
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.toPoll() } }

    override fun observeMyVotes(estateId: String, userId: String): Flow<Map<String, Int>> =
        votes(estateId)
            .where { "userId" equalTo userId }
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val pollId = doc.string("pollId") ?: return@mapNotNull null
                    val option = doc.long("option")?.toInt() ?: return@mapNotNull null
                    pollId to option
                }.toMap()
            }

    override suspend fun createPoll(estateId: String, poll: Poll): Result<String> = runCatching {
        val ref = polls(estateId).document
        ref.set(
            mapOf(
                "question" to poll.question,
                "options" to poll.options,
                "createdBy" to poll.createdBy,
                "createdByName" to poll.createdByName,
                "createdAt" to poll.createdAt,
                "closesAt" to poll.closesAt,
                "closed" to false,
                "counts" to emptyMap<String, Long>(),
                "voteCount" to 0
            )
        )
        api.notifyInBackground("type" to "poll", "estateId" to estateId, "pollId" to ref.id)
        ref.id
    }

    /** The vote and the tally move together, so the count can never drift from the votes cast. */
    override suspend fun vote(estateId: String, pollId: String, userId: String, option: Int): Result<Unit> = runCatching {
        firestore.batch()
            .set(
                votes(estateId).document("${pollId}_$userId"),
                mapOf("pollId" to pollId, "userId" to userId, "option" to option, "at" to nowMillis())
            )
            .update(
                polls(estateId).document(pollId),
                "counts.$option" to FieldValue.increment(1),
                "voteCount" to FieldValue.increment(1)
            )
            .commit()
    }

    override suspend fun closePoll(estateId: String, pollId: String): Result<Unit> = runCatching {
        polls(estateId).document(pollId).update("closed" to true)
    }

    override suspend fun deletePoll(estateId: String, pollId: String): Result<Unit> = runCatching {
        polls(estateId).document(pollId).delete()
    }

    private fun DocumentSnapshot.toPoll(): Poll {
        val options = stringList("options").orEmpty()
        val counts = intMap("counts").orEmpty()
        return Poll(
            id = id,
            question = string("question").orEmpty(),
            options = options,
            createdBy = string("createdBy").orEmpty(),
            createdByName = string("createdByName").orEmpty(),
            createdAt = long("createdAt") ?: 0L,
            closesAt = long("closesAt") ?: 0L,
            closed = boolean("closed") ?: false,
            counts = options.indices.map { counts["$it"] ?: 0 },
            voteCount = long("voteCount")?.toInt() ?: 0
        )
    }
}
