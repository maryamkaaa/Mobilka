package com.kkk19lll.flowersmobile.views.client

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import com.kkk19lll.flowersmobile.views.saveUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

// ==================== МОДЕЛИ ДАННЫХ ====================

@Serializable
data class ProfileResponse(
    val id: Int,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String,
    val userId: Int
)

@Serializable
data class UpdateProfileRequest(
    val phone: String,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class ApiResponse(
    val message: String
)

// ==================== API МЕТОДЫ ====================

object ProfileApiService {
    private const val BASE_URL = "http://10.0.2.2:5034/api/Profile"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getProfile(clientId: Int): ProfileResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$clientId")
            Log.d("ProfileAPI", "Fetching profile from: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ProfileAPI", "Profile response: $response")
                json.decodeFromString<ProfileResponse>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ProfileAPI", "Error getting profile: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileAPI", "Error getting profile", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun updateProfile(clientId: Int, request: UpdateProfileRequest): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$clientId")
            Log.d("ProfileAPI", "Updating profile at: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

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
                Log.e("ProfileAPI", "Error updating profile: $responseCode, error: $errorStream")
            }

            success
        } catch (e: Exception) {
            Log.e("ProfileAPI", "Error updating profile", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun changePassword(clientId: Int, request: ChangePasswordRequest): ApiResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$clientId/change-password")
            Log.d("ProfileAPI", "Changing password at: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonBody = json.encodeToString(request)

            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            return@withContext when (responseCode) {
                HttpURLConnection.HTTP_OK -> json.decodeFromString<ApiResponse>(response)
                else -> {
                    Log.e("ProfileAPI", "Error changing password: $responseCode")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileAPI", "Error changing password", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}

// ==================== СОСТОЯНИЕ ====================

class ProfileState {
    var profile by mutableStateOf<ProfileResponse?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var isEditMode by mutableStateOf(false)
    var isChangingPassword by mutableStateOf(false)
    var snackbarMessage by mutableStateOf<String?>(null)

    // Поля для редактирования
    var editPhone by mutableStateOf("")
    var editFirstName by mutableStateOf("")
    var editLastName by mutableStateOf("")
    var editPatronymic by mutableStateOf("")
    var editEmail by mutableStateOf("")

    // Поля для смены пароля
    var oldPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
}

// ==================== ОСНОВНОЙ ЭКРАН ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileState = remember { ProfileState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val userData = getCurrentUserData(context)

    // Загрузка профиля при старте
    LaunchedEffect(Unit) {
        if (userData?.clientId != null) {
            profileState.isLoading = true
            profileState.error = null
            try {
                val profile = ProfileApiService.getProfile(userData.clientId)
                if (profile != null) {
                    profileState.profile = profile
                    // Заполняем поля редактирования
                    profileState.editPhone = profile.phone
                    profileState.editFirstName = profile.firstName
                    profileState.editLastName = profile.lastName
                    profileState.editPatronymic = profile.patronymic ?: ""
                    profileState.editEmail = profile.email
                } else {
                    profileState.error = "Не удалось загрузить профиль"
                }
            } catch (e: Exception) {
                profileState.error = "Ошибка: ${e.message}"
                Log.e("ProfileTab", "Error loading profile", e)
            } finally {
                profileState.isLoading = false
            }
        } else {
            profileState.error = "Пользователь не авторизован"
        }
    }

    // Обработка сообщений snackbar
    LaunchedEffect(profileState.snackbarMessage) {
        profileState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            profileState.snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "👤 Мой профиль",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    if (!profileState.isEditMode && !profileState.isChangingPassword) {
                        IconButton(onClick = { profileState.isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = SoftPink)
                        }
                    }
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
            when {
                profileState.isLoading && profileState.profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SoftPink)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Загрузка профиля...", color = WarmGray, fontSize = 14.sp)
                        }
                    }
                }

                profileState.error != null && profileState.profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "😕 ${profileState.error!!}",
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
                                            profileState.isLoading = true
                                            val profile = ProfileApiService.getProfile(userData.clientId)
                                            if (profile != null) {
                                                profileState.profile = profile
                                                profileState.error = null
                                            }
                                            profileState.isLoading = false
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

                profileState.isChangingPassword -> {
                    ChangePasswordContent(
                        profileState = profileState,
                        onBack = { profileState.isChangingPassword = false },
                        onSave = {
                            coroutineScope.launch {
                                if (userData?.clientId != null) {
                                    if (profileState.newPassword != profileState.confirmPassword) {
                                        profileState.snackbarMessage = "Пароли не совпадают"
                                        return@launch
                                    }
                                    if (profileState.newPassword.length < 6) {
                                        profileState.snackbarMessage = "Пароль должен быть не менее 6 символов"
                                        return@launch
                                    }

                                    val response = ProfileApiService.changePassword(
                                        userData.clientId,
                                        ChangePasswordRequest(
                                            oldPassword = profileState.oldPassword,
                                            newPassword = profileState.newPassword
                                        )
                                    )

                                    if (response != null) {
                                        profileState.snackbarMessage = response.message
                                        profileState.isChangingPassword = false
                                        profileState.oldPassword = ""
                                        profileState.newPassword = ""
                                        profileState.confirmPassword = ""
                                    } else {
                                        profileState.snackbarMessage = "Не удалось изменить пароль"
                                    }
                                }
                            }
                        }
                    )
                }

                profileState.isEditMode -> {
                    EditProfileContent(
                        profileState = profileState,
                        onCancel = {
                            // Восстанавливаем исходные данные
                            profileState.profile?.let { profile ->
                                profileState.editPhone = profile.phone
                                profileState.editFirstName = profile.firstName
                                profileState.editLastName = profile.lastName
                                profileState.editPatronymic = profile.patronymic ?: ""
                                profileState.editEmail = profile.email
                            }
                            profileState.isEditMode = false
                        },
                        onSave = {
                            coroutineScope.launch {
                                if (userData?.clientId != null) {
                                    val success = ProfileApiService.updateProfile(
                                        userData.clientId,
                                        UpdateProfileRequest(
                                            phone = profileState.editPhone,
                                            firstName = profileState.editFirstName,
                                            lastName = profileState.editLastName,
                                            patronymic = profileState.editPatronymic.takeIf { it.isNotBlank() },
                                            email = profileState.editEmail
                                        )
                                    )

                                    if (success) {
                                        // Обновляем локальные данные
                                        val updatedProfile = ProfileApiService.getProfile(userData.clientId)
                                        if (updatedProfile != null) {
                                            profileState.profile = updatedProfile
                                            profileState.editPhone = updatedProfile.phone
                                            profileState.editFirstName = updatedProfile.firstName
                                            profileState.editLastName = updatedProfile.lastName
                                            profileState.editPatronymic = updatedProfile.patronymic ?: ""
                                            profileState.editEmail = updatedProfile.email
                                        }
                                        profileState.isEditMode = false
                                        profileState.snackbarMessage = "Профиль успешно обновлен"
                                    } else {
                                        profileState.snackbarMessage = "Ошибка при обновлении профиля"
                                    }
                                }
                            }
                        }
                    )
                }

                else -> {
                    ProfileContent(
                        profile = profileState.profile,
                        onLogout = onLogout,
                        onChangePassword = { profileState.isChangingPassword = true }
                    )
                }
            }
        }
    }
}

// ==================== КОМПОНЕНТЫ ====================

@Composable
fun ProfileContent(
    profile: ProfileResponse?,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Аватар и приветствие
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌸", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Добро пожаловать!",
                        fontSize = 14.sp,
                        color = WarmGray
                    )
                }
            }
        }

        item {
            // Личная информация
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Личная информация",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoItem("Имя", profile?.firstName ?: "")
                    Divider(color = WarmGray.copy(alpha = 0.2f))
                    ProfileInfoItem("Фамилия", profile?.lastName ?: "")
                    Divider(color = WarmGray.copy(alpha = 0.2f))
                    if (!profile?.patronymic.isNullOrBlank()) {
                        ProfileInfoItem("Отчество", profile?.patronymic ?: "")
                        Divider(color = WarmGray.copy(alpha = 0.2f))
                    }
                    ProfileInfoItem("Телефон", profile?.phone ?: "")
                    Divider(color = WarmGray.copy(alpha = 0.2f))
                    ProfileInfoItem("Email", profile?.email ?: "")
                }
            }
        }

        item {
            // Кнопка смены пароля
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangePassword() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SoftPink)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Сменить пароль",
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = WarmGray)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.7f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Выйти из аккаунта", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun EditProfileContent(
    profileState: ProfileState,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Редактирование профиля",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = profileState.editFirstName,
                        onValueChange = { profileState.editFirstName = it },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.editLastName,
                        onValueChange = { profileState.editLastName = it },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.editPatronymic,
                        onValueChange = { profileState.editPatronymic = it },
                        label = { Text("Отчество (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.editPhone,
                        onValueChange = { profileState.editPhone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.editEmail,
                        onValueChange = { profileState.editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGray.copy(alpha = 0.3f),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отмена")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
fun ChangePasswordContent(
    profileState: ProfileState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Смена пароля",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = profileState.oldPassword,
                        onValueChange = { profileState.oldPassword = it },
                        label = { Text("Текущий пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.newPassword,
                        onValueChange = { profileState.newPassword = it },
                        label = { Text("Новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileState.confirmPassword,
                        onValueChange = { profileState.confirmPassword = it },
                        label = { Text("Подтвердите новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPink,
                            focusedLabelColor = SoftPink
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGray.copy(alpha = 0.3f),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Назад")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Изменить")
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = WarmGray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}