
package com.example.edu_ai.data.remote.ai

import com.google.gson.annotations.SerializedName

data class QuizResponse(
    @SerializedName("quiz_title") val title: String,
    @SerializedName("questions") val questions: List<QuizQuestion>,
    @SerializedName("learning_outcomes") val learningOutcomes: List<String>? = null
)

data class QuizQuestion(
    @SerializedName("question_text") val text: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("correct_option_index") val correctIndex: Int,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("learning_outcome") val learningOutcome: String? = null
)


 