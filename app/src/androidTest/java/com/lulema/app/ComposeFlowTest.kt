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
import org.junit.Assert.*
import com.lulema.app.data.Entry

@RunWith(AndroidJUnit4::class)
class ComposeFlowTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @Test fun addRecordAndNavigateCalendarStatsSettings() {
  runBlocking {(rule.activity.application as LuLeMaApplication).repository.clear()}
  checkpoint("cleared")
  rule.waitUntil(20000) {rule.onAllNodesWithText("+ 记一次").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText("+ 记一次").performClick()
  checkpoint("form-opened")
  rule.onNodeWithText("持续时长 / 分钟（可选）").performScrollTo().performTextInput("15")
  checkpoint("duration-entered")
  rule.onNodeWithText("个人备注（可选，1000 字内）").performScrollTo().performTextInput("人工构造的 UI 测试档案")
  checkpoint("note-entered")
  rule.onNodeWithTag("more-fields").performScrollTo().performClick()
  rule.onNodeWithTag("hand-count-input").performScrollTo().performTextInput("123")
  rule.onNodeWithTag("volume-input").performScrollTo().performTextInput("1.234")
  rule.onNodeWithTag("location-input").performScrollTo().performTextInput("虚构 UI 地点")
  checkpoint("new-fields-entered")
  rule.activityRule.scenario.recreate()
  rule.onNodeWithText("人工构造的 UI 测试档案").performScrollTo().assertExists()
  rule.onNodeWithText("15").assertExists()
  rule.onNodeWithText("123").assertExists();rule.onNodeWithText("1.234").assertExists();rule.onNodeWithText("虚构 UI 地点").assertExists()
  checkpoint("form-restored")
  rule.onNodeWithText("保存档案").performScrollTo().performClick()
  rule.waitUntil(20000) {rule.onAllNodesWithText("档案详情").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithTag("detail-hands").assertTextEquals("手部运动次数 · 123 次")
  rule.onNodeWithTag("detail-volume").assertTextEquals("出货量 · 1.234 mL")
  rule.onNodeWithTag("detail-location").assertTextEquals("地点 · 虚构 UI 地点")
  rule.onNodeWithText("记录已保存，档案柜又多了一个坐标。").assertExists()
  checkpoint("new-detail-verified")
  rule.onNodeWithTag("edit-entry").performScrollTo().performClick()
  rule.onNodeWithText("持续时长 / 分钟（可选）").performScrollTo().performTextReplacement("61")
  rule.onNodeWithText("保存档案").performScrollTo().performClick()
  rule.waitUntil(20000) {rule.onAllNodesWithText("长篇档案入库完毕，给自己留点休息时间。").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithTag("detail-hands").assertTextEquals("手部运动次数 · 123 次")
  rule.onNodeWithTag("close-details").performScrollTo().performClick()
  checkpoint("edited-detail-verified")
  rule.onNodeWithTag("today-count").assertTextEquals("今日记录 1 次")
  screenshot("home")
  checkpoint("home-captured")
  rule.onNodeWithTag("report-tabs").performScrollTo()
  rule.onNodeWithTag("report-count").assert(hasAnyDescendant(hasText("1 次")))
  rule.onNodeWithTag("report-MONTH").performClick()
  rule.onNodeWithTag("report-MONTH").assertIsSelected()
  screenshot("report-month")
  rule.onNodeWithTag("report-YEAR").performClick()
  rule.onNodeWithTag("report-YEAR").assertIsSelected()
  screenshot("report-year")
  checkpoint("reports-verified")
  val repo=(rule.activity.application as LuLeMaApplication).repository
  runBlocking {repo.save(Entry())}
  rule.waitUntil(20000) {rule.onAllNodes(hasTestTag("report-count") and hasAnyDescendant(hasText("2 次"))).fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithTag("nav-achievements").performClick()
  rule.onNodeWithText("二连击破").performScrollTo().assertExists()
  assertTrue(runBlocking {"daily_2" in repo.preferences().achievements})
  screenshot("achievements")
  checkpoint("achievements-verified")
  rule.onNodeWithTag("nav-home").performClick()
  rule.onNodeWithTag("report-YEAR").performScrollTo().assertIsSelected()
  rule.onNodeWithTag("nav-calendar").performClick()
  rule.onNodeWithText("返回今天").assertExists()
  screenshot("calendar")
  rule.onNodeWithTag("nav-stats").performClick()
  rule.onNodeWithText("历史档案").assertExists()
  screenshot("stats")
  rule.onNodeWithTag("nav-me").performClick()
  rule.onNodeWithText("本地昵称").assertExists()
  screenshot("settings")
  rule.onNodeWithText("深色").performClick()
  rule.waitUntil(15000) {rule.onAllNodes(hasText("深色") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
  rule.activityRule.scenario.recreate()
  rule.waitUntil(15000) {rule.onAllNodes(hasText("深色") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText("深色").assertIsSelected()
  screenshot("settings-dark")
  checkpoint("navigation-and-dark-verified")
  rule.onNodeWithTag("nav-home").performClick()
  rule.onNodeWithText("最近一次记录").performScrollTo()
  // The newest synthetic entry is first; deleting through its real detail UI updates the report.
  rule.onNodeWithContentDescription("查看或编辑档案").performScrollTo().performClick()
  rule.onNodeWithText("删除这条档案").performScrollTo().performClick()
  rule.onNodeWithText("确认").performClick()
  rule.waitUntil(20000) {runBlocking {repo.dao.all().size==1}}
  rule.onNodeWithTag("today-count").performScrollTo().assertTextEquals("今日记录 1 次")
  rule.onNodeWithTag("report-tabs").performScrollTo()
  rule.waitUntil(20000) {rule.onAllNodes(hasTestTag("report-count") and hasAnyDescendant(hasText("1 次"))).fetchSemanticsNodes().isNotEmpty()}
  checkpoint("delete-and-report-refresh-verified")
 }
 private fun checkpoint(stage:String) {android.util.Log.i("LuLeMaUiTest",stage)}
 private fun screenshot(name:String) {
  rule.waitForIdle()
  rule.waitUntil(15000) {
   rule.onAllNodesWithTag("achievement-banner").fetchSemanticsNodes().isEmpty() &&
    rule.onAllNodesWithText("档案已保存，宇宙依然正常运转").fetchSemanticsNodes().isEmpty()
  }
  val inst=InstrumentationRegistry.getInstrumentation()
  val directory=File(inst.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
  val image=inst.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
  File(directory,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
  image.recycle()
 }
}
