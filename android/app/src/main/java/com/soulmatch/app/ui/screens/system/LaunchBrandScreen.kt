package com.soulmatch.app.ui.screens.system

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.soulmatch.app.R

@Composable
fun LaunchBrandScreen() {
    Image(
        painter = painterResource(id = R.drawable.splash_content),
        contentDescription = "SoulMatch",
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentScale = ContentScale.FillBounds
    )
}
