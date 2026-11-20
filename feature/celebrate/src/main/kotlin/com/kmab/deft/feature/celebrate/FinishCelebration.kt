@file:Suppress("MagicNumber") // Approved celebration timeline and Canvas geometry (visual design section 8.1).

package com.kmab.deft.feature.celebrate

import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.designsystem.CoverImage
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import java.io.File
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/** A one-shot finish moment; saved phase/elapsed time never replay an already shown animation on rotation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishCelebration(
    bookId: Long,
    title: String,
    author: String,
    activeSeconds: Long,
    sessions: Int,
    days: Int,
    enabled: Boolean,
    rating: Double?,
    favorite: Boolean,
    onRating: (Double?) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    animationsEnabled: Boolean = ValueAnimator.areAnimatorsEnabled(),
    coverPath: String? = null,
    busy: Boolean = false,
) {
    var phase by rememberSaveable(bookId) {
        mutableStateOf(
            if (enabled &&
                animationsEnabled
            ) {
                "animation"
            } else {
                "rating"
            },
        )
    }
    var elapsed by rememberSaveable(bookId) { mutableFloatStateOf(0f) }
    val progress = remember(bookId) { Animatable(elapsed) }
    val view = LocalView.current
    var hapticPlayed by rememberSaveable(bookId) { mutableStateOf(false) }
    LaunchedEffect(phase, enabled, animationsEnabled) {
        if (phase != "animation") return@LaunchedEffect
        if (!enabled || !animationsEnabled) {
            phase = "rating"
            return@LaunchedEffect
        }
        progress.animateTo(
            FINISH_DURATION,
            tween((FINISH_DURATION - elapsed).toInt().coerceAtLeast(1), easing = LinearEasing),
        ) {
            elapsed = value
            if (value >= 750 && !hapticPlayed) {
                hapticPlayed = true
                view.performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >=
                        30
                    ) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.CONTEXT_CLICK
                    },
                )
            }
        }
        phase = "rating"
    }
    if (phase == "animation") {
        Dialog(onDismissRequest = {
            phase = "rating"
        }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            val t = progress.value
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .clickable(role = Role.Button) { phase = "rating" }
                    .semantics { contentDescription = "skip celebration" },
                contentAlignment = Alignment.Center,
            ) {
                val exit = fraction(t, 2300f, 2800f)
                val compactCover = maxHeight < 500.dp
                Canvas(
                    Modifier.fillMaxSize().graphicsLayer { translationX = size.width * exit },
                ) { finishFields(bookId, t) }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).graphicsLayer {
                        translationX = size.width * exit
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val rise = fraction(t, 500f, 1000f)
                    CelebrationCover(
                        title,
                        author,
                        coverPath,
                        Modifier
                            .width(
                                if (compactCover) {
                                    88.dp
                                } else {
                                    132.dp
                                },
                            ).graphicsLayer {
                                alpha = rise
                                translationY = (1 - rise) * 72.dp.toPx()
                                rotationZ = (1 - rise) * -8f
                                scaleX = .85f + .15f * rise
                                scaleY = scaleX
                                shadowElevation = 16.dp.toPx() * rise
                            },
                    )
                    Text(
                        "finished.",
                        style = DeftTypography.celebrate,
                        color = Ink,
                        modifier =
                            Modifier.graphicsLayer {
                                alpha = fraction(t, 850f, 1300f)
                                translationY =
                                    (1 - alpha) * 18.dp.toPx()
                            },
                    )
                    val count = fraction(t, 1000f, 1900f)
                    FinishCounters(
                        (activeSeconds * count).toLong(),
                        (sessions * count).toInt(),
                        (days * count).toInt(),
                        Modifier.graphicsLayer { alpha = fraction(t, 1000f, 1150f) },
                    )
                }
            }
        }
    } else {
        val c = LocalDeftColors.current
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = DeftShapes.sheet,
            containerColor = c.surface,
            contentColor = c.text,
            dragHandle = null,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("finish_sheet")
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("finished.", Modifier.weight(1f), style = DeftTypography.headline, color = c.finishedText)
                    IconButton(onDismiss) { Icon(DeftActionIcons.Close, "close popup") }
                }
                Text(title, style = DeftTypography.body)
                Text("how was it?", style = DeftTypography.title)
                AssessmentControls(rating, favorite, onRating, onFavorite, !busy)
                DeftButton("done", onDismiss, Modifier.fillMaxWidth(), icon = DeftActionIcons.Check)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinishCounters(
    seconds: Long,
    sessions: Int,
    days: Int,
    modifier: Modifier,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            (if (seconds >= 3600) "${seconds / 3600}h ${seconds % 3600 / 60}m" else "${seconds / 60}m") to "time read",
            sessions.toString() to "sessions",
            days.toString() to "days",
        ).forEach { (value, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = DeftTypography.statMedium, color = Ink)
                Text(label, style = DeftTypography.mono, color = Ink)
            }
        }
    }
}

@Composable
private fun CelebrationCover(
    title: String,
    author: String,
    path: String?,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).diskCache(null).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    val file =
        remember(path) {
            path
                ?.let { runCatching { File(it).canonicalFile }.getOrNull() }
                ?.takeIf { it.isAbsolute && it.toPath().startsWith(context.filesDir.canonicalFile.toPath()) }
        }
    CoverImage(title, author, modifier) {
        GeneratedCover(title, author)
        if (file != null) {
            AsyncImage(
                file,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.finishFields(
    seed: Long,
    time: Float,
) {
    fun field(
        color: Color,
        start: Float,
        end: Float,
    ) {
        val leading = fraction(time, start, end) * (size.width + size.height * .22f)
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(leading, 0f)
                lineTo(leading - size.height * .22f, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(path, color)
    }
    field(Purple, 0f, 380f)
    field(Green, 300f, 850f)
    val random = Random(seed)
    val speed = fraction(time, 250f, 1200f)
    repeat(18) {
        val y = random.nextFloat() * size.height
        val length = 24f + random.nextFloat() * size.width * .25f
        val x = size.width * (1.3f - speed * 2) + random.nextFloat() * size.width * .4f
        if (speed > 0f &&
            speed < 1f
        ) {
            drawRect(
                (
                    if (it % 2 ==
                        0
                    ) {
                        Ink
                    } else {
                        Paper
                    }
                ).copy(alpha = (1 - speed) * .45f),
                Offset(x, y),
                Size(length, 2.dp.toPx()),
            )
        }
    }
    val burst = fraction(time, 750f, 1450f)
    repeat(30) {
        val angle = random.nextDouble() * Math.PI * 2
        val radius = (60f + random.nextFloat() * 180f) * burst
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val from = center + direction * radius.dp.toPx()
        if (burst > 0f && burst < 1f) {
            drawLine(
                listOf(Purple, Ink, Amber)[it % 3].copy(alpha = 1 - burst),
                from,
                from + direction * (8f + random.nextFloat() * 14f).dp.toPx(),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }
}

private fun fraction(
    time: Float,
    start: Float,
    end: Float,
): Float = ((time - start) / (end - start)).coerceIn(0f, 1f)

private const val FINISH_DURATION = 2800f
private val Ink = Color(0xFF161619)
private val Paper = Color(0xFFF0EDE8)
private val Purple = Color(0xFFC373FF)
private val Green = Color(0xFF3DE08A)
private val Amber = Color(0xFFF5C451)
