package com.lulema.app

import com.lulema.app.data.*
import com.lulema.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.math.BigDecimal

class ReportTest {
 private val today=LocalDate.of(2024,2,29)
 private fun e(day:LocalDate=today,duration:Int=0)=Entry(day=day.toEpochDay(),duration=duration)
 @Test fun weekStartFollowsPreferenceAndCurrentPeriodStopsAtToday() {
  val monday=ReportAnalyzer.analyze(emptyList(),Preferences(),ReportPeriod.WEEK,today,today)
  val sunday=ReportAnalyzer.analyze(emptyList(),Preferences(mondayFirst=false),ReportPeriod.WEEK,today,today)
  assertEquals(LocalDate.of(2024,2,26),monday.start);assertEquals(LocalDate.of(2024,2,25),sunday.start)
  assertEquals(today,monday.end);assertFalse(monday.complete)
 }
 @Test fun weekCanSpanYearAndCompletedPeriodIsFullLength() {
  val report=ReportAnalyzer.analyze(emptyList(),Preferences(),ReportPeriod.WEEK,LocalDate.of(2025,1,1),LocalDate.of(2025,2,1))
  assertEquals(LocalDate.of(2024,12,30),report.start);assertEquals(LocalDate.of(2025,1,5),report.end);assertTrue(report.complete)
 }
 @Test fun leapMonthIncludesLeapDayAndExcludesAdjacentMonths() {
  val records=listOf(e(LocalDate.of(2024,1,31)),e(LocalDate.of(2024,2,1)),e(),e(LocalDate.of(2024,3,1)))
  val report=ReportAnalyzer.analyze(records,Preferences(),ReportPeriod.MONTH,today,today)
  assertEquals(2,report.summary.count);assertEquals(2,report.activeDays);assertEquals(LocalDate.of(2024,2,1),report.start);assertEquals(today,report.end)
 }
 @Test fun annualBoundariesInclude366DaysInLeapYear() {
  val end=LocalDate.of(2024,12,31)
  val records=(0L..365L).map {e(LocalDate.of(2024,1,1).plusDays(it))}+e(LocalDate.of(2023,12,31))+e(LocalDate.of(2025,1,1))
  val report=ReportAnalyzer.analyze(records,Preferences(),ReportPeriod.YEAR,end,end)
  assertEquals(366,report.activeDays);assertEquals(366,report.longestStreak);assertEquals(366,report.summary.count)
 }
 @Test fun reportTotalsSeparateRecordsMissingMeasurementsAndDecimalVolume() {
  val data=listOf(e(duration=10).copy(handCount=Int.MAX_VALUE,volumeMl=0.1,location="虚构甲"),e(duration=20).copy(handCount=Int.MAX_VALUE,volumeMl=0.2,location="虚构甲"),e().copy(handCount=0,volumeMl=0.0,location="虚构乙"),e())
  val report=ReportAnalyzer.analyze(data,Preferences(),ReportPeriod.MONTH,today,today)
  assertEquals(4,report.summary.count);assertEquals(1,report.activeDays);assertEquals(30L,report.summary.minutes)
  assertEquals(15.0,report.summary.average,0.0);assertEquals(2,report.summary.missingDurationCount)
  assertEquals(4294967294L,report.summary.handCountTotal);assertEquals(0,report.summary.volumeTotal!!.compareTo(BigDecimal("0.3")))
  assertEquals(listOf("虚构甲" to 2,"虚构乙" to 1),report.locations)
 }
 @Test fun emptyReportHasNoFakeMeasurements() {
  val report=ReportAnalyzer.analyze(emptyList(),Preferences(),ReportPeriod.WEEK,today,today)
  assertEquals(0,report.summary.count);assertEquals(0,report.activeDays);assertEquals(0,report.longestStreak)
  assertNull(report.summary.handCountTotal);assertNull(report.summary.volumeTotal);assertEquals(0,report.summary.measuredCount);assertTrue(report.unlocked.isEmpty())
 }
 @Test fun achievementsUseLocalUnlockDateAndStreakIsWithinReportRange() {
  val zone=ZoneId.of("Asia/Shanghai")
  val at=today.atStartOfDay(zone).toInstant().toEpochMilli()
  val p=Preferences(achievements=mapOf("first" to at,"secret" to today.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()))
  val data=(0L..6L).map {e(today.minusDays(it))}
  val report=ReportAnalyzer.analyze(data,p,ReportPeriod.WEEK,today,today,zone)
  assertEquals(listOf("first"),report.unlocked.map {it.first.key});assertEquals(4,report.longestStreak)
 }
}
