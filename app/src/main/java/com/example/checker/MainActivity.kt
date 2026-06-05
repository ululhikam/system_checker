package com.example.checker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.checker.data.CheckerRepository
import com.example.checker.ui.DashboardScreen
import com.example.checker.ui.HistoryScreen
import com.example.checker.ui.HoaxScreen
import com.example.checker.ui.ScamScreen
import com.example.checker.ui.theme.CheckerTheme
import com.example.checker.ui.theme.CardBorder
import com.example.checker.ui.theme.ObsidianBg
import com.example.checker.ui.theme.NeonGreen
import com.example.checker.ui.theme.TextSteel

class MainActivity : ComponentActivity() {

    private val repository = CheckerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestNotificationPermission()

        setContent {
            CheckerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBg,
                    bottomBar = {
                        NavigationBar(
                            containerColor = ObsidianBg,
                            tonalElevation = 0.dp
                        ) {
                            val items = listOf(
                                NavigationItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                                NavigationItem("hoax", "Hoax Cek", Icons.Default.FactCheck),
                                NavigationItem("scam", "Scam Pindai", Icons.Default.Security),
                                NavigationItem("history", "Riwayat", Icons.Default.History)
                            )

                            items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        unselectedIconColor = TextSteel,
                                        selectedTextColor = NeonGreen,
                                        unselectedTextColor = TextSteel,
                                        indicatorColor = CardBorder
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(navController = navController, repository = repository)
                        }
                        composable("hoax") {
                            HoaxScreen(repository = repository)
                        }
                        composable("scam") {
                            ScamScreen(repository = repository)
                        }
                        composable("history") {
                            HistoryScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2026
                )
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
