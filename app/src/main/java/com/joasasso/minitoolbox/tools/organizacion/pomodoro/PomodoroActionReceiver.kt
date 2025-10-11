package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PomodoroActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP    -> PomodoroAlarmReceiver.stopPomodoro(context)
            ACTION_SILENCE -> PomodoroAlarmReceiver.silenceAlarm(context)
        }
    }
}
