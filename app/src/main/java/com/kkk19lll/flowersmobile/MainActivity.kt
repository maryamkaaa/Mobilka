package com.kkk19lll.flowersmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kkk19lll.flowersmobile.navigation.Screen
import com.kkk19lll.flowersmobile.ui.theme.FlowersMobileTheme
import com.kkk19lll.flowersmobile.views.ClientMainView
import com.kkk19lll.flowersmobile.views.CourierMainView
import com.kkk19lll.flowersmobile.views.LoginResponse
import com.kkk19lll.flowersmobile.views.LoginView
import com.kkk19lll.flowersmobile.views.UserRole
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import com.kkk19lll.flowersmobile.views.logout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowersMobileTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Состояние для отслеживания данных пользователя
                var currentUserData by remember { mutableStateOf(getCurrentUserData(context)) }

                // Функция для обработки успешного входа
                fun handleLoginSuccess(response: LoginResponse) {
                    currentUserData = response
                    when (response.role) {
                        UserRole.CLIENT -> navController.navigate(Screen.ClientMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        UserRole.COURIER -> navController.navigate(Screen.CourierMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }

                // Функция для выхода
                fun handleLogout() {
                    logout(context)
                    currentUserData = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (currentUserData != null) {
                            when (currentUserData?.role) {
                                UserRole.CLIENT -> Screen.ClientMain.route
                                UserRole.COURIER -> Screen.CourierMain.route
                                else -> Screen.Login.route
                            }
                        } else {
                            Screen.Login.route
                        },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginView(
                                onNavigateToClient = { response -> handleLoginSuccess(response) },
                                onNavigateToCourier = { response -> handleLoginSuccess(response) },
                                onForgotPasswordClick = {
                                    // TODO: Implement forgot password
                                }
                            )
                        }

                        composable(Screen.ClientMain.route) {
                            ClientMainView(
                                onLogout = { handleLogout() }
                            )
                        }

                        composable(Screen.CourierMain.route) {
                            CourierMainView(
                                onLogout = { handleLogout() }
                            )
                        }
                    }
                }
            }
        }
    }
}