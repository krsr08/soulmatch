package com.soulmatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.soulmatch.app.data.config.mediaUrl
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.TextSecondary

@Composable
fun MemberPhoto(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolvedUrl = mediaUrl(photoUrl)
    if (resolvedUrl == null) {
        MemberPhotoPlaceholder(
            contentDescription = contentDescription,
            modifier = modifier,
            shape = shape
        )
        return
    }

    SubcomposeAsyncImage(
        model = resolvedUrl,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = {
            MemberPhotoPlaceholder(
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                shape = shape
            )
        },
        error = {
            MemberPhotoPlaceholder(
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                shape = shape
            )
        }
    )
}

@Composable
fun MemberPhotoPlaceholder(
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(SurfaceSoft),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Divider.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = contentDescription,
                tint = TextSecondary,
                modifier = Modifier.padding(13.dp)
            )
        }
    }
}
