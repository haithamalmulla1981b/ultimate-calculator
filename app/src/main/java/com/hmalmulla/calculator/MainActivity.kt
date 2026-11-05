package com.hmalmulla.calculator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hmalmulla.calculator.ui.calculator.CalculatorViewModel
import com.hmalmulla.calculator.ui.calculator.ScientificCalculatorScreen
import com.hmalmulla.calculator.ui.calculator.StandardCalculatorScreen
import com.hmalmulla.calculator.ui.converter.ConverterScreen
import com.hmalmulla.calculator.ui.history.HistoryScreen
import com.hmalmulla.calculator.ui.settings.SettingsScreen
import com.hmalmulla.calculator.ui.theme.CalculatorTheme
import com.hmalmulla.calculator.ui.tools.CurrencyScreen
import com.hmalmulla.calculator.ui.tools.FinanceTools
import com.hmalmulla.calculator.ui.tools.MathTools
import com.hmalmulla.calculator.ui.tools.ToolIcon
import com.hmalmulla.calculator.ui.tools.ToolList
import com.hmalmulla.calculator.ui.tools.ToolScreen

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Dest("calculator", "Calculator", Icons.Outlined.Calculate),
    Dest("scientific", "Scientific", Icons.Outlined.Science),
    Dest("converter", "Convert", Icons.Outlined.SwapHoriz),
    Dest("finance", "Finance", Icons.Outlined.AccountBalanceWallet),
    Dest("history", "History", Icons.Outlined.History),
    Dest("settings", "Settings", Icons.Outlined.Settings),
)

class MainActivity : ComponentActivity() {
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val vm: CalculatorViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            CalculatorTheme(settings) {
                AppRoot(vm, pendingRoute) { pendingRoute = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { pendingRoute = it }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

private fun NavHostController.goTop(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: CalculatorViewModel, pendingRoute: String?, consumed: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val topLevel = destinations.any { it.route == current }

    LaunchedEffect(pendingRoute) {
        val r = pendingRoute ?: return@LaunchedEffect
        if (destinations.any { it.route == r }) nav.goTop(r)
        consumed()
    }

    val title = destinations.firstOrNull { it.route == current }?.label
    Scaffold(
        topBar = {
            if (topLevel && current != "calculator" && current != "scientific" && !wide) {
                TopAppBar(title = { Text(title ?: "") })
            }
        },
        bottomBar = {
            if (!wide && topLevel) {
                NavigationBar {
                    destinations.forEach { d ->
                        NavigationBarItem(
                            selected = current == d.route,
                            onClick = { nav.goTop(d.route) },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label, maxLines = 1) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            if (wide && topLevel) {
                NavigationRail {
                    destinations.forEach { d ->
                        NavigationRailItem(
                            selected = current == d.route,
                            onClick = { nav.goTop(d.route) },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = nav, startDestination = "calculator",
                    enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
                ) {
                    composable("calculator") { StandardCalculatorScreen(vm) }
                    composable("scientific") { ScientificCalculatorScreen(vm) { nav.navigate("mathtools") } }
                    composable("converter") { ConverterScreen() }
                    composable("finance") {
                        ToolList(FinanceTools, onOpen = { nav.navigate("finance/${it.id}") }) {
                            Card(
                                onClick = { nav.navigate("currency") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    ToolIcon(Icons.Outlined.CurrencyExchange)
                                    Text("  Currency converter  ·  needs internet", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    composable("finance/{id}") { e ->
                        val tool = FinanceTools.firstOrNull { it.id == e.arguments?.getString("id") }
                        if (tool != null) ToolScreen(tool) { nav.popBackStack() }
                    }
                    composable("currency") { CurrencyScreen { nav.popBackStack() } }
                    composable("mathtools") {
                        MathToolsList(onOpen = { nav.navigate("math/$it") }, onBack = { nav.popBackStack() })
                    }
                    composable("math/{id}") { e ->
                        val tool = MathTools.firstOrNull { it.id == e.arguments?.getString("id") }
                        if (tool != null) ToolScreen(tool) { nav.popBackStack() }
                    }
                    composable("history") {
                        HistoryScreen(onReuse = { h -> vm.reuse(h); nav.goTop("calculator") })
                    }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MathToolsList(onOpen: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Math tools") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad)) { ToolList(MathTools, onOpen = { onOpen(it.id) }) }
    }
}
