package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen {
    SPLASH,
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD,
    HOME,
    CART,
    CHECKOUT,
    TRACKING,
    PROFILE,
    ORDER_HISTORY
}

data class CartDisplayItem(
    val cartId: Int,
    val foodItem: FoodItem,
    val quantity: Int
)

data class ToastMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val type: ToastType = ToastType.INFO
)

enum class ToastType {
    SUCCESS, INFO, ERROR, WARNING
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getDatabase(application).appDao()

    // Screen State
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Theme Toggle
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _demoOtpCode = MutableStateFlow("")
    val demoOtpCode: StateFlow<String> = _demoOtpCode.asStateFlow()

    private val _showOtpVerifyDialog = MutableStateFlow(false)
    val showOtpVerifyDialog: StateFlow<Boolean> = _showOtpVerifyDialog.asStateFlow()

    private val _otpTempUser = MutableStateFlow<UserEntity?>(null) // Temp holder for login/signup before OTP

    // Menu Data
    val allCategories = FoodData.categories
    private val _allFoodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val allFoodItemsFlow: StateFlow<List<FoodItem>> = _allFoodItems.asStateFlow()
    val allFoodItems: List<FoodItem> get() = _allFoodItems.value

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Favorites State (Stored in Room DB)
    private val _favoriteFoodIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteFoodIds: StateFlow<Set<Int>> = _favoriteFoodIds.asStateFlow()

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItemEntity>>(emptyList())
    val cartItems: StateFlow<List<CartItemEntity>> = _cartItems.asStateFlow()

    val cartDisplayItems: StateFlow<List<CartDisplayItem>> = combine(_cartItems, _allFoodItems) { cartList, foodList ->
        cartList.mapNotNull { entity ->
            val food = foodList.find { it.id == entity.foodId }
            if (food != null) {
                CartDisplayItem(entity.id, food, entity.quantity)
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Promo Code
    private val _appliedPromoCode = MutableStateFlow<String?>(null)
    val appliedPromoCode: StateFlow<String?> = _appliedPromoCode.asStateFlow()

    // Order and Review States
    private val _orderHistory = MutableStateFlow<List<OrderHistoryEntity>>(emptyList())
    val orderHistory: StateFlow<List<OrderHistoryEntity>> = _orderHistory.asStateFlow()

    private val _selectedFoodItem = MutableStateFlow<FoodItem?>(null)
    val selectedFoodItem: StateFlow<FoodItem?> = _selectedFoodItem.asStateFlow()

    private val _activeReviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val activeReviews: StateFlow<List<ReviewEntity>> = _activeReviews.asStateFlow()

    // Active Order Tracking State
    private val _activeTrackedOrder = MutableStateFlow<OrderHistoryEntity?>(null)
    val activeTrackedOrder: StateFlow<OrderHistoryEntity?> = _activeTrackedOrder.asStateFlow()

    private val _droneProgress = MutableStateFlow(0.0f) // 0.0f to 1.0f
    val droneProgress: StateFlow<Float> = _droneProgress.asStateFlow()

    private val _droneStatus = MutableStateFlow("Preparing order")
    val droneStatus: StateFlow<String> = _droneStatus.asStateFlow()

    private val _droneGps = MutableStateFlow<GpsCoordinates>(
        GpsCoordinates(
            latitude = DroneGpsService.HANGAR_LAT,
            longitude = DroneGpsService.HANGAR_LNG,
            altitudeMeters = 0.0,
            speedKmh = 0.0,
            satellitesCount = 8,
            signalStrengthPercentage = 90,
            signalStatus = "CALIBRATING_SENSORS"
        )
    )
    val droneGps: StateFlow<GpsCoordinates> = _droneGps.asStateFlow()

    private val _deliverySecondsRemaining = MutableStateFlow(150) // Speeded up countdown for demo (starts at 150s)
    val deliverySecondsRemaining: StateFlow<Int> = _deliverySecondsRemaining.asStateFlow()

    private val _showReviewFormForCompletedOrder = MutableStateFlow<Int?>(null) // stores foodId to review
    val showReviewFormForCompletedOrder: StateFlow<Int?> = _showReviewFormForCompletedOrder.asStateFlow()

    // Voice Search Mocks
    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice: StateFlow<Boolean> = _isListeningVoice.asStateFlow()

    // Notification Toasts
    private val _toasts = MutableStateFlow<List<ToastMessage>>(emptyList())
    val toasts: StateFlow<List<ToastMessage>> = _toasts.asStateFlow()

    private var trackingJob: Job? = null

    init {
        // Collect database flows
        viewModelScope.launch {
            appDao.getCartItems().collect { _cartItems.value = it }
        }
        viewModelScope.launch {
            appDao.getFavorites().collect { favList ->
                _favoriteFoodIds.value = favList.map { it.foodId }.toSet()
            }
        }
        viewModelScope.launch {
            appDao.getOrderHistory().collect { list ->
                _orderHistory.value = list
                
                // Real-time synchronization of active tracked order with backend/Admin database updates!
                val active = _activeTrackedOrder.value
                if (active != null) {
                    val latest = list.find { it.id == active.id }
                    if (latest != null && latest.status != active.status) {
                        _activeTrackedOrder.value = latest
                        _droneStatus.value = latest.status
                        
                        val (mappedProgress, secondsLeft) = when (latest.status) {
                            "Preparing order", "Preparing" -> 0.10f to 150
                            "Drone dispatched", "Dispatched" -> 0.35f to 110
                            "In transit", "In Transit" -> 0.65f to 60
                            "Arriving" -> 0.85f to 20
                            "Delivered" -> 1.0f to 0
                            else -> 0.10f to 150
                        }
                        
                        _droneProgress.value = mappedProgress
                        _deliverySecondsRemaining.value = secondsLeft
                        
                        if (latest.status == "Delivered") {
                            trackingJob?.cancel()
                            showToast("Delivery dropped! Landing successful.", ToastType.SUCCESS)
                            val firstFoodId = latest.itemsJson.split(",").firstOrNull()?.split(":")?.firstOrNull()?.toIntOrNull()
                            if (firstFoodId != null) {
                                _showReviewFormForCompletedOrder.value = firstFoodId
                            }
                        } else {
                            showToast("Flight status updated: ${latest.status}", ToastType.INFO)
                        }
                        
                        _droneGps.value = DroneGpsService.trackDroneGps(
                            progress = mappedProgress,
                            status = latest.status,
                            orderId = latest.id
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            try {
                val existing = appDao.getFoodItemsOnce()
                if (existing.isEmpty()) {
                    val entities = FoodData.foodItems.map { it.toEntity() }
                    appDao.insertFoodItems(entities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            appDao.getFoodItemsFlow().collect { entities ->
                _allFoodItems.value = entities.map { it.toDomainModel() }
            }
        }

        // Animated Splash Screen Flow
        viewModelScope.launch {
            delay(2200)
            _currentScreen.value = Screen.LOGIN
        }
    }

    // Toast Utility
    fun showToast(text: String, type: ToastType = ToastType.INFO) {
        viewModelScope.launch {
            val newToast = ToastMessage(text = text, type = type)
            _toasts.value = _toasts.value + newToast
            delay(4000) // Toast duration
            _toasts.value = _toasts.value.filter { it.id != newToast.id }
        }
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
        showToast(
            if (_isDarkMode.value) "Futuristic Dark Mode Activated" else "Clean Light Mode Activated",
            ToastType.INFO
        )
    }

    // Screen Navigation
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Authentication System ---
    fun registerUser(name: String, email: String, phone: String, password: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            showToast("Please fill all details.", ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            val existing = appDao.getUserByEmail(email)
            if (existing != null) {
                showToast("Email already registered.", ToastType.ERROR)
                return@launch
            }
            val newUser = UserEntity(
                email = email,
                phone = phone,
                name = name,
                passwordHash = password // Secure hashing mock
            )
            // Save temp user for OTP verification
            _otpTempUser.value = newUser
            triggerOtpVerification()
        }
    }

    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            showToast("Please fill all fields.", ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            val user = appDao.getUserByEmail(email)
            if (user == null || user.passwordHash != password) {
                showToast("Invalid email or password.", ToastType.ERROR)
                return@launch
            }
            _otpTempUser.value = user
            triggerOtpVerification()
        }
    }

    private fun triggerOtpVerification() {
        val otp = (100000 + Random.nextInt(900000)).toString()
        _demoOtpCode.value = otp
        _showOtpVerifyDialog.value = true
        showToast("DEMO OTP Sent to device: $otp", ToastType.WARNING)
    }

    fun verifyOtp(enteredCode: String) {
        if (enteredCode == _demoOtpCode.value) {
            _showOtpVerifyDialog.value = false
            val user = _otpTempUser.value
            if (user != null) {
                viewModelScope.launch {
                    // If it was registration, insert to DB now
                    val dbUser = appDao.getUserByEmail(user.email)
                    if (dbUser == null) {
                        appDao.insertUser(user)
                    }
                    val loggedUser = appDao.getUserByEmail(user.email)
                    _currentUser.value = loggedUser
                    _otpTempUser.value = null
                    showToast("Welcome back, ${loggedUser?.name ?: "Pilot"}!", ToastType.SUCCESS)
                    _currentScreen.value = Screen.HOME
                }
            }
        } else {
            showToast("Invalid OTP code. Try again.", ToastType.ERROR)
        }
    }

    fun closeOtpDialog() {
        _showOtpVerifyDialog.value = false
        _otpTempUser.value = null
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            showToast("Please enter email.", ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            val user = appDao.getUserByEmail(email)
            if (user == null) {
                showToast("Email not found in database.", ToastType.ERROR)
            } else {
                showToast("Reset password link dispatched to $email!", ToastType.SUCCESS)
                _currentScreen.value = Screen.LOGIN
            }
        }
    }

    fun updateProfile(name: String, phone: String, email: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = name, phone = phone, email = email)
            appDao.insertUser(updated)
            _currentUser.value = updated
            showToast("Quantum Profile Updated!", ToastType.SUCCESS)
        }
    }

    fun logout() {
        _currentUser.value = null
        showToast("Logged out from SkyBite Central", ToastType.INFO)
        _currentScreen.value = Screen.LOGIN
    }

    // --- Search & Categories ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun startVoiceSearch() {
        viewModelScope.launch {
            _isListeningVoice.value = true
            showToast("Securing quantum channel. Speak now...", ToastType.INFO)
            delay(2500)
            val searchMocks = listOf("Biryani", "Double Smash Burger", "Choco Volcano", "Sushi", "Paneer Tikka", "Alfredo White Pasta")
            val selected = searchMocks.random()
            _searchQuery.value = selected
            _isListeningVoice.value = false
            showToast("Voice detected: \"$selected\"", ToastType.SUCCESS)
        }
    }

    // --- Favorites Manager ---
    fun toggleFavorite(foodId: Int) {
        viewModelScope.launch {
            if (_favoriteFoodIds.value.contains(foodId)) {
                appDao.deleteFavorite(foodId)
                _favoriteFoodIds.value = _favoriteFoodIds.value - foodId
                showToast("Removed from Flight Favorites.", ToastType.INFO)
            } else {
                appDao.insertFavorite(FavoriteEntity(foodId))
                _favoriteFoodIds.value = _favoriteFoodIds.value + foodId
                showToast("Added to Flight Favorites!", ToastType.SUCCESS)
            }
        }
    }

    // --- Cart Management ---
    fun addToCart(foodId: Int) {
        val foodName = allFoodItems.find { it.id == foodId }?.name ?: "Item"
        viewModelScope.launch {
            val existing = _cartItems.value.find { it.foodId == foodId }
            if (existing != null) {
                appDao.updateCartQuantity(existing.id, existing.quantity + 1)
            } else {
                appDao.insertCartItem(CartItemEntity(foodId = foodId, quantity = 1))
            }
            showToast("Added $foodName to Hangar Cart!", ToastType.SUCCESS)
        }
    }

    fun updateCartItemQty(cartId: Int, newQty: Int) {
        viewModelScope.launch {
            if (newQty <= 0) {
                appDao.deleteCartItem(cartId)
                showToast("Removed from Hangar Cart", ToastType.INFO)
            } else {
                appDao.updateCartQuantity(cartId, newQty)
            }
        }
    }

    fun removeCartItem(cartId: Int) {
        viewModelScope.launch {
            appDao.deleteCartItem(cartId)
            showToast("Removed item", ToastType.INFO)
        }
    }

    fun applyPromo(code: String) {
        val formatted = code.uppercase().trim()
        if (formatted == "SKYFLY20" || formatted == "DRONEFREE" || formatted == "BITE100") {
            _appliedPromoCode.value = formatted
            showToast("Promo Code $formatted Applied successfully!", ToastType.SUCCESS)
        } else {
            showToast("Invalid Promo Code", ToastType.ERROR)
        }
    }

    fun removePromo() {
        _appliedPromoCode.value = null
        showToast("Promo Code Removed", ToastType.INFO)
    }

    // Pricing calculation helpers
    fun getCartSummary(): CartSummary {
        val list = cartDisplayItems.value
        val subtotal = list.sumOf { it.foodItem.price * it.quantity }
        val promo = _appliedPromoCode.value

        var discount = 0.0
        var deliveryFee = 30.0

        if (promo != null) {
            when (promo) {
                "SKYFLY20" -> discount = subtotal * 0.20
                "DRONEFREE" -> deliveryFee = 0.0
                "BITE100" -> if (subtotal >= 400.0) discount = 100.0
            }
        }

        val gst = (subtotal - discount) * 0.18
        val grandTotal = if (subtotal > 0) (subtotal - discount + deliveryFee + gst) else 0.0

        return CartSummary(
            subtotal = subtotal,
            discount = discount,
            deliveryFee = deliveryFee,
            gst = gst,
            grandTotal = grandTotal
        )
    }

    // --- Order Checkout & Drone Flight Simulator ---
    fun placeOrder(address: String, city: String, pincode: String, phone: String, paymentMethod: String) {
        if (cartDisplayItems.value.isEmpty()) {
            showToast("Your hangar cart is empty!", ToastType.ERROR)
            return
        }
        if (address.isBlank() || city.isBlank() || pincode.isBlank() || phone.isBlank()) {
            showToast("Delivery coordinates required.", ToastType.ERROR)
            return
        }

        viewModelScope.launch {
            val summary = getCartSummary()
            val listStr = cartDisplayItems.value.joinToString(",") { "${it.foodItem.id}:${it.quantity}" }
            
            val order = OrderHistoryEntity(
                timestamp = System.currentTimeMillis(),
                totalAmount = summary.grandTotal,
                itemsJson = listStr,
                deliveryAddress = "$address, $city, PIN: $pincode",
                status = "Preparing order"
            )

            val orderId = appDao.insertOrder(order)
            val fullOrder = order.copy(id = orderId.toInt())

            _activeTrackedOrder.value = fullOrder
            appDao.clearCart() // empty cart
            _appliedPromoCode.value = null

            showToast("Launch cleared! Payment approved.", ToastType.SUCCESS)
            _currentScreen.value = Screen.TRACKING
            startDroneFlightSimulation()
        }
    }

    private fun startDroneFlightSimulation() {
        trackingJob?.cancel()
        _droneProgress.value = 0.0f
        _droneStatus.value = "Preparing order"
        _deliverySecondsRemaining.value = 150

        val currentOrder = _activeTrackedOrder.value
        val orderId = currentOrder?.id ?: 9999

        _droneGps.value = DroneGpsService.trackDroneGps(0.0f, "Preparing order", orderId)

        trackingJob = viewModelScope.launch {
            // Speeded up delivery for demo: increments progress to 100% over 50 seconds
            val steps = 100
            for (i in 1..steps) {
                delay(500) // 50 seconds total
                _droneProgress.value = i / 100.0f
                _deliverySecondsRemaining.value = 150 - (i * 1.5).toInt()

                when (i) {
                    15 -> {
                        _droneStatus.value = "Drone dispatched"
                        showToast("Drone disengaging launcher pad!", ToastType.INFO)
                    }
                    40 -> {
                        _droneStatus.value = "In transit"
                        showToast("Drone is cruising in stratosphere at 60 km/h.", ToastType.INFO)
                    }
                    75 -> {
                        _droneStatus.value = "Arriving"
                        showToast("Drone is descending into your airspace!", ToastType.WARNING)
                    }
                    100 -> {
                        _droneStatus.value = "Delivered"
                        _deliverySecondsRemaining.value = 0
                        showToast("Delivery dropped! Landing successful.", ToastType.SUCCESS)
                        
                        // Set the order as delivered in Room DB
                        val active = _activeTrackedOrder.value
                        if (active != null) {
                            val updated = active.copy(status = "Delivered")
                            appDao.insertOrder(updated)
                        }

                        // Trigger Review pop up for the first item in the order
                        val firstFoodId = active?.itemsJson?.split(",")?.firstOrNull()?.split(":")?.firstOrNull()?.toIntOrNull()
                        if (firstFoodId != null) {
                            _showReviewFormForCompletedOrder.value = firstFoodId
                        }
                    }
                }

                _droneGps.value = DroneGpsService.trackDroneGps(
                    progress = _droneProgress.value,
                    status = _droneStatus.value,
                    orderId = orderId
                )
            }
        }
    }

    // --- Review System ---
    fun loadReviewsForFood(foodId: Int) {
        viewModelScope.launch {
            appDao.getReviewsForFood(foodId).collect {
                _activeReviews.value = it
            }
        }
    }

    fun submitReview(foodId: Int, rating: Int, comment: String) {
        val name = _currentUser.value?.name ?: "Anonymous Pilot"
        viewModelScope.launch {
            val entity = ReviewEntity(
                foodId = foodId,
                userName = name,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            appDao.insertReview(entity)
            showToast("Review transmitted successfully!", ToastType.SUCCESS)
            _showReviewFormForCompletedOrder.value = null
            // reload reviews
            loadReviewsForFood(foodId)
        }
    }

    fun closeReviewForm() {
        _showReviewFormForCompletedOrder.value = null
    }

    fun selectFoodItem(item: FoodItem?) {
        _selectedFoodItem.value = item
        if (item != null) {
            loadReviewsForFood(item.id)
        }
    }
}

data class CartSummary(
    val subtotal: Double,
    val discount: Double,
    val deliveryFee: Double,
    val gst: Double,
    val grandTotal: Double
)
