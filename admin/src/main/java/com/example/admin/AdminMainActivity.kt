package com.example.admin

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Models for Admin Console
data class Order(
    val id: Int,
    val timestamp: Long,
    val totalAmount: Double,
    val itemsJson: String,
    val deliveryAddress: String,
    val status: String
)

data class Food(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val category: String
)

class AdminMainActivity : ComponentActivity() {

    private val orderUri = Uri.parse("content://com.example.skybite.provider/order_history")
    private val foodUri = Uri.parse("content://com.example.skybite.provider/food_items")

    private var orderObserver: ContentObserver? = null
    private var foodObserver: ContentObserver? = null

    // State for live lists
    private val ordersState = mutableStateListOf<Order>()
    private val foodsState = mutableStateListOf<Food>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initial loads
        refreshOrders()
        refreshFoodItems()

        // Register ContentObservers for real-time reactivity when the user places an order!
        orderObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshOrders()
            }
        }
        contentResolver.registerContentObserver(orderUri, true, orderObserver!!)

        foodObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshFoodItems()
            }
        }
        contentResolver.registerContentObserver(foodUri, true, foodObserver!!)

        setContent {
            AdminAppContent(
                orders = ordersState,
                foods = foodsState,
                onCompleteOrder = { order -> completeOrderAndSendBroadcast(order) },
                onAddFoodItem = { name, price, desc, category, imageUrl -> 
                    addFoodItem(name, price, desc, category, imageUrl)
                },
                onDeleteFoodItem = { foodId -> deleteFoodItem(foodId) }
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

    private fun refreshFoodItems() {
        try {
            val list = mutableListOf<Food>()
            val cursor = contentResolver.query(foodUri, null, null, null, "id DESC")
            cursor?.use { c ->
                val idIdx = c.getColumnIndex("id")
                val nameIdx = c.getColumnIndex("name")
                val priceIdx = c.getColumnIndex("price")
                val descIdx = c.getColumnIndex("description")
                val imgIdx = c.getColumnIndex("imageUrl")
                val catIdx = c.getColumnIndex("category")

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getInt(idIdx) else 0
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                    val price = if (priceIdx >= 0) c.getDouble(priceIdx) else 0.0
                    val desc = if (descIdx >= 0) c.getString(descIdx) ?: "" else ""
                    val img = if (imgIdx >= 0) c.getString(imgIdx) ?: "" else ""
                    val cat = if (catIdx >= 0) c.getString(catIdx) ?: "" else ""
                    list.add(Food(id, name, price, desc, img, cat))
                }
            }
            foodsState.clear()
            foodsState.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addFoodItem(name: String, price: Double, desc: String, category: String, imageUrl: String) {
        if (name.isBlank() || desc.isBlank() || category.isBlank()) {
            Toast.makeText(this, "Please enter all food details", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val values = ContentValues().apply {
                put("name", name)
                put("price", price)
                put("description", desc)
                put("imageUrl", imageUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60" })
                put("category", category)
                put("isTrending", 1)
                put("isPopular", 1)
                put("isAiRecommended", 0)
                put("rating", 4.8)
                put("deliveryTimeMin", 20)
            }
            val resultUri = contentResolver.insert(foodUri, values)
            if (resultUri != null) {
                Toast.makeText(this, "🚀 Item Launched to SkyBite Hangar!", Toast.LENGTH_LONG).show()
                refreshFoodItems()
            } else {
                Toast.makeText(this, "Failed to insert item", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error inserting item: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun deleteFoodItem(foodId: Int) {
        try {
            val rows = contentResolver.delete(foodUri, "id = ?", arrayOf(foodId.toString()))
            if (rows > 0) {
                Toast.makeText(this, "Removed from flight hangar.", Toast.LENGTH_SHORT).show()
                refreshFoodItems()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeOrderAndSendBroadcast(order: Order) {
        try {
            // 1. Update the order status in User App's ContentProvider
            val values = ContentValues().apply {
                put("status", "Delivered")
            }
            val updated = contentResolver.update(
                orderUri, 
                values, 
                "id = ?", 
                arrayOf(order.id.toString())
            )

            if (updated > 0) {
                // 2. Broadcast a system-wide delivery intent to the User app so it triggers an immediate toast!
                val intent = Intent("com.example.skybite.ORDER_DELIVERED").apply {
                    putExtra("order_id", order.id)
                    // Ensure the intent is exported and reachable
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                sendBroadcast(intent)
                
                Toast.makeText(this, "🛸 Drone delivery signaled! Broadcast sent.", Toast.LENGTH_LONG).show()
                refreshOrders()
            } else {
                Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error completing order: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orderObserver?.let { contentResolver.unregisterContentObserver(it) }
        foodObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}

// Visual layout constants matching Cyberpunk/Futuristic dark theme
val SkyDarkBackground = Color(0xFF0A0E1A)
val SkyDarkSurface = Color(0xFF131A2E)
val SkyDarkAccent = Color(0xFF00D4AA)
val SkyLightAccent = Color(0xFF00A485)
val SkyDarkTextPrimary = Color(0xFFF3F4F6)
val SkyDarkTextSecondary = Color(0xFF9CA3AF)

@Composable
fun AdminAppContent(
    orders: List<Order>,
    foods: List<Food>,
    onCompleteOrder: (Order) -> Unit,
    onAddFoodItem: (name: String, price: Double, desc: String, category: String, imageUrl: String) -> Unit,
    onDeleteFoodItem: (Int) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Active Deliveries, 1 = Food Launchpad, 2 = Hangar Telemetry

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SkyDarkSurface)
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Admin",
                                tint = SkyDarkAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SKYBITE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = SkyDarkTextPrimary
                            )
                        }
                        Text(
                            "QUANTUM CONTROL COMMAND",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SkyDarkAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    // Status pill
                    Surface(
                        color = Color(0x2000D4AA),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, SkyDarkAccent, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SkyDarkAccent, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkyDarkAccent
                            )
                        }
                    }
                }
            }

            // Tab switchers
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = SkyDarkSurface,
                contentColor = SkyDarkAccent
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DRONE CONTROL (${orders.count { it.status != "Delivered" }})")
                        }
                    },
                    selectedContentColor = SkyDarkAccent,
                    unselectedContentColor = SkyDarkTextSecondary
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LAUNCHPAD")
                        }
                    },
                    selectedContentColor = SkyDarkAccent,
                    unselectedContentColor = SkyDarkTextSecondary
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("METRICS")
                        }
                    },
                    selectedContentColor = SkyDarkAccent,
                    unselectedContentColor = SkyDarkTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "tab_transitions"
                ) { target ->
                    when (target) {
                        0 -> ActiveOrdersConsole(orders = orders, onCompleteOrder = onCompleteOrder)
                        1 -> FoodLaunchpad(foods = foods, onAddFoodItem = onAddFoodItem, onDeleteFood = onDeleteFoodItem)
                        2 -> QuantumMetricsDashboard(orders = orders, foods = foods)
                    }
                }
            }
        }
    }
}

@Composable
fun QuantumMetricsDashboard(
    orders: List<Order>,
    foods: List<Food>
) {
    var useSimulatedData by remember { mutableStateOf(orders.isEmpty()) }
    
    // Define the simulated orders and foods in case orders list is empty or requested
    val simulatedOrders = remember {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        listOf(
            Order(9001, now - 4 * dayMs, 1480.0, "1:2,2:1", "Gate Alpha Sector 9", "Delivered"),
            Order(9002, now - 3 * dayMs, 2240.0, "2:3,3:2", "Dock 14 Sky Station", "Delivered"),
            Order(9003, now - 2 * dayMs, 890.0, "1:1,4:1", "Hangar Beta Dome 3", "Delivered"),
            Order(9004, now - 1 * dayMs, 3120.0, "3:4,1:2", "Quadrant Delta Sector 2", "Delivered"),
            Order(9005, now, 1850.0, "2:2,4:2", "Neo Delhi Sky Platform", "Pending")
        )
    }

    val simulatedFoods = remember {
        listOf(
            Food(1, "Premium Mutton Biryani", 349.0, "Cooked to perfection", "", "Biryani"),
            Food(2, "Cheesy Quantum Pizza", 299.0, "Delicious cheese burst", "", "Pizza"),
            Food(3, "Neo Galactic Burger", 199.0, "Crispy patty burger", "", "Burgers"),
            Food(4, "Schezwan Noodles", 249.0, "Spicy schezwan flavor", "", "Chinese")
        )
    }

    val activeOrders = if (useSimulatedData) simulatedOrders else orders
    val activeFoods = if (useSimulatedData) simulatedFoods else foods

    // Grouping orders by day
    val sdf = remember { java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()) }
    val dayMap = remember(activeOrders) {
        activeOrders.groupBy { sdf.format(java.util.Date(it.timestamp)) }
            .mapValues { (_, dayOrders) ->
                dayOrders.size to dayOrders.sumOf { it.totalAmount }
            }
            .toList()
            .sortedBy { (day, _) ->
                activeOrders.find { sdf.format(java.util.Date(it.timestamp)) == day }?.timestamp ?: 0L
            }
    }

    val days = dayMap.map { it.first }
    val orderCounts = dayMap.map { it.second.first.toFloat() }
    val revenues = dayMap.map { it.second.second.toFloat() }

    // Toggle for Vol vs Revenue
    var chartMode by remember { mutableStateOf(0) } // 0 = Volume, 1 = Revenue

    // Calculate top selling food items
    val salesMap = remember(activeOrders) {
        val map = mutableMapOf<Int, Int>()
        activeOrders.forEach { order ->
            if (order.itemsJson.isNotBlank()) {
                order.itemsJson.split(",").forEach { item ->
                    val parts = item.split(":")
                    if (parts.size == 2) {
                        val foodId = parts[0].toIntOrNull()
                        val qty = parts[1].toIntOrNull()
                        if (foodId != null && qty != null) {
                            map[foodId] = (map[foodId] ?: 0) + qty
                        }
                    }
                }
            }
        }
        map
    }

    val topFoodItems = remember(salesMap, activeFoods) {
        salesMap.toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (foodId, qty) ->
                val food = activeFoods.find { it.id == foodId } ?: Food(
                    id = foodId,
                    name = "Quantum Item #$foodId",
                    price = 199.0,
                    description = "",
                    imageUrl = "",
                    category = "In-Flight"
                )
                food to qty
            }
    }

    val maxSold = remember(topFoodItems) {
        topFoodItems.maxOfOrNull { it.second } ?: 1
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Telemetry warning/simulation card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (useSimulatedData) Color(0x15FF6B35) else Color(0x1500D4AA)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (useSimulatedData) Color(0xFFFF6B35).copy(alpha = 0.3f) else SkyDarkAccent.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (useSimulatedData) "⚠️ SIMULATION TELEMETRY" else "📡 LIVE QUANTUM FEEDS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (useSimulatedData) Color(0xFFFF6B35) else SkyDarkAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (useSimulatedData) {
                                "Displaying simulated historical flight metrics. Switch feed type to use live database telemetry if available."
                            } else {
                                "Displaying real-time transactional database telemetry gathered from active delivery drones."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyDarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = !useSimulatedData,
                        onCheckedChange = { checked ->
                            if (!checked || orders.isNotEmpty()) {
                                useSimulatedData = !checked
                            } else {
                                useSimulatedData = true
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SkyDarkAccent,
                            checkedTrackColor = SkyDarkAccent.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color(0xFFFF6B35),
                            uncheckedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f)
                        ),
                        enabled = orders.isNotEmpty()
                    )
                }
            }
        }

        // Summary Cards Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalRev = activeOrders.sumOf { it.totalAmount }
                    SummaryStatCard(
                        title = "TOTAL REVENUE",
                        value = "₹${"%,.0f".format(totalRev)}",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = SkyDarkAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "TOTAL MISSIONS",
                        value = activeOrders.size.toString(),
                        icon = Icons.Default.QueryStats,
                        accentColor = Color(0xFF00BFFF),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val completed = activeOrders.count { it.status == "Delivered" }
                    val itemsSoldCount = salesMap.values.sum()
                    SummaryStatCard(
                        title = "DRONES RETURNED",
                        value = completed.toString(),
                        icon = Icons.Default.FlightLand,
                        accentColor = Color(0xFFADFF2F),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "ITEMS DISPATCHED",
                        value = itemsSoldCount.toString(),
                        icon = Icons.Default.LocalShipping,
                        accentColor = Color(0xFFFF6B35),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Chart 1: Daily Order Volume / Revenue
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY HANGAR TELEMETRY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SkyDarkTextPrimary
                            )
                            Text(
                                text = "Volume trend over the last 5 days",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyDarkTextSecondary
                            )
                        }

                        // Toggle Group for volume vs revenue
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x10FFFFFF))
                        ) {
                            Text(
                                text = "VOL",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (chartMode == 0) SkyDarkAccent else Color.Transparent)
                                    .clickable { chartMode = 0 }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (chartMode == 0) SkyDarkBackground else SkyDarkTextSecondary
                            )
                            Text(
                                text = "REV",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (chartMode == 1) SkyDarkAccent else Color.Transparent)
                                    .clickable { chartMode = 1 }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (chartMode == 1) SkyDarkBackground else SkyDarkTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (days.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Awaiting data packets...", color = SkyDarkTextSecondary)
                        }
                    } else {
                        val activePoints = if (chartMode == 0) orderCounts else revenues
                        val accentColor = if (chartMode == 0) Color(0xFF00D4AA) else Color(0xFFFF6B35)

                        LineAreaChart(
                            points = activePoints,
                            labels = days,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            lineColor = accentColor,
                            areaColor = accentColor.copy(alpha = 0.15f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // X Axis label row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEachIndexed { index, day ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyDarkTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (chartMode == 0) "${activePoints[index].toInt()} msg" else "₹${"%.0f".format(activePoints[index])}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chart 2: Top Selling Food Items
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOP DISPATCHED QUANTUMS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkyDarkTextPrimary
                    )
                    Text(
                        text = "Most requested foods delivered by SkyBite Jets",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyDarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (topFoodItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No dispatched units found.", color = SkyDarkTextSecondary)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topFoodItems.forEach { (food, qty) ->
                                TopFoodItemBar(
                                    foodName = food.name,
                                    category = food.category,
                                    quantitySold = qty,
                                    maxSold = maxSold,
                                    price = food.price,
                                    imageUrl = food.imageUrl
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyDarkTextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = SkyDarkTextPrimary
                )
            }
        }
    }
}

@Composable
fun LineAreaChart(
    points: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = SkyDarkAccent,
    areaColor: Color = SkyDarkAccent.copy(alpha = 0.2f),
    gridColor: Color = Color.White.copy(alpha = 0.05f)
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (points.isEmpty()) return@Canvas

        val maxVal = points.maxOrNull() ?: 1f
        val minVal = 0f
        val valRange = maxVal - minVal
        val yFactor = if (valRange == 0f) 1f else height / valRange
        val xFactor = if (points.size > 1) width / (points.size - 1) else width

        // Draw horizontal gridlines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = height * i / gridCount
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Build path for Area
        val areaPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            points.forEachIndexed { index, value ->
                val x = index * xFactor
                val y = height - (value * yFactor)
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }

        // Build path for Line
        val linePath = androidx.compose.ui.graphics.Path().apply {
            points.forEachIndexed { index, value ->
                val x = index * xFactor
                val y = height - (value * yFactor)
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        // Draw gradient area fill
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw Line stroke
        drawPath(
            path = linePath,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Draw circles on points
        points.forEachIndexed { index, value ->
            val x = index * xFactor
            val y = height - (value * yFactor)
            drawCircle(
                color = SkyDarkBackground,
                radius = 5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
fun TopFoodItemBar(
    foodName: String,
    category: String,
    quantitySold: Int,
    maxSold: Int,
    price: Double,
    imageUrl: String
) {
    val progress = if (maxSold > 0) quantitySold.toFloat() / maxSold else 0f
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyDarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        AsyncImage(
            model = imageUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60" },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkyDarkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$quantitySold sold",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SkyDarkAccent
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            // Custom cyber loading bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x15FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00D4AA), Color(0xFF00A485))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyDarkTextSecondary
                )
                Text(
                    text = "₹${price}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyDarkTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActiveOrdersConsole(
    orders: List<Order>,
    onCompleteOrder: (Order) -> Unit
) {
    val activeOrders = orders.filter { it.status != "Delivered" }
    val completedOrders = orders.filter { it.status == "Delivered" }

    if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = SkyDarkTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No orders found in User Database.",
                    color = SkyDarkTextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Launch the user app and place an order to see it live here!",
                    color = SkyDarkTextSecondary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeOrders.isNotEmpty()) {
                item {
                    Text(
                        "IN-FLIGHT SYSTEM (ACTIVE ORDERS)",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B35),
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                items(activeOrders) { order ->
                    AdminOrderCard(order = order, isActive = true, onCompleteOrder = onCompleteOrder)
                }
            }

            if (completedOrders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DELIVERED DRONE RUNS",
                        fontWeight = FontWeight.ExtraBold,
                        color = SkyDarkAccent,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                items(completedOrders) { order ->
                    AdminOrderCard(order = order, isActive = false, onCompleteOrder = onCompleteOrder)
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    isActive: Boolean,
    onCompleteOrder: (Order) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isActive) Color(0xFFFF6B35).copy(alpha = 0.4f) else SkyDarkAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ORDER #${order.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SkyDarkTextPrimary
                    )
                    Text(
                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyDarkTextSecondary
                    )
                }

                Surface(
                    color = if (isActive) Color(0xFFFF6B35).copy(alpha = 0.15f) else SkyDarkAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(
                        1.dp, 
                        if (isActive) Color(0xFFFF6B35) else SkyDarkAccent, 
                        RoundedCornerShape(20.dp)
                    )
                ) {
                    Text(
                        order.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActive) Color(0xFFFF6B35) else SkyDarkAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0x10FFFFFF))

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = SkyDarkAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ITEMS: ${order.itemsJson}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkyDarkTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF6B35), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "COORDINATES: ${order.deliveryAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyDarkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AMOUNT RECEIVED: ₹${order.totalAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = SkyDarkAccent
                )

                if (isActive) {
                    Button(
                        onClick = { onCompleteOrder(order) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyDarkAccent,
                            contentColor = SkyDarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightLand, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGNAL DELIVERED", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodLaunchpad(
    foods: List<Food>,
    onAddFoodItem: (name: String, price: Double, desc: String, category: String, imageUrl: String) -> Unit,
    onDeleteFood: (Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Biryani") }
    var desc by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    val presetCategories = listOf(
        "Biryani", "Pizza", "Burgers", "Chinese", "Desserts", "Drinks", "Sushi", "Tacos"
    )

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop&q=60" to "Biryani Premium",
        "https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=500&auto=format&fit=crop&q=60" to "Pizza Delight",
        "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=60" to "Classic Burger",
        "https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500&auto=format&fit=crop&q=60" to "Noodles Schezwan"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "LAUNCH A NEW SKYBITE ITEM",
                fontWeight = FontWeight.ExtraBold,
                color = SkyDarkAccent,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SkyDarkAccent.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Food Item Name", color = SkyDarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SkyDarkTextPrimary,
                            unfocusedTextColor = SkyDarkTextPrimary,
                            focusedBorderColor = SkyDarkAccent,
                            unfocusedBorderColor = Color(0x309CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price (₹)", color = SkyDarkTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SkyDarkTextPrimary,
                                unfocusedTextColor = SkyDarkTextPrimary,
                                focusedBorderColor = SkyDarkAccent,
                                unfocusedBorderColor = Color(0x309CA3AF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Simple category selector row
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Category", style = MaterialTheme.typography.labelSmall, color = SkyDarkAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(presetCategories) { cat ->
                                    val isSelected = cat == category
                                    Surface(
                                        color = if (isSelected) SkyDarkAccent else Color(0x10FFFFFF),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .clickable { category = cat }
                                            .border(1.dp, if (isSelected) SkyDarkAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                    ) {
                                        Text(
                                            cat,
                                            color = if (isSelected) SkyDarkBackground else SkyDarkTextPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description", color = SkyDarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SkyDarkTextPrimary,
                            unfocusedTextColor = SkyDarkTextPrimary,
                            focusedBorderColor = SkyDarkAccent,
                            unfocusedBorderColor = Color(0x309CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL (Or click a preset below)", color = SkyDarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SkyDarkTextPrimary,
                            unfocusedTextColor = SkyDarkTextPrimary,
                            focusedBorderColor = SkyDarkAccent,
                            unfocusedBorderColor = Color(0x309CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Image presets
                    Column {
                        Text("Image presets", style = MaterialTheme.typography.labelSmall, color = SkyDarkTextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presetImages.forEach { (url, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x10FFFFFF))
                                        .clickable { imageUrl = url }
                                        .border(1.dp, if (imageUrl == url) SkyDarkAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label, 
                                        color = if (imageUrl == url) SkyDarkAccent else SkyDarkTextSecondary, 
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 199.0
                            onAddFoodItem(name, price, desc, category, imageUrl)
                            name = ""
                            priceStr = ""
                            desc = ""
                            imageUrl = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyDarkAccent,
                            contentColor = SkyDarkBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LAUNCH ITEM TO HANGAR", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "LAUNCHED HANGAR CATALOG",
                fontWeight = FontWeight.ExtraBold,
                color = SkyDarkAccent,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        if (foods.isEmpty()) {
            item {
                Text("Catalog empty or offline", color = SkyDarkTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(foods) { food ->
                HangarCatalogRow(food = food, onDelete = onDeleteFood)
            }
        }
    }
}

@Composable
fun HangarCatalogRow(
    food: Food,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SkyDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = food.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(food.name, fontWeight = FontWeight.Bold, color = SkyDarkTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x10FFFFFF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            food.category, 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyDarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("₹${food.price}", fontWeight = FontWeight.Black, color = SkyDarkAccent)
                }
            }

            IconButton(
                onClick = { onDelete(food.id) }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
            }
        }
    }
}
