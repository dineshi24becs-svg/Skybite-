package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.SkyBiteTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private var deliveryReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic BroadcastReceiver for Admin App notifications
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.skybite.ORDER_DELIVERED") {
                    val orderId = intent.getIntExtra("order_id", -1)
                    viewModel.showToast(
                        "SkyBite Quantum Jet: Order #$orderId has been delivered successfully by Drone!",
                        com.example.ui.viewmodel.ToastType.SUCCESS
                    )
                }
            }
        }
        deliveryReceiver = receiver
        val filter = IntentFilter("com.example.skybite.ORDER_DELIVERED")
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val cartDisplayItems by viewModel.cartDisplayItems.collectAsState()
            val toasts by viewModel.toasts.collectAsState()

            // Overlays and dialogs
            val selectedFoodItem by viewModel.selectedFoodItem.collectAsState()
            val showOtpVerifyDialog by viewModel.showOtpVerifyDialog.collectAsState()
            val isListeningVoice by viewModel.isListeningVoice.collectAsState()
            val postDeliveryReviewFoodId by viewModel.showReviewFormForCompletedOrder.collectAsState()

            val cartCount = cartDisplayItems.sumOf { it.quantity }

            SkyBiteTheme(darkTheme = isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentScreen != Screen.SPLASH && 
                                currentScreen != Screen.LOGIN && 
                                currentScreen != Screen.SIGNUP && 
                                currentScreen != Screen.FORGOT_PASSWORD
                            ) {
                                AppHeader(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    isDarkMode = isDarkMode,
                                    cartItemCount = cartCount
                                )
                            }
                        },
                        bottomBar = {
                            if (currentScreen != Screen.SPLASH && 
                                currentScreen != Screen.LOGIN && 
                                currentScreen != Screen.SIGNUP && 
                                currentScreen != Screen.FORGOT_PASSWORD
                            ) {
                                FuturisticBottomNavigation(
                                    currentScreen = currentScreen,
                                    isDarkMode = isDarkMode,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "screen_transitions"
                            ) { target ->
                                when (target) {
                                    Screen.SPLASH -> SplashScreen(isDarkMode = isDarkMode)
                                    Screen.LOGIN -> LoginScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.SIGNUP -> SignUpScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.HOME -> HomeScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.CART -> CartScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.CHECKOUT -> CheckoutScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.TRACKING -> TrackingScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.PROFILE -> ProfileScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                    Screen.ORDER_HISTORY -> OrderHistoryScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                }
                            }
                        }
                    }

                    // Floating Toasts Notifications Overlay
                    ToastContainer(toasts = toasts)

                    // Modals and Overlays
                    selectedFoodItem?.let { item ->
                        FoodDetailsModal(item = item, viewModel = viewModel, isDarkMode = isDarkMode)
                    }

                    if (showOtpVerifyDialog) {
                        OtpDialog(viewModel = viewModel, isDarkMode = isDarkMode)
                    }

                    if (isListeningVoice) {
                        VoiceSearchOverlay(isDarkMode = isDarkMode)
                    }

                    postDeliveryReviewFoodId?.let { foodId ->
                        PostDeliveryReviewDialog(foodId = foodId, viewModel = viewModel, isDarkMode = isDarkMode)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deliveryReceiver?.let { unregisterReceiver(it) }
    }
}

@Composable
fun FuturisticBottomNavigation(
    currentScreen: Screen,
    isDarkMode: Boolean,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = if (isDarkMode) Color(0xFF111827).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
        tonalElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .glassmorphicBorder(isDarkMode, 40f)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.height(64.dp)
        ) {
            NavigationBarItem(
                selected = currentScreen == Screen.HOME,
                onClick = { onNavigate(Screen.HOME) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Menu", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    selectedTextColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag("nav_home_tab")
            )
            NavigationBarItem(
                selected = currentScreen == Screen.CART || currentScreen == Screen.CHECKOUT,
                onClick = { onNavigate(Screen.CART) },
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") },
                label = { Text("Hangar", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    selectedTextColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag("nav_cart_tab")
            )
            NavigationBarItem(
                selected = currentScreen == Screen.ORDER_HISTORY || currentScreen == Screen.TRACKING,
                onClick = { onNavigate(Screen.ORDER_HISTORY) },
                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                label = { Text("Logs", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    selectedTextColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag("nav_history_tab")
            )
            NavigationBarItem(
                selected = currentScreen == Screen.PROFILE,
                onClick = { onNavigate(Screen.PROFILE) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Pilot", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    selectedTextColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag("nav_profile_tab")
            )
        }
    }
}
