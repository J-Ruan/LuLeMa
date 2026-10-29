package com.lulema.app.domain

object EntryInput {
 fun handCount(text:String):Int? {
  if(text.isBlank())return null
  require(text.matches(Regex("[0-9]+"))) {"手部运动次数请输入非负整数"}
  return text.toIntOrNull() ?: throw IllegalArgumentException("手部运动次数超过整数上限")
 }
 fun volume(text:String):Double? {
  if(text.isBlank())return null
  require(text.matches(Regex("[0-9]+(\\.[0-9]{1,3})?"))) {"出货量请输入非负小数，最多三位小数"}
  val value=text.toDoubleOrNull()
  require(value!=null && value.isFinite() && value in 0.0..1000.0) {"出货量需为 0–1000 mL"}
  return value
 }
}
