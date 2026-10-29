@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lulema.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lulema.app.data.Entry
import com.lulema.app.data.Preferences
import com.lulema.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable internal fun ReportsPanel(entries:List<Entry>,preferences:Preferences,today:LocalDate,yearViewed:()->Unit) {
 var selected by rememberSaveable {mutableStateOf(ReportPeriod.WEEK.name)}
 val period=ReportPeriod.valueOf(selected)
 val report by produceState<PeriodReport?>(null,entries,preferences,period,today) {
  value=withContext(Dispatchers.Default) {ReportAnalyzer.analyze(entries,preferences,period,today,today)}
 }
 Section("周期报告")
 SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("report-tabs")) {
  ReportPeriod.entries.forEachIndexed { index, option ->
   SegmentedButton(selected=period==option,onClick={selected=option.name;if(option==ReportPeriod.YEAR)yearViewed()},shape=SegmentedButtonDefaults.itemShape(index,ReportPeriod.entries.size),modifier=Modifier.testTag("report-${option.name}")) {Text(option.title)}
  }
 }
 report?.takeIf {it.period==period}?.let { ReportContent(it,preferences.mondayFirst) } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
}

@Composable private fun ReportContent(report:PeriodReport,mondayFirst:Boolean) {
 val summary=report.summary
 Tile {
  Section(when(report.period) {ReportPeriod.WEEK->"本周报告";ReportPeriod.MONTH->"${report.start.year} 年 ${report.start.monthValue} 月报告";ReportPeriod.YEAR->"${report.start.year} 年度报告"})
  Text("${report.start} — ${report.end}${if(report.complete)" · 完整周期" else " · 进行中，统计截至今天"}",style=MaterialTheme.typography.bodySmall)
  if(summary.count==0)Text("暂无数据。留白也是生活的一部分。")
  FlowRow(horizontalArrangement=Arrangement.spacedBy(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Box(Modifier.testTag("report-count")) {Metric("记录总次数","${summary.count} 次")}
   Metric("累计时长","${summary.minutes} 分钟")
   Metric("平均已填时长",if(summary.measuredCount==0)"暂无数据" else String.format(Locale.CHINA,"%.1f 分钟",summary.average))
   Metric("有记录的自然日","${report.activeDays} 天")
   Metric("周期内最长连续记录","${report.longestStreak} 天")
   Metric("手部运动次数合计",summary.handCountTotal?.let {"$it 次"}?:"未填写")
   Metric("出货量合计",summary.volumeTotal?.let {"${it.stripTrailingZeros().toPlainString()} mL"}?:"未填写")
  }
  Text("平均值仅含已填写且大于 0 的时长 · 已填写 ${summary.measuredCount} 次，未填写 ${summary.missingDurationCount} 次。",style=MaterialTheme.typography.bodySmall)
  if(report.locations.isNotEmpty()) {
   Text("地点分布 · 仅本机显示",style=MaterialTheme.typography.titleSmall)
   report.locations.take(5).forEach {(place,count)->Text("$place · $count 次",style=MaterialTheme.typography.bodyMedium)}
   if(report.locations.size>5)Text("另有 ${report.locations.size-5} 个地点",style=MaterialTheme.typography.bodySmall)
  } else Text("地点分布 · 未填写",style=MaterialTheme.typography.bodySmall)
  Text("本周期新解锁勋章",style=MaterialTheme.typography.titleSmall)
  Text(if(report.unlocked.isEmpty())"暂无新勋章" else report.unlocked.joinToString("、") {it.first.name},style=MaterialTheme.typography.bodyMedium)
 }
 when(report.period) {
  ReportPeriod.WEEK,ReportPeriod.MONTH -> {
   val count=ChronoUnit.DAYS.between(report.start,report.end).toInt()+1
   ChartCard(if(report.period==ReportPeriod.WEEK)"本周每日记录" else "本月每日记录",Analysis.daily(report.entries,report.end,count).map {it.second.toFloat()},"${report.start} — ${report.end}","次")
  }
  ReportPeriod.YEAR -> {
   ChartCard("年度月度记录趋势",(1..12).map {Analysis.month(report.entries,YearMonth.of(report.start.year,it)).size.toFloat()},"${report.start.year} 年 · 1–12 月，未来月份尚无数据","次")
   YearHeatmap(report.start.year,report.entries,mondayFirst)
  }
 }
}
