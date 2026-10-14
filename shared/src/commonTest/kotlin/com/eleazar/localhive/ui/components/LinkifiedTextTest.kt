package com.eleazar.localhive.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkifiedTextTest {

    private fun links(text: String): List<Pair<String, String>> {
        val annotated = linkify(text, Color.Blue)
        return annotated.getLinkAnnotations(0, annotated.length).map {
            annotated.text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url
        }
    }

    @Test
    fun plainTextIsUntouched() {
        assertEquals(emptyList(), links("Bin day is Thursday."))
        assertEquals("Bin day is Thursday.", linkify("Bin day is Thursday.", Color.Blue).text)
    }

    @Test
    fun findsEveryLinkAndKeepsTheText() {
        val text = "Minutes at https://example.com/a?b=1 and www.estate.org too"
        assertEquals(
            listOf("https://example.com/a?b=1" to "https://example.com/a?b=1", "www.estate.org" to "https://www.estate.org"),
            links(text)
        )
        assertEquals(text, linkify(text, Color.Blue).text)
    }

    @Test
    fun sentencePunctuationStaysOutOfTheLink() {
        assertEquals(listOf("https://example.com" to "https://example.com"), links("See https://example.com."))
        assertEquals(listOf("https://example.com" to "https://example.com"), links("(see https://example.com)"))
    }

    @Test
    fun bracketsThatBelongToTheLinkStay() {
        val wiki = "https://en.wikipedia.org/wiki/Hive_(band)"
        assertEquals(listOf(wiki to wiki), links("Read $wiki!"))
    }
}
