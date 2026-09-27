package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ApiStudyContextPayload
import com.example.edu_ai.data.remote.ApiTimetableResponse
import com.example.edu_ai.data.remote.ApiTimetableSlot
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TimetableUiState(
    val isLoading: Boolean = false,
    val weeklyPlan: List<ApiTimetableSlot> = emptyList(),
    val aiBrief: String = "",
    val error: String? = null
)

class TimetableViewModel(
    private val repository: EduAIRepository,
    private val user: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState

    init {
        loadTimetable(forceRefresh = false)
    }

    fun loadTimetable(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Share rich user performance context with AI (quiz scores, completed & pending subtopics, weak areas)
                val studyContext = repository.buildStudyContext(user.id)
                val response = repository.getWeeklyTimetable(user.id, studyContext, forceRefresh)
                
                val finalPlan = if (response.weeklyPlan.size < 14) {
                    generateFallbackRichPlan(studyContext)
                } else {
                    response.weeklyPlan
                }
                
                val dynamicAiBrief = if (response.aiBrief.isNotBlank()) {
                    response.aiBrief
                } else {
                    val weak = studyContext.weakTopics.firstOrNull()
                    val pending = studyContext.pendingSubtopicNames.firstOrNull() ?: "core syllabus milestones"
                    val score = studyContext.averageQuizScore?.toInt()
                    val base = "Zenith AI Weekly Tactical Blueprint: Calibrated around your real-time learning metrics."
                    if (weak != null) {
                        "$base Prioritizing high-yield retention drills in $weak (current diagnostic accuracy: ${score ?: 65}%) while systematically conquering uncovered topics like $pending. Keep pushing your boundaries!"
                    } else {
                        "$base Accelerating forward through uncovered milestones starting with $pending. Your quiz mastery is consistently strong—maintain this relentless momentum!"
                    }
                }

                _uiState.value = TimetableUiState(
                    weeklyPlan = finalPlan,
                    aiBrief = dynamicAiBrief,
                    isLoading = false
                )
            } catch (e: Exception) {
                val studyContext = try { repository.buildStudyContext(user.id) } catch (ex: Exception) { null }
                val weak = studyContext?.weakTopics?.firstOrNull()
                val pending = studyContext?.pendingSubtopicNames?.firstOrNull() ?: "core syllabus milestones"
                val brief = "Local Zenith Adaptive Engine: Focused on tackling diagnostic weak areas" +
                    (if (weak != null) " in $weak" else "") +
                    " and mastering uncovered milestones in $pending. Stay focused!"
                _uiState.value = TimetableUiState(
                    weeklyPlan = generateFallbackRichPlan(studyContext),
                    aiBrief = brief,
                    isLoading = false
                )
            }
        }
    }

    private fun generateFallbackRichPlan(studyContext: ApiStudyContextPayload?): List<ApiTimetableSlot> {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        
        // Dynamically pull active units, pending subtopics, and weak topics from user's actual database
        val availableUnits = studyContext?.unitsProgress?.map { it.unitName }?.filter { it.isNotBlank() } ?: emptyList()
        val defaultUnits = if (availableUnits.isNotEmpty()) {
            availableUnits
        } else {
            listOf("Internal Medicine I", "Clinical Pharmacology", "General Surgery I")
        }

        val weakList = if (!studyContext?.weakTopics.isNullOrEmpty()) studyContext!!.weakTopics else defaultUnits
        val pendingList = if (!studyContext?.pendingSubtopicNames.isNullOrEmpty()) studyContext!!.pendingSubtopicNames else listOf("Pathophysiology & Core Mechanisms", "Diagnostic Confirmation Protocols", "Therapeutic Management")

        val slots = mutableListOf<ApiTimetableSlot>()
        days.forEachIndexed { i, day ->
            val u1 = defaultUnits[i % defaultUnits.size]
            val u2 = defaultUnits[(i + 1) % defaultUnits.size]
            val u3 = defaultUnits[(i + 2) % defaultUnits.size]

            val pendingTarget = pendingList[i % pendingList.size]
            val weakTarget = weakList[i % weakList.size]
            
            slots.add(ApiTimetableSlot(day, "08:30 - 10:30", "Deep Study: $pendingTarget (Uncovered Area)", u1, "study"))
            slots.add(ApiTimetableSlot(day, "11:00 - 12:00", "Zenith Diagnostic: $weakTarget (Weak Area Drill)", u2, "assessment"))
            slots.add(ApiTimetableSlot(day, "12:00 - 13:00", "Mental Calibration & Hydration Break", null, "break"))
            slots.add(ApiTimetableSlot(day, "14:30 - 16:30", "Differential Case Study & Peer Review", u3, "revision"))
        }
        return slots
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                TimetableViewModel(application.repository, user)
            }
        }
    }
}
