package com.kkk19lll.flowersmobile.views

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.kkk19lll.flowersmobile.MainActivity
import com.kkk19lll.flowersmobile.R
import com.kkk19lll.flowersmobile.ui.theme.PrimaryLight
import com.kkk19lll.flowersmobile.ui.theme.PrimaryDark
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.SoftPinkDark
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

// Модели данных
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val userId: Int,
    val email: String,
    val role: String,
    val clientId: Int?,
    val employeeId: Int?
)

@Serializable
data class ErrorResponse(
    val message: String,
    val error: String? = null
)

// Константы для ролей
object UserRole {
    const val CLIENT = "user"
    const val COURIER = "courier"
}

@Composable
fun LoginView(
    onLoginSuccess: (LoginResponse) -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToClient: (LoginResponse) -> Unit = {},
    onNavigateToCourier: (LoginResponse) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Функция для навигации на основе роли
    fun navigateBasedOnRole(response: LoginResponse) {
        when (response.role) {
            UserRole.CLIENT -> {
                Log.d("LoginView", "Navigating to Client screen for user: ${response.email}")
                onNavigateToClient(response)
            }
            UserRole.COURIER -> {
                Log.d("LoginView", "Navigating to Courier screen for courier: ${response.email}")
                onNavigateToCourier(response)
            }
            else -> {
                Log.e("LoginView", "Unknown role: ${response.role}")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Неизвестная роль пользователя")
                }
            }
        }
    }

    // Проверяем, не залогинен ли уже пользователь
    LaunchedEffect(Unit) {
        if (isUserLoggedIn(context)) {
            val userData = getCurrentUserData(context)
            if (userData != null) {
                navigateBasedOnRole(userData)
            }
        }
    }

    // Валидация
    fun validateForm(): Boolean {
        var isValid = true

        if (email.isBlank()) {
            emailError = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Введите корректный email"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Пароль должен быть не менее 6 символов"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    // Функция логина
    fun performLogin() {
        if (!validateForm()) return

        isLoading = true

        coroutineScope.launch {
            try {
                // Выполняем сетевой запрос в фоновом потоке
                val response = withContext(Dispatchers.IO) {
                    loginUser(email, password)
                }

                isLoading = false

                if (response != null) {
                    // Сохраняем данные в SharedPreferences
                    saveUserData(context, response)

                    // Выполняем навигацию на основе роли
                    navigateBasedOnRole(response)
                    onLoginSuccess(response)
                } else {
                    snackbarHostState.showSnackbar("Ошибка подключения к серверу")
                }
            } catch (e: Exception) {
                isLoading = false
                snackbarHostState.showSnackbar("Ошибка: ${e.message}")
            }
        }
    }

    // UI компонент
    LoginContent(
        email = email,
        onEmailChange = { email = it; emailError = null },
        password = password,
        onPasswordChange = { password = it; passwordError = null },
        isLoading = isLoading,
        emailError = emailError,
        passwordError = passwordError,
        onLoginClick = { performLogin() },
        onForgotPasswordClick = onForgotPasswordClick,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    emailError: String?,
    passwordError: String?,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Snackbar для ошибок
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Иконка цветка
        Icon(
            painter = painterResource(id = R.drawable.ico_app),
            contentDescription = "Flower Icon",
            tint = SoftPink,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Flower Paradise",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "свежие цветы с доставкой",
            fontSize = 14.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Поле Email
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftPink,
                focusedLabelColor = SoftPink,
                cursorColor = SoftPink,
                unfocusedBorderColor = WarmGray,
                unfocusedLabelColor = WarmGray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле Пароль
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftPink,
                focusedLabelColor = SoftPink,
                cursorColor = SoftPink,
                unfocusedBorderColor = WarmGray,
                unfocusedLabelColor = WarmGray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End),
            enabled = !isLoading
        ) {
            Text(
                text = "Забыли пароль?",
                fontSize = 12.sp,
                color = SoftPinkDark
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка входа с индикатором загрузки
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftPink,
                contentColor = Color.White,
                disabledContainerColor = WarmGray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Войти",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Flower Paradise © 2024",
            fontSize = 11.sp,
            color = WarmGray,
            textAlign = TextAlign.Center
        )
    }
}

// Функция логина
suspend fun loginUser(email: String, password: String): LoginResponse? {
    // Эта функция уже выполняется в фоновом потоке благодаря withContext(Dispatchers.IO)
    return try {
        val url = URL("http://10.0.2.2:5034/api/Auth/login") // для эмулятора

        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
        }

        // Создаем тело запроса
        val loginRequest = LoginRequest(email, password)
        val jsonBody = Json.encodeToString(loginRequest)

        // Отправляем запрос
        connection.outputStream.use { os ->
            val writer = OutputStreamWriter(os, "UTF-8")
            writer.write(jsonBody)
            writer.flush()
        }

        // Читаем ответ
        val responseCode = connection.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            // Успешный вход
            connection.inputStream.bufferedReader().use {
                val responseText = it.readText()
                Json.decodeFromString<LoginResponse>(responseText)
            }
        } else {
            // Ошибка
            connection.errorStream?.bufferedReader()?.use {
                val errorText = it.readText()
                try {
                    val error = Json.decodeFromString<ErrorResponse>(errorText)
                    Log.e("LoginError", "Server error: ${error.message}")
                } catch (e: Exception) {
                    Log.e("LoginError", "Unknown error: $errorText")
                }
            }
            null
        }

        connection.disconnect()
        response
    } catch (e: Exception) {
        Log.e("LoginError", "Network error", e)
        null
    }
}

// Функции для работы с SharedPreferences
fun saveUserData(context: Context, response: LoginResponse) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putInt("user_id", response.userId)
        putString("user_email", response.email)
        putString("user_role", response.role)
        if (response.clientId != null) putInt("client_id", response.clientId)
        if (response.employeeId != null) putInt("employee_id", response.employeeId)
        putBoolean("is_logged_in", true)
        apply()
    }
}

fun isUserLoggedIn(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_logged_in", false)
}

fun getCurrentUserRole(context: Context): String? {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return if (prefs.getBoolean("is_logged_in", false)) {
        prefs.getString("user_role", null)
    } else {
        null
    }
}

fun getCurrentUserData(context: Context): LoginResponse? {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return if (prefs.getBoolean("is_logged_in", false)) {
        LoginResponse(
            userId = prefs.getInt("user_id", -1),
            email = prefs.getString("user_email", "") ?: "",
            role = prefs.getString("user_role", "") ?: "",
            clientId = if (prefs.contains("client_id")) prefs.getInt("client_id", -1) else null,
            employeeId = if (prefs.contains("employee_id")) prefs.getInt("employee_id", -1) else null
        )
    } else {
        null
    }
}

fun logout(context: Context) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginViewPreview() {
    MaterialTheme {
        LoginView(
            onLoginSuccess = { response ->
                println("Login success: ${response.email}, Role: ${response.role}")
            },
            onNavigateToClient = { response ->
                println("Navigating to Client Screen for: ${response.email}")
            },
            onNavigateToCourier = { response ->
                println("Navigating to Courier Screen for: ${response.email}")
            },
            onForgotPasswordClick = {
                println("Forgot password clicked")
            }
        )
    }
}