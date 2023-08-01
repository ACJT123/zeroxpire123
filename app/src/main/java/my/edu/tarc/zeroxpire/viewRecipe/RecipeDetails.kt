package my.edu.tarc.zeroxpire.viewRecipe

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.ResponseCallback
import my.edu.tarc.zeroxpire.WebDB
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.net.UnknownHostException

class RecipeDetails : Fragment(), ResponseCallback {
    private lateinit var instructions : String
    private lateinit var currentView: View
    private var recipe = Recipe()

    private val ingredientsCheckBoxArrayList = ArrayList<CheckBox>()
    private val instructionsTextViewArrayList = ArrayList<TextView>()
    private val linearLayoutArrayList = ArrayList<LinearLayout>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe_details, container, false)
        val args: RecipeDetailsArgs by navArgs()
        val recipeID = args.recipeID
        getRecommend(recipeID)

        val recipeImageView = currentView.findViewById<ImageView>(R.id.recipeDescImageView)
        val recipeDetailsBackImageButton = currentView.findViewById<ImageView>(R.id.recipeDetailsBackImageButton)

        //top bar back button
        recipeDetailsBackImageButton.setOnClickListener {
            findNavController().popBackStack()
        }






        return currentView
    }

    private fun createNewLinearLayout(
        left: Int = 0,
        right: Int = 0,
        top: Int = 0,
        bottom: Int = 0
    ): LinearLayout {
        val newLinearLayout = LinearLayout(currentView.context, null, R.style.RecipeDetails)

        //apply attributes
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(left,top,right,bottom)

        newLinearLayout.layoutParams = layoutParams
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        return newLinearLayout
    }

    private fun createNewTextView(text: String, typeface: Int): TextView {
        val newTextView = TextView(currentView.context)

        //apply attributes
        newTextView.text = text
        newTextView.textSize = 24F
        newTextView.setTypeface(null, typeface)
        newTextView.isVisible = true

        return newTextView
    }

    private fun createNewCheckBox(text: String = "", typeface: Int = Typeface.NORMAL): CheckBox {
        val newCheckBox = CheckBox(currentView.context)

        //apply attributes
        newCheckBox.text = text
        newCheckBox.textSize = 24F
        newCheckBox.setTypeface(null, typeface)
        newCheckBox.isVisible = true

        return newCheckBox
    }

    override fun onResponseReceived() {
        displayIngredients()

        displayInstructions()

        //set note
        val recipeDetailsNoteTextView = currentView.findViewById<TextView>(R.id.recipeDetailsNoteTextView)
        recipeDetailsNoteTextView.text = recipe.note
    }

    private fun displayIngredients() {
        val recipeDetailsIngredientsLinearLayout = currentView.findViewById<LinearLayout>(R.id.recipeDetailsIngredientsLinearLayout)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                recipe.ingredientArrayList.forEach {
                    val newCheckBox = createNewCheckBox(it)
                    ingredientsCheckBoxArrayList.add(newCheckBox)
                    recipeDetailsIngredientsLinearLayout.addView(newCheckBox)
                }
            }
        }
    }

    private fun displayInstructions() {
        val recipeDetailsInstructionsLinearLayout = currentView.findViewById<LinearLayout>(R.id.recipeDetailsInstructionsLinearLayout)
        var numSteps = 1
        CoroutineScope(Dispatchers.IO).launch {
            val httpClient = OkHttpClient()
            val request = Request.Builder().url(recipe.instructionsLink).build()
            val response = httpClient.newCall(request).execute()
            instructions = response.body?.string().toString()
            val reader = BufferedReader(StringReader(instructions))

            withContext(Dispatchers.Main) {
                reader.forEachLine {
                    val newStepTextView = createNewTextView("Step $numSteps: ", Typeface.BOLD)
                    val newInstructionTextView = createNewTextView(it, Typeface.NORMAL)
                    instructionsTextViewArrayList.add(newInstructionTextView)

                    val newLinearLayout = createNewLinearLayout()
                    linearLayoutArrayList.add(newLinearLayout)

                    newLinearLayout.addView(newStepTextView)
                    newLinearLayout.addView(newInstructionTextView)
                    recipeDetailsInstructionsLinearLayout.addView(newLinearLayout)
                    numSteps++
                }
            }
        }
    }

    private fun getRecommend(recipeID: String) {
        val url = StringBuilder()
            .append(currentView.context.getString(R.string.url_server))
            .append(currentView.context.getString(R.string.recipeGetRecipeByIDURL))
            .append("?recipeID=")
            .append(recipeID)
            .toString()
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
                    recipe.note = jsonRecipe.getString("note")
                    recipe.author = jsonRecipe.getString("author")
                    for (i in 0 until ingredientArr.length()) {
                        recipe.ingredientArrayList.add(ingredientArr.getJSONObject(i).getString("ingredientName"))
                    }

                    onResponseReceived()
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
            com.android.volley.Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(currentView.context).addToRequestQueue(jsonObjectRequest)
    }

}



////recipe instructions
//currentRecipe?.child("instructions")?.addListenerForSingleValueEvent(
//object : ValueEventListener {
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    override fun onDataChange(dataSnapshot: DataSnapshot) {
//        val link = dataSnapshot.getValue(String::class.java)
//
//
//    }
//    override fun onCancelled(databaseError: DatabaseError) {
//        recipeTextView.text = getString(R.string.recipeDetailsErrorOccurred)
//    }
//}
//)
//

//recipe image
//        currentRecipe?.child("image")?.addListenerForSingleValueEvent(
//            object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    val link = dataSnapshot.getValue(String::class.java)
//                    if (link != null) {
//                        Picasso.get().load(link).into(recipeImageView)
//                    }else {
//                        recipeImageView.setImageResource(R.drawable.baseline_image_24)
//                    }
//                }
//                override fun onCancelled(databaseError: DatabaseError) {
//                    recipeImageView.setImageResource(R.drawable.baseline_broken_image_24)
//                }
//            }
//        )
