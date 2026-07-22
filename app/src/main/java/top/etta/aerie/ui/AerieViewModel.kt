package top.etta.aerie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.etta.aerie.di.AppContainer
import top.etta.aerie.data.session.LoginInput
import top.etta.aerie.data.session.LoginResult
import top.etta.aerie.data.session.SessionRepository
import top.etta.aerie.data.session.UserRole

data class LoginUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

class AerieViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val session = sessionRepository.session

    private val mutableLoginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = mutableLoginUiState.asStateFlow()

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
    }

    fun logout() {
        sessionRepository.logout()
    }
}

class AerieViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AerieViewModel::class.java))
        return AerieViewModel(appContainer.sessionRepository) as T
    }
}
