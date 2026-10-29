@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lulema.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lulema.app.data.Entry
import com.lulema.app.domain.DurationEvaluator
import java.math.BigDecimal
import java.time.LocalDate

internal fun formatVolume(value:Double) = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable internal fun EntryDetails(entry:Entry,dismiss:()->Unit,edit:()->Unit,delete:()->Unit) {
 ModalBottomSheet(onDismissRequest=dismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Section("档案详情")
   Text(LocalDate.ofEpochDay(entry.day).toString())
   Text(entry.minute?.let {"%02d:%02d".format(it/60,it%60)}?:"未填写时间")
   Text(if(entry.duration>0)"持续时长 ${entry.duration} 分钟" else "未填写时长")
   Tile {Text("时长点评",style=MaterialTheme.typography.labelLarge);Text(DurationEvaluator.evaluate(entry.duration).text);Text("娱乐性文案，不代表医学标准、健康评分或个人能力。",style=MaterialTheme.typography.bodySmall)}
   Text("手部运动次数 · ${entry.handCount?.let {"$it 次"}?:"未填写"}",modifier=Modifier.testTag("detail-hands"))
   Text("出货量 · ${entry.volumeMl?.let {"${formatVolume(it)} mL"}?:"未填写"}",modifier=Modifier.testTag("detail-volume"))
   Text("地点 · ${entry.location?:"未填写"}",modifier=Modifier.testTag("detail-location"))
   Text("心情 · ${entry.mood.ifEmpty {"未填写"}}")
   Text("备注 · ${entry.note.ifEmpty {"未填写"}}")
   Button(onClick=edit,modifier=Modifier.fillMaxWidth().testTag("edit-entry")) {Text("编辑档案")}
   TextButton(onClick=delete) {Text("删除这条档案",color=MaterialTheme.colorScheme.error)}
   OutlinedButton(onClick=dismiss,modifier=Modifier.fillMaxWidth().testTag("close-details")) {Text("返回记录")}
  }
 }
}
