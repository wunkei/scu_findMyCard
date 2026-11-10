package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object MatchNotificationHelper {

    const val CHANNEL_ID = "lost_found_match_channel"
    private const val CHANNEL_NAME = "寻卡关联匹配通知"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当他人发布的拾获卡片证件号与您的实名证件号完全匹配时，向您发送实时提醒"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 关联要求：证件号完全匹配（学号或身份证号完全一致）才判定为关联匹配，进而推送通知
     */
    fun isPostMatchingUser(post: LostFoundPost, user: UserProfile): Boolean {
        if (post.type != "拾卡") return false

        val postDoc = post.documentNumber.replace(" ", "").trim().uppercase()
        if (postDoc.isBlank()) return false

        val userStudentId = user.studentId.replace(" ", "").trim().uppercase().removeSuffix("@SCU.EDU.CN")
        val userIdCard = user.idCard.replace(" ", "").trim().uppercase()

        // 仅当证件号码完全一致（学号或身份证号）才匹配
        val matchByStudentId = userStudentId.isNotBlank() && postDoc == userStudentId
        val matchByIdCard = userIdCard.isNotBlank() && postDoc == userIdCard

        return matchByStudentId || matchByIdCard
    }

    fun sendMatchNotification(context: Context, post: LostFoundPost) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            post.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val title = "🎉 找到了属于您的卡片！"
        val content = "有人在【${post.campus}·${post.location}】登记了一张${post.title}（证件号：${post.documentNumber}），与您的证件号完全匹配！"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(post.id.hashCode(), notification)
    }
}
