package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val AgentAccent = Color(0xFF9B0044)
private val AgentSurface = Color(0xFFFFFAF7)
private val AgentCard = Color(0xFFFFF3F0)
private val AgentMuted = Color(0xFF6B5960)
private val AgentBorder = Color(0xFFE8D8D4)

enum class AgentTab(val label: String) {
    Dashboard("Dashboard"),
    Profiles("Profiles"),
    Plans("Plans"),
    Account("Account")
}

enum class AgentTopBarMode {
    Menu,
    Close
}

enum class AgentDrawerDestination {
    Dashboard,
    PendingProfiles,
    ManagedProfiles,
    AddMember,
    Activities,
    Plans,
    Account,
    Onboarding
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
    onDrawerDestination: (AgentDrawerDestination) -> Unit = {},
    modifier: Modifier = Modifier,
    topBarMode: AgentTopBarMode = AgentTopBarMode.Menu,
    showBottomBar: Boolean = true,
    onBack: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                AgentDrawerContent(
                    title = title,
                    selectedTab = selectedTab,
                    onDestinationSelected = { destination ->
                        scope.launch { drawerState.close() }
                        onDrawerDestination(destination)
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = AgentSurface,
            topBar = {
                AgentTopBar(
                    mode = topBarMode,
                    onMenu = { scope.launch { drawerState.open() } },
                    onBack = onBack
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    AgentBottomNavigation(
                        selectedTab = selectedTab,
                        onOpenDashboard = onOpenDashboard,
                        onOpenProfiles = onOpenProfiles,
                        onOpenPlans = onOpenPlans,
                        onOpenAccount = onOpenAccount
                    )
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
}

@Composable
private fun AgentTopBar(
    mode: AgentTopBarMode,
    onMenu: () -> Unit,
    onBack: (() -> Unit)?
) {
    Surface(
        color = Color(0xFFFFFCFA),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, AgentBorder.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (mode) {
                AgentTopBarMode.Menu -> {
                    IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu", tint = Color(0xFF1E1B18))
                    }
                }

                AgentTopBarMode.Close -> {
                    IconButton(onClick = { onBack?.invoke() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF1E1B18))
                    }
                }
            }
            Text(
                "SoulMatch",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                ),
                color = AgentAccent
            )
            IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (mode == AgentTopBarMode.Close) Icons.Filled.MoreVert else Icons.Filled.Menu,
                    contentDescription = "More options",
                    tint = Color(0xFF1E1B18)
                )
            }
        }
    }
}

@Composable
private fun AgentBottomNavigation(
    selectedTab: AgentTab,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val items = listOf(
        AgentBottomItem(AgentTab.Dashboard, Icons.Outlined.Home),
        AgentBottomItem(AgentTab.Profiles, Icons.Outlined.ViewList),
        AgentBottomItem(AgentTab.Plans, Icons.Outlined.Badge),
        AgentBottomItem(AgentTab.Account, Icons.Outlined.Person)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(horizontal = 8.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.tab == selectedTab,
                    onClick = {
                        when (item.tab) {
                            AgentTab.Dashboard -> onOpenDashboard()
                            AgentTab.Profiles -> onOpenProfiles()
                            AgentTab.Plans -> onOpenPlans()
                            AgentTab.Account -> onOpenAccount()
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.tab.label) },
                    label = {
                        Text(
                            item.tab.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgentAccent,
                        selectedTextColor = AgentAccent,
                        indicatorColor = Color(0xFFFFF0F4),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                )
            }
        }
    }
}

@Composable
private fun AgentDrawerContent(
    title: String,
    selectedTab: AgentTab,
    onDestinationSelected: (AgentDrawerDestination) -> Unit
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AgentCard,
            border = BorderStroke(1.dp, AgentBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF211517),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (selectedTab) {
                        AgentTab.Dashboard -> "Track verifications, activity, and profile health."
                        AgentTab.Profiles -> "Manage pending and verified member profiles."
                        AgentTab.Plans -> "Review plan benefits and growth options."
                        AgentTab.Account -> "Update onboarding details and rate settings."
                    },
                    color = AgentMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        agentDrawerItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = false,
                icon = { Icon(item.icon, contentDescription = null) },
                onClick = { onDestinationSelected(item.destination) },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surface)
            )
        }
    }
}

private data class AgentBottomItem(
    val tab: AgentTab,
    val icon: ImageVector
)

private data class AgentDrawerItem(
    val label: String,
    val destination: AgentDrawerDestination,
    val icon: ImageVector
)

private val agentDrawerItems = listOf(
    AgentDrawerItem("Dashboard", AgentDrawerDestination.Dashboard, Icons.Filled.Home),
    AgentDrawerItem("Pending Verifications", AgentDrawerDestination.PendingProfiles, Icons.Filled.Search),
    AgentDrawerItem("Managed Profiles", AgentDrawerDestination.ManagedProfiles, Icons.Filled.AssignmentTurnedIn),
    AgentDrawerItem("Add New Member", AgentDrawerDestination.AddMember, Icons.Filled.Add),
    AgentDrawerItem("Recent Activity", AgentDrawerDestination.Activities, Icons.Filled.Favorite),
    AgentDrawerItem("Plans", AgentDrawerDestination.Plans, Icons.Filled.Chat),
    AgentDrawerItem("Account", AgentDrawerDestination.Account, Icons.Filled.Person),
    AgentDrawerItem("Update Profile", AgentDrawerDestination.Onboarding, Icons.Filled.Edit)
)

internal val AgentColorsAccent = AgentAccent
internal val AgentColorsSurface = AgentSurface
internal val AgentColorsCard = AgentCard
internal val AgentColorsMuted = AgentMuted
internal val AgentShapesCard = RoundedCornerShape(24.dp)
