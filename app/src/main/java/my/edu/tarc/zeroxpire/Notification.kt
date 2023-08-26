package my.edu.tarc.zeroxpire

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat


const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val count = intent.getIntExtra("countExtra", 0) // Get the count extra
        val ingredientNames = intent.getStringArrayListExtra("ingredientNames") // Get the list of ingredient names

        val notification: Notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.final_logo)
            .setContentTitle("$count ingredients are going to expire")
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(ingredientNames?.joinToString("\n") ?: "")) // Set ingredient names as expanded text
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)
    }
}
