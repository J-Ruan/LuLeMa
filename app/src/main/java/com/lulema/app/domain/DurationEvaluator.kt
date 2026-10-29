package com.lulema.app.domain

object DurationEvaluator {
 enum class Band { UNSPECIFIED, FLASH, BRIEF, REGULAR, LONG, EXTENDED }
 data class Evaluation(val band: Band, val text: String)

 fun evaluate(minutes: Int?): Evaluation = when {
  minutes == null || minutes <= 0 -> Evaluation(Band.UNSPECIFIED, "未填写时长，研究所拒绝猜测。")
  minutes < 3 -> Evaluation(Band.FLASH, "极速档案已入库，统计工作完成。")
  minutes <= 9 -> Evaluation(Band.BRIEF, "简短档案收好，研究所保持中立。")
  minutes <= 30 -> Evaluation(Band.REGULAR, "记录已保存，档案柜又多了一个坐标。")
  minutes <= 60 -> Evaluation(Band.LONG, "这份档案篇幅较长，已认真归档。")
  else -> Evaluation(Band.EXTENDED, "长篇档案入库完毕，给自己留点休息时间。")
 }
}
