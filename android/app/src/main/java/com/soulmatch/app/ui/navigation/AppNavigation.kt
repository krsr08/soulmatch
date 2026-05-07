package com.soulmatch.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmatch.app.data.models.AppContentData
import com.soulmatch.app.data.models.BrandingConfig
import com.soulmatch.app.data.models.ClientIntegrationsData
import com.soulmatch.app.data.models.LegalContentData
import com.soulmatch.app.data.models.NavigationContentData
import com.soulmatch.app.ui.screens.auth.OTPVerificationScreen
import com.soulmatch.app.ui.screens.auth.PhoneEntryScreen
import com.soulmatch.app.ui.screens.auth.ProfileWizardScreen
import com.soulmatch.app.ui.screens.auth.RoleSelectionScreen
import com.soulmatch.app.ui.screens.auth.WelcomeScreen
import com.soulmatch.app.ui.screens.agent.AgentAccountScreen
import com.soulmatch.app.ui.screens.agent.AgentClientProfileScreen
import com.soulmatch.app.ui.screens.agent.AgentDashboardScreen
import com.soulmatch.app.ui.screens.agent.AgentDrawerDestination
import com.soulmatch.app.ui.screens.agent.AgentOnboardingScreen
import com.soulmatch.app.ui.screens.agent.AgentActivitiesScreen
import com.soulmatch.app.ui.screens.agent.AgentPlansScreen
import com.soulmatch.app.ui.screens.agent.AgentProfilesScreen
import com.soulmatch.app.ui.screens.chat.ChatListScreen
import com.soulmatch.app.ui.screens.chat.ChatScreen
import com.soulmatch.app.ui.screens.home.BestMatchesScreen
import com.soulmatch.app.ui.screens.home.DashboardScreen
import com.soulmatch.app.ui.screens.home.NotificationsScreen
import com.soulmatch.app.ui.screens.interests.InterestsScreen
import com.soulmatch.app.ui.screens.legal.LegalContentScreen
import com.soulmatch.app.ui.screens.profile.MyProfileScreen
import com.soulmatch.app.ui.screens.profile.PartnerPreferencesScreen
import com.soulmatch.app.ui.screens.profile.TrustDetailsScreen
import com.soulmatch.app.ui.screens.profile.FamilyDecisionBoardScreen
import com.soulmatch.app.ui.screens.profile.SafetyCenterScreen
import com.soulmatch.app.ui.screens.profile.HelpSupportScreen
import com.soulmatch.app.ui.screens.profile.AstrologyServicesScreen
import com.soulmatch.app.ui.screens.profile.ProfileDetailScreen
import com.soulmatch.app.ui.screens.profile.SoulMatchAssistScreen
import com.soulmatch.app.ui.screens.profile.SpotlightScreen
import com.soulmatch.app.ui.screens.search.SearchScreen
import com.soulmatch.app.ui.screens.settings.SettingsScreen
import com.soulmatch.app.ui.screens.success.SuccessStoriesScreen
import com.soulmatch.app.ui.screens.subscription.SubscriptionHistoryScreen
import com.soulmatch.app.ui.screens.subscription.SubscriptionScreen
import com.soulmatch.app.ui.viewmodels.AnalyticsViewModel
import com.soulmatch.app.ui.components.ProfileDrawerRoutes

@Composable
fun AppNavigation(
    startDestination: String = "welcome",
    isAuthenticated: Boolean = false,
    navigationLocked: Boolean = false,
    branding: BrandingConfig = BrandingConfig(),
    content: AppContentData = AppContentData(),
    legal: LegalContentData = LegalContentData(),
    clientIntegrations: ClientIntegrationsData = ClientIntegrationsData()
) {
    val nav = rememberNavController()
    val analytics: AnalyticsViewModel = hiltViewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    fun openAgentDrawer(destination: AgentDrawerDestination) {
        when (destination) {
            AgentDrawerDestination.Dashboard -> nav.navigate("agent_dashboard")
            AgentDrawerDestination.PendingProfiles -> nav.navigate("agent_profiles?filter=pending")
            AgentDrawerDestination.ManagedProfiles -> nav.navigate("agent_profiles?filter=managed")
            AgentDrawerDestination.AddMember -> nav.navigate("agent_client_profile")
            AgentDrawerDestination.Activities -> nav.navigate("agent_activities")
            AgentDrawerDestination.Plans -> nav.navigate("agent_plans")
            AgentDrawerDestination.Account -> nav.navigate("agent_account")
            AgentDrawerDestination.Onboarding -> nav.navigate("agent_onboarding")
        }
    }
    LaunchedEffect(currentRoute) {
        analytics.trackPage(currentRoute)
    }
    Scaffold(
        bottomBar = {
            if (isAuthenticated && !navigationLocked && currentRoute.shouldShowBottomNavigation()) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    labels = content.navigation,
                    onNavigate = { destination ->
                        if (navigationLocked) return@AppBottomNavigation
                        analytics.trackClick("bottom_nav_$destination", currentRoute)
                        if (destination == "dashboard") {
                            nav.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                                launchSingleTop = true
                            }
                        } else if (currentRoute != destination) {
                            nav.navigate(destination) {
                                popUpTo("dashboard") { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        }
    ) { appPadding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(appPadding)
        ) {
        composable("welcome") {
            WelcomeScreen(
                branding = branding,
                content = content.auth,
                googleWebClientId = clientIntegrations.googleWebClientId,
                onOtpSent = { phone -> nav.navigate(buildOtpRoute(phone)) },
                onOpenTerms = { nav.navigate("legal/terms") },
                onOpenPrivacy = { nav.navigate("legal/privacy") },
                onAuthenticated = { route ->
                    nav.navigate(route) {
                        popUpTo("welcome") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            "legal/{document}",
            arguments = listOf(navArgument("document") { type = NavType.StringType })
        ) { backStack ->
            val document = when (backStack.arguments?.getString("document")) {
                "privacy" -> legal.privacy
                else -> legal.terms
            }
            LegalContentScreen(
                document = document,
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "phone_entry?entryMode={entryMode}&userType={userType}",
            arguments = listOf(
                navArgument("entryMode") {
                    type = NavType.StringType
                    defaultValue = "login"
                },
                navArgument("userType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val userType = backStack.arguments?.getString("userType")?.takeIf { it.isNotBlank() }
            val entryMode = backStack.arguments?.getString("entryMode") ?: "login"
            PhoneEntryScreen(
                content = content.phoneEntry,
                userType = userType,
                entryMode = entryMode,
                onOTPSent = { phone -> nav.navigate(buildOtpRoute(phone, userType)) },
                onVerified = { route ->
                    nav.navigate(route) { popUpTo("welcome") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "otp/{phone}?userType={userType}",
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType },
                navArgument("userType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            OTPVerificationScreen(
                phone = Uri.decode(backStack.arguments?.getString("phone") ?: ""),
                userType = backStack.arguments?.getString("userType")?.takeIf { it.isNotBlank() },
                onVerified = { route ->
                    nav.navigate(route) { popUpTo("welcome") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable("agent_onboarding") {
            AgentOnboardingScreen(
                onBack = { nav.popBackStack() },
                onCompleted = {
                    nav.navigate("agent_dashboard") {
                        popUpTo("agent_onboarding") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenProfiles = { nav.navigate("agent_profiles") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenAccount = { nav.navigate("agent_account") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable("agent_dashboard") {
            AgentDashboardScreen(
                onOpenOnboarding = { nav.navigate("agent_onboarding") },
                onOpenProfiles = { filter -> nav.navigate("agent_profiles?filter=${Uri.encode(filter)}") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenAccount = { nav.navigate("agent_account") },
                onOpenCreateProfile = { nav.navigate("agent_client_profile") },
                onOpenActivities = { nav.navigate("agent_activities") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable(
            "agent_profiles?filter={filter}",
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = "all"
                }
            )
        ) { backStack ->
            AgentProfilesScreen(
                filter = backStack.arguments?.getString("filter") ?: "all",
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenAccount = { nav.navigate("agent_account") },
                onOpenCreateProfile = { nav.navigate("agent_client_profile") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable("agent_activities") {
            AgentActivitiesScreen(
                onBack = { nav.popBackStack() },
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenProfiles = { nav.navigate("agent_profiles?filter=all") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenAccount = { nav.navigate("agent_account") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable("agent_plans") {
            AgentPlansScreen(
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenProfiles = { nav.navigate("agent_profiles") },
                onOpenAccount = { nav.navigate("agent_account") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable("agent_account") {
            AgentAccountScreen(
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenProfiles = { nav.navigate("agent_profiles") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenOnboarding = { nav.navigate("agent_onboarding") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable("agent_client_profile") {
            AgentClientProfileScreen(
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.navigate("agent_profiles?filter=pending") {
                        popUpTo("agent_client_profile") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenDashboard = { nav.navigate("agent_dashboard") },
                onOpenProfiles = { nav.navigate("agent_profiles") },
                onOpenPlans = { nav.navigate("agent_plans") },
                onOpenAccount = { nav.navigate("agent_account") },
                onDrawerDestination = ::openAgentDrawer
            )
        }
        composable(
            "profile_wizard/{step}?returnToProfile={returnToProfile}",
            arguments = listOf(
                navArgument("step") { type = NavType.IntType },
                navArgument("returnToProfile") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStack ->
            val resolvedStep = backStack.arguments?.let { args ->
                if (args.containsKey("step")) args.getInt("step").coerceIn(1, 6) else 1
            } ?: 1
            val returnToProfile = backStack.arguments?.getBoolean("returnToProfile") ?: false
            ProfileWizardScreen(
                step = resolvedStep,
                isSectionEdit = returnToProfile,
                onNextStep = { next ->
                    if (returnToProfile) {
                        nav.navigate("my_profile") {
                            popUpTo("my_profile") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else if (next > 6) {
                        nav.navigate("dashboard") { popUpTo("welcome") { inclusive = true } }
                    } else {
                        nav.navigate("profile_wizard/$next")
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                content = content.home,
                onViewProfile = { nav.navigate("profile/$it") },
                onOpenChat = { participantId, name -> nav.navigate("chat/$participantId/${Uri.encode(name)}") },
                onOpenSearch = { nav.navigate("search") },
                onOpenMessages = { nav.navigate("chat_list") },
                onOpenInterests = { nav.navigate("interests") },
                onOpenBestMatches = { nav.navigate("best_matches") },
                onOpenNotifications = { nav.navigate("notifications") },
                onOpenProfile = { nav.navigate("my_profile") },
                onProfileMenuDestination = { destination ->
                    when (destination) {
                        ProfileDrawerRoutes.EditProfile -> nav.navigate("my_profile")
                        ProfileDrawerRoutes.PartnerPreference -> nav.navigate("partner_preferences")
                        ProfileDrawerRoutes.SoulMatchAssist -> nav.navigate("soulmatch_assist")
                        ProfileDrawerRoutes.Spotlight -> nav.navigate("spotlight")
                        ProfileDrawerRoutes.AstrologyServices -> nav.navigate("astrology_services")
                        ProfileDrawerRoutes.AccountSettings -> nav.navigate("settings")
                        ProfileDrawerRoutes.SafetyCenter -> nav.navigate("safety_center")
                        ProfileDrawerRoutes.SubscriptionHistory -> nav.navigate("subscription_history")
                        ProfileDrawerRoutes.HelpSupport -> nav.navigate("help_support")
                        ProfileDrawerRoutes.SuccessStories2026 -> nav.navigate("success_stories/2026")
                        ProfileDrawerRoutes.SuccessStories2025 -> nav.navigate("success_stories/2025")
                        ProfileDrawerRoutes.SuccessStories2024 -> nav.navigate("success_stories/2024")
                    }
                },
                onOpenSubscription = { nav.navigate("subscription") }
            )
        }
        composable("notifications") {
            NotificationsScreen(
                onBack = { nav.popBackStack() },
                onOpenProfile = { nav.navigate("my_profile") },
                onOpenActivity = { nav.navigate("interests") },
                onOpenBestMatches = { nav.navigate("best_matches") }
            )
        }
        composable("best_matches") {
            BestMatchesScreen(
                onViewProfile = { nav.navigate("profile/$it") },
                onSubscribe = { nav.navigate("subscription") },
                onBack = { nav.popBackStack() }
            )
        }
        composable("search") {
            SearchScreen(
                onViewProfile = { nav.navigate("profile/$it") },
                onOpenChat = { participantId, name -> nav.navigate("chat/$participantId/${Uri.encode(name)}") },
                onSubscribe = { nav.navigate("subscription") },
                onBack = { nav.popBackStack() }
            )
        }
        composable("profile/{profileId}", arguments = listOf(navArgument("profileId") { type = NavType.StringType })) { backStack ->
            ProfileDetailScreen(
                profileId = backStack.arguments?.getString("profileId") ?: "",
                onBack = { nav.popBackStack() },
                onSubscribe = { nav.navigate("subscription") },
                onOpenChat = { participantId, name -> nav.navigate("chat/$participantId/${Uri.encode(name)}") }
            )
        }
        composable("my_profile") {
            MyProfileScreen(
                onBack = { nav.popBackStack() },
                onSubscribe = { nav.navigate("subscription") },
                onEditSection = { step -> nav.navigate("profile_wizard/$step?returnToProfile=true") },
                onOpenSettings = { nav.navigate("settings") },
                onOpenAssist = { nav.navigate("soulmatch_assist") },
                onOpenPartnerPreferences = { nav.navigate("partner_preferences") },
                onOpenTrustDetails = { nav.navigate("trust_details") },
                onOpenFamilyBoard = { nav.navigate("family_decisions") }
            )
        }
        composable("partner_preferences") {
            PartnerPreferencesScreen(onBack = { nav.popBackStack() })
        }
        composable("trust_details") {
            TrustDetailsScreen(onBack = { nav.popBackStack() })
        }
        composable("family_decisions") {
            FamilyDecisionBoardScreen(
                onBack = { nav.popBackStack() },
                onViewProfile = { nav.navigate("profile/$it") }
            )
        }
        composable("chat_list") {
            ChatListScreen(
                onOpenChat = { participantId, name -> nav.navigate("chat/$participantId/${Uri.encode(name)}") },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "chat/{participantId}/{name}",
            arguments = listOf(
                navArgument("participantId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStack ->
            ChatScreen(
                chatId = backStack.arguments?.getString("participantId") ?: "",
                participantName = Uri.decode(backStack.arguments?.getString("name") ?: ""),
                onSubscribe = { nav.navigate("subscription") },
                onBack = { nav.popBackStack() }
            )
        }
        composable("interests") {
            InterestsScreen(
                onViewProfile = { nav.navigate("profile/$it") },
                onOpenChat = { participantId, name -> nav.navigate("chat/$participantId/${Uri.encode(name)}") },
                onSubscribe = { nav.navigate("subscription") },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "subscription?landOnPage={landOnPage}&routeCode={routeCode}&targetPackageId={targetPackageId}",
            arguments = listOf(
                navArgument("landOnPage") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("routeCode") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("targetPackageId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val args = backStack.arguments
            SubscriptionScreen(
                onBack = { nav.popBackStack() },
                razorpayKeyId = clientIntegrations.razorpayKeyId,
                landOnPage = args?.getInt("landOnPage")?.takeIf { it >= 0 },
                routeCode = args?.getInt("routeCode")?.takeIf { it >= 0 },
                targetPackageId = args?.getString("targetPackageId")?.takeIf { it.isNotBlank() },
                onPaymentResultDone = {
                    nav.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("auth_role_selection") {
            RoleSelectionScreen(
                onResolved = { route ->
                    nav.navigate(route) {
                        popUpTo("welcome") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("subscription_history") {
            SubscriptionHistoryScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onLogout = { nav.navigate("welcome") { popUpTo(0) { inclusive = true } } }
            )
        }
        composable("spotlight") {
            SpotlightScreen(
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate("settings") },
                onUpgrade = { nav.navigate("subscription?routeCode=33") }
            )
        }
        composable("soulmatch_assist") {
            SoulMatchAssistScreen(
                onBack = { nav.popBackStack() },
                onOpenFamilyStep = { nav.navigate("profile_wizard/4?returnToProfile=true") },
                onOpenSubscription = { nav.navigate("subscription?routeCode=44") }
            )
        }
        composable("astrology_services") {
            AstrologyServicesScreen(
                onBack = { nav.popBackStack() },
                onCompleteHoroscope = { nav.navigate("profile_wizard/6?returnToProfile=true") },
                onUpgrade = { nav.navigate("subscription?routeCode=18") }
            )
        }
        composable("safety_center") {
            SafetyCenterScreen(
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate("settings") },
                onOpenVerification = { nav.navigate("my_profile") },
                onOpenHelp = { nav.navigate("help_support") }
            )
        }
        composable("help_support") {
            HelpSupportScreen(
                onBack = { nav.popBackStack() },
                onOpenSafetyCenter = { nav.navigate("safety_center") },
                onOpenPrivacy = { nav.navigate("legal/privacy") },
                onOpenTerms = { nav.navigate("legal/terms") }
            )
        }
        composable(
            "success_stories/{year}",
            arguments = listOf(navArgument("year") { type = NavType.StringType })
        ) { backStack ->
            SuccessStoriesScreen(
                year = backStack.arguments?.getString("year") ?: "2026",
                onBack = { nav.popBackStack() }
            )
        }
    }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val routePrefixes: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("home", "dashboard", listOf("dashboard", "best_matches"), Icons.Filled.Home),
    BottomNavItem("search", "search", listOf("search"), Icons.Filled.Search),
    BottomNavItem("activity", "interests", listOf("interests"), Icons.Filled.Favorite),
    BottomNavItem("chat", "chat_list", listOf("chat_list", "chat/"), Icons.Filled.Chat),
    BottomNavItem(
        "profile",
        "my_profile",
        listOf("my_profile", "partner_preferences", "trust_details", "settings", "profile/", "family_decisions", "soulmatch_assist", "spotlight", "astrology_services", "safety_center", "subscription", "subscription_history", "help_support", "success_stories/"),
        Icons.Filled.Person
    )
)

@Composable
private fun AppBottomNavigation(
    currentRoute: String,
    labels: NavigationContentData,
    onNavigate: (String) -> Unit
) {
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
            bottomNavItems.forEach { item ->
                val label = when (item.label) {
                    "home" -> labels.home
                    "search" -> labels.search
                    "activity" -> labels.activity
                    "chat" -> labels.chat
                    "profile" -> labels.profile
                    else -> item.label
                }
                val selected = item.routePrefixes.any { prefix ->
                    currentRoute == prefix || currentRoute.startsWith(prefix)
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = label) },
                    label = {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color(0xFFFFF0F4),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                )
            }
        }
    }
}

private fun String.shouldShowBottomNavigation(): Boolean {
    if (isBlank()) return false
    return !startsWith("welcome") &&
        !startsWith("phone_entry") &&
        !startsWith("otp/") &&
        !startsWith("agent_") &&
        !startsWith("profile_wizard") &&
        !startsWith("legal/")
}

private fun buildOtpRoute(phone: String, userType: String? = null): String {
    val encodedPhone = Uri.encode(phone)
    return if (userType.isNullOrBlank()) {
        "otp/$encodedPhone"
    } else {
        "otp/$encodedPhone?userType=${Uri.encode(userType)}"
    }
}
