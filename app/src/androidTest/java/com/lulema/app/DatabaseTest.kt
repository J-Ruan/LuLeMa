package com.lulema.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lulema.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:ArchiveDatabase
 private lateinit var repo:ArchiveRepository
 private val name="test-${UUID.randomUUID()}.db"
 @Before fun open() {db=Room.databaseBuilder(context,ArchiveDatabase::class.java,name).build();repo=ArchiveRepository(db)}
 @After fun close() {db.close();context.deleteDatabase(name)}
 @Test fun createEditDeleteMultipleHistoricalAndPersistence()=runBlocking {
  val e=Entry(day=LocalDate.of(2024,2,29).toEpochDay(),duration=15)
  repo.save(e);repo.save(e.copy(id=UUID.randomUUID().toString(),duration=20))
  assertEquals(2,repo.dao.all().size)
  repo.save(e.copy(duration=30,modified=e.modified+1));assertEquals(50,repo.dao.all().sumOf {it.duration})
  repo.dao.delete(e.id);assertEquals(1,repo.dao.all().size)
  repo.updatePreferences {it.copy(theme="dark",nickname="虚构用户").unlock("dark")}
  db.close();db=Room.databaseBuilder(context,ArchiveDatabase::class.java,name).build();repo=ArchiveRepository(db)
  assertEquals(1,repo.dao.all().size);assertEquals("dark",repo.preferences().theme);assertTrue("dark" in repo.preferences().achievements)
 }
 @Test fun importsMergeDuplicateIdsReplaceAndRollback()=runBlocking {
  val e=Entry(duration=12);repo.save(e)
  repo.restore(listOf(e.copy(duration=50,modified=e.modified+2)),Preferences(),false)
  repo.restore(listOf(e),Preferences(),false)
  assertEquals(1,repo.dao.all().size);assertEquals(50,repo.dao.all().single().duration)
  try {repo.restore(listOf(e.copy(duration=-1)),Preferences(),true);fail("Expected invalid import")}catch(_:IllegalArgumentException){}
  assertEquals(50,repo.dao.all().single().duration)
  repo.restore(emptyList(),Preferences(theme="dark",reminders=true),true)
  assertTrue(repo.dao.all().isEmpty());assertEquals("dark",repo.preferences().theme);assertFalse(repo.preferences().reminders)
  repo.clear();assertEquals(Preferences(),repo.preferences())
 }
}
