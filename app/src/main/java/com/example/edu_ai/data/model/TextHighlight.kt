package com.example.edu_ai.data.model

import java.util.UUID

data class TextHighlight(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val colorHex: String = "#FEF08A",
    val label: String = "Key Concept",
    val subtopicId: Long = 0L,
    val targetKey: String = "",
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HighlightColor(
    val hex: String,
    val displayName: String,
    val bgHex: Long,
    val textHex: Long
) {
    YELLOW("#FEF08A", "Key Concept", 0xFFFEF08A, 0xFF713F12),
    GREEN("#A7F3D0", "High Yield", 0xFFA7F3D0, 0xFF064E3B),
    CYAN("#7DD3FC", "Clinical Fact", 0xFF7DD3FC, 0xFF0C4A6E),
    PURPLE("#E9D5FF", "Exam Trap", 0xFFE9D5FF, 0xFF581C87),
    CORAL("#FECDD3", "Critical Rule", 0xFFFECDD3, 0xFF881337)
}
