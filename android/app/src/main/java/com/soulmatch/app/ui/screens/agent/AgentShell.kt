package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AgentAccent = Color(0xFFB1084B)
private val AgentSurface = Color(0xFFFFF8F3)
private val AgentCard = Color(0xFFFFF2EE)
private val AgentMuted = Color(0xFF7B6B6F)

enum class AgentTab(val label: String) {
    Dashboard("Dashboard"),
    Profiles("Profiles"),
    Plans("Plans"),
    Account("Account")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AgentScaffold(
    title: String,
    selectedTab: AgentTab,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = AgentSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgentSurface),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = null, tint = Color(0xFF463A3C))
                        Text("SoulMatch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AgentAccent)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFF8F0EE), CircleShape)
                                .border(1.dp, AgentAccent.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                listOf(
                    Triple(AgentTab.Dashboard, Icons.Outlined.Home, onOpenDashboard),
                    Triple(AgentTab.Profiles, Icons.Outlined.ViewList, onOpenProfiles),
                    Triple(AgentTab.Plans, Icons.Outlined.Badge, onOpenPlans),
                    Triple(AgentTab.Account, Icons.Outlined.Person, onOpenAccount)
                ).forEach { (tab, icon, onClick) ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = onClick,
                        icon = {
                            Icon(
                                icon,
                                contentDescription = tab.label,
                                modifier = if (selectedTab == tab) {
                                    Modifier
                                        .background(AgentAccent, CircleShape)
                                        .padding(8.dp)
                                } else Modifier,
                                tint = if (selectedTab == tab) Color.White else AgentMuted
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedTextColor = AgentAccent,
                            unselectedTextColor = AgentMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        content(
            Modifier
                .padding(padding)
                .fillMaxWidth()
        )
    }
}

internal val AgentColorsAccent = AgentAccent
internal val AgentColorsSurface = AgentSurface
internal val AgentColorsCard = AgentCard
internal val AgentColorsMuted = AgentMuted
internal val AgentShapesCard = RoundedCornerShape(24.dp)
