package com.taskchain.core.model

/**
 * Decides whether run feedback may sound or vibrate, for foreground runner feedback and background alerts alike.
 * Routine mute takes precedence over task settings: both must allow the output. Sound also needs its token enabled
 * in the run's snapshotted sound settings; a task's own sound URI never changes that flag.
 * Constructor inputs: none. Dependencies: `RoutineRun` routine gates and `SoundSettings`, `RoutineTask` task gates.
 */
object FeedbackGates {
    /**
     * Use this function before playing a run sound.
     * Inputs: `run` — the run whose routine gate and sound snapshot apply; `task` — the task causing the sound;
     * `token` — the semantic sound.
     */
    fun soundEnabled(run: RoutineRun, task: RoutineTask, token: SoundToken): Boolean =
        run.routineSoundEnabled && task.soundEnabled && run.soundSettings[token].enabled

    /**
     * Use this function before any run haptic, including readiness countdown pulses.
     * Inputs: `run` — the run whose routine gate applies; `task` — the task causing the haptic.
     */
    fun vibrateEnabled(run: RoutineRun, task: RoutineTask): Boolean =
        run.routineVibrateEnabled && task.vibrateEnabled
}
