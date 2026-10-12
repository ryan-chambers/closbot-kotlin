package com.ryanthink.closbotkt.feature.notes

private const val SEPARATOR = ","

/**
 * Turns the labels as typed in a form ("Chablis, Chardonnay") into a list: split on commas, trimmed, with blank and
 * repeated ones dropped.
 */
fun parseLabels(text: String): List<String> =
    text.split(SEPARATOR).map(String::trim).filter(String::isNotEmpty).distinct()

/** The reverse of [parseLabels]: the text to show in a form for a note's [labels]. */
fun formatLabels(labels: List<String>): String = labels.joinToString("$SEPARATOR ")
