package com.lulema.app.domain

import com.lulema.app.data.Preferences

data class ReminderConfiguration(val enabled: Boolean, val hour: Int, val minute: Int, val intervalDays: Int) {
 companion object {
  fun from(preferences: Preferences) = ReminderConfiguration(preferences.reminders, preferences.reminderHour, preferences.reminderMinute, preferences.reminderDays)
  fun changed(before: Preferences, after: Preferences) = from(before) != from(after)
 }
}
