package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.local.KeyManager
import com.example.data.local.ModelCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Welcome : UiState
    data object ActiveChat : UiState
}

class ChatViewModel(
    private val repository: ChatRepository,
    private val keyManager: KeyManager
) : ViewModel() {

    // API Key State
    private val _apiKey = MutableStateFlow<String?>(keyManager.getApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    val isApiKeySubmitted: StateFlow<Boolean> = _apiKey
        .combine(flowOf(true)) { key, _ -> !key.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), !keyManager.getApiKey().isNullOrBlank())

    // Instagram Prompt states
    private val _isInstagramPromptEnabled = MutableStateFlow(keyManager.isInstagramPromptEnabled())
    val isInstagramPromptEnabled: StateFlow<Boolean> = _isInstagramPromptEnabled.asStateFlow()

    private val _showInstagramFollowPrompt = MutableStateFlow(false)
    val showInstagramFollowPrompt: StateFlow<Boolean> = _showInstagramFollowPrompt.asStateFlow()

    private val _lastUserMessageQuery = MutableStateFlow("")
    val lastUserMessageQuery: StateFlow<String> = _lastUserMessageQuery.asStateFlow()

    fun setInstagramPromptEnabled(enabled: Boolean) {
        keyManager.setInstagramPromptEnabled(enabled)
        _isInstagramPromptEnabled.value = enabled
    }

    fun dismissInstagramPrompt() {
        _showInstagramFollowPrompt.value = false
    }

    // App Navigation state (Local dynamic view matching user intent)
    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId.asStateFlow()

    // Model selection
    private val _selectedModelId = MutableStateFlow(keyManager.getSelectedModelId())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    // Chat sessions from database
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active session details
    private val _activeSession = _activeSessionId.flatMapLatest { id ->
        if (id != null) {
            // Retrieve session details from database
            kotlinx.coroutines.flow.flow {
                val s = repository.getSession(id)
                emit(s)
            }
        } else {
            flowOf<ChatSession?>(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current messages in active session
    val messages: StateFlow<List<ChatMessage>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) {
            repository.getMessagesForSession(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Models Cache Flow
    val cachedModels: StateFlow<List<ModelCache>> = repository.allCachedModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Input field state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Operations states
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Fetch models from OpenRouter to initialize cache on start
        refreshModels()
    }

    fun submitApiKey(key: String) {
        if (key.isNotBlank()) {
            keyManager.saveApiKey(key)
            _apiKey.value = key.trim()
            _errorMessage.value = null
            _toastMessage.value = "API Key saved successfully!"
            refreshModels()
        } else {
            _errorMessage.value = "API key cannot be empty"
        }
    }

    fun clearApiKey() {
        keyManager.clearApiKey()
        _apiKey.value = null
        _activeSessionId.value = null
        _toastMessage.value = "API Key cleared locally."
    }

    fun selectSession(sessionId: Int?) {
        _activeSessionId.value = sessionId
        _inputText.value = ""
        _errorMessage.value = null
        viewModelScope.launch {
            if (sessionId != null) {
                repository.getSession(sessionId)?.let { s ->
                    _selectedModelId.value = s.modelId
                    keyManager.saveSelectedModelId(s.modelId)
                }
            }
        }
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
        keyManager.saveSelectedModelId(modelId)
        viewModelScope.launch {
            val sId = _activeSessionId.value
            if (sId != null) {
                repository.updateSessionModel(sId, modelId)
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun createChatSession(title: String) {
        viewModelScope.launch {
            try {
                val finalTitle = title.ifBlank { "New Chat Sessions" }
                val sessionId = repository.createNewSession(finalTitle, _selectedModelId.value)
                selectSession(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create session: ${e.localizedMessage}"
            }
        }
    }

    fun renameSession(sessionId: Int, newName: String) {
        viewModelScope.launch {
            try {
                if (newName.isNotBlank()) {
                    repository.updateSessionTitle(sessionId, newName)
                    _toastMessage.value = "Session renamed!"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteSession(sessionId)
                if (_activeSessionId.value == sessionId) {
                    _activeSessionId.value = null
                }
                _toastMessage.value = "Session deleted."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete session."
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _activeSessionId.value = null
                _toastMessage.value = "All conversations cleared."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear chats: ${e.localizedMessage}"
            }
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val sId = _activeSessionId.value
        val key = _apiKey.value

        if (text.isEmpty()) return

        if (key.isNullOrBlank()) {
            _errorMessage.value = "API key missing! Please check settings."
            return
        }

        if (sId == null) {
            // Auto create session if sending outside an active session, then send
            viewModelScope.launch {
                try {
                    val fallbackTitle = if (text.length > 25) text.take(25) + "..." else text
                    val newSId = repository.createNewSession(fallbackTitle, _selectedModelId.value)
                    _activeSessionId.value = newSId
                    _inputText.value = ""
                    executeSend(newSId, text, key, _selectedModelId.value)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to auto-create session: ${e.localizedMessage?.trim() ?: e.toString()}"
                }
            }
        } else {
            _inputText.value = ""
            viewModelScope.launch {
                executeSend(sId, text, key, _selectedModelId.value)
            }
        }
    }

    private suspend fun executeSend(sessionId: Int, text: String, key: String, modelId: String) {
        _isSending.value = true
        _errorMessage.value = null
        val result = repository.sendMessage(sessionId, text, key, modelId)
        _isSending.value = false
        if (result.isFailure) {
            _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "API Request Failed"
            // Restore input text so User does not lose their typed prompt!
            _inputText.value = text
        } else {
            // SUCCESS: Trigger Instagram Follow Popup if the filter is turned on
            if (keyManager.isInstagramPromptEnabled()) {
                _lastUserMessageQuery.value = text
                _showInstagramFollowPrompt.value = true
            }
        }
    }

    fun refreshModels() {
        // Prevent simultaneous triggers
        if (_isLoadingModels.value) return

        viewModelScope.launch {
            _isLoadingModels.value = true
            val result = repository.fetchAndCacheModels()
            _isLoadingModels.value = false
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Network query failure"
                // If it's a 401/Invalid key error, inform the user
                if (error.contains("401", ignoreCase = true) || error.contains("Forbidden", ignoreCase = true)) {
                    _toastMessage.value = "Model fetch failed (Verify API Key)."
                } else {
                    _toastMessage.value = "Offline mode active (used cached models)."
                }
            } else {
                _toastMessage.value = "Models updated."
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    // -- Export Actions --

    fun exportMarkdown(sessionId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val md = repository.exportMarkdown(sessionId)
            onResult(md)
        }
    }

    fun exportJson(sessionId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportJson(sessionId)
            onResult(json)
        }
    }

    // -- Import Action --

    fun importChatBackup(jsonContent: String) {
        viewModelScope.launch {
            val result = repository.importJson(jsonContent)
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                selectSession(newId)
                _toastMessage.value = "Chat imported successfully!"
            } else {
                _errorMessage.value = "Failed to import chat: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val keyManager: KeyManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, keyManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
