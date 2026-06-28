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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.soulmatch.app.data.models.MonetizationRuntimeData
import com.soulmatch.app.data.models.NavigationContentData
import com.soulmatch.app.ui.screens.auth.OTPVerificationScreen
import com.soulmatch.app.ui.screens.auth.PhoneEntryScreen
import com.soulmatch.app.ui.screens.auth.ProfileWizardScreen
import com.soulmatch.app.ui.screens.auth.RoleSelectionScreen
import com.soulmatch.app.ui.screens.auth.WelcomeScreen
import com.soulmatch.app.ui.screens.auth.LanguageSelectionScreen
import com.soulmatch.app.ui.screens.auth.OnboardingBenefitScreen
import com.soulmatch.app.ui.screens.auth.ForgotPasswordScreen
import com.soulmatch.app.ui.screens.auth.ResetPasswordScreen
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
import com.soulmatch.app.ui.screens.design.DesignHotspot
import com.soulmatch.app.ui.screens.design.ExactDesignScreen
import com.soulmatch.app.ui.screens.design.backHotspot
import com.soulmatch.app.ui.screens.design.bottomNavHotspots
import com.soulmatch.app.ui.screens.design.profileCardHotspots
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
import com.soulmatch.app.ui.screens.profile.SafetyCenterDetailScreen
import com.soulmatch.app.ui.screens.profile.HelpSupportScreen
import com.soulmatch.app.ui.screens.profile.AstrologyServicesScreen
import com.soulmatch.app.ui.screens.profile.ProfileDetailScreen
import com.soulmatch.app.ui.screens.profile.SoulMatchAssistScreen
import com.soulmatch.app.ui.screens.profile.SpotlightScreen
import com.soulmatch.app.ui.screens.search.SearchScreen
import com.soulmatch.app.ui.screens.settings.DeleteAccountScreen
import com.soulmatch.app.ui.screens.settings.LogoutConfirmationScreen
import com.soulmatch.app.ui.screens.settings.NotificationSettingsScreen
import com.soulmatch.app.ui.screens.settings.PaymentScreen
import com.soulmatch.app.ui.screens.settings.PrivacySettingsScreen
import com.soulmatch.app.ui.screens.settings.SettingsScreen
import com.soulmatch.app.ui.screens.success.SuccessStoriesScreen
import com.soulmatch.app.ui.screens.subscription.SubscriptionHistoryScreen
import com.soulmatch.app.ui.screens.subscription.SubscriptionScreen
import com.soulmatch.app.ui.viewmodels.AnalyticsViewModel
import com.soulmatch.app.ui.components.navigation.ProfileDrawerRoutes

@Composable
fun AppNavigation(
    startDestination: String = "welcome",
    isAuthenticated: Boolean = false,
    navigationLocked: Boolean = false,
    branding: BrandingConfig = BrandingConfig(),
    content: AppContentData = AppContentData(),
    monetization: MonetizationRuntimeData = MonetizationRuntimeData(),
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
            if (isAuthenticated && !navigationLocked && currentRoute.shouldShowBottomNavigation() && !currentRoute.usesExactDesignScreen()) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    labels = content.navigation,
                    monetization = monetization,
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
        composable("language_selection") {
            LanguageSelectionScreen(
                onContinue = {
                    nav.navigate("onboarding_benefit") {
                        popUpTo("language_selection") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("onboarding_benefit") {
            OnboardingBenefitScreen(
                onContinue = {
                    nav.navigate("welcome") {
                        popUpTo("onboarding_benefit") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("welcome") {
            WelcomeScreen(
                branding = branding,
                content = content.auth,
                googleWebClientId = clientIntegrations.googleWebClientId,
                onOtpSent = { phone -> nav.navigate(buildOtpRoute(phone, "member")) },
                onBackToLanguage = { nav.navigate("language_selection") },
                onForgotPassword = { nav.navigate("forgot_password") },
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
        composable("forgot_password") {
            ForgotPasswordScreen(
                onBack = { nav.popBackStack() },
                onOpenReset = { nav.navigate("reset_password") }
            )
        }
        composable("reset_password") {
            ResetPasswordScreen(
                onBack = { nav.popBackStack() },
                onDone = {
                    nav.navigate("welcome") {
                        popUpTo("welcome") { inclusive = false }
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
                onLogout = { nav.navigate("welcome") { popUpTo(0) { inclusive = true } } },
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
        composable("profile_intro") {
            ExactDesignScreen(
                assetName = "10_profile_creation_intro_screen.png",
                hotspots = listOf(
                    DesignHotspot(294f, 104f, 58f, 58f) { nav.navigate("profile_intro_info") },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_wizard/1") }
                )
            )
        }
        composable("profile_intro_info") {
            ExactDesignScreen(
                assetName = "10a_profile_creation_info_overlay.png",
                hotspots = listOf(
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_wizard/1") }
                )
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
            if (returnToProfile) {
                ExactDesignScreen(
                    assetName = "37_edit_profile_screen.png",
                    hotspots = listOf(
                        backHotspot { nav.popBackStack() },
                        DesignHotspot(18f, 236f, 354f, 54f) { nav.navigate("profile_wizard/1") },
                        DesignHotspot(18f, 292f, 354f, 54f) { nav.navigate("profile_wizard/2") },
                        DesignHotspot(18f, 348f, 354f, 54f) { nav.navigate("profile_wizard/3") },
                        DesignHotspot(18f, 404f, 354f, 54f) { nav.navigate("profile_wizard/4") },
                        DesignHotspot(18f, 460f, 354f, 54f) { nav.navigate("profile_wizard/5") },
                        DesignHotspot(18f, 516f, 354f, 54f) { nav.navigate("profile_wizard/6") },
                        DesignHotspot(18f, 572f, 354f, 54f) { nav.navigate("profile_photo_upload") },
                        DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("my_profile") }
                    )
                )
            } else {
                val asset = when (resolvedStep) {
                    1 -> "11_basic_details_screen.png"
                    2 -> "12_religious_and_community_details_screen.png"
                    3 -> "13_education_and_career_screen.png"
                    4 -> "14_family_details_screen.png"
                    5 -> "15_lifestyle_details_screen.png"
                    else -> "16_partner_preferences_screen.png"
                }
                val nextRoute = if (resolvedStep >= 6) "profile_photo_upload" else "profile_wizard/${resolvedStep + 1}"
                ExactDesignScreen(
                    assetName = asset,
                    hotspots = listOf(
                        backHotspot { nav.popBackStack() },
                        DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate(nextRoute) }
                    )
                )
            }
        }
        composable("profile_photo_upload") {
            ExactDesignScreen(
                assetName = "17_photo_upload_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_verification") }
                )
            )
        }
        composable("profile_verification") {
            ExactDesignScreen(
                assetName = "18_verification_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_preview_review") }
                )
            )
        }
        composable("profile_preview_review") {
            ExactDesignScreen(
                assetName = "19_profile_preview_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_under_review") }
                )
            )
        }
        composable("profile_under_review") {
            ExactDesignScreen(
                assetName = "20_profile_under_review_screen.png",
                hotspots = listOf(
                    DesignHotspot(28f, 682f, 334f, 58f) { nav.navigate("help_support") },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("dashboard") { popUpTo("welcome") { inclusive = true } } }
                )
            )
        }
        composable("profile_correction_required") {
            ExactDesignScreen(
                assetName = "21_profile_rejected_correction_required_screen.png",
                hotspots = listOf(
                    DesignHotspot(28f, 682f, 334f, 58f) { nav.navigate("profile_wizard/1?returnToProfile=true") },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("profile_under_review") }
                )
            )
        }
        composable("dashboard") {
            ExactDesignScreen(
                assetName = "22_home_dashboard.png",
                hotspots = bottomNavHotspots(
                    onHome = { nav.navigate("dashboard") },
                    onMatches = { nav.navigate("best_matches") },
                    onInterests = { nav.navigate("interests") },
                    onMessages = { nav.navigate("chat_list") },
                    onAccount = { nav.navigate("my_profile") }
                ) + profileCardHotspots(
                    onInterest = { nav.navigate("express_interest") },
                    onViewProfile = { nav.navigate("profile/demo") }
                ) + listOf(
                    DesignHotspot(318f, 36f, 56f, 56f) { nav.navigate("notifications") },
                    DesignHotspot(262f, 548f, 104f, 44f) { nav.navigate("subscription") }
                )
            )
        }
        composable("notifications") {
            ExactDesignScreen(
                assetName = "32_notifications_screen.png",
                hotspots = listOf(backHotspot { nav.popBackStack() })
            )
        }
        composable("best_matches") {
            ExactDesignScreen(
                assetName = "23_matches_listing_screen.png",
                hotspots = bottomNavHotspots(
                    onHome = { nav.navigate("dashboard") },
                    onMatches = { nav.navigate("best_matches") },
                    onInterests = { nav.navigate("interests") },
                    onMessages = { nav.navigate("chat_list") },
                    onAccount = { nav.navigate("my_profile") }
                ) + profileCardHotspots(
                    onInterest = { nav.navigate("express_interest") },
                    onViewProfile = { nav.navigate("profile/demo") }
                ) + listOf(backHotspot { nav.popBackStack() })
            )
        }
        composable("search") {
            ExactDesignScreen(
                assetName = "24_advanced_search_filter_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(204f, 776f, 154f, 48f) { nav.navigate("best_matches") },
                    DesignHotspot(32f, 776f, 154f, 48f) { nav.navigate("search") }
                )
            )
        }
        composable("profile/{profileId}", arguments = listOf(navArgument("profileId") { type = NavType.StringType })) { backStack ->
            ExactDesignScreen(
                assetName = "25_match_profile_detail_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 238f, 160f, 52f) { nav.navigate("express_interest") },
                    DesignHotspot(202f, 238f, 160f, 52f) { nav.navigate("chat/demo/Meera%20Kapoor") }
                )
            )
        }
        composable("my_profile") {
            ExactDesignScreen(
                assetName = "36_my_profile_screen.png",
                hotspots = bottomNavHotspots(
                    onHome = { nav.navigate("dashboard") },
                    onMatches = { nav.navigate("best_matches") },
                    onInterests = { nav.navigate("interests") },
                    onMessages = { nav.navigate("chat_list") },
                    onAccount = { nav.navigate("my_profile") }
                ) + listOf(
                    DesignHotspot(36f, 235f, 144f, 48f) { nav.navigate("profile_wizard/1?returnToProfile=true") },
                    DesignHotspot(206f, 235f, 144f, 48f) { nav.navigate("profile/demo") },
                    DesignHotspot(18f, 420f, 354f, 54f) { nav.navigate("settings") },
                    DesignHotspot(18f, 476f, 354f, 54f) { nav.navigate("notification_settings") },
                    DesignHotspot(18f, 532f, 354f, 54f) { nav.navigate("help_support") }
                )
            )
        }
        composable("partner_preferences") {
            PartnerPreferencesScreen(onBack = { nav.popBackStack() })
        }
        composable("trust_details") {
            TrustDetailsScreen(
                onBack = { nav.popBackStack() },
                onEditProfileStep = { step -> nav.navigate("profile_wizard/$step?returnToProfile=true") }
            )
        }
        composable("family_decisions") {
            FamilyDecisionBoardScreen(
                onBack = { nav.popBackStack() },
                onViewProfile = { nav.navigate("profile/$it") }
            )
        }
        composable("chat_list") {
            ExactDesignScreen(
                assetName = "34_messages_chat_list_screen.png",
                hotspots = bottomNavHotspots(
                    onHome = { nav.navigate("dashboard") },
                    onMatches = { nav.navigate("best_matches") },
                    onInterests = { nav.navigate("interests") },
                    onMessages = { nav.navigate("chat_list") },
                    onAccount = { nav.navigate("my_profile") }
                ) + listOf(
                    DesignHotspot(18f, 248f, 354f, 76f) { nav.navigate("chat/demo/Meera%20Kapoor") },
                    DesignHotspot(46f, 190f, 296f, 44f) { nav.navigate("subscription") }
                )
            )
        }
        composable(
            "chat/{participantId}/{name}",
            arguments = listOf(
                navArgument("participantId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStack ->
            ExactDesignScreen(
                assetName = "35_chat_detail_screen.png",
                hotspots = listOf(backHotspot { nav.popBackStack() })
            )
        }
        composable(
            "interests?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val tabAsset = when (backStack.arguments?.getString("tab").orEmpty()) {
                "shortlisted" -> "28_shortlisted_profiles_screen.png"
                "visitors" -> "29_viewed_my_profile_screen.png"
                "viewed" -> "30_my_viewed_profiles_screen.png"
                else -> "31_interests_screen.png"
            }
            ExactDesignScreen(
                assetName = tabAsset,
                hotspots = bottomNavHotspots(
                    onHome = { nav.navigate("dashboard") },
                    onMatches = { nav.navigate("best_matches") },
                    onInterests = { nav.navigate("interests") },
                    onMessages = { nav.navigate("chat_list") },
                    onAccount = { nav.navigate("my_profile") }
                ) + listOf(
                    DesignHotspot(286f, 260f, 76f, 38f) { nav.navigate("profile/demo") },
                    DesignHotspot(286f, 448f, 76f, 38f) { nav.navigate("profile/demo") }
                )
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
            ExactDesignScreen(
                assetName = "38_subscription_plans_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 706f, 334f, 58f) { nav.navigate("payment") }
                )
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
            ExactDesignScreen(
                assetName = "42_settings_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(18f, 224f, 354f, 54f) { nav.navigate("my_profile") },
                    DesignHotspot(18f, 280f, 354f, 54f) { nav.navigate("privacy_settings") },
                    DesignHotspot(18f, 336f, 354f, 54f) { nav.navigate("notification_settings") },
                    DesignHotspot(18f, 392f, 354f, 54f) { nav.navigate("privacy_settings") },
                    DesignHotspot(18f, 448f, 354f, 54f) { nav.navigate("help_support") },
                    DesignHotspot(18f, 616f, 354f, 54f) { nav.navigate("delete_account") },
                    DesignHotspot(18f, 672f, 354f, 54f) { nav.navigate("logout_confirmation") }
                )
            )
        }
        composable("privacy_settings") {
            ExactDesignScreen(
                assetName = "43_privacy_settings_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.popBackStack() }
                )
            )
        }
        composable("notification_settings") {
            ExactDesignScreen(
                assetName = "44_notification_settings_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.popBackStack() }
                )
            )
        }
        composable("payment") {
            ExactDesignScreen(
                assetName = "39_payment_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("payment_success") }
                )
            )
        }
        composable("payment_success") {
            ExactDesignScreen(
                assetName = "40_payment_success_screen.png",
                hotspots = listOf(DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("dashboard") })
            )
        }
        composable("payment_failure") {
            ExactDesignScreen(
                assetName = "41_payment_failure_screen.png",
                hotspots = listOf(
                    DesignHotspot(28f, 684f, 334f, 58f) { nav.navigate("payment") },
                    DesignHotspot(28f, 754f, 334f, 58f) { nav.navigate("help_support") }
                )
            )
        }
        composable("delete_account") {
            ExactDesignScreen(
                assetName = "46_delete_account_screen.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 674f, 334f, 58f) { nav.navigate("welcome") { popUpTo(0) { inclusive = true } } },
                    DesignHotspot(28f, 744f, 334f, 58f) { nav.popBackStack() }
                )
            )
        }
        composable("logout_confirmation") {
            ExactDesignScreen(
                assetName = "47_logout_confirmation_modal.png",
                hotspots = listOf(
                    DesignHotspot(28f, 648f, 334f, 58f) { nav.navigate("welcome") { popUpTo(0) { inclusive = true } } },
                    DesignHotspot(28f, 718f, 334f, 58f) { nav.popBackStack() }
                )
            )
        }
        composable("express_interest") {
            ExactDesignScreen(
                assetName = "26_express_interest_bottom_sheet.png",
                hotspots = listOf(
                    backHotspot { nav.popBackStack() },
                    DesignHotspot(28f, 648f, 334f, 58f) { nav.navigate("interest_sent") },
                    DesignHotspot(28f, 718f, 334f, 58f) { nav.popBackStack() }
                )
            )
        }
        composable("interest_sent") {
            ExactDesignScreen(
                assetName = "27_interest_sent_toast_state.png",
                hotspots = listOf(backHotspot { nav.popBackStack() })
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
                onOpenHelp = { nav.navigate("help_support") },
                onOpenSafetyDestination = { destination ->
                    nav.navigate(resolveSafetyDestination(destination))
                },
                content = content.safetyCenter
            )
        }
        composable(
            "safety_center/article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) { backStack ->
            SafetyCenterDetailScreen(
                articleId = Uri.decode(backStack.arguments?.getString("articleId") ?: ""),
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate("settings") },
                onOpenVerification = { nav.navigate("my_profile") },
                onOpenHelp = { nav.navigate("help_support") },
                onOpenSafetyDestination = { destination ->
                    nav.navigate(resolveSafetyDestination(destination))
                },
                content = content.safetyCenter
            )
        }
        composable("help_support") {
            ExactDesignScreen(
                assetName = "45_help_and_support_screen.png",
                hotspots = listOf(backHotspot { nav.popBackStack() })
            )
        }
        composable(
            "success_stories/{year}",
            arguments = listOf(navArgument("year") { type = NavType.StringType })
        ) { backStack ->
            SuccessStoriesScreen(
                year = backStack.arguments?.getString("year") ?: "overview",
                onBack = { nav.popBackStack() },
                onOpenYear = { selectedYear -> nav.navigate("success_stories/$selectedYear") }
            )
        }
    }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val routePrefixes: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val overrideLabel: String? = null
)

private fun bottomNavItems(monetization: MonetizationRuntimeData): List<BottomNavItem> {
    val fourthItem = when {
        monetization.isFreeAccessMode() -> BottomNavItem(
            label = "account",
            route = "my_profile",
            routePrefixes = listOf("my_profile", "settings", "trust_details", "partner_preferences", "soulmatch_assist", "astrology_services", "safety_center", "help_support"),
            icon = Icons.Filled.Person,
            overrideLabel = monetization.freeAccessLabel.ifBlank { "Account" }
        )
        monetization.isFixedPriceMode() -> BottomNavItem(
            label = "upgrade",
            route = "subscription",
            routePrefixes = listOf("subscription", "subscription_history", "spotlight"),
            icon = Icons.Filled.WorkspacePremium,
            overrideLabel = monetization.fixedPriceLabel.ifBlank { "₹${monetization.fixedPriceAmount.coerceAtLeast(1)}" }
        )
        else -> BottomNavItem(
            label = "upgrade",
            route = "subscription",
            routePrefixes = listOf("subscription", "subscription_history", "spotlight"),
            icon = Icons.Filled.WorkspacePremium
        )
    }
    return listOf(
        BottomNavItem("home", "dashboard", listOf("dashboard", "best_matches"), Icons.Filled.Home),
        BottomNavItem("activity", "interests", listOf("interests"), Icons.Filled.AccessTime),
        BottomNavItem("chat", "chat_list", listOf("chat_list", "chat/"), Icons.Filled.Chat),
        fourthItem
    )
}

@Composable
private fun AppBottomNavigation(
    currentRoute: String,
    labels: NavigationContentData,
    monetization: MonetizationRuntimeData,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 10.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            bottomNavItems(monetization).forEach { item ->
                val label = item.overrideLabel ?: when (item.label) {
                    "home" -> labels.home
                    "search" -> labels.search
                    "activity" -> labels.activity
                    "chat" -> labels.chat.takeUnless { it.equals("Chat", ignoreCase = true) } ?: "Messenger"
                    "profile" -> labels.profile
                    "upgrade" -> labels.upgrade
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
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color(0xFFFFF1E8),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                )
            }
        }
    }
}

private fun MonetizationRuntimeData.isFreeAccessMode(): Boolean {
    return !subscriptionModelEnabled && accessMode.equals("free", ignoreCase = true)
}

private fun MonetizationRuntimeData.isFixedPriceMode(): Boolean {
    return !subscriptionModelEnabled && accessMode.equals("fixed_price", ignoreCase = true)
}

private fun String.shouldShowBottomNavigation(): Boolean {
    if (isBlank()) return false
    return !startsWith("welcome") &&
        !startsWith("language_selection") &&
        !startsWith("onboarding_benefit") &&
        !startsWith("forgot_password") &&
        !startsWith("reset_password") &&
        !startsWith("phone_entry") &&
        !startsWith("otp/") &&
        !startsWith("auth_role_selection") &&
        !startsWith("agent_") &&
        !startsWith("profile_wizard") &&
        !startsWith("legal/")
}

private fun String.usesExactDesignScreen(): Boolean {
    if (isBlank()) return false
    val exactPrefixes = listOf(
        "dashboard",
        "notifications",
        "best_matches",
        "search",
        "profile/",
        "my_profile",
        "chat_list",
        "chat/",
        "interests",
        "subscription",
        "settings",
        "privacy_settings",
        "notification_settings",
        "payment",
        "payment_success",
        "payment_failure",
        "delete_account",
        "logout_confirmation",
        "express_interest",
        "interest_sent",
        "help_support",
        "profile_photo_upload",
        "profile_verification",
        "profile_preview_review",
        "profile_under_review",
        "profile_correction_required",
        "profile_intro"
    )
    return exactPrefixes.any { prefix -> this == prefix || startsWith(prefix) }
}

private fun buildOtpRoute(phone: String, userType: String? = null): String {
    val encodedPhone = Uri.encode(phone)
    return if (userType.isNullOrBlank()) {
        "otp/$encodedPhone"
    } else {
        "otp/$encodedPhone?userType=${Uri.encode(userType)}"
    }
}

private fun resolveSafetyDestination(destination: String): String {
    val normalized = destination.trim()
    return when {
        normalized.equals("settings", ignoreCase = true) -> "settings"
        normalized.equals("my_profile", ignoreCase = true) ||
            normalized.equals("verification", ignoreCase = true) ||
            normalized.equals("trust_details", ignoreCase = true) -> "my_profile"
        normalized.equals("help", ignoreCase = true) ||
            normalized.equals("help_support", ignoreCase = true) ||
            normalized.equals("support", ignoreCase = true) -> "help_support"
        normalized.equals("hidden_members", ignoreCase = true) ||
            normalized.equals("hidden", ignoreCase = true) -> "interests?tab=hidden"
        normalized.equals("blocked_members", ignoreCase = true) ||
            normalized.equals("blocked", ignoreCase = true) -> "interests?tab=blocked"
        normalized.equals("reported_members", ignoreCase = true) ||
            normalized.equals("reports", ignoreCase = true) ||
            normalized.equals("reported", ignoreCase = true) -> "interests?tab=reported"
        normalized.startsWith("article:", ignoreCase = true) ->
            "safety_center/article/${Uri.encode(normalized.substringAfter(':'))}"
        normalized.startsWith("safety_center/article/") -> normalized
        normalized.isBlank() -> "safety_center"
        else -> "safety_center/article/${Uri.encode(normalized)}"
    }
}
