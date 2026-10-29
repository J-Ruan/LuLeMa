package com.lulema.app

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.lulema.app.notification.ReminderScheduler
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReminderTest {
 @Test fun nicknameThemeAndWeekStartKeepTheActualScheduledWorkId()=runBlocking {
  val inst=InstrumentationRegistry.getInstrumentation()
  val app=inst.targetContext.applicationContext as LuLeMaApplication
  val repo=app.repository
  repo.clear()
  repo.updatePreferences {it.copy(reminders=true)}
  ReminderScheduler.update(app,repo.preferences())
  val work=WorkManager.getInstance(app)
  fun id()=work.getWorkInfosForUniqueWork("archive-reminder").get(20,TimeUnit.SECONDS).first { !it.state.isFinished }.id
  val before=id()
  val store=ViewModelStore()
  lateinit var vm:ArchiveViewModel
  inst.runOnMainSync {vm=ArchiveViewModel(app);store.put("reminder-test",vm)}
  suspend fun waitFor(check:suspend ()->Boolean) {withTimeout(20000) {while(!check())delay(50)}}
  try {
   inst.runOnMainSync {vm.setting("nickname") {it.copy(nickname="虚构调度测试员")}}
   waitFor {repo.preferences().nickname=="虚构调度测试员"};assertEquals(before,id())
   inst.runOnMainSync {vm.setting("theme") {it.copy(theme="dark")}}
   waitFor {repo.preferences().theme=="dark"};assertEquals(before,id())
   inst.runOnMainSync {vm.setting("weekStart") {it.copy(mondayFirst=false)}}
   waitFor {!repo.preferences().mondayFirst};assertEquals(before,id())
   inst.runOnMainSync {vm.setting("reminderTime") {it.copy(reminderHour=7)}}
   waitFor {id()!=before}
   inst.runOnMainSync {vm.setting("reminders") {it.copy(reminders=false)}}
   waitFor {work.getWorkInfosForUniqueWork("archive-reminder").get(20,TimeUnit.SECONDS).none { !it.state.isFinished }}
  } finally {
   inst.runOnMainSync {store.clear()}
   repo.clear();ReminderScheduler.update(app,repo.preferences())
  }
 }
}
