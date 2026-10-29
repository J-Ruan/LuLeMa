package com.lulema.app.domain

import com.lulema.app.data.Entry
import com.lulema.app.data.Preferences

object AchievementEngine {
 fun longestStreak(days: Iterable<Long>): Int {
  var previous: Long? = null
  var current = 0
  var longest = 0
  for(day in days.toSet().sorted()) {
   current = if(previous != null && day == previous + 1) current + 1 else 1
   longest = maxOf(longest, current)
   previous = day
  }
  return longest
 }

 fun eligible(entries: List<Entry>): Set<String> {
  val records = entries.groupBy { it.id }.values.map { versions -> versions.maxBy { it.modified } }
  return buildSet {
   if(records.isNotEmpty()) add("first")
   if(records.any { it.duration > 0 }) add("duration")
   val maximum = records.groupingBy { it.day }.eachCount().values.maxOrNull() ?: 0
   for(count in 2..5) if(maximum >= count) add("daily_$count")
   if(maximum >= 6) add("daily_over_5")
   if(longestStreak(records.map { it.day }) >= 7) add("streak_7")
   if(records.any { it.duration in 1..2 }) add("duration_under_3")
   if(records.any { it.duration in 61..1440 }) add("duration_over_60")
  }
 }

 fun reconcile(entries: List<Entry>, preferences: Preferences, at: Long = System.currentTimeMillis()): Preferences =
  eligible(entries).fold(preferences) { state, key -> state.unlock(key, at) }
}
