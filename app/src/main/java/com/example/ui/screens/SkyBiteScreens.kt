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
import androidx.compose.material.icons.automirrored.filled.List
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
                    text = "Pilot, login to dispatch your meal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Quantum Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
                    label = { Text("Access Code") },
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

                TextButton(
                    onClick = { viewModel.navigateTo(Screen.FORGOT_PASSWORD) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Emergency access key lost?",
                        color = if (isDarkMode) SkyDarkAccent else SkyLightAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SkyBiteButton(
                    text = "INITIATE LAUNCH",
                    onClick = { viewModel.loginUser(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.RocketLaunch,
                    testTag = "login_button"
                )

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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
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
                    label = { Text("Quantum Email") },
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Access Code") },
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
                    onClick = { viewModel.registerUser(name, email, phone, password) },
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
                    text = "Request Emergency Core Link",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 22.sp),
                    color = if (isDarkMode) SkyDarkTextPrimary else SkyLightTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "A hyper-spatial beam link will be fired to reset your security password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) SkyDarkTextSecondary else SkyLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Quantum Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = sleekTextFieldColors(isDarkMode)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SkyBiteButton(
                    text = "FIRE BEAM RESET",
                    onClick = { viewModel.forgotPassword(email) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.SettingsBackupRestore,
                    testTag = "forgot_submit_button"
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { viewModel.navigateTo(Screen.LOGIN) }) {
                    Text(
                        text = "Recalibrate Login Space",
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
                        Icons.Default.ArrowForward,
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
                            icon = Icons.Default.Launch,
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
                        PaymentSelectorChip(selectedPayment == "PAYPAL", "PAYPAL", Icons.Default.Payment, "PayPal", isDarkMode) { selectedPayment = "PAYPAL" }
                        PaymentSelectorChip(selectedPayment == "UPI", "UPI", Icons.Default.QrCode, "UPI / QR", isDarkMode) { selectedPayment = "UPI" }
                        PaymentSelectorChip(selectedPayment == "COD", "COD", Icons.Default.CurrencyExchange, "Cash (COD)", isDarkMode) { selectedPayment = "COD" }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedPayment) {
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
                text = "CONFIRM SPACE DISPATCH",
                onClick = { viewModel.placeOrder(address, city, pincode, phone, selectedPayment) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Launch,
                testTag = "place_order_btn"
            )
            Spacer(modifier = Modifier.height(24.dp))
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
                    TelemetryCanvasMap(progress)

                    // Overlay metrics
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("DRONE FREQUENCY: ACTIVE", color = Color(0xFF00D4AA), style = MaterialTheme.typography.labelSmall)
                        Text("EST SPEED: 72 KM/H", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("ALTITUDE: 120M", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }

                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("GPS: LOCKED", color = Color(0xFF00D4AA), style = MaterialTheme.typography.labelSmall)
                        Text("BEAM ANGLE: 42°", color = Color.White, style = MaterialTheme.typography.labelSmall)
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

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = progress,
                        color = Color(0xFF00D4AA),
                        trackColor = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

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
                            Text("100% QUANTUM", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00D4AA))
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
}

// Stunning Custom Radar Mapping Canvas
@Composable
fun TelemetryCanvasMap(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)

        // Draw radar grids
        for (i in 1..4) {
            drawCircle(
                color = Color(0x1000D4AA),
                radius = (width / 7) * i,
                style = Stroke(width = 2f)
            )
        }

        // Radar coordinates axes lines
        drawLine(Color(0x1500D4AA), start = Offset(0f, height/2), end = Offset(width, height/2), strokeWidth = 1f)
        drawLine(Color(0x1500D4AA), start = Offset(width/2, 0f), end = Offset(width/2, height), strokeWidth = 1f)

        // Radar Sweep Green Gradient Cone
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEndX = center.x + (width / 2) * cos(sweepRad).toFloat()
        val sweepEndY = center.y + (width / 2) * sin(sweepRad).toFloat()
        drawLine(
            color = Color(0x6000D4AA),
            start = center,
            end = Offset(sweepEndX, sweepEndY),
            strokeWidth = 4f
        )

        // Flight route coordinates (Restaurant to Customer)
        val routeStart = Offset(width * 0.15f, height * 0.8f)
        val routeEnd = Offset(width * 0.85f, height * 0.2f)

        // Draw flight route dotted path
        drawLine(
            color = Color(0x30FFFFFF),
            start = routeStart,
            end = routeEnd,
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Dynamic Drone Coordinate on Route
        val droneX = routeStart.x + (routeEnd.x - routeStart.x) * progress
        val droneY = routeStart.y + (routeEnd.y - routeStart.y) * progress
        val dronePos = Offset(droneX, droneY)

        // Draw Restaurant Dot
        drawCircle(
            color = Color(0xFF00D4AA),
            radius = 12f,
            center = routeStart
        )
        drawCircle(
            color = Color(0xFF00D4AA),
            radius = 24f,
            center = routeStart,
            style = Stroke(width = 2f)
        )

        // Draw Customer Hangar Dot
        drawCircle(
            color = Color(0xFFFF6B35),
            radius = 12f,
            center = routeEnd
        )
        // Pulsing radar ring on customer
        val pulseRadius = 24f + (16f * sin((System.currentTimeMillis() % 1000) / 1000f * 2 * Math.PI).toFloat())
        drawCircle(
            color = Color(0x50FF6B35),
            radius = pulseRadius,
            center = routeEnd,
            style = Stroke(width = 2f)
        )

        // Draw Drone Vector Icon / Dot
        drawCircle(
            color = Color.White,
            radius = 14f,
            center = dronePos
        )
        drawCircle(
            color = Color(0xFFFF6B35),
            radius = 8f,
            center = dronePos
        )
        // Draw drone halo
        drawCircle(
            color = Color(0x40FFFFFF),
            radius = 22f,
            center = dronePos,
            style = Stroke(width = 3f)
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
                Icon(Icons.Default.Logout, contentDescription = null)
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
