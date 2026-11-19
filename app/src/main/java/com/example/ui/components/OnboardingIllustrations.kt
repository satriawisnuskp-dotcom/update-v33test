package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WelcomeHeroIllustration(modifier: Modifier = Modifier.size(164.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.24f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.54f),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Gold coin peeking out of top of wallet
        val coinCenter = Offset(w * 0.46f, h * 0.27f)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFB45309))
            ),
            radius = w * 0.165f,
            center = coinCenter
        )
        drawCircle(
            color = Color(0xFFFEF3C7).copy(alpha = 0.65f),
            radius = w * 0.125f,
            center = coinCenter,
            style = Stroke(width = w * 0.015f)
        )

        // Wallet back body
        val walletTop = h * 0.26f
        val walletLeft = w * 0.16f
        val walletW = w * 0.66f
        val walletH = h * 0.46f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E3F33), Color(0xFF142B23))
            ),
            topLeft = Offset(walletLeft, walletTop),
            size = Size(walletW, walletH),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
        )

        // Wallet front leather cover
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF285544), Color(0xFF19382C)),
                start = Offset(walletLeft, walletTop + walletH * 0.14f),
                end = Offset(walletLeft + walletW, walletTop + walletH)
            ),
            topLeft = Offset(walletLeft, walletTop + walletH * 0.14f),
            size = Size(walletW, walletH * 0.86f),
            cornerRadius = CornerRadius(w * 0.085f, w * 0.085f)
        )

        // Wallet stitched border
        drawRoundRect(
            color = Color(0xFF4ADE80).copy(alpha = 0.28f),
            topLeft = Offset(walletLeft + w * 0.025f, walletTop + walletH * 0.18f),
            size = Size(walletW - w * 0.05f, walletH * 0.78f),
            cornerRadius = CornerRadius(w * 0.065f, w * 0.065f),
            style = Stroke(width = w * 0.01f)
        )

        // Wallet clasp flap on right
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF316652), Color(0xFF1F4738))
            ),
            topLeft = Offset(walletLeft + walletW * 0.62f, walletTop + walletH * 0.39f),
            size = Size(walletW * 0.41f, walletH * 0.26f),
            cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
        )
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = w * 0.026f,
            center = Offset(walletLeft + walletW * 0.72f, walletTop + walletH * 0.52f)
        )

        // Sprouting green leaves behind calendar bottom-right
        val leaf1 = Path().apply {
            moveTo(w * 0.73f, h * 0.71f)
            quadraticTo(w * 0.70f, h * 0.51f, w * 0.87f, h * 0.45f)
            quadraticTo(w * 0.86f, h * 0.63f, w * 0.73f, h * 0.71f)
            close()
        }
        drawPath(
            brush = Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF15803D))),
            path = leaf1
        )
        val leaf2 = Path().apply {
            moveTo(w * 0.76f, h * 0.71f)
            quadraticTo(w * 0.84f, h * 0.56f, w * 0.94f, h * 0.57f)
            quadraticTo(w * 0.89f, h * 0.70f, w * 0.76f, h * 0.71f)
            close()
        }
        drawPath(
            brush = Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF166534))),
            path = leaf2
        )

        // Mini 3D calendar card bottom-right
        val calLeft = w * 0.58f
        val calTop = h * 0.55f
        val calW = w * 0.28f
        val calH = h * 0.28f
        drawRoundRect(
            color = Color(0xFFE2EEFF),
            topLeft = Offset(calLeft, calTop),
            size = Size(calW, calH),
            cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))),
            topLeft = Offset(calLeft, calTop),
            size = Size(calW, calH * 0.30f),
            cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
        )
        // Calendar binder rings
        for (i in 0..2) {
            val rx = calLeft + calW * (0.24f + i * 0.26f)
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(rx, calTop - h * 0.02f),
                end = Offset(rx, calTop + h * 0.03f),
                strokeWidth = w * 0.018f,
                cap = StrokeCap.Round
            )
        }
        // Calendar grid dots
        for (row in 0..1) {
            for (col in 0..2) {
                val dx = calLeft + calW * (0.22f + col * 0.28f)
                val dy = calTop + calH * (0.52f + row * 0.24f)
                drawCircle(
                    color = if (row == 0 && col == 1) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                    radius = w * 0.018f,
                    center = Offset(dx, dy)
                )
            }
        }
    }
}

@Composable
fun Step3BillsIllustration(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Fanned green banknotes
        rotate(-14f, pivot = Offset(w * 0.5f, h * 0.55f)) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
                topLeft = Offset(w * 0.16f, h * 0.20f),
                size = Size(w * 0.62f, h * 0.36f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
        }
        rotate(6f, pivot = Offset(w * 0.5f, h * 0.55f)) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981))),
                topLeft = Offset(w * 0.22f, h * 0.24f),
                size = Size(w * 0.62f, h * 0.36f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
            drawCircle(
                color = Color(0xFF047857).copy(alpha = 0.45f),
                radius = w * 0.075f,
                center = Offset(w * 0.53f, h * 0.42f)
            )
        }

        // Stacked gold coins bottom-left
        for (i in 0..2) {
            drawOval(
                color = if (i == 2) Color(0xFFFBBF24) else Color(0xFFD97706),
                topLeft = Offset(w * 0.14f, h * (0.68f - i * 0.07f)),
                size = Size(w * 0.32f, h * 0.14f)
            )
        }

        // Mini calendar bottom-right
        drawRoundRect(
            color = Color(0xFFE2EEFF),
            topLeft = Offset(w * 0.52f, h * 0.52f),
            size = Size(w * 0.36f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            color = Color(0xFF3B82F6),
            topLeft = Offset(w * 0.52f, h * 0.52f),
            size = Size(w * 0.36f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
    }
}

@Composable
fun Step4PieChartIllustration(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = w * 0.10f
        val chartSize = Size(w - pad * 2f, h - pad * 2f)

        // Subtle 3D rim
        drawArc(
            color = Color(0xFF1D4ED8),
            startAngle = 20f,
            sweepAngle = 210f,
            useCenter = true,
            topLeft = Offset(pad, pad + h * 0.05f),
            size = chartSize
        )
        // Blue main slice
        drawArc(
            brush = Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
            startAngle = 110f,
            sweepAngle = 220f,
            useCenter = true,
            topLeft = Offset(pad, pad),
            size = chartSize
        )
        // Emerald slice slightly popped out
        drawArc(
            brush = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
            startAngle = -30f,
            sweepAngle = 82f,
            useCenter = true,
            topLeft = Offset(pad + w * 0.04f, pad),
            size = chartSize
        )
        // Amber slice
        drawArc(
            brush = Brush.linearGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))),
            startAngle = 54f,
            sweepAngle = 54f,
            useCenter = true,
            topLeft = Offset(pad + w * 0.02f, pad + h * 0.03f),
            size = chartSize
        )
    }
}

@Composable
fun Step5CoinJarIllustration(modifier: Modifier = Modifier.size(104.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Jar body shadow & glass tint
        val jarLeft = w * 0.22f
        val jarTop = h * 0.24f
        val jarW = w * 0.50f
        val jarH = h * 0.62f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF34D399).copy(alpha = 0.28f), Color(0xFF065F46).copy(alpha = 0.45f))
            ),
            topLeft = Offset(jarLeft, jarTop),
            size = Size(jarW, jarH),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )
        drawRoundRect(
            color = Color(0xFF6EE7B7).copy(alpha = 0.45f),
            topLeft = Offset(jarLeft, jarTop),
            size = Size(jarW, jarH),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = w * 0.018f)
        )

        // Jar lid
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF64748B))),
            topLeft = Offset(jarLeft + jarW * 0.16f, h * 0.14f),
            size = Size(jarW * 0.68f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Coins stacked inside jar
        val coinPositions = listOf(
            Offset(w * 0.36f, h * 0.72f),
            Offset(w * 0.52f, h * 0.74f),
            Offset(w * 0.44f, h * 0.62f),
            Offset(w * 0.56f, h * 0.60f),
            Offset(w * 0.39f, h * 0.52f),
            Offset(w * 0.49f, h * 0.43f)
        )
        coinPositions.forEach { center ->
            drawCircle(
                brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                radius = w * 0.075f,
                center = center
            )
            drawCircle(
                color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                radius = w * 0.048f,
                center = center,
                style = Stroke(width = w * 0.01f)
            )
        }

        // Glass highlight reflection
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(jarLeft + jarW * 0.14f, jarTop + jarH * 0.16f),
            end = Offset(jarLeft + jarW * 0.14f, jarTop + jarH * 0.80f),
            strokeWidth = w * 0.022f,
            cap = StrokeCap.Round
        )

        // Mini blue calendar card bottom-right
        val calLeft = w * 0.56f
        val calTop = h * 0.60f
        drawRoundRect(
            color = Color(0xFFE2EEFF),
            topLeft = Offset(calLeft, calTop),
            size = Size(w * 0.28f, h * 0.26f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRoundRect(
            color = Color(0xFF3B82F6),
            topLeft = Offset(calLeft, calTop),
            size = Size(w * 0.28f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
}

@Composable
fun CompletionCheckmarkBadge(modifier: Modifier = Modifier.size(116.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.48f
        val cy = h * 0.54f
        val outerR = w * 0.34f
        val innerR = w * 0.28f

        // Soft emerald glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF10B981).copy(alpha = 0.28f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.46f
            ),
            radius = w * 0.46f,
            center = Offset(cx, cy)
        )

        // Scalloped 10-point rosette badge
        val points = 20
        val sealPath = Path()
        for (i in 0 until points) {
            val angle = (i * 2.0 * PI / points) - PI / 2.0
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) sealPath.moveTo(px, py) else sealPath.lineTo(px, py)
        }
        sealPath.close()

        drawPath(
            brush = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
            path = sealPath
        )

        // Inner crisp circle
        drawCircle(
            color = Color(0xFF10B981),
            radius = w * 0.23f,
            center = Offset(cx, cy)
        )

        // Checkmark stroke
        val checkPath = Path().apply {
            moveTo(cx - w * 0.11f, cy + h * 0.005f)
            lineTo(cx - w * 0.025f, cy + h * 0.09f)
            lineTo(cx + w * 0.125f, cy - h * 0.075f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Golden sparkle top-right
        val sx = w * 0.80f
        val sy = h * 0.20f
        val sr = w * 0.085f
        val starPath = Path().apply {
            moveTo(sx, sy - sr)
            quadraticTo(sx, sy, sx + sr, sy)
            quadraticTo(sx, sy, sx, sy + sr)
            quadraticTo(sx, sy, sx - sr, sy)
            quadraticTo(sx, sy, sx, sy - sr)
            close()
        }
        drawPath(
            color = Color(0xFFFBBF24),
            path = starPath
        )
    }
}
