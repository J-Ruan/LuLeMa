package com.lulema.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeFlowTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @Test fun addRecordAndNavigateCalendarStatsSettings() {
  runBlocking {(rule.activity.application as LuLeMaApplication).repository.clear()}
  rule.onNodeWithText("+ 记一笔").performClick()
  rule.onNodeWithText("持续时长 / 分钟（可选）").performTextInput("15")
  rule.onNodeWithText("个人备注（可选，1000 字内）").performTextInput("人工构造的 UI 测试档案")
  rule.onNodeWithText("保存档案").performScrollTo().performClick()
  rule.waitUntil(10000) {rule.onAllNodesWithText("今日档案已入库").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText("日历",useUnmergedTree=true).performClick()
  rule.onNodeWithText("返回今天").assertExists()
  rule.onNodeWithText("统计",useUnmergedTree=true).performClick()
  rule.onNodeWithText("历史档案").assertExists()
  rule.onNodeWithText("我的",useUnmergedTree=true).performClick()
  rule.onNodeWithText("本地昵称").assertExists()
 }
}
