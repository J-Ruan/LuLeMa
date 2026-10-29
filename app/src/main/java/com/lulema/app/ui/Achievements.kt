package com.lulema.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.lulema.app.data.Preferences
import com.lulema.app.domain.Achievement
import com.lulema.app.domain.Achievements
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

private fun medalIcon(key:String):ImageVector = when(key) {
 "first" -> Icons.Outlined.FolderOpen
 "duration" -> Icons.Outlined.Timer
 "history" -> Icons.Outlined.ManageHistory
 "stats" -> Icons.Outlined.QueryStats
 "backup" -> Icons.Outlined.Backup
 "dark" -> Icons.Outlined.DarkMode
 "secret" -> Icons.Outlined.Visibility
 "settings" -> Icons.Outlined.Tune
 "year" -> Icons.Outlined.Public
 "daily_2" -> Icons.Outlined.LooksTwo
 "daily_3" -> Icons.Outlined.Looks3
 "daily_4" -> Icons.Outlined.Looks4
 "daily_5" -> Icons.Outlined.Looks5
 "daily_over_5" -> Icons.Outlined.Diamond
 "streak_7" -> Icons.Outlined.DateRange
 "duration_under_3" -> Icons.Outlined.Bolt
 else -> Icons.Outlined.HourglassBottom
}

@Composable internal fun AchievementsPage(preferences:Preferences) {
 Page("成就中心","每份档案都有自己的位置。勋章只是纪念，无需刻意收集。") {
  Tile {
   Metric("勋章墙","${Achievements.count {it.key in preferences.achievements}} / ${Achievements.size} 枚勋章")
   Text("解锁后会保留；次数和时长不代表健康或个人能力。",style=MaterialTheme.typography.bodySmall)
  }
  val fontScale=LocalDensity.current.fontScale
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val columns=if(maxWidth<300.dp || fontScale>=1.3f)1 else 2
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Achievements.chunked(columns).forEach { row ->
     Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) {
      row.forEach { achievement -> MedalCard(achievement,preferences.achievements[achievement.key],Modifier.weight(1f)) }
      if(row.size<columns) Spacer(Modifier.weight(1f))
     }
    }
   }
  }
 }
}

@Composable private fun MedalCard(achievement:Achievement,unlockedAt:Long?,modifier:Modifier) {
 val unlocked=unlockedAt!=null
 val ink=if(unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
 val accent=if(unlocked) Color(0xFFB19A6B) else ink.copy(alpha=0.4f)
 Surface(modifier=modifier.semantics(mergeDescendants=true) {},shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Box(Modifier.size(80.dp).align(Alignment.CenterHorizontally),contentAlignment=Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
     val center=Offset(size.width/2,size.height/2)
     val radius=size.minDimension*0.42f
     val path=Path()
     val points=6+(Achievements.indexOf(achievement)%4)
     repeat(points*2) { i ->
      val angle=-Math.PI/2+i*Math.PI/points
      val r=radius*(if(i%2==0)1f else 0.86f)
      val x=center.x+cos(angle).toFloat()*r
      val y=center.y+sin(angle).toFloat()*r
      if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
     }
     path.close()
     if(unlocked)drawPath(path,accent.copy(alpha=0.14f))
     drawPath(path,accent,style=Stroke(2.dp.toPx()))
     drawCircle(accent.copy(alpha=0.65f),radius*0.7f,center,style=Stroke(1.dp.toPx()))
    }
    Icon(medalIcon(achievement.key),contentDescription=null,tint=ink.copy(alpha=if(unlocked)1f else 0.6f),modifier=Modifier.size(28.dp))
   }
   Text(achievement.name,style=MaterialTheme.typography.titleMedium,color=ink)
   Text(achievement.description,style=MaterialTheme.typography.bodySmall,color=ink)
   Text(achievement.condition,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(unlockedAt?.let {"已解锁 · ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()}"}?:"尚未解锁",style=MaterialTheme.typography.labelMedium)
  }
 }
}
