package com.ryanthink.closbotkt.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryanthink.closbotkt.R

/** Stateful entry point: connects [ChatContent] to its [ChatViewModel]. */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatContent(state = state, onSendMessage = viewModel::onSendMessage, modifier = modifier)
}

/** Stateless chat UI: renders [state] and reports the user's intent through [onSendMessage]. */
@Composable
fun ChatContent(
    state: ChatUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The draft is UI element state, not screen state, so it stays here rather than in the ViewModel.
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Index of the last row: the greeting, then each message, then the waiting indicator if shown.
    val lastIndex = state.messages.size + if (state.isWaiting) 1 else 0
    LaunchedEffect(lastIndex) { listState.animateScrollToItem(lastIndex) }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MessageBubble(stringResource(R.string.chat_greeting), ChatSender.ASSISTANT) }
            items(state.messages) { message -> MessageBubble(message.text, message.sender) }
            if (state.isWaiting) {
                item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
            }
        }

        if (state.hasError) {
            Text(
                text = stringResource(R.string.chat_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_input_hint)) },
            )
            Button(
                onClick = {
                    onSendMessage(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank() && !state.isWaiting,
            ) {
                Text(stringResource(R.string.chat_send))
            }
        }
    }
}

@Composable
private fun MessageBubble(text: String, sender: ChatSender) {
    val isUser = sender == ChatSender.USER
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(text = text, modifier = Modifier.padding(12.dp))
        }
    }
}
