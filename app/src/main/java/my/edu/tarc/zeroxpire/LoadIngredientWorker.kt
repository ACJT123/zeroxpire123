package my.edu.tarc.zeroxpire

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.adapters.GoalAdapter
import my.edu.tarc.zeroxpire.model.Goal
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LoadIngredientWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val userId = inputData.getString("userId")

        // Load ingredients from your data source (e.g., database, API)
        val ingredientList = loadIngredients(userId)

        // Get the current date and time
        val currentDate = Calendar.getInstance()

        // Calculate the threshold date (current date + 1 day)
        val expirationThreshold = Calendar.getInstance()
        expirationThreshold.add(Calendar.SECOND, 2)

        // Filter ingredients that are expired within the next 24 hours
        val expiringIngredients = ingredientList.filter { ingredient ->
            ingredient.expiryDate.after(currentDate.time)
        }

        // Notify user about expiring ingredients
        if (expiringIngredients.isNotEmpty()) {
            showNotification(expiringIngredients.size)
        }

        return Result.success()
    }

    private fun loadIngredients(userId: String?): List<Ingredient> {
        // Load and return the list of ingredients associated with the user
        // Replace this with your actual implementation (e.g., fetching from a database or API)
        return emptyList()
    }

    private fun showNotification(expiringCount: Int) {
        val channelId = "my_channel_id"
        val notificationId = 1

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Expiring Ingredients")
            .setContentText("$expiringCount ingredients will expire within the next 24 hours.")
            .setSmallIcon(R.drawable.final_logo) // Replace with your own icon

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
