package com.gregoryhpotter.textlistscanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gregoryhpotter.textlistscanner.R

@Composable
fun ScanningMascot(
    modifier: Modifier = Modifier,
) {
    val mascotDescription = stringResource(R.string.scanning_mascot_description)
    val textMeasurer = rememberTextMeasurer()
    
    Box(
        modifier = modifier
            .size(240.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = mascotDescription
            }
            .clip(CircleShape)
            .background(Color(0xFF1A232E)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            val cream = Color(0xFFFDEFD0)
            val brown = Color(0xFF8D6E63)
            val hatColor = Color(0xFF8B735B)
            val teal = Color(0xFF4DB6AC)
            val white = Color.White

            fun DrawScope.drawMascotShapes(isOutline: Boolean) {
                val strokeWidth = if (isOutline) 14.dp.toPx() else 0f
                val drawStyle = if (isOutline) Stroke(width = strokeWidth, join = StrokeJoin.Round, cap = StrokeCap.Round) else Fill
                val colorOverride = if (isOutline) white else null

                // Cape/Body
                val capePath = Path().apply {
                    moveTo(w * 0.25f, h * 0.85f)
                    quadraticTo(w * 0.35f, h * 0.65f, w * 0.55f, h * 0.68f)
                    lineTo(w * 0.72f, h * 0.72f)
                    lineTo(w * 0.78f, h * 0.92f)
                    close()
                }
                drawPath(capePath, colorOverride ?: hatColor, style = drawStyle)

                // Head
                val headPath = Path().apply {
                    addOval(Rect(w * 0.38f, h * 0.38f, w * 0.68f, h * 0.72f))
                }
                drawPath(headPath, colorOverride ?: cream, style = drawStyle)

                // Ear
                val earPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.48f)
                    quadraticTo(w * 0.28f, h * 0.52f, w * 0.32f, h * 0.78f)
                    quadraticTo(w * 0.38f, h * 0.88f, w * 0.48f, h * 0.68f)
                    close()
                }
                drawPath(earPath, colorOverride ?: brown, style = drawStyle)

                // Hat
                val hatPath = Path().apply {
                    moveTo(w * 0.32f, h * 0.48f)
                    quadraticTo(w * 0.53f, h * 0.28f, w * 0.74f, h * 0.48f)
                    lineTo(w * 0.79f, h * 0.53f)
                    quadraticTo(w * 0.53f, h * 0.58f, w * 0.27f, h * 0.53f)
                    close()
                }
                drawPath(hatPath, colorOverride ?: hatColor, style = drawStyle)
            }

            // Mascot Outline
            drawMascotShapes(isOutline = true)
            
            // Magnifying Glass Center & Dimensions
            val glassCenter = Offset(w * 0.72f, h * 0.48f)
            val glassRadius = w * 0.22f
            val handleEnd = glassCenter + Offset(glassRadius * 1.1f, glassRadius * 2.1f)

            // Glass Handle Outline
            drawLine(
                color = white,
                start = glassCenter + Offset(glassRadius * 0.6f, glassRadius * 0.9f),
                end = handleEnd,
                strokeWidth = 26.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Glass Rim Outline
            drawCircle(
                color = white,
                radius = glassRadius,
                center = glassCenter,
                style = Stroke(width = 22.dp.toPx())
            )

            // Mascot Content
            drawMascotShapes(isOutline = false)

            // Magnifying Glass Handle
            drawLine(
                color = teal,
                start = glassCenter + Offset(glassRadius * 0.6f, glassRadius * 0.9f),
                end = handleEnd,
                strokeWidth = 14.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Magnifying Glass Rim
            drawCircle(
                color = teal,
                radius = glassRadius,
                center = glassCenter,
                style = Stroke(width = 10.dp.toPx())
            )

            // Scanning Frame
            val frameSize = glassRadius * 0.6f
            val cornerLen = frameSize * 0.35f
            val fOff = frameSize / 2f
            
            val dirs = listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f))
            dirs.forEach { d ->
                val cornerPos = glassCenter + Offset(d.x * fOff, d.y * fOff)
                drawLine(white.copy(alpha = 0.9f), cornerPos, cornerPos + Offset(0f, -d.y * cornerLen), 3.dp.toPx(), StrokeCap.Round)
                drawLine(white.copy(alpha = 0.9f), cornerPos, cornerPos + Offset(-d.x * cornerLen, 0f), 3.dp.toPx(), StrokeCap.Round)
            }

            // 'W' marker
            drawCircle(white, radius = glassRadius * 0.28f, center = glassCenter, style = Stroke(width = 2.dp.toPx()))

            val textLayoutResult = textMeasurer.measure(
                text = "W",
                style = TextStyle(
                    color = white,
                    fontSize = (glassRadius * 0.35f).toSp(),
                    fontWeight = FontWeight.Bold,
                ),
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = glassCenter - Offset(
                    textLayoutResult.size.width / 2f,
                    textLayoutResult.size.height / 2f,
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun ScanningMascotPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        ScanningMascot()
    }
}
