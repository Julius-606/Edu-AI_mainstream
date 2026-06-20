package com.example.edu_ai.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ModuleWithSubtopics(
    @Embedded val module: ModuleEntity,
    @Relation(
        parentColumn = "moduleId",
        entityColumn = "moduleId"
    )
    val subtopics: List<SubtopicEntity>
)

data class UnitWithModules(
    @Embedded val unit: UnitEntity,
    @Relation(
        entity = ModuleEntity::class,
        parentColumn = "localId",
        entityColumn = "unitId"
    )
    val modules: List<ModuleWithSubtopics>
)
