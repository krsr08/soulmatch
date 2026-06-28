package com.soulmatch.app.ui.screens.system

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import com.soulmatch.app.R
import com.soulmatch.app.ui.design.SoulMatchTokens

@Composable
fun LaunchBrandScreen() {
    val progressTarget = remember { 1f }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 3000),
        label = "splash-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.1f))
            Image(
                painter = painterResource(id = R.drawable.app_icon_splash),
                contentDescription = "SoulMatch",
                modifier = Modifier.size(280.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "SoulMatch",
                color = SoulMatchTokens.Tangerine,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .height(10.dp),
                color = SoulMatchTokens.Tangerine,
                trackColor = SoulMatchTokens.Border
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading profiles...",
                color = SoulMatchTokens.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
