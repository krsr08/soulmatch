package com.soulmatch.app.ui.screens.system

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.soulmatch.app.R
import com.soulmatch.app.ui.design.SoulMatchTokens

private const val SplashDesignWidth = 390f
private const val SplashDesignHeight = 844f

@Composable
fun LaunchBrandScreen() {
    val progressTarget = remember { 1f }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 3000),
        label = "splash-progress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_content),
            contentDescription = "SoulMatch",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .offset(
                    x = (maxWidth.value * 128f / SplashDesignWidth).dp,
                    y = (maxHeight.value * 693f / SplashDesignHeight).dp
                )
                .size(
                    width = (maxWidth.value * 134f / SplashDesignWidth).dp,
                    height = (maxHeight.value * 10f / SplashDesignHeight).dp
                ),
            color = SoulMatchTokens.Tangerine,
            trackColor = Color.Transparent
        )
    }
}
