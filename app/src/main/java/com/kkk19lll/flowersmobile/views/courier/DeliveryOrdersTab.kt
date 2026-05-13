package com.kkk19lll.flowersmobile.views.courier

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// ==================== МОДЕЛИ ДАННЫХ ====================

@Serializable
data class CourierDeliveryTask(
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
data class CourierDeliveryAction(
    val action: String,
    val idOrder: Int,
    val photoAtDoor: String? = null,
    val problemDescription: String? = null
)

// ==================== API МЕТОДЫ ====================

object CourierDeliveryApi {
    private const val BASE_URL = "http://10.0.2.2:5034/api/Courier"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getCourierOrders(employeeId: Int): List<CourierDeliveryTask>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$employeeId/orders")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierDeliveryApi", "Response: $response")
                json.decodeFromString<List<CourierDeliveryTask>>(response)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CourierDeliveryApi", "Error response code: $responseCode, body: $errorResponse")
                null
            }
        } catch (e: Exception) {
            Log.e("CourierDeliveryApi", "Error getting orders", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun performCourierAction(request: CourierDeliveryAction): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/action")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            val requestBody = json.encodeToString(request)
            Log.d("CourierDeliveryApi", "Request body length: ${requestBody.length}")
            Log.d("CourierDeliveryApi", "Request body: $requestBody")

            connection.outputStream.write(requestBody.toByteArray())
            connection.outputStream.flush()

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CourierDeliveryApi", "Success response: $response")
                true
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CourierDeliveryApi", "Error: $responseCode - $responseMessage, body: $errorResponse")
                false
            }
        } catch (e: Exception) {
            Log.e("CourierDeliveryApi", "Error performing action", e)
            false
        } finally {
            connection?.disconnect()
        }
    }
}

// ==================== UI КОМПОНЕНТЫ ====================

@Composable
fun DeliveryStatusBadge(status: String) {
    val (color, emoji) = when (status) {
        "Готов к доставке", "Готов к выдаче" -> Color(0xFF3F51B5) to "📦"
        "В доставке" -> Color(0xFF00BCD4) to "🚚"
        "Выполнен" -> Color(0xFF4CAF50) to "✅"
        else -> WarmGray to "📋"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (status) {
                    "Готов к доставке", "Готов к выдаче" -> "Готов"
                    "В доставке" -> "В пути"
                    else -> status
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun DeliveryTaskCard(
    task: CourierDeliveryTask,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = "Заказ #${task.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                DeliveryStatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("👤 ${task.clientName}", fontSize = 14.sp, color = TextPrimary)
            Text("📞 ${task.clientPhone}", fontSize = 12.sp, color = SoftPink)
            Text(
                text = "📍 ${task.address ?: "Адрес не указан"}",
                fontSize = 13.sp,
                color = WarmGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0xFFEEEEEE))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💰 ${String.format("%.0f", task.totalAmount)} ₽",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPink
                )
                Text(
                    text = "${getPaymentIcon(task.paymentMethod)} ${task.paymentMethod}",
                    fontSize = 12.sp,
                    color = WarmGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (task.status == "В доставке") "✅ Подтвердить доставку" else "📦 Начать доставку",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryExecutionDialog(
    context: android.content.Context,
    task: CourierDeliveryTask,
    hasPhoto: Boolean,
    isCashCollected: Boolean,
    onDismiss: () -> Unit,
    onPhotoRequest: () -> Unit,
    onCashCollectedChange: (Boolean) -> Unit,
    onPickup: () -> Unit,
    onDelivered: () -> Unit,
    onProblem: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Заказ #${task.id}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✖️", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DeliveryStatusBadge(status = task.status)

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("👤 ${task.clientName}", fontSize = 14.sp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${task.clientPhone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("📞 ", fontSize = 14.sp)
                            Text(task.clientPhone, fontSize = 14.sp, color = SoftPink)
                        }

                        Text("📍 ${task.address ?: "Адрес не указан"}", fontSize = 14.sp)

                        if (!task.address.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    val uri = Uri.parse("geo:0,0?q=${task.address}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                modifier = Modifier.padding(start = 0.dp)
                            ) {
                                Text("🧭 Открыть в навигаторе", fontSize = 12.sp)
                            }
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💰 Сумма:", color = WarmGray)
                            Text("${String.format("%.0f", task.totalAmount)} ₽", fontWeight = FontWeight.Bold, color = SoftPink)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💳 Оплата:", color = WarmGray)
                            Text("${getPaymentIcon(task.paymentMethod)} ${task.paymentMethod}")
                        }

                        if (!task.clientNotes.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("💬", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(task.clientNotes, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    task.status == "Готов к доставке" || task.status == "Готов к выдаче" -> {
                        Button(
                            onClick = onPickup,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📦 Забрал в магазине", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onProblem,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚠️ Возникла проблема")
                        }
                    }

                    task.status == "В доставке" -> {
                        Text("📸 Фото подтверждения:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onPhotoRequest,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasPhoto) Color(0xFF4CAF50) else SoftPink
                            )
                        ) {
                            Text(if (hasPhoto) "✅ Фото готово" else "📷 Сделать фото")
                        }

                        if (task.paymentMethod.contains("Наличными") || task.paymentMethod.contains("при получении")) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isCashCollected,
                                    onCheckedChange = onCashCollectedChange,
                                    colors = CheckboxDefaults.colors(checkedColor = SoftPink)
                                )
                                Text(
                                    if (task.paymentMethod.contains("Наличными")) "💰 Наличные получены" else "💳 Оплата получена",
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onDelivered,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasPhoto
                        ) {
                            Text("✅ Подтвердить доставку")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onProblem,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚠️ Возникла проблема")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemDialog(
    context: android.content.Context,
    taskId: Int,
    problemDescription: String,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "⚠️ Проблема с заказом #$taskId",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Опишите проблему подробно:", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: клиент не отвечает, адрес не найден...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = problemDescription.isNotBlank()
                    ) {
                        Text("📤 Отправить")
                    }
                }
            }
        }
    }
}

// ==================== ФУНКЦИИ ДЛЯ РАБОТЫ С ФОТО ====================

// Функция для конвертации Bitmap в Base64 с сжатием
fun bitmapToBase64(bitmap: Bitmap, maxSizeKB: Int = 200): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    var quality = 85
    var compressedBitmap = bitmap

    // Сжимаем пока размер не станет меньше maxSizeKB
    do {
        byteArrayOutputStream.reset()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        quality -= 10
    } while (byteArrayOutputStream.size() > maxSizeKB * 1024 && quality > 10)

    val byteArray = byteArrayOutputStream.toByteArray()
    Log.d("CameraUtils", "Photo size after compression: ${byteArray.size / 1024} KB")
    return Base64.encodeToString(byteArray, Base64.NO_WRAP) // NO_WRAP убирает переносы строк
}

// Функция для получения Bitmap из URI
fun getBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        Log.e("CameraUtils", "Error getting bitmap from URI", e)
        null
    }
}

// ==================== ОСНОВНОЙ КОМПОНЕНТ ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrdersTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userData = getCurrentUserData(context)

    var deliveryTasks by remember { mutableStateOf<List<CourierDeliveryTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<CourierDeliveryTask?>(null) }
    var showExecutionDialog by remember { mutableStateOf(false) }
    var showProblemDialog by remember { mutableStateOf(false) }
    var problemDescription by remember { mutableStateOf("") }
    var hasPhoto by remember { mutableStateOf(false) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isCashCollected by remember { mutableStateOf(false) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher для камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            try {
                val bitmap = getBitmapFromUri(context, tempPhotoUri!!)
                if (bitmap != null) {
                    // Сжимаем фото до 200KB и убираем переносы строк
                    photoBase64 = bitmapToBase64(bitmap, 200)
                    hasPhoto = true
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Фото сохранено (сжато)")
                    }
                    Log.d("DeliveryOrdersTab", "Photo Base64 length: ${photoBase64?.length}")
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Ошибка при обработке фото")
                    }
                }
            } catch (e: Exception) {
                Log.e("DeliveryOrdersTab", "Error processing photo", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Ошибка при сохранении фото: ${e.message}")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Фото не было сохранено")
            }
        }
        tempPhotoUri = null
    }

    // Функция для запуска камеры
    fun takePhoto() {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "delivery_photo_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FlowersMobile")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            val file = File(context.cacheDir, "delivery_photo_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        tempPhotoUri = uri
        uri?.let { cameraLauncher.launch(it) }
    }

    // Launcher для запроса разрешения камеры
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && selectedTask != null) {
            takePhoto()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Необходимо разрешение на использование камеры")
            }
        }
    }

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = true
            if (userData?.employeeId != null) {
                val orders = CourierDeliveryApi.getCourierOrders(userData.employeeId)
                deliveryTasks = orders?.filter {
                    it.status == "Готов к доставке" ||
                            it.status == "Готов к выдаче" ||
                            it.status == "В доставке"
                } ?: emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(userData?.employeeId) {
        loadOrders()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "📦 Заказы",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SoftPink)
                    }
                }
                deliveryTasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Нет активных заказов", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Назначенные заказы появятся здесь", fontSize = 14.sp, color = WarmGray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(deliveryTasks) { task ->
                            DeliveryTaskCard(
                                task = task,
                                onClick = {
                                    selectedTask = task
                                    hasPhoto = false
                                    photoBase64 = null
                                    isCashCollected = false
                                    showExecutionDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showExecutionDialog && selectedTask != null) {
            DeliveryExecutionDialog(
                context = context,
                task = selectedTask!!,
                hasPhoto = hasPhoto,
                isCashCollected = isCashCollected,
                onDismiss = {
                    showExecutionDialog = false
                    hasPhoto = false
                    photoBase64 = null
                },
                onPhotoRequest = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            takePhoto()
                        }
                        else -> {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                onCashCollectedChange = { isCashCollected = it },
                onPickup = {
                    coroutineScope.launch {
                        val success = CourierDeliveryApi.performCourierAction(
                            CourierDeliveryAction(
                                action = "pickup",
                                idOrder = selectedTask!!.id,
                                photoAtDoor = null,
                                problemDescription = "" // Добавляем пустую строку
                            )
                        )
                        if (success) {
                            snackbarHostState.showSnackbar("Заказ #${selectedTask!!.id} взят в доставку")
                            loadOrders()
                            showExecutionDialog = false
                        } else {
                            snackbarHostState.showSnackbar("Ошибка: не удалось взять заказ")
                        }
                    }
                },
                onDelivered = {
                    coroutineScope.launch {
                        val success = CourierDeliveryApi.performCourierAction(
                            CourierDeliveryAction(
                                action = "delivered",
                                idOrder = selectedTask!!.id,
                                photoAtDoor = photoBase64,
                                problemDescription = "" // Добавляем пустую строку
                            )
                        )
                        if (success) {
                            snackbarHostState.showSnackbar("Заказ #${selectedTask!!.id} доставлен")
                            loadOrders()
                            showExecutionDialog = false
                            hasPhoto = false
                            photoBase64 = null
                        } else {
                            snackbarHostState.showSnackbar("Ошибка: не удалось подтвердить доставку")
                        }
                    }
                },
                onProblem = {
                    showExecutionDialog = false
                    showProblemDialog = true
                }
            )
        }

        if (showProblemDialog && selectedTask != null) {
            ProblemDialog(
                context = context,
                taskId = selectedTask!!.id,
                problemDescription = problemDescription,
                onDescriptionChange = { problemDescription = it },
                onDismiss = {
                    showProblemDialog = false
                    problemDescription = ""
                },
                onSubmit = {
                    coroutineScope.launch {
                        val success = CourierDeliveryApi.performCourierAction(
                            CourierDeliveryAction(
                                action = "problem",
                                idOrder = selectedTask!!.id,
                                problemDescription = problemDescription
                            )
                        )
                        if (success) {
                            snackbarHostState.showSnackbar("Проблема отправлена")
                            loadOrders()
                            showProblemDialog = false
                            problemDescription = ""
                        } else {
                            snackbarHostState.showSnackbar("Ошибка отправки")
                        }
                    }
                }
            )
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