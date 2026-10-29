package com.lulema.app

import com.lulema.app.data.*
import com.lulema.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class V020DomainTest {
 private val leap=LocalDate.of(2024,2,29)
 private fun record(day:LocalDate=leap,duration:Int=0)=Entry(day=day.toEpochDay(),duration=duration)
 private val dailyKeys=(2..5).map {"daily_$it"}+"daily_over_5"

 @Test fun everyDailyThresholdAndAllFiveUnlockTogetherAtSix() {
  for(count in 0..7) {
   val keys=AchievementEngine.eligible(List(count) {record()})
   for(threshold in 2..6)assertEquals("count=$count threshold=$threshold",count>=threshold,(if(threshold==6)"daily_over_5" else "daily_$threshold") in keys)
  }
  assertTrue(AchievementEngine.eligible(List(6) {record()}).containsAll(dailyKeys))
 }
 @Test fun datesAreCountedSeparatelyAndRepeatedIdentityNeverCountsTwice() {
  val one=record()
  assertFalse("daily_2" in AchievementEngine.eligible(listOf(one,one.copy(duration=20,modified=one.modified+1))))
  assertFalse("daily_2" in AchievementEngine.eligible(listOf(one,record(leap.plusDays(1)))))
 }
 @Test fun historicalStreakCrossesLeapDayAndYearWithoutDependingOnOrder() {
  val start=leap.minusDays(3)
  val records=(0L..6L).map {record(start.plusDays(it))}.reversed()
  assertTrue("streak_7" in AchievementEngine.eligible(records+record(leap)))
  assertEquals(7,AchievementEngine.longestStreak((0L..6L).map {LocalDate.of(2023,12,28).plusDays(it).toEpochDay()}))
 }
 @Test fun missingDayAndSevenRecordsOnOneDayDoNotCreateAStreak() {
  assertFalse("streak_7" in AchievementEngine.eligible(List(7) {record()}))
  val days=listOf(0L,1L,2L,4L,5L,6L,7L)
  assertEquals(4,AchievementEngine.longestStreak(days.map {leap.minusDays(20).plusDays(it).toEpochDay()}))
 }
 @Test fun durationAchievementsRespectExactBoundaries() {
  for(duration in listOf(0,1,2,3,60,61,1440)) {
   val keys=AchievementEngine.eligible(listOf(record(duration=duration)))
   assertEquals(duration in 1..2,"duration_under_3" in keys)
   assertEquals(duration>60,"duration_over_60" in keys)
  }
 }
 @Test fun alreadyUnlockedAchievementsKeepTheirOriginalTimestampAfterDeletion() {
  val at=System.currentTimeMillis()-1000
  val unlocked=AchievementEngine.reconcile(List(6) {record(duration=61)},Preferences().unlock("secret",at),at)
  assertEquals(unlocked,AchievementEngine.reconcile(emptyList(),unlocked,at+1))
  assertEquals(unlocked,AchievementEngine.reconcile(List(6) {record(duration=61)},unlocked,at+1))
  unlocked.validate()
 }
 @Test fun evaluatorCoversEveryBoundaryAndMissingValues() {
  val cases=mapOf<Int?,DurationEvaluator.Band>(null to DurationEvaluator.Band.UNSPECIFIED,-1 to DurationEvaluator.Band.UNSPECIFIED,0 to DurationEvaluator.Band.UNSPECIFIED,1 to DurationEvaluator.Band.FLASH,2 to DurationEvaluator.Band.FLASH,3 to DurationEvaluator.Band.BRIEF,9 to DurationEvaluator.Band.BRIEF,10 to DurationEvaluator.Band.REGULAR,30 to DurationEvaluator.Band.REGULAR,31 to DurationEvaluator.Band.LONG,60 to DurationEvaluator.Band.LONG,61 to DurationEvaluator.Band.EXTENDED)
  cases.forEach {(minutes,band)->assertEquals(band,DurationEvaluator.evaluate(minutes).band)}
 }
 @Test fun optionalCountsDistinguishZeroNullAndRejectInvalidInput() {
  assertNull(EntryInput.handCount(""));assertEquals(0,EntryInput.handCount("0"));assertEquals(Int.MAX_VALUE,EntryInput.handCount("2147483647"))
  listOf("-1","1.2","abc","2147483648","+2").forEach {assertThrows(IllegalArgumentException::class.java) {EntryInput.handCount(it)}}
  assertThrows(IllegalArgumentException::class.java) {record().copy(handCount=-1).validate()}
 }
 @Test fun volumeUsesFiniteNonNegativeBoundedDecimalValues() {
  assertNull(EntryInput.volume(""));assertEquals(0.0,EntryInput.volume("0")!!,0.0);assertEquals(12.345,EntryInput.volume("12.345")!!,0.0);assertEquals(1000.0,EntryInput.volume("1000.000")!!,0.0)
  listOf("-1","NaN","Infinity","1e300","1000.001","0.0001","abc","1,2").forEach {assertThrows(IllegalArgumentException::class.java) {EntryInput.volume(it)}}
  listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,1000.001,0.0001).forEach {assertThrows(IllegalArgumentException::class.java) {record().copy(volumeMl=it).validate()}}
 }
 @Test fun locationAndNewFieldsHaveSafeLimits() {
  record().copy(location="虚构".repeat(50),volumeMl=1000.0,handCount=Int.MAX_VALUE).validate()
  listOf("x".repeat(101)," ").forEach {assertThrows(IllegalArgumentException::class.java) {record().copy(location=it).validate()}}
 }
 @Test fun timestampsAllowHistoryAndSmallClockDriftButRejectPoisonedFuture() {
  val now=System.currentTimeMillis()
  record().copy(created=1,modified=2).validate(now)
  record().copy(created=now,modified=now+TimestampPolicy.CLOCK_SKEW_MS).validate(now)
  listOf(0L,-1L,Long.MAX_VALUE,now+TimestampPolicy.CLOCK_SKEW_MS+1).forEach {bad->
   assertThrows(IllegalArgumentException::class.java) {record().copy(created=bad,modified=bad).validate(now)}
   assertThrows(IllegalArgumentException::class.java) {Preferences(achievements=mapOf("first" to bad)).validate(now)}
  }
  assertThrows(IllegalArgumentException::class.java) {record().copy(created=now,modified=now-1).validate(now)}
 }
 @Test fun unrelatedPreferencesDoNotChangeReminderConfiguration() {
  val p=Preferences(reminders=true)
  listOf(p.copy(nickname="虚构昵称"),p.copy(theme="dark"),p.copy(mondayFirst=false),p.unlock("first")).forEach {assertFalse(ReminderConfiguration.changed(p,it))}
  listOf(p.copy(reminders=false),p.copy(reminderHour=7),p.copy(reminderMinute=12),p.copy(reminderDays=30)).forEach {assertTrue(ReminderConfiguration.changed(p,it))}
 }
}
