package my.edu.tarc.zeroxpire.view.goal

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.databinding.FragmentCreateGoalBinding
import my.edu.tarc.zeroxpire.goal.LatestGoalIdCallback
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*


class CreateGoalFragment : Fragment(), IngredientClickListener{
    private lateinit var binding: FragmentCreateGoalBinding
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var selectedCompletionDate: Date? = null

    val goalViewModel : GoalViewModel by activityViewModels()
    val ingredientViewModel: IngredientViewModel by activityViewModels()

    private var selectedIngredients: MutableList<Ingredient> = mutableListOf()
    private var selectedIngredientsTemporary: MutableList<Ingredient> = mutableListOf()
    private var storedIngredients: MutableList<Ingredient> = mutableListOf()
    private var getFromStoredIngredients: MutableList<Ingredient> = mutableListOf()

    private lateinit var bottomSheetIngredientAdapter: IngredientAdapter
    private lateinit var selectedIngredientAdapter: IngredientAdapter

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var recyclerView: RecyclerView

    //data
    private var id: Int = 0
    private var name: String = ""
    private var date: String = ""
    private var numOfIngredients: Int = 0

    private var selectedDate: Date? = null // Variable to store the selected date
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        binding = FragmentCreateGoalBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedIngredientAdapter = IngredientAdapter(object : IngredientClickListener {
            override fun onIngredientClick(ingredient: Ingredient) {
                // Do nothing here, as this is a dummy click listener
            }
        }, goalViewModel)
        bottomSheetIngredientAdapter = IngredientAdapter(this, goalViewModel)

        val addIngredientDialogBtn = binding.addIngredientDialogBtn

        storedIngredients.clear()
        selectedIngredients.clear()
        selectedIngredientsTemporary.clear()
        getFromStoredIngredients.clear()

        ingredientViewModel.ingredientList.observe(viewLifecycleOwner){ingredients->
            storedIngredients = ingredients as MutableList<Ingredient>
            getFromStoredIngredients = storedIngredients
            Log.d("Stored ingredients", getFromStoredIngredients.toString())
        }




        addIngredientDialogBtn.setOnClickListener {
            showBottomSheetDialog(bottomSheetIngredientAdapter)
        }

        binding.chooseTargetCompletionDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.createBtn.setOnClickListener {
            storeGoal()
        }

        selectedIngredientRecyclerReview(selectedIngredientAdapter)

        //nav stuff
        navBack()
    }

    private fun storeGoal(){
        val goalName = binding.enterGoalName.text.toString()
        val targetCompletionDate = selectedDate

        // Check if targetCompletionDate is null before storing the goal
        if (targetCompletionDate == null) {
            // Show an error message or any other appropriate action
            // For example, you can display a Toast message:
            Toast.makeText(requireContext(), "Please select a completion date", Toast.LENGTH_SHORT).show()
            return
        }

        val targetCompletionDateConverted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(targetCompletionDate)
        val url = getString(R.string.url_server) + getString(R.string.url_create_goal) +
                "?goalName=" + goalName +
                "&targetCompletionDate=" + targetCompletionDateConverted

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {
                            Toast.makeText(requireContext(), getString(R.string.delete), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.recipeDetailsErrorOccurred), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    updateManually()
                    Log.d("Fragment", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->

                Log.d("Second", "Response : %s".format(error.message.toString()))
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)

        getLatestGoalId(object : LatestGoalIdCallback {
            override fun onLatestGoalIdReceived(goalId: Int) {
                // Use the latestGoalId here
                Log.d("LatestgoalIdddddd", goalId.toString())
                testing(goalId)
                updateGoalIdForIngredient(goalId)
                // ... (other code)

                // You can now perform the actions that need to be executed after getting the latestGoalId
                findNavController().navigateUp()
                findNavController().clearBackStack(R.id.goalFragment)
            }
        })
    }

    private fun updateManually(){
        for(ingredient in selectedIngredients){
            val url = getString(R.string.url_server) + getString(R.string.url_getLatestGoalId_goal) +
                    "?ingredientId=" + ingredient.ingredientId
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, null,
                { response ->
                    try {
                        if(response!=null){
                            val strResponse = response.toString()
                            val jsonResponse = JSONObject(strResponse)
                            val success: String = jsonResponse.get("success").toString()

                            if (success == "1") {
                                Toast.makeText(requireContext(), "GoalID is updated successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                //toast(requireContext(), "Failed to update.")
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
        }
    }

    private fun updateGoalIdForIngredient(goalId: Int){
//        var url: String? = null
        for(ingredient in selectedIngredients){
            val url = getString(R.string.url_server) + getString(R.string.url_updateGoalIdForIngredient_goal) +
                    "?goalId=" + goalId + "&ingredientId=" + ingredient.ingredientId
            Log.d("ingredientID", ingredient.ingredientId.toString())
            Log.d("goalId", goalId.toString())
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, null,
                { response ->
                    try {
                        if(response!=null){
                            val strResponse = response.toString()
                            val jsonResponse = JSONObject(strResponse)
                            val success: String = jsonResponse.get("success").toString()

                            if (success == "1") {
                                Toast.makeText(requireContext(), "GoalID is updated successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                //toast(requireContext(), "Failed to update.")
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
        }

    }

    private fun testing(goalId: Int) {
        Toast.makeText(requireContext(), goalId.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun getLatestGoalId(callback: LatestGoalIdCallback) {
        val url = getString(R.string.url_server) + getString(R.string.url_getLatestGoalId_goal)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        var latestGoalId = 0

                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonGoal: JSONObject = jsonArray.getJSONObject(i)
                                val goalId = jsonGoal.getInt("goalId")
                                latestGoalId = goalId
                            }
                        }

                        // Invoke the callback with the latestGoalId
                        callback.onLatestGoalIdReceived(latestGoalId)
                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                } catch (e: Exception) {
                    Log.d("ContactRepository", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("FuuuuuuuuuuuuuuuuccccccK", "Error Response: ${error.message}")
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }



    private fun selectedIngredientRecyclerReview(adapter: IngredientAdapter){
        recyclerView = binding.selectedIngredientRecyclerViewGoal
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        adapter.setIngredient(selectedIngredients)
        deleteSelectedIngredient(adapter, recyclerView)
    }

    private fun showBottomSheetDialog(adapter: IngredientAdapter) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        recyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val addBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        addBtn.isEnabled = selectedIngredientsTemporary.isNotEmpty()

        addBtn.setOnClickListener {
            selectedIngredients = selectedIngredientsTemporary.toMutableList()
            Log.d("Temporary -> Selected", selectedIngredients.toString())
            bottomSheetDialog.dismiss()

            // Notify the selectedIngredientAdapter about the data change
            selectedIngredientAdapter.setIngredient(selectedIngredients)
            Log.d("minus",getFromStoredIngredients.minus(selectedIngredients).toString())
            bottomSheetDialog.setOnDismissListener {
                selectedIngredientsTemporary.clear()
                Log.d("minus",getFromStoredIngredients.minus(selectedIngredients).toString())

            }
            if(selectedIngredients.isNotEmpty()){
                Log.d("Selected is not empty", selectedIngredients.size.toString())
                binding.noIngredientHasRecordedLayout.visibility = View.INVISIBLE
                binding.numOfSelectedIngredientsTextView.text = "Total: ${selectedIngredients.size} ingredient"
            }
            else {
                Log.d("Selected is empty", selectedIngredients.size.toString())
                binding.noIngredientHasRecordedLayout.visibility = View.VISIBLE
                binding.numOfSelectedIngredientsTextView.visibility = View.INVISIBLE
            }
        }


//        // Remove ingredients that are already stored in selectedIngredients from getFromStoredIngredients
//        getFromStoredIngredients.removeAll(selectedIngredients)

        bottomSheetDialog.show()

        adapter.setIngredient(getFromStoredIngredients.minus(selectedIngredients.toSet()))
        adapter.notifyDataSetChanged()
    }


    private fun deleteSelectedIngredient(adapter: IngredientAdapter, recyclerView: RecyclerView) {
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
                builder.setMessage("Are you sure you want to Delete?").setCancelable(false)
                    .setPositiveButton("Delete") { dialog, id ->
                        val position = viewHolder.adapterPosition
                        selectedIngredients.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        dialog.dismiss()
                        if (selectedIngredients.isEmpty()) {
                            binding.noIngredientHasRecordedLayout.visibility = View.VISIBLE
                            binding.numOfSelectedIngredientsTextView.visibility = View.INVISIBLE
                        } else {
                            binding.noIngredientHasRecordedLayout.visibility = View.INVISIBLE
                            binding.numOfSelectedIngredientsTextView.text = "Total: ${selectedIngredients.size} ingredient"
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
                binding.chooseTargetCompletionDate.setText(selectedDateString)
            },
            year,
            month,
            dayOfMonth
        )

        datePickerDialog.show()
    }


    private fun navBack() {
        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onIngredientClick(ingredient: Ingredient) {
        recyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        val layoutManager = recyclerView.layoutManager

        if (layoutManager is LinearLayoutManager) {
            val clickedItemPosition = bottomSheetIngredientAdapter.getPosition(ingredient)
            val clickedItemView = layoutManager.findViewByPosition(clickedItemPosition)

            val isItemSelected = clickedItemView?.tag as? Boolean ?: false

            if (isItemSelected) {
                // Reset the background color of the clicked item to the default color
                clickedItemView?.setBackgroundColor(Color.WHITE)
                clickedItemView?.tag = false

                // Deselect the ingredient if it was selected
                selectedIngredientsTemporary.remove(ingredient)
            } else {
                // Check if the ingredient is not already in the selectedIngredientsTemporary list
                if (!selectedIngredientsTemporary.contains(ingredient)) {
                    // Change the background color of the clicked item to the selected color
                    clickedItemView?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.btnColor))
                    clickedItemView?.tag = true

                    // Select the ingredient
                    selectedIngredientsTemporary.add(ingredient)
                } else {
                    // If the ingredient is already in the list, remove it to toggle the selection
                    //clickedItemView?.setBackgroundColor(Color.WHITE)
                    clickedItemView?.tag = false
                    selectedIngredientsTemporary.remove(ingredient)
                }
            }
        }

        val addBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        addBtn.isEnabled = selectedIngredientsTemporary.isNotEmpty()

        val selectedTextView = bottomSheetView.findViewById<TextView>(R.id.selectedTextView)
        selectedTextView.text = if(selectedIngredientsTemporary.isEmpty()){
            "Select ingredients that you want to clear."
        }
        else{
            "${selectedIngredientsTemporary.size} ingredient selected."
        }

        Log.d("SelectedIngredients", selectedIngredientsTemporary.toString())
    }



}