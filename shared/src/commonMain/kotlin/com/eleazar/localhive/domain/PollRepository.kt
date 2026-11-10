package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Poll
import kotlinx.coroutines.flow.Flow

/** Estate polls. Admins create and close them; each member has one vote, which they can move while the poll is open. */
interface PollRepository {
    /** Newest first. */
    fun observePolls(estateId: String): Flow<List<Poll>>
    /** The option the user picked in each poll they've voted in, by poll id. */
    fun observeMyVotes(estateId: String, userId: String): Flow<Map<String, Int>>
    /** Returns the new poll's id, and tells the estate about it. */
    suspend fun createPoll(estateId: String, poll: Poll): Result<String>
    suspend fun vote(estateId: String, pollId: String, userId: String, option: Int): Result<Unit>
    /** Moves the user's existing vote from [from] to [to]; the total stays the same. */
    suspend fun changeVote(estateId: String, pollId: String, userId: String, from: Int, to: Int): Result<Unit>
    suspend fun closePoll(estateId: String, pollId: String): Result<Unit>
    suspend fun deletePoll(estateId: String, pollId: String): Result<Unit>
}
