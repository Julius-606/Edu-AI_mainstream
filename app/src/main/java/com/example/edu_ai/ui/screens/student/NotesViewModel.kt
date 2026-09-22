
package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.NoteEntity
import com.example.edu_ai.data.local.UserEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> = dao.getAllNotes(user.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            dao.updateNote(note.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            dao.deleteNote(noteId)
        }
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                NotesViewModel(application.database.dao(), user)
            }
        }
    }
}


 