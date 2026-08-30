package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.domain.model.AiMessage
import com.cashflow.ai.domain.model.AiRole
import com.cashflow.ai.domain.repository.TransactionRepository
import com.cashflow.ai.domain.usecase.ai.AnswerFinancialQuestionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class AiChatViewModel(
    private val transactionRepository: TransactionRepository,
    private val answerFinancialQuestionUseCase: AnswerFinancialQuestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var nextMessageId = 1L

    init {
        // Welcome message from the assistant
        addAssistantMessage(
            "👋 Hi! I'm your AI Finance Assistant.\n\n" +
                "Ask me anything about your money — for example:\n" +
                "• \"How much did I spend on coffee this month?\"\n" +
                "• \"Compare last month vs this month\"\n" +
                "• \"What's my top expense category?\"\n" +
                "• \"Predict my spending for next month\""
        )
    }

    fun sendQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + AiMessage(
                id = nextMessageId++,
                role = AiRole.USER,
                text = trimmed
            ),
            isProcessing = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val summary = transactionRepository.getSummary().first()

                val currency = summary.currency ?: com.cashflow.ai.domain.model.Currency.IDR
                val result = answerFinancialQuestionUseCase(trimmed, transactions, currency)

                addAssistantMessage(result.answer, result.isOffTopic)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to answer your question."
                )
            }
        }
    }

    fun clearChat() {
        _uiState.value = AiChatUiState(
            messages = listOf(
                AiMessage(
                    id = nextMessageId++,
                    role = AiRole.ASSISTANT,
                    text = "Chat cleared. Ask me anything about your finances! 💰"
                )
            )
        )
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun addAssistantMessage(text: String, isOffTopicNotice: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + AiMessage(
                id = nextMessageId++,
                role = AiRole.ASSISTANT,
                text = text
            ),
            isProcessing = false
        )
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val answerFinancialQuestionUseCase: AnswerFinancialQuestionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiChatViewModel(transactionRepository, answerFinancialQuestionUseCase) as T
        }
    }
}