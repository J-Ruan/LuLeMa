package com.lulema.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.lulema.app.data.*
import com.lulema.app.backup.*
import com.lulema.app.notification.ReminderScheduler
import com.lulema.app.domain.ReminderConfiguration
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
 init { task { repo.reconcileAchievements(); ReminderScheduler.update(app,repo.preferences(),false) } }
 private fun task(block:suspend ()->Unit) { viewModelScope.launch { try { block() } catch(e:CancellationException) { throw e } catch(_:IllegalArgumentException) { messages.send("输入数据无效，请检查填写内容和设备时间。") } catch(_:Exception) { messages.send("本地数据操作未完成，请重试。") } } }
 fun save(entry:Entry,onSaved:()->Unit) = task { repo.save(entry);messages.send("档案已保存，宇宙依然正常运转");onSaved() }
 fun delete(id:String)=task { repo.delete(id);messages.send("档案已删除；已解锁勋章继续保留") }
 fun unlock(key:String)=task { repo.updatePreferences { it.unlock(key) } }
 fun setting(key:String,change:(Preferences)->Preferences)=task {
  val changeResult = repo.updatePreferences { old ->
   val changed=change(old)
   if(changed==old) return@updatePreferences old
   var next=changed.copy(changedSettings=old.changedSettings+key)
   if(next.theme=="dark")next=next.unlock("dark")
   if(next.changedSettings.size>=3)next=next.unlock("settings")
   next
  }
  if(ReminderConfiguration.changed(changeResult.previous,changeResult.current)) schedule(changeResult.current)
 }
 fun clear()=task { repo.clear(); _preview.value=null;ReminderScheduler.update(getApplication(),Preferences());messages.send("全部本地档案、设置和成就已清除") }
 fun cancelled() = task { messages.send("操作已取消，未更改档案。") }
 private suspend fun schedule(preferences:Preferences) {
  try { ReminderScheduler.update(getApplication(),preferences) }
  catch(e:CancellationException) { throw e }
  catch(_:Exception) { messages.send("设置已保存，但提醒计划未能更新。请检查系统设置后重试。") }
 }
 fun export(uri:Uri,password:String)=backupTask("export") {
  val (records,p)=repo.snapshot()
  val chars=password.takeIf { it.isNotEmpty() }?.toCharArray()
  val bytes=try {BackupCodec.encode(Backup(records=records,settings=p),chars)} finally { chars?.fill('\u0000') }
  getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) } ?: throw java.io.IOException()
  repo.updatePreferences { it.unlock("backup") };messages.send("备份已保存，请妥善保管文件和密码")
 }
 fun inspect(uri:Uri,password:String)=backupTask("inspect") {
  _preview.value=null
  val bytes=getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
   val output=java.io.ByteArrayOutputStream()
   val buffer=ByteArray(8192)
   while(true) {val n=stream.read(buffer);if(n<0)break;require(output.size()+n<=BackupCodec.MAX_BYTES);output.write(buffer,0,n)}
   output.toByteArray() } ?: throw java.io.IOException()
  val chars=password.takeIf { it.isNotEmpty() }?.toCharArray()
  _preview.value=try {BackupCodec.decode(bytes,chars)} finally { chars?.fill('\u0000') }
 }
 private fun backupTask(operation:String,block:suspend ()->Unit) {
  if(_busy.value) return
  _busy.value=true
  viewModelScope.launch {
   try { withContext(Dispatchers.IO) { block() } }
   catch(e:CancellationException) {throw e}
   catch(error:Exception) {
    val message=when {
     operation=="restore" -> "数据恢复失败，事务已回滚，原数据未被替换。"
     error is BackupPasswordRequiredException -> "这是加密备份，请输入至少 8 位的备份密码后重试。"
     error is javax.crypto.BadPaddingException -> "备份密码错误或文件已损坏，原数据未被更改。"
     error is java.io.IOException || error is SecurityException -> if(operation=="export") "文件无法写入，请重新选择保存位置；未完成的文件请勿使用。" else "文件无法读取，请检查文件位置和访问权限。"
     operation=="export" -> "导出未完成，请检查密码长度、数据大小和设备时间。"
     else -> "备份格式、内容或时间无效，或文件超过 16 MiB；原数据未被更改。"
    }
    messages.send(message)
   } finally {_busy.value=false}
  }
 }
 fun dismissPreview() {_preview.value=null}
 fun restore(replace:Boolean)=backupTask("restore") {
  val b=_preview.value?:return@backupTask
  val result=repo.restore(b.records,b.settings,replace);_preview.value=null
  if(ReminderConfiguration.changed(result.previous,result.current)) schedule(result.current)
  messages.send(if(replace) "已覆盖导入；提醒默认关闭" else "已合并导入，同 ID 仅保留较新记录")
 }
}
