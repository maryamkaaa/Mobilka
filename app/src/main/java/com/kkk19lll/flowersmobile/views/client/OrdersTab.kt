package com.kkk19lll.flowersmobile.views.client

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.SoftPinkDark
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================== МОДЕЛИ ДАННЫХ ====================

@Serializable
data class Order(
    val id: Int,
    val totalAmount: Double,
    val created: String,
    val plannedDelivery: String?,
    val actualDelivery: String?,
    val status: String,
    val statusDescription: String?,
    val deliveryMethod: String,
    val paymentMethod: String,
    val completedPhoto: String?,
    val floristName: String?,
    val floristPhoto: String? = null,
    val bonusesUsed: Int = 0,
    val bonusesAwarded: Int = 0
)

@Serializable
data class OrderDetail(
    val id: Int,
    val totalAmount: Double,
    val created: String,
    val plannedDelivery: String?,
    val actualDelivery: String?,
    val deliveryAddress: String?,
    val clientNotes: String?,
    val completedPhoto: String?,
    val status: String,
    val statusDescription: String?,
    val deliveryMethod: String,
    val deliveryPrice: Double,
    val paymentMethod: String,
    val clientName: String,
    val clientPhone: String,
    val floristName: String?,
    val courierName: String?,
    val floristPhoto: String? = null,
    val bonusesUsed: Int = 0,
    val bonusesAwarded: Int = 0
)

@Serializable
data class OrderItem(
    val productName: String,
    val productPhoto: String?,
    val quantity: Int,
    val totalPrice: Double
)

@Serializable
data class OrderDetailsResponse(
    val order: OrderDetail,
    val items: List<OrderItem>
)

@Serializable
data class ReviewRequest(
    val clientId: Int,
    val productId: Int,
    val rating: Int,
    val comment: String
)

// ==================== СОСТОЯНИЕ ====================

class OrdersState {
    var orders by mutableStateOf<List<Order>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var selectedOrderId by mutableStateOf<Int?>(null)
    var orderDetails by mutableStateOf<OrderDetailsResponse?>(null)
    var isLoadingDetails by mutableStateOf(false)
    var activeTab by mutableStateOf(0)
}

// ==================== API МЕТОДЫ ====================

object OrdersApiService {
    private const val BASE_URL = "http://10.0.2.2:5034/api"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getClientOrders(clientId: Int): List<Order>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/Order/client/$clientId")
            Log.d("OrdersAPI", "Fetching orders from: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("OrdersAPI", "Orders response: $response")
                json.decodeFromString<List<Order>>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("OrdersAPI", "Error getting orders: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("OrdersAPI", "Error getting orders", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getOrderDetails(orderId: Int): OrderDetailsResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/Order/$orderId")
            Log.d("OrdersAPI", "Fetching order details from: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("OrdersAPI", "Order details response: $response")
                json.decodeFromString<OrderDetailsResponse>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("OrdersAPI", "Error getting order details: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("OrdersAPI", "Error getting order details", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun addReview(
        orderId: Int,
        clientId: Int,
        productId: Int,
        rating: Int,
        comment: String
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/Order/$orderId/review")
            Log.d("OrdersAPI", "Adding review to: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val request = ReviewRequest(clientId, productId, rating, comment)
            val jsonBody = json.encodeToString(request)

            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val success = responseCode in 200..299

            if (!success) {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("OrdersAPI", "Error adding review: $responseCode, error: $errorStream")
            }

            success
        } catch (e: Exception) {
            Log.e("OrdersAPI", "Error adding review", e)
            false
        } finally {
            connection?.disconnect()
        }
    }
}

// ==================== ОСНОВНОЙ ЭКРАН ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ordersState = remember { OrdersState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val userData = getCurrentUserData(context)

    LaunchedEffect(Unit, ordersState.activeTab) {
        if (userData?.clientId != null) {
            ordersState.isLoading = true
            ordersState.error = null

            try {
                val allOrders = OrdersApiService.getClientOrders(userData.clientId)
                if (allOrders != null) {
                    ordersState.orders = when (ordersState.activeTab) {
                        // ИСПРАВЛЕНО: Активные заказы - статусы "Новый" (1), "В работе" (2), "Собран" (3), "В доставке" (4), "Готов к выдаче" (5)
                        1 -> allOrders.filter {
                            it.status == "Новый" ||
                                    it.status == "В работе" ||
                                    it.status == "Собран" ||
                                    it.status == "В доставке" ||
                                    it.status == "Готов к выдаче"
                        }
                        // ИСПРАВЛЕНО: Завершенные заказы - статусы "Выполнен" (6)
                        2 -> allOrders.filter {
                            it.status == "Выполнен"
                        }
                        else -> allOrders
                    }
                    ordersState.error = null
                } else {
                    ordersState.error = "Не удалось загрузить заказы"
                }
            } catch (e: Exception) {
                ordersState.error = "Ошибка: ${e.message}"
                Log.e("OrdersTab", "Error loading orders", e)
            } finally {
                ordersState.isLoading = false
            }
        } else {
            ordersState.error = "Пользователь не авторизован"
        }
    }

    LaunchedEffect(ordersState.selectedOrderId) {
        if (ordersState.selectedOrderId != null) {
            ordersState.isLoadingDetails = true
            try {
                val details = OrdersApiService.getOrderDetails(ordersState.selectedOrderId!!)
                ordersState.orderDetails = details
                if (details == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Не удалось загрузить детали заказа")
                    }
                }
            } catch (e: Exception) {
                Log.e("OrdersTab", "Error loading order details", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Ошибка загрузки деталей")
                }
            } finally {
                ordersState.isLoadingDetails = false
            }
        } else {
            ordersState.orderDetails = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📦 Мои заказы",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = ordersState.activeTab,
                containerColor = Color.White,
                contentColor = SoftPink,
                indicator = { tabPositions ->
                    if (ordersState.activeTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[ordersState.activeTab])
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(SoftPink)
                        )
                    }
                }
            ) {
                listOf("Все", "Активные", "Выполненные").forEachIndexed { index, title ->
                    Tab(
                        selected = ordersState.activeTab == index,
                        onClick = { ordersState.activeTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = if (ordersState.activeTab == index) FontWeight.Medium else FontWeight.Normal,
                                color = if (ordersState.activeTab == index) SoftPink else TextPrimary
                            )
                        }
                    )
                }
            }

            when {
                ordersState.isLoading && ordersState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SoftPink)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Загрузка заказов...", color = WarmGray, fontSize = 14.sp)
                        }
                    }
                }

                ordersState.error != null && ordersState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "😕 ${ordersState.error!!}",
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (userData?.clientId != null) {
                                            ordersState.isLoading = true
                                            val orders = OrdersApiService.getClientOrders(userData.clientId)
                                            if (orders != null) {
                                                ordersState.orders = orders
                                                ordersState.error = null
                                            }
                                            ordersState.isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Повторить")
                            }
                        }
                    }
                }

                ordersState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "У вас пока нет заказов",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Перейдите в каталог, чтобы сделать первый заказ",
                                fontSize = 14.sp,
                                color = WarmGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ordersState.orders) { order ->
                            OrderCard(
                                order = order,
                                onClick = { ordersState.selectedOrderId = order.id }
                            )
                        }
                    }
                }
            }
        }

        if (ordersState.selectedOrderId != null && ordersState.orderDetails != null) {
            OrderDetailsBottomSheet(
                orderDetails = ordersState.orderDetails!!,
                isLoading = ordersState.isLoadingDetails,
                onDismiss = {
                    ordersState.selectedOrderId = null
                    ordersState.orderDetails = null
                },
                onAddReview = { productId, rating, comment ->
                    coroutineScope.launch {
                        if (userData?.clientId != null) {
                            val success = OrdersApiService.addReview(
                                orderId = ordersState.selectedOrderId!!,
                                clientId = userData.clientId,
                                productId = productId,
                                rating = rating,
                                comment = comment
                            )
                            if (success) {
                                snackbarHostState.showSnackbar("Спасибо за отзыв!")
                                val updatedDetails = OrdersApiService.getOrderDetails(ordersState.selectedOrderId!!)
                                if (updatedDetails != null) {
                                    ordersState.orderDetails = updatedDetails
                                }
                            } else {
                                snackbarHostState.showSnackbar("Не удалось добавить отзыв")
                            }
                        }
                    }
                }
            )
        }
    }
}

// ==================== КОМПОНЕНТЫ ====================

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Заказ #${order.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Сумма", fontSize = 12.sp, color = WarmGray)
                    Text(String.format("%.0f ₽", order.totalAmount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftPinkDark)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Дата заказа", fontSize = 12.sp, color = WarmGray)
                    Text(formatDate(order.created), fontSize = 14.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = WarmGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getDeliveryIcon(order.deliveryMethod), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(order.deliveryMethod, fontSize = 13.sp, color = WarmGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getPaymentIcon(order.paymentMethod), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(order.paymentMethod, fontSize = 13.sp, color = WarmGray)
                }
            }

            if (order.plannedDelivery != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Планируемая доставка: ${formatDate(order.plannedDelivery)}", fontSize = 12.sp, color = WarmGray)
                }
            }
        }
    }
}

// ИСПРАВЛЕНО: Статус бейдж с правильными цветами для всех статусов
@Composable
fun StatusBadge(status: String) {
    val (color, textColor) = when (status) {
        "Новый" -> Pair(Color(0xFF2196F3), Color.White)      // Синий
        "В работе" -> Pair(Color(0xFFFF9800), Color.White)   // Оранжевый
        "Собран" -> Pair(Color(0xFF9C27B0), Color.White)     // Фиолетовый
        "В доставке" -> Pair(Color(0xFF00BCD4), Color.White) // Голубой
        "Готов к выдаче" -> Pair(Color(0xFF3F51B5), Color.White) // Индиго
        "Выполнен" -> Pair(Color(0xFF4CAF50), Color.White)   // Зеленый
        "Отказ клиента" -> Pair(Color(0xFFFF5722), Color.White) // Оранжево-красный
        "Отменен" -> Pair(Color(0xFFF44336), Color.White)     // Красный
        else -> Pair(WarmGray, Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsBottomSheet(
    orderDetails: OrderDetailsResponse,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAddReview: (Int, Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var selectedProductName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SoftPink)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Заказ #${orderDetails.order.id}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatDate(orderDetails.order.created), fontSize = 14.sp, color = WarmGray)
                        }
                        StatusBadge(status = orderDetails.order.status)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("👤 Информация о получателе", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(orderDetails.order.clientName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(orderDetails.order.clientPhone, fontSize = 14.sp, color = WarmGray)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🚚 Доставка", fontSize = 12.sp, color = WarmGray)
                                Text(orderDetails.order.deliveryMethod, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("${String.format("%.0f", orderDetails.order.deliveryPrice)} ₽", fontSize = 12.sp, color = SoftPinkDark)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("💳 Оплата", fontSize = 12.sp, color = WarmGray)
                                Text(orderDetails.order.paymentMethod, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }
                    }
                }

                if (!orderDetails.order.deliveryAddress.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📍 Адрес доставки", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(orderDetails.order.deliveryAddress, fontSize = 14.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                if (!orderDetails.order.clientNotes.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💬 Комментарий", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(orderDetails.order.clientNotes, fontSize = 14.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                item {
                    Text("🌸 Состав заказа", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                }

                items(orderDetails.items) { item ->
                    OrderItemCard(
                        item = item,
                        orderStatus = orderDetails.order.status,
                        onAddReview = { productId, productName ->
                            selectedProductId = productId
                            selectedProductName = productName
                            showReviewDialog = true
                        }
                    )
                }

                item {
                    Divider(color = WarmGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Итого к оплате:", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(String.format("%.0f ₽", orderDetails.order.totalAmount), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SoftPinkDark)
                    }
                }

                if (orderDetails.order.floristName != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("👩‍🌾 Флорист: ${orderDetails.order.floristName}", fontSize = 12.sp, color = WarmGray)
                            if (orderDetails.order.courierName != null) {
                                Text("🚚 Курьер: ${orderDetails.order.courierName}", fontSize = 12.sp, color = WarmGray)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showReviewDialog && selectedProductId != null) {
        ReviewDialog(
            productName = selectedProductName,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                onAddReview(selectedProductId!!, rating, comment)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun OrderItemCard(
    item: OrderItem,
    orderStatus: String,
    onAddReview: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SoftPink.copy(alpha = 0.2f), SoftPink.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(getFlowerEmoji(item.productName), fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${item.quantity} шт", fontSize = 12.sp, color = WarmGray)
                Text(String.format("%.0f ₽", item.totalPrice), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SoftPinkDark)
            }

            // ИСПРАВЛЕНО: Отзыв можно оставить только для выполненных заказов (статус "Выполнен")
            if (orderStatus == "Выполнен") {
                IconButton(
                    onClick = { onAddReview(1, item.productName) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("💬", fontSize = 20.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    productName: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оставить отзыв", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("Товар: $productName", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Ваша оценка:", fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= rating) "★" else "☆",
                            fontSize = 32.sp,
                            color = if (i <= rating) SoftPink else WarmGray,
                            modifier = Modifier.clickable { rating = i }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Ваш комментарий (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPink,
                        focusedLabelColor = SoftPink
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, comment) }, enabled = rating in 1..5) {
                Text("Отправить", color = SoftPink, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = WarmGray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

private fun formatDate(dateString: String): String {
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

private fun getDeliveryIcon(deliveryMethod: String): String {
    return when {
        deliveryMethod.contains("Самовывоз") -> "🏪"
        deliveryMethod.contains("Доставка") -> "🚚"
        else -> "📦"
    }
}

private fun getPaymentIcon(paymentMethod: String): String {
    return when {
        paymentMethod.contains("Картой") -> "💳"
        paymentMethod.contains("Наличными") -> "💰"
        else -> "💵"
    }
}

private fun getFlowerEmoji(name: String): String {
    return when {
        name.contains("Роза") || name.contains("роза") -> "🌹"
        name.contains("Тюльпан") || name.contains("тюльпан") -> "🌷"
        name.contains("Ромашка") || name.contains("ромашка") -> "🌼"
        name.contains("Пион") || name.contains("пион") -> "🌸"
        name.contains("Лилия") || name.contains("лилия") -> "🌺"
        name.contains("Хризантема") || name.contains("хризантема") -> "🌻"
        name.contains("Орхидея") || name.contains("орхидея") -> "🦋"
        name.contains("Лаванда") || name.contains("лаванда") -> "💜"
        name.contains("Гортензия") || name.contains("гортензия") -> "💙"
        name.contains("Гербера") || name.contains("гербера") -> "🌼"
        else -> "🌸"
    }
}