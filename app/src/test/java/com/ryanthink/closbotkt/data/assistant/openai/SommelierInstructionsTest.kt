package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.knowledge.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.WineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SommelierInstructionsTest {

    private fun note(text: String) = RagQueryResult(score = 90f, content = text)

    private fun context(personal: List<String> = emptyList(), other: List<String> = emptyList()) =
        WineContext(personalNotes = personal.map(::note), otherContext = other.map(::note))

    @Test
    fun `always describes a sommelier`() {
        assertTrue(sommelierInstructions(burgundyFocus = true, context = null).contains("sommelier", ignoreCase = true))
        assertTrue(
            sommelierInstructions(burgundyFocus = false, context = null).contains("sommelier", ignoreCase = true)
        )
    }

    @Test
    fun `mentions Burgundy when the focus is on`() {
        assertTrue(sommelierInstructions(burgundyFocus = true, context = null).contains("Burgundy"))
    }

    @Test
    fun `leaves Burgundy out when the focus is off`() {
        assertFalse(sommelierInstructions(burgundyFocus = false, context = null).contains("Burgundy"))
    }

    @Test
    fun `no context and empty context give the same instructions`() {
        assertEquals(
            sommelierInstructions(burgundyFocus = true, context = null),
            sommelierInstructions(burgundyFocus = true, context = context()),
        )
    }

    @Test
    fun `without context there is no mention of notes`() {
        val instructions = sommelierInstructions(burgundyFocus = true, context = null)

        assertFalse(instructions.contains("personal notes", ignoreCase = true))
        assertFalse(instructions.contains("additional notes", ignoreCase = true))
    }

    @Test
    fun `includes personal notes and introduces them`() {
        val instructions =
            sommelierInstructions(true, context(personal = listOf("Loved the 2019 Chablis", "Too oaky for me")))

        assertTrue(instructions.contains("personal notes", ignoreCase = true))
        assertTrue(instructions.contains("Loved the 2019 Chablis"))
        assertTrue(instructions.contains("Too oaky for me"))
        assertFalse(instructions.contains("additional notes", ignoreCase = true))
    }

    @Test
    fun `includes other context and introduces it as additional notes`() {
        val instructions = sommelierInstructions(true, context(other = listOf("Meursault is rich and nutty")))

        assertTrue(instructions.contains("additional notes", ignoreCase = true))
        assertTrue(instructions.contains("Meursault is rich and nutty"))
        assertFalse(instructions.contains("personal notes", ignoreCase = true))
    }

    @Test
    fun `includes both kinds of context together`() {
        val instructions =
            sommelierInstructions(true, context(personal = listOf("My own note"), other = listOf("A wine fact")))

        assertTrue(instructions.contains("My own note"))
        assertTrue(instructions.contains("A wine fact"))
    }

    @Test
    fun `keeps the notes in the order given`() {
        val instructions = sommelierInstructions(true, context(personal = listOf("first note", "second note")))

        assertTrue(instructions.indexOf("first note") < instructions.indexOf("second note"))
    }

    @Test
    fun `separates the base prompt and each kind of context with a blank line`() {
        val instructions = sommelierInstructions(true, context(personal = listOf("My own note"), other = listOf("A wine fact")))

        val paragraphs = instructions.split("\n\n")

        assertEquals(3, paragraphs.size)
        assertTrue(paragraphs[1].contains("A wine fact"))
        assertTrue(paragraphs[2].contains("My own note"))
    }

    @Test
    fun `has no blank lines when there is no context`() {
        assertFalse(sommelierInstructions(true, context = null).contains("\n\n"))
    }

    @Test
    fun `uses real newlines, not a backslash and an n`() {
        val instructions = sommelierInstructions(true, context(personal = listOf("My own note"), other = listOf("A wine fact")))

        assertFalse(instructions.contains("\\n"))
    }

    @Test
    fun `puts the context after the base prompt`() {
        val withContext = sommelierInstructions(true, context(personal = listOf("a note")))
        val without = sommelierInstructions(true, context = null)

        assertTrue(withContext.startsWith(without))
    }
}
