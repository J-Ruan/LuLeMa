package com.lulema.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

@Serializable
@Entity(tableName = "records")
data class Entry(
 @Required @PrimaryKey val id: String = UUID.randomUUID().toString(),
 @Required val day: Long = LocalDate.now().toEpochDay(),
 val minute: Int? = null,
 @Required val duration: Int = 0,
 val mood: String = "",
 val note: String = "",
 @Required val created: Long = System.currentTimeMillis(),
 @Required val modified: Long = created
) {
 fun validate() {
  require(runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)) { "记录 ID 无效" }
  require(day in LocalDate.of(1900,1,1).toEpochDay()..LocalDate.now().toEpochDay()) { "日期需在 1900 年至今天之间" }
  require(minute == null || minute in 0..1439) { "时间无效" }
  require(duration in 0..1440) { "时长需为 0–1440 分钟" }
  require(mood.length <= 20 && note.length <= 1000) { "心情最多 20 字，备注最多 1000 字" }
  require(created > 0 && modified >= created) { "记录时间戳无效" }
 }
}
@Serializable
data class Preferences(
 val nickname: String = "档案管理员",
 val theme: String = "system",
 val mondayFirst: Boolean = true,
 val reminders: Boolean = false,
 val reminderHour: Int = 20,
 val reminderMinute: Int = 0,
 val reminderDays: Int = 7,
 val changedSettings: Set<String> = emptySet(),
 val achievements: Map<String, Long> = emptyMap()
) {
 fun validate() {
  require(nickname.length in 1..30 && theme in listOf("system","light","dark")) { "设置无效" }
  require(reminderHour in 0..23 && reminderMinute in 0..59 && reminderDays in 7..90) { "提醒设置无效" }
  require(changedSettings.all {it in setOf("nickname","theme","weekStart","reminders","reminderTime","reminderInterval")}) {"设置来源无效"}
  require(achievements.keys.all { it in setOf("first","duration","history","stats","backup","dark","secret","settings","year") } && achievements.values.all { it > 0 }) { "成就数据无效" }
 }
 fun unlock(key: String): Preferences = if(key in achievements) this else copy(achievements = achievements + (key to System.currentTimeMillis()))
}
@Entity(tableName = "profile")
data class Profile(@PrimaryKey val id: Int = 1, val json: String)

@Dao
interface ArchiveDao {
 @Query("SELECT * FROM records ORDER BY day DESC, COALESCE(minute,-1) DESC, created DESC") fun observeEntries(): Flow<List<Entry>>
 @Query("SELECT * FROM profile WHERE id=1") fun observeProfile(): Flow<Profile?>
 @Query("SELECT * FROM records ORDER BY day DESC, created DESC") suspend fun all(): List<Entry>
 @Query("SELECT * FROM profile WHERE id=1") suspend fun profile(): Profile?
 @Upsert suspend fun put(entry: Entry)
 @Upsert suspend fun putProfile(profile: Profile)
 @Query("DELETE FROM records WHERE id=:id") suspend fun delete(id: String)
 @Query("DELETE FROM records") suspend fun clearEntries()
 @Query("DELETE FROM profile") suspend fun clearProfile()
}
@Database(entities = [Entry::class, Profile::class], version = 1, exportSchema = true)
abstract class ArchiveDatabase : RoomDatabase() { abstract fun dao(): ArchiveDao }

val ArchiveJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }
class ArchiveRepository(private val db: ArchiveDatabase) {
 val dao = db.dao()
 suspend fun preferences(): Preferences = dao.profile()?.let { ArchiveJson.decodeFromString<Preferences>(it.json) } ?: Preferences()
 suspend fun updatePreferences(change: (Preferences)->Preferences) = db.withTransaction {
  val next = change(preferences()); next.validate(); dao.putProfile(Profile(json=ArchiveJson.encodeToString(next)))
 }
 suspend fun save(entry: Entry) = db.withTransaction {
  entry.validate(); dao.put(entry)
  var p = preferences().unlock("first")
  if(entry.duration > 0) p=p.unlock("duration")
  dao.putProfile(Profile(json=ArchiveJson.encodeToString(p)))
 }
 suspend fun snapshot(): Pair<List<Entry>,Preferences> = db.withTransaction { dao.all() to preferences() }
 suspend fun restore(entries: List<Entry>, settings: Preferences, replace: Boolean) = db.withTransaction {
  entries.forEach { it.validate() }; settings.validate()
  if(replace) { dao.clearEntries(); dao.putProfile(Profile(json=ArchiveJson.encodeToString(settings.copy(reminders=false)))) }
  val existing = dao.all().associateBy { it.id }
  entries.forEach { e -> if(replace || existing[e.id]?.modified?.let { e.modified > it } != false) dao.put(e) }
 }
 suspend fun clear() = db.withTransaction { dao.clearEntries(); dao.clearProfile() }
}
