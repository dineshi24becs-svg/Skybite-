package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.ToastMessage
import com.example.ui.viewmodel.ToastType
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- Helper UI Components ---

// High-fidelity Glassmorphic Modifier
@Composable
fun Modifier.glassmorphicBorder(
    isDarkMode: Boolean,
    cornerRadius: Float = 48f
): Modifier {
    val borderColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    return this.drawBehind {
        drawRoundRect(
            color = borderColor,
            style = Stroke(width = 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
    }
}

// Glowing Accent Button
@Composable
fun SkyBiteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSecondary: Boolean = false,
    testTag: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )

    val colorScheme = MaterialTheme.colorScheme
    val buttonBg = if (isSecondary) {
        colorScheme.surfaceVariant
    } else {
        colorScheme.secondary
    }

    val glowColor = if (isSecondary) {
        Color.Transparent
    } else {
        colorScheme.primary.copy(alpha = 0.25f * glowIntensity)
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .shadowGlowing(glowColor, 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonBg,
            contentColor = if (isSecondary) colorScheme.onSurface else colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
    }
}

fun Modifier.shadowGlowing(color: Color, radius: androidx.compose.ui.unit.Dp) = this.drawBehind {
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx(), radius.toPx())
    )
}

// Visual HUD Badge
@Composable
fun HudBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Custom Coil Async Image
@Composable
fun SkyBiteFoodImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// --- Dynamic Toast System ---
@Composable
fun ToastContainer(toasts: List<ToastMessage>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toasts.forEach { toast ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    val cardBg = when (toast.type) {
                        ToastType.SUCCESS -> Color(0xFF0F5132)
                        ToastType.ERROR -> Color(0xFF842029)
                        ToastType.WARNING -> Color(0xFF664D03)
                        ToastType.INFO -> Color(0xFF084298)
                    }
                    val textColor = Color.White
                    val icon = when (toast.type) {
                        ToastType.SUCCESS -> Icons.Default.CheckCircle
                        ToastType.ERROR -> Icons.Default.Error
                        ToastType.WARNING -> Icons.Default.Warning
                        ToastType.INFO -> Icons.Default.Info
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 450.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = toast.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 1. Splash Screen ---
@Composable
fun SplashScreen(isDarkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "propellers"
    )

    val backgroundBrush = if (isDarkMode) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF1E2640), SkyDarkBackground),
            center = Offset(500f, 800f),
            radius = 1200f
        )
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-Tech Drone Animated Vector Logo (Canvas Drawing)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = floatAnim.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Outer structural scanner ring
                    drawCircle(
                        color = Color(0x3000D4AA),
                        radius = size.width / 2.5f,
                        style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f)))
                    )

                    // Drone core
                    drawCircle(
                        color = Color(0xFFFF6B35),
                        radius = size.width / 10f
                    )
                    drawCircle(
                        color = Color(0xFF00D4AA),
                        radius = size.width / 14f
                    )

                    // Carbon frame wings
                    val angles = listOf(45f, 135f, 225f, 315f)
                    angles.forEach { angle ->
                        val rad = Math.toRadians(angle.toDouble())
                        val endX = center.x + (size.width / 3.2f) * cos(rad).toFloat()
                        val endY = center.y + (size.width / 3.2f) * sin(rad).toFloat()
                        
                        drawLine(
                            color = Color(0xFF9CA3AF),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 10f
                        )

                        // Blade mounts
                        drawCircle(
                            color = Color(0xFF111827),
                            radius = 16f,
                            center = Offset(endX, endY)
                        )
                    }
                }

                // Rotating blade overlay
                val angles = listOf(45f, 135f, 225f, 315f)
                angles.forEach { angle ->
                    val rad = Math.toRadians(angle.toDouble())
                    val dX = 100 + 62.5f * cos(rad).toFloat()
                    val dY = 100 + 62.5f * sin(rad).toFloat()

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = (dX - 20).dp, y = (dY - 20).dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0x3000D4AA), radius = size.width / 2)
                            // Draw spinning bar
                            val bRad = Math.toRadians(rotateAnim.toDouble())
                            val len = size.width / 2
                            val bEndX = size.width / 2 + len * cos(bRad).toFloat()
                            val bEndY = size.height / 2 + len * sin(bRad).toFloat()
                            val bStartX = size.width / 2 - len * cos(bRad).toFloat()
                            val bStartY = size.height / 2 - len * sin(bRad).toFloat()

                            drawLine(
                                color = Color(0xFF00D4AA),
                                start = Offset(bStartX, bStartY),
                                end = Offset(bEndX, bEndY),
                                strokeWidth = 4f
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SKYBITE",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = if (isDarkMode) SkyDarkAccent else SkyLightAccent
            )

            Text(
                text = "DRONE · FAST · FRESH",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF00D4AA),
                strokeWidth = 4.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// --- 2. Login Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("User") } // "User" or "Admin"

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF111E36), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to SkyBite",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (selectedRole == "Admin") "Access Space Logistics Console" else "Login to dispatch your meal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                // Role Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .background(
                            color = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("User", "Admin").forEach { role ->
                        val isSelected = selectedRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(
                                    color = if (isSelected) {
                                        if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                    } else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRole = role }
                                .testTag("role_select_${role.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (role == "User") "CUSTOMER USER" else "SPACE ADMIN",
                                color = if (isSelected) Color.White else {
                                    if (isDarkMode) Color.Gray else Color.DarkGray
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (selectedRole == "Admin") "Admin Username / Email" else "Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (selectedRole == "Admin") "Admin Password" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedRole == "User") {
                    TextButton(
                        onClick = { viewModel.navigateTo(Screen.FORGOT_PASSWORD) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                SkyBiteButton(
                    text = if (selectedRole == "Admin") "ACCESS TERMINAL" else "INITIATE LAUNCH",
                    onClick = {
                        if (selectedRole == "Admin") {
                            viewModel.loginAdmin(email, password)
                        } else {
                            viewModel.loginUser(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = if (selectedRole == "Admin") Icons.Default.Settings else Icons.Default.RocketLaunch,
                    testTag = "login_button"
                )

                if (selectedRole == "User") {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New pilot candidate?",
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.navigateTo(Screen.SIGNUP) }) {
                            Text(
                                text = "Sign Up",
                                color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Use default: admin / admin",
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// --- 3. Sign Up Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("User") } // "User" or "Admin"
    var hotelName by remember { mutableStateOf("") }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF111E36), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enlist Flight Pilot",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Register with the orbital service network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                // Role Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .background(
                            color = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("User", "Admin").forEach { role ->
                        val isSelected = selectedRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(
                                    color = if (isSelected) {
                                        if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                    } else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRole = role }
                                .testTag("signup_role_select_${role.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (role == "User") "CUSTOMER USER" else "SPACE ADMIN",
                                color = if (isSelected) Color.White else {
                                    if (isDarkMode) Color.Gray else Color.DarkGray
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (selectedRole == "Admin") "Owner / Admin Name" else "Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_name_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (selectedRole == "Admin") "Admin Username / Email" else "Quantum Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Comms Frequency (Phone)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_phone_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                if (selectedRole == "Admin") {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = hotelName,
                        onValueChange = { hotelName = it },
                        label = { Text("Hotel / Restaurant Name") },
                        leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_hotel_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = sleekTextFieldColors(isDarkMode)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Access Code (Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SkyBiteButton(
                    text = "ESTABLISH CONNECT",
                    onClick = { viewModel.registerUser(name, email, phone, password, selectedRole, hotelName) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Engineering,
                    testTag = "signup_submit_button"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already enlisted?",
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { viewModel.navigateTo(Screen.LOGIN) }) {
                        Text(
                            text = "Login",
                            color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// --- OTP Verification Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpDialog(viewModel: AppViewModel, isDarkMode: Boolean) {
    var code by remember { mutableStateOf("") }
    val correctCode by viewModel.demoOtpCode.collectAsState()

    Dialog(
        onDismissRequest = { viewModel.closeOtpDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) SkyDarkSurface else Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OTP Verification Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter the 6-digit access OTP code transmitted to your terminal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Box displaying correct demo OTP for easy copy-paste/submission
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) SkyDarkOrange.copy(alpha = 0.15f) else SkyLightOrange.copy(alpha = 0.15f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DEMO OTP CODE: $correctCode",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("6-Digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 4.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeOtpDialog() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    SkyBiteButton(
                        text = "Verify",
                        onClick = { viewModel.verifyOtp(code) },
                        modifier = Modifier.weight(1f),
                        testTag = "otp_verify_btn"
                    )
                }
            }
        }
    }
}

// --- 4. Forgot Password Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    var email by remember { mutableStateOf("") }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF111E36), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Forgot Password",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 22.sp),
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enter your Username or Email to reset your security password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Username or Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SkyBiteButton(
                    text = "RESET PASSWORD",
                    onClick = { viewModel.forgotPassword(email) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.SettingsBackupRestore,
                    testTag = "forgot_submit_button"
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { viewModel.navigateTo(Screen.LOGIN) }) {
                    Text(
                        text = "Back to Login",
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// --- Custom Header Icon Button ---
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    isActive: Boolean = false,
    badgeCount: Int = 0
) {
    val bg = if (isActive) {
        if (isDarkMode) SkyDarkAccent else SkyLightAccent
    } else {
        if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    }
    val tint = if (isActive) {
        if (isDarkMode) SkyDarkBackground else Color.White
    } else {
        if (isDarkMode) SkyDarkAccent else SkyLightAccent
    }
    
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .glassmorphicBorder(isDarkMode, 24f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.dp)
                    .background(if (isDarkMode) SkyDarkOrange else SkyLightOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
        }
    }
}

// --- Sticky Custom Header ---
@Composable
fun AppHeader(
    viewModel: AppViewModel,
    currentUser: UserEntity?,
    isDarkMode: Boolean,
    cartItemCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = if (isDarkMode) SkyDarkBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.navigateTo(Screen.HOME) }
            ) {
                Icon(
                    Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SKYBITE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer(alpha = alphaAnim)
                                .background(if (isDarkMode) SkyDarkAccent else SkyLightAccent, CircleShape)
                        )
                        Text(
                            text = "DRONE · FAST · FRESH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Theme Toggle
                HeaderIconButton(
                    icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Light Mode",
                    onClick = { viewModel.toggleTheme() },
                    isDarkMode = isDarkMode
                )

                // Profile Button
                HeaderIconButton(
                    icon = Icons.Default.Person,
                    contentDescription = "Profile",
                    onClick = { 
                        if (currentUser != null) {
                            viewModel.navigateTo(Screen.PROFILE)
                        } else {
                            viewModel.navigateTo(Screen.LOGIN)
                        }
                    },
                    isDarkMode = isDarkMode
                )

                // History Button
                HeaderIconButton(
                    icon = Icons.Default.History,
                    contentDescription = "History",
                    onClick = { 
                        if (currentUser != null) {
                            viewModel.navigateTo(Screen.ORDER_HISTORY)
                        } else {
                            viewModel.navigateTo(Screen.LOGIN)
                        }
                    },
                    isDarkMode = isDarkMode
                )

                // Cart Icon with Badge (Active Style!)
                HeaderIconButton(
                    icon = Icons.Default.ShoppingCart,
                    contentDescription = "Hangar Cart",
                    onClick = { viewModel.navigateTo(Screen.CART) },
                    isDarkMode = isDarkMode,
                    isActive = cartItemCount > 0,
                    badgeCount = cartItemCount
                )
            }
        }
    }
}

// Reusable Sleek Text Field Colors
@Composable
fun sleekTextFieldColors(isDarkMode: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (isDarkMode) SkyDarkSurface else Color.White,
    unfocusedContainerColor = if (isDarkMode) SkyDarkSurface else Color.White,
    focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
    unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
    focusedLabelColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
    unfocusedLabelColor = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
    focusedTextColor = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
    unfocusedTextColor = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
)

// --- Voice Listening Dialog Overlay ---
@Composable
fun VoiceSearchOverlay(isDarkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(300.dp)
                    .height(250.dp)
                    .glassmorphicBorder(isDarkMode, 48f),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SECURE QUANTUM VOICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                if (isDarkMode) SkyDarkOrange.copy(alpha = 0.2f) else SkyLightOrange.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((50 * pulseAnim).dp)
                                .background(
                                    if (isDarkMode) SkyDarkOrange.copy(alpha = 0.4f) else SkyLightOrange.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "LISTENING TELEMETRY...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// --- 5. Home Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favoriteFoodIds by viewModel.favoriteFoodIds.collectAsState()
    val allFoodItems by viewModel.allFoodItemsFlow.collectAsState()

    // Filtered menu
    val filteredMenu = remember(allFoodItems, searchQuery, selectedCategory) {
        allFoodItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Popular items
    val popularItems = remember(allFoodItems) { allFoodItems.filter { it.isPopular } }
    // AI items
    val aiRecommended = remember(allFoodItems) { allFoodItems.filter { it.isAiRecommended } }
    // Trending items
    val trendingItems = remember(allFoodItems) { allFoodItems.filter { it.isTrending } }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp), // space for bottom padding
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Hero Banner Item
        item {
            HeroSection(viewModel, isDarkMode)
        }

        // Real-Time Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search molecular food...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.startVoiceSearch() }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_food_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )
            }
        }

        // Category Filter
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Orbit Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Slots") },
                            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                                selectedLabelColor = if (isDarkMode) SkyDarkBackground else Color.White
                            )
                        )
                    }

                    items(viewModel.allCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { viewModel.selectCategory(cat.name) },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                val vector = when (cat.iconName) {
                                    "restaurant" -> Icons.Default.Restaurant
                                    "local_pizza" -> Icons.Default.LocalPizza
                                    "lunch_dining" -> Icons.Default.LunchDining
                                    "ramen_dining" -> Icons.Default.Restaurant
                                    "cake" -> Icons.Default.Cake
                                    "local_bar" -> Icons.Default.LocalBar
                                    "rice_bowl" -> Icons.Default.LunchDining
                                    "eco" -> Icons.Default.Eco
                                    else -> Icons.Default.Fastfood
                                }
                                Icon(vector, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                                selectedLabelColor = if (isDarkMode) SkyDarkBackground else Color.White
                            )
                        )
                    }
                }
            }
        }

        // Carousel popular slider
        if (searchQuery.isBlank() && selectedCategory == null) {
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Popular Airspaces",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(popularItems) { item ->
                            PopularItemCard(item, viewModel, isDarkMode, favoriteFoodIds.contains(item.id))
                        }
                    }
                }
            }

            // AI Recommendations Row
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00D4AA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Recommended Flights",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(aiRecommended) { item ->
                            AIRecommendedCard(item, viewModel, isDarkMode)
                        }
                    }
                }
            }
        }

        // Main Food List Header
        item {
            Text(
                text = if (selectedCategory != null) "Hangar Slot: $selectedCategory" else "Dispatched Food Modules",
                style = MaterialTheme.typography.titleLarge,
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Food List (Responsive Column grid representation inside list using chunks of 2 for beautiful grid display)
        val chunkedItems = filteredMenu.chunked(2)
        items(chunkedItems) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        FoodGridCard(
                            item = item,
                            viewModel = viewModel,
                            isDarkMode = isDarkMode,
                            isFav = favoriteFoodIds.contains(item.id)
                        )
                    }
                }
                // if odd item in chunk
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (filteredMenu.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No flying units found matching coordinates",
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Hero banner structure with stats
@Composable
fun HeroSection(viewModel: AppViewModel, isDarkMode: Boolean) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val bladeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bladeRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glassmorphicBorder(isDarkMode, 48f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Text Block
                Column(modifier = Modifier.weight(1.2f)) {
                    Surface(
                        color = if (isDarkMode) SkyDarkOrange.copy(alpha = 0.15f) else SkyLightOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ORBITAL FOOD LOGISTICS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Food that flies to your door in minutes",
                        style = MaterialTheme.typography.displayMedium,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                        fontWeight = FontWeight.Black,
                        lineHeight = 36.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ultra-fast thermal cargo containers. Zero friction delivery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                    )
                }

                // Floating Animated Drone Vector
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(140.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        // Draw radar sweeps
                        drawCircle(
                            color = Color(0x1500D4AA),
                            radius = size.width / 2.2f,
                            style = Stroke(width = 2f)
                        )

                        // Draw Drone body
                        drawCircle(
                            color = Color(0xFF00D4AA),
                            radius = size.width / 12f
                        )

                        // Wings
                        drawLine(
                            color = Color(0xFF9CA3AF),
                            start = Offset(center.x - 30, center.y - 15),
                            end = Offset(center.x + 30, center.y + 15),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color(0xFF9CA3AF),
                            start = Offset(center.x - 30, center.y + 15),
                            end = Offset(center.x + 30, center.y - 15),
                            strokeWidth = 6f
                        )
                    }

                    // Blade elements
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .offset(y = (-40).dp, x = (-30).dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = Math.toRadians(bladeRotation.toDouble())
                            drawLine(
                                color = Color(0xFFFF6B35),
                                start = Offset(size.width/2 - 15 * cos(r).toFloat(), size.height/2 - 15 * sin(r).toFloat()),
                                end = Offset(size.width/2 + 15 * cos(r).toFloat(), size.height/2 + 15 * sin(r).toFloat()),
                                strokeWidth = 3f
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .offset(y = (-40).dp, x = 30.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = Math.toRadians(-bladeRotation.toDouble())
                            drawLine(
                                color = Color(0xFFFF6B35),
                                start = Offset(size.width/2 - 15 * cos(r).toFloat(), size.height/2 - 15 * sin(r).toFloat()),
                                end = Offset(size.width/2 + 15 * cos(r).toFloat(), size.height/2 + 15 * sin(r).toFloat()),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudBadge("25 MIN AVG", Icons.Default.Timelapse, if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                HudBadge("₹30 FEE", Icons.Default.FlightTakeoff, if (isDarkMode) SkyDarkOrange else SkyLightOrange)
                HudBadge("4.8 RATING", Icons.Default.Star, Color(0xFFFFD700))
            }
        }
    }
}

// Popular slider card
@Composable
fun PopularItemCard(
    item: FoodItem,
    viewModel: AppViewModel,
    isDarkMode: Boolean,
    isFav: Boolean
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(280.dp)
            .clickable { viewModel.selectFoodItem(item) }
            .glassmorphicBorder(isDarkMode, 24f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                SkyBiteFoodImage(
                    url = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                
                IconButton(
                    onClick = { viewModel.toggleFavorite(item.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color.Red else Color.White
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(item.rating.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${item.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                    )
                    IconButton(
                        onClick = { viewModel.addToCart(item.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDarkMode) SkyDarkOrange else SkyLightOrange, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// AI Recommended Row Card
@Composable
fun AIRecommendedCard(
    item: FoodItem,
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(100.dp)
            .clickable { viewModel.selectFoodItem(item) }
            .glassmorphicBorder(isDarkMode, 16f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.5f) else Color.White)
    ) {
        Row {
            SkyBiteFoodImage(
                url = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                )
                Text(
                    text = "AI Match Score: 98%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00D4AA),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${item.price.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkOrange else SkyLightOrange
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Main Food Grid Card
@Composable
fun FoodGridCard(
    item: FoodItem,
    viewModel: AppViewModel,
    isDarkMode: Boolean,
    isFav: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .glassmorphicBorder(isDarkMode, 24f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                SkyBiteFoodImage(
                    url = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                
                IconButton(
                    onClick = { viewModel.toggleFavorite(item.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = item.hotelName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${item.price.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.selectFoodItem(item) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "View", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { viewModel.addToCart(item.id) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isDarkMode) SkyDarkOrange else SkyLightOrange, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- 6. Food Details Popup Dialog ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailsModal(
    item: FoodItem,
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    val reviews by viewModel.activeReviews.collectAsState()
    var ratingInput by remember { mutableStateOf(5) }
    var reviewCommentInput by remember { mutableStateOf("") }

    val relatedFoods = remember {
        viewModel.allFoodItems.filter { it.category == item.category && it.id != item.id }.take(3)
    }

    Dialog(
        onDismissRequest = { viewModel.selectFoodItem(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp) // padding for floating footer button
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        SkyBiteFoodImage(
                            url = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.selectFoodItem(null) },
                            modifier = Modifier
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .align(Alignment.TopStart)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                            color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                        )

                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.rating} Rating",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            HudBadge("Slot: ${item.category}", Icons.Default.Category, if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                        }

                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "₹${item.price}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                            color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Related foods
                        if (relatedFoods.isNotEmpty()) {
                            Text(
                                text = "Related Flying Modules",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                relatedFoods.forEach { food ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectFoodItem(food) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        Column {
                                            SkyBiteFoodImage(
                                                url = food.imageUrl,
                                                contentDescription = food.name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                            )
                                            Text(
                                                text = food.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(6.dp),
                                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Reviews section
                        Text(
                            text = "Atmospheric Reviews (${reviews.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Submit review card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Write a review:", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (i in 1..5) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= ratingInput) Color(0xFFFFD700) else Color.Gray,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { ratingInput = i }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = reviewCommentInput,
                                    onValueChange = { reviewCommentInput = it },
                                    placeholder = { Text("Share flight feedback...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SkyBiteButton(
                                    text = "Transmit",
                                    onClick = {
                                        if (reviewCommentInput.isNotBlank()) {
                                            viewModel.submitReview(item.id, ratingInput, reviewCommentInput)
                                            reviewCommentInput = ""
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }

                        // Display Reviews
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            reviews.forEach { r ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurfaceVariant.copy(alpha = 0.5f) else Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(r.userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                                            Row {
                                                for (i in 1..r.rating) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            r.comment,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            if (reviews.isEmpty()) {
                                Text("No reviews transmitted yet. Be the first pilot to review!", style = MaterialTheme.typography.bodyMedium, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }

                // Add to Cart floating bottom footer
                Surface(
                    color = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL AMOUNT", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
                            Text("₹${item.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        SkyBiteButton(
                            text = "ADD TO HANGAR",
                            onClick = {
                                viewModel.addToCart(item.id)
                                viewModel.selectFoodItem(null)
                            },
                            icon = Icons.Default.ShoppingCart
                        )
                    }
                }
            }
        }
    }
}

// --- 7. Cart & Order Summary Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val list by viewModel.cartDisplayItems.collectAsState()
    val appliedPromo by viewModel.appliedPromoCode.collectAsState()
    var promoInput by remember { mutableStateOf("") }

    val summary = viewModel.getCartSummary()

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp) // space for header
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Hangar Cargo Cart",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(16.dp)
            )

            if (list.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your hangar has no flying cargo loaded",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SkyBiteButton(
                            text = "EXPLORE MENU FLIGHTS",
                            onClick = { viewModel.navigateTo(Screen.HOME) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list) { displayItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicBorder(isDarkMode, 16f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SkyBiteFoodImage(
                                    url = displayItem.foodItem.imageUrl,
                                    contentDescription = displayItem.foodItem.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayItem.foodItem.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                                    )
                                    Text(
                                        text = "₹${displayItem.foodItem.price.toInt()} each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQty(displayItem.cartId, displayItem.quantity - 1) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDarkMode) Color.White else Color.Black)
                                    }
                                    Text(
                                        displayItem.quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQty(displayItem.cartId, displayItem.quantity + 1) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(if (isDarkMode) SkyDarkOrange else SkyLightOrange, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Promo Code Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .glassmorphicBorder(isDarkMode, 16f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Quantum Coupon Modules",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Codes: SKYFLY20 (20% Off), DRONEFREE (Free Delivery), BITE100 (₹100 Off on ₹400+)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = promoInput,
                                        onValueChange = { promoInput = it },
                                        placeholder = { Text("Enter Promo Code") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("promo_input_field"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SkyBiteButton(
                                        text = if (appliedPromo != null) "Applied" else "Verify",
                                        onClick = {
                                            if (promoInput.isNotBlank()) {
                                                viewModel.applyPromo(promoInput)
                                                promoInput = ""
                                            }
                                        }
                                    )
                                }

                                if (appliedPromo != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Active: $appliedPromo", color = Color(0xFF00D4AA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        TextButton(onClick = { viewModel.removePromo() }) {
                                            Text("Remove Code", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Price Breakdown Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicBorder(isDarkMode, 16f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Payment Summary Matrix", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                PriceRow("Subtotal", "₹${summary.subtotal.toInt()}", isDarkMode)
                                PriceRow("Promo Discount", "-₹${summary.discount.toInt()}", isDarkMode, color = Color(0xFF00D4AA))
                                PriceRow("Delivery Coordinates Fee", "₹${summary.deliveryFee.toInt()}", isDarkMode)
                                PriceRow("IGST / CGST (18%)", "₹${summary.gst.toInt()}", isDarkMode)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Grand Flight Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                                    Text("₹${summary.grandTotal.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SkyBiteButton(
                            text = "PROCEED TO FLIGHT LAUNCH",
                            onClick = { viewModel.navigateTo(Screen.CHECKOUT) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.AutoMirrored.Filled.Launch,
                            testTag = "proceed_checkout_btn"
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PriceRow(label: String, value: String, isDarkMode: Boolean, color: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color ?: (if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary))
    }
}

// --- 8. Payment & Checkout Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Payment method choice: CARD, PAYPAL, COD, UPI
    var selectedPayment by remember { mutableStateOf("CARD") }

    // Card details
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    val summary = viewModel.getCartSummary()

    val adminUsername by viewModel.adminUsername.collectAsState()
    val phonePeNumber by viewModel.adminPhonePeNumber.collectAsState()
    val phonePeUpi by viewModel.adminPhonePeUpi.collectAsState()
    val isSubscribed by viewModel.adminPaymentSubscribed.collectAsState()

    var showPhonePeOverlay by remember { mutableStateOf(false) }
    var phonePeStep by remember { mutableStateOf(1) } // 1 = Secure shake, 2 = Pin entry, 3 = Authorizing payment, 4 = Success
    var phonePePin by remember { mutableStateOf("") }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Dispatch Verification Center",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Address module
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Hangar Delivery Coordinates", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_field"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("checkout_city_field"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it },
                            label = { Text("PIN Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("checkout_pin_field"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Comms Phone Frequency") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_field"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Payment methods list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Select Fuel / Payment Core", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentSelectorChip(selectedPayment == "CARD", "CARD", Icons.Default.CreditCard, "Card", isDarkMode) { selectedPayment = "CARD" }
                        PaymentSelectorChip(selectedPayment == "PHONEPE", "PHONEPE", Icons.Default.Smartphone, "PhonePe", isDarkMode) { selectedPayment = "PHONEPE" }
                        PaymentSelectorChip(selectedPayment == "PAYPAL", "PAYPAL", Icons.Default.Payment, "PayPal", isDarkMode) { selectedPayment = "PAYPAL" }
                        PaymentSelectorChip(selectedPayment == "UPI", "UPI", Icons.Default.QrCode, "UPI / QR", isDarkMode) { selectedPayment = "UPI" }
                        PaymentSelectorChip(selectedPayment == "COD", "COD", Icons.Default.CurrencyExchange, "Cash (COD)", isDarkMode) { selectedPayment = "COD" }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedPayment) {
                        "PHONEPE" -> {
                            if (!isSubscribed) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0x15FF5555) else Color(0x15DD0000))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5555), modifier = Modifier.size(32.dp))
                                        Text(
                                            text = "PHONEPE SERVICE SUSPENDED",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color(0xFFFF5555) else Color(0xFFDD0000),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "The merchant administrator has not activated or renewed their Online Payment Gateway Subscription inside Space Settings. Please contact the administrator or select a different payment method.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF5f259f)) // PhonePe Purple
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(imageVector = Icons.Default.Smartphone, contentDescription = null, tint = Color.White)
                                                Text("PhonePe Gateway Integration", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("SECURE UPI", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                                        Text(
                                            text = "COUPLING PILOT PAYEE: ${adminUsername.uppercase()}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00D4AA)
                                        )

                                        Text(
                                            text = "Your exact cart total ₹${summary.grandTotal.toInt()} will be transferred dynamically via secured PhonePe UPI.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Mini QR
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White, RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Canvas(modifier = Modifier.size(70.dp)) {
                                                drawRect(Color.White, size = size)
                                                val sqSize = size.width / 10
                                                for (i in 0..9) {
                                                    for (j in 0..9) {
                                                        val isAnchor = (i < 3 && j < 3) || (i > 6 && j < 3) || (i < 3 && j > 6)
                                                        val strHash = (phonePeUpi.hashCode() + i * 17 + j * 31).let { if (it < 0) -it else it }
                                                        val fill = if (isAnchor) true else (strHash % 2 == 0)
                                                        if (fill) {
                                                            drawRect(
                                                                color = Color(0xFF5f259f),
                                                                topLeft = Offset(i * sqSize, j * sqSize),
                                                                size = androidx.compose.ui.geometry.Size(sqSize, sqSize)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = "PhonePe: $phonePeNumber | UPI ID: $phonePeUpi",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        "CARD" -> {
                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text("Cardholder Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Card Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { cardCvv = it },
                                    label = { Text("CVV") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        "PAYPAL" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF003087))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PAYPAL COUPLING ONLINE (DEMO MODE)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        "COD" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkMode) Color(0x15FF6B35) else Color(0x15E0531C))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "CASH ON DELIVERY ACCEPTED AT DRONE LANDING",
                                    color = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        "UPI" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("UPI / QR Payment Console", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Beautiful vector simulated QR Code using Canvas
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    // border
                                    drawRect(Color.White, size = size)
                                    // dynamic QR boxes representing actual data
                                    val sqSize = size.width / 10
                                    for (i in 0..9) {
                                        for (j in 0..9) {
                                            // anchor markers
                                            val isAnchor = (i < 3 && j < 3) || (i > 6 && j < 3) || (i < 3 && j > 6)
                                            val fill = if (isAnchor) true else (((i * 3 + j * 7) % 2) == 0)
                                            if (fill) {
                                                drawRect(
                                                    color = Color.Black,
                                                    topLeft = Offset(i * sqSize, j * sqSize),
                                                    size = androidx.compose.ui.geometry.Size(sqSize, sqSize)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("UPI ID: skybite@phonepe", fontWeight = FontWeight.Bold, color = if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.showToast("UPI deep linking launched inside PhonePe", ToastType.SUCCESS) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5f259f))
                                    ) {
                                        Text("Launch PhonePe", color = Color.White)
                                    }
                                    Button(
                                        onClick = { viewModel.showToast("UPI deep linking launched inside Google Pay", ToastType.SUCCESS) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a73e8))
                                    ) {
                                        Text("Launch GPay", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Total breakdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Verified Payload:", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                    Text("₹${summary.grandTotal.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                }
            }

            SkyBiteButton(
                text = if (selectedPayment == "PHONEPE") "OPEN PHONEPE & PAY ₹${summary.grandTotal.toInt()}" else "CONFIRM SPACE DISPATCH",
                onClick = {
                    if (address.isBlank() || city.isBlank() || pincode.isBlank() || phone.isBlank()) {
                        viewModel.showToast("Delivery coordinates required.", ToastType.ERROR)
                    } else if (selectedPayment == "PHONEPE" && !isSubscribed) {
                        viewModel.showToast("PhonePe gateway suspended. Contact administrator.", ToastType.WARNING)
                    } else if (selectedPayment == "PHONEPE") {
                        showPhonePeOverlay = true
                        phonePeStep = 1
                        phonePePin = ""
                    } else {
                        viewModel.placeOrder(address, city, pincode, phone, selectedPayment)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = if (selectedPayment == "PHONEPE") Icons.Default.Smartphone else Icons.AutoMirrored.Filled.Launch,
                testTag = "place_order_btn"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPhonePeOverlay) {
        Dialog(onDismissRequest = { showPhonePeOverlay = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D144A)) // Deep PhonePe Purple
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "PhonePe",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable { showPhonePeOverlay = false }
                                .padding(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    when (phonePeStep) {
                        1 -> {
                            // Step 1: Connecting
                            Spacer(modifier = Modifier.height(24.dp))
                            CircularProgressIndicator(color = Color(0xFF00D4AA), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "CONTACTING SATELLITE CORE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Establishing secure shell with PhonePe servers...",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            LaunchedEffect(Unit) {
                                delay(1500)
                                phonePeStep = 2
                            }
                        }
                        2 -> {
                            // Step 2: PIN Entry
                            Text(
                                text = "PAYING SECURELY",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Merchant details
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = adminUsername.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00D4AA),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "PhonePe: $phonePeNumber | UPI: $phonePeUpi",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "₹${summary.grandTotal.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ENTER 4-DIGIT UPI PIN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            // PIN dots display
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0..3) {
                                    val isFilled = phonePePin.length > i
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                color = if (isFilled) Color.White else Color.White.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Numeric Keypad
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val keys = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("⌫", "0", "✔")
                                )

                                keys.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { key ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (key == "⌫") {
                                                            if (phonePePin.isNotEmpty()) {
                                                                phonePePin = phonePePin.dropLast(1)
                                                            }
                                                        } else if (key == "✔") {
                                                            if (phonePePin.length == 4) {
                                                                phonePeStep = 3
                                                            } else {
                                                                viewModel.showToast("Please enter 4-digit PIN.", ToastType.WARNING)
                                                            }
                                                        } else {
                                                            if (phonePePin.length < 4) {
                                                                phonePePin += key
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    color = if (key == "✔") Color(0xFF00D4AA) else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Step 3: Authorizing
                            Spacer(modifier = Modifier.height(24.dp))
                            CircularProgressIndicator(color = Color(0xFF00D4AA), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AUTHORIZING TRANSACTION",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Quantum validation in progress...",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            LaunchedEffect(Unit) {
                                delay(2000)
                                phonePeStep = 4
                            }
                        }
                        4 -> {
                            // Step 4: Success
                            Spacer(modifier = Modifier.height(24.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00D4AA),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "TRANSACTION APPROVED!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Funds securely coupled to $adminUsername",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            LaunchedEffect(Unit) {
                                delay(1200)
                                showPhonePeOverlay = false
                                viewModel.placeOrder(address, city, pincode, phone, "PHONEPE")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentSelectorChip(
    selected: Boolean,
    value: String,
    icon: ImageVector,
    label: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) {
            (if (isDarkMode) SkyDarkAccent else SkyLightAccent).copy(alpha = 0.25f)
        } else {
            if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                if (isDarkMode) SkyDarkAccent else SkyLightAccent
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) {
                    if (isDarkMode) SkyDarkAccent else SkyLightAccent
                } else {
                    if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                } else {
                    if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                }
            )
        }
    }
}

// --- 9. Drone Tracking & Simulation Screen ---
@Composable
fun TrackingScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val activeOrder by viewModel.activeTrackedOrder.collectAsState()
    val progress by viewModel.droneProgress.collectAsState()
    val status by viewModel.droneStatus.collectAsState()
    val secondsLeft by viewModel.deliverySecondsRemaining.collectAsState()
    val droneGps by viewModel.droneGps.collectAsState()
    val shortestPathNodes by viewModel.shortestPathNodes.collectAsState()
    val heavyTraffic by viewModel.heavyTrafficRoutingMode.collectAsState()
    val hotelName = viewModel.getActiveOrderHotelName()

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Orbital Flight Telemetry",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // High-Tech Custom Canvas Radar Map View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 32f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TelemetryCanvasMap(
                        progress = progress,
                        shortestPathNodes = shortestPathNodes,
                        heavyTraffic = heavyTraffic,
                        hotelName = hotelName,
                        customerAddress = activeOrder?.deliveryAddress ?: "Sector 7 Hangar",
                        isDarkMode = isDarkMode
                    )

                    // Overlay metrics
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("DRONE FREQUENCY: ACTIVE", color = Color(0xFF00D4AA), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("EST SPEED: ${String.format("%.1f", droneGps.speedKmh)} KM/H", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("ALTITUDE: ${String.format("%.1f", droneGps.altitudeMeters)}M", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("STATUS: ${droneGps.signalStatus}", color = Color(0x90FFFFFF), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                    }

                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("GPS: LOCKED (${droneGps.satellitesCount} SATS)", color = Color(0xFF00D4AA), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("LAT: ${String.format("%.6f", droneGps.latitude)}° N", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("LNG: ${String.format("%.6f", droneGps.longitude)}° E", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("SIGNAL: ${droneGps.signalStrengthPercentage}% QUALITY", color = Color(0x90FFFFFF), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                    }

                    // Monospace floating lat/lng HUD footer
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE LAT: ${String.format("%.6f", droneGps.latitude)}",
                            color = Color(0xFF00D4AA),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "LIVE LNG: ${String.format("%.6f", droneGps.longitude)}",
                            color = Color(0xFF00D4AA),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Tracking progress stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Status: $status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkMode) SkyDarkOrange else SkyLightOrange
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stepped Status Progression Bar
                    RealtimeStatusProgressionBar(progress = progress, isDarkMode = isDarkMode)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("EST TIME LEFT", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
                            Text(
                                if (progress >= 1.0f) "DELIVERED" else "${secondsLeft / 60}:${String.format("%02d", secondsLeft % 60)} MIN",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SIGNAL INTEGRITY", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
                            Text("${droneGps.signalStrengthPercentage}% ${droneGps.signalStatus.take(5)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00D4AA))
                        }
                    }
                }
            }

            // Secure Delivery Handover Verification Card
            if (activeOrder != null) {
                val orderId = activeOrder!!.id
                val correctOtp = viewModel.getDeliveryOtp(orderId)
                val submittedOtps by viewModel.orderOtpsSubmitted.collectAsState()
                val userSubmittedOtp = submittedOtps[orderId] ?: ""
                val isMatched = userSubmittedOtp == correctOtp

                var otpInputText by remember(orderId) { mutableStateOf("") }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .glassmorphicBorder(isDarkMode, 24f)
                        .testTag("delivery_otp_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SkyDarkSurface.copy(alpha = 0.85f) else Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isMatched || status == "Delivered") Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = if (isMatched || status == "Delivered") Color(0xFF00D4AA) else Color(0xFFFF6B35),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Secure Cargo Handover Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To unlock the drone's molecular cargo compartment upon landing, retrieve your unique verification pin below and transmit it to Space Command.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // High-contrast security pin box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .border(1.dp, if (isDarkMode) Color(0x30FFFFFF) else Color(0x15000000), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "YOUR DELIVERY VERIFICATION OTP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = correctOtp,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                                    letterSpacing = 4.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (status != "Delivered") {
                            OutlinedTextField(
                                value = otpInputText,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        otpInputText = it
                                    }
                                },
                                label = { Text("Enter 4-Digit Verification OTP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_input_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (otpInputText.length == 4) {
                                        viewModel.submitDeliveryOtp(orderId, otpInputText)
                                    } else {
                                        viewModel.showToast("Please enter a valid 4-digit OTP", ToastType.ERROR)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_otp_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "TRANSMIT HANDSHAKE PIN",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Match status indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isMatched) Color(0xFF00D4AA) else Color(0xFFFF6B35).copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMatched) "✓ SECURE LOCK RELEASED. Awaiting Admin confirmation." else "✗ SECURE LOCK ACTIVE. Enter correct OTP.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatched) Color(0xFF00D4AA) else Color(0xFFFF6B35).copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            // Already Delivered
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1500D4AA))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00D4AA),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SECURE HANDOVER COMPLETE! cargo disengaged.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D4AA)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Dijkstra Router Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quantum Router (Dijkstra's Shortest Path)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Plots optimal delivery course between the food prep hotel and your entered coordinates. Shortest path is dynamically calculated using active airway congestion factors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (heavyTraffic) "STORM BYPASS ROUTING ACTIVATED" else "DIRECT SKY CORRIDOR ROUTING",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (heavyTraffic) Color(0xFFFF6B35) else Color(0xFF00D4AA)
                            )
                            Text(
                                text = if (heavyTraffic) "Heavy ion-storm detected. Re-routing around magnetic obstacles." else "Atmospheric interference clear. Optimal straightway channels locked.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                            )
                        }
                        
                        Switch(
                            checked = heavyTraffic,
                            onCheckedChange = { viewModel.toggleHeavyTrafficMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00D4AA),
                                checkedTrackColor = Color(0x5000D4AA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RESOLVED ACTIVE WAYPOINTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Display nodes along the shortest path
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shortestPathNodes.forEachIndexed { index, node ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background((if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)))
                                    .border(
                                        width = 1.dp,
                                        color = if (index == 0) Color(0xFF00D4AA) else if (index == shortestPathNodes.size - 1) Color(0xFFFF6B35) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (index == 0) "START" else if (index == shortestPathNodes.size - 1) "END" else "WAYPOINT ${index}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = if (index == 0) Color(0xFF00D4AA) else if (index == shortestPathNodes.size - 1) Color(0xFFFF6B35) else Color.Gray
                                    )
                                    Text(
                                        text = if (index == 0) hotelName.split(" ").firstOrNull() ?: node.name else node.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color.Black
                                    )
                                }
                            }

                            if (index < shortestPathNodes.size - 1) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = if (isDarkMode) Color.DarkGray else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Status timeline
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassmorphicBorder(isDarkMode, 16f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Flight Route Checklist", style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    TimelineItem("Preparing Order", "Kitchen crew preparing your molecular fuel", progress >= 0.05f, isDarkMode)
                    TimelineItem("Drone Dispatched", "Payload locked. Rocket disengaged", progress >= 0.15f, isDarkMode)
                    TimelineItem("In Transit", "Cruising at Mach 0.05 altitude", progress >= 0.40f, isDarkMode)
                    TimelineItem("Arriving", "Descending into terminal coordinates", progress >= 0.75f, isDarkMode)
                    TimelineItem("Delivered", "Thermal payload soft drop complete", progress >= 1.00f, isDarkMode)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "GPS RECEIVER DATA PACKET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background((if (isDarkMode) Color.Black else Color(0xFFF5F5F5)).copy(alpha = 0.6f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DRONE ID: SB-904", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
                            Text("LATITUDE: ${String.format("%.6f", droneGps.latitude)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                            Text("LONGITUDE: ${String.format("%.6f", droneGps.longitude)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS: ${droneGps.signalStatus}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00D4AA), fontWeight = FontWeight.Bold)
                            Text("ALTITUDE: ${String.format("%.1f", droneGps.altitudeMeters)} meters", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                            Text("SPEED: ${String.format("%.1f", droneGps.speedKmh)} km/h", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.showToast("Quantum audio channel established with SkyBite dispatcher.", ToastType.SUCCESS) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Help")
                }
                
                SkyBiteButton(
                    text = "DISENGAGE",
                    onClick = { viewModel.navigateTo(Screen.HOME) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val oneMinuteAlert by viewModel.oneMinuteNotificationAlert.collectAsState()
    if (oneMinuteAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOneMinuteAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("DRONE 1-MINUTE AWAY!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(oneMinuteAlert ?: "")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissOneMinuteAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA))
                ) {
                    Text("OK, ACKNOWLEDGED", color = Color.White)
                }
            }
        )
    }
}

// Stunning Custom Radar Mapping Canvas with Shortest Path Dijkstra Visualizer
@Composable
fun TelemetryCanvasMap(
    progress: Float,
    shortestPathNodes: List<MapNode>,
    heavyTraffic: Boolean,
    hotelName: String,
    customerAddress: String,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    // Animated rotor blade angle for the drone icon
    val rotorAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotor_spin"
    )

    val paintStartText = remember(isDarkMode) {
        android.graphics.Paint().apply {
            color = if (isDarkMode) 0xFF00D4AA.toInt() else 0xFF00A485.toInt()
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
    }

    val paintEndText = remember(isDarkMode) {
        android.graphics.Paint().apply {
            color = if (isDarkMode) 0xFFFF6B35.toInt() else 0xFFD84B16.toInt()
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
    }

    val paintWaypointText = remember(isDarkMode) {
        android.graphics.Paint().apply {
            color = if (isDarkMode) 0x90FFFFFF.toInt() else 0x90000000.toInt()
            textSize = 18f
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)

        // Helper function to map path coordinates (percentages) to actual pixel bounds
        fun getCanvasOffset(nodeX: Float, nodeY: Float): Offset {
            return Offset(nodeX / 100f * width, nodeY / 100f * height)
        }

        // Draw tactical radar grids
        for (i in 1..4) {
            drawCircle(
                color = if (isDarkMode) Color(0x0F00D4AA) else Color(0x0C00A485),
                radius = (width / 7) * i,
                style = Stroke(width = 1.5f)
            )
        }

        // Axis crosshairs
        drawLine(
            color = if (isDarkMode) Color(0x1000D4AA) else Color(0x0A00A485),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f
        )
        drawLine(
            color = if (isDarkMode) Color(0x1000D4AA) else Color(0x0A00A485),
            start = Offset(width / 2, 0f),
            end = Offset(width / 2, height),
            strokeWidth = 1f
        )

        // Radar Sweep Scanner Line
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEndX = center.x + (width / 2) * cos(sweepRad).toFloat()
        val sweepEndY = center.y + (width / 2) * sin(sweepRad).toFloat()
        drawLine(
            color = if (isDarkMode) Color(0x4000D4AA) else Color(0x2500A485),
            start = center,
            end = Offset(sweepEndX, sweepEndY),
            strokeWidth = 2f
        )

        // Draw ALL possible sky lanes (streets/graph edges) in a low-opacity tactical blue
        val allEdges = PathFinder.getEdges(heavyTraffic)
        allEdges.forEach { edge ->
            val fromNode = PathFinder.nodes.find { it.id == edge.fromNodeId }
            val toNode = PathFinder.nodes.find { it.id == edge.toNodeId }
            if (fromNode != null && toNode != null) {
                val p1 = getCanvasOffset(fromNode.xPercent, fromNode.yPercent)
                val p2 = getCanvasOffset(toNode.xPercent, toNode.yPercent)
                // Draw thin street lines
                drawLine(
                    color = if (isDarkMode) Color(0x1F475569) else Color(0x1F94A3B8),
                    start = p1,
                    end = p2,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                )
            }
        }

        // Highlight the optimal SHORTEST PATH (Dijkstra) in glowing green/cyan
        if (shortestPathNodes.size > 1) {
            for (i in 0 until shortestPathNodes.size - 1) {
                val n1 = shortestPathNodes[i]
                val n2 = shortestPathNodes[i + 1]
                val p1 = getCanvasOffset(n1.xPercent, n1.yPercent)
                val p2 = getCanvasOffset(n2.xPercent, n2.yPercent)

                // Thick neon route line
                drawLine(
                    color = if (isDarkMode) Color(0x5000D4AA) else Color(0x6000A485),
                    start = p1,
                    end = p2,
                    strokeWidth = 8f
                )
                drawLine(
                    color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                    start = p1,
                    end = p2,
                    strokeWidth = 3f
                )
            }
        }

        // Draw intersection nodes/waypoints as tactical dots
        PathFinder.nodes.forEach { node ->
            if (node.id != "HOTEL" && node.id != "CUSTOMER") {
                val pos = getCanvasOffset(node.xPercent, node.yPercent)
                drawCircle(
                    color = if (isDarkMode) Color(0x80334155) else Color(0x80CBD5E1),
                    radius = 6f,
                    center = pos
                )
                // Label each intersection
                drawContext.canvas.nativeCanvas.drawText(
                    node.name.uppercase(),
                    pos.x,
                    pos.y - 12f,
                    paintWaypointText
                )
            }
        }

        // Draw Starting Hangar (Hotel)
        val startNode = PathFinder.nodes.first()
        val startPos = getCanvasOffset(startNode.xPercent, startNode.yPercent)
        drawCircle(
            color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
            radius = 12f,
            center = startPos
        )
        drawCircle(
            color = if (isDarkMode) Color(0x4000D4AA) else Color(0x3000A485),
            radius = 22f,
            center = startPos,
            style = Stroke(width = 2f)
        )
        // Draw Starting Text label
        drawContext.canvas.nativeCanvas.drawText(
            "HOTEL: " + hotelName.uppercase().take(22) + " (START)",
            startPos.x,
            startPos.y + 32f,
            paintStartText
        )

        // Draw Customer Hangar (Delivery Address)
        val endNode = PathFinder.nodes.last()
        val endPos = getCanvasOffset(endNode.xPercent, endNode.yPercent)
        drawCircle(
            color = Color(0xFFFF6B35),
            radius = 12f,
            center = endPos
        )
        // Pulsing radar target marker
        val pulseRadius = 24f + (14f * sin((System.currentTimeMillis() % 1000) / 1000f * 2 * Math.PI).toFloat())
        drawCircle(
            color = Color(0x40FF6B35),
            radius = pulseRadius,
            center = endPos,
            style = Stroke(width = 2f)
        )
        // Draw Customer Text label
        val formattedAddr = customerAddress.replace(", Bangalore", "").replace(", India", "")
        drawContext.canvas.nativeCanvas.drawText(
            "DROP: " + formattedAddr.uppercase().take(28),
            endPos.x,
            endPos.y - 24f,
            paintEndText
        )

        // Calculate and Draw the Drone moving smoothly along the Dijkstra shortest path nodes
        val dronePosPair = PathFinder.getPositionOnPath(shortestPathNodes, progress)
        val dronePos = getCanvasOffset(dronePosPair.first, dronePosPair.second)

        // Draw Drone Body (Central micro-chip or core)
        drawCircle(
            color = Color.White,
            radius = 14f,
            center = dronePos
        )
        drawCircle(
            color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
            radius = 8f,
            center = dronePos
        )
        drawCircle(
            color = if (isDarkMode) Color(0x4000D4AA) else Color(0x3000A485),
            radius = 24f,
            center = dronePos,
            style = Stroke(width = 2f)
        )

        // Draw 4 Rotating Quadcopter Rotors!
        val rotorDistance = 24f
        val radAngle = Math.toRadians(rotorAngle.toDouble())
        val cosA = cos(radAngle).toFloat()
        val sinA = sin(radAngle).toFloat()

        // 4 Rotor arms angles: 45, 135, 225, 315
        val angles = listOf(45.0, 135.0, 225.0, 315.0)
        angles.forEach { angle ->
            val armRad = Math.toRadians(angle)
            val armEndX = dronePos.x + rotorDistance * cos(armRad).toFloat()
            val armEndY = dronePos.y + rotorDistance * sin(armRad).toFloat()
            val armPos = Offset(armEndX, armEndY)

            // Arm connection line
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = dronePos,
                end = armPos,
                strokeWidth = 2f
            )

            // Rotor blade line 1 (rotated dynamically)
            val bladeLength = 10f
            val b1 = Offset(armPos.x + bladeLength * cosA, armPos.y + bladeLength * sinA)
            val b2 = Offset(armPos.x - bladeLength * cosA, armPos.y - bladeLength * sinA)
            drawLine(
                color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                start = b1,
                end = b2,
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun RealtimeStatusProgressionBar(
    progress: Float,
    isDarkMode: Boolean
) {
    val activeColor = Color(0xFF00D4AA)
    val pendingColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
    val subtextColor = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary

    val preparingCompleted = progress >= 0.15f
    val preparingActive = progress in 0.0f..0.15f

    val transitCompleted = progress >= 1.0f
    val transitActive = progress in 0.15f..1.0f

    val deliveredCompleted = progress >= 1.0f
    val deliveredActive = progress >= 1.0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(pendingColor)
            )

            // Dynamic progress fill line
            val fillFraction = when {
                progress < 0.15f -> {
                    0.05f + (progress / 0.15f) * 0.40f
                }
                progress < 1.0f -> {
                    0.45f + ((progress - 0.15f) / 0.85f) * 0.50f
                }
                else -> 1.0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .padding(horizontal = 36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(activeColor, Color(0xFF00BFFF))
                        )
                    )
            )

            // Milestone nodes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Milestone 1: Preparing
                MilestoneNode(
                    completed = preparingCompleted,
                    active = preparingActive,
                    icon = Icons.Default.Restaurant,
                    isDarkMode = isDarkMode
                )

                // Milestone 2: In Transit
                MilestoneNode(
                    completed = transitCompleted,
                    active = transitActive,
                    icon = Icons.Default.FlightTakeoff,
                    isDarkMode = isDarkMode
                )

                // Milestone 3: Delivered
                MilestoneNode(
                    completed = deliveredCompleted,
                    active = deliveredActive,
                    icon = Icons.Default.CheckCircle,
                    isDarkMode = isDarkMode
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Node labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Label 1: Preparing
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Preparing",
                    fontWeight = if (preparingActive) FontWeight.Black else FontWeight.Bold,
                    color = if (preparingActive || preparingCompleted) activeColor else subtextColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = if (preparingCompleted) "Ready" else if (preparingActive) "Cooking..." else "Pending",
                    color = subtextColor,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Label 2: In Transit
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "In Transit",
                    fontWeight = if (transitActive) FontWeight.Black else FontWeight.Bold,
                    color = if (transitActive || transitCompleted) activeColor else subtextColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = if (transitCompleted) "Arrived" else if (transitActive) "Flying" else "Pending",
                    color = subtextColor,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Label 3: Delivered
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delivered",
                    fontWeight = if (deliveredActive) FontWeight.Black else FontWeight.Bold,
                    color = if (deliveredActive) activeColor else subtextColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = if (deliveredCompleted) "Payload Dropped" else "Pending",
                    color = subtextColor,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun MilestoneNode(
    completed: Boolean,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean
) {
    val nodeColor = when {
        completed -> Color(0xFF00D4AA)
        active -> Color(0xFF00BFFF)
        else -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    }

    val iconColor = when {
        completed -> Color(0xFF090E17)
        active -> Color.White
        else -> if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(nodeColor)
            .border(
                width = if (active) 2.dp else 0.dp,
                color = if (active) Color.White else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun TimelineItem(
    title: String,
    description: String,
    completed: Boolean,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) Color(0xFF00D4AA) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (completed) {
                    if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                } else Color.Gray
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

// --- Dynamic Feedbacks Reviews Modal after Delivery Drop ---
@Composable
fun PostDeliveryReviewDialog(
    foodId: Int,
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    var ratingInput by remember { mutableStateOf(5) }
    var commentInput by remember { mutableStateOf("") }
    val foodItem = remember { viewModel.allFoodItems.find { it.id == foodId } }

    Dialog(
        onDismissRequest = { viewModel.closeReviewForm() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .glassmorphicBorder(isDarkMode, 48f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Launch Success!",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                )
                Text(
                    "How was the payload delivery?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                if (foodItem != null) {
                    Text(
                        foodItem.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { ratingInput = i }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= ratingInput) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Rate quality of drone speed, flavor molecular metrics...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeReviewForm() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Later")
                    }
                    SkyBiteButton(
                        text = "Transmit Feedback",
                        onClick = {
                            viewModel.submitReview(foodId, ratingInput, commentInput)
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

// --- 10. Edit Profile Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val user by viewModel.currentUser.collectAsState()

    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quantum Pilot Profile",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(if (isDarkMode) SkyDarkOrange.copy(alpha = 0.2f) else SkyLightOrange.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, if (isDarkMode) SkyDarkOrange else SkyLightOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isDarkMode) SkyDarkOrange else SkyLightOrange,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Comms Frequency (Phone)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Database Email Coordinate") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SkyBiteButton(
                text = "UPDATE PROFILE MODULE",
                onClick = { viewModel.updateProfile(name, phone, email) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Save
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DISENGAGE PILOT COUPLING")
            }
        }
    }
}

// --- 11. Order History Screen ---
@Composable
fun OrderHistoryScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val orders by viewModel.orderHistory.collectAsState()

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F1626), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(bottom = 80.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Hangar Flight Logs",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(16.dp)
            )

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No previous flight launches registered in logs",
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicBorder(isDarkMode, 16f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SkyDarkSurface else Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("LAUNCH ID: #${log.id}", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) SkyDarkAccent else SkyLightAccent)
                                    Text(
                                        "STATUS: ${log.status.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (log.status == "Delivered") Color(0xFF00D4AA) else Color(0xFFFF6B35),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Parse items list mock representation: "1:2,2:1" -> "2x Aero-Spiced Biryani, 1x Pizza"
                                val itemsList = log.itemsJson.split(",").mapNotNull { item ->
                                    val parts = item.split(":")
                                    val id = parts.getOrNull(0)?.toIntOrNull()
                                    val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                    val food = viewModel.allFoodItems.find { it.id == id }
                                    if (food != null) {
                                        "${qty}x ${food.name}"
                                    } else null
                                }

                                Text(
                                    itemsList.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PriceRow("Delivery Address", log.deliveryAddress, isDarkMode)
                                PriceRow("Cost Payload", "₹${log.totalAmount.toInt()}", isDarkMode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    val orderHistory by viewModel.orderHistory.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val foodItems by viewModel.allFoodItemsFlow.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val adminHotel = currentUser?.hotelName ?: "SkyBite Central Hangar"
    val isSuperAdmin = currentUser?.id == -99

    // Filter food items and orders specifically for this admin's hotel
    val filteredFoodItems = remember(foodItems, adminHotel, isSuperAdmin) {
        if (isSuperAdmin) {
            foodItems
        } else {
            foodItems.filter { it.hotelName == adminHotel }
        }
    }

    val filteredOrders = remember(orderHistory, adminHotel, isSuperAdmin, foodItems) {
        if (isSuperAdmin) {
            orderHistory
        } else {
            orderHistory.filter { order ->
                val itemPairs = order.itemsJson.split(",").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val itemId = parts[0].toIntOrNull()
                        if (itemId != null) {
                            foodItems.find { item -> item.id == itemId }
                        } else null
                    } else null
                }
                itemPairs.any { it?.hotelName == adminHotel }
            }
        }
    }
    
    var currentTab by remember { mutableStateOf(0) } // 0 = Flight Jobs, 1 = Hangar Catalog, 2 = User Directory
    
    // Add Item Fields State
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Burger") }
    var newImgUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80") }

    val bgBrush = if (isDarkMode) {
        Brush.radialGradient(colors = listOf(Color(0xFF0F172A), SkyDarkBackground))
    } else {
        Brush.verticalGradient(listOf(SkyLightBackground, Color.White))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SKYBITE SPACE COMMAND",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                    )
                    Text(
                        text = "Quantum Drone Logistics Terminal: $adminHotel",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.showToast("Disengaging from Admin Terminal...", ToastType.INFO)
                        viewModel.navigateTo(Screen.LOGIN)
                    },
                    modifier = Modifier
                        .background(
                            color = if (isDarkMode) Color(0x11FF5555) else Color(0x11FF0000),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = if (isDarkMode) Color(0xFFFF5555) else Color(0xFFDD0000)
                    )
                }
            }

            // Quick Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stat 1: Total Revenue
                val totalRevenue = filteredOrders.sumOf { it.totalAmount }
                AdminStatCard(
                    title = "REVENUE PAYLOAD",
                    value = "₹${totalRevenue.toInt()}",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFF00D4AA),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )

                // Stat 2: Active Drones (Flight Logs)
                AdminStatCard(
                    title = "FLIGHT LOGS",
                    value = "${filteredOrders.size}",
                    icon = Icons.Default.RocketLaunch,
                    color = Color(0xFFFFA500),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )

                // Stat 3: Registered Pilots (Users)
                AdminStatCard(
                    title = "PILOTS",
                    value = "${allUsers.size}",
                    icon = Icons.Default.Person,
                    color = Color(0xFF2196F3),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom Tabs
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = Color.Transparent,
                contentColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("FLIGHT JOBS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("HANGAR CATALOG", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text("USER DIRECTORY", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    text = { Text("SPACE SETTINGS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> AdminFlightJobsSection(
                        orders = filteredOrders,
                        onUpdateStatus = { id, status -> viewModel.updateOrderStatus(id, status) },
                        isDarkMode = isDarkMode,
                        viewModel = viewModel
                    )
                    1 -> AdminHangarCatalogSection(
                        foodItems = filteredFoodItems,
                        onAddItem = { name, price, desc, cat, img -> viewModel.addNewFoodItem(name, price, desc, cat, img) },
                        onDeleteItem = { id -> viewModel.deleteFoodItem(id) },
                        newName = newName,
                        onNameChange = { newName = it },
                        newPrice = newPrice,
                        onPriceChange = { newPrice = it },
                        newDesc = newDesc,
                        onDescChange = { newDesc = it },
                        newCategory = newCategory,
                        onCategoryChange = { newCategory = it },
                        newImgUrl = newImgUrl,
                        onImgUrlChange = { newImgUrl = it },
                        isDarkMode = isDarkMode
                    )
                    2 -> AdminUserDirectorySection(
                        users = allUsers,
                        isDarkMode = isDarkMode
                    )
                    3 -> AdminGatewaySettingsSection(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }

    val adminAlert by viewModel.adminNotificationAlert.collectAsState()
    if (adminAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdminNotificationAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Calling alert",
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("SYSTEM RADAR CALL!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(adminAlert ?: "")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAdminNotificationAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA))
                ) {
                    Text("ACKNOWLEDGE INCOMING FEED", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
            )
        }
    }
}

@Composable
fun AdminFlightJobsSection(
    orders: List<OrderHistoryEntity>,
    onUpdateStatus: (Int, String) -> Unit,
    isDarkMode: Boolean,
    viewModel: AppViewModel
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No cargo runs placed in SkyBite system.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RUN ID: #${order.id}",
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                            )
                            
                            // Status Chip
                            val statusColor = when (order.status) {
                                "Preparing order", "Preparing" -> Color(0xFFFFA500)
                                "Drone dispatched", "Dispatched" -> Color(0xFF2196F3)
                                "In transit", "In Transit" -> Color(0xFF00BCD4)
                                "Arriving" -> Color(0xFF9C27B0)
                                "Delivered" -> Color(0xFF00D4AA)
                                else -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Splitting
                        val itemsList = order.itemsJson.split(",").mapNotNull { item ->
                            val parts = item.split(":")
                            val id = parts.getOrNull(0)?.toIntOrNull()
                            val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                            val food = viewModel.allFoodItems.find { it.id == id }
                            if (food != null) {
                                "${qty}x ${food.name}"
                            } else null
                        }

                        Text(
                            text = "Cargo: " + itemsList.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )

                        Text(
                            text = "Dest: ${order.deliveryAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDarkMode) Color.Gray.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Secure harbor protocol (OTP status)
                        val submittedOtps by viewModel.orderOtpsSubmitted.collectAsState()
                        val userOtp = submittedOtps[order.id]
                        val correctOtp = viewModel.getDeliveryOtp(order.id)
                        val isOtpMatched = userOtp == correctOtp

                        if (order.status != "Delivered") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = (if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isOtpMatched) Color(0xFF00D4AA).copy(alpha = 0.4f) else Color(0xFFFF6B35).copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isOtpMatched) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isOtpMatched) Color(0xFF00D4AA) else Color(0xFFFF6B35),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "SECURE HARBOR PROTOCOL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Required Target PIN: $correctOtp",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                                    )
                                    Text(
                                        text = "Customer Transmitted PIN: ${userOtp ?: "Awaiting input..."}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOtpMatched) Color(0xFF00D4AA) else (if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary)
                                    )
                                    if (isOtpMatched) {
                                        Text(
                                            text = "✓ Handshake PIN verified. Ready to authorize drop off.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00D4AA)
                                        )
                                    } else {
                                        // Allow Admin to enter the OTP manually as a fallback call
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            var manualOtp by remember(order.id) { mutableStateOf("") }
                                            OutlinedTextField(
                                                value = manualOtp,
                                                onValueChange = { if (it.length <= 4) manualOtp = it },
                                                label = { Text("Manual PIN override", fontSize = 9.sp) },
                                                modifier = Modifier.weight(1f).height(48.dp),
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    if (manualOtp == correctOtp) {
                                                        viewModel.submitDeliveryOtp(order.id, manualOtp)
                                                    } else {
                                                        viewModel.showToast("Manual override failed: Pin mismatch.", ToastType.ERROR)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Verify", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Admin actions
                        Text(
                            text = "COMMAND FLIGHT PATTERN:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val statuses = listOf("Preparing", "Dispatched", "In Transit", "Arriving", "Delivered")
                            statuses.forEach { s ->
                                val isCurrent = order.status == s || (s == "Preparing" && order.status == "Preparing order") || (s == "Dispatched" && order.status == "Drone dispatched")
                                val btnBg = if (isCurrent) {
                                    if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                } else {
                                    if (isDarkMode) Color(0xFF111827) else Color.LightGray.copy(alpha = 0.4f)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .background(btnBg, RoundedCornerShape(6.dp))
                                        .clickable { onUpdateStatus(order.id, s) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s.uppercase().replace("IN ", ""),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else (if (isDarkMode) Color.Gray else Color.DarkGray)
                                    )
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
fun AdminHangarCatalogSection(
    foodItems: List<FoodItem>,
    onAddItem: (String, Double, String, String, String) -> Unit,
    onDeleteItem: (Int) -> Unit,
    newName: String,
    onNameChange: (String) -> Unit,
    newPrice: String,
    onPriceChange: (String) -> Unit,
    newDesc: String,
    onDescChange: (String) -> Unit,
    newCategory: String,
    onCategoryChange: (String) -> Unit,
    newImgUrl: String,
    onImgUrlChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Form Section Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DEPLOY NEW FOOD CARGO",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = onNameChange,
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = onPriceChange,
                            label = { Text("Price (₹)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                            )
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = onCategoryChange,
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = onDescChange,
                        label = { Text("Cargo Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newImgUrl,
                        onValueChange = onImgUrlChange,
                        label = { Text("Image URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val priceVal = newPrice.toDoubleOrNull() ?: 0.0
                            onAddItem(newName, priceVal, newDesc, newCategory, newImgUrl)
                            onNameChange("")
                            onPriceChange("")
                            onDescChange("")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH NEW CARGO ITEM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Items List Header
        item {
            Text(
                text = "ACTIVE HANGAR ITEMS (${foodItems.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Items Catalog
        items(foodItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SkyDarkSurfaceVariant.copy(alpha = 0.6f) else Color.LightGray.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                        )
                        Text(
                            text = "${item.category} • ₹${item.price.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                    }

                    IconButton(
                        onClick = { onDeleteItem(item.id) },
                        modifier = Modifier.background(
                            color = if (isDarkMode) Color(0x11FF5555) else Color(0x11FF0000),
                            shape = CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Cargo",
                            tint = if (isDarkMode) Color(0xFFFF5555) else Color(0xFFDD0000),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserDirectorySection(
    users: List<UserEntity>,
    isDarkMode: Boolean
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No pilots registered on SkyBite satellite database.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)
                    )
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
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Email: ${user.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                            )
                            Text(
                                text = "Comms: ${user.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = (if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COUPLED PILOT",
                                color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485),
                                fontSize = 8.sp,
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
fun AdminGatewaySettingsSection(viewModel: AppViewModel, isDarkMode: Boolean) {
    val adminUsername by viewModel.adminUsername.collectAsState()
    val adminPassword by viewModel.adminPassword.collectAsState()
    val phonePeNumber by viewModel.adminPhonePeNumber.collectAsState()
    val phonePeUpi by viewModel.adminPhonePeUpi.collectAsState()
    val isSubscribed by viewModel.adminPaymentSubscribed.collectAsState()

    var usernameInput by remember(adminUsername) { mutableStateOf(adminUsername) }
    var passwordInput by remember(adminPassword) { mutableStateOf(adminPassword) }
    var phonePeInput by remember(phonePeNumber) { mutableStateOf(phonePeNumber) }
    var upiInput by remember(phonePeUpi) { mutableStateOf(phonePeUpi) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Subscription Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GATEWAY SUBSCRIPTION MANAGEMENT",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To allow cosmic customers to pay using modern PhonePe UPI or direct QR scanning, the Space Command portal requires an active subscription core.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSubscribed) {
                                    if (isDarkMode) Color(0x1500D4AA) else Color(0x1500A485)
                                } else {
                                    if (isDarkMode) Color(0x15FF5555) else Color(0x15DD0000)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (isSubscribed) {
                                        if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                    } else {
                                        if (isDarkMode) Color(0xFFFF5555) else Color(0xFFDD0000)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSubscribed) "SUBSCRIPTION ACTIVE" else "SUBSCRIPTION INACTIVE",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSubscribed) {
                                    if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                                } else {
                                    if (isDarkMode) Color(0xFFFF5555) else Color(0xFFDD0000)
                                }
                            )
                            Text(
                                text = if (isSubscribed) "Clients can check out using direct PhonePe UPI" else "PhonePe checkout core is disabled for customers",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.toggleAdminSubscription(!isSubscribed) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) {
                                if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            } else {
                                if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                            },
                            contentColor = if (isSubscribed) {
                                if (isDarkMode) Color.White else Color.Black
                            } else {
                                Color.White
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "DEACTIVATE GATEWAY SUBSCRIPTION" else "ACTIVATE LOGISTICS SUBSCRIPTION (FREE DEMO)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Credentials & PhonePe Config Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SkyDarkSurfaceVariant else Color.LightGray.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SPACE TERMINAL CONFIGURATION",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Security Authentication (Your Unique Admin Name)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Unique Admin Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Admin Console Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDarkMode) Color.Gray.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. PhonePe Payments Cargo Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phonePeInput,
                        onValueChange = { phonePeInput = it },
                        label = { Text("PhonePe Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("PhonePe UPI ID / QR Details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) SkyDarkAccent else SkyLightAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Visual Sandbox
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PREVIEW UPI CARGO COUPLING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Canvas(modifier = Modifier.size(100.dp)) {
                                drawRect(Color.White, size = size)
                                val sqSize = size.width / 10
                                for (i in 0..9) {
                                    for (j in 0..9) {
                                        val isAnchor = (i < 3 && j < 3) || (i > 6 && j < 3) || (i < 3 && j > 6)
                                        val strHash = (upiInput.hashCode() + i * 17 + j * 31).let { if (it < 0) -it else it }
                                        val fill = if (isAnchor) true else (strHash % 2 == 0)
                                        if (fill) {
                                            drawRect(
                                                color = Color(0xFF5f259f), // PhonePe Purple
                                                topLeft = Offset(i * sqSize, j * sqSize),
                                                size = androidx.compose.ui.geometry.Size(sqSize, sqSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PhonePe: $phonePeInput",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Text(
                            text = "UPI: $upiInput",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.updateAdminSettings(
                                usernameInput,
                                passwordInput,
                                phonePeInput,
                                upiInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF00D4AA) else Color(0xFF00A485)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE CONFIGURATION MODULES", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
