package my.edu.tarc.zeroxpire.recipe.viewModel

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.recipe.Recipe
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException

class RecipeDetailsViewModel {

    fun getRecipeById(
        userId: String,
        recipeID: String,
        view: View,
        callback: (Recipe) -> Unit
    ) {
        val recipe = Recipe()
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.recipeGetRecipeByIDURL))
            .append("?userId=")
            .append(userId)
            .append("&recipeID=")
            .append(recipeID)
            .toString()
        Log.d("RecipeDetails", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("JSON", response.toString())
                    val jsonRecipe = response.getJSONObject("records")
                    val ingredientArr = jsonRecipe.getJSONArray("ingredientName")

                    recipe.recipeID = jsonRecipe.getInt("recipeID")
                    recipe.title = jsonRecipe.getString("title")
                    recipe.instructionsLink = jsonRecipe.getString("instructionsLink")
                    recipe.imageLink = jsonRecipe.getString("imageLink")
                    recipe.note = jsonRecipe.getString("note")
                    recipe.authorID = jsonRecipe.getString("author")
                    recipe.authorName = jsonRecipe.getString("userName")
                    recipe.isBookmarked = jsonRecipe.getBoolean("isBookmarked")
                    for (i in 0 until ingredientArr.length()) {
                        recipe.ingredientNamesArrayList.add(ingredientArr[i].toString())
                    }

                    callback(recipe)
                }
            } catch (e: UnknownHostException) {
                Log.d("RecipeDetails", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("RecipeDetails", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("FK", "Error Response: ${error.message}")
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }


    fun editRecipe(recipe: Recipe,
                   ingredientIDArrayList: ArrayList<Int>,
                   view: View,
                   callback: (Boolean) -> Unit)
    {
        //create string including the arguments
        //to create ingredients array
        val ingredients = StringBuilder()
        ingredientIDArrayList.forEach {
            ingredients.append("&ingredientIDArr[]=$it")
        }

        val instructionsLink = cleanedAccessToken(recipe.instructionsLink, recipe.title, "txt")

        val imageLink = cleanedAccessToken(recipe.imageLink, recipe.title, "jpg")

        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.recipeUpdateRecipeURL))
            .append("?recipeID=")
            .append(recipe.recipeID)
            .append("&title=")
            .append(recipe.title)
            .append("&instructionsLink=")
            .append(instructionsLink)
            .append("&imageLink=")
            .append(imageLink)
            .append("&note=")
            .append(recipe.note)
            .append(ingredients)
            .toString()
        Log.d("recipe update", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    if (response.getBoolean("success")) {
                        Toast.makeText(view.context, "Successfully edited recipe", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(view.context, "Failed to edit recipe", Toast.LENGTH_SHORT).show()
                    }
                }
                callback(true)
            } catch (e: UnknownHostException) {
                Log.d("recipe update", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("recipe update", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("recipe update", "Error Response: ${error.message}")
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }

    fun deleteRecipe(
        recipeID: String,
        isDeleted: Int,
        view: View,
        callback: (Boolean) -> Unit)
    {
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.recipeDeleteRecipeURL))
            .append("?recipeID=")
            .append(recipeID)
            .append("&isDeleted=")
            .append(isDeleted)
            .toString()
        Log.d("recipe delete", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("recipe delete", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("recipe delete", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("recipe delete", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("recipe delete", "Error Response: ${error.message}")
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }

    private fun cleanedAccessToken(accessToken: String, title: String, fileType: String): String {
        val tokenLength = 36
        return java.lang.StringBuilder()
            .append(title)
            .append(".$fileType?alt=media%26token=")
            .append(accessToken.substring(accessToken.length-tokenLength, accessToken.length))
            .toString()
    }

    fun convertXmlToPdf(view: View, displayMetrics: DisplayMetrics, fileName: String) {



        view.measure(
            View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.EXACTLY)
        )
        Log.d("pdf", "Width Now " + view.measuredWidth)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        // Create a new PdfDocument instance
        val document = PdfDocument()

        // Obtain the width and height of the view
        val viewWidth = view.measuredWidth
        val viewHeight = view.measuredHeight
        // Create a PageInfo object specifying the page attributes
        val pageInfo = PdfDocument.PageInfo.Builder(viewWidth, viewHeight, 1).create()

        // Start a new page
        val page = document.startPage(pageInfo)

        // Get the Canvas object to draw on the page
        val canvas = page.canvas

        // Create a Paint object for styling the view
        val paint = Paint()
        paint.color = Color.WHITE

        // Draw the view on the canvas
        view.draw(canvas)

        // Finish the page
        document.finishPage(page)

        // Specify the path and filename of the output PDF file
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(downloadsDir, fileName)
        try {
            // Save the document to a file
            val fos = FileOutputStream(filePath)
            document.writeTo(fos)
            document.close()
            fos.close()
            // PDF conversion successful
            Toast.makeText(view.context, "Download successful", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            // Error occurred while converting to PDF
        }
    }



}