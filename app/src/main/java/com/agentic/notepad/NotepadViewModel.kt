package com.agentic.notepad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotepadUiState(
    val notes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val activeNote: Note? = null,
    val searchQuery: String = ""
)

class NotepadViewModel(private val repository: NoteRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val activeNoteId = MutableStateFlow<String?>(null)

    val state: StateFlow<NotepadUiState> = combine(
        repository.notes,
        searchQuery,
        activeNoteId
    ) { notes, query, activeId ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isBlank()) {
            notes.sortedByDescending { it.updatedAt }
        } else {
            notes.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.content.contains(trimmed, ignoreCase = true)
            }.sortedByDescending { it.updatedAt }
        }
        val active = filtered.find { it.id == activeId }
            ?: notes.find { it.id == activeId }
            ?: filtered.firstOrNull()
        NotepadUiState(
            notes = notes.sortedByDescending { it.updatedAt },
            filteredNotes = filtered,
            activeNote = active,
            searchQuery = trimmed
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = NotepadUiState()
    )

    fun onSearch(query: String) {
        searchQuery.value = query
    }

    fun selectNote(noteId: String?) {
        activeNoteId.value = noteId
    }

    fun createNote() {
        val newNote = Note(title = "Untitled note")
        activeNoteId.value = newNote.id
        viewModelScope.launch {
            repository.upsert(newNote)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.upsert(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.delete(noteId)
            if (activeNoteId.value == noteId) {
                activeNoteId.value = null
            }
        }
    }
}

class NotepadViewModelFactory(
    private val repository: NoteRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotepadViewModel::class.java)) {
            return NotepadViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}
