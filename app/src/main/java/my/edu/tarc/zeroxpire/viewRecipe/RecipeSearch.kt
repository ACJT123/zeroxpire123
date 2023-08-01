package my.edu.tarc.zeroxpire.viewRecipe

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.database.*
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import kotlin.collections.ArrayList

class RecipeSearch : Fragment() {
    //declaration
    private var firebaseDatabase: FirebaseDatabase? = FirebaseDatabase.getInstance("https://zeroxpire-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private var recipesDatabaseReference: DatabaseReference? = firebaseDatabase!!.getReference("Recipes")

    private lateinit var spinnerArrayAdapter: ArrayAdapter<String>
    private lateinit var includeIngredientSpinner : Spinner
    private lateinit var backImageView: ImageView
    private lateinit var searchBarSearchView: SearchView

    private lateinit var ingredientNameArrayList: ArrayList<String>

    private val recipeTitleArrayList = ArrayList<Recipe>()

    private lateinit var currentView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_recipe_search, container, false)

        //instantiation
        searchRecipe("fried")
        val spinnerAdapter = loadIngredient()
        includeIngredientSpinner = currentView.findViewById(R.id.includeIngredientSpinner)
        includeIngredientSpinner.adapter = spinnerAdapter
        backImageView = currentView.findViewById(R.id.backImageView)
        searchBarSearchView = currentView.findViewById(R.id.searchBarSearchView)

        backImageView.setOnClickListener {
            findNavController().popBackStack()
        }
//        searchBarSearchView.setOnQueryTextListener()

        return currentView
    }

    private fun loadIngredient(): ArrayAdapter<String> {
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_ingredient)
        val ingredientNameArrayList = ArrayList<String>()
        ingredientNameArrayList.add("Filter by ingredient")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        for (i in 0 until size) {
                            val jsonIngredient: JSONObject = jsonArray.getJSONObject(i)
                            Log.d("JSON", jsonIngredient.toString())
                            val ingredientName = jsonIngredient.getString("ingredientName")
                            ingredientNameArrayList.add(ingredientName)
                        }
                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                } catch (e: Exception) {
                    Log.d("ContactRepository", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("FK", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
        return ArrayAdapter(currentView.context, android.R.layout.simple_spinner_item, ingredientNameArrayList)
    }

    private fun searchRecipe(keyword: String) {
        val url: String = getString(R.string.url_server) + getString(R.string.recipeSearchURL) + "?keyword=" + keyword

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        for (i in 0 until size) {
                            val jsonRecipe: JSONObject = jsonArray.getJSONObject(i)
                            Log.d("JSON", jsonRecipe.toString())
                            val recipeID = jsonRecipe.getString("recipeID").toInt()
                            val title = jsonRecipe.getString("title")
                            val instructionsLink = jsonRecipe.getString("instructionsLink")
                            val note = jsonRecipe.getString("note")
                            val author = jsonRecipe.getString("author")
                            val recipe = Recipe(recipeID, title, instructionsLink, note, author)
                            recipeTitleArrayList.add(recipe)
                        }
                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                }
                catch (e: Exception) {
                    Log.d("ContactRepository", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("FK", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }

}