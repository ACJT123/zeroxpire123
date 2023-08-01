package my.edu.tarc.zeroxpire.view.goal

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.databinding.FragmentGoalDetailBinding
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import androidx.lifecycle.Observer
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import java.util.*

class GoalDetailFragment : Fragment(), IngredientClickListener {

    private lateinit var binding: FragmentGoalDetailBinding

    private var originalName: String? = null

    private var selectedDate: Long? = null // Variable to store the selected date as a Long value

    private var progressDialog: ProgressDialog? = null

    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =  FragmentGoalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = IngredientAdapter(this, goalViewModel)

        loadIngredient(adapter)

        // Set fragment result listeners to receive data from other fragments
        setFragmentResultListener("requestName") { _, bundle ->
            val result = bundle.getString("name")
            originalName = result
            binding.enterIngredientName.setText(originalName)
        }

        setFragmentResultListener("requestDate") { _, bundle ->
            val dateString = bundle.getString("date")
            selectedDate = parseDateStringToLong(dateString)
            binding.chooseExpiryDate.setText(dateString)
        }

        // Button click listeners
        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ingredientsInGoal.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsInGoal.adapter = adapter

        ingredientViewModel.ingredientList.observe(viewLifecycleOwner, Observer { ingredients ->
            adapter.setIngredient(ingredients)
        })
    }

    private fun loadIngredient(adapter: IngredientAdapter) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Loading...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val url: String = getString(R.string.url_server) + getString(R.string.url_getGoalIngredients_ingredient)
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
                                val ingredientImage = jsonIngredient.getString("ingredientImage")

                                // Convert the Base64 image string to a ByteArray
                                val imageByteArray = convertImageStringToByteArray(ingredientImage)
                                val isDelete = jsonIngredient.getInt("isDelete")

                                val goalId = jsonIngredient.optInt("goalId", 0)
                                val ingredient: Ingredient

                                if (goalId == 0) {
                                    ingredient = Ingredient(
                                        ingredientId,
                                        ingredientName,
                                        Date(expiryDateInMillis),
                                        Date(dateAddedInMillis),
                                        ingredientImage,
                                        isDelete,
                                        null // Set goalId to null when it is 0
                                    )
                                } else {
                                    ingredient = Ingredient(
                                        ingredientId,
                                        ingredientName,
                                        Date(expiryDateInMillis),
                                        Date(dateAddedInMillis),
                                        ingredientImage,
                                        isDelete,
                                        goalId // Set goalId to its value when it is not 0
                                    )
                                }

                                ingredientViewModel.addIngredient(ingredient)
                            }
                        }


                        // Dismiss the progress dialog when finished loading ingredients
                        progressDialog?.dismiss()

                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                    progressDialog?.dismiss()
                } catch (e: Exception) {
                    Log.d("ContactRepository", "Response: ${e.message}")
                    progressDialog?.dismiss()
                }
            },
            { error ->
                ingredientViewModel.deleteAllIngredients()
                progressDialog?.dismiss()
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }

    private fun convertImageStringToByteArray(imageString: String): ByteArray {
        return try {
            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val outputStream = ByteArrayOutputStream()
            decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.d("ContactRepository", "Error converting image string to byte array: ${e.message}")
            ByteArray(0)
        }
    }
    private fun parseDateStringToLong(dateString: String?): Long? {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = format.parse(dateString)
            date?.time
        } catch (e: Exception) {
            null
        }
    }

    override fun onIngredientClick(ingredient: Ingredient) {

    }

}