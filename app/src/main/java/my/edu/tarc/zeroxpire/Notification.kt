// BackgroundService.kt
import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.view.ingredient.IngredientWorker.Companion.CHANNEL_ID
private const val CHANNEL_ID = "my_channel_id"
private const val NOTIFICATION_ID = 1
//class BackgroundService : Service() {
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Implement your notification logic here
//        showNotification()
//
//        // Return START_STICKY to indicate that the service should be restarted if killed by the system
//        return START_STICKY
//    }
//
//    private fun showNotification() {
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.final_logo)
//            .setContentTitle("Notification Title")
//            .setContentText("Notification Text")
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        val notificationManager = NotificationManagerCompat.from(this)
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//}
const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class Notification : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra(titleExtra))
            .setContentText(intent.getStringExtra(messageExtra))
            .build()

        val  manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)
    }

}