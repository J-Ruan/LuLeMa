package com.lulema.app

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lulema.app.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupDeviceTest {
 @Test fun viewModelExportsPreviewsRestoresAllFieldsAndRejectsTamperingWithoutMutation()=runBlocking {
  val inst=InstrumentationRegistry.getInstrumentation()
  val app=inst.targetContext.applicationContext as LuLeMaApplication
  val repo=app.repository
  repo.clear()
  val record=Entry(duration=2,handCount=123,volumeMl=1.234,location="虚构文件测试地点")
  repo.save(record)
  val file=File(app.cacheDir,"synthetic-v2-backup.lulema")
  val store=ViewModelStore()
  lateinit var vm:ArchiveViewModel
  inst.runOnMainSync {vm=ArchiveViewModel(app);store.put("backup-test",vm)}
  suspend fun finished()=withTimeout(180000) {while(vm.busy.value)delay(100)}
  try {
   inst.runOnMainSync {vm.export(Uri.fromFile(file),"synthetic-password")}
   finished()
   assertTrue(file.exists());assertFalse(file.readText().contains("虚构文件测试地点"));assertTrue(file.readText().contains("ciphertext"))
   repo.clear()
   inst.runOnMainSync {vm.inspect(Uri.fromFile(file),"synthetic-password")}
   finished()
   assertEquals(2,vm.preview.value!!.formatVersion)
   assertTrue(repo.dao.all().isEmpty())
   inst.runOnMainSync {vm.restore(true)}
   finished()
   assertEquals(record,repo.dao.all().single());assertTrue("duration_under_3" in repo.preferences().achievements)
   val snapshot=repo.snapshot()
   val text=file.readText();val marker="\"ciphertext\":\"";val index=text.indexOf(marker)+marker.length
   file.writeText(text.substring(0,index)+(if(text[index]=='A')"B" else "A")+text.substring(index+1))
   inst.runOnMainSync {vm.inspect(Uri.fromFile(file),"synthetic-password")}
   finished()
   assertNull(vm.preview.value);assertEquals(snapshot,repo.snapshot())
  } finally {
   inst.runOnMainSync {store.clear()}
   file.delete();repo.clear()
  }
 }
}
