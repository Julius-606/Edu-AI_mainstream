
package com.example.edu_ai.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TopicWithSubtopics(
    @Embedded val topic: TopicEntity,
    @Relation(
        parentColumn = "topicId",
        entityColumn = "topicId"
    )
    val subtopics: List<SubtopicEntity>
)

data class ModuleWithTopics(
    @Embedded val module: ModuleEntity,
    @Relation(
        entity = TopicEntity::class,
        parentColumn = "moduleId",
        entityColumn = "moduleId"
    )
    val topics: List<TopicWithSubtopics>
)

data class UnitWithModules(
    @Embedded val unit: UnitEntity,
    @Relation(
        entity = ModuleEntity::class,
        parentColumn = "localId",
        entityColumn = "unitId"
    )
    val modules: List<ModuleWithTopics>
)


