package com.lulema.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lulema.app.domain.AchievementEngine
import com.lulema.app.domain.Achievements

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
 @Required val modified: Long = created,
 val handCount: Int? = null,
 val volumeMl: Double? = null,
 val location: String? = null
) {
 fun validate(now: Long = System.currentTimeMillis()) {
  require(runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)) { "记录 ID 无效" }
  require(day in LocalDate.of(1900,1,1).toEpochDay()..LocalDate.now().toEpochDay()) { "日期需在 1900 年至今天之间" }
  require(minute == null || minute in 0..1439) { "时间无效" }
  require(duration in 0..1440) { "时长需为 0–1440 分钟" }
  require(mood.length <= 20 && note.length <= 1000) { "心情最多 20 字，备注最多 1000 字" }
  require(handCount == null || handCount >= 0) { "手部运动次数需为 0–2147483647 的整数" }
  require(volumeMl == null || volumeMl.isFinite() && volumeMl in 0.0..1000.0 && java.math.BigDecimal.valueOf(volumeMl).stripTrailingZeros().scale() <= 3) { "出货量需为 0–1000 mL，最多三位小数" }
  require(location == null || location.isNotBlank() && location.length <= 100) { "地点最多 100 字，留空请不填写" }
  TimestampPolicy.validate(created, now)
  TimestampPolicy.validate(modified, now)
  require(modified >= created) { "修改时间不能早于创建时间" }
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
 fun validate(now: Long = System.currentTimeMillis()) {
  require(nickname.length in 1..30 && theme in listOf("system","light","dark")) { "设置无效" }
  require(reminderHour in 0..23 && reminderMinute in 0..59 && reminderDays in 7..90) { "提醒设置无效" }
  require(changedSettings.all {it in setOf("nickname","theme","weekStart","reminders","reminderTime","reminderInterval")}) {"设置来源无效"}
  require(achievements.keys.all { key -> Achievements.any { it.key == key } }) { "成就数据无效" }
  achievements.values.forEach { TimestampPolicy.validate(it, now) }
 }
 fun unlock(key: String, at: Long = System.currentTimeMillis()): Preferences {
  require(Achievements.any { it.key == key }) { "成就 key 无效" }
  return if(key in achievements) this else copy(achievements = achievements + (key to at))
 }
}
object TimestampPolicy {
 // Allow five minutes of clock drift, without allowing distant-future merges to dominate forever.
 const val CLOCK_SKEW_MS = 5 * 60 * 1000L
 fun validate(value: Long, now: Long = System.currentTimeMillis()) {
  val upper = now.coerceAtMost(Long.MAX_VALUE - CLOCK_SKEW_MS) + CLOCK_SKEW_MS
  require(value > 0 && value <= upper) { "时间戳无效或明显晚于当前时间，请检查设备时钟" }
 }
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
@Database(entities = [Entry::class, Profile::class], version = 2, exportSchema = true)
abstract class ArchiveDatabase : RoomDatabase() {
 abstract fun dao(): ArchiveDao
 companion object {
  val MIGRATION_1_2 = object : Migration(1, 2) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE records ADD COLUMN handCount INTEGER")
    db.execSQL("ALTER TABLE records ADD COLUMN volumeMl REAL")
    db.execSQL("ALTER TABLE records ADD COLUMN location TEXT")
   }
  }
 }
}

val ArchiveJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }
class ArchiveRepository(private val db: ArchiveDatabase) {
 val dao = db.dao()
 suspend fun preferences(): Preferences = dao.profile()?.let { ArchiveJson.decodeFromString<Preferences>(it.json) } ?: Preferences()
 suspend fun updatePreferences(change: (Preferences)->Preferences) = db.withTransaction {
  val old = preferences()
  val next = change(old); next.validate(); dao.putProfile(Profile(json=ArchiveJson.encodeToString(next)))
  PreferenceChange(old, next)
 }
 suspend fun save(entry: Entry) = db.withTransaction {
  entry.validate(); dao.put(entry)
  reconcileAchievements()
 }
 suspend fun delete(id: String) = db.withTransaction { dao.delete(id); reconcileAchievements() }
 suspend fun reconcileAchievements() = db.withTransaction {
  val old = preferences()
  val next = AchievementEngine.reconcile(dao.all(), old)
  if(next != old) dao.putProfile(Profile(json=ArchiveJson.encodeToString(next)))
 }
 suspend fun snapshot(): Pair<List<Entry>,Preferences> = db.withTransaction { dao.all() to preferences() }
 suspend fun restore(entries: List<Entry>, settings: Preferences, replace: Boolean) = db.withTransaction {
  entries.forEach { it.validate() }; settings.validate()
  require(entries.map { it.id }.distinct().size == entries.size) { "备份包含重复 ID" }
  val before = preferences()
  if(replace) { dao.clearEntries(); dao.putProfile(Profile(json=ArchiveJson.encodeToString(settings.copy(reminders=false)))) }
  val existing = dao.all().associateBy { it.id }
  entries.forEach { e -> if(replace || existing[e.id]?.modified?.let { e.modified > it } != false) dao.put(e) }
  reconcileAchievements()
  PreferenceChange(before, preferences())
 }
 suspend fun clear() = db.withTransaction { dao.clearEntries(); dao.clearProfile() }
}
data class PreferenceChange(val previous: Preferences, val current: Preferences)
