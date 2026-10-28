package com.lulema.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.lulema.app.data.*
import com.lulema.app.backup.*
import com.lulema.app.notification.ReminderScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class ArchiveViewModel(app:Application):AndroidViewModel(app) {
 private val repo=(app as LuLeMaApplication).repository
 val entries=repo.dao.observeEntries().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings=repo.dao.observeProfile().map<Profile?, Preferences?> { it?.let { profile -> ArchiveJson.decodeFromString<Preferences>(profile.json) } ?: Preferences() }.stateIn(viewModelScope,SharingStarted.Eagerly,null)
 var pendingBackupPassword: String? = null
 val messages=Channel<String>(Channel.BUFFERED)
 private val _preview=MutableStateFlow<Backup?>(null)
 val preview=_preview.asStateFlow()
 private val _busy=MutableStateFlow(false)
 val busy=_busy.asStateFlow()
 init { task { ReminderScheduler.update(app,repo.preferences(),false) } }
 private fun task(block:suspend ()->Unit) { viewModelScope.launch { try { block() } catch(e:CancellationException) { throw e } catch(_:Exception) { messages.send("操作未完成。请检查输入、备份密码或文件是否有效。") } } }
 fun save(entry:Entry,onSaved:()->Unit) = task { repo.save(entry);messages.send("档案已保存，宇宙依然正常运转");onSaved() }
 fun delete(id:String)=task { repo.dao.delete(id);messages.send("档案已删除") }
 fun unlock(key:String)=task { repo.updatePreferences { it.unlock(key) } }
 fun setting(key:String,change:(Preferences)->Preferences)=task {
  repo.updatePreferences { old ->
   val changed=change(old)
   if(changed==old) return@updatePreferences old
   var next=changed.copy(changedSettings=old.changedSettings+key)
   if(next.theme=="dark")next=next.unlock("dark")
   if(next.changedSettings.size>=3)next=next.unlock("settings")
   next
  }
  ReminderScheduler.update(getApplication(),repo.preferences())
 }
 fun clear()=task { repo.clear(); _preview.value=null;ReminderScheduler.update(getApplication(),Preferences());messages.send("全部本地档案、设置和成就已清除") }
 fun export(uri:Uri,password:String)=backupTask {
  val (records,p)=repo.snapshot()
  val bytes=BackupCodec.encode(Backup(records=records,settings=p),password.takeIf { it.isNotEmpty() }?.toCharArray())
  getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) } ?: error("无法写入")
  repo.updatePreferences { it.unlock("backup") };messages.send("备份已保存，请妥善保管文件和密码")
 }
 fun inspect(uri:Uri,password:String)=backupTask {
  val bytes=getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
   val output=java.io.ByteArrayOutputStream()
   val buffer=ByteArray(8192)
   while(true) {val n=stream.read(buffer);if(n<0)break;require(output.size()+n<=BackupCodec.MAX_BYTES);output.write(buffer,0,n)}
   output.toByteArray() } ?: error("无法读取")
  _preview.value=BackupCodec.decode(bytes,password.takeIf { it.isNotEmpty() }?.toCharArray())
 }
 private fun backupTask(block:suspend ()->Unit) {
  if(_busy.value) return
  _busy.value=true
  viewModelScope.launch { try { withContext(Dispatchers.IO) { block() } } catch(e:CancellationException) {throw e} catch(_:Exception) {messages.send("备份操作失败：文件无效、密码错误、文件过大或无法访问。原有数据未被替换。")} finally {_busy.value=false} }
 }
 fun dismissPreview() {_preview.value=null}
 fun restore(replace:Boolean)=backupTask {
  val b=_preview.value?:return@backupTask
  repo.restore(b.records,b.settings,replace);_preview.value=null
  ReminderScheduler.update(getApplication(),repo.preferences())
  messages.send(if(replace) "已覆盖导入；提醒默认关闭" else "已合并导入，同 ID 仅保留较新记录")
 }
}
