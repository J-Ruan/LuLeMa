package com.lulema.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lulema.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MigrationTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 @get:Rule val helper=MigrationTestHelper(instrumentation,ArchiveDatabase::class.java)
 @Test fun realV1DatabaseUpgradesWithoutLosingRecordsPreferencesOrBadges()=runBlocking {
  val name="migration-${UUID.randomUUID()}.db"
  val id=UUID.randomUUID().toString()
  val profile=Preferences(nickname="虚构旧用户",theme="dark",mondayFirst=false,reminders=true,achievements=mapOf("first" to 1700000000000))
  helper.createDatabase(name,1).apply {
   execSQL("INSERT INTO records (id,day,minute,duration,mood,note,created,modified) VALUES (?,?,?,?,?,?,?,?)",arrayOf<Any?>(id,19782,123,15,"平静","虚构旧备注",1700000000000L,1700000000000L))
   execSQL("INSERT INTO profile (id,json) VALUES (?,?)",arrayOf<Any?>(1,ArchiveJson.encodeToString(profile)))
   close()
  }
  helper.runMigrationsAndValidate(name,2,true,ArchiveDatabase.MIGRATION_1_2).apply {
   query("SELECT handCount,volumeMl,location FROM records").use {cursor->assertTrue(cursor.moveToFirst());repeat(3) {assertTrue(cursor.isNull(it))}}
   close()
  }
  val db=Room.databaseBuilder(instrumentation.targetContext,ArchiveDatabase::class.java,name).addMigrations(ArchiveDatabase.MIGRATION_1_2).build()
  try {
   val repo=ArchiveRepository(db)
   assertEquals(profile,repo.preferences())
   val old=repo.dao.all().single()
   assertEquals(id,old.id);assertEquals("虚构旧备注",old.note);assertEquals(15,old.duration);assertNull(old.handCount);assertNull(old.volumeMl);assertNull(old.location)
   repo.save(old.copy(handCount=100,volumeMl=1.125,location="虚构升级后地点",modified=System.currentTimeMillis()))
   assertEquals(100,repo.dao.all().single().handCount)
   repo.save(Entry(duration=3));assertEquals(2,repo.dao.all().size)
   repo.delete(id);assertEquals(1,repo.dao.all().size)
   assertEquals(1700000000000L,repo.preferences().achievements["first"])
  } finally {db.close();instrumentation.targetContext.deleteDatabase(name)}
 }
}
