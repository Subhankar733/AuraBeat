package com.subho.aurabeat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isplaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF7C4DFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    // এনিমেশনের জন্য ফেজ ভ্যালু
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barsCount = 20

    Canvas(modifier = modifier.height(50.dp).fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / (barsCount * 1.5f)
        val spacing = barWidth * 0.5f

        for (i in 0 until barsCount) {
            val heightMultiplier = if (isplaying) {
                // সাইন ওয়েভ ও ফেজ দিয়ে ডায়নামিক উচ্চতা তৈরি করা
                (sin((i + phase) * 0.3f) + 1f) * 0.5f * 0.8f + 0.2f
            } else {
                0.1f // পজ থাকলে বারগুলো ছোট হয়ে থাকবে
            }
            
            val barHeight = canvasHeight * heightMultiplier
            val x = i * (barWidth + spacing) + spacing
            val y = canvasHeight - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
