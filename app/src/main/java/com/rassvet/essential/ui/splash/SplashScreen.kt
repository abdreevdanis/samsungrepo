package com.rassvet.essential.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rassvet.essential.ui.components.EssentialLogo
import com.rassvet.essential.ui.theme.EssentialBrand
import com.rassvet.essential.ui.theme.EssentialDisplayFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LogoSize = 192.dp

@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlineProgress = remember { Animatable(0f) }
    val crackProgresses = remember { EssentialLogo.CRACKS.map { Animatable(0f) } }
    val titleAlpha = remember { Animatable(0f) }
    val titleAccent = remember { Animatable(0f) }

    val outline = remember { EssentialLogo.parsePath(EssentialLogo.OUTLINE) }
    val cracks = remember { EssentialLogo.CRACKS.map { EssentialLogo.parsePath(it) } }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val logoStroke = EssentialLogo.defaultStroke

    LaunchedEffect(Unit) {
        outlineProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
        crackProgresses.forEach { progress ->
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                )
            }
            delay(240)
        }
        delay(280)
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        )
        titleAccent.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        )
        delay(320)
        onAnimationComplete()
    }

    val titleColor =
        lerp(onSurface, EssentialBrand, titleAccent.value)
            .copy(alpha = titleAlpha.value)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(LogoSize)) {
                val scale = size.minDimension / EssentialLogo.VIEWPORT
                val outlineStyle = Stroke(
                    width = EssentialLogo.OUTLINE_STROKE_VP * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val crackStyle = Stroke(
                    width = EssentialLogo.CRACK_STROKE_VP * scale,
                    cap = StrokeCap.Butt,
                    join = StrokeJoin.Round,
                )
                withTransform({
                    translate(size.width / 2f, size.height / 2f)
                    scale(scale, scale, pivot = Offset.Zero)
                    translate(-EssentialLogo.VIEWPORT / 2f, -EssentialLogo.VIEWPORT / 2f)
                }) {
                    if (outlineProgress.value < 1f) {
                        drawPartialPath(outline, outlineProgress.value, logoStroke, outlineStyle)
                    } else {
                        clipPath(outline) {
                            cracks.forEachIndexed { index, path ->
                                drawPartialPath(
                                    path,
                                    crackProgresses[index].value,
                                    logoStroke,
                                    crackStyle,
                                )
                            }
                        }
                        drawPath(outline, logoStroke, style = outlineStyle)
                    }
                }
            }
            Text(
                text = "Essential",
                color = titleColor,
                style = TextStyle(
                    fontFamily = EssentialDisplayFontFamily,
                    fontSize = 32.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.4).sp,
                ),
                modifier = Modifier.offset(y = (-22).dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPartialPath(
    path: Path,
    progress: Float,
    color: Color,
    style: Stroke,
) {
    if (progress <= 0f) return
    val measure = PathMeasure()
    measure.setPath(path, false)
    val length = measure.length
    if (length <= 0f) return
    val segment = Path()
    measure.getSegment(0f, length * progress.coerceIn(0f, 1f), segment, true)
    drawPath(segment, color, style = style)
}


