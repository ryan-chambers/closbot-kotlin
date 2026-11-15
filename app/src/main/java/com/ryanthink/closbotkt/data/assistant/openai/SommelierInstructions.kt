package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.knowledge.WineContext

/**
 * Builds the system instructions sent with a chat message.
 *
 * Ported from `createChatSystemPrompt` in the old app's `prompt.service.ts` (English only). The pieces, in order:
 * 1. The base prompt. It always says the assistant is a sommelier. When [burgundyFocus] is on it says the expertise is
 *    in wine "from Burgundy"; when off it leaves that out (the old prompt was "expertise in wine {{fromWhere}}").
 * 2. If [context] has other context, a paragraph saying the assistant has access to the user's additional notes (the
 *    old wording: "You have access to some of my additional notes: ... Make sure to include it if relevant. Also be
 *    sure to mention the source."), followed by every item's content.
 * 3. If [context] has personal notes, a paragraph saying these are the user's personal notes and to use them if
 *    appropriate, followed by every item's content.
 *
 * A section with nothing to show is left out entirely, so a null [context] and one with two empty lists give the same
 * result. This function does not decide whether to look anything up; it only formats what it is given.
 */
internal fun sommelierInstructions(burgundyFocus: Boolean, context: WineContext?): String =
    TODO("Build the instructions as described above")
