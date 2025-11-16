package com.example.yoodl.ui.pages.homepage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun VioletBlurBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000011))
    ) {

        // Top-left violet blob
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset((-80).dp, (-60).dp)
                .graphicsLayer { alpha = 0.45f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6830FF),
                            Color(0xA93E27FF),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom-right blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(120.dp, 550.dp)
                .graphicsLayer { alpha = 0.25f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6830FF),
                            Color(0xFF000047),
                            Color(0xFF000011)
                        )
                    ),
                    shape = CircleShape
                )
        )

    }
}
