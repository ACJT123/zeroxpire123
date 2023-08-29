package my.edu.tarc.zeroxpire

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

class LoadIngredientWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val userId = inputData.getString("userId")


        Log.d("doing work", "working")

        loadIngredient(userId)
        return Result.success()
    }

    private fun loadIngredient(userId: String?) {
        val ingredientViewModel = IngredientViewModel(applicationContext as Application)
        val url: String = applicationContext.getString(R.string.url_server) + applicationContext.getString((R.string.url_read_ingredient)) + "?userId=${userId}"
        //Log.d("uid", auth.currentUser?.uid.toString())
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        if (ingredientViewModel.ingredientList.value?.isNotEmpty()!!) {
                            ingredientViewModel.deleteAllIngredients()
                        }
                        Log.d("Size", size.toString())


                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonIngredient: JSONObject = jsonArray.getJSONObject(i)
                                val ingredientId = jsonIngredient.getInt("ingredientId")
                                val ingredientName = jsonIngredient.getString("ingredientName")
                                val expiryDateString = jsonIngredient.getString("expiryDate")
                                val expiryDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDateString)
                                val expiryDateInMillis = expiryDate?.time ?: 0L
                                val dateAddedString = jsonIngredient.getString("dateAdded")
                                val addedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateAddedString)
                                val dateAddedInMillis = addedDate?.time ?: 0L
                                val ingredientImage = jsonIngredient.getString("ingredientImage").replace("&amp;", "&")
                                Log.d("decode", ingredientImage)
                                val ingredientCategory = jsonIngredient.getString("ingredientCategory")
                                val isDelete = jsonIngredient.getInt("isDelete")
                                val goalId = jsonIngredient.optInt("goalId", 0)
                                val userId = jsonIngredient.getString("userId")
                                val ingredient: Ingredient

                                if(isDelete == 0){
                                    if (goalId == 0) {
                                        ingredient = Ingredient(
                                            ingredientId,
                                            ingredientName,
                                            Date(expiryDateInMillis),
                                            Date(dateAddedInMillis),
                                            ingredientImage,
                                            ingredientCategory,
                                            isDelete,
                                            null,
                                            userId// Set goalId to null when it is 0
                                        )
                                    } else {
                                        ingredient = Ingredient(
                                            ingredientId,
                                            ingredientName,
                                            Date(expiryDateInMillis),
                                            Date(dateAddedInMillis),
                                            ingredientImage,
                                            ingredientCategory,
                                            isDelete,
                                            goalId, // Set goalId to its value when it is not 0
                                            userId
                                        )
                                    }
                                    ingredientViewModel.addIngredient(ingredient)
                                    Log.d("Worker", "Working...")
                                }
                            }
                        }



                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")

                } catch (e: Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")

                }
            },
            { error ->
                //i think is when there is nothing to return then it will return 404
                ingredientViewModel.deleteAllIngredients()

            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(applicationContext).addToRequestQueue(jsonObjectRequest)
    }
}