package com.lulema.app.notification

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.lulema.app.*
import com.lulema.app.R
import com.lulema.app.data.Preferences
import java.time.ZonedDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit

class ReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result {
  val p=(applicationContext as LuLeMaApplication).repository.preferences()
  if(!p.reminders) return Result.success()
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return Result.success()
  val manager=applicationContext.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("archive","档案馆提醒",NotificationManager.IMPORTANCE_DEFAULT).apply { lockscreenVisibility=Notification.VISIBILITY_PRIVATE })
  val intent=PendingIntent.getActivity(applicationContext,0,Intent(applicationContext,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  manager.notify(7,NotificationCompat.Builder(applicationContext,"archive")
   .setSmallIcon(R.drawable.ic_notification).setContentTitle("档案馆提醒")
   .setContentText("重要数据记得备份。按自己的节奏生活。")
   .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
   .setPublicVersion(NotificationCompat.Builder(applicationContext,"archive").setSmallIcon(R.drawable.ic_notification).setContentTitle("档案馆提醒").setContentText("你有一条本地提醒").build())
   .setContentIntent(intent).setAutoCancel(true).build())
  return Result.success()
 }
}
object ReminderScheduler {
 fun update(context:Context,p:Preferences,replace:Boolean=true) {
  val work=WorkManager.getInstance(context)
  if(!p.reminders) { work.cancelUniqueWork("archive-reminder");context.getSystemService(NotificationManager::class.java).cancel(7);return }
  val now=ZonedDateTime.now()
  var target=now.withHour(p.reminderHour).withMinute(p.reminderMinute).withSecond(0).withNano(0)
  if(!target.isAfter(now)) target=target.plusDays(1)
  val request=PeriodicWorkRequestBuilder<ReminderWorker>(p.reminderDays.toLong(),TimeUnit.DAYS)
   .setInitialDelay(Duration.between(now,target).seconds,TimeUnit.SECONDS).build()
  work.enqueueUniquePeriodicWork("archive-reminder",if(replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,request)
 }
}
