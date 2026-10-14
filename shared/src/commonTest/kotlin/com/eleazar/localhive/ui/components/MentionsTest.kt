package com.eleazar.localhive.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.eleazar.localhive.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MentionsTest {

    private fun typed(text: String) = TextFieldValue(text, TextRange(text.length))

    private val ada = User(id = "u1", displayName = "Ada Obi")
    private val bola = User(id = "u2", displayName = "Bola")
    private val nameless = User(id = "u3")

    @Test
    fun findsTheMentionBeingTyped() {
        assertEquals(MentionQuery(4, ""), activeMentionQuery(typed("Hey @")))
        assertEquals(MentionQuery(4, "Ada O"), activeMentionQuery(typed("Hey @Ada O")))
        assertEquals(MentionQuery(0, "Bo"), activeMentionQuery(typed("@Bo")))
    }

    @Test
    fun ignoresEmailsAndFinishedMentions() {
        assertNull(activeMentionQuery(typed("mail ada@estate.org")))
        assertNull(activeMentionQuery(typed("@Ada\nnext line")))
        assertNull(activeMentionQuery(typed("@Ada Obi  and then")))
        assertNull(activeMentionQuery(typed("no tag here")))
    }

    @Test
    fun suggestsByAnyWordOfTheName() {
        val members = listOf(ada, bola, nameless)
        assertEquals(listOf(ada), mentionSuggestions(members, "ob"))
        assertEquals(listOf(ada), mentionSuggestions(members, "Ada O"))
        assertEquals(listOf(ada, bola), mentionSuggestions(members, ""))
    }

    @Test
    fun insertingReplacesTheQueryAndMovesTheCursor() {
        val value = typed("Thanks @ad")
        val inserted = insertMention(value, activeMentionQuery(value)!!, ada)
        assertEquals("Thanks @Ada Obi ", inserted.text)
        assertEquals(TextRange(inserted.text.length), inserted.selection)
    }

    @Test
    fun onlyTagsStillInTheTextCount() {
        val picked = mapOf("u1" to "Ada Obi", "u2" to "Bola")
        assertEquals(mapOf("u1" to "Ada Obi"), mentionsIn("Thanks @Ada Obi!", picked))
    }

    @Test
    fun mentionsAndLinksBecomeSeparateLinks() {
        val text = "@Ada Obi see https://x.org and @Ada"
        val annotated = linkify(text, Color.Blue, mapOf("u1" to "Ada Obi", "u9" to "Ada"))
        val links = annotated.getLinkAnnotations(0, annotated.length).map {
            val item = it.item
            text.substring(it.start, it.end) to if (item is LinkAnnotation.Clickable) item.tag else (item as LinkAnnotation.Url).url
        }
        assertEquals(listOf("@Ada Obi" to "u1", "https://x.org" to "https://x.org", "@Ada" to "u9"), links)
        assertEquals(text, annotated.text)
    }
}
