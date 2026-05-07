package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class AgentPlanCardData(
    val name: String,
    val price: String,
    val features: List<String>,
    val highlighted: Boolean = false
)

@Composable
fun AgentPlansScreen(
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenAccount: () -> Unit,
    onDrawerDestination: (AgentDrawerDestination) -> Unit
) {
    val plans = listOf(
        AgentPlanCardData("Free", "₹0 /month", listOf("5 Profile Limit", "Basic Visibility", "2 Contact Views")),
        AgentPlanCardData("Silver", "₹2,499 /month", listOf("20 Profile Limit", "Enhanced Visibility", "15 Contact Views")),
        AgentPlanCardData("Gold", "₹4,999 /month", listOf("50 Profile Limit", "Priority Visibility", "50 Contact Views", "Verified Badge"), true),
        AgentPlanCardData("Platinum", "₹9,999 /month", listOf("Unlimited Profiles", "Featured Placement", "Unlimited Contacts", "Advanced Analytics"))
    )
    AgentScaffold(
        title = "Plans",
        selectedTab = AgentTab.Plans,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = {},
        onOpenAccount = onOpenAccount,
        onDrawerDestination = onDrawerDestination
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elevate Your Agency", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("Choose a plan that fits your growth and client volume.", color = AgentColorsMuted)
                }
            }
            items(plans) { plan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (plan.highlighted) AgentColorsAccent else Color.White),
                    shape = AgentShapesCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(plan.name, color = if (plan.highlighted) Color.White else Color.Black, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(plan.price, color = if (plan.highlighted) Color.White else AgentColorsAccent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        plan.features.forEach { feature ->
                            Text("• $feature", color = if (plan.highlighted) Color.White else AgentColorsMuted)
                        }
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (plan.highlighted) Color.White else AgentColorsAccent,
                                contentColor = if (plan.highlighted) AgentColorsAccent else Color.White
                            )
                        ) {
                            Text(if (plan.name == "Free") "Current Plan" else "Upgrade Now")
                        }
                    }
                }
            }
        }
    }
}
