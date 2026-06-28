package com.soulmatch.app.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SoulMatchTokens {
    val Bg = Color(0xFFFFFFFF)
    val Card = Color(0xFFFFFFFF)
    val Border = Color(0xFFF3F4F6)
    val Text = Color(0xFF1A1A1A)
    val Muted = Color(0xFF888888)
    val Tangerine = Color(0xFFFF5C00)
    val TangerineSoft = Color(0xFFFFF1E8)
    val Gold = Color(0xFFFF8533)
    val GoldSoft = Color(0xFFFFD5BD)
    val Ivory = Color(0xFFF7F8FA)
    val Success = Color(0xFF218764)
    val Error = Color(0xFFD24B5D)

    val ScreenPadding = 18.dp
    val CardRadius = 12.dp
    val PillRadius = 999.dp
    val ControlHeight = 60.dp
    val CompactControlHeight = 48.dp
    val IconButtonSize = 44.dp
    val IconSize = 22.dp
}

@Composable
fun SoulMatchScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = SoulMatchTokens.ScreenPadding),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
fun SoulMatchTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (navigationIcon != null && onNavigationClick != null) {
            SoulMatchIconButton(
                icon = navigationIcon,
                contentDescription = "Back",
                onClick = onNavigationClick
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (actionIcon != null && onActionClick != null) {
            SoulMatchIconButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                onClick = onActionClick
            )
        }
    }
}

@Composable
fun SoulMatchCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SoulMatchTokens.Card,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    radius: Dp = SoulMatchTokens.CardRadius,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, SoulMatchTokens.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SoulMatchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(SoulMatchTokens.ControlHeight),
        shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoulMatchTokens.Tangerine,
            contentColor = Color.White,
            disabledContainerColor = SoulMatchTokens.GoldSoft,
            disabledContentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun SoulMatchSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(SoulMatchTokens.ControlHeight),
        shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
        border = BorderStroke(1.dp, SoulMatchTokens.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = SoulMatchTokens.Text,
            disabledContentColor = SoulMatchTokens.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun SoulMatchInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = SoulMatchTokens.Muted) },
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius)
    )
}

@Composable
fun SoulMatchPill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
        color = if (selected) SoulMatchTokens.TangerineSoft else Color.White,
        border = BorderStroke(1.dp, if (selected) SoulMatchTokens.Tangerine else SoulMatchTokens.Border)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) SoulMatchTokens.Tangerine else SoulMatchTokens.Text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SoulMatchIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SoulMatchTokens.Tangerine
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(SoulMatchTokens.IconButtonSize)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
            color = Color.White,
            border = BorderStroke(1.dp, SoulMatchTokens.Border)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(SoulMatchTokens.IconSize))
            }
        }
    }
}

@Composable
fun SoulMatchSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                modifier = Modifier.clickable(onClick = onActionClick),
                style = MaterialTheme.typography.labelMedium,
                color = SoulMatchTokens.Tangerine,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
