package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
    ORDER_HISTORY,
    ADMIN_DASHBOARD
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

    private val prefs = application.getSharedPreferences("skybite_admin_prefs", Context.MODE_PRIVATE)

    private val _adminUsername = MutableStateFlow(prefs.getString("admin_username", "admin") ?: "admin")
    val adminUsername: StateFlow<String> = _adminUsername.asStateFlow()

    private val _adminPassword = MutableStateFlow(prefs.getString("admin_password", "admin") ?: "admin")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _adminPhonePeNumber = MutableStateFlow(prefs.getString("admin_phonepe_number", "9876543210") ?: "9876543210")
    val adminPhonePeNumber: StateFlow<String> = _adminPhonePeNumber.asStateFlow()

    private val _adminPhonePeUpi = MutableStateFlow(prefs.getString("admin_phonepe_upi", "9876543210@ybl") ?: "9876543210@ybl")
    val adminPhonePeUpi: StateFlow<String> = _adminPhonePeUpi.asStateFlow()

    private val _adminPaymentSubscribed = MutableStateFlow(prefs.getBoolean("admin_payment_subscribed", false))
    val adminPaymentSubscribed: StateFlow<Boolean> = _adminPaymentSubscribed.asStateFlow()

    // Screen State
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Theme Toggle
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentUserRole = MutableStateFlow("User")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: StateFlow<List<UserEntity>> = _allUsers.asStateFlow()

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

    private val _heavyTrafficRoutingMode = MutableStateFlow(false)
    val heavyTrafficRoutingMode: StateFlow<Boolean> = _heavyTrafficRoutingMode.asStateFlow()

    val shortestPathNodes: StateFlow<List<MapNode>> = _heavyTrafficRoutingMode.map { heavy ->
        PathFinder.findShortestPath(startId = "HOTEL", endId = "CUSTOMER", heavyTraffic = heavy)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PathFinder.findShortestPath(startId = "HOTEL", endId = "CUSTOMER", heavyTraffic = false)
    )

    fun toggleHeavyTrafficMode() {
        _heavyTrafficRoutingMode.value = !_heavyTrafficRoutingMode.value
        val modeText = if (_heavyTrafficRoutingMode.value) "Heavy Strom/Congestion Routing Mode Active" else "Standard Stratospheric Clear Route Active"
        showToast("Dijkstra recalculated: $modeText", ToastType.SUCCESS)
    }

    fun getActiveOrderHotelName(): String {
        val active = _activeTrackedOrder.value ?: return "SkyBite Central Hangar"
        val firstFoodId = active.itemsJson.split(",").firstOrNull()?.split(":")?.firstOrNull()?.toIntOrNull()
        if (firstFoodId != null) {
            val items = _allFoodItems.value
            val match = items.find { it.id == firstFoodId }
            if (match != null) {
                return match.hotelName
            }
        }
        return "SkyBite Central Hangar"
    }

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

    private val _showDeliveredDialog = MutableStateFlow<OrderHistoryEntity?>(null)
    val showDeliveredDialog: StateFlow<OrderHistoryEntity?> = _showDeliveredDialog.asStateFlow()

    fun dismissDeliveredDialog() {
        _showDeliveredDialog.value = null
    }

    // --- Secure OTP Delivery & System Alerts ---
    private val _orderOtpsSubmitted = MutableStateFlow<Map<Int, String>>(emptyMap())
    val orderOtpsSubmitted: StateFlow<Map<Int, String>> = _orderOtpsSubmitted.asStateFlow()

    private val _oneMinuteNotificationAlert = MutableStateFlow<String?>(null)
    val oneMinuteNotificationAlert: StateFlow<String?> = _oneMinuteNotificationAlert.asStateFlow()

    private val _adminNotificationAlert = MutableStateFlow<String?>(null)
    val adminNotificationAlert: StateFlow<String?> = _adminNotificationAlert.asStateFlow()

    fun dismissOneMinuteAlert() {
        _oneMinuteNotificationAlert.value = null
    }

    fun dismissAdminNotificationAlert() {
        _adminNotificationAlert.value = null
    }

    fun getDeliveryOtp(orderId: Int): String {
        val hash = (orderId * 179 + 4823) % 9000 + 1000
        return hash.toString()
    }

    fun submitDeliveryOtp(orderId: Int, otp: String) {
        val current = _orderOtpsSubmitted.value.toMutableMap()
        current[orderId] = otp
        _orderOtpsSubmitted.value = current
        
        val correctOtp = getDeliveryOtp(orderId)
        if (otp == correctOtp) {
            showToast("Secure handshake complete! OTP matched.", ToastType.SUCCESS)
        } else {
            showToast("Security lockout active: Invalid OTP code '$otp'. Try again.", ToastType.ERROR)
        }
    }

    fun sendSystemPushNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "skybite_delivery_channel"
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "SkyBite Cargo Tracking",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates of drone air-routes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
            appDao.getAllUsersFlow().collect { _allUsers.value = it }
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
                            _showDeliveredDialog.value = latest
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
    fun registerUser(name: String, email: String, phone: String, password: String, role: String = "User", hotelName: String? = null) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            showToast("Please fill all details.", ToastType.ERROR)
            return
        }
        if (role == "Admin" && (hotelName == null || hotelName.isBlank())) {
            showToast("Hotel name is required for Admin registration.", ToastType.ERROR)
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
                passwordHash = password, // Secure hashing mock
                role = role,
                hotelName = if (role == "Admin") hotelName else null
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
                    _currentUserRole.value = loggedUser?.role ?: "User"
                    _otpTempUser.value = null
                    showToast("Welcome back, ${loggedUser?.name ?: "Pilot"}!", ToastType.SUCCESS)
                    if (loggedUser?.role == "Admin") {
                        _currentScreen.value = Screen.ADMIN_DASHBOARD
                    } else {
                        _currentScreen.value = Screen.HOME
                    }
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

    fun loginAdmin(emailText: String, passwordText: String) {
        if (emailText.isBlank() || passwordText.isBlank()) {
            showToast("Please fill all fields.", ToastType.ERROR)
            return
        }
        val cleanEmail = emailText.trim()
        val customUser = adminUsername.value
        val customPass = adminPassword.value
        
        val isDefaultAdmin = (cleanEmail == "admin" || cleanEmail == "admin@skybite.com") && 
                (passwordText == "admin" || passwordText == "admin123")
        val isCustomAdmin = cleanEmail.lowercase() == customUser.lowercase() && passwordText == customPass

        if (isDefaultAdmin || isCustomAdmin) {
            val adminName = if (isCustomAdmin) customUser else "SkyBite Commander Admin"
            showToast("Welcome back, $adminName!", ToastType.SUCCESS)
            _currentUser.value = UserEntity(
                id = -99,
                email = "admin@skybite.com",
                phone = "000-000-0000",
                name = adminName,
                passwordHash = customPass,
                role = "Admin",
                hotelName = "SkyBite Central Hangar"
            )
            _currentUserRole.value = "Admin"
            _currentScreen.value = Screen.ADMIN_DASHBOARD
        } else {
            viewModelScope.launch {
                val user = appDao.getUserByEmail(cleanEmail)
                if (user != null && user.passwordHash == passwordText && user.role == "Admin") {
                    showToast("Admin credentials matched. Opening Space Command for ${user.hotelName}...", ToastType.SUCCESS)
                    _currentUser.value = user
                    _currentUserRole.value = "Admin"
                    _currentScreen.value = Screen.ADMIN_DASHBOARD
                } else if (user != null && user.passwordHash == passwordText) {
                    showToast("This account is not registered as an Admin.", ToastType.ERROR)
                } else {
                    showToast("Invalid admin credentials. Use configured Unique Username or admin/admin", ToastType.ERROR)
                }
            }
        }
    }

    fun updateAdminSettings(username: String, password: String, phonePeNum: String, upiId: String) {
        if (username.isBlank() || password.isBlank() || phonePeNum.isBlank() || upiId.isBlank()) {
            showToast("Please fill all fields.", ToastType.ERROR)
            return
        }
        prefs.edit()
            .putString("admin_username", username.trim())
            .putString("admin_password", password.trim())
            .putString("admin_phonepe_number", phonePeNum.trim())
            .putString("admin_phonepe_upi", upiId.trim())
            .apply()

        _adminUsername.value = username.trim()
        _adminPassword.value = password.trim()
        _adminPhonePeNumber.value = phonePeNum.trim()
        _adminPhonePeUpi.value = upiId.trim()
        
        // Also update current logged in user name if applicable
        val curr = _currentUser.value
        if (curr != null && curr.id == -99) {
            _currentUser.value = curr.copy(name = username.trim())
        }
        showToast("Space Command credentials & PhonePe settings updated!", ToastType.SUCCESS)
    }

    fun toggleAdminSubscription(subscribed: Boolean) {
        prefs.edit().putBoolean("admin_payment_subscribed", subscribed).apply()
        _adminPaymentSubscribed.value = subscribed
        if (subscribed) {
            showToast("PhonePe Payment Gateway active! Subscription VALID.", ToastType.SUCCESS)
        } else {
            showToast("PhonePe Payment Gateway inactive! Subscription REVOKED.", ToastType.WARNING)
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String, adminOtpInput: String? = null) {
        viewModelScope.launch {
            val list = _orderHistory.value
            val found = list.find { it.id == orderId }
            if (found != null) {
                val correctOtp = getDeliveryOtp(orderId)
                
                if (newStatus == "Delivered") {
                    val userSubmittedOtp = _orderOtpsSubmitted.value[orderId]
                    val isUserVerified = userSubmittedOtp == correctOtp
                    val isAdminVerified = adminOtpInput?.trim() == correctOtp
                    
                    if (!isUserVerified && !isAdminVerified) {
                        showToast("ACCESS DENIED: Secure Lock active. Matching Delivery OTP required!", ToastType.ERROR)
                        return@launch
                    }
                    
                    // Trigger Successful delivery local notification!
                    sendSystemPushNotification(
                        title = "DELIVERY SUCCESSFUL!",
                        message = "Secure release confirmed for Order #$orderId! Cargo container unlocked."
                    )
                }

                val updated = found.copy(status = newStatus)
                appDao.insertOrder(updated)
                showToast("Order #$orderId updated to: $newStatus", ToastType.SUCCESS)
                
                if (_activeTrackedOrder.value?.id == orderId) {
                    _activeTrackedOrder.value = updated
                    _droneStatus.value = newStatus
                    val (mappedProgress, secondsLeft) = when (newStatus) {
                        "Preparing order", "Preparing" -> 0.10f to 150
                        "Drone dispatched", "Dispatched" -> 0.35f to 110
                        "In transit", "In Transit" -> 0.65f to 60
                        "Arriving" -> 0.85f to 20
                        "Delivered" -> 1.0f to 0
                        else -> 0.10f to 150
                    }
                    _droneProgress.value = mappedProgress
                    _deliverySecondsRemaining.value = secondsLeft
                    _droneGps.value = DroneGpsService.trackDroneGps(
                        progress = mappedProgress,
                        status = newStatus,
                        orderId = orderId
                    )
                }
            }
        }
    }

    fun deleteFoodItem(id: Int) {
        viewModelScope.launch {
            appDao.deleteFoodItem(id)
            showToast("Item deleted from Hangar Catalog", ToastType.SUCCESS)
        }
    }

    fun addNewFoodItem(name: String, price: Double, description: String, category: String, imageUrl: String) {
        if (name.isBlank() || price <= 0.0 || description.isBlank() || category.isBlank() || imageUrl.isBlank()) {
            showToast("Please enter valid item details.", ToastType.ERROR)
            return
        }
        val adminHotel = _currentUser.value?.hotelName ?: "SkyBite Central Hangar"
        viewModelScope.launch {
            val item = FoodItemEntity(
                name = name,
                price = price,
                description = description,
                category = category,
                imageUrl = imageUrl,
                isTrending = Random.nextBoolean(),
                isPopular = Random.nextBoolean(),
                isAiRecommended = Random.nextBoolean(),
                rating = 4.0 + Random.nextDouble() * 1.0,
                deliveryTimeMin = 15 + Random.nextInt(20),
                hotelName = adminHotel
            )
            appDao.insertFoodItem(item)
            showToast("Successfully deployed new food item for $adminHotel!", ToastType.SUCCESS)
        }
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
            // Speeded up delivery for demo: increments progress to 75% then pauses, waiting for admin Delivered update
            val steps = 75
            for (i in 1..steps) {
                delay(500) // 37.5 seconds total simulation flight
                _droneProgress.value = i / 100.0f
                _deliverySecondsRemaining.value = 150 - (i * 1.5).toInt()

                if (i == 60) {
                    _droneStatus.value = "Arriving"
                    sendSystemPushNotification(
                        title = "DRONE 1 MINUTE AWAY!",
                        message = "Your high-speed cargo drone for Order #$orderId is 1 minute away from landing. Clear the landing zone!"
                    )
                    _oneMinuteNotificationAlert.value = "Cargo Drone for Order #$orderId is 1 minute away from landing! Check telemetry."
                    showToast("ALERT: Drone is 1-minute away from your doorstep!", ToastType.WARNING)
                }

                when (i) {
                    15 -> {
                        _droneStatus.value = "Drone dispatched"
                        showToast("Drone disengaging launcher pad!", ToastType.INFO)
                    }
                    40 -> {
                        _droneStatus.value = "In transit"
                        showToast("Drone is cruising in stratosphere at 60 km/h.", ToastType.INFO)
                    }
                    70 -> {
                        _droneStatus.value = "Arriving"
                        showToast("Drone is descending into your airspace!", ToastType.WARNING)
                    }
                }

                _droneGps.value = DroneGpsService.trackDroneGps(
                    progress = _droneProgress.value,
                    status = _droneStatus.value,
                    orderId = orderId
                )
            }
            // Transition to persistent processing state until admin confirms landing/delivery
            _droneStatus.value = "Awaiting Verification"
            _deliverySecondsRemaining.value = 0
            
            // Trigger Arrived notifications
            sendSystemPushNotification(
                title = "DRONE ARRIVED!",
                message = "Cargo Drone for Order #$orderId has landed at your doorstep. Enter secure lock OTP to claim cargo."
            )
            _adminNotificationAlert.value = "ORDER ARRIVED! Order #$orderId has arrived at customer doorstep. Secure Lock active. Awaiting OTP handshake."
            showToast("Approaching delivery site. Awaiting user OTP secure handshake...", ToastType.WARNING)
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
