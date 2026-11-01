package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcRoundIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_cancel
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_hold_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_hold_to_record
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_record
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_recording_now
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_release_to_cancel
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_stop
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_transcribing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

/** The waveform with the elapsed time; announced once as "Recording" when it appears. */
@Composable
private fun WaveformWithTime(elapsedMs: Long, level: () -> Float, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val recording = stringResource(Res.string.chat_voice_recording_now)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        VoiceWaveform(
            level = level,
            color = c.textBase,
            emptyColor = c.textFaint,
            reducedMotion = reducedMotion,
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .semantics {
                    contentDescription = recording
                    liveRegion = LiveRegionMode.Polite
                }
                .testTag("chat_voice_waveform"),
        )
        Text(
            formatElapsed(elapsedMs),
            style = OcTheme.mono.codeSmall,
            color = c.textMuted,
            maxLines = 1,
            modifier = Modifier.testTag("chat_voice_elapsed"),
        )
    }
}

/** The message box while recording with a tap: cancel on one side, confirm on the other. */
@Composable
internal fun RecorderBar(
    elapsedMs: Long,
    level: () -> Float,
    reducedMotion: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chat_voice_recorder"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OcIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(Res.string.chat_voice_cancel),
            onClick = onCancel,
            modifier = Modifier.testTag("chat_voice_cancel"),
        )
        WaveformWithTime(elapsedMs, level, reducedMotion, Modifier.weight(1f).padding(horizontal = OcSpacing.xs))
        OcRoundIconButton(
            icon = Icons.Filled.Check,
            contentDescription = stringResource(Res.string.chat_voice_stop),
            onClick = onConfirm,
            container = c.bgAccent,
            content = c.onAccent,
            modifier = Modifier.testTag("chat_voice_confirm"),
        )
    }
}

/**
 * The text area while the microphone is held: the waveform, the time and what releasing does.
 * [cancelArmed] once the finger slid away: releasing then drops the recording.
 */
@Composable
internal fun HeldRecorderBar(elapsedMs: Long, level: () -> Float, reducedMotion: Boolean, cancelArmed: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = OcSpacing.sm, top = OcSpacing.xs, bottom = OcSpacing.xs)
            .testTag("chat_voice_recorder"),
    ) {
        WaveformWithTime(elapsedMs, level, reducedMotion, Modifier.fillMaxWidth())
        val hint by animateColorAsState(if (cancelArmed) c.danger.fg else c.textMuted, label = "voice_hold_hint")
        Text(
            stringResource(if (cancelArmed) Res.string.chat_voice_release_to_cancel else Res.string.chat_voice_hold_hint),
            style = MaterialTheme.typography.labelSmall,
            color = hint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("chat_voice_hold_hint"),
        )
    }
}

/** What the recorder becomes while the provider transcribes: a compact line in the same place. */
@Composable
internal fun TranscribingBar(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chat_voice_transcribing"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OcIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(Res.string.chat_voice_cancel),
            onClick = onCancel,
            modifier = Modifier.testTag("chat_voice_cancel"),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.CenterHorizontally),
        ) {
            OcSpinner()
            Text(
                stringResource(Res.string.chat_voice_transcribing),
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // Keeps the label centered in the box.
        Spacer(Modifier.size(48.dp))
    }
}

/**
 * The microphone with hold to record: a tap does what the plain button does ([onTap]); a long
 * press starts recording ([onHold]) until released ([onRelease] with whether the finger slid
 * further than [CANCEL_DISTANCE] away, which cancels). [onSlide] tells when that changes. The
 * gesture is handled here, so the visual button below must not be clickable itself; a screen
 * reader's double tap is [onTap].
 */
@Composable
internal fun HoldToRecordMic(
    recording: Boolean,
    transcribing: Boolean,
    levels: StateFlow<Float>?,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onSlide: (cancel: Boolean) -> Unit,
    onRelease: (cancel: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    val slide by rememberUpdatedState(onSlide)
    val release by rememberUpdatedState(onRelease)
    val label = stringResource(
        when {
            recording -> Res.string.chat_voice_stop
            transcribing -> Res.string.chat_voice_transcribing
            else -> Res.string.chat_voice_record
        },
    )
    val hint = stringResource(Res.string.chat_voice_hold_to_record)
    // Read only while drawing: the level does not recompose the button.
    val level = levels?.collectAsState()
    val container by animateColorAsState(if (recording) c.bgAccent else Color.Transparent, label = "hold_mic")
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (!recording && !transcribing) stateDescription = hint
                onClick {
                    tap()
                    true
                }
            }
            .pointerInput(transcribing) {
                if (transcribing) return@pointerInput
                val cancelDistance = CANCEL_DISTANCE.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var outcome = Press.Long
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        val up = waitForUpOrCancellation()
                        outcome = if (up != null) {
                            up.consume()
                            Press.Tap
                        } else {
                            Press.Cancelled
                        }
                    }
                    when (outcome) {
                        Press.Tap -> tap()
                        Press.Cancelled -> Unit
                        Press.Long -> {
                            hold()
                            var cancel = false
                            var ended = false
                            try {
                                while (!ended) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    if (change == null || change.changedToUpIgnoreConsumed()) {
                                        change?.consume()
                                        ended = true
                                    } else {
                                        change.consume()
                                        val away = (change.position - down.position).getDistance() > cancelDistance
                                        if (away != cancel) {
                                            cancel = away
                                            slide(cancel)
                                        }
                                    }
                                }
                            } catch (e: CancellationException) {
                                // The button left the screen mid-gesture: keep what was said.
                                release(false)
                                throw e
                            }
                            release(cancel)
                        }
                    }
                }
            }
            .testTag("chat_mic_button"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = container,
            contentColor = if (recording) c.onAccent else if (transcribing) c.textFaint else c.iconBase,
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    val scale = if (recording) 1f + (level?.value ?: 0f) * 0.15f else 1f
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (recording) Icons.Filled.Mic else Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private enum class Press { Tap, Long, Cancelled }

/** How far the finger slides from the microphone to cancel a held recording. */
private val CANCEL_DISTANCE = 80.dp
