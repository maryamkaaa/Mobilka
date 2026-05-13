package com.kkk19lll.flowersmobile.views.courier

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================== МОДЕЛИ ДАННЫХ ====================

@Serializable
data class CourierHistoryOrder(
    @kotlinx.serialization.SerialName("id")
    val id: Int,
    @kotlinx.serialization.SerialName("totalAmount")
    val totalAmount: Double,
    @kotlinx.serialization.SerialName("created")
    val created: String,
    @kotlinx.serialization.SerialName("plannedDelivery")
    val plannedDelivery: String?,
    @kotlinx.serialization.SerialName("address")
    val address: String?,
    @kotlinx.serialization.SerialName("clientNotes")
    val clientNotes: String?,
    @kotlinx.serialization.SerialName("completedPhoto")
    val completedPhoto: String?,
    @kotlinx.serialization.SerialName("status")
    val status: String,
    @kotlinx.serialization.SerialName("clientName")
    val clientName: String,
    @kotlinx.serialization.SerialName("clientPhone")
    val clientPhone: String,
    @kotlinx.serialization.SerialName("paymentMethod")
    val paymentMethod: String
)

@Serializable
data class CourierStatisticsData(
    @kotlinx.serialization.SerialName("totalDeliveries")
    val totalDeliveries: Long,
    @kotlinx.serialization.SerialName("completedDeliveries")
    val completedDeliveries: Long,
    @kotlinx.serialization.SerialName("avgDelayMinutes")
    val avgDelayMinutes: Double
)

// ==================== OFFLINE DATA STORAGE ====================

object OfflineDataStorage {
    private var cachedAddresses: List<CourierHistoryOrder> = emptyList()

    fun saveAddresses(orders: List<CourierHistoryOrder>) {
        cachedAddresses = orders.filter { !it.address.isNullOrBlank() }
    }

    fun getAddresses(): List<CourierHistoryOrder> = cachedAddresses
}

// ==================== API МЕТОДЫ ====================

object CourierProfileApiService {
    private const val BASE_URL = "http://10.0.2.2:5034/api/Courier"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getCourierStats(employeeId: Int): CourierStatisticsData? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/stats")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<CourierStatisticsData>(response)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CourierProfileAPI", "Error getting stats", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getOrderHistory(employeeId: Int): List<CourierHistoryOrder>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/orders")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<List<CourierHistoryOrder>>(response)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CourierProfileAPI", "Error getting history", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}

// ==================== ОСНОВНОЙ ЭКРАН ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierProfileTab(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val userData = getCurrentUserData(context)

    var isLoadingStats by remember { mutableStateOf(true) }
    var courierStats by remember { mutableStateOf<CourierStatisticsData?>(null) }
    var orderHistory by remember { mutableStateOf<List<CourierHistoryOrder>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }
    var selectedOrderForDetails by remember { mutableStateOf<CourierHistoryOrder?>(null) }
    var showOfflineInfo by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(userData?.employeeId) {
        if (userData?.employeeId != null) {
            isLoadingStats = true
            val stats = CourierProfileApiService.getCourierStats(userData.employeeId)
            courierStats = stats
            isLoadingStats = false

            isLoadingHistory = true
            val orders = CourierProfileApiService.getOrderHistory(userData.employeeId)
            val completedOrders = orders?.filter { it.status == "Выполнен" } ?: emptyList()
            orderHistory = completedOrders
            OfflineDataStorage.saveAddresses(completedOrders)
            isLoadingHistory = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Привет, ${userData?.email?.split("@")?.first() ?: "Курьер"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            "Готов к работе 🚚",
                            fontSize = 12.sp,
                            color = SoftPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Выйти",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                selectedOrderForDetails != null -> {
                    HistoryOrderDetailsDialog(
                        order = selectedOrderForDetails!!,
                        onDismiss = { selectedOrderForDetails = null }
                    )
                }
                showOfflineInfo -> {
                    OfflineAddressesDialog(
                        addresses = OfflineDataStorage.getAddresses(),
                        onDismiss = { showOfflineInfo = false }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8F9FA))
                    ) {
                        HeroSection(
                            userName = userData?.email?.split("@")?.first() ?: "Курьер",
                            stats = courierStats,
                            isLoading = isLoadingStats
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = SoftPink,
                            divider = {
                                Divider(
                                    color = Color(0xFFE9ECEF),
                                    thickness = 1.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📜", fontSize = 16.sp)
                                        Text("История", fontSize = 14.sp)
                                    }
                                },
                                selectedContentColor = SoftPink,
                                unselectedContentColor = WarmGray
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📱", fontSize = 16.sp)
                                        Text("Офлайн", fontSize = 14.sp)
                                    }
                                },
                                selectedContentColor = SoftPink,
                                unselectedContentColor = WarmGray
                            )
                        }

                        when (selectedTab) {
                            0 -> HistoryTab(
                                orderHistory = orderHistory,
                                isLoading = isLoadingHistory,
                                onOrderClick = { selectedOrderForDetails = it }
                            )
                            1 -> OfflineTab(
                                addressesCount = OfflineDataStorage.getAddresses().size,
                                onShowDetails = { showOfflineInfo = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== HERO SECTION ====================

@Composable
fun HeroSection(
    userName: String,
    stats: CourierStatisticsData?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SoftPink, SoftPink.copy(alpha = 0.7f)),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.take(2).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Добро пожаловать! 👋",
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                    Text(
                        userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShimmerStatCard()
                    ShimmerStatCard()
                    ShimmerStatCard()
                }
            } else if (stats != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        value = "${stats.totalDeliveries}",
                        label = "Всего",
                        emoji = "📦",
                        color = Color(0xFF4CAF50)
                    )
                    StatCard(
                        value = "${stats.completedDeliveries}",
                        label = "Выполнено",
                        emoji = "✅",
                        color = Color(0xFF2196F3)
                    )
                    StatCard(
                        value = "${String.format("%.0f", stats.avgDelayMinutes)}",
                        label = "мин",
                        emoji = "⏰",
                        color = Color(0xFFFF9800)
                    )
                }
            } else {
                Text(
                    "Нет данных статистики",
                    fontSize = 14.sp,
                    color = WarmGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RowScope.StatCard(
    value: String,
    label: String,
    emoji: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RowScope.ShimmerStatCard() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFE9ECEF),
            modifier = Modifier.size(48.dp)
        ) {}
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFFE9ECEF),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .width(50.dp)
                .height(20.dp)
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = Color(0xFFE9ECEF),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .width(60.dp)
                .height(12.dp)
        ) {}
    }
}

// ==================== HISTORY TAB ====================

@Composable
fun HistoryTab(
    orderHistory: List<CourierHistoryOrder>,
    isLoading: Boolean,
    onOrderClick: (CourierHistoryOrder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            items(5) {
                ShimmerOrderCard()
            }
        } else if (orderHistory.isEmpty()) {
            item {
                EmptyStateView(
                    emoji = "📦",
                    title = "Нет доставок",
                    description = "Выполненные заказы появятся здесь"
                )
            }
        } else {
            items(orderHistory) { order ->
                ModernOrderCard(
                    order = order,
                    onClick = { onOrderClick(order) }
                )
            }
        }
    }
}

@Composable
fun ModernOrderCard(
    order: CourierHistoryOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftPink.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎁", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Заказ #${order.id}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = formatHistoryDate(order.created),
                            fontSize = 11.sp,
                            color = WarmGray
                        )
                    }
                }

                ModernStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!order.address.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("📍", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.address,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("👤", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.clientName,
                        fontSize = 12.sp,
                        color = WarmGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "${String.format("%.0f", order.totalAmount)} ₽",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPink
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📞", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = order.clientPhone,
                    fontSize = 11.sp,
                    color = WarmGray
                )
            }
        }
    }
}

@Composable
fun ModernStatusBadge(status: String) {
    val (color, bgColor) = when (status) {
        "Выполнен" -> Pair(Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.1f))
        "В доставке" -> Pair(Color(0xFF2196F3), Color(0xFF2196F3).copy(alpha = 0.1f))
        "Готов к доставке" -> Pair(Color(0xFFFF9800), Color(0xFFFF9800).copy(alpha = 0.1f))
        else -> Pair(WarmGray, WarmGray.copy(alpha = 0.1f))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Text(
            status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ShimmerOrderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE9ECEF),
                        modifier = Modifier.size(40.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Surface(
                            color = Color(0xFFE9ECEF),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                        ) {}
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFE9ECEF),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                        ) {}
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE9ECEF),
                    modifier = Modifier
                        .width(80.dp)
                        .height(28.dp)
                ) {}
            }
        }
    }
}

// ==================== OFFLINE TAB ====================

@Composable
fun OfflineTab(
    addressesCount: Int,
    onShowDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            shape = CircleShape,
            color = SoftPink.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("📱", fontSize = 48.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Офлайн-доступ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Сохраненные адреса и телефоны доступны без интернета",
            fontSize = 14.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Сохранено адресов",
                    fontSize = 14.sp,
                    color = WarmGray
                )
                Text(
                    "$addressesCount",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPink
                )
                Text(
                    "доставок",
                    fontSize = 14.sp,
                    color = WarmGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onShowDetails,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftPink
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Просмотреть все 📋", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Данные обновляются автоматически\nпосле выполнения заказов",
            fontSize = 11.sp,
            color = WarmGray,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== ДИАЛОГИ ====================

@Composable
fun HistoryOrderDetailsDialog(
    order: CourierHistoryOrder,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SoftPink.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📋", fontSize = 24.sp)
                    }
                }
                Column {
                    Text(
                        text = "Заказ #${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = formatHistoryDate(order.created),
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DetailSection(
                            title = "Клиент",
                            emoji = "👤"
                        ) {
                            DetailRow("Имя", order.clientName)
                            DetailRow("Телефон", order.clientPhone)
                        }
                    }

                    item {
                        DetailSection(
                            title = "Адрес доставки",
                            emoji = "📍"
                        ) {
                            Text(
                                text = order.address ?: "Не указан",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    item {
                        DetailSection(
                            title = "Информация о заказе",
                            emoji = "ℹ️"
                        ) {
                            DetailRow("Сумма", "${String.format("%.0f", order.totalAmount)} ₽")
                            DetailRow("Оплата", order.paymentMethod)
                            DetailRow("Статус", order.status)
                            if (order.plannedDelivery != null) {
                                DetailRow("Плановая доставка", formatHistoryDate(order.plannedDelivery))
                            }
                        }
                    }

                    if (!order.clientNotes.isNullOrBlank()) {
                        item {
                            DetailSection(
                                title = "Комментарий",
                                emoji = "💬"
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8F9FA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = order.clientNotes,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftPink
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть ❌", color = Color.White)
            }
        }
    )
}

@Composable
fun OfflineAddressesDialog(
    addresses: List<CourierHistoryOrder>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SoftPink.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📱", fontSize = 24.sp)
                    }
                }
                Column {
                    Text(
                        text = "Офлайн-доступ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${addresses.size} сохраненных адресов",
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }
            }
        },
        text = {
            if (addresses.isEmpty()) {
                EmptyStateView(
                    emoji = "📭",
                    title = "Нет сохраненных адресов",
                    description = "Адреса появятся здесь после выполнения заказов"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(addresses) { order ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8F9FA)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "📍 ${order.address ?: "Адрес не указан"}",
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "👤 ${order.clientName}",
                                        fontSize = 12.sp,
                                        color = WarmGray
                                    )
                                    Text(
                                        text = "📞 ${order.clientPhone}",
                                        fontSize = 12.sp,
                                        color = WarmGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftPink
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть ❌", color = Color.White)
            }
        }
    )
}

@Composable
fun DetailSection(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = WarmGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
fun EmptyStateView(
    emoji: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = WarmGray.copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 40.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = WarmGray,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

private fun formatHistoryDate(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime = LocalDateTime.parse(dateString, formatter)
        dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (e: Exception) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val dateTime = LocalDateTime.parse(dateString, formatter)
            dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        } catch (e2: Exception) {
            dateString.take(10)
        }
    }
}