package com.taskchain

import android.content.Context
import com.taskchain.data.BuiltInRoutineLibrary
import com.taskchain.data.CompletionRepository
import com.taskchain.data.DataStoreUserPreferenceRepository
import com.taskchain.data.FileCompletionRepository
import com.taskchain.data.FileRoutineRepository
import com.taskchain.data.FileRoutineRunRepository
import com.taskchain.data.RoutineRepository
import com.taskchain.data.RoutineRunRepository
import com.taskchain.data.UserPreferenceRepository
import com.taskchain.data.recoverTerminalSession
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.run.RoutineRunEngine
import com.taskchain.reminder.AndroidReminderScheduler
import com.taskchain.reminder.ReminderScheduler
import com.taskchain.reminder.TimerFeedback
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json

/** Small application composition root that wires domain ports to local Android adapters. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dataDirectory = File(appContext.filesDir, "data")

    val routines: RoutineRepository = FileRoutineRepository(File(dataDirectory, "routines"), json)
    val activeRun: RoutineRunRepository = FileRoutineRunRepository(File(dataDirectory, "sessions/active-session.json"), json)
    val completions: CompletionRepository = FileCompletionRepository(File(dataDirectory, "history/events.json"), json)
    val preferences: UserPreferenceRepository = DataStoreUserPreferenceRepository(appContext)
    val builtInLibrary = BuiltInRoutineLibrary(appContext, json)
    val runEngine = RoutineRunEngine()
    val reminders: ReminderScheduler = AndroidReminderScheduler(appContext)
    val timerFeedback = TimerFeedback(appContext)

    /** Use this function before UI loads so an interrupted terminal run reaches append-only history. */
    suspend fun recoverTerminalRun() = recoverTerminalSession(activeRun, completions, runEngine)

    /** Use this function when creating a new persistent routine identity offline. */
    fun newRoutineId(): RoutineId = RoutineId(UUID.randomUUID().toString())

    /** Use this function when creating a new routine-step identity offline. */
    fun newStepId(): RoutineStepId = RoutineStepId(UUID.randomUUID().toString())

    /** Use this function when starting a distinct routine execution offline. */
    fun newRunId(): RoutineRunId = RoutineRunId(UUID.randomUUID().toString())

    /** Use this function wherever domain state needs wall-clock epoch milliseconds. */
    fun now(): Long = System.currentTimeMillis()
}
