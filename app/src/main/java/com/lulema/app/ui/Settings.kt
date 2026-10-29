package com.lulema.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lulema.app.*
import com.lulema.app.data.Preferences
import com.lulema.app.domain.Achievements
import java.time.*

@Composable internal fun SettingsPage(vm:ArchiveViewModel,p:Preferences,busy:Boolean,achievements:()->Unit) {
 val context=LocalContext.current
 var dialog by rememberSaveable {mutableStateOf<String?>(null)}
 var nickname by rememberSaveable(p.nickname) {mutableStateOf(p.nickname)}
 var interval by rememberSaveable(p.reminderDays) {mutableStateOf(p.reminderDays.toString())}
 var encrypted by remember {mutableStateOf(true)}
 var password by remember {mutableStateOf("")}
 var info by remember {mutableStateOf("")}
 val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri->
  val secret=vm.pendingBackupPassword;vm.pendingBackupPassword=null
  if(uri!=null && secret!=null)vm.export(uri,secret) else vm.cancelled()
 }
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
  val secret=vm.pendingBackupPassword;vm.pendingBackupPassword=null
  if(uri!=null && secret!=null)vm.inspect(uri,secret) else vm.cancelled()
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->
  if(granted)vm.setting("reminders") {it.copy(reminders=true)} else info="通知权限未授予，提醒保持关闭。可在系统设置中开启。"
 }
 Page("我的","${p.nickname}，研究所欢迎你。") {
  Tile {
   Section("个人设置")
   OutlinedTextField(value=nickname,onValueChange={if(it.length<=30)nickname=it},label={Text("本地昵称")},singleLine=true,modifier=Modifier.fillMaxWidth())
   TextButton(enabled=nickname.isNotBlank(),onClick={vm.setting("nickname") {it.copy(nickname=nickname.trim())}}) {Text("保存昵称")}
   Text("外观",color=MaterialTheme.colorScheme.onSurfaceVariant)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("system" to "跟随系统","light" to "浅色","dark" to "深色").forEach {(key,label)->FilterChip(selected=p.theme==key,onClick={vm.setting("theme") {it.copy(theme=key)}},label={Text(label)})}}
   Row {Text("星期一作为一周开始",Modifier.weight(1f));Switch(checked=p.mondayFirst,onCheckedChange={value->vm.setting("weekStart") {it.copy(mondayFirst=value)}})}
  }
  Tile {
   Section("本地提醒")
   Row {Column(Modifier.weight(1f)) {Text("启用低频提醒");Text("默认关闭 · 不要求你做任何事",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)};Switch(checked=p.reminders,onCheckedChange={enabled->
    if(enabled && Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    else vm.setting("reminders") {it.copy(reminders=enabled)}
   })}
   TextButton(onClick={TimePickerDialog(context,{_,h,m->vm.setting("reminderTime") {it.copy(reminderHour=h,reminderMinute=m)}},p.reminderHour,p.reminderMinute,true).show()}) {Text("提醒时间 · %02d:%02d".format(p.reminderHour,p.reminderMinute))}
   OutlinedTextField(value=interval,onValueChange={if(it.length<=2 && it.all(Char::isDigit))interval=it},label={Text("间隔天数 / 7–90 天（7 天为每周）")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
   TextButton(enabled=interval.toIntOrNull() in 7..90,onClick={vm.setting("reminderInterval") {it.copy(reminderDays=interval.toInt())}}) {Text("保存频率")}
   Text("预览：档案馆提醒\n重要数据记得备份。按自己的节奏生活。",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("锁屏默认仅显示中性通知。系统可能延迟投递；不是精确闹钟。跨时区或夏令时变化后，可调整提醒时间，或关闭后重新开启。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(Build.VERSION.SDK_INT>=33 && p.reminders && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)Text("系统通知权限已关闭，请在系统设置中恢复或关闭此开关。",color=MaterialTheme.colorScheme.error)
  }
  Tile {
   Section("数据与隐私")
   Text("记录、手部运动次数、出货量、手填地点和勋章只在本机数据库。无网络和定位权限，系统自动备份已关闭；卸载前请主动导出。",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Button(enabled=!busy,onClick={password="";encrypted=true;dialog="export"},modifier=Modifier.fillMaxWidth()) {Text("导出备份")}
   OutlinedButton(enabled=!busy,onClick={password="";dialog="import"},modifier=Modifier.fillMaxWidth()) {Text("导入备份")}
   if(busy) {LinearProgressIndicator(Modifier.fillMaxWidth());Text("正在处理备份，请稍候")}
   TextButton(enabled=!busy,onClick={dialog="clear"}) {Text("清除全部数据",color=MaterialTheme.colorScheme.error)}
  }
  Tile {Section("成就中心");Text("${p.achievements.size} / ${Achievements.size} · 研究生活的小纪念");TextButton(onClick=achievements) {Text("查看成就")}}
  Tile {
   Section("关于撸了吗")
   Text("LuLeMa · v${BuildConfig.VERSION_NAME}\nSerious tracking. Ridiculous vibes.")
   Text("这是娱乐性记录工具，不是医学诊断工具。无需登录，无广告，无追踪。\nMIT License · © 2026 LuLeMa contributors",color=MaterialTheme.colorScheme.onSurfaceVariant)
   TextButton(onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,"https://github.com/J-Ruan/LuLeMa".toUri()))}.onFailure {info="没有可用浏览器，请手动访问 github.com/J-Ruan/LuLeMa"}}) {Text("GitHub 开源项目 ↗")}
   Text("外部链接由系统浏览器打开，App 本身不申请网络权限。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
 if(dialog=="clear")Confirm("清除全部本地数据？","这会永久删除记录、设置和成就，并关闭提醒。无法撤销，请先备份。",{dialog=null}) {vm.clear();dialog=null}
 if(dialog=="export" || dialog=="import") {
  val exporting=dialog=="export"
  AlertDialog(onDismissRequest={dialog=null;password=""},title={Text(if(exporting)"导出备份" else "导入备份")},text={
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(exporting)Row {Text("密码保护",Modifier.weight(1f));Switch(checked=encrypted,onCheckedChange={encrypted=it})}
    if(!exporting || encrypted) {
     Text("加密备份请输入至少 8 位密码。明文导入可留空。密码无法找回。",color=MaterialTheme.colorScheme.onSurfaceVariant)
     OutlinedTextField(value=password,onValueChange={if(it.length<=128)password=it},label={Text("备份密码")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),singleLine=true)
    }
    if(exporting && !encrypted)Text("注意：明文文件包含私密记录、时长、手部运动次数、出货量、地点、备注、设置和成就。选择安全位置并妥善保管。",color=MaterialTheme.colorScheme.error)
    if(!exporting)Text("先验证并预览文件，再选择合并或覆盖。无效文件不会改变原有数据。")
   }
  },dismissButton={TextButton(onClick={dialog=null;password=""}) {Text("取消")}},confirmButton={
   TextButton(enabled=!exporting || !encrypted || password.length>=8,onClick={
    vm.pendingBackupPassword=if(exporting && !encrypted)"" else password
    password="";dialog=null
    if(exporting)exportLauncher.launch("LuLeMa-${LocalDate.now()}.lulema") else importLauncher.launch(arrayOf("*/*"))
   }) {Text(if(exporting)"选择保存位置" else "选择备份文件")}
  })
 }
 if(info.isNotEmpty())AlertDialog(onDismissRequest={info=""},text={Text(info)},confirmButton={TextButton(onClick={info=""}) {Text("知道了")}})
}
