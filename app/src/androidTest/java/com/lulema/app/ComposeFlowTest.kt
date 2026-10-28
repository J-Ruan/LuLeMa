package com.lulema.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeFlowTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @Test fun addRecordAndNavigateCalendarStatsSettings() {
  runBlocking {(rule.activity.application as LuLeMaApplication).repository.clear()}
  rule.waitUntil(15000) {rule.onAllNodesWithText("+ 记一笔").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText("+ 记一笔").performClick()
  rule.onNodeWithText("持续时长 / 分钟（可选）").performTextInput("15")
  rule.onNodeWithText("个人备注（可选，1000 字内）").performTextInput("人工构造的 UI 测试档案")
  rule.activityRule.scenario.recreate()
  rule.onNodeWithText("人工构造的 UI 测试档案").assertExists()
  rule.onNodeWithText("15").assertExists()
  rule.onNodeWithText("保存档案").performScrollTo().performClick()
  rule.waitUntil(10000) {rule.onAllNodesWithText("今日档案已入库").fetchSemanticsNodes().isNotEmpty()}
  screenshot("home")
  rule.onNodeWithText("日历",useUnmergedTree=true).performClick()
  rule.onNodeWithText("返回今天").assertExists()
  screenshot("calendar")
  rule.onNodeWithText("统计",useUnmergedTree=true).performClick()
  rule.onNodeWithText("历史档案").assertExists()
  screenshot("stats")
  rule.onNodeWithText("我的",useUnmergedTree=true).performClick()
  rule.onNodeWithText("本地昵称").assertExists()
  screenshot("settings")
 }
 private fun screenshot(name:String) {
  rule.waitForIdle()
  rule.waitUntil(15000) {
   rule.onAllNodes(hasText("已解锁",substring=true)).fetchSemanticsNodes().isEmpty() &&
    rule.onAllNodesWithText("档案已保存，宇宙依然正常运转").fetchSemanticsNodes().isEmpty()
  }
  val inst=InstrumentationRegistry.getInstrumentation()
  val directory=File(inst.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
  val image=inst.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
  File(directory,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
  image.recycle()
 }
}
