package com.kkk19lll.flowersmobile.views.client

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkk19lll.flowersmobile.ui.theme.SoftPink
import com.kkk19lll.flowersmobile.ui.theme.SoftPinkDark
import com.kkk19lll.flowersmobile.ui.theme.TextPrimary
import com.kkk19lll.flowersmobile.ui.theme.WarmGray
import com.kkk19lll.flowersmobile.views.getCurrentUserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Модели данных для API
@Serializable
data class Category(
    val id: Int,
    val name: String,
    val description: String?,
    val parentId: Int?
)

@Serializable
data class Product(
    val id: Int,
    val photo: String?,
    val name: String,
    val description: String?,
    val price: Double,
    val category: String,
    val status: String
)

@Serializable
data class ProductDetail(
    val id: Int,
    val photo: String?,
    val name: String,
    val description: String?,
    val price: Double,
    val category: String,
    val status: String,
    val categoryId: Int,
    val categoryName: String
)

@Serializable
data class CompositionItem(
    val name: String,
    val quantity: Int
)

@Serializable
data class Review(
    val rating: Int,
    val comment: String?,
    val clientName: String,
    val isPublished: Boolean
)

@Serializable
data class ProductDetailsResponse(
    val product: ProductDetail,
    val composition: List<CompositionItem>,
    val reviews: List<Review>
)

@Serializable
data class RawMaterial(
    val idRawName: Int,
    val rawName: String,
    val rawPrice: Int,
    val quantity: Int,
    val subcategoryId: Int? = null,
    val categoryName: String? = null
)

@Serializable
data class BouquetItem(
    val idRawName: Int,
    var quantity: Int
)

@Serializable
data class BouquetConstructorRequest(
    val idPackaging: Int,
    val flowers: List<BouquetItem>,
    val extras: List<BouquetItem>
)

@Serializable
data class BouquetItemDetail(
    val name: String,
    val quantity: Int,
    val price: Int,
    val totalPrice: Int
)

@Serializable
data class BouquetPreviewResponse(
    val totalPrice: Double,
    val items: List<BouquetItemDetail>,
    val previewImage: String
)

@Serializable
data class CartItem(
    val productId: Int,
    val name: String,
    val price: Double,
    var quantity: Int,
    val photo: String?
)

@Serializable
data class OrderItemRequest(
    val idProduct: Int,
    val quantity: Int
)

@Serializable
data class OrderCreateRequest(
    val idDeliveryMethod: Int,
    val idPaymentMethod: Int,
    val deliveryAddress: String,
    val plannedDeliveryDate: String?,
    val clientNotes: String,
    val items: List<OrderItemRequest>,
    val totalAmount: Double
)

@Serializable
data class OrderCreateResponse(
    val message: String,
    val orderId: Int,
    val totalAmount: Double
)

// Состояния для экранов
class CatalogState {
    var categories by mutableStateOf<List<Category>>(emptyList())
    var products by mutableStateOf<List<Product>>(emptyList())
    var selectedCategory by mutableStateOf<Int?>(null)
    var searchQuery by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var minPrice by mutableStateOf<Double?>(null)
    var maxPrice by mutableStateOf<Double?>(null)
    var showFilters by mutableStateOf(false)
}

class CartState {
    var items by mutableStateOf<List<CartItem>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun addItem(product: Product, quantity: Int = 1) {
        val existingItem = items.find { it.productId == product.id }
        if (existingItem != null) {
            val updatedItems = items.toMutableList()
            val index = updatedItems.indexOf(existingItem)
            updatedItems[index] = existingItem.copy(quantity = existingItem.quantity + quantity)
            items = updatedItems
        } else {
            items = items + CartItem(
                productId = product.id,
                name = product.name,
                price = product.price,
                quantity = quantity,
                photo = product.photo
            )
        }
    }

    fun removeItem(productId: Int) {
        items = items.filter { it.productId != productId }
    }

    fun updateQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) {
            removeItem(productId)
            return
        }
        val updatedItems = items.toMutableList()
        val index = updatedItems.indexOfFirst { it.productId == productId }
        if (index != -1) {
            updatedItems[index] = updatedItems[index].copy(quantity = quantity)
            items = updatedItems
        }
    }

    fun getTotalPrice(): Double {
        return items.sumOf { it.price * it.quantity }
    }

    fun getTotalItems(): Int {
        return items.sumOf { it.quantity }
    }

    fun clear() {
        items = emptyList()
    }
}

class ProductDetailState {
    var productDetails by mutableStateOf<ProductDetailsResponse?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var quantity by mutableStateOf(1)
}

class BouquetConstructorState {
    var availablePackaging by mutableStateOf<List<RawMaterial>>(emptyList())
    var availableFlowers by mutableStateOf<List<RawMaterial>>(emptyList())
    var availableExtras by mutableStateOf<List<RawMaterial>>(emptyList())
    var selectedPackaging by mutableStateOf<Int?>(null)
    var selectedFlowers = mutableStateListOf<BouquetItem>()
    var selectedExtras = mutableStateListOf<BouquetItem>()
    var previewData by mutableStateOf<BouquetPreviewResponse?>(null)
    var isLoading by mutableStateOf(false)
    var isCalculating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var currentTab by mutableStateOf(0)
    var isCreatingOrder by mutableStateOf(false)
}

// API сервис
object ApiService {
    private const val BASE_URL = "http://10.0.2.2:5034/api"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getCategories(): List<Category>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/Catalog/categories")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiService", "Categories response: $response")
                json.decodeFromString<List<Category>>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiService", "Error getting categories: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting categories", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getProducts(
        categoryId: Int? = null,
        search: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        statusId: Int? = null
    ): List<Product>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlBuilder = StringBuilder("$BASE_URL/Catalog/products")
            val params = mutableListOf<String>()

            if (!search.isNullOrBlank()) {
                params.add("search=${URLEncoder.encode(search, "UTF-8")}")
            }

            if (categoryId != null) {
                params.add("categoryId=$categoryId")
            }

            if (minPrice != null) {
                params.add("minPrice=$minPrice")
            }

            if (maxPrice != null) {
                params.add("maxPrice=$maxPrice")
            }

            if (statusId != null) {
                params.add("statusId=$statusId")
            }

            val urlString = if (params.isNotEmpty()) {
                urlBuilder.append("?").append(params.joinToString("&")).toString()
            } else {
                urlBuilder.toString()
            }

            Log.d("ApiService", "Products GET URL: $urlString")

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiService", "Products response: $response")
                json.decodeFromString<List<Product>>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiService", "Error getting products: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting products", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getProductDetails(productId: Int): ProductDetailsResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/Catalog/products/$productId")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiService", "Product details response: $response")
                json.decodeFromString<ProductDetailsResponse>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiService", "Error getting product details: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting product details", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getRawMaterialsByType(type: String): List<RawMaterial>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/BouquetConstructor/materials?type=$type")
            Log.d("ApiService", "Requesting URL: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("ApiService", "Response code for $type: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiService", "Raw materials response for $type: $response")

                if (response == "[]" || response.isBlank()) {
                    Log.w("ApiService", "Empty response for $type")
                    return@withContext emptyList()
                }

                try {
                    val materials = json.decodeFromString<List<RawMaterial>>(response)
                    Log.d("ApiService", "Decoded ${materials.size} materials for $type")
                    materials
                } catch (e: Exception) {
                    Log.e("ApiService", "Error parsing JSON for $type", e)
                    emptyList()
                }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiService", "Error getting raw materials for $type: $responseCode, error: $errorStream")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting raw materials for $type", e)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun calculateBouquet(request: BouquetConstructorRequest): BouquetPreviewResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/BouquetConstructor/calculate")
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = json.encodeToString(request)
            Log.d("ApiService", "Calculate request: $jsonBody")

            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiService", "Calculate response: $response")
                json.decodeFromString<BouquetPreviewResponse>(response)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiService", "Error calculating bouquet: $responseCode, error: $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error calculating bouquet", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun createOrder(
        request: OrderCreateRequest,
        clientId: Int
    ): OrderCreateResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/BouquetConstructor/create-order?clientId=$clientId")
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = json.encodeToString(request)
            Log.d("ApiService", "Create order request: $jsonBody")
            Log.d("ApiService", "Create order URL: $url")

            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("ApiService", "Create order response: $response")
                json.decodeFromString<OrderCreateResponse>(response)
            } else {
                Log.e("ApiService", "Error creating order: $responseCode, error: $response")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error creating order", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}

// Основной экран каталога с табами
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val catalogState = remember { CatalogState() }
    val cartState = remember { CartState() }
    val constructorState = remember { BouquetConstructorState() }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var showOrderForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadCategories(catalogState, snackbarHostState)
    }

    LaunchedEffect(catalogState.selectedCategory, catalogState.searchQuery, catalogState.minPrice, catalogState.maxPrice) {
        loadProducts(catalogState, snackbarHostState)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && constructorState.availableFlowers.isEmpty() && !constructorState.isLoading) {
            loadConstructorData(constructorState, snackbarHostState)
        }
    }

    // Отслеживаем изменения выбранных элементов и пересчитываем стоимость
    LaunchedEffect(
        constructorState.selectedPackaging,
        constructorState.selectedFlowers.toList(),
        constructorState.selectedExtras.toList()
    ) {
        if (constructorState.selectedPackaging != null && constructorState.selectedFlowers.isNotEmpty()) {
            calculateAndUpdatePreview(constructorState, snackbarHostState, coroutineScope)
        } else {
            constructorState.previewData = null
        }
    }

    if (selectedProductId != null) {
        ProductDetailScreen(
            productId = selectedProductId!!,
            cartState = cartState,
            onBack = { selectedProductId = null },
            onAddToCart = { product, quantity ->
                cartState.addItem(product, quantity)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✅ ${product.name} добавлен в корзину")
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                when (selectedTab) {
                    0 -> {
                        CatalogTopBar(
                            searchQuery = catalogState.searchQuery,
                            onSearchChange = { catalogState.searchQuery = it },
                            onFilterClick = { catalogState.showFilters = true },
                            cartItemCount = cartState.getTotalItems(),
                            onCartClick = { showCart = true }
                        )
                    }
                    1 -> {
                        TopAppBar(
                            title = {
                                Text(
                                    "🌸 Конструктор букетов",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = TextPrimary
                            ),
                            actions = {
                                IconButton(
                                    onClick = {
                                        if (constructorState.previewData != null) {
                                            showPreview = true
                                        }
                                    },
                                    enabled = constructorState.previewData != null
                                ) {
                                    Text(
                                        text = "👁️",
                                        fontSize = 24.sp,
                                        color = if (constructorState.previewData != null) SoftPink else WarmGray
                                    )
                                }
                                IconButton(
                                    onClick = { showCart = true }
                                ) {
                                    BadgedBox(
                                        badgeCount = cartState.getTotalItems()
                                    ) {
                                        Text("🛒", fontSize = 24.sp)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { selectedTab = 0 }) {
                                    Text("←", fontSize = 28.sp, color = SoftPink)
                                }
                            }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (selectedTab == 1 && constructorState.previewData != null) {
                    FloatingActionButton(
                        onClick = {
                            showOrderForm = true
                        },
                        containerColor = SoftPink,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📝", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Заказать",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(SoftPink)
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛍️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Каталог", fontSize = 14.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌸", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Конструктор", fontSize = 14.sp)
                            }
                        }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        if (catalogState.isLoading && catalogState.products.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SoftPink)
                            }
                        } else if (catalogState.error != null && catalogState.products.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "😕 ${catalogState.error!!}",
                                        color = Color.Red,
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                loadCategories(catalogState, snackbarHostState)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("🔄 Повторить")
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CategoriesRow(
                                    categories = catalogState.categories,
                                    selectedCategory = catalogState.selectedCategory,
                                    onCategoryClick = { categoryId ->
                                        catalogState.selectedCategory = categoryId
                                    },
                                    isLoading = catalogState.isLoading
                                )

                                ProductsGrid(
                                    products = catalogState.products,
                                    onProductClick = { productId ->
                                        selectedProductId = productId
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        if (constructorState.isLoading && constructorState.availableFlowers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SoftPink)
                            }
                        } else if (constructorState.error != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "😕 ${constructorState.error!!}",
                                        color = Color.Red,
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                loadConstructorData(constructorState, snackbarHostState)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("🔄 Повторить")
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                TabRow(
                                    selectedTabIndex = constructorState.currentTab,
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                    indicator = { tabPositions ->
                                        if (constructorState.currentTab < tabPositions.size) {
                                            Box(
                                                modifier = Modifier
                                                    .tabIndicatorOffset(tabPositions[constructorState.currentTab])
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(SoftPink)
                                            )
                                        }
                                    }
                                ) {
                                    listOf("📦 Упаковка", "🌸 Цветы", "✨ Декор").forEachIndexed { index, title ->
                                        Tab(
                                            selected = constructorState.currentTab == index,
                                            onClick = { constructorState.currentTab = index },
                                            text = {
                                                Text(
                                                    title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (constructorState.currentTab == index) FontWeight.Medium else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }
                                }

                                when (constructorState.currentTab) {
                                    0 -> PackagingList(
                                        packaging = constructorState.availablePackaging,
                                        selectedId = constructorState.selectedPackaging,
                                        onSelect = { packagingId ->
                                            constructorState.selectedPackaging = packagingId
                                        }
                                    )
                                    1 -> FlowersList(
                                        flowers = constructorState.availableFlowers,
                                        selectedFlowers = constructorState.selectedFlowers,
                                        onAdd = { flowerId ->
                                            val existing = constructorState.selectedFlowers.find { it.idRawName == flowerId }
                                            val flower = constructorState.availableFlowers.find { it.idRawName == flowerId }
                                            if (existing != null) {
                                                if (existing.quantity < (flower?.quantity ?: 0)) {
                                                    existing.quantity++
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Недостаточно товара на складе")
                                                    }
                                                }
                                            } else {
                                                if (flower != null && flower.quantity > 0) {
                                                    constructorState.selectedFlowers.add(BouquetItem(flowerId, 1))
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Товар временно отсутствует")
                                                    }
                                                }
                                            }
                                        },
                                        onRemove = { flowerId ->
                                            val existing = constructorState.selectedFlowers.find { it.idRawName == flowerId }
                                            if (existing != null) {
                                                if (existing.quantity > 1) {
                                                    existing.quantity--
                                                } else {
                                                    constructorState.selectedFlowers.remove(existing)
                                                }
                                            }
                                        },
                                        onQuantityChange = { flowerId, quantity ->
                                            val existing = constructorState.selectedFlowers.find { it.idRawName == flowerId }
                                            val flower = constructorState.availableFlowers.find { it.idRawName == flowerId }
                                            if (existing != null && flower != null) {
                                                val validQuantity = quantity.coerceIn(1, flower.quantity)
                                                existing.quantity = validQuantity
                                                if (quantity != validQuantity) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Доступно только ${flower.quantity} шт")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    2 -> ExtrasList(
                                        extras = constructorState.availableExtras,
                                        selectedExtras = constructorState.selectedExtras,
                                        onAdd = { extraId ->
                                            val existing = constructorState.selectedExtras.find { it.idRawName == extraId }
                                            val extra = constructorState.availableExtras.find { it.idRawName == extraId }
                                            if (existing != null) {
                                                if (existing.quantity < (extra?.quantity ?: 0)) {
                                                    existing.quantity++
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Недостаточно товара на складе")
                                                    }
                                                }
                                            } else {
                                                if (extra != null && extra.quantity > 0) {
                                                    constructorState.selectedExtras.add(BouquetItem(extraId, 1))
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Товар временно отсутствует")
                                                    }
                                                }
                                            }
                                        },
                                        onRemove = { extraId ->
                                            val existing = constructorState.selectedExtras.find { it.idRawName == extraId }
                                            if (existing != null) {
                                                if (existing.quantity > 1) {
                                                    existing.quantity--
                                                } else {
                                                    constructorState.selectedExtras.remove(existing)
                                                }
                                            }
                                        },
                                        onQuantityChange = { extraId, quantity ->
                                            val existing = constructorState.selectedExtras.find { it.idRawName == extraId }
                                            val extra = constructorState.availableExtras.find { it.idRawName == extraId }
                                            if (existing != null && extra != null) {
                                                val validQuantity = quantity.coerceIn(1, extra.quantity)
                                                existing.quantity = validQuantity
                                                if (quantity != validQuantity) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Доступно только ${extra.quantity} шт")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }

                                if (constructorState.isCalculating) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = SoftPink
                                        )
                                    }
                                } else if (constructorState.previewData != null) {
                                    CurrentPriceCard(
                                        totalPrice = constructorState.previewData!!.totalPrice,
                                        itemsCount = constructorState.selectedFlowers.size + constructorState.selectedExtras.size + (if (constructorState.selectedPackaging != null) 1 else 0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (catalogState.showFilters) {
                FiltersBottomSheet(
                    minPrice = catalogState.minPrice,
                    maxPrice = catalogState.maxPrice,
                    onMinPriceChange = { catalogState.minPrice = it },
                    onMaxPriceChange = { catalogState.maxPrice = it },
                    onApply = { catalogState.showFilters = false },
                    onDismiss = { catalogState.showFilters = false }
                )
            }

            if (showCart) {
                CartBottomSheet(
                    cartState = cartState,
                    onDismiss = { showCart = false },
                    onCheckout = {
                        showCart = false
                        showOrderForm = true
                    },
                    onUpdateQuantity = { productId, quantity ->
                        cartState.updateQuantity(productId, quantity)
                    },
                    onRemoveItem = { productId ->
                        cartState.removeItem(productId)
                    }
                )
            }

            if (showPreview && constructorState.previewData != null) {
                BouquetPreviewBottomSheet(
                    preview = constructorState.previewData!!,
                    onDismiss = { showPreview = false },
                    onOrderClick = {
                        showPreview = false
                        showOrderForm = true
                    }
                )
            }

            if (showOrderForm) {
                val totalPrice = if (selectedTab == 0) {
                    cartState.getTotalPrice()
                } else {
                    constructorState.previewData?.totalPrice ?: 0.0
                }

                OrderFormBottomSheet(
                    totalPrice = totalPrice,
                    onDismiss = { showOrderForm = false },
                    onSubmit = { deliveryMethod, paymentMethod, address, notes, deliveryDate ->
                        coroutineScope.launch {
                            if (selectedTab == 0) {
                                createOrderFromCart(
                                    cartState = cartState,
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    deliveryMethod = deliveryMethod,
                                    paymentMethod = paymentMethod,
                                    address = address,
                                    notes = notes,
                                    deliveryDate = deliveryDate,
                                    totalPrice = totalPrice,
                                    onSuccess = { orderId ->
                                        showOrderForm = false
                                        cartState.clear()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("✅ Заказ #$orderId оформлен на сумму ${String.format("%.0f", totalPrice)} ₽!")
                                        }
                                    }
                                )
                            } else {
                                createOrderFromBouquet(
                                    constructorState = constructorState,
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    deliveryMethod = deliveryMethod,
                                    paymentMethod = paymentMethod,
                                    address = address,
                                    notes = notes,
                                    deliveryDate = deliveryDate,
                                    totalPrice = totalPrice,
                                    onSuccess = { orderId ->
                                        showOrderForm = false
                                        constructorState.selectedPackaging = null
                                        constructorState.selectedFlowers.clear()
                                        constructorState.selectedExtras.clear()
                                        constructorState.previewData = null
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("✅ Заказ #$orderId оформлен на сумму ${String.format("%.0f", totalPrice)} ₽!")
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

// Функция для расчета и обновления preview
suspend fun calculateAndUpdatePreview(
    state: BouquetConstructorState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    if (state.isCalculating) return

    if (state.selectedPackaging == null || state.selectedFlowers.isEmpty()) {
        state.previewData = null
        return
    }

    // Проверяем, что все выбранные цветы и декор есть в наличии
    val invalidFlowers = state.selectedFlowers.filter { flower ->
        val available = state.availableFlowers.find { it.idRawName == flower.idRawName }
        available == null || flower.quantity > available.quantity
    }

    val invalidExtras = state.selectedExtras.filter { extra ->
        val available = state.availableExtras.find { it.idRawName == extra.idRawName }
        available == null || extra.quantity > available.quantity
    }

    if (invalidFlowers.isNotEmpty() || invalidExtras.isNotEmpty()) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("⚠️ Некоторые товары недоступны в выбранном количестве")
        }
        state.previewData = null
        return
    }

    val request = BouquetConstructorRequest(
        idPackaging = state.selectedPackaging!!,
        flowers = state.selectedFlowers.toList(),
        extras = state.selectedExtras.toList()
    )

    state.isCalculating = true
    try {
        val preview = ApiService.calculateBouquet(request)
        state.previewData = preview
        if (preview != null) {
            Log.d("BouquetConstructor", "Preview calculated: totalPrice=${preview.totalPrice}")
        }
    } catch (e: Exception) {
        Log.e("BouquetConstructor", "Error calculating bouquet", e)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("❌ Ошибка при расчете стоимости")
        }
        state.previewData = null
    } finally {
        state.isCalculating = false
    }
}

@Composable
fun BadgedBox(
    badgeCount: Int,
    content: @Composable () -> Unit
) {
    Box {
        content()
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .background(Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск цветов...", color = WarmGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = SoftPink,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(28.dp),
                leadingIcon = {
                    Text("🔍", fontSize = 20.sp)
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = onFilterClick) {
                            Text("⚙️", fontSize = 20.sp)
                        }
                        IconButton(onClick = onCartClick) {
                            BadgedBox(badgeCount = cartItemCount) {
                                Text("🛒", fontSize = 24.sp)
                            }
                        }
                    }
                },
                singleLine = true
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            scrolledContainerColor = Color.White
        )
    )
}

@Composable
fun CategoriesRow(
    categories: List<Category>,
    selectedCategory: Int?,
    onCategoryClick: (Int?) -> Unit,
    isLoading: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            CategoryChip(
                name = "Все",
                isSelected = selectedCategory == null,
                onClick = { onCategoryClick(null) }
            )
        }

        if (isLoading && categories.isEmpty()) {
            items(3) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(WarmGray.copy(alpha = 0.3f))
                )
            }
        } else {
            items(categories) { category ->
                CategoryChip(
                    name = category.name,
                    isSelected = selectedCategory == category.id,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) SoftPink else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) SoftPink else WarmGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun ProductsGrid(
    products: List<Product>,
    onProductClick: (Int) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Товары не найдены",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Попробуйте изменить параметры поиска",
                    color = WarmGray,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SoftPink.copy(alpha = 0.2f),
                                SoftPink.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                val flowerEmoji = getFlowerEmoji(product.name)
                Text(
                    text = flowerEmoji,
                    fontSize = 64.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .clickable { isFavorite = !isFavorite },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFavorite) "❤️" else "🤍",
                        fontSize = 20.sp
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = String.format("%.0f ₽", product.price),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = WarmGray,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (product.status == "В наличии") "● В наличии" else "● Нет в наличии",
                        fontSize = 11.sp,
                        color = if (product.status == "В наличии") Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    minPrice: Double?,
    maxPrice: Double?,
    onMinPriceChange: (Double?) -> Unit,
    onMaxPriceChange: (Double?) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 20.sp, color = WarmGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Цена",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minPrice?.let { String.format("%.0f", it) } ?: "",
                    onValueChange = { value: String ->
                        onMinPriceChange(value.toDoubleOrNull())
                    },
                    label = { Text("От", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPink,
                        focusedLabelColor = SoftPink,
                        unfocusedBorderColor = WarmGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = maxPrice?.let { String.format("%.0f", it) } ?: "",
                    onValueChange = { value: String ->
                        onMaxPriceChange(value.toDoubleOrNull())
                    },
                    label = { Text("До", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPink,
                        focusedLabelColor = SoftPink,
                        unfocusedBorderColor = WarmGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPink)
            ) {
                Text(
                    "Применить",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    cartState: CartState,
    onBack: () -> Unit,
    onAddToCart: (Product, Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { ProductDetailState() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(productId) {
        state.isLoading = true
        try {
            val details = ApiService.getProductDetails(productId)
            if (details != null) {
                state.productDetails = details
                state.error = null
            } else {
                state.error = "Не удалось загрузить детали товара"
            }
        } catch (e: Exception) {
            state.error = "Ошибка загрузки: ${e.message}"
            Log.e("ProductDetail", "Error loading product details", e)
        } finally {
            state.isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Детали товара",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 28.sp, color = SoftPink)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        BadgedBox(badgeCount = cartState.getTotalItems()) {
                            Text("🛒", fontSize = 24.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.productDetails != null && !state.isLoading) {
                ProductBottomBar(
                    product = state.productDetails!!.product,
                    price = state.productDetails!!.product.price,
                    quantity = state.quantity,
                    onQuantityChange = { state.quantity = it },
                    onAddToCart = {
                        val product = Product(
                            id = state.productDetails!!.product.id,
                            photo = state.productDetails!!.product.photo,
                            name = state.productDetails!!.product.name,
                            description = state.productDetails!!.product.description,
                            price = state.productDetails!!.product.price,
                            category = state.productDetails!!.product.category,
                            status = state.productDetails!!.product.status
                        )
                        onAddToCart(product, state.quantity)
                        state.quantity = 1
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("✅ ${product.name} добавлен в корзину")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SoftPink)
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "❌ ${state.error!!}",
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            state.isLoading = true
                            state.error = null
                            coroutineScope.launch {
                                try {
                                    val details = ApiService.getProductDetails(productId)
                                    if (details != null) {
                                        state.productDetails = details
                                    } else {
                                        state.error = "Не удалось загрузить детали товара"
                                    }
                                } catch (e: Exception) {
                                    state.error = "Ошибка загрузки: ${e.message}"
                                } finally {
                                    state.isLoading = false
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
        } else {
            state.productDetails?.let { details ->
                ProductDetailContent(
                    details = details,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun ProductDetailContent(
    details: ProductDetailsResponse,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SoftPink.copy(alpha = 0.3f),
                                SoftPink.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                val flowerEmoji = getFlowerEmoji(details.product.name)
                Text(
                    text = flowerEmoji,
                    fontSize = 100.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = details.product.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("%.0f ₽", details.product.price),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📂 ", fontSize = 14.sp)
                        Text(
                            text = details.product.category,
                            fontSize = 14.sp,
                            color = WarmGray
                        )
                    }

                    Text(
                        text = if (details.product.status == "В наличии") "● В наличии" else "● Нет в наличии",
                        fontSize = 14.sp,
                        color = if (details.product.status == "В наличии") Color(0xFF4CAF50) else Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = WarmGray.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Описание",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = details.product.description ?: "Нет описания",
                    fontSize = 14.sp,
                    color = TextPrimary.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = WarmGray.copy(alpha = 0.2f))
            }
        }

        if (details.composition.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Состав",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.05f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            details.composition.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${item.name}",
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${item.quantity} шт",
                                        fontSize = 14.sp,
                                        color = SoftPinkDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = WarmGray.copy(alpha = 0.2f))
            }
        }

        item {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Отзывы",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    if (details.reviews.isNotEmpty()) {
                        Text(
                            text = "${details.reviews.size} отзывов",
                            fontSize = 14.sp,
                            color = WarmGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (details.reviews.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftPink.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💬", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Пока нет отзывов",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Будьте первым, кто оставит отзыв!",
                                fontSize = 12.sp,
                                color = WarmGray
                            )
                        }
                    }
                } else {
                    details.reviews.take(3).forEach { review ->
                        ReviewItem(review = review)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (details.reviews.size > 3) {
                        TextButton(
                            onClick = { /* Show all reviews */ },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Показать все отзывы (${details.reviews.size})",
                                color = SoftPink,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👤", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = review.clientName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Row {
                    repeat(5) { index ->
                        Text(
                            text = if (index < review.rating) "★" else "☆",
                            fontSize = 14.sp,
                            color = if (index < review.rating) SoftPink else WarmGray
                        )
                    }
                }
            }

            if (!review.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    fontSize = 13.sp,
                    color = TextPrimary.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ProductBottomBar(
    product: ProductDetail,
    price: Double,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                        .clickable { if (quantity > 1) onQuantityChange(quantity - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        fontSize = 20.sp,
                        color = SoftPinkDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = quantity.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                        .clickable { onQuantityChange(quantity + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        color = SoftPinkDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = String.format("%.0f ₽", price * quantity),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )

                Button(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "В корзину",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartState: CartState,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Корзина",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 20.sp, color = WarmGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cartState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛍️", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Корзина пуста",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Добавьте товары из каталога",
                            fontSize = 14.sp,
                            color = WarmGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartState.items) { item ->
                        CartItemCard(
                            item = item,
                            onQuantityChange = { onUpdateQuantity(item.productId, it) },
                            onRemove = { onRemoveItem(item.productId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = WarmGray.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Итого:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format("%.0f ₽", cartState.getTotalPrice()),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftPinkDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPink)
                ) {
                    Text(
                        text = "Оформить заказ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val flowerEmoji = getFlowerEmoji(item.name)
                Text(text = flowerEmoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format("%.0f ₽", item.price),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                            .clickable { onQuantityChange(item.quantity - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = item.quantity.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                            .clickable { onQuantityChange(item.quantity + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Удалить",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

// Компоненты конструктора букетов
@Composable
fun PackagingList(
    packaging: List<RawMaterial>,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(packaging) { item ->
            PackagingItem(
                packaging = item,
                isSelected = selectedId == item.idRawName,
                onSelect = { onSelect(item.idRawName) }
            )
        }
    }
}

@Composable
fun PackagingItem(
    packaging: RawMaterial,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SoftPink.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val packagingEmoji = when {
                packaging.rawName.contains("корзин") -> "🧺"
                packaging.rawName.contains("лент") -> "🎀"
                packaging.rawName.contains("упаковк") -> "📦"
                packaging.rawName.contains("крафт") -> "📜"
                packaging.rawName.contains("пленк") -> "🛍️"
                packaging.rawName.contains("коробк") -> "📦"
                packaging.rawName.contains("сетк") -> "🕸️"
                packaging.rawName.contains("бант") -> "🎀"
                packaging.rawName.contains("открыт") -> "💌"
                else -> "🎁"
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = packagingEmoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = packaging.rawName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "${packaging.rawPrice} ₽",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )
                if (packaging.quantity > 0) {
                    Text(
                        text = "В наличии: ${packaging.quantity} шт",
                        fontSize = 12.sp,
                        color = SoftPink
                    )
                } else {
                    Text(
                        text = "Нет в наличии",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }

            if (isSelected) {
                Text("✅", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun FlowersList(
    flowers: List<RawMaterial>,
    selectedFlowers: SnapshotStateList<BouquetItem>,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onQuantityChange: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(flowers) { flower ->
            val selected = selectedFlowers.find { it.idRawName == flower.idRawName }

            FlowerItem(
                flower = flower,
                quantity = selected?.quantity ?: 0,
                onAdd = { onAdd(flower.idRawName) },
                onRemove = { onRemove(flower.idRawName) },
                onQuantityChange = { quantity -> onQuantityChange(flower.idRawName, quantity) }
            )
        }
    }
}

@Composable
fun FlowerItem(
    flower: RawMaterial,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flowerEmoji = getFlowerEmoji(flower.rawName)

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftPink.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = flowerEmoji, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = flower.rawName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${flower.rawPrice} ₽/шт",
                        fontSize = 14.sp,
                        color = SoftPinkDark
                    )
                    if (flower.quantity > 0) {
                        Text(
                            text = "В наличии: ${flower.quantity} шт",
                            fontSize = 12.sp,
                            color = SoftPink
                        )
                    } else {
                        Text(
                            text = "Нет в наличии",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }
                }
            }

            if (flower.quantity > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (quantity > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                                    .clickable { onRemove() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            TextField(
                                value = quantity.toString(),
                                onValueChange = { newValue: String ->
                                    val newQty = newValue.toIntOrNull() ?: 0
                                    if (newQty >= 0 && newQty <= flower.quantity) {
                                        onQuantityChange(newQty)
                                    }
                                },
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = SoftPink,
                                    unfocusedIndicatorColor = WarmGray
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                                    .clickable(enabled = quantity < flower.quantity) { onAdd() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity < flower.quantity) SoftPinkDark else WarmGray
                                )
                            }
                        }

                        Text(
                            text = "= ${flower.rawPrice * quantity} ₽",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftPinkDark
                        )
                    } else {
                        Button(
                            onClick = onAdd,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPink)
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtrasList(
    extras: List<RawMaterial>,
    selectedExtras: SnapshotStateList<BouquetItem>,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onQuantityChange: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(extras) { extra ->
            val selected = selectedExtras.find { it.idRawName == extra.idRawName }

            ExtraItem(
                extra = extra,
                quantity = selected?.quantity ?: 0,
                onAdd = { onAdd(extra.idRawName) },
                onRemove = { onRemove(extra.idRawName) },
                onQuantityChange = { quantity -> onQuantityChange(extra.idRawName, quantity) }
            )
        }
    }
}

@Composable
fun ExtraItem(
    extra: RawMaterial,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val extraEmoji = when {
                    extra.rawName.contains("лент") -> "🎀"
                    extra.rawName.contains("открыт") -> "💌"
                    extra.rawName.contains("игруш") -> "🧸"
                    extra.rawName.contains("конфет") -> "🍬"
                    extra.rawName.contains("шарик") -> "🎈"
                    extra.rawName.contains("свеч") -> "🕯️"
                    extra.rawName.contains("горш") -> "🏺"
                    extra.rawName.contains("нож") -> "🔪"
                    extra.rawName.contains("секатор") -> "✂️"
                    extra.rawName.contains("грунт") -> "🌱"
                    extra.rawName.contains("удобрен") -> "💊"
                    else -> "✨"
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftPink.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = extraEmoji, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = extra.rawName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${extra.rawPrice} ₽/шт",
                        fontSize = 14.sp,
                        color = SoftPinkDark
                    )
                    if (extra.quantity > 0) {
                        Text(
                            text = "В наличии: ${extra.quantity} шт",
                            fontSize = 12.sp,
                            color = SoftPink
                        )
                    } else {
                        Text(
                            text = "Нет в наличии",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }
                }
            }

            if (extra.quantity > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (quantity > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                                    .clickable { onRemove() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            TextField(
                                value = quantity.toString(),
                                onValueChange = { newValue: String ->
                                    val newQty = newValue.toIntOrNull() ?: 0
                                    if (newQty >= 0 && newQty <= extra.quantity) {
                                        onQuantityChange(newQty)
                                    }
                                },
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = SoftPink,
                                    unfocusedIndicatorColor = WarmGray
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SoftPink.copy(alpha = 0.1f), CircleShape)
                                    .clickable(enabled = quantity < extra.quantity) { onAdd() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity < extra.quantity) SoftPinkDark else WarmGray
                                )
                            }
                        }

                        Text(
                            text = "= ${extra.rawPrice * quantity} ₽",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftPinkDark
                        )
                    } else {
                        Button(
                            onClick = onAdd,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPink)
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentPriceCard(
    totalPrice: Double,
    itemsCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPink),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Текущая стоимость",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = String.format("%.0f ₽", totalPrice),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Badge(
                containerColor = Color.White
            ) {
                Text(
                    text = "$itemsCount элементов",
                    color = SoftPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BouquetPreviewBottomSheet(
    preview: BouquetPreviewResponse,
    onDismiss: () -> Unit,
    onOrderClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ваш букет",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 20.sp, color = WarmGray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SoftPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌸", fontSize = 80.sp)
                    Text("✨", fontSize = 60.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Состав букета:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            preview.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• ${item.name} x${item.quantity}",
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${item.totalPrice} ₽",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoftPinkDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = WarmGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Итого:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = String.format("%.0f ₽", preview.totalPrice),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOrderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPink)
            ) {
                Text(
                    text = "Оформить заказ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormBottomSheet(
    totalPrice: Double,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, String, String, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var deliveryMethod by remember { mutableStateOf(1) }
    var paymentMethod by remember { mutableStateOf(1) }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Оформление заказа",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Способ доставки",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { deliveryMethod = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliveryMethod == 1) SoftPink else Color.White,
                        contentColor = if (deliveryMethod == 1) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = if (deliveryMethod != 1) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text("📦 Самовывоз")
                }
                Button(
                    onClick = { deliveryMethod = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliveryMethod == 2) SoftPink else Color.White,
                        contentColor = if (deliveryMethod == 2) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = if (deliveryMethod != 2) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text("🚚 Доставка")
                }
            }

            if (deliveryMethod == 2) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес доставки") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPink,
                        focusedLabelColor = SoftPink
                    ),
                    isError = address.isBlank() && deliveryMethod == 2
                )
                if (address.isBlank() && deliveryMethod == 2) {
                    Text(
                        text = "Укажите адрес доставки",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Способ оплаты",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { paymentMethod = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentMethod == 1) SoftPink else Color.White,
                        contentColor = if (paymentMethod == 1) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = if (paymentMethod != 1) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text("💳 Картой")
                }
                Button(
                    onClick = { paymentMethod = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentMethod == 2) SoftPink else Color.White,
                        contentColor = if (paymentMethod == 2) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = if (paymentMethod != 2) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text("💰 Наличными")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Комментарий к заказу") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftPink,
                    focusedLabelColor = SoftPink
                ),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = deliveryDate,
                onValueChange = {
                    deliveryDate = it
                    dateError = null
                    // Валидируем формат даты
                    if (it.isNotBlank()) {
                        try {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            LocalDate.parse(it, formatter)
                        } catch (e: DateTimeParseException) {
                            dateError = "Неверный формат. Используйте ГГГГ-ММ-ДД"
                        }
                    }
                },
                label = { Text("Желаемая дата доставки") },
                placeholder = { Text("ГГГГ-ММ-ДД") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftPink,
                    focusedLabelColor = SoftPink
                ),
                isError = dateError != null,
                supportingText = {
                    if (dateError != null) {
                        Text(
                            text = dateError!!,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Divider(color = WarmGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Итого к оплате:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = String.format("%.0f ₽", totalPrice),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftPinkDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (deliveryMethod == 2 && address.isBlank()) {
                        return@Button
                    }
                    if (deliveryDate.isNotBlank() && dateError != null) {
                        return@Button
                    }
                    isLoading = true
                    onSubmit(
                        deliveryMethod,
                        paymentMethod,
                        address,
                        notes,
                        if (deliveryDate.isNotBlank()) deliveryDate else null
                    )
                    isLoading = false
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                enabled = !isLoading &&
                        (deliveryMethod != 2 || address.isNotBlank()) &&
                        (deliveryDate.isBlank() || dateError == null)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Подтвердить заказ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Вспомогательные функции
suspend fun loadCategories(
    state: CatalogState,
    snackbarHostState: SnackbarHostState
) {
    state.isLoading = true
    try {
        val categories = ApiService.getCategories()
        if (categories != null) {
            state.categories = categories
            state.error = null
        } else {
            state.error = "Не удалось загрузить категории"
        }
    } catch (e: Exception) {
        state.error = "Ошибка: ${e.message}"
        Log.e("Catalog", "Error loading categories", e)
    } finally {
        state.isLoading = false
    }
}

suspend fun loadProducts(
    state: CatalogState,
    snackbarHostState: SnackbarHostState
) {
    state.isLoading = true
    try {
        val products = ApiService.getProducts(
            categoryId = state.selectedCategory,
            search = state.searchQuery.ifBlank { null },
            minPrice = state.minPrice,
            maxPrice = state.maxPrice
        )
        if (products != null) {
            state.products = products
            state.error = null
        } else {
            state.error = "Не удалось загрузить товары"
        }
    } catch (e: Exception) {
        state.error = "Ошибка: ${e.message}"
        Log.e("Catalog", "Error loading products", e)
    } finally {
        state.isLoading = false
    }
}

suspend fun loadConstructorData(
    state: BouquetConstructorState,
    snackbarHostState: SnackbarHostState
) {
    state.isLoading = true
    try {
        val packaging = ApiService.getRawMaterialsByType("packaging")
        if (packaging != null) {
            state.availablePackaging = packaging.filter { it.quantity > 0 }
            Log.d("Constructor", "Loaded ${state.availablePackaging.size} packaging items")
        } else {
            state.availablePackaging = emptyList()
            Log.w("Constructor", "No packaging data received")
        }

        val flowers = ApiService.getRawMaterialsByType("flowers")
        if (flowers != null) {
            state.availableFlowers = flowers.filter { it.quantity > 0 }
            Log.d("Constructor", "Loaded ${state.availableFlowers.size} flower items")
        } else {
            state.availableFlowers = emptyList()
            Log.w("Constructor", "No flowers data received")
        }

        val extras = ApiService.getRawMaterialsByType("extras")
        if (extras != null) {
            state.availableExtras = extras.filter { it.quantity > 0 }
            Log.d("Constructor", "Loaded ${state.availableExtras.size} extra items")
        } else {
            state.availableExtras = emptyList()
            Log.w("Constructor", "No extras data received")
        }

        state.error = null
    } catch (e: Exception) {
        state.error = "Ошибка: ${e.message}"
        Log.e("BouquetConstructor", "Error loading constructor data", e)
    } finally {
        state.isLoading = false
    }
}

suspend fun createOrderFromCart(
    cartState: CartState,
    context: Context,
    snackbarHostState: SnackbarHostState,
    deliveryMethod: Int,
    paymentMethod: Int,
    address: String,
    notes: String,
    deliveryDate: String?,
    totalPrice: Double,
    onSuccess: (Int) -> Unit
) {
    val userData = getCurrentUserData(context)
    if (userData?.clientId == null) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Ошибка: пользователь не авторизован")
        }
        return
    }

    if (cartState.items.isEmpty()) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Корзина пуста")
        }
        return
    }

    val items = cartState.items.map { item ->
        OrderItemRequest(
            idProduct = item.productId,
            quantity = item.quantity
        )
    }

    // Преобразуем дату в формат ISO 8601 для PostgreSQL timestamp
    val formattedDeliveryDate = if (!deliveryDate.isNullOrBlank()) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val parsedDate = LocalDate.parse(deliveryDate, formatter)
            parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            Log.e("OrderCreation", "Invalid date format: $deliveryDate")
            null
        }
    } else {
        null
    }

    val request = OrderCreateRequest(
        idDeliveryMethod = deliveryMethod,
        idPaymentMethod = paymentMethod,
        deliveryAddress = address,
        plannedDeliveryDate = formattedDeliveryDate,
        clientNotes = notes,
        items = items,
        totalAmount = totalPrice
    )

    try {
        val response = ApiService.createOrder(request, userData.clientId)
        if (response != null) {
            withContext(Dispatchers.Main) {
                onSuccess(response.orderId)
            }
        } else {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("❌ Не удалось создать заказ")
            }
        }
    } catch (e: Exception) {
        Log.e("OrderCreation", "Error creating order", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Ошибка: ${e.message}")
        }
    }
}

suspend fun createOrderFromBouquet(
    constructorState: BouquetConstructorState,
    context: Context,
    snackbarHostState: SnackbarHostState,
    deliveryMethod: Int,
    paymentMethod: Int,
    address: String,
    notes: String,
    deliveryDate: String?,
    totalPrice: Double,
    onSuccess: (Int) -> Unit
) {
    val userData = getCurrentUserData(context)
    if (userData?.clientId == null) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Ошибка: пользователь не авторизован")
        }
        return
    }

    if (constructorState.selectedPackaging == null || constructorState.selectedFlowers.isEmpty()) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Выберите упаковку и хотя бы один цветок")
        }
        return
    }

    // Преобразуем дату в формат ISO 8601 для PostgreSQL timestamp
    val formattedDeliveryDate = if (!deliveryDate.isNullOrBlank()) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val parsedDate = LocalDate.parse(deliveryDate, formatter)
            parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            Log.e("OrderCreation", "Invalid date format: $deliveryDate")
            null
        }
    } else {
        null
    }

    // Создаем подробное описание букета для комментария
    val bouquetDescription = buildString {
        append("🌸 Букет из конструктора\n\n")
        append("📦 Упаковка: ${constructorState.availablePackaging.find { it.idRawName == constructorState.selectedPackaging }?.rawName ?: "ID=${constructorState.selectedPackaging}"}\n\n")
        append("🌺 Цветы:\n")
        constructorState.selectedFlowers.forEach { flower ->
            val flowerName = constructorState.availableFlowers.find { it.idRawName == flower.idRawName }?.rawName ?: "ID=${flower.idRawName}"
            append("  • $flowerName x${flower.quantity}\n")
        }
        if (constructorState.selectedExtras.isNotEmpty()) {
            append("\n✨ Декор:\n")
            constructorState.selectedExtras.forEach { extra ->
                val extraName = constructorState.availableExtras.find { it.idRawName == extra.idRawName }?.rawName ?: "ID=${extra.idRawName}"
                append("  • $extraName x${extra.quantity}\n")
            }
        }
        append("\n💰 Общая стоимость: ${totalPrice} ₽")
    }

    // Для букета из конструктора используем специальный ID товара
    val items = listOf(
        OrderItemRequest(
            idProduct = 999, // ID для кастомного букета
            quantity = 1
        )
    )

    val fullNotes = if (notes.isNotEmpty()) {
        "$notes\n\n$bouquetDescription"
    } else {
        bouquetDescription
    }

    val request = OrderCreateRequest(
        idDeliveryMethod = deliveryMethod,
        idPaymentMethod = paymentMethod,
        deliveryAddress = address,
        plannedDeliveryDate = formattedDeliveryDate,
        clientNotes = fullNotes,
        items = items,
        totalAmount = totalPrice
    )

    try {
        val response = ApiService.createOrder(request, userData.clientId)
        if (response != null) {
            withContext(Dispatchers.Main) {
                onSuccess(response.orderId)
            }
        } else {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("❌ Не удалось создать заказ")
            }
        }
    } catch (e: Exception) {
        Log.e("OrderCreation", "Error creating bouquet order", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("❌ Ошибка: ${e.message}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCatalogTab() {
    MaterialTheme {
        CatalogTab()
    }
}