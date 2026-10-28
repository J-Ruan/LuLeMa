@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lulema.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.lulema.app.*
import com.lulema.app.data.*
import com.lulema.app.domain.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val Gray=Color(0xFF86868B)
private data class Tab(val route:String,val title:String,val icon:ImageVector)
private val tabs=listOf(Tab("home","首页",Icons.Outlined.Home),Tab("calendar","日历",Icons.Outlined.CalendarMonth),Tab("stats","统计",Icons.Outlined.BarChart),Tab("me","我的",Icons.Outlined.Person))

@Composable
fun ArchiveApp(vm:ArchiveViewModel) {
 val entries by vm.entries.collectAsStateWithLifecycle()
 val loadedPreferences by vm.settings.collectAsStateWithLifecycle()
 val p=loadedPreferences ?: run { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()};return }
 val busy by vm.busy.collectAsStateWithLifecycle()
 val preview by vm.preview.collectAsStateWithLifecycle()
 val dark=when(p.theme) {"dark"->true;"light"->false;else->isSystemInDarkTheme()}
 val colors=if(dark) darkColorScheme(primary=Color.White,onPrimary=Color.Black,background=Color.Black,surface=Color.Black,surfaceContainer=Color(0xFF1C1C1E),secondaryContainer=Color(0xFF333333)) else lightColorScheme(primary=Color(0xFF111111),onPrimary=Color.White,background=Color.White,surface=Color.White,surfaceContainer=Color(0xFFF5F5F7),secondaryContainer=Color(0xFFE8E8EB))
 MaterialTheme(colorScheme=colors) {
  val nav=rememberNavController()
  val route=nav.currentBackStackEntryAsState().value?.destination?.route?:"home"
  val snack=remember { SnackbarHostState() }
  LaunchedEffect(vm) { for(message in vm.messages)snack.showSnackbar(message) }
  var form by rememberSaveable { mutableStateOf(false) }
  var editId by rememberSaveable { mutableStateOf<String?>(null) }
  var newDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
  var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
  var secret by rememberSaveable { mutableStateOf(false) }
  val today by produceState(LocalDate.now()) { while(true) { value=LocalDate.now();delay(30000) } }
  fun add(day:LocalDate) { newDay=day.toEpochDay();editId=null;form=true }
  var achievement by remember { mutableStateOf<String?>(null) }
  var known by remember { mutableStateOf<Set<String>?>(null) }
  LaunchedEffect(p.achievements) {
   val old=known
   if(old!=null) achievement=(p.achievements.keys-old).firstOrNull()
   known=p.achievements.keys
  }
  LaunchedEffect(achievement) { if(achievement!=null) {delay(3200);achievement=null} }
  Scaffold(snackbarHost={SnackbarHost(snack)},bottomBar={
   NavigationBar(containerColor=colors.surface) { tabs.forEach { tab ->
    NavigationBarItem(selected=route==tab.route,onClick={
     if(tab.route=="stats")vm.unlock("stats")
     nav.navigate(tab.route) {popUpTo(nav.graph.startDestinationId) {saveState=true};launchSingleTop=true;restoreState=true}
    },icon={Icon(tab.icon,contentDescription=null)},label={Text(tab.title)})
   } }
  }) { padding ->
   Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
    NavHost(navController=nav,startDestination="home",modifier=Modifier.widthIn(max=760.dp).fillMaxSize(),enterTransition={fadeIn()},exitTransition={fadeOut()}) {
     composable("home") { Home(entries,today,{add(today)},{ e->editId=e.id;form=true },{vm.unlock("secret");secret=true}) }
     composable("calendar") { CalendarPage(entries,p,today,{add(it)},{e->editId=e.id;form=true},{vm.unlock("history")}) }
     composable("stats") { StatsPage(entries,today) {vm.unlock("year")} }
     composable("me") { SettingsPage(vm,p,busy,{nav.navigate("achievements")}) }
     composable("achievements") { AchievementsPage(p) {nav.popBackStack()} }
    }
    AnimatedVisibility(achievement!=null,modifier=Modifier.align(Alignment.TopCenter),enter=fadeIn()+expandVertically(),exit=fadeOut()+shrinkVertically()) {
     Surface(shape=RoundedCornerShape(24.dp),tonalElevation=8.dp,shadowElevation=4.dp,modifier=Modifier.padding(12.dp)) {
      Text("✧  已解锁 · ${Achievements.find { it.key==achievement }?.name.orEmpty()}",Modifier.padding(20.dp))
     }
    }
   }
  }
  if(form) {
   val editing=entries.find {it.id==editId}
   EntryForm(editing,newDay,{form=false},{entry->vm.save(entry) {form=false}},{deleteId=it})
  }
  if(deleteId!=null) Confirm("删除这条档案？","删除后无法撤销。",{deleteId=null}) {vm.delete(deleteId!!);deleteId=null;form=false}
  if(secret) AlertDialog(onDismissRequest={secret=false},title={Text("录取通知书")},text={Text("你已被人类迷惑行为研究所录取。\n工位自备，工资随缘。")},confirmButton={TextButton(onClick={secret=false}) {Text("收到，先摸会儿鱼")}})
  if(preview!=null) {
   val b=preview!!
   AlertDialog(onDismissRequest=vm::dismissPreview,title={Text("备份预览")},text={Text("格式 v${b.formatVersion} · ${b.records.size} 条记录\n累计 ${b.records.sumOf {it.duration.toLong()}} 分钟\n导出于 ${Instant.ofEpochMilli(b.exportedAt).atZone(ZoneId.systemDefault()).toLocalDate()}\n\n合并：保留本机设置，同 ID 使用较新记录。\n覆盖：替换全部记录、设置和成就，提醒默认关闭。")},dismissButton={TextButton(onClick=vm::dismissPreview) {Text("取消")}},confirmButton={ Column { TextButton(enabled=!busy,onClick={vm.restore(false)}){Text("合并导入")};TextButton(enabled=!busy,onClick={vm.restore(true)}){Text("确认覆盖全部数据")} } })
  }
 }
}

@Composable internal fun Page(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  Text(title,fontSize=34.sp,fontWeight=FontWeight.Bold)
  if(subtitle.isNotEmpty())Text(subtitle,color=Gray,style=MaterialTheme.typography.bodyMedium)
  content()
  Spacer(Modifier.height(12.dp))
 }
}
@Composable internal fun Tile(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Surface(modifier.fillMaxWidth().animateContentSize(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
 }
}
@Composable internal fun Section(title:String) {Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)}
@Composable internal fun Metric(label:String,value:String) { Column {Text(label,color=Gray,style=MaterialTheme.typography.labelLarge);AnimatedContent(targetState=value,label="metric") {Text(it,fontSize=28.sp,fontWeight=FontWeight.SemiBold)}} }
private fun timeLabel(e:Entry) = e.minute?.let {"%02d:%02d".format(it/60,it%60)}?:"未填写时间"
@Composable private fun RecordRow(e:Entry,onClick:()->Unit) {
 Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainer,modifier=Modifier.fillMaxWidth()) {
  Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(LocalDate.ofEpochDay(e.day).toString(),fontWeight=FontWeight.SemiBold)
    Text("${timeLabel(e)} · ${if(e.duration==0) "时长未填写" else "${e.duration} 分钟"}",color=Gray)
    if(e.mood.isNotEmpty())Text(e.mood,style=MaterialTheme.typography.labelMedium)
    if(e.note.isNotEmpty())Text(e.note,maxLines=2,style=MaterialTheme.typography.bodySmall)
   }
   Icon(Icons.Outlined.ChevronRight,"查看或编辑档案")
  }
 }
}
@Composable private fun Home(entries:List<Entry>,today:LocalDate,add:()->Unit,edit:(Entry)->Unit,secret:()->Unit) {
 var taps by remember { mutableIntStateOf(0) }
 var lastTap by remember { mutableLongStateOf(0L) }
 var quote by rememberSaveable {mutableIntStateOf(0)}
 val subtitle=remember { Copybook.subtitles.random() }
 val month=Analysis.summary(Analysis.month(entries,YearMonth.from(today)))
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  Text("撸了吗",fontSize=40.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable {
   val now=System.currentTimeMillis();taps=if(now-lastTap>2000)1 else taps+1;lastTap=now
   if(taps>=5) {secret();taps=0}
  })
  Text(subtitle,color=Gray)
  Tile {
   Text(today.format(DateTimeFormatter.ofPattern("M 月 d 日 · EEEE",java.util.Locale.CHINA)),color=Gray)
   Text(if(entries.any {it.day==today.toEpochDay()}) "今日档案已入库" else "今日暂无神秘操作记录",fontSize=24.sp,fontWeight=FontWeight.SemiBold)
   Text("按自己的节奏生活。记录只是记录。",color=Gray)
   val interactions=remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
   val pressed by interactions.collectIsPressedAsState()
   val scale by animateFloatAsState(if(pressed)0.97f else 1f,label="button")
   Button(onClick=add,interactionSource=interactions,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp).scale(scale)) {Text("+ 记一笔",fontSize=18.sp)}
  }
  Section("本月概览")
  Tile {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Metric("记录", "${month.count} 笔");Metric("累计时长","${month.minutes} 分钟")};Text("最近一次 · ${month.latest?.let {LocalDate.ofEpochDay(it)}?:"暂无"}",color=Gray)}
  Section("最近一次记录")
  entries.firstOrNull()?.let { e->RecordRow(e) {edit(e)} } ?: Text("档案柜还是空的，不着急。",color=Gray)
  Tile {Text("今日抽象语录",color=Gray);Text(Copybook.quotes[quote%Copybook.quotes.size],fontSize=19.sp);TextButton(onClick={quote++}) {Text("换一个结论")}}
  Text("娱乐性记录工具，不提供医学诊断。",color=Gray,style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun CalendarPage(entries:List<Entry>,p:Preferences,today:LocalDate,add:(LocalDate)->Unit,edit:(Entry)->Unit,history:()->Unit) {
 var monthText by rememberSaveable {mutableStateOf(YearMonth.from(today).toString())}
 var selectedDay by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
 val month=YearMonth.parse(monthText)
 val selected=LocalDate.ofEpochDay(selectedDay)
 val marks=remember(entries) {entries.groupingBy {it.day}.eachCount()}
 Page("日历","历史不会说谎，除非你填错了日期。") {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
   IconButton(enabled=month>YearMonth.of(1900,1),onClick={monthText=month.minusMonths(1).toString();selectedDay=month.minusMonths(1).atDay(1).toEpochDay();history()}) {Icon(Icons.Outlined.ChevronLeft,"上个月")}
   Text("${month.year} 年 ${month.monthValue} 月",fontWeight=FontWeight.SemiBold)
   IconButton(enabled=month<YearMonth.from(today),onClick={monthText=month.plusMonths(1).toString();selectedDay=month.plusMonths(1).atDay(1).toEpochDay()}) {Icon(Icons.Outlined.ChevronRight,"下个月")}
  }
  TextButton(onClick={monthText=YearMonth.from(today).toString();selectedDay=today.toEpochDay()}) {Text("返回今天")}
  AnimatedContent(targetState=month,label="month") { shown ->
   Column {
    Row { (if(p.mondayFirst) listOf("一","二","三","四","五","六","日") else listOf("日","一","二","三","四","五","六")).forEach {Text(it,Modifier.weight(1f).wrapContentWidth(),color=Gray)} }
    Analysis.calendar(shown,p.mondayFirst).chunked(7).forEach { week ->
     Row(Modifier.fillMaxWidth()) {week.forEach {day ->
      val chosen=day?.toEpochDay()==selectedDay
      Box(Modifier.weight(1f).heightIn(min=52.dp).then(if(day!=null && day<=today) Modifier.clickable {selectedDay=day.toEpochDay()} else Modifier),contentAlignment=Alignment.Center) {
       if(day!=null) Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Surface(shape=RoundedCornerShape(12.dp),color=if(chosen)MaterialTheme.colorScheme.primary else Color.Transparent) {Text("${day.dayOfMonth}",Modifier.padding(8.dp),color=if(chosen)MaterialTheme.colorScheme.onPrimary else if(day>today)Gray else MaterialTheme.colorScheme.onSurface)}
        if(marks.containsKey(day.toEpochDay()))Text("•",fontSize=10.sp,modifier=Modifier.semantics {contentDescription="${day} 有 ${marks[day.toEpochDay()]} 条记录"})
       }
      }
     }}
    }
   }
  }
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(selected.toString(),Modifier.weight(1f),fontWeight=FontWeight.Bold);TextButton(onClick={add(selected)}) {Text("+ 记一笔")}}
  val records=entries.filter {it.day==selectedDay}
  if(records.isEmpty())Text("这一天没有档案。留白也很好。",color=Gray)
  records.forEach {e->RecordRow(e) {edit(e)}}
 }
}

@Composable private fun EntryForm(entry:Entry?,day:Long,dismiss:()->Unit,save:(Entry)->Unit,delete:(String)->Unit) {
 val context=LocalContext.current
 val key=entry?.id?:"new-$day"
 var date by rememberSaveable(key) {mutableLongStateOf(entry?.day?:day)}
 var minute by rememberSaveable(key) {mutableStateOf(entry?.minute)}
 var duration by rememberSaveable(key) {mutableStateOf(entry?.duration?.takeIf {it>0}?.toString()?:"")}
 var mood by rememberSaveable(key) {mutableStateOf(entry?.mood?:"")}
 var note by rememberSaveable(key) {mutableStateOf(entry?.note?:"")}
 var error by remember {mutableStateOf("")}
 val stableId = rememberSaveable(key) { entry?.id ?: java.util.UUID.randomUUID().toString() }
 val stableCreated = rememberSaveable(key) { entry?.created ?: System.currentTimeMillis() }
 val base = remember(key,stableId) { entry ?: Entry(id=stableId,day=day,created=stableCreated) }
 ModalBottomSheet(onDismissRequest=dismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(if(entry==null)"记一笔" else "档案详情",fontSize=28.sp,fontWeight=FontWeight.Bold)
   OutlinedButton(onClick={val d=LocalDate.ofEpochDay(date);DatePickerDialog(context,{_,y,m,v->date=LocalDate.of(y,m+1,v).toEpochDay()},d.year,d.monthValue-1,d.dayOfMonth).apply {datePicker.minDate=LocalDate.of(1900,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();datePicker.maxDate=System.currentTimeMillis()}.show()}) {Text("日期  ${LocalDate.ofEpochDay(date)}")}
   Row(verticalAlignment=Alignment.CenterVertically) {
    OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->minute=h*60+m},minute?.div(60)?:12,minute?.rem(60)?:0,true).show()}) {Text(minute?.let {"时间  %02d:%02d".format(it/60,it%60)}?:"选择时间（可选）")}
    if(minute!=null) TextButton(onClick={minute=null}) {Text("移除")}
   }
   OutlinedTextField(value=duration,onValueChange={if(it.length<=4 && it.all(Char::isDigit)) duration=it},label={Text("持续时长 / 分钟（可选）")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Copybook.moods.forEach { m->FilterChip(selected=mood==m,onClick={mood=if(mood==m)"" else m},label={Text(m)})} }
   OutlinedTextField(value=mood,onValueChange={if(it.length<=20)mood=it},label={Text("心情 / 自定义（可选，20 字内）")},singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=note,onValueChange={if(it.length<=1000)note=it},label={Text("个人备注（可选，1000 字内）")},maxLines=4,modifier=Modifier.fillMaxWidth())
   if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
   Button(onClick={
    val e=base.copy(day=date,minute=minute,duration=if(duration.isBlank())0 else duration.toIntOrNull()?:-1,mood=mood.trim(),note=note.trim(),modified=maxOf(System.currentTimeMillis(),base.created,base.modified))
    runCatching {e.validate()}.onSuccess {save(e)}.onFailure {error=it.message?:"请检查输入"}
   },modifier=Modifier.fillMaxWidth()) {Text("保存档案")}
   if(entry!=null)TextButton(onClick={delete(entry.id)}) {Text("删除这条档案",color=MaterialTheme.colorScheme.error)}
  }
 }
}
@Composable internal fun Confirm(title:String,body:String,dismiss:()->Unit,confirm:()->Unit) {
 AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Text(body)},dismissButton={TextButton(onClick=dismiss) {Text("取消")}},confirmButton={TextButton(onClick=confirm) {Text("确认")}})
}
