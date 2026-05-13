package com.kkk19lll.flowersmobile.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.views.client.CatalogTab
import com.kkk19lll.flowersmobile.views.client.OrdersTab
import com.kkk19lll.flowersmobile.views.client.ProfileTab

@Composable
fun ClientMainView(
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        BottomNavItem("Каталог", "🛍️"),
        BottomNavItem("Заказы", "📦"),
        BottomNavItem("Профиль", "👤")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SoftPink, // Используем SoftPink вместо LeafGreen
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Text(
                                text = tab.emoji,
                                fontSize = 20.sp
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor = SoftPink.copy(alpha = 0.3f) // Используем SoftPink для индикатора
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> CatalogTab()
                1 -> OrdersTab()
                2 -> ProfileTab(onLogout = onLogout)
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val emoji: String
)

@Preview(showBackground = true, showSystemUi = true, name = "Главный экран клиента")
@Composable
fun PreviewClientMainView() {
    MaterialTheme {
        ClientMainView(onLogout = {})
    }
}