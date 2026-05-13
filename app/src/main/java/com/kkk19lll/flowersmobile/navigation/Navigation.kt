package com.kkk19lll.flowersmobile.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ClientMain : Screen("client_main")
    object CourierMain : Screen("courier_main")
}