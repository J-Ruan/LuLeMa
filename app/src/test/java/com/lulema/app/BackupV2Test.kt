package com.lulema.app

import com.lulema.app.backup.*
import com.lulema.app.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class BackupV2Test {
 private fun backup()=Backup(records=listOf(Entry(handCount=123,volumeMl=1.234,location="虚构地点"),Entry(handCount=0,volumeMl=0.0)),settings=Preferences().unlock("daily_2"))
 @Test fun allFieldsRoundTripThroughPlainAndAuthenticatedEncryptedBackups() {
  val b=backup()
  assertEquals(2,b.formatVersion)
  assertEquals(b,BackupCodec.decode(BackupCodec.encode(b)))
  val encrypted=BackupCodec.encode(b,"synthetic-password".toCharArray())
  assertThrows(BackupPasswordRequiredException::class.java) {BackupCodec.decode(encrypted)}
  assertFalse(encrypted.toString(Charsets.UTF_8).contains("虚构地点"))
  assertEquals(b,BackupCodec.decode(encrypted,"synthetic-password".toCharArray()))
 }
 @Test fun genuinelyOldV1FilesWithNoNewFieldsStillImport() {
  val old="""{"formatVersion":1,"exportedAt":1700000000000,"records":[{"id":"aadd5c20-1919-4c4a-a4b5-a20a296b6fd2","day":19000,"minute":null,"duration":0,"mood":"","note":"虚构旧档案","created":1700000000000,"modified":1700000000000}],"settings":{"achievements":{"first":1700000000000}}}"""
  val b=BackupCodec.decode(old.toByteArray())
  assertEquals(1,b.formatVersion);assertNull(b.records.single().handCount);assertNull(b.records.single().volumeMl);assertNull(b.records.single().location)
  assertTrue("first" in b.settings.achievements)
  // V1 encryption envelope and AAD remain supported after the payload version upgrade.
  val oldEncrypted=BackupCodec.encode(b,"synthetic-password".toCharArray())
  assertEquals(b,BackupCodec.decode(oldEncrypted,"synthetic-password".toCharArray()))
 }
 @Test fun invalidNewFieldsAndUnknownAchievementsAreRejectedBeforeRestore() {
  val b=backup()
  for(bad in listOf(b.copy(records=listOf(b.records.first().copy(handCount=-1))),b.copy(records=listOf(b.records.first().copy(volumeMl=Double.NaN))),b.copy(records=listOf(b.records.first().copy(location="x".repeat(101)))),b.copy(settings=Preferences(achievements=mapOf("unknown" to 1))))) {
   assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(bad)}
  }
 }
 @Test fun extremeExportAndAchievementTimestampsAreRejected() {
  val b=backup()
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(b.copy(exportedAt=Long.MAX_VALUE))}
  val text=BackupCodec.encode(b).toString(Charsets.UTF_8)
  val json=ArchiveJson.parseToJsonElement(text).jsonObject
  val poisoned=JsonObject(json+("exportedAt" to JsonPrimitive(Long.MAX_VALUE))).toString()
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.decode(poisoned.toByteArray())}
  assertThrows(IllegalArgumentException::class.java) {BackupCodec.encode(b.copy(settings=Preferences(achievements=mapOf("first" to Long.MAX_VALUE))))}
 }
}
