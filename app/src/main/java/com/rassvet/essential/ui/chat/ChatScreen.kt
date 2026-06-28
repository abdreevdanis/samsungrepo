package com.rassvet.essential.ui.chat

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.rassvet.essential.R
import com.rassvet.essential.ui.components.ThinkingStatusRow
import com.rassvet.essential.ui.editor.MarkdownContent
import com.rassvet.essential.data.api.EssentialApiErrors
import com.rassvet.essential.data.chat.ChatEngine
import com.rassvet.essential.ui.chat.rememberChatHaptics
import com.rassvet.essential.data.local.AppDatabase
import com.rassvet.essential.data.local.VaultPreferencesRepository
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ChatMode { Local, Cloud }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    db: AppDatabase,
    prefs: VaultPreferencesRepository,
    chatEngine: ChatEngine,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeGguf by prefs.activeGgufName.collectAsState(initial = null)
    var mode by remember { mutableStateOf(ChatMode.Local) }
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var isThinking by remember { mutableStateOf(false) }
    var apiBase by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf<String?>(null) }
    var vaultUri by remember { mutableStateOf<String?>(null) }
    val cloudProvider by prefs.cloudLlmProvider.collectAsState(initial = "essential")
    val openAiBase by prefs.openAiCompatBaseUrl.collectAsState(initial = null)
    val openAiKey by prefs.openAiCompatApiKey.collectAsState(initial = null)
    val openAiModel by prefs.openAiCompatModel.collectAsState(initial = null)
    val geminiKey by prefs.geminiApiKey.collectAsState(initial = null)
    val geminiModel by prefs.geminiModel.collectAsState(initial = null)
    val semanticSearch by prefs.semanticSearchEnabled.collectAsState(initial = true)
    val streamReplies by prefs.streamReplies.collectAsState(initial = true)
    val chatHapticsEnabled by prefs.chatHapticsEnabled.collectAsState(initial = true)
    val chatHaptics = rememberChatHaptics(chatHapticsEnabled)
    val listState = rememberLazyListState()

    LaunchedEffect(prefs) {
        coroutineScope {
            launch { prefs.apiBaseUrl.collect { apiBase = it } }
            launch { prefs.authToken.collect { token = it } }
            launch { prefs.vaultTreeUri.collect { vaultUri = it } }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.size > 0) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == ChatMode.Local,
                    onClick = { mode = ChatMode.Local },
                    label = { Text(stringResource(R.string.chat_mode_local)) },
                )
                FilterChip(
                    selected = mode == ChatMode.Cloud,
                    onClick = { mode = ChatMode.Cloud },
                    label = { Text(stringResource(R.string.chat_mode_cloud)) },
                )
            }
            Text(
                text = if (mode == ChatMode.Cloud) {
                    when (cloudProvider) {
                        "openai_compat" -> stringResource(R.string.chat_hint_cloud_openai)
                        "gemini" -> stringResource(R.string.chat_hint_cloud_gemini)
                        else -> stringResource(R.string.chat_hint_cloud)
                    }
                } else {
                    stringResource(R.string.chat_hint_local)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages) { (role, content) ->
                    ChatMessageItem(role = role, content = content)
                }
            }
            if (isThinking) {
                ThinkingBubble()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.chat_message_placeholder)) },
                    shape = MaterialTheme.shapes.large,
                    enabled = !isThinking,
                )
                Button(
                    enabled = !isThinking,
                    onClick = {
                        val userText = input.trim()
                        if (userText.isEmpty()) return@Button
                        input = ""
                        chatHaptics.onMessageSent()
                        messages.add("user" to userText)
                        isThinking = true
                        scope.launch {
                            delay(16)
                            val thinkingStart = System.currentTimeMillis()
                            val assistantIdx = messages.size
                            messages.add("assistant" to "")
                            val history = messages
                                .take(assistantIdx - 1)
                                .map { (role, content) -> ChatEngine.HistoryLine(role == "user", content) }
                            val sb = StringBuilder()
                            val reply = try {
                                chatEngine.streamReply(
                                    ChatEngine.Request(
                                        userVisibleText = userText,
                                        queryForSearch = userText,
                                        history = history,
                                        activeModel = activeGguf,
                                        vaultStored = vaultUri,
                                        streamReplies = streamReplies,
                                        semanticSearchEnabled = semanticSearch,
                                        cloudMode = mode == ChatMode.Cloud,
                                        cloudProvider = cloudProvider,
                                        apiBase = apiBase,
                                        authToken = token,
                                        openAiBase = openAiBase,
                                        openAiKey = openAiKey,
                                        openAiModel = openAiModel,
                                        geminiKey = geminiKey,
                                        geminiModel = geminiModel,
                                    ),
                                ) { chunk ->
                                    if (streamReplies) sb.append(chunk) else sb.setLength(0).also { sb.append(chunk) }
                                    messages[assistantIdx] = "assistant" to sb.toString()
                                }.text
                            } catch (e: Exception) {
                                EssentialApiErrors.chatMessage(context, e)
                            }
                            val elapsed = System.currentTimeMillis() - thinkingStart
                            if (elapsed < 800L) delay(800L - elapsed)
                            chatHaptics.onGenerationComplete()
                            isThinking = false
                            messages[assistantIdx] = "assistant" to reply.ifBlank {
                                context.getString(
                                    R.string.chat_error_cloud,
                                    context.getString(R.string.chat_error_no_response),
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.chat_send))
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(role: String, content: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isAssistant = role == "assistant"

    val bubbleBg = if (isAssistant)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val textColor = if (isAssistant)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isAssistant) Alignment.TopStart else Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isAssistant) Modifier.fillMaxWidth(0.96f)
                    else Modifier.widthIn(max = 300.dp),
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomEnd = if (isAssistant) 16.dp else 4.dp,
                        bottomStart = if (isAssistant) 4.dp else 16.dp,
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (isAssistant) {
                MarkdownContent(
                    text = content,
                    baseColor = textColor,
                    mutedColor = textColor.copy(alpha = 0.62f),
                )
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
            if (isAssistant && content.isNotBlank()) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(content))
                        Toast.makeText(context, R.string.chat_copied, Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.chat_copy),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val bubbleBg = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
            .background(bubbleBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        ThinkingStatusRow(
            color = textColor.copy(alpha = pulseAlpha),
            style = MaterialTheme.typography.bodyMedium,
            logoSize = 30.dp,
            spacing = 8.dp,
        )
    }
}

@Composable
private fun MorphBlob(
    phase: Float,
    color1: androidx.compose.ui.graphics.Color,
    color2: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = minOf(size.width, size.height) * 0.4f
        val n = 8

        val pts = Array(n) { i ->
            val angle = (2.0 * PI * i / n).toFloat()
            val warp = sin(phase + i * 1.3f) * base * 0.28f +
                cos(phase * 0.65f + i * 2.0f) * base * 0.14f
            val r = base + warp
            Offset(cx + r * cos(angle), cy + r * sin(angle))
        }

        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n) {
            val prev = pts[(i - 1 + n) % n]
            val curr = pts[i]
            val next = pts[(i + 1) % n]
            val next2 = pts[(i + 2) % n]
            path.cubicTo(
                curr.x + (next.x - prev.x) / 5f,
                curr.y + (next.y - prev.y) / 5f,
                next.x - (next2.x - curr.x) / 5f,
                next.y - (next2.y - curr.y) / 5f,
                next.x,
                next.y,
            )
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(color1, color2),
                center = Offset(cx, cy),
                radius = base * 1.5f,
            ),
        )
    }
}


