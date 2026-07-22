package top.etta.aerie.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.etta.aerie.BuildConfig
import top.etta.aerie.R
import top.etta.aerie.data.chat.ChatConnectionStatus
import top.etta.aerie.data.chat.ChatMessage
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.session.LoginInput
import top.etta.aerie.data.session.SessionState
import top.etta.aerie.data.session.UserRole

@Composable
fun AerieApp(viewModel: AerieViewModel) {
    val sessionState by viewModel.session.collectAsStateWithLifecycle()
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onForeground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val current = sessionState) {
        SessionState.SignedOut -> LoginScreen(
            loginUiState = loginUiState,
            onLogin = viewModel::login,
            onPreview = viewModel::enterPreview,
        )
        is SessionState.SignedIn -> MainShell(
            displayName = current.session.displayName,
            role = current.session.role,
            isLocalPreview = current.session.isLocalPreview,
            onLogout = viewModel::logout,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun LoginScreen(
    loginUiState: LoginUiState,
    onLogin: (LoginInput) -> Unit,
    onPreview: (UserRole) -> Unit,
) {
    var serverUrl by remember { mutableStateOf(BuildConfig.DEFAULT_SERVER_URL) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("VIVO Y500 Pro") }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.aerie_mark),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Aerie 云栖",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "等待服务器认证",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    leadingIcon = {
                        Icon(Icons.Default.PersonOutline, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it.filter(Char::isDigit).take(8) },
                    label = { Text("八位配对码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("设备名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            loginUiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        onLogin(
                            LoginInput(
                                serverUrl = serverUrl.trim(),
                                username = username.trim(),
                                password = password,
                                pairingCode = pairingCode,
                                deviceName = deviceName.trim(),
                            ),
                        )
                    },
                    enabled = !loginUiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (loginUiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("登录并配对")
                    }
                }
            }
            if (BuildConfig.ALLOW_LOCAL_PREVIEW) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onPreview(UserRole.Owner) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("主账号预览")
                        }
                        OutlinedButton(
                            onClick = { onPreview(UserRole.Guest) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("访客预览")
                        }
                    }
                }
            }
        }
    }
}

private data class Destination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    displayName: String,
    role: UserRole,
    isLocalPreview: Boolean,
    onLogout: () -> Unit,
    viewModel: AerieViewModel,
) {
    val destinations = remember {
        listOf(
            Destination("聊天", Icons.Default.ChatBubbleOutline),
            Destination("任务", Icons.Default.TaskAlt),
            Destination("文件", Icons.Default.FolderOpen),
            Destination("设置", Icons.Default.MoreHoriz),
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aerie 云栖", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (role == UserRole.Owner) "主账号 · $displayName" else "访客 · $displayName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "退出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedIndex) {
            0 -> ChatScreen(innerPadding, isLocalPreview, viewModel)
            1 -> TaskScreen(innerPadding, isLocalPreview, viewModel)
            2 -> EmptyOperationalScreen(innerPadding, "暂无可用文件")
            else -> SettingsScreen(innerPadding, role, isLocalPreview, viewModel)
        }
    }
}

@Composable
private fun ChatScreen(
    contentPadding: PaddingValues,
    isLocalPreview: Boolean,
    viewModel: AerieViewModel,
) {
    var draft by remember { mutableStateOf("") }
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val pending by viewModel.pendingOutbound.collectAsStateWithLifecycle()
    val connection by viewModel.chatConnection.collectAsStateWithLifecycle()
    val action by viewModel.chatActionState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (isLocalPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本地预览",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        if (!isLocalPreview) {
            ConnectionBanner(connection.status, connection.retryDelaySeconds)
        }
        pending.forEach { item ->
            PendingConfirmationRow(
                pending = item,
                enabled = !action.isBusy,
                onConfirm = { viewModel.confirmPending(item.clientRequestId) },
            )
        }
        if (action.errorMessage != null) {
            Text(
                text = action.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isLocalPreview && connection.status == ChatConnectionStatus.Connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        text = if (isLocalPreview) "本地预览暂无消息" else "暂无消息",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.messageId }) { message ->
                    MessageBubble(message)
                }
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(20_000) },
                placeholder = { Text("发送消息") },
                modifier = Modifier.weight(1f),
                maxLines = 5,
            )
            IconButton(
                onClick = {
                    val content = draft
                    draft = ""
                    viewModel.sendMessage(content)
                },
                enabled = draft.isNotBlank() && !isLocalPreview && !action.isBusy,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }
    }
}

@Composable
private fun ConnectionBanner(
    status: ChatConnectionStatus,
    retryDelaySeconds: Int?,
) {
    val label = when (status) {
        ChatConnectionStatus.Idle -> "等待连接"
        ChatConnectionStatus.Connecting -> "正在连接电脑"
        ChatConnectionStatus.Connected -> "已连接电脑"
        ChatConnectionStatus.Reconnecting ->
            "连接中断${retryDelaySeconds?.let { "，${it} 秒后重试" } ?: "，准备重试"}"
        ChatConnectionStatus.Offline -> "电脑端暂时不可用"
    }
    val tint = when (status) {
        ChatConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        ChatConnectionStatus.Offline -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        ChatConnectionStatus.Connected -> Icons.Default.Check
        ChatConnectionStatus.Connecting, ChatConnectionStatus.Reconnecting -> Icons.Default.Sync
        else -> Icons.Default.CloudOff
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun PendingConfirmationRow(
    pending: PendingOutbound,
    enabled: Boolean,
    onConfirm: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "待确认：${pending.text}",
                modifier = Modifier.weight(1f),
                maxLines = 2,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onConfirm, enabled = enabled) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("确认发送")
            }
        }
    }
}

@Composable
private fun TaskScreen(
    contentPadding: PaddingValues,
    isLocalPreview: Boolean,
    viewModel: AerieViewModel,
) {
    val requests by viewModel.chatRequests.collectAsStateWithLifecycle()
    val action by viewModel.chatActionState.collectAsStateWithLifecycle()
    if (isLocalPreview || requests.isEmpty()) {
        EmptyOperationalScreen(contentPadding, if (isLocalPreview) "本地预览暂无任务" else "暂无执行中的任务")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(requests, key = { it.requestId }) { request ->
            RequestRow(
                request = request,
                enabled = !action.isBusy,
                onCancel = { viewModel.cancelRequest(request.requestId) },
                onRetry = { viewModel.retryRequest(request.requestId) },
            )
        }
    }
}

@Composable
private fun RequestRow(
    request: ChatRequestRecord,
    enabled: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val active = request.status in setOf("queued", "running", "cancel_requested")
    val retryable = request.status in setOf("failed", "cancelled")
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = requestStatusLabel(request.status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                request.errorCode?.let { code ->
                    Text(
                        text = "错误：$code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = request.requestId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (active) {
                IconButton(onClick = onCancel, enabled = enabled) {
                    Icon(Icons.Default.Close, contentDescription = "取消任务")
                }
            } else if (retryable) {
                IconButton(onClick = onRetry, enabled = enabled) {
                    Icon(Icons.Default.Refresh, contentDescription = "重试任务")
                }
            }
        }
    }
}

private fun requestStatusLabel(status: String): String = when (status) {
    "queued" -> "排队中"
    "running" -> "执行中"
    "cancel_requested" -> "正在取消"
    "completed" -> "已完成"
    "failed" -> "执行失败"
    "cancelled" -> "已取消"
    else -> status
}

@Composable
private fun EmptyOperationalScreen(
    contentPadding: PaddingValues,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SettingsScreen(
    contentPadding: PaddingValues,
    role: UserRole,
    isLocalPreview: Boolean,
    viewModel: AerieViewModel,
) {
    val action by viewModel.chatActionState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("账号", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(if (role == UserRole.Owner) "主账号" else "访客账号")
        Text(
            text = if (isLocalPreview) "本地预览会话" else "已认证设备会话",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isLocalPreview) {
            Button(
                onClick = viewModel::synchronizeChat,
                enabled = !action.isBusy,
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("立即同步")
            }
        }
    }
}
