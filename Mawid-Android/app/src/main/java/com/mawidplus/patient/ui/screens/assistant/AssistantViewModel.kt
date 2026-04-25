package com.mawidplus.patient.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.data.dto.ChatTurnDto
import com.mawidplus.patient.data.dto.RecommendedDoctorDto
import com.mawidplus.patient.data.repository.AssistantRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessageUi(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val recommendedDoctors: List<RecommendedDoctorDto> = emptyList(),
    val severity: String? = null,
    val summary: String? = null,
    val isError: Boolean = false,
    val retryText: String? = null,
)

class AssistantViewModel(
    private val repository: AssistantRepository = AssistantRepository(),
) : ViewModel() {

    companion object {
        private const val MAX_USER_MESSAGE_LEN = 4000
    }

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(
        listOf(
            ChatMessageUi(
                isUser = false,
                text = "أهلاً بك في مساعد موعد+ الذكي.\n" +
                    "كيف أقدر أساعدك اليوم؟ وصف لي ايش تحس فيه.",
            ),
        ),
    )
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _quickReplies = MutableStateFlow<List<String>>(emptyList())
    val quickReplies: StateFlow<List<String>> = _quickReplies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** آخر ملخص للحالة من الـ LLM */
    private val _caseSummary = MutableStateFlow<String?>(null)
    val caseSummary: StateFlow<String?> = _caseSummary.asStateFlow()

    /** آخر مستوى خطورة من الـ LLM */
    private val _severity = MutableStateFlow<String?>(null)
    val severity: StateFlow<String?> = _severity.asStateFlow()

    /** آخر رسالة مستخدم أُرسلت إلى الـ API (لإعادة المحاولة) */
    private var _lastUserMessage: String? = null

    private fun historyBeforeSend(): List<ChatTurnDto> = _messages.value.map { m ->
        ChatTurnDto(
            role = if (m.isUser) "user" else "assistant",
            content = m.text,
        )
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return
        if (trimmed.length > MAX_USER_MESSAGE_LEN) return

        _lastUserMessage = trimmed
        val historyPayload = historyBeforeSend()
        _messages.update { it + ChatMessageUi(isUser = true, text = trimmed) }
        _quickReplies.value = emptyList()
        _isLoading.value = true
        viewModelScope.launch {
            when (val r = repository.chat(trimmed, _sessionId.value, historyPayload)) {
                is Result.Success -> {
                    r.data.sessionId?.let { sid -> _sessionId.value = sid }

                    // الأطباء فقط عندما يؤكد الباك إند وجود تخصص مطابق مع أطباء
                    val doctors = if (r.data.readyForDoctors && r.data.specialtyAvailable) {
                        r.data.recommendedDoctor.orEmpty().filter { d -> !d.id.isNullOrBlank() }
                    } else {
                        emptyList()
                    }

                    // تحديث الملخص والخطورة من الـ LLM
                    r.data.summary?.let { s -> if (s.isNotBlank()) _caseSummary.value = s }
                    r.data.severity?.let { s -> if (s.isNotBlank()) _severity.value = s }

                    _messages.update {
                        it + ChatMessageUi(
                            isUser = false,
                            text = r.data.assistantMessage,
                            recommendedDoctors = doctors,
                            severity = r.data.severity,
                            summary = r.data.summary,
                        )
                    }
                    _quickReplies.value = r.data.quickReplies
                }
                is Result.Error -> {
                    _messages.update {
                        it + ChatMessageUi(
                            isUser = false,
                            text = "تعذر إكمال المحادثة: ${r.message}",
                            isError = true,
                            retryText = trimmed,
                        )
                    }
                }
                is Result.Loading -> {} // never emitted by repository.chat; kept for exhaustive when
            }
            _isLoading.value = false
        }
    }

    fun retryLastMessage() {
        val msg = _lastUserMessage ?: return
        sendUserMessage(msg)
    }
}
