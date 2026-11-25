package com.example.blelearning.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.blelearning.presentation.dashboard.DashboardScreen
import com.example.blelearning.presentation.device.DeviceScreen
import com.example.blelearning.presentation.gatt.GattScreen
import com.example.blelearning.presentation.logs.LogsScreen
import com.example.blelearning.presentation.notification.NotificationScreen
import com.example.blelearning.presentation.scanner.ScannerScreen
import java.util.UUID

object Routes {
    const val DASHBOARD = "dashboard"
    const val SCANNER = "scanner"
    const val DEVICE = "device"
    const val NOTIFICATIONS = "notifications"
    const val LOGS = "logs"
    const val GATT = "gatt/{service}/{characteristic}"
    fun gatt(service: UUID, characteristic: UUID) = "gatt/$service/$characteristic"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onScan = { nav.navigate(Routes.SCANNER) },
                onDevice = { nav.navigate(Routes.DEVICE) },
                onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onLogs = { nav.navigate(Routes.LOGS) },
            )
        }
        composable(Routes.SCANNER) {
            ScannerScreen(onBack = back, onConnecting = { nav.navigate(Routes.DEVICE) })
        }
        composable(Routes.DEVICE) {
            DeviceScreen(
                onBack = back,
                onOpenCharacteristic = { s, c -> nav.navigate(Routes.gatt(s, c)) },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
            )
        }
        composable(
            Routes.GATT,
            arguments = listOf(
                navArgument("service") { type = NavType.StringType },
                navArgument("characteristic") { type = NavType.StringType },
            ),
        ) {
            GattScreen(onBack = back, onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
        }
        composable(Routes.NOTIFICATIONS) { NotificationScreen(onBack = back) }
        composable(Routes.LOGS) { LogsScreen(onBack = back) }
    }
}
