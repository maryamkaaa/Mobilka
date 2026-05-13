package com.kkk19lll.flowersmobile.views.courier

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================== МОДЕЛИ ДАННЫХ ====================

@Serializable
data class CourierOrder(
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

// ==================== API МЕТОДЫ ====================

object CourierApiService {
    private const val BASE_URL = "http://10.0.2.2:5034/api/Courier"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getShiftStatus(employeeId: Int): Boolean? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/shift/status")
            Log.d("CourierAPI", "Get shift status URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "Get shift status response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierAPI", "Shift status response: $response")
                val jsonObject = json.decodeFromString<JsonObject>(response)
                jsonObject["isActive"]?.jsonPrimitive?.boolean
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error getting shift status", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun startShift(employeeId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/shift/start")
            Log.d("CourierAPI", "Start shift URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "Start shift response code: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error starting shift", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun endShift(employeeId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/shift/end")
            Log.d("CourierAPI", "End shift URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "End shift response code: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error ending shift", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getMyOrders(employeeId: Int): List<CourierOrder>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/orders")
            Log.d("CourierAPI", "Get my orders URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "Get my orders response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierAPI", "My orders response: $response")
                json.decodeFromString<List<CourierOrder>>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CourierAPI", "Error getting orders: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error getting orders", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getAvailableOrders(): List<CourierOrder>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/available")
            Log.d("CourierAPI", "Get available orders URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "Available orders response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierAPI", "Available orders response: $response")
                json.decodeFromString<List<CourierOrder>>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CourierAPI", "Error getting available orders: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error getting available orders", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun assignOrder(employeeId: Int, orderId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/orders/$orderId/assign")
            Log.d("CourierAPI", "Assign order URL: $url")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("CourierAPI", "Assign order response code: $responseCode")

            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierAPI", "Assign order response: $response")
                true
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CourierAPI", "Error assigning order: $responseCode, error: $errorStream")
                false
            }
        } catch (e: Exception) {
            Log.e("CourierAPI", "Error assigning order", e)
            false
        } finally {
            connection?.disconnect()
        }
    }
}

// ==================== ОСНОВНОЙ ЭКРАН ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userData = getCurrentUserData(context)

    var isShiftActive by remember { mutableStateOf(false) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var shiftStartTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var myOrders by remember { mutableStateOf<List<CourierOrder>>(emptyList()) }
    var availableOrders by remember { mutableStateOf<List<CourierOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAvailableOrders by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }

    // Загрузка статуса смены при запуске экрана
    LaunchedEffect(userData?.employeeId) {
        if (userData?.employeeId != null) {
            isLoadingInitial = true
            val status = CourierApiService.getShiftStatus(userData.employeeId)
            if (status != null) {
                isShiftActive = status
                if (isShiftActive) {
                    shiftStartTime = LocalDateTime.now()
                    val orders = CourierApiService.getMyOrders(userData.employeeId)
                    myOrders = orders ?: emptyList()
                    debugInfo = "Найдено заказов: ${orders?.size ?: 0}, ID курьера: ${userData.employeeId}"
                }
            }
            isLoadingInitial = false
        }
    }

    // Загрузка заказов при активации смены
    LaunchedEffect(isShiftActive, userData?.employeeId) {
        if (isShiftActive && userData?.employeeId != null) {
            isLoading = true
            val orders = CourierApiService.getMyOrders(userData.employeeId)
            myOrders = orders ?: emptyList()
            debugInfo = "Найдено заказов: ${orders?.size ?: 0}, ID курьера: ${userData.employeeId}"
            isLoading = false
        } else if (!isShiftActive) {
            myOrders = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🚚 Курьерская смена",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoadingInitial) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SoftPink)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Загрузка статуса смены...", color = WarmGray)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Карточка управления сменой
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isShiftActive) SoftPink.copy(alpha = 0.1f) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isShiftActive) "✅ Смена активна" else "⏸ Смена не начата",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isShiftActive) SoftPink else Color.Gray
                            )

                            if (isShiftActive && shiftStartTime != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Начало: ${shiftStartTime!!.format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"))}",
                                    fontSize = 12.sp,
                                    color = WarmGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "📦 Мои заказы: ${myOrders.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SoftPink
                                )

                                if (debugInfo.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = debugInfo,
                                        fontSize = 10.sp,
                                        color = WarmGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (userData?.employeeId != null) {
                                                if (!isShiftActive) {
                                                    val success = CourierApiService.startShift(userData.employeeId)
                                                    if (success) {
                                                        isShiftActive = true
                                                        shiftStartTime = LocalDateTime.now()
                                                        snackbarHostState.showSnackbar("Смена начата")

                                                        isLoading = true
                                                        val orders = CourierApiService.getMyOrders(userData.employeeId)
                                                        myOrders = orders ?: emptyList()
                                                        debugInfo = "Найдено заказов: ${orders?.size ?: 0}"
                                                        isLoading = false
                                                    } else {
                                                        snackbarHostState.showSnackbar("Ошибка начала смены")
                                                    }
                                                } else {
                                                    val success = CourierApiService.endShift(userData.employeeId)
                                                    if (success) {
                                                        isShiftActive = false
                                                        shiftStartTime = null
                                                        myOrders = emptyList()
                                                        showAvailableOrders = false
                                                        snackbarHostState.showSnackbar("Смена завершена")
                                                    } else {
                                                        snackbarHostState.showSnackbar("Ошибка завершения смены")
                                                    }
                                                }
                                            } else {
                                                snackbarHostState.showSnackbar("Пользователь не авторизован")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isShiftActive) Color.Red.copy(alpha = 0.7f) else SoftPink,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isShiftActive) "Завершить" else "Начать", fontSize = 14.sp)
                                }

                                if (isShiftActive) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isLoading = true
                                                val orders = CourierApiService.getAvailableOrders()
                                                availableOrders = orders ?: emptyList()
                                                showAvailableOrders = true
                                                isLoading = false

                                                if (availableOrders.isEmpty()) {
                                                    snackbarHostState.showSnackbar("Нет доступных заказов")
                                                } else {
                                                    snackbarHostState.showSnackbar("Найдено ${availableOrders.size} доступных заказов")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SoftPink,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📋 Доступные", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when {
                        !isShiftActive -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🚚", fontSize = 64.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Начните смену чтобы видеть заказы",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Нажмите кнопку \"Начать\" выше",
                                        fontSize = 14.sp,
                                        color = WarmGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        showAvailableOrders -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Доступные заказы",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(onClick = { showAvailableOrders = false }) {
                                        Text("Назад", color = SoftPink)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = SoftPink)
                                    }
                                } else if (availableOrders.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("📭", fontSize = 48.sp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Нет доступных заказов",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Все заказы уже назначены курьерам",
                                                fontSize = 14.sp,
                                                color = WarmGray
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(availableOrders) { order ->
                                            AvailableOrderCard(
                                                order = order,
                                                onAssign = {
                                                    coroutineScope.launch {
                                                        if (userData?.employeeId != null) {
                                                            isLoading = true
                                                            val success = CourierApiService.assignOrder(userData.employeeId, order.id)
                                                            if (success) {
                                                                snackbarHostState.showSnackbar("Заказ #${order.id} назначен")
                                                                val newMyOrders = CourierApiService.getMyOrders(userData.employeeId)
                                                                myOrders = newMyOrders ?: emptyList()
                                                                val newAvailableOrders = CourierApiService.getAvailableOrders()
                                                                availableOrders = newAvailableOrders ?: emptyList()
                                                                debugInfo = "Мои заказы: ${myOrders.size}, Доступные: ${availableOrders.size}"

                                                                if (availableOrders.isEmpty()) {
                                                                    showAvailableOrders = false
                                                                }
                                                            } else {
                                                                snackbarHostState.showSnackbar("Не удалось назначить заказ")
                                                            }
                                                            isLoading = false
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        isLoading -> {
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

                        myOrders.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📭", fontSize = 64.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Нет назначенных заказов",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Нажмите кнопку \"Доступные\" чтобы взять заказ",
                                        fontSize = 14.sp,
                                        color = WarmGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Мои заказы",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(myOrders) { order ->
                                        OrderCard(order = order)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== КАРТОЧКА ДОСТУПНОГО ЗАКАЗА ====================

@Composable
fun AvailableOrderCard(order: CourierOrder, onAssign: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                Text(
                    "Заказ #${order.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Сумма", fontSize = 12.sp, color = WarmGray)
                    Text(
                        "${String.format("%.0f", order.totalAmount)} ₽",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftPink
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Дата заказа", fontSize = 12.sp, color = WarmGray)
                    Text(formatDate(order.created), fontSize = 13.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = WarmGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 ", fontSize = 14.sp)
                Text(order.clientName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("📞 ${order.clientPhone}", fontSize = 12.sp, color = WarmGray)
            }

            if (!order.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("📍 ", fontSize = 14.sp)
                    Text(order.address, fontSize = 13.sp, color = WarmGray)
                }
            }

            if (order.plannedDelivery != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ ", fontSize = 12.sp)
                    Text(
                        "Доставить к: ${formatDate(order.plannedDelivery)}",
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAssign,
                colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Назначить себе", fontSize = 14.sp)
            }
        }
    }
}

// ==================== КАРТОЧКА ЗАКАЗА ====================

@Composable
fun OrderCard(order: CourierOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                Text(
                    "Заказ #${order.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Сумма", fontSize = 12.sp, color = WarmGray)
                    Text(
                        "${String.format("%.0f", order.totalAmount)} ₽",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftPink
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Дата заказа", fontSize = 12.sp, color = WarmGray)
                    Text(formatDate(order.created), fontSize = 13.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = WarmGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 ", fontSize = 14.sp)
                Text(order.clientName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("📞 ${order.clientPhone}", fontSize = 12.sp, color = WarmGray)
            }

            if (!order.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("📍 ", fontSize = 14.sp)
                    Text(order.address, fontSize = 13.sp, color = WarmGray)
                }
            }

            if (order.plannedDelivery != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ ", fontSize = 12.sp)
                    Text(
                        "Доставить к: ${formatDate(order.plannedDelivery)}",
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getPaymentIcon(order.paymentMethod), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Оплата: ${order.paymentMethod}", fontSize = 12.sp, color = WarmGray)
            }

            if (!order.clientNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
                ) {
                    Text(
                        "💬 ${order.clientNotes}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ====================

@Composable
fun StatusBadge(status: String) {
    val (color, textColor) = when (status) {
        "Собран" -> Pair(Color(0xFF9C27B0), Color.White)
        "Готов к доставке", "Готов к выдаче" -> Pair(Color(0xFF3F51B5), Color.White)
        "В доставке" -> Pair(Color(0xFF00BCD4), Color.White)
        "Выполнен" -> Pair(Color(0xFF4CAF50), Color.White)
        "Отменен" -> Pair(Color(0xFFF44336), Color.White)
        else -> Pair(WarmGray, Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
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

private fun getPaymentIcon(paymentMethod: String): String {
    return when {
        paymentMethod.contains("Картой") -> "💳"
        paymentMethod.contains("Наличными") -> "💰"
        else -> "💵"
    }
}