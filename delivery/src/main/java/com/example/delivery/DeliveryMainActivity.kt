package com.example.delivery

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// High-tech Theme Colors
val DeliveryDarkBackground = Color(0xFF090D16)
val DeliveryDarkSurface = Color(0xFF131B2E)
val DeliveryDarkSurfaceVariant = Color(0xFF1E293B)
val DeliveryTealAccent = Color(0xFF10B981) // Emerald Green
val DeliveryNeonOrange = Color(0xFFF97316) // Thruster orange
val DeliveryTextPrimary = Color(0xFFF8FAFC)
val DeliveryTextSecondary = Color(0xFF94A3B8)

data class Order(
    val id: Int,
    val timestamp: Long,
    val totalAmount: Double,
    val itemsJson: String,
    val deliveryAddress: String,
    val status: String
)

data class UserAccount(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String
)

class DeliveryMainActivity : ComponentActivity() {

    private val orderUri = Uri.parse("content://com.example.skybite.provider/order_history")
    private val userUri = Uri.parse("content://com.example.skybite.provider/users")

    private var orderObserver: ContentObserver? = null
    private var userObserver: ContentObserver? = null

    // Live state lists
    private val ordersState = mutableStateListOf<Order>()
    private val usersState = mutableStateListOf<UserAccount>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        refreshOrders()
        refreshUsers()

        // Register ContentObservers for real-time reactivity
        orderObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshOrders()
            }
        }
        contentResolver.registerContentObserver(orderUri, true, orderObserver!!)

        userObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshUsers()
            }
        }
        contentResolver.registerContentObserver(userUri, true, userObserver!!)

        setContent {
            DeliveryAppContent(
                orders = ordersState,
                users = usersState,
                onUpdateOrderStatus = { orderId, newStatus ->
                    updateOrderStatus(orderId, newStatus)
                },
                onRefresh = {
                    refreshOrders()
                    refreshUsers()
                }
            )
        }
    }

    private fun refreshOrders() {
        try {
            val list = mutableListOf<Order>()
            val cursor = contentResolver.query(orderUri, null, null, null, "timestamp DESC")
            cursor?.use { c ->
                val idIdx = c.getColumnIndex("id")
                val timestampIdx = c.getColumnIndex("timestamp")
                val totalIdx = c.getColumnIndex("totalAmount")
                val itemsIdx = c.getColumnIndex("itemsJson")
                val addressIdx = c.getColumnIndex("deliveryAddress")
                val statusIdx = c.getColumnIndex("status")

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getInt(idIdx) else 0
                    val timestamp = if (timestampIdx >= 0) c.getLong(timestampIdx) else 0L
                    val total = if (totalIdx >= 0) c.getDouble(totalIdx) else 0.0
                    val items = if (itemsIdx >= 0) c.getString(itemsIdx) ?: "" else ""
                    val address = if (addressIdx >= 0) c.getString(addressIdx) ?: "" else ""
                    val status = if (statusIdx >= 0) c.getString(statusIdx) ?: "" else ""
                    list.add(Order(id, timestamp, total, items, address, status))
                }
            }
            ordersState.clear()
            ordersState.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshUsers() {
        try {
            val list = mutableListOf<UserAccount>()
            val cursor = contentResolver.query(userUri, null, null, null, "id DESC")
            cursor?.use { c ->
                val idIdx = c.getColumnIndex("id")
                val nameIdx = c.getColumnIndex("name")
                val emailIdx = c.getColumnIndex("email")
                val phoneIdx = c.getColumnIndex("phone")

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getInt(idIdx) else 0
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "Anonymous" else "Anonymous"
                    val email = if (emailIdx >= 0) c.getString(emailIdx) ?: "" else ""
                    val phone = if (phoneIdx >= 0) c.getString(phoneIdx) ?: "" else ""
                    list.add(UserAccount(id, name, email, phone))
                }
            }
            usersState.clear()
            usersState.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOrderStatus(orderId: Int, status: String) {
        try {
            val values = ContentValues().apply {
                put("status", status)
            }
            val rows = contentResolver.update(orderUri, values, "id = ?", arrayOf(orderId.toString()))
            if (rows > 0) {
                if (status == "Delivered") {
                    val intent = Intent("com.example.skybite.ORDER_DELIVERED").apply {
                        putExtra("order_id", orderId)
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    sendBroadcast(intent)
                }
                Toast.makeText(this, "Order #$orderId updated to $status", Toast.LENGTH_SHORT).show()
                refreshOrders()
            } else {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orderObserver?.let { contentResolver.unregisterContentObserver(it) }
        userObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}

@Composable
fun DeliveryAppContent(
    orders: List<Order>,
    users: List<UserAccount>,
    onUpdateOrderStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Flight Jobs, 1 = User Accounts, 2 = Pilot Radar
    var selectedOrderForPilot by remember { mutableStateOf<Order?>(null) }

    // If an order gets updated externally, keep selectedOrderForPilot in sync
    LaunchedEffect(orders) {
        selectedOrderForPilot?.let { current ->
            selectedOrderForPilot = orders.find { it.id == current.id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliveryDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeliveryDarkSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = DeliveryTealAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SKYBITE PILOT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = DeliveryTextPrimary
                            )
                        }
                        Text(
                            "FLIGHT DISPATCH & USER BACKEND",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeliveryNeonOrange,
                            letterSpacing = 1.sp
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DeliveryTealAccent
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeliveryDarkSurface,
                contentColor = DeliveryTealAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FLIGHT JOBS (${orders.count { it.status != "Delivered" }})")
                        }
                    },
                    selectedContentColor = DeliveryTealAccent,
                    unselectedContentColor = DeliveryTextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("USERS DB")
                        }
                    },
                    selectedContentColor = DeliveryTealAccent,
                    unselectedContentColor = DeliveryTextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PILOT CONSOLE")
                        }
                    },
                    selectedContentColor = DeliveryTealAccent,
                    unselectedContentColor = DeliveryTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "tab_navigation"
                ) { target ->
                    when (target) {
                        0 -> FlightJobsScreen(
                            orders = orders,
                            onUpdateStatus = onUpdateOrderStatus,
                            onSelectForPilot = {
                                selectedOrderForPilot = it
                                selectedTab = 2 // Jump to Pilot Console
                            }
                        )
                        1 -> UserAccountsScreen(users = users)
                        2 -> PilotConsoleScreen(
                            selectedOrder = selectedOrderForPilot,
                            onUpdateStatus = onUpdateOrderStatus,
                            onClearSelection = { selectedOrderForPilot = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlightJobsScreen(
    orders: List<Order>,
    onUpdateStatus: (Int, String) -> Unit,
    onSelectForPilot: (Order) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AirplanemodeInactive,
                    contentDescription = null,
                    tint = DeliveryTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No pending flight shipments found",
                    color = DeliveryTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (order.status != "Delivered") DeliveryTealAccent.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SHIPMENT ID: #FLG-${order.id}",
                                    fontWeight = FontWeight.Black,
                                    color = DeliveryTextPrimary,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(order.timestamp))
                                Text(
                                    text = "Launched: $date",
                                    color = DeliveryTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Status Chip
                            Surface(
                                color = when (order.status) {
                                    "Delivered" -> DeliveryTealAccent.copy(alpha = 0.2f)
                                    "In Transit" -> DeliveryNeonOrange.copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = when (order.status) {
                                        "Delivered" -> DeliveryTealAccent
                                        "In Transit" -> DeliveryNeonOrange
                                        else -> Color.Gray
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = when (order.status) {
                                        "Delivered" -> DeliveryTealAccent
                                        "In Transit" -> DeliveryNeonOrange
                                        else -> Color.White
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = DeliveryTextSecondary.copy(alpha = 0.2f))

                        Text(
                            text = "Destination Address:",
                            fontWeight = FontWeight.Bold,
                            color = DeliveryTealAccent,
                            fontSize = 11.sp
                        )
                        Text(
                            text = order.deliveryAddress,
                            color = DeliveryTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Cargo Inventory:",
                            fontWeight = FontWeight.Bold,
                            color = DeliveryTealAccent,
                            fontSize = 11.sp
                        )
                        Text(
                            text = order.itemsJson,
                            color = DeliveryTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (order.status != "Delivered") {
                                Button(
                                    onClick = { onSelectForPilot(order) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeliveryNeonOrange),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Flight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PILOT SHIPMENT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (order.status == "Preparing") {
                                Button(
                                    onClick = { onUpdateStatus(order.id, "In Transit") },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeliveryTealAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("DISPATCH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else if (order.status == "In Transit") {
                                Button(
                                    onClick = { onUpdateStatus(order.id, "Delivered") },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeliveryTealAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("SAFE DROP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserAccountsScreen(users: List<UserAccount>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BACK-END USER REGISTRY",
                    fontWeight = FontWeight.Black,
                    color = DeliveryTextPrimary,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Total Registered Accounts in Local Room DB",
                    color = DeliveryTextSecondary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DeliveryTealAccent.copy(alpha = 0.15f), CircleShape)
            ) {
                Text(
                    text = users.size.toString(),
                    color = DeliveryTealAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = DeliveryTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No user profiles found in backend", color = DeliveryTextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(DeliveryDarkSurfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = DeliveryTealAccent,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = DeliveryTextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(12.dp), tint = DeliveryTextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = user.email, color = DeliveryTextSecondary, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = DeliveryTextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = user.phone.ifBlank { "No Contact Phone" }, color = DeliveryTextSecondary, fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "UID: ${user.id}",
                                color = DeliveryNeonOrange,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PilotConsoleScreen(
    selectedOrder: Order?,
    onUpdateStatus: (Int, String) -> Unit,
    onClearSelection: () -> Unit
) {
    if (selectedOrder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = null,
                    tint = DeliveryTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Drone Selected",
                    color = DeliveryTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please head to the Flight Jobs tab and select a drone cargo shipment to pilot.",
                    color = DeliveryTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        var flightAltitude by remember { mutableFloatStateOf(120f) }
        var thrusterPower by remember { mutableFloatStateOf(80f) }
        var mockBattery by remember { mutableIntStateOf(92) }

        // Decrement simulated battery slowly
        LaunchedEffect(selectedOrder) {
            while (true) {
                kotlinx.coroutines.delay(8000)
                if (mockBattery > 15) mockBattery -= 1
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Order details header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "CO-PILOT LINK ESTABLISHED",
                                    fontWeight = FontWeight.Black,
                                    color = DeliveryTealAccent,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "Drone ID: #DRN-909-${selectedOrder.id}",
                                    fontWeight = FontWeight.Bold,
                                    color = DeliveryTextPrimary,
                                    fontSize = 16.sp
                                )
                            }

                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Deselect", tint = DeliveryTextSecondary)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = DeliveryTextSecondary.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CARGO DESTINATION", color = DeliveryTextSecondary, fontSize = 10.sp)
                                Text(
                                    selectedOrder.deliveryAddress,
                                    color = DeliveryTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(200.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("TELEMETRY STATUS", color = DeliveryTextSecondary, fontSize = 10.sp)
                                Text(
                                    selectedOrder.status.uppercase(),
                                    color = if (selectedOrder.status == "Delivered") DeliveryTealAccent else DeliveryNeonOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Hangar Map / Radar
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "LIVE TACTICAL RADAR SENSORS",
                            fontWeight = FontWeight.Black,
                            color = DeliveryNeonOrange,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeliveryDarkBackground)
                                .border(1.dp, DeliveryTealAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val radarLinesColor = DeliveryTealAccent.copy(alpha = 0.15f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                drawCircle(
                                    color = radarLinesColor,
                                    radius = 70.dp.toPx(),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(
                                    color = radarLinesColor,
                                    radius = 45.dp.toPx(),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(
                                    color = radarLinesColor,
                                    radius = 20.dp.toPx(),
                                    style = Stroke(width = 1.dp.toPx())
                                )

                                // Crosshairs
                                drawLine(
                                    color = radarLinesColor,
                                    start = Offset(0f, center.y),
                                    end = Offset(size.width, center.y)
                                )
                                drawLine(
                                    color = radarLinesColor,
                                    start = Offset(center.x, 0f),
                                    end = Offset(center.x, size.height)
                                )

                                // Draw Restaurant Hangar Point (Bangalore Center)
                                drawCircle(
                                    color = DeliveryNeonOrange,
                                    radius = 6.dp.toPx(),
                                    center = Offset(center.x - 30.dp.toPx(), center.y + 20.dp.toPx())
                                )

                                // Draw Customer Delivery Point
                                drawCircle(
                                    color = DeliveryTealAccent,
                                    radius = 6.dp.toPx(),
                                    center = Offset(center.x + 40.dp.toPx(), center.y - 30.dp.toPx())
                                )

                                // Route dotted line
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    start = Offset(center.x - 30.dp.toPx(), center.y + 20.dp.toPx()),
                                    end = Offset(center.x + 40.dp.toPx(), center.y - 30.dp.toPx()),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                // Drone Active Node (advancing path)
                                val droneProgressVal = when (selectedOrder.status) {
                                    "Preparing" -> 0.0f
                                    "In Transit" -> 0.55f
                                    else -> 1.0f
                                }
                                val droneX = (center.x - 30.dp.toPx()) + ((center.x + 40.dp.toPx()) - (center.x - 30.dp.toPx())) * droneProgressVal
                                val droneY = (center.y + 20.dp.toPx()) + ((center.y - 30.dp.toPx()) - (center.y + 20.dp.toPx())) * droneProgressVal

                                drawCircle(
                                    color = Color.White,
                                    radius = 4.dp.toPx(),
                                    center = Offset(droneX, droneY)
                                )

                                drawCircle(
                                    color = if (selectedOrder.status == "In Transit") DeliveryNeonOrange else DeliveryTealAccent,
                                    radius = 12.dp.toPx(),
                                    center = Offset(droneX, droneY),
                                    style = Stroke(width = 1.5f.dp.toPx())
                                )
                            }

                            // Overlay Status Indicators
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LAT: 12.9716", color = DeliveryTextSecondary, fontSize = 9.sp)
                                Text("LNG: 77.5946", color = DeliveryTextSecondary, fontSize = 9.sp)
                                Text("DGPS LOCK: 14 SATS", color = DeliveryTealAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "DRONE FLIGHT COMMANDS",
                            fontWeight = FontWeight.Black,
                            color = DeliveryTealAccent,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onUpdateStatus(selectedOrder.id, "Preparing") },
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = DeliveryDarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("PREPARING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeliveryTextPrimary)
                            }

                            Button(
                                onClick = { onUpdateStatus(selectedOrder.id, "In Transit") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DeliveryNeonOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LAUNCH IN-TRANSIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onUpdateStatus(selectedOrder.id, "Delivered") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DeliveryTealAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DROP DELIVERED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Live sliders
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "FLIGHT INSTRUMENTS",
                            fontWeight = FontWeight.Black,
                            color = DeliveryTextPrimary,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Altitude Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ALTITUDE", color = DeliveryTextSecondary, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                            Slider(
                                value = flightAltitude,
                                onValueChange = { flightAltitude = it },
                                valueRange = 0f..250f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = DeliveryTealAccent,
                                    activeTrackColor = DeliveryTealAccent
                                )
                            )
                            Text("${flightAltitude.toInt()} M", color = DeliveryTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(45.dp))
                        }

                        // Thruster Power
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("THRUST", color = DeliveryTextSecondary, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                            Slider(
                                value = thrusterPower,
                                onValueChange = { thrusterPower = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = DeliveryNeonOrange,
                                    activeTrackColor = DeliveryNeonOrange
                                )
                            )
                            Text("${thrusterPower.toInt()}%", color = DeliveryTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(45.dp))
                        }
                    }
                }
            }

            // Realtime Stats HUD
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BATTERY CELLS", color = DeliveryTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$mockBattery%", color = if (mockBattery < 25) Color.Red else DeliveryTealAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DRONE AIRSPEED", color = DeliveryTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            val computedSpeed = (thrusterPower * 0.85).toInt()
                            Text("$computedSpeed KM/H", color = DeliveryTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DeliveryDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISH STATUS", color = DeliveryTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (thrusterPower > 10) "ACTIVE" else "STANDBY", color = DeliveryTealAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
