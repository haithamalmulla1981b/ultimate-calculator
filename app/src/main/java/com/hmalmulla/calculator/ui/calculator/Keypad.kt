package com.hmalmulla.calculator.ui.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hmalmulla.calculator.data.HistoryEntry
import com.hmalmulla.calculator.engine.NumberFormatter
import com.hmalmulla.calculator.ui.theme.LocalSettings

enum class KeyKind { NUMBER, OPERATOR, FUNCTION, EQUALS, CLEAR }

/** Feedback (sound and vibration) according to settings. */
@Composable
fun rememberKeyFeedback(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val s = LocalSettings.current
    return remember(s.haptics, s.sound) {
        {
            if (s.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (s.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.CalcKey(
    label: String,
    kind: KeyKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    description: String = label,
    onLongClick: (() -> Unit)? = null,
    longLabel: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (kind) {
        KeyKind.NUMBER -> c.surfaceVariant to c.onSurface
        KeyKind.OPERATOR -> c.primaryContainer to c.onPrimaryContainer
        KeyKind.FUNCTION -> c.secondaryContainer to c.onSecondaryContainer
        KeyKind.EQUALS -> c.primary to c.onPrimary
        KeyKind.CLEAR -> c.error.copy(alpha = 0.18f) to c.error
    }
    val feedback = rememberKeyFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "key")
    val sizeScale = LocalSettings.current.buttonSize.scale
    Box(
        modifier = modifier
            .weight(weight)
            .fillMaxSize()
            .padding(4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = { feedback(); onClick() },
                onLongClick = onLongClick?.let { lc -> { feedback(); lc() } },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content() else {
            val base = if (kind == KeyKind.NUMBER) 26 else if (label.length > 3) 15 else 22
            Text(
                label,
                color = fg,
                fontSize = (base * sizeScale).sp,
                fontWeight = if (kind == KeyKind.NUMBER) FontWeight.Normal else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (longLabel != null) {
            Text(
                longLabel,
                color = fg.copy(alpha = 0.55f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 9.dp),
            )
        }
    }
}

@Composable
fun BackspaceKey(scope: RowScope, onClick: () -> Unit, onLongClick: () -> Unit) {
    with(scope) {
        CalcKey("⌫", KeyKind.FUNCTION, onClick, description = "Delete", onLongClick = onLongClick) {
            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Result", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(i, "Share calculation"))
}

/** Expression, live preview / result, error, memory indicator and quick actions. */
@Composable
fun CalcDisplay(
    state: CalcUiState,
    recent: List<HistoryEntry>,
    onRecent: (HistoryEntry) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAngle: Boolean = false,
    onAngle: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val c = MaterialTheme.colorScheme
    val exprScroll = rememberScrollState()
    LaunchedEffect(state.expression) { exprScroll.scrollTo(exprScroll.maxValue) }
    val shownExpr = if (settings.grouping) NumberFormatter.groupExpression(state.expression) else state.expression

    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (state.memory.isNotEmpty()) {
                Surface(color = c.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "M  " + NumberFormatter.format(state.memory.first(), settings.decimals, settings.grouping),
                        color = c.onPrimaryContainer, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp).semantics { contentDescription = "Memory has a value" },
                    )
                }
            }
            if (showAngle) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = c.secondaryContainer, shape = RoundedCornerShape(8.dp),
                    onClick = onAngle,
                ) {
                    Text(
                        state.angle.name, color = c.onSecondaryContainer, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            .semantics { contentDescription = "Angle unit ${state.angle.name}, tap to switch" },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (state.evaluated && state.result != null) {
                IconButton(onClick = { copyToClipboard(context, state.result.replace(",", "")) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy result")
                }
                IconButton(onClick = { onShare("${state.expression} = ${state.result}") }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share calculation")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = shownExpr.ifEmpty { "0" },
            style = if (state.evaluated) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
            color = if (state.evaluated) c.onSurfaceVariant else c.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(exprScroll).semantics { contentDescription = "Expression ${state.expression}" },
        )
        val second = when {
            state.error != null -> state.error
            state.evaluated -> state.result ?: ""
            else -> state.preview
        }
        Text(
            text = second,
            style = if (state.evaluated && state.error == null) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
            color = when {
                state.error != null -> c.error
                state.evaluated -> c.primary
                else -> c.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickableCopy(state.result?.takeIf { state.evaluated }) { copyToClipboard(context, it.replace(",", "")) }
                .semantics { contentDescription = if (state.error != null) "Error: $second" else "Result $second" },
        )
        AnimatedVisibility(visible = recent.isNotEmpty() && state.expression.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                recent.forEach { r ->
                    AssistChip(onClick = { onRecent(r) }, label = { Text(r.result, maxLines = 1) })
                }
            }
        }
    }
}

/** Tap the result to copy it. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCopy(text: String?, onCopy: (String) -> Unit): Modifier =
    if (text == null) this else this.combinedClickable(onClick = { onCopy(text) }, onClickLabel = "Copy result")

@Composable
fun MemoryRow(vm: CalculatorViewModel, height: androidx.compose.ui.unit.Dp) {
    Row(Modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp)) {
        val mem = listOf(
            Triple("MC", "Memory clear") { vm.memoryClear() },
            Triple("MR", "Memory recall") { vm.memoryRecall() },
            Triple("M+", "Memory add") { vm.memoryAdd() },
            Triple("M−", "Memory subtract") { vm.memorySubtract() },
            Triple("MS", "Memory store") { vm.memoryStore() },
        )
        mem.forEach { (l, d, a) -> MemKey(l, d, a) }
    }
}

@Composable
private fun RowScope.MemKey(label: String, description: String, onClick: () -> Unit) {
    val feedback = rememberKeyFeedback()
    Box(
        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(12.dp))
            .combinedClickableMem(onClick = { feedback(); onClick() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableMem(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)

val TransparentColor = Color.Transparent
