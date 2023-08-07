package my.edu.tarc.zeroxpire.view.ingredient

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentAddIngredientBinding
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AddIngredientFragment : Fragment() {
    private lateinit var binding: FragmentAddIngredientBinding
    private var selectedDate: Date? = null // Variable to store the selected date

    private val ingredientViewModel: IngredientViewModel by activityViewModels()

    private val categoryList = ArrayList<String>()

    private lateinit var auth: FirebaseAuth
    // Data
    private var id: Int = 0

    private var fileUri: Uri? = null

    private var progressDialog: ProgressDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddIngredientBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.enterIngredientName.doAfterTextChanged {
            // Clear the error when the user starts typing
            binding.enterIngredientNameLayout.error = null
        }

        if(arguments?.isEmpty == false){
            val recognizedName = arguments?.getString("recognizedIngredientName").toString()
            binding.enterIngredientName.setText(recognizedName)

            val recognizedDate = arguments?.getString("recognizedExpiryDate").toString()
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = dateFormatter.parse(recognizedDate)

            val outputDateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val formattedDate = parsedDate?.let { outputDateFormat.format(it) }
            selectedDate = formattedDate?.let { outputDateFormat.parse(it) }
            binding.chooseExpiryDate.setText(formattedDate)
        }







        //image
        binding.ingredientImage.setPadding(0, 0, 0, 0)
        binding.ingredientImage.scaleType = ImageView.ScaleType.CENTER_CROP
        fileUri = arguments?.getParcelable<Uri>("ingredientImage")
        Log.d("ingredientImage!!", fileUri.toString())
        Glide.with(requireContext())
            .load(fileUri)
            .centerCrop()
            .into(binding.ingredientImage)

//        setFragmentResultListener("requestName") { _, bundle ->
//            val result = bundle.getString("name")
//            binding.enterIngredientName.setText("$result")
//        }
//
//        setFragmentResultListener("requestCategory") { _, bundle ->
//            val result = bundle.getString("category")
//            binding.chooseCategory.setText("$result")
//        }
//
//        setFragmentResultListener("requestImage") { _, bundle ->
//            val result = bundle.getString("image")
//            Glide.with(requireContext())
//                .load(result)
//                .centerCrop()
//                .into(binding.ingredientImage)
//        }

        binding.chooseExpiryDate.setOnClickListener {
            showDatePickerDialog()
        }

        showCategory()

        binding.addBtn.setOnClickListener {
            //TODO: it is not working for those images that taken using camera
            storeIngredient()
        }

        binding.upBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addIngredientFragment_to_ingredientFragment)
        }

        binding.ingredientImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
    }

    private fun showCategory(){
        val ingredientCategories = resources.getStringArray(R.array.ingredient_categories)
        val adapter = ArrayAdapter(requireContext(), R.layout.category_list, ingredientCategories)
        binding.chooseCategory.setAdapter(adapter)

        // Set an item click listener to handle the selected category
        binding.chooseCategory.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedCategory = ingredientCategories[position]
            Toast.makeText(requireContext(), "Selected category: $selectedCategory", Toast.LENGTH_SHORT).show()
        }

        // Programmatically open the dropdown list when the user clicks the AutoCompleteTextView
        binding.chooseCategory.setOnClickListener {
            binding.chooseCategory.showDropDown()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            fileUri = data?.data
            try {
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, fileUri)
                binding.ingredientImage.setImageBitmap(bitmap)
                binding.ingredientImage.setPadding(0, 0, 0, 0)
                binding.ingredientImage.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun showDatePickerDialog() {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedDate = calendar.time
                val selectedDateString =
                    SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(selectedDate)
                binding.chooseExpiryDate.setText(selectedDateString)
            },
            year,
            month,
            dayOfMonth
        )

        datePickerDialog.show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun storeIngredient() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Adding...")
//        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val ingredientName = binding.enterIngredientName.text.toString()
        val ingredientCategory = binding.chooseCategory.text.toString()
        Log.d("ingredientCategory", ingredientCategory)

        if (ingredientName.isNotEmpty() && selectedDate != null) {
            // Get the current date
            val currentDate = Date()

            val encodedIngredientName = URLEncoder.encode(ingredientName, "UTF-8")

            val storage = Firebase.storage("gs://zeroxpire.appspot.com")
            val imageRef = storage.reference.child("ingredientImage/${encodedIngredientName}.jpg")


            fileUri?.let { uri ->
                imageRef.putFile(uri)
                    .addOnSuccessListener { _ ->
                        // Image upload successful, continue storing other ingredient data
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Get the download URL of the uploaded image
                            val imageUrl = downloadUri.toString()
                            Log.d("imageUrl", imageUrl)

                            // Create the newIngredient object with the image URL
                            val newIngredient = Ingredient(
                                id,
                                ingredientName,
                                selectedDate!!,
                                currentDate,
                                imageUrl,
                                ingredientCategory,
                                0,
                                null,
                                auth.currentUser?.uid!!
                            )

                            if (isNetworkAvailable()) {
                                // Store ingredient remotely
                                addIngredient(newIngredient)
                            } else {
                                ingredientViewModel.addIngredient(newIngredient)
                            }

                        }
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors that occurred during image upload
                        Log.e("AddIngredientFragment", "Image upload failed: ${exception.message}")
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT)
                            .show()
                    }
            } ?: run {
                // Handle the case where fileUri is null (no image selected)
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            // Handle the case where ingredientName or selectedDate is empty
            if (ingredientName.isEmpty()) {
                binding.enterIngredientNameLayout.error = "Please enter the ingredient's name"
                binding.enterIngredientName.requestFocus()
            }
            if (selectedDate == null) {
                binding.chooseExpiryDateLayout.error = "Please select the expiry date"
                binding.chooseExpiryDate.requestFocus()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun addIngredient(ingredient: Ingredient) {
        val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(ingredient.expiryDate)
        val formattedIngredientCategory = if (ingredient.ingredientCategory.contains("&")){
            ingredient.ingredientCategory.replace("&", "%26")
        }
        else{
            ingredient.ingredientCategory
        }
        val url = getString(R.string.url_server) + getString(R.string.url_create_ingredient) +
                "?ingredientName=" + ingredient.ingredientName +
                "&expiryDate=" + expiryDate +
                "&ingredientImage=" + URLEncoder.encode(
            ingredient.ingredientImage.toString().substringAfterLast("%2F"), "UTF-8"
        ) + "&ingredientCategory=" + formattedIngredientCategory  + "&userId=" + auth.currentUser?.uid
        Log.d("url", url)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {
                            Toast.makeText(
                                requireContext(),
                                "Ingredient is added successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressDialog?.dismiss()
                            findNavController().popBackStack()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.recipeDetailsErrorOccurred),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("AddIngredientFragment", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->
                Log.d("AddIngredientFragmentID", "Response : %s".format(error.message.toString()))
            }
        )
        jsonObjectRequest.retryPolicy =
            DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }
}
