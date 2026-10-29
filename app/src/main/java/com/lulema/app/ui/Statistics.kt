package com.lulema.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.lulema.app.data.Entry
import com.lulema.app.domain.Analysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*

private data class Charts(val days:List<Pair<LocalDate,Int>>,val months:List<Float>,val durations:List<Float>,val heat:Map<Long,Int>)
@Composable
internal fun StatsPage(entries:List<Entry>,today:LocalDate,mondayFirst:Boolean,yearViewed:()->Unit) {
 var range by rememberSaveable {mutableIntStateOf(7)}
 var year by rememberSaveable {mutableIntStateOf(today.year)}
 var annual by rememberSaveable {mutableStateOf(false)}
 val charts by produceState(Charts(emptyList(),emptyList(),emptyList(),emptyMap()),entries,range,year,today) {
  value=withContext(Dispatchers.Default) {
   Charts(Analysis.daily(entries,today,range),(1..12).map {Analysis.month(entries,YearMonth.of(year,it)).size.toFloat()},entries.filter {it.duration>0 && it.day in today.minusDays(range.toLong()-1).toEpochDay()..today.toEpochDay()}.sortedWith(compareBy<Entry>{it.day}.thenBy {it.minute?:-1}).map {it.duration.toFloat()},Analysis.year(entries,year).groupingBy {it.day}.eachCount())
  }
 }
 Page("统计","这数据，严谨得有点离谱。") {
  val all=Analysis.summary(entries)
  val monthly=Analysis.summary(Analysis.month(entries,YearMonth.from(today)))
  Tile {Metric("历史档案","${all.count} 次");Metric("本月档案","${monthly.count} 次");Metric("累计时长","${all.minutes} 分钟");Metric("平均已填时长",if(all.measuredCount==0)"暂无数据" else "%.1f 分钟".format(all.average));Text("最近一次 · ${all.latest?.let {LocalDate.ofEpochDay(it)}?:"暂无"}",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  Text("频率只是个人记录，不是成绩。平均值与时长趋势仅含大于 0 的已填写时长；未填写 ${all.missingDurationCount} 次。",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {listOf(7,30).forEach {days->FilterChip(selected=range==days,onClick={range=days},label={Text("最近 $days 天")})}}
  ChartCard("最近 $range 天 · 每日记录",charts.days.map {it.second.toFloat()},"${today.minusDays(range.toLong()-1)} — $today", "次")
  ChartCard("单次时长变化",charts.durations,"仅已填时长，按档案日期、时间排列；共 ${charts.durations.size} 次","分钟",true)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   TextButton(enabled=year>1900,onClick={year--}) {Text("上一年")}
   TextButton(onClick={annual=!annual;if(annual)yearViewed()}) {Text("$year 年度报告")}
   TextButton(enabled=year<today.year,onClick={year++}) {Text("下一年")}
  }
  ChartCard("每月档案观察中心",charts.months,"$year 年 · 从 1 月至 12 月","次")
  if(annual) {
   Tile {
    Section("人类迷惑行为年度研究报告")
    val stats=Analysis.summary(Analysis.year(entries,year))
    Text("$year 年共记录 ${stats.count} 次 · ${stats.minutes} 分钟")
    Text("宇宙照常运转。谢谢你照顾自己的生活。",color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  YearHeatmap(year,entries,mondayFirst)
 }
}
@Composable internal fun YearHeatmap(year:Int,entries:List<Entry>,mondayFirst:Boolean) {
 val heat=remember(entries,year) {Analysis.year(entries,year).groupingBy {it.day}.eachCount()}
 Tile {
   Section("年度日历热力图")
   Text("$year 年 · 每列一周，${if(mondayFirst)"周一至周日" else "周日至周六"}。空白代表无记录。",color=MaterialTheme.colorScheme.onSurfaceVariant)
   val start=LocalDate.of(year,1,1)
   val count=start.lengthOfYear()
   val offset=(start.dayOfWeek.value-(if(mondayFirst)1 else 7)+7)%7
   val weeks=(offset+count+6)/7
   val color=MaterialTheme.colorScheme.onSurface
   Box(Modifier.horizontalScroll(rememberScrollState())) {
    Canvas(Modifier.width((weeks*13).dp).height(91.dp).semantics {contentDescription="$year 年热力图，共记录 ${heat.values.sum()} 次，${heat.size} 天有记录"}) {
     val step=13.dp.toPx();val cell=10.dp.toPx()
     repeat(count) {i->val d=start.plusDays(i.toLong());val n=heat[d.toEpochDay()]?:0
      drawRect(color.copy(alpha=when {n==0->0.06f;n==1->0.3f;n==2->0.55f;else->0.9f}),Offset(((i+offset)/7)*step,((i+offset)%7)*step),Size(cell,cell))
     }
    }
   }
   Text("浅 → 深：0 / 1 / 2 / 3+ 次；颜色只表示数量。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(heat.isEmpty())Text("本年度暂无档案，空白也很完整。")
  }
}
@Composable internal fun ChartCard(title:String,values:List<Float>,caption:String,unit:String,line:Boolean=false) {
 Tile {
  Section(title)
  Text(caption,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
  if(values.isEmpty() || values.all {it==0f})Text("暂无有效图表数据。",Modifier.padding(vertical=24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
  else {
   val color=MaterialTheme.colorScheme.onSurface
   var shown by remember(values) {mutableStateOf(false)}
   LaunchedEffect(values) {shown=true}
   val progress by animateFloatAsState(if(shown)1f else 0f,label="chart")
   val max=(values.maxOrNull()?:1f).coerceAtLeast(1f)
   Text("最高 ${max.toInt()} $unit · 合计 ${values.sumOf {it.toLong()}} $unit",style=MaterialTheme.typography.labelMedium)
   Canvas(Modifier.fillMaxWidth().height(120.dp).semantics {contentDescription="$title：${values.joinToString {it.toInt().toString()}} $unit"}) {
    val step=size.width/values.size
    values.forEachIndexed {i,v->
     val y=size.height*(1-v/max*progress)
     if(line) {
      val point=Offset(step*(i+0.5f),y)
      drawCircle(color,3.dp.toPx(),point)
      if(i>0)drawLine(color,Offset(step*(i-0.5f),size.height*(1-values[i-1]/max*progress)),point,2.dp.toPx())
     }else drawRect(color.copy(alpha=0.8f),Offset(step*i+step*0.15f,y),Size(step*0.7f,size.height-y))
    }
   }
  }
 }
}
