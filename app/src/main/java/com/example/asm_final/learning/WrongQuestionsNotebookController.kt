package com.example.asm_final.learning

import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotebookState(
    val items: List<WrongQuestionEntry>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filterState: String = "ALL", // ALL, NEEDS_REVIEW, RESOLVED
    val retryingEntryId: String? = null,
    val selectedRetryOptions: Map<String, String> = emptyMap(),
    val retryResults: Map<String, WrongQuestionRetryResult> = emptyMap(),
    val retryErrorMessage: String? = null
)

class WrongQuestionsNotebookController(
    val repository: LibraryRepository,
    val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(NotebookState())
    val state: StateFlow<NotebookState> = _state.asStateFlow()

    var requestGeneration: Int = 0
        private set

    var currentSubjectWireId: String? = null
        private set
    var currentGrade: Int? = null
        private set

    fun loadNotebook(subjectWireId: String?, grade: Int?, filterState: String = "ALL"): Job {
        val gen = ++requestGeneration
        val targetOwnerId = repository.ownerId
        val targetEndpoint = repository.endpoint

        currentSubjectWireId = subjectWireId
        currentGrade = grade

        _state.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                filterState = filterState,
                retryErrorMessage = null
            )
        }
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val list = repository.loadWrongQuestions(subjectWireId, grade, filterState)
                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {
                    _state.update {
                        it.copy(
                            items = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {
                    val msg = formatError(e)
                    _state.update {
                        it.copy(
                            items = null,
                            isLoading = false,
                            errorMessage = msg
                        )
                    }
                }
            }
        }
    }

    fun selectRetryOption(entryId: String, option: String) {
        _state.update { cur ->
            cur.copy(selectedRetryOptions = cur.selectedRetryOptions + (entryId to option))
        }
    }

    fun submitRetry(entry: WrongQuestionEntry): Job? {
        val selected = _state.value.selectedRetryOptions[entry.id] ?: return null
        if (_state.value.retryingEntryId != null) return null

        val targetGen = requestGeneration
        val targetOwnerId = repository.ownerId
        val targetEndpoint = repository.endpoint
        val targetEntryId = entry.id

        _state.update {
            it.copy(retryingEntryId = targetEntryId, retryErrorMessage = null)
        }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = repository.retryWrongQuestion(targetEntryId, selected)
                if (targetGen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {
                    _state.update { cur ->
                        val updatedItems = cur.items?.map { item ->
                            if (item.id == targetEntryId) {
                                item.copy(
                                    state = result.state,
                                    selectedOption = result.selectedOption,
                                    updatedAt = result.updatedAt
                                )
                            } else item
                        }
                        cur.copy(
                            items = updatedItems,
                            retryingEntryId = null,
                            retryResults = cur.retryResults + (targetEntryId to result),
                            retryErrorMessage = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (targetGen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {
                    val msg = when {
                        e is BackendClient.Failure && e.code == "LEGACY_ENTRY_CANNOT_RETRY" ->
                            "Mục sổ lỗi sai cũ thiếu dữ liệu câu hỏi nên không thể luyện lại an toàn."
                        e is BackendClient.Failure && e.code == "ENTRY_NOT_FOUND" ->
                            "Không tìm thấy mục sổ lỗi sai này."
                        else -> formatError(e)
                    }
                    _state.update {
                        it.copy(retryingEntryId = null, retryErrorMessage = msg)
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    if (targetGen == requestGeneration) {
                        _state.update { it.copy(retryingEntryId = null) }
                    }
                }
            }
        }
    }

    fun clearRetryFeedback(entryId: String) {
        _state.update { cur ->
            cur.copy(
                retryResults = cur.retryResults - entryId,
                selectedRetryOptions = cur.selectedRetryOptions - entryId,
                retryErrorMessage = null
            )
        }
    }

    private fun formatError(e: Exception): String {
        return if (e is BackendClient.Failure) {
            when (e.code) {
                "SERVER_NOT_CONFIGURED" -> "Chưa cấu hình địa chỉ máy chủ backend."
                "NETWORK_ERROR", "CONNECT_ERROR", "TIMEOUT" -> "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối."
                else -> "Lỗi máy chủ: ${e.code} (HTTP ${e.status})"
            }
        } else {
            "Lỗi kết nối máy chủ: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }
}
