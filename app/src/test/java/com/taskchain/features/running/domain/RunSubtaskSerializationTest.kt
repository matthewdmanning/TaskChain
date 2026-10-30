package com.taskchain.features.running.domain

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies that runs and history events built by the Running engine keep their subtask state through JSON. */
class RunSubtaskSerializationTest {
    /** Use this function to verify an engine-advanced run and its engine-mapped history event round-trip. */
    @Test
    fun engineSubtaskStateRoundTripsAcrossRunAndHistory() {
        val json = Json { }
        val template = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            tasks = listOf(
                RoutineTask(
                    RoutineTaskId("task"),
                    "Task",
                    subtasks = listOf(
                        RoutineSubtask(RoutineSubtaskId("one"), "One", 60),
                        RoutineSubtask(RoutineSubtaskId("two"), "Two", 120),
                    ),
                ),
            ),
        )
        val run = RoutineRunEngine().completeCurrent(
            RoutineRunEngine().start(template, RoutineRunId("run"), 0),
            30_000L,
        )
        val event = RoutineRunEngine().toCompletionEvent(
            run.copy(
                tasks = run.tasks.map {
                    it.copy(status = RunTaskStatus.COMPLETED, actualDurationMillis = 30_000L)
                },
                status = RunStatus.COMPLETED,
                endedAtEpochMillis = 30_000L,
            ),
        )

        assertEquals(run, json.decodeFromString<RoutineRun>(json.encodeToString(run)))
        assertEquals(event, json.decodeFromString<CompletionEvent>(json.encodeToString(event)))
    }
}
