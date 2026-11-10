package com.eleazar.localhive.domain.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    /** The message this one answers, when the sender swiped or chose Reply. */
    val replyTo: MessageReply? = null
)

/**
 * A copy of the message being answered, kept on the reply itself so it still reads right when
 * the original is far back in the history and not loaded.
 */
data class MessageReply(
    val messageId: String,
    val senderId: String,
    val content: String
) {
    companion object {
        /** How much of the original is kept; the bubble only shows a couple of lines anyway. */
        const val MAX_PREVIEW_LENGTH = 200

        fun of(message: Message) = MessageReply(
            messageId = message.id,
            senderId = message.senderId,
            content = message.content.take(MAX_PREVIEW_LENGTH)
        )
    }
}
