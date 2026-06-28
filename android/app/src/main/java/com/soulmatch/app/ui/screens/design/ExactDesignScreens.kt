package com.soulmatch.app.ui.screens.design

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val DesignWidth = 390f
private const val DesignHeight = 844f

data class DesignHotspot(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val onClick: () -> Unit
)

@Composable
fun ExactDesignScreen(
    assetName: String,
    hotspots: List<DesignHotspot> = emptyList(),
    overlay: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val image = remember(assetName) {
        context.assets.open("design/screens/$assetName").use { input ->
            BitmapFactory.decodeStream(input).asImageBitmap()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        hotspots.forEach { hotspot ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (maxWidth.value * hotspot.left / DesignWidth).dp,
                        y = (maxHeight.value * hotspot.top / DesignHeight).dp
                    )
                    .size(
                        width = (maxWidth.value * hotspot.width / DesignWidth).dp,
                        height = (maxHeight.value * hotspot.height / DesignHeight).dp
                    )
                    .clickable(onClick = hotspot.onClick)
            )
        }
        overlay()
    }
}

fun backHotspot(onBack: () -> Unit) = DesignHotspot(0f, 34f, 84f, 58f, onBack)

fun bottomNavHotspots(
    onHome: () -> Unit,
    onMatches: () -> Unit,
    onInterests: () -> Unit,
    onMessages: () -> Unit,
    onAccount: () -> Unit
) = listOf(
    DesignHotspot(0f, 758f, 78f, 86f, onHome),
    DesignHotspot(78f, 758f, 78f, 86f, onMatches),
    DesignHotspot(156f, 758f, 78f, 86f, onInterests),
    DesignHotspot(234f, 758f, 78f, 86f, onMessages),
    DesignHotspot(312f, 758f, 78f, 86f, onAccount)
)

fun profileCardHotspots(
    onInterest: () -> Unit,
    onViewProfile: () -> Unit
) = listOf(
    DesignHotspot(226f, 260f, 70f, 38f, onInterest),
    DesignHotspot(300f, 260f, 76f, 38f, onViewProfile),
    DesignHotspot(226f, 448f, 70f, 38f, onInterest),
    DesignHotspot(300f, 448f, 76f, 38f, onViewProfile),
    DesignHotspot(226f, 646f, 70f, 38f, onInterest),
    DesignHotspot(300f, 646f, 76f, 38f, onViewProfile)
)
