package com.lulema.app.domain

import com.lulema.app.data.Entry
import com.lulema.app.data.Preferences
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

enum class ReportPeriod(val title: String) { WEEK("周报"), MONTH("月报"), YEAR("年报") }
data class PeriodReport(
 val period: ReportPeriod,
 val start: LocalDate,
 val end: LocalDate,
 val complete: Boolean,
 val entries: List<Entry>,
 val summary: Analysis.Summary,
 val activeDays: Int,
 val longestStreak: Int,
 val locations: List<Pair<String, Int>>,
 val unlocked: List<Pair<Achievement, Long>>
)

object ReportAnalyzer {
 fun analyze(entries: List<Entry>, preferences: Preferences, period: ReportPeriod,
             anchor: LocalDate = LocalDate.now(), today: LocalDate = LocalDate.now(),
             zone: ZoneId = ZoneId.systemDefault()): PeriodReport {
  val start = when(period) {
   ReportPeriod.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(if(preferences.mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))
   ReportPeriod.MONTH -> anchor.withDayOfMonth(1)
   ReportPeriod.YEAR -> anchor.withDayOfYear(1)
  }
  val fullEnd = when(period) {
   ReportPeriod.WEEK -> start.plusDays(6)
   ReportPeriod.MONTH -> start.plusMonths(1).minusDays(1)
   ReportPeriod.YEAR -> start.plusYears(1).minusDays(1)
  }
  val end = minOf(fullEnd, today)
  val records = entries.filter { it.day in start.toEpochDay()..end.toEpochDay() }
  val unlocked = Achievements.mapNotNull { achievement ->
   preferences.achievements[achievement.key]?.takeIf {
    Instant.ofEpochMilli(it).atZone(zone).toLocalDate() in start..end
   }?.let { achievement to it }
  }.sortedBy { it.second }
  return PeriodReport(period, start, end, fullEnd <= today, records, Analysis.summary(records),
   records.map { it.day }.distinct().size, AchievementEngine.longestStreak(records.map { it.day }),
   records.mapNotNull { it.location?.trim()?.takeIf(String::isNotEmpty) }.groupingBy { it }.eachCount()
    .toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }), unlocked)
 }
}
