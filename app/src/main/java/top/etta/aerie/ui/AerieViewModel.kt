package top.etta.aerie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.etta.aerie.di.AppContainer
import top.etta.aerie.data.chat.ChatConnectionState
import top.etta.aerie.data.chat.ChatMessage
import top.etta.aerie.data.chat.ChatOperationResult
import top.etta.aerie.data.chat.ChatRepository
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.session.LoginInput
import top.etta.aerie.data.session.LoginResult
import top.etta.aerie.data.session.SessionRepository
import top.etta.aerie.data.session.SessionState
import top.etta.aerie.data.session.UserRole
import top.etta.aerie.sync.ForegroundSyncController
import top.etta.aerie.sync.ForegroundSyncCapability
import top.etta.aerie.sync.NoOpForegroundSyncController
import top.etta.aerie.sync.foregroundWorkState

data class LoginUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

data class ChatActionUiState(
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val awaitingConfirmationId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AerieViewModel(
    private val sessionRepository: SessionRepository,
    private val chatRepository: ChatRepository,
    private val foregroundSyncController: ForegroundSyncController =
        NoOpForegroundSyncController,
) : ViewModel() {
    val session = sessionRepository.session
    val foregroundSyncCapability: StateFlow<ForegroundSyncCapability> =
        foregroundSyncController.capability

    private val mutableLoginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = mutableLoginUiState.asStateFlow()

    private val activeRemoteAccount: Flow<String?> = sessionRepository.session
        .map { state ->
            (state as? SessionState.SignedIn)?.session
                ?.takeUnless { it.isLocalPreview }
                ?.accountId
        }
        .distinctUntilChanged()

    val chatMessages: StateFlow<List<ChatMessage>> = activeRemoteAccount
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf<List<ChatMessage>>(emptyList())
            } else {
                chatRepository.observeMessages(accountId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chatRequests: StateFlow<List<ChatRequestRecord>> = activeRemoteAccount
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf<List<ChatRequestRecord>>(emptyList())
            } else {
                chatRepository.observeRequests(accountId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingOutbound: StateFlow<List<PendingOutbound>> = activeRemoteAccount
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf<List<PendingOutbound>>(emptyList())
            } else {
                chatRepository.observePending(accountId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chatConnection: StateFlow<ChatConnectionState> = activeRemoteAccount
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(ChatConnectionState())
            } else {
                chatRepository.observeConnection(accountId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatConnectionState())

    private val mutableChatActionState = MutableStateFlow(ChatActionUiState())
    val chatActionState: StateFlow<ChatActionUiState> = mutableChatActionState.asStateFlow()
    private var foregroundSyncJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.restoreSession()
        }
        viewModelScope.launch {
            sessionRepository.session
                .map { state ->
                    (state as? SessionState.SignedIn)?.session
                        ?.takeUnless { it.isLocalPreview }
                }
                .distinctUntilChanged()
                .collectLatest { activeSession ->
                    if (activeSession != null) {
                        try {
                            chatRepository.runEventStream(activeSession.accountId)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            // The repository exposes recoverable connection state; keep the
                            // session collector alive if an unexpected client error escapes.
                        }
                    }
                }
        }
        viewModelScope.launch {
            combine(chatRequests, pendingOutbound, ::foregroundWorkState)
                .distinctUntilChanged()
                .collect { state -> foregroundSyncController.update(state) }
        }
    }

    fun login(input: LoginInput) {
        if (mutableLoginUiState.value.isSubmitting) return
        viewModelScope.launch {
            mutableLoginUiState.value = LoginUiState(isSubmitting = true)
            mutableLoginUiState.value = when (val result = sessionRepository.login(input)) {
                LoginResult.Success -> LoginUiState()
                is LoginResult.Failure -> LoginUiState(errorMessage = result.message)
            }
        }
    }

    fun enterPreview(role: UserRole) {
        sessionRepository.enterLocalPreview(role)
        mutableLoginUiState.value = LoginUiState()
        mutableChatActionState.value = ChatActionUiState()
    }

    fun sendMessage(text: String) {
        performChatAction { accountId -> chatRepository.submit(accountId, text) }
    }

    fun confirmPending(clientRequestId: String) {
        performChatAction { accountId ->
            chatRepository.confirmPending(accountId, clientRequestId)
        }
    }

    fun cancelRequest(requestId: String) {
        performChatAction { accountId -> chatRepository.cancel(accountId, requestId) }
    }

    fun retryRequest(requestId: String) {
        performChatAction { accountId -> chatRepository.retry(accountId, requestId) }
    }

    fun synchronizeChat() {
        val accountId = currentRemoteAccountId() ?: return
        if (mutableChatActionState.value.isBusy) return
        viewModelScope.launch {
            mutableChatActionState.value = ChatActionUiState(isBusy = true)
            mutableChatActionState.value = when (val result = chatRepository.synchronize(accountId)) {
                is ChatOperationResult.Success -> ChatActionUiState()
                is ChatOperationResult.AwaitingConfirmation ->
                    ChatActionUiState(awaitingConfirmationId = result.clientRequestId)
                is ChatOperationResult.Failure ->
                    ChatActionUiState(errorMessage = result.message)
            }
        }
    }

    fun onForeground() {
        val accountId = currentRemoteAccountId() ?: return
        foregroundSyncJob?.cancel()
        foregroundSyncJob = viewModelScope.launch {
            sessionRepository.refreshAccessToken()
            if (currentRemoteAccountId() == accountId) {
                chatRepository.synchronize(accountId)
            }
        }
    }

    fun clearChatError() {
        mutableChatActionState.value = mutableChatActionState.value.copy(errorMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            sessionRepository.logout()
        }
    }

    private fun performChatAction(action: suspend (String) -> ChatOperationResult) {
        if (mutableChatActionState.value.isBusy) return
        val accountId = currentRemoteAccountId()
        if (accountId == null) {
            mutableChatActionState.value = ChatActionUiState(errorMessage = "当前会话不支持网络聊天")
            return
        }
        viewModelScope.launch {
            mutableChatActionState.value = ChatActionUiState(isBusy = true)
            mutableChatActionState.value = when (val result = action(accountId)) {
                is ChatOperationResult.Success -> ChatActionUiState()
                is ChatOperationResult.AwaitingConfirmation ->
                    ChatActionUiState(awaitingConfirmationId = result.clientRequestId)
                is ChatOperationResult.Failure ->
                    ChatActionUiState(errorMessage = result.message)
            }
        }
    }

    private fun currentRemoteAccountId(): String? =
        (sessionRepository.session.value as? SessionState.SignedIn)?.session
            ?.takeUnless { it.isLocalPreview }
            ?.accountId
}

class AerieViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AerieViewModel::class.java))
        return AerieViewModel(
            sessionRepository = appContainer.sessionRepository,
            chatRepository = appContainer.chatRepository,
            foregroundSyncController = appContainer.foregroundSyncController,
        ) as T
    }
}
