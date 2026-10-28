package com.lulema.app.domain

import com.lulema.app.data.Entry
import java.time.LocalDate
import java.time.YearMonth

object Analysis {
 data class Summary(val count: Int, val minutes: Long, val average: Double, val latest: Long?)
 fun summary(entries: List<Entry>): Summary {
  val total=entries.sumOf { it.duration.toLong() }
  return Summary(entries.size,total,if(entries.isEmpty()) 0.0 else total.toDouble()/entries.size, entries.maxOfOrNull { it.day })
 }
 fun month(entries: List<Entry>, month: YearMonth) = entries.filter { it.day in month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay() }
 fun year(entries: List<Entry>, year: Int) = entries.filter { LocalDate.ofEpochDay(it.day).year==year }
 fun daily(entries: List<Entry>, end: LocalDate, days: Int): List<Pair<LocalDate,Int>> {
  require(days in 1..366)
  val groups=entries.groupingBy { it.day }.eachCount()
  return (days-1 downTo 0).map { offset -> end.minusDays(offset.toLong()).let { it to (groups[it.toEpochDay()]?:0) } }
 }
 fun calendar(month: YearMonth, mondayFirst: Boolean): List<LocalDate?> {
  val offset=(month.atDay(1).dayOfWeek.value-(if(mondayFirst)1 else 7)+7)%7
  val cells=List(offset) { null }+(1..month.lengthOfMonth()).map { month.atDay(it) }
  return cells+List((7-cells.size%7)%7) { null }
 }
}
object Copybook {
 val subtitles=listOf("欢迎来到人类迷惑行为管理局","你的生活可以抽象，数据必须严谨","严肃统计，不严肃生活","离谱，但有据可查")
 val quotes=listOf("档案已保存，宇宙依然正常运转。","今日报告：研究所仍在摸鱼。","历史不会说谎，除非你填错了日期。","数据分析完毕，结论是没有结论。","给生活留点空白，也给自己一点耐心。")
 val moods=listOf("平静","轻松","快乐","一般","神秘")
}
data class Achievement(val key:String,val name:String,val description:String,val condition:String)
val Achievements=listOf(
 Achievement("first","档案管理员","数据库为你安排了一个工位。","创建第一条记录"),
 Achievement("duration","时间管理大师","时间有了归宿，秒针松了口气。","首次填写非零持续时间"),
 Achievement("history","日历考古学家","过去没有消失，只是翻了页。","查看历史月份"),
 Achievement("stats","数据分析师","结论是：这是一份数据。","打开统计中心"),
 Achievement("backup","备份大师","为档案安排了一艘救生艇。","成功导出备份"),
 Achievement("dark","暗夜研究员","研究所终于关灯了。","开启深色模式"),
 Achievement("secret","神秘观察员","录取通知书没有编制。","连续点击首页标题五次"),
 Achievement("settings","设置研究生","设置页面已被你研究明白。","修改三项不同设置"),
 Achievement("year","宇宙档案馆长","年度报告很严谨，宇宙暂不置评。","查看年度统计")
)
