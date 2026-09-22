
package com.example.edu_ai.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.edu_ai.repository.EduAIRepository

class TeacherViewModelFactory(private val repository: EduAIRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


 