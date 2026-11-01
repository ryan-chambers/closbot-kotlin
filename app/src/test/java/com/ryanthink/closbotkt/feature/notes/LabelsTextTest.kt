package com.ryanthink.closbotkt.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsTextTest {

    @Test
    fun `parsing splits on commas and trims`() {
        assertEquals(listOf("Chablis", "Chardonnay"), parseLabels("  Chablis ,Chardonnay  "))
    }

    @Test
    fun `parsing drops blank and repeated labels`() {
        assertEquals(listOf("Chablis", "Merlot"), parseLabels("Chablis,, ,Merlot,Chablis"))
    }

    @Test
    fun `parsing nothing gives no labels`() {
        assertTrue(parseLabels("").isEmpty())
        assertTrue(parseLabels(" , ").isEmpty())
    }

    @Test
    fun `formatting joins labels with a comma and a space`() {
        assertEquals("Chablis, Chardonnay", formatLabels(listOf("Chablis", "Chardonnay")))
        assertEquals("", formatLabels(emptyList()))
    }

    @Test
    fun `formatted labels parse back to the same list`() {
        val labels = listOf("Chablis", "Premier Cru")

        assertEquals(labels, parseLabels(formatLabels(labels)))
    }
}
