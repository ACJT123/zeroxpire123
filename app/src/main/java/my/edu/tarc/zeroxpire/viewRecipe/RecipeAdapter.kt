package my.edu.tarc.zeroxpire.viewRecipe

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.ResponseCallback
import my.edu.tarc.zeroxpire.WebDB
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException


class RecipeAdapter : RecyclerView.Adapter<RecipeRecyclerViewHolder>(), ResponseCallback {
    // declaration
    private lateinit var parentContext: Context
    private var holderArrayList = ArrayList<RecipeRecyclerViewHolder>()

    //TODO: get user id
    private val userId: String = "1"
    private val recipeArrayList = ArrayList<Recipe>()
    private var notYetRecommended = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeRecyclerViewHolder {
        parentContext = parent.context
        if (notYetRecommended) {
            getRecommend()
            notYetRecommended = false
        }
        val view = LayoutInflater.from(parentContext).inflate(viewType, parent, false)
        return RecipeRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.recipe_frame
    }

    override fun onBindViewHolder(holder: RecipeRecyclerViewHolder, position: Int) {
        // declaration: views
        holderArrayList.add(holder)




    }

    private fun getRecommend() {
        val url = StringBuilder()
            .append(parentContext.getString(R.string.url_server))
            .append(parentContext.getString(R.string.recipeGetRecommendURL))
            .toString()
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    val strResponse = response.toString()
                    val jsonResponse = JSONObject(strResponse)
                    val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                    val size: Int = jsonArray.length()

                    for (i in 0 until size) {
                        val jsonRecipe: JSONObject = jsonArray.getJSONObject(i)
                        Log.d("JSON", jsonRecipe.toString())
                        val id = jsonRecipe.getInt("recipeID")
                        val title = jsonRecipe.getString("title")
                        val instructionsLink = jsonRecipe.getString("instructionsLink")
                        val note = jsonRecipe.getString("note")
                        val author = jsonRecipe.getString("author")
                        recipeArrayList.add(Recipe(id, title, instructionsLink, note, author))
                    }
                    onResponseReceived()
                }
            } catch (e: UnknownHostException) {
                Log.d("ContactRepository", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("ContactRepository", "Response: ${e.message}")
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
        WebDB.getInstance(parentContext).addToRequestQueue(jsonObjectRequest)
    }

    override fun getItemCount(): Int {
        return 50
    }

    override fun onResponseReceived() {
        for (i in 0..recipeArrayList.size) {
            val currentRecipe = recipeArrayList[i]

            val recipeDescConstraintLayout : ConstraintLayout = holderArrayList[i].getView().findViewById(R.id.recipeDescConstraintLayout)
            val titleTextView: TextView = holderArrayList[i].getView().findViewById(R.id.recipe_title_textview)
            val ingredientsTextView: TextView = holderArrayList[i].getView().findViewById(R.id.recipe_ingredients_textview)
            val recipeImageView : ImageView = holderArrayList[i].getView().findViewById(R.id.recipe_imageView)
            val shareButton : Button = holderArrayList[i].getView().findViewById(R.id.shareButton)
            val bookmarkButton : Button = holderArrayList[i].getView().findViewById(R.id.bookmarkButton)

            titleTextView.text = currentRecipe.title

            // navigation
            recipeDescConstraintLayout.setOnClickListener {
                val action = RecipeFragmentDirections.actionRecipeFragmentToRecipeDetails(currentRecipe.recipeID.toString())
                Navigation.findNavController(holderArrayList[i].getView()).navigate(action)
            }
        }
    }
}




//bookmark button
//        bookmarkButton.setOnClickListener {
//            // if already exist in database
//            if (bookmarksDatabaseReference?.equalTo(recipeID.toString()).
//                on("bookmark_exists", fun() {}))
//            {
//                bookmarksDatabaseReference?.child(recipeID.toString())?.
//                setValue(recipeID)?.addOnCompleteListener {
//                    Toast.makeText(parentContext, "Added to bookmarks", Toast.LENGTH_SHORT).show()
//                }
//            } else
//            {
//                //set
//                bookmarksDatabaseReference?.child(recipeID.toString())?.
//                setValue(recipeID)?.addOnCompleteListener {
//                    Toast.makeText(parentContext, "Added to bookmarks", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }


//title
//        currentRecipe?.child("title")?.get()?.addOnCompleteListener { recipeTitle ->
//            titleTextView.text = recipeTitle.result.value.toString()
//        }
//
//        //ingredients
//        currentRecipe?.child("ingredients")?.get()?.addOnCompleteListener {recipeIngredients ->
//            ingredientsTextView.text = recipeIngredients.result.value.toString()
//        }
//
//        //image
//        currentRecipe?.child("image")?.addListenerForSingleValueEvent(
//            object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    // getting a DataSnapshot for the location at the
//                    // specified relative path and getting in the link variable
//                    val link = dataSnapshot.getValue(String::class.java)
//                    if (link != null) {
//                        // loading that data into recipeImageView
//                        // variable which is ImageView
//                        Picasso.get().load(link).into(recipeImageView)
//                    }else {
//                        recipeImageView.setImageResource(R.drawable.baseline_image_24)
//                    }
//                }
//
//                // this will called when any problem occurs in getting data
//                override fun onCancelled(databaseError: DatabaseError) {
//                    recipeImageView.setImageResource(R.drawable.baseline_broken_image_24)
//                }
//            }
//        )