package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.local.ModelCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isKeySubmitted by viewModel.isApiKeySubmitted.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val showInstagramFollowPrompt by viewModel.showInstagramFollowPrompt.collectAsStateWithLifecycle()
    val lastUserQuery by viewModel.lastUserMessageQuery.collectAsStateWithLifecycle()

    // Handle standard toast notifications
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // Handle generic error warnings
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Show alert dialog if required, or let top notification bar handle it
        }
    }

    if (showInstagramFollowPrompt) {
        InstagramPromoDialog(
            query = lastUserQuery,
            onDismiss = { viewModel.dismissInstagramPrompt() }
        )
    }

    if (!isKeySubmitted) {
        // Render Api Key Submission Flow Screen
        ApiKeySubmissionScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        // Render Main Core Chat Experience with Drawer Backing
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(310.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    ChatDrawerContent(
                        viewModel = viewModel,
                        onCloseDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            MainChatContent(
                viewModel = viewModel,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = modifier
            )
        }
    }
}

/**
 * 1. API Key Entry Welcome Overlay Screen
 */
@Composable
fun ApiKeySubmissionScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    val isSearching by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Control setup popup
    var showSetupDialog by remember { mutableStateOf(true) }

    // Infinite breathing pulse for the core logo dots and connecting bridges
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Pure professional light theme
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant 3x3 grid drawing representing the "LLMGrid" logo
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                val gridColor = Color(0xFF1E293B)
                val activeColor = Color(0xFF0056C6)
                val dotRadius = 10f
                val spacing = size.width / 2f

                // Draw connecting lines
                for (i in 0..2) {
                    // Horizontal lines
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = androidx.compose.ui.geometry.Offset(0f, i * spacing),
                        end = androidx.compose.ui.geometry.Offset(size.width, i * spacing),
                        strokeWidth = 4f
                    )
                    // Vertical lines
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = androidx.compose.ui.geometry.Offset(i * spacing, 0f),
                        end = androidx.compose.ui.geometry.Offset(i * spacing, size.height),
                        strokeWidth = 4f
                    )
                }

                // Draw customized highlighted path line (as in the logo: active column on the left and active connection at bottom)
                drawLine(
                    color = activeColor,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, size.height),
                    strokeWidth = 6f * pulseScale
                )
                drawLine(
                    color = activeColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(spacing, size.height),
                    strokeWidth = 6f * pulseScale
                )

                // Draw 3x3 grid of dots with dynamic reactive pulse sizes on highlighted coordinates
                for (row in 0..2) {
                    for (col in 0..2) {
                        val isHighlighted = (col == 0) || (row == 2 && col == 1)
                        val finalRadius = if (isHighlighted) dotRadius * 1.35f * pulseScale else dotRadius
                        drawCircle(
                            color = if (isHighlighted) activeColor else gridColor,
                            radius = finalRadius,
                            center = androidx.compose.ui.geometry.Offset(col * spacing, row * spacing)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LLMGrid",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "LLMGrid - Your gateway to every LLM.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0056C6),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Unified core intelligence with zero latency. Secure local storage. Fast client backup integrations.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { showSetupDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("get_started_setup_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056C6))
            ) {
                Text(
                    text = "Configure API Key",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }

    // First time setup popup Dialog
    if (showSetupDialog) {
        Dialog(onDismissRequest = { showSetupDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secured Keys",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "LLMGrid API Key Setup",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter your custom API Key to start chatting with any model securely. Your key stays 100% offline in sandbox private storage.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.Info else Icons.Default.Lock,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    if (!errorMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.submitApiKey(keyInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_api_key_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.0.dp
                            )
                        } else {
                            Text(
                                text = "Connect Securely",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Don't have an API Key? Get one here",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Navigation Drawer Chat History Rooms Listing
 */
@Composable
fun ChatDrawerContent(
    viewModel: ChatViewModel,
    onCloseDrawer: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher for JSON restoration files
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val jsonText = stream.bufferedReader().use { it.readText() }
                    viewModel.importChatBackup(jsonText)
                }
            } catch (e: Exception) {
                viewModel.showToast("Failed to parse local file backup.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Identity Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEF2F6)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                ) {
                    val gridColor = Color(0xFF1E293B)
                    val activeColor = Color(0xFF0056C6)
                    val dotRadius = 2.5f
                    val spacing = size.width / 2f

                    // Lines
                    for (i in 0..2) {
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = androidx.compose.ui.geometry.Offset(0f, i * spacing),
                            end = androidx.compose.ui.geometry.Offset(size.width, i * spacing),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = androidx.compose.ui.geometry.Offset(i * spacing, 0f),
                            end = androidx.compose.ui.geometry.Offset(i * spacing, size.height),
                            strokeWidth = 1f
                        )
                    }

                    drawLine(
                        color = activeColor,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = activeColor,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(spacing, size.height),
                        strokeWidth = 2f
                    )

                    for (row in 0..2) {
                        for (col in 0..2) {
                            val isHighlighted = (col == 0) || (row == 2 && col == 1)
                            drawCircle(
                                color = if (isHighlighted) activeColor else gridColor,
                                radius = if (isHighlighted) dotRadius * 1.3f else dotRadius,
                                center = androidx.compose.ui.geometry.Offset(col * spacing, row * spacing)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "LLMGrid",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gateway To Every LLM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF0056C6)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create New Chat Room trigger button
        Button(
            onClick = {
                viewModel.createChatSession("New Discussion")
                onCloseDrawer()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("new_chat_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch New Chat", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PREVIOUS CONVERSATIONS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // List of previous conversations
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = "No stored chats yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                items(sessions) { session ->
                    SessionRowItem(
                        session = session,
                        isActive = (session.id == activeSessionId),
                        onClick = {
                            viewModel.selectSession(session.id)
                            onCloseDrawer()
                        },
                        onDelete = {
                            viewModel.deleteSession(session.id)
                        }
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Import backups button
        OutlinedButton(
            onClick = {
                try {
                    importLauncher.launch("application/json")
                } catch (e: Exception) {
                    viewModel.showToast("No compatible file explorer available.")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Backup (.json)", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Settings / Resets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.clearApiKey()
                    onCloseDrawer()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lock Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = {
                    viewModel.clearAllData()
                    viewModel.showToast("All databases cleared.")
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear All Chats",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        // Toggle Instagram updates switch settings row
        val isInstagramPromptEnabled by viewModel.isInstagramPromptEnabled.collectAsStateWithLifecycle()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setInstagramPromptEnabled(!isInstagramPromptEnabled) }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "Daily Tech Updates",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Suggest creators on Instagram after chats",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isInstagramPromptEnabled,
                onCheckedChange = { viewModel.setInstagramPromptEnabled(it) },
                modifier = Modifier.scale(0.85f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        DeveloperProfileCard()
    }
}

@Composable
fun DeveloperProfileCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "KR",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.developer_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Creator & AI Systems Lead",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Profile" else "Expand Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Let's innovate on intelligent custom AI models, specialized prompting databases, and modern systems solutions. Connect directly below!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.linkedin.com/in/krunal-rana/")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("linkedin_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x150A66C2),
                                contentColor = Color(0xFF0A66C2)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_linkedin),
                                contentDescription = "LinkedIn logo",
                                modifier = Modifier.size(13.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LinkedIn",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.instagram.com/meet.b2.ai/")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("instagram_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x15D62976),
                                contentColor = Color(0xFFD62976)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = "Instagram logo",
                                modifier = Modifier.size(13.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Instagram",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Row design representing previous Session Room item list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionRowItem(
    session: ChatSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(session.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showRenameDialog = true }
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = session.modelId.substringAfter("/"),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("Discussion Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotBlank()) {
                            // Trigger rename flow
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Rename Title")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 3. Main Chat Area Container Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatContent(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val rawInput by viewModel.inputText.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val apiCachedModels by viewModel.cachedModels.collectAsStateWithLifecycle()
    val activeModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val currentErrorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isModelSelectionOpen by remember { mutableStateOf(false) }
    var searchModelQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Smooth automatically scrolling to last list index on keyboard/message counts alterations
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isModelSelectionOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            val displayName = activeModelId.substringAfterLast("/")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Provider: " + activeModelId.substringBefore("/", "Unknown"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer menu")
                    }
                },
                actions = {
                    val sId = activeSessionId
                    if (sId != null) {
                        var isExportExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { isExportExpanded = true }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export backup options")
                            }
                            DropdownMenu(
                                expanded = isExportExpanded,
                                onDismissRequest = { isExportExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share Markdown (.md)") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isExportExpanded = false
                                        viewModel.exportMarkdown(sId) { markdownText ->
                                            shareString(context, markdownText, "chat_room_export.md")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share JSON Backup") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isExportExpanded = false
                                        viewModel.exportJson(sId) { jsonText ->
                                            shareString(context, jsonText, "chat_backup.json")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Chat Conversation") },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isExportExpanded = false
                                        viewModel.exportMarkdown(sId) { markdownText ->
                                            copyToClipboard(context, markdownText)
                                            viewModel.showToast("Copied to clipboard!")
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Global Error bar
            if (currentErrorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Error detail",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentErrorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearErrorMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Close error message",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Message scrolling view list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && !isSending) {
                    // Visual Empty greeting showcase!
                    EmptyWorkspaceHint(
                        activeModelId = activeModelId,
                        onQuickChipClick = { prompt ->
                            viewModel.updateInputText(prompt)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubbleItem(message = message)
                        }

                        // Playful bouncing dots animation in active sending states
                        if (isSending) {
                            item {
                                typingIndicatorItem()
                            }
                        }
                    }
                }
            }

            // Bottom prompt bar input trigger layout
            BottomPromptPillRow(
                messageText = rawInput,
                isSending = isSending,
                onValueChange = { viewModel.updateInputText(it) },
                onSendTrigger = { viewModel.sendMessage() }
            )
        }
    }

    // Modal select model sheet representation
    if (isModelSelectionOpen) {
        ModalBottomSheet(
            onDismissRequest = { isModelSelectionOpen = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select OpenRouter Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = { viewModel.refreshModels() }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh catalog models list")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchModelQuery,
                    onValueChange = { searchModelQuery = it },
                    placeholder = { Text("Filter models...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Favorite / Popular quick chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickModels = listOf(
                        "google/gemini-2.5-flash" to "Gemini 2.5 Flash",
                        "openai/gpt-4o-mini" to "GPT-4o Mini",
                        "meta-llama/llama-3-8b-instruct" to "Llama 3"
                    )
                    quickModels.forEach { (id, label) ->
                        val isSelected = activeModelId == id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectModel(id)
                                isModelSelectionOpen = false
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = if (searchModelQuery.isBlank()) {
                    apiCachedModels
                } else {
                    apiCachedModels.filter {
                        it.name.contains(searchModelQuery, ignoreCase = true) ||
                                it.id.contains(searchModelQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "No models cached or loaded yet. Run refresh or enter key first.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filtered) { model ->
                            val isChosen = model.id == activeModelId
                            Card(
                                onClick = {
                                    viewModel.selectModel(model.id)
                                    isModelSelectionOpen = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = model.id,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!model.description.isNullOrBlank()) {
                                            Text(
                                                text = model.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Ctx: ${model.contextLength / 1000}k",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "In/Out per M: \$${String.format("%.2f", model.pricingPrompt)} / \$${String.format("%.2f", model.pricingCompletion)}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
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
}

/**
 * Empty Chat welcome dashboard with quick chip triggers
 */
@Composable
fun EmptyWorkspaceHint(
    activeModelId: String,
    onQuickChipClick: (String) -> Unit
) {
    // Elegant pulsing animation for the empty state core intelligence logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(90.dp)
                .padding(8.dp)
        ) {
            val gridColor = Color(0xFF94A3B8)
            val activeColor = Color(0xFF0056C6)
            val dotRadius = 7.5f
            val spacing = size.width / 2f

            // Draw connecting lines
            for (i in 0..2) {
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = androidx.compose.ui.geometry.Offset(0f, i * spacing),
                    end = androidx.compose.ui.geometry.Offset(size.width, i * spacing),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = androidx.compose.ui.geometry.Offset(i * spacing, 0f),
                    end = androidx.compose.ui.geometry.Offset(i * spacing, size.height),
                    strokeWidth = 3f
                )
            }

            // Draw customized active highlight lines
            drawLine(
                color = activeColor,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                strokeWidth = 4.5f * pulseScale
            )
            drawLine(
                color = activeColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(spacing, size.height),
                strokeWidth = 4.5f * pulseScale
            )

            // Draw dots
            for (row in 0..2) {
                for (col in 0..2) {
                    val isHighlighted = (col == 0) || (row == 2 && col == 1)
                    val finalRadius = if (isHighlighted) dotRadius * 1.35f * pulseScale else dotRadius
                    drawCircle(
                        color = if (isHighlighted) activeColor else gridColor,
                        radius = finalRadius,
                        center = androidx.compose.ui.geometry.Offset(col * spacing, row * spacing)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "LLMGrid Intelligence",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Gateway routing model currently active:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = activeModelId,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0056C6),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "SUGGESTED PROMPTS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val promptSuggestions = listOf(
            Triple("Write a simple Android coroutine helper class or function.", Icons.Default.Build, Color(0xFF0056C6)),
            Triple("Explain the difference between Jetpack Compose Flow states and LiveData.", Icons.Default.Info, Color(0xFF0284C7)),
            Triple("Write a creative short science-fiction story about quantum space agents.", Icons.Default.Star, Color(0xFFD62976))
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)
        ) {
            promptSuggestions.forEach { (suggestion, icon, accentColor) ->
                Card(
                    onClick = { onQuickChipClick(suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chat Bubble item representation. Left for AI responses, Right for User queries.
 */
@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.role == "user"
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(message.id) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { if (isUser) it / 2 else -it / 2 },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
        ) + fadeIn(animationSpec = spring(stiffness = 300f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomStart = if (isUser) 10.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 10.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Parse and render basic formats (Bold, Lists, Code)
                    FormattedMarkdownText(text = message.content)
                }
            }
        }
    }
}

/**
 * Custom light markdown text formatter to clean chat display elements
 */
@Composable
fun FormattedMarkdownText(text: String) {
    // Standard splitting to clean simple elements like headers and ticks
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            if (line.startsWith("```")) {
                // Ignore raw tick block edges
            } else if (line.startsWith("- ")) {
                Row(modifier = Modifier.padding(start = 4.dp)) {
                    Text(text = "• ", fontWeight = FontWeight.Bold)
                    Text(text = line.removePrefix("- "), fontSize = 14.sp)
                }
            } else if (line.startsWith("#")) {
                val headerLevel = line.takeWhile { it == '#' }.length
                val textOnly = line.trimStart('#').trim()
                Text(
                    text = textOnly,
                    fontWeight = FontWeight.Black,
                    fontSize = if (headerLevel == 1) 18.sp else 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            } else {
                Text(
                    text = line,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Bouncing active typing indicator visual representation
 */
@Composable
fun typingIndicatorItem() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 2.dp, bottomEnd = 10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.width(72.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val cycle = remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        cycle.value = (cycle.value + 1) % 3
                        delay(250)
                    }
                }
                repeat(3) { index ->
                    val isActive = cycle.value == index
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Bottom inputs prompting bar
 */
@Composable
fun BottomPromptPillRow(
    messageText: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSendTrigger: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onValueChange,
                placeholder = { Text("Ask anything...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(onSend = {
                    if (messageText.isNotBlank() && !isSending) {
                        onSendTrigger()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                })
            )

            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank() && !isSending) {
                        onSendTrigger()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                containerColor = if (messageText.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (messageText.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_button"),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send request",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -- Text share Intent utility helpers --

private fun shareString(context: Context, text: String, name: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export chat file: $name")
    context.startActivity(shareIntent)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Chat", text)
    clipboard.setPrimaryClip(clip)
}

@Composable
fun InstagramPromoDialog(
    query: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gradient Ring Instagram Logo Ring Representation
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFEDA75),
                                    Color(0xFFFA7E1E),
                                    Color(0xFFD62976),
                                    Color(0xFF962FBF),
                                    Color(0xFF4F5BD5)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Daily AI & Tech Insights",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getPromoMessageForQuery(query),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.instagram.com/meet.b2.ai/")
                        )
                        context.startActivity(intent)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD62976),
                        contentColor = Color.White
                    )
                ) {
                    Text("Follow @meet.b2.ai", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.height(36.dp)) {
                    Text("Maybe Later", color = MaterialTheme.colorScheme.outline)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You can disable these update prompts anytime inside the application Settings menu.",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

fun getPromoMessageForQuery(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("code") || q.contains("program") || q.contains("develop") || q.contains("kotlin") || q.contains("java") || q.contains("android") || q.contains("api") -> {
            "Level up your coding skills! Follow @meet.b2.ai on Instagram for expert Android advice, custom AI systems engineering, and fast prompting tutorials."
        }
        q.contains("write") || q.contains("essay") || q.contains("email") || q.contains("summary") || q.contains("text") || q.contains("creative") -> {
            "Boost your creative flow! Follow @meet.b2.ai on Instagram for productivity shortcuts, content creation tips, and cutting-edge automation."
        }
        q.contains("business") || q.contains("startup") || q.contains("job") || q.contains("resume") || q.contains("interview") || q.contains("work") -> {
            "Supercharge your professional growth! Follow @meet.b2.ai on Instagram to unlock career Hacks, business optimization ideas, and real-case AI examples."
        }
        else -> {
            "Stay ahead of the AI revolution! Follow @meet.b2.ai on Instagram for daily updates, intelligent model releases, and fast tech tips."
        }
    }
}
