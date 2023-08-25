package my.edu.tarc.zeroxpire.view.goal

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.MainActivity
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

    private lateinit var requestQueue: RequestQueue

    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth

    private var goalId: Int? = null

    private var ingredientWithGoalId: MutableList<Ingredient> = mutableListOf()
    private var ingredientWithGoalIdNeedToBeCleared: MutableList<Ingredient> = mutableListOf()
    private var finalSelectionList: MutableList<Ingredient> = mutableListOf()
    private var getIngredientWithoutGoalIdFromDB: MutableList<Ingredient> = mutableListOf()
    private var selectedIngredients: MutableList<Ingredient> = mutableListOf()

    private lateinit var adapter: IngredientAdapter
    private lateinit var bottomSheetIngredientAdapter: IngredientAdapter
    private lateinit var selectedIngredientAdapter: IngredientAdapter
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var recyclerView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentGoalDetailBinding.inflate(inflater, container, false)

        requestQueue = Volley.newRequestQueue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        ///initialize things
        auth = FirebaseAuth.getInstance()
        adapter = IngredientAdapter(this, goalViewModel)
        bottomSheetIngredientAdapter = IngredientAdapter(this, goalViewModel)

        //recyclerview stuff
        binding.ingredientsInGoal.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsInGoal.adapter = adapter
        recyclerView = binding.ingredientsInGoal
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        //clear selections
        ingredientWithGoalIdNeedToBeCleared.clear()
        finalSelectionList.clear()
        getIngredientWithoutGoalIdFromDB.clear()
        selectedIngredients.clear()
        ingredientWithGoalId.clear()

        //widget declaration
        val addIngredientDialogBtn = binding.addIngredientsBtn
        val chooseTargetCompletionDatePicker = binding.chooseTargetCompletionDate
        val upBtn = binding.upBtn
        val saveBtn = binding.saveBtn
        val deleteBtn = binding.deleteBtn

        //get goal details from goalFragment
        setFragmentResultsFunctions()

        //load from room db and display as an option in the bottom sheet
        ingredientViewModel.getAllIngredientsWithoutGoalId()
            .observe(viewLifecycleOwner, Observer { ingredients ->
                getIngredientWithoutGoalIdFromDB = ingredients as MutableList<Ingredient>
                bottomSheetIngredientAdapter.setIngredient(getIngredientWithoutGoalIdFromDB)
                for(ingredient in ingredients){
                    Log.d("GoalDetailFragment: getIngredientWithoutGoalId", ingredient.ingredientName)
                }
            })

        //listeners
        addIngredientDialogBtn.setOnClickListener {
            showBottomSheetDialog()
        }
        chooseTargetCompletionDatePicker.setOnClickListener {
            showDatePickerDialog()
        }
        upBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        saveBtn.setOnClickListener {
            if(finalSelectionList.isEmpty()){
                deleteGoal("The goal must have at least 1 ingredient, or you want delete this goal instead?")
            }
            else{
                updateGoal()
            }
        }
        deleteBtn.setOnClickListener {
            deleteGoal("Are you sure want to delete this goal?")
        }


        //swipe to delete from the existing ingredient list
        swipeToDeleteExistingIngredientList()
    }

    private fun setFragmentResultsFunctions(){
        // Set fragment result listeners to receive data from other fragments
        setFragmentResultListener("requestName") { _, bundle ->
            val result = bundle.getString("name")
            originalName = result
            binding.enterIngredientName.setText(originalName)
        }

        setFragmentResultListener("requestDate") { _, bundle ->
            val dateString = bundle.getString("date")
            selectedDate = parseDateStringToLong(dateString)
            binding.chooseTargetCompletionDate.setText(dateString)
        }

        setFragmentResultListener("requestId") { _, bundle ->
            val goalIdd = bundle.getInt("id")
            goalId = goalIdd
            // Observe ingredients only if goalId is not null
            if (goalId != null) {
                ingredientViewModel.getIngredientsByGoalId(goalId!!)
                    .observe(viewLifecycleOwner, Observer { ingredients ->

                        //set recyclerview with the ingredients which associated with the goalId
                        adapter.setIngredient(ingredients)

                        finalSelectionList = ingredients as MutableList<Ingredient>
                        ingredientWithGoalId = finalSelectionList

                        Log.d("GoalDetailFragment: ingredientWithGoalId", ingredients.toString())
                        Log.d("GoalDetailFragment: finalSelectionList", finalSelectionList.toString())
                    })
            }
        }
    }

    private fun showBottomSheetDialog() {
        //initialize recyclerview components
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        recyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = bottomSheetIngredientAdapter

        //widget declaration
        val header = bottomSheetView.findViewById<TextView>(R.id.selectedTextView)
        val addBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)

        //widget initialization
        header.text = "Add ingredient to your goal"
        addBtn.isEnabled = selectedIngredients.isNotEmpty()

        bottomSheetDialog.show()

        //widget listeners
        addBtn.setOnClickListener {

            if (selectedIngredients.isNotEmpty()) {

                //check whether the selected ingredient in the bottom sheet is the element of the ingredientWithGoalIdNeedToBeCleared
                //if yes, remove from ingredientWithGoalIdNeedToBeCleared, else remain because it is ready to be removed form this goal
                for (selectedIngredient in selectedIngredients) {
                    for (ingredientWithGoalId in ingredientWithGoalId) {
                        if (selectedIngredient.ingredientGoalId == ingredientWithGoalId.ingredientGoalId) {
                            ingredientWithGoalIdNeedToBeCleared.remove(selectedIngredient)
                        }
                    }
                }

                //finalSelectionList + selectedIngredients
                //existing list + selected list
                finalSelectionList.addAll(selectedIngredients)

                //remove all selections from the selected ingredients
                getIngredientWithoutGoalIdFromDB.removeAll(selectedIngredients)

                //notify adapter to update the recycler view
                bottomSheetIngredientAdapter.notifyDataSetChanged()
                adapter.notifyDataSetChanged()

                Toast.makeText(requireContext(), "Added to the list", Toast.LENGTH_SHORT).show()

                bottomSheetDialog.setOnDismissListener {
                    selectedIngredients.clear()
                }

                bottomSheetDialog.dismiss()
            }
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
                binding.chooseTargetCompletionDate.setText(dateString)
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

    private fun deleteGoal(msg: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("Delete") { _, _ ->
                clearGoalIdForIngredient(goalId!!)
                val url = getString(R.string.url_server) + getString(R.string.url_delete_goal) + "?goalId=" + goalId
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.POST, url, null,
                    { response ->
                        try {
                            if(response!=null){
                                val strResponse = response.toString()
                                val jsonResponse = JSONObject(strResponse)
                                val success: String = jsonResponse.get("success").toString()

                                if (success == "1") {
                                    toast(requireContext(), "Goal is deleted successfully.")
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

    private fun toast(context: Context?, msg: String) {
        context?.let {
            Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGoal() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Updating...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        //updateGoalDetails()

        val urlClear = getString(R.string.url_server) + getString(R.string.url_clearGoalIdForIngredient_goal) + "?goalId=" + goalId
        val jsonObjectRequestClear = JsonObjectRequest(
            Request.Method.POST, urlClear, null,
            { response ->
                try {
                    if(response!=null){
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {
                            val ingredientIds =
                                finalSelectionList.joinToString("&ingredientIDArr[]=") { it.ingredientId.toString() }
                            for (ingredient in finalSelectionList) {
                                Log.d("GoalDetailFragment: ingredientWithGoalIdFromDBUpdate", ingredient.ingredientName)
                            }
                            val url =
                                getString(R.string.url_server) + getString(R.string.url_updateGoalIdForIngredient_goal) +
                                        "?goalId=" + goalId + "&ingredientIDArr[]=$ingredientIds"

                            Log.d("GoalDetailFragment: updateURLLLLLLLLLLL", url.toString())

                            val jsonObjectRequest = JsonObjectRequest(
                                Request.Method.POST, url, null,
                                { response ->
                                    try {
                                        if (response != null) {
                                            val strResponse = response.toString()
                                            val jsonResponse = JSONObject(strResponse)
                                            val success: String = jsonResponse.get("success").toString()

                                            if (success == "1") {

                                            } else {
                                                //toast(requireContext(), "Failed to update.")
                                            }
                                        }
                                    } catch (e: java.lang.Exception) {
                                        Log.d("UpdateGoal", "Response: %s".format(e.message.toString()))
                                    }
                                },
                                { error ->
                                    // Handle error response, if required
                                    Log.d("UpdateGoal", "Error Response: ${error.message}")
                                }
                            )
                            jsonObjectRequest.retryPolicy =
                                DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
                            WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
//        }

                            progressDialog?.dismiss()
                            Toast.makeText(requireContext(), "Goal detail is updated successfully.", Toast.LENGTH_SHORT)
                                .show()
                            findNavController().navigateUp()
                            findNavController().clearBackStack(R.id.goalFragment)

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
        jsonObjectRequestClear.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequestClear)


    }

    private fun updateGoalDetails() {

        val urlUpdateGoalDetails =
            getString(R.string.url_server) + getString(R.string.url_update_goal) +
                    "?goalId=" + goalId + "&goalName=" + binding.enterIngredientName.text.toString() + "&targetCompletionDate=" + SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )
                .format(binding.chooseTargetCompletionDate.text)


        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, urlUpdateGoalDetails, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {

                        } else {

                        }
                    }
                } catch (e: java.lang.Exception) {
                    Log.d("Update", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->
                // Handle error response, if required
                Log.d("Update", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy =
            DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)

    }

    private fun swipeToDeleteExistingIngredientList() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Remove this ingredient from this goal?").setCancelable(false)
                    .setPositiveButton("Remove") { dialog, id ->
                        Log.d("ingredientSizeLeft", finalSelectionList.size.toString())
                        val position = viewHolder.adapterPosition

                        val ingredientToRemove = adapter.getIngredientAt(position)

                        // Add the ingredient to be removed to the ingredientWithGoalIdNeedToBeCleared list
                        if (ingredientToRemove in ingredientWithGoalId) {
                            ingredientWithGoalIdNeedToBeCleared.add(ingredientToRemove)
                        }

                        // Remove the ingredient from the lists
                        finalSelectionList.remove(ingredientToRemove)
                        getIngredientWithoutGoalIdFromDB.add(ingredientToRemove)

                        bottomSheetIngredientAdapter.notifyItemRemoved(position)
                        adapter.notifyItemRemoved(position)

                        // Clear selections
                        selectedIngredients.clear()

                        dialog.dismiss()

                        Log.d("GoalDetailFragment: finalSelectionList", finalSelectionList.size.toString())
                        if (finalSelectionList.isEmpty()) {
                            binding.emptyIngredientImageView.visibility = View.VISIBLE
                        } else {
                            binding.emptyIngredientImageView.visibility = View.GONE
                        }

                    }.setNegativeButton("Cancel") { dialog, id ->
                        dialog.dismiss()
                        adapter.notifyDataSetChanged()
                    }
                val alert = builder.create()
                alert.show()


            }


            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                ).addBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.secondaryColor
                    )
                ).addActionIcon(R.drawable.baseline_delete_24).create().decorate()
                super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    private fun clearGoalIdForIngredient(goalId: Int) {

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
        //recycler view stuffs
        recyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        val layoutManager = recyclerView.layoutManager

        //widget declaration
        val addBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        val selectedTextView = bottomSheetView.findViewById<TextView>(R.id.selectedTextView)

        if (layoutManager is LinearLayoutManager) {
            val clickedItemPosition = bottomSheetIngredientAdapter.getPosition(ingredient)
            val clickedItemView = layoutManager.findViewByPosition(clickedItemPosition)

            // Check if the ingredient is not already in the selectedIngredientsTemporary list
            if (!selectedIngredients.contains(ingredient)) {
                // Change the background color of the clicked item to the selected color
                clickedItemView?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.btnColor
                    )
                )
                clickedItemView?.tag = true

                // Select the ingredient
                selectedIngredients.add(ingredient)
            } else {
                // If the ingredient is already in the list, remove it to toggle the selection
                clickedItemView?.setBackgroundColor(Color.WHITE)
                clickedItemView?.tag = false
                selectedIngredients.remove(ingredient)
            }
        }

        addBtn.isEnabled = selectedIngredients.isNotEmpty()
        selectedTextView.text = if (selectedIngredients.isEmpty()) {
            "Add ingredient to your goal"
        } else {
            "${selectedIngredients.size} ingredient selected."
        }

        Log.d("GoalDetailFragment: SelectedIngredients", selectedIngredients.toString())
    }

    private fun formatDateToStringFromLong(dateLong: Long?): String {
        return if (dateLong != null) {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.format(Date(dateLong))
        } else {
            ""
        }
    }
}

//TODO settle the date format