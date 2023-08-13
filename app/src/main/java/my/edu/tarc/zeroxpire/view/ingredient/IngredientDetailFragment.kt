package my.edu.tarc.zeroxpire.view.ingredient

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentIngredientDetailBinding
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class IngredientDetailFragment : Fragment() {
    private lateinit var binding: FragmentIngredientDetailBinding

    private var selectedDate: Long? = null // Variable to store the selected date as a Long value

    private val ingredientViewModel: IngredientViewModel by activityViewModels()

    private var ingredientId: Int = 0
    private var originalName: String? = null

    private lateinit var requestQueue: RequestQueue
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIngredientDetailBinding.inflate(inflater, container, false)
        requestQueue = Volley.newRequestQueue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        setFragmentResultListener("requestCategory") { _, bundle ->
            val result = bundle.getString("category")
            binding.chooseCategory.setText("$result")
        }


        setFragmentResultListener("requestId") { _, bundle ->
            val id = bundle.getInt("id", 0) // Use a default value if the key is not found or the value is not an integer
            ingredientId = id
        }

        setFragmentResultListener("") { _, bundle ->
            val result = bundle.getString("name")
            originalName = result
            binding.enterIngredientName.setText(originalName)
        }

        binding.ingredientImage.setPadding(0, 0, 0, 0)
        binding.ingredientImage.scaleType = ImageView.ScaleType.CENTER_CROP
        setFragmentResultListener("requestImage") { _, bundle ->
            val imageUri = bundle.getString("image")
            Glide.with(requireContext())
                .load(imageUri)
                .centerCrop()
                .into(binding.ingredientImage)
        }

        binding.enterIngredientName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val newName = s?.toString()
                val isNameChanged = newName != originalName
                val isDateChanged = selectedDate != null

                binding.saveBtn.isEnabled = isNameChanged || isDateChanged
            }
        })

        binding.chooseExpiryDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val newDate = parseDateStringToLong(s?.toString())
                val isDateChanged = newDate != selectedDate
                val isNameChanged = binding.enterIngredientName.text.toString() != originalName

                binding.saveBtn.isEnabled = isNameChanged || isDateChanged
            }
        })

        showCategory()

        // Button click listeners
        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chooseExpiryDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveBtn.setOnClickListener {
            val newName = binding.enterIngredientName.text.toString()
            val newDate = selectedDate
            val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(newDate)

            if (newName.isNotEmpty()) {
                if (newName != originalName) {
                    //ingredientViewModel.updateIngredientName(ingredientId, newName)
                    val url = getString(R.string.url_server) + getString(R.string.url_update_ingredient) +"?ingredientName=" + newName +
                            "&expiryDate=" + expiryDate + "&ingredientId=" + ingredientId
                    Log.d("ingredientID", ingredientId.toString())
                    Log.d("ingredientName", newName)
                    Log.d("expiryDate", expiryDate)
                    val jsonObjectRequest = JsonObjectRequest(
                        Request.Method.POST, url, null,
                        { response ->
                            try {
                                if(response!=null){
                                    val strResponse = response.toString()
                                    val jsonResponse = JSONObject(strResponse)
                                    val success: String = jsonResponse.get("success").toString()

                                    if (success == "1") {
                                        toast(requireContext(), "Ingredient is updated successfully.")
                                    } else {
                                        toast(requireContext(), "Failed to update.")
                                    }
                                }
                            }
                            catch (e: java.lang.Exception) {
                                Log.d("Update", "Response: %s".format(e.message.toString()))
                            }
                        },
                        { error ->
                            // Handle error response, if required
                            Log.d("Update", "Error Response: ${error.message}")
                        }
                    )
                    jsonObjectRequest.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
                    WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
                } else {
                    toast(requireContext(),"No changes made to the ingredient name.")
                }
            } else {
                binding.enterIngredientName.error = "Please enter the ingredient's name"
                binding.enterIngredientName.requestFocus()
            }

//            if (newDate != null) {
//                ingredientViewModel.updateExpiryDate(ingredientId, newDate)
//                val dateString = formatDateToStringFromLong(newDate)
//                toast("Expiry date updated successfully! New date: $dateString")
//            } else {
//                toast("No expiry date selected.")
//            }
            findNavController().navigateUp()
        }

        binding.deleteBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.ingredientImage.setOnClickListener {
            openImagePicker()
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

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = selectedCalendar.timeInMillis
                val dateString = formatDateToStringFromLong(selectedDate)
                binding.chooseExpiryDate.setText(dateString)
            },
            year,
            month,
            dayOfMonth
        )

        // Set the selected date as the default date
        if (selectedDate != null) {
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.timeInMillis = selectedDate!!

            val selectedYear = selectedCalendar.get(Calendar.YEAR)
            val selectedMonth = selectedCalendar.get(Calendar.MONTH)
            val selectedDayOfMonth = selectedCalendar.get(Calendar.DAY_OF_MONTH)

            datePickerDialog.updateDate(selectedYear, selectedMonth, selectedDayOfMonth)
        }

        datePickerDialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Are you sure you want to delete?")
            .setCancelable(false)
            .setPositiveButton("Delete") { _, _ ->
                val url = getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + ingredientId
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.POST, url, null,
                    { response ->
                        try {
                            if(response!=null){
                                val strResponse = response.toString()
                                val jsonResponse = JSONObject(strResponse)
                                val success: String = jsonResponse.get("success").toString()

                                if (success == "1") {
                                    toast(requireContext(), "Ingredient is deleted successfully.")
                                } else {
                                    toast(requireContext(), "Failed to delete.")
                                }
                            }
                        }
                        catch (e: java.lang.Exception) {
                            Log.d("Delete", "Response: %s".format(e.message.toString()))
                        }
                    },
                    { error ->
                        // Handle error response, if required
                        Log.d("Delete", "Error Response: ${error.message}")
                    }
                )
                jsonObjectRequest.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
                WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun openImagePicker() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, 1)
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

    private fun formatDateToStringFromLong(dateLong: Long?): String {
        return if (dateLong != null) {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.format(Date(dateLong))
        } else {
            ""
        }
    }

    private fun toast(context: Context?, msg: String) {
        context?.let {
            Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
        }
    }

}
