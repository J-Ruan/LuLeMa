package com.lulema.app

import com.lulema.app.data.*
import com.lulema.app.domain.*
import com.lulema.app.backup.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.util.TimeZone

class ArchiveTest {
 private val day=LocalDate.of(2024,2,29)
 private fun entry(date:LocalDate=day,duration:Int=15)=Entry(day=date.toEpochDay(),duration=duration)
 private fun backup()=Backup(records=listOf(entry()),settings=Preferences(nickname="虚构测试员"))
 @Test fun validRecordAllowsHistoricalDateAndOptionalTime() {entry().validate();entry().copy(minute=0).validate();entry().copy(minute=1439).validate()}
 @Test fun durationRejectsInvalidAndOverflow() {listOf(-1,1441,Int.MAX_VALUE).forEach {assertThrows(IllegalArgumentException::class.java) {entry(duration=it).validate()}}}
 @Test fun rejectsFutureDatesAndInvalidFields() {
  listOf(entry(LocalDate.now().plusDays(1)),entry().copy(minute=1440),entry().copy(note="x".repeat(1001)),entry().copy(modified=0),entry().copy(id="invalid")).forEach {e->assertThrows(IllegalArgumentException::class.java) {e.validate()}}
 }
 @Test fun multipleRecordsOnSameDayAreCountedIndependently() {val data=listOf(entry(),entry(duration=20));assertEquals(2,Analysis.daily(data,day,7).last().second);assertEquals(35L,Analysis.summary(data).minutes)}
 @Test fun monthlyBoundariesAndLeapYear() {val data=listOf(entry(day.minusDays(1)),entry(),entry(day.plusDays(1)));assertEquals(2,Analysis.month(data,YearMonth.of(2024,2)).size);assertEquals(1,Analysis.month(data,YearMonth.of(2024,3)).size)}
 @Test fun annualBoundaries() {val data=listOf(entry(LocalDate.of(2023,12,31)),entry(LocalDate.of(2024,1,1)),entry(LocalDate.of(2024,12,31)),entry(LocalDate.of(2025,1,1)));assertEquals(2,Analysis.year(data,2024).size)}
 @Test fun emptyDatabaseStatistics() {val s=Analysis.summary(emptyList());assertEquals(0,s.count);assertEquals(0L,s.minutes);assertEquals(0.0,s.average,0.0);assertNull(s.latest);assertTrue(Analysis.daily(emptyList(),day,30).all {it.second==0})}
 @Test fun totalsUseLongAndAverageIncludesUnspecifiedDuration() {val s=Analysis.summary(listOf(entry(duration=0),entry(duration=60)));assertEquals(60L,s.minutes);assertEquals(30.0,s.average,0.0)}
 @Test fun calendarHandlesWeekStartLeapAndYearRollover() {
  val feb=Analysis.calendar(YearMonth.of(2024,2),true);assertEquals(29,feb.filterNotNull().size);assertEquals(day,feb.filterNotNull().last());assertEquals(0,feb.size%7)
  assertEquals(LocalDate.of(2023,1,1),Analysis.calendar(YearMonth.of(2023,1),false).first())
  assertEquals(6,Analysis.calendar(YearMonth.of(2023,1),true).takeWhile {it==null}.size)
 }
 @Test fun recordDatesDoNotShiftAcrossTimeZones() {
  val old=TimeZone.getDefault()
  try { listOf("Asia/Shanghai","America/Los_Angeles","Pacific/Kiritimati").forEach {zone->TimeZone.setDefault(TimeZone.getTimeZone(zone));assertEquals(day,LocalDate.ofEpochDay(entry().day));assertEquals(1,Analysis.month(listOf(entry()),YearMonth.of(2024,2)).size)} } finally {TimeZone.setDefault(old)}
 }
 @Test fun plainBackupRoundTripsRecordsSettingsAchievements() {val b=backup().copy(settings=Preferences(theme="dark").unlock("first"));assertEquals(b,BackupCodec.decode(BackupCodec.encode(b)))}
 @Test fun encryptedBackupRoundTripAndRandomizedCiphertext() {
  val b=backup();val pass="test-password".toCharArray();val a=BackupCodec.encode(b,pass);val c=BackupCodec.encode(b,pass)
  assertFalse(a.contentEquals(c));assertEquals(b,BackupCodec.decode(a,pass));assertFalse(a.toString(Charsets.UTF_8).contains("虚构测试员"))
 }
 @Test fun wrongPasswordAndTamperAreRejected() {
  val a=BackupCodec.encode(backup(),"test-password".toCharArray())
  assertThrows(Exception::class.java) {BackupCodec.decode(a,"wrong-password".toCharArray())}
  val text=a.toString(Charsets.UTF_8);val marker="\"ciphertext\":\"";val at=text.indexOf(marker)+marker.length
  val tampered=text.substring(0,at)+(if(text[at]=='A')"B" else "A")+text.substring(at+1)
  assertThrows(Exception::class.java) {BackupCodec.decode(tampered.toByteArray(),"test-password".toCharArray())}
 }
 @Test fun rejectsCorruptUnknownVersionAndDuplicateIds() {
  listOf(byteArrayOf(),"garbage".toByteArray(),"{}".toByteArray(),"[]".toByteArray()).forEach {data->assertThrows(Exception::class.java) {BackupCodec.decode(data)}}
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(backup().copy(formatVersion=99))}
  val e=entry();assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(backup().copy(records=listOf(e,e)))}
 }
 @Test fun excessiveNestingAndTruncatedJsonAreRejected() {
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.decode(("[".repeat(10000)+"]".repeat(10000)).toByteArray())}
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.decode("{\"records\": [".toByteArray())}
 }
 @Test fun rejectsOversizeAndShortPassword() {
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES+1))}
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(backup(),"short".toCharArray())}
 }
 @Test fun achievementsNeverDependOnRecordFrequency() {val p=Preferences().unlock("stats");assertEquals(p,p.unlock("stats"));assertEquals(1,p.achievements.size);assertEquals(9,Achievements.size)}
 @Test fun settingsValidationPreventsHighFrequencyReminders() {assertThrows(IllegalArgumentException::class.java) {Preferences(reminderDays=1).validate()};Preferences(reminderDays=7).validate();Preferences(reminderDays=90).validate()}
}
