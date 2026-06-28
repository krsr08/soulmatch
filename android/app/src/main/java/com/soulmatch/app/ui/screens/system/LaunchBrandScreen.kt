package com.soulmatch.app.ui.screens.system

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.soulmatch.app.R
import com.soulmatch.app.ui.design.SoulMatchTokens

@Composable
fun LaunchBrandScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_content),
            contentDescription = "SoulMatch",
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
