package my.edu.tarc.zeroxpire.view.ingredient

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import my.edu.tarc.zeroxpire.MainActivity
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.databinding.FragmentIngredientBinding
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*


class IngredientFragment : Fragment(), IngredientClickListener {
    private lateinit var binding: FragmentIngredientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var requestQueue: RequestQueue

    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    private var isSort: Boolean = false
    private var id: Int = 0

    private var progressDialog: ProgressDialog? = null
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetDialog: BottomSheetDialog

    val CHANNEL_ID = "channelID"
    val CHANNEL_NAME = "channelName"
    val NOTIF_ID = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        binding = FragmentIngredientBinding.inflate(inflater, container, false)

        requestQueue = Volley.newRequestQueue(requireContext())


        return binding.root
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val mainActivity = activity as? MainActivity
        mainActivity?.loadIngredient()

        binding.sortBtn.setBackgroundResource(R.drawable.baseline_sort_24)

        val user = Firebase.auth.currentUser
        if (user != null) {
            val photoUrl = user.photoUrl
            val profilePictureUrl = photoUrl?.toString()
            if (profilePictureUrl != null) {
                Glide.with(this)
                    .load(profilePictureUrl)
                    .into(binding.profilePicture)
            }
            else{
                Glide.with(this)
                    .load(R.drawable.messi)
                    .into(binding.profilePicture)
            }
            if(user.displayName == ""){
                getUsername()
            } else {
                getUsername()
                binding.username.text = user.displayName
            }
        } else {
            binding.username.text = "You are not signed in yet!"
            binding.profilePicture.elevation = 0F
        }

        val adapter = IngredientAdapter(this, goalViewModel)

        //loadIngredient(adapter)

        //createNotificationChannel()



        ingredientViewModel.ingredientList.observe(viewLifecycleOwner, Observer { ingredients ->
            goalViewModel.goalList.observe(viewLifecycleOwner, Observer { goals ->
                val unconsumedIngredients = ingredients.filter { ingredient ->
                    val goal = goals.find { it.goalId == ingredient.ingredientGoalId }
                    goal?.completedDate == null
                }

                logg("unconsumedIngredients: $unconsumedIngredients")
                adapter.setIngredient(unconsumedIngredients)


            })
        })


        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter

        binding.recyclerview.visibility = View.VISIBLE

        sortIngredient(adapter)

        binding.filterBtn.setOnClickListener{
            filterIngredient(adapter)
        }

        searchIngredient(adapter)
        delete(adapter)
        greeting()
        navigateBack()
    }

    private fun filterIngredient(adapter: IngredientAdapter) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_filter, null)

        val resetButton = bottomSheetView.findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
            // Clear all filters and show the original list
            adapter.setIngredient(ingredientViewModel.ingredientList.value ?: emptyList())
            bottomSheetDialog.dismiss() // Close the bottom sheet after resetting
        }
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        applyFilters(adapter, bottomSheetView)
    }

    private fun applyFilters(adapter: IngredientAdapter, bottomSheetView: View) {

        val expired = bottomSheetView.findViewById<RadioButton>(R.id.radioExpired)
        val notExpired = bottomSheetView.findViewById<RadioButton>(R.id.radioNotExpired)
        val isGoal = bottomSheetView.findViewById<RadioButton>(R.id.radioIsGoal)
        val isNotGoal = bottomSheetView.findViewById<RadioButton>(R.id.radioIsNotGoal)
        val vegChip = bottomSheetView.findViewById<Chip>(R.id.chipVegetables)
        val meatChip = bottomSheetView.findViewById<Chip>(R.id.chipMeats)
        val fruitsChip = bottomSheetView.findViewById<Chip>(R.id.chipFruits)
        val seafoodChip = bottomSheetView.findViewById<Chip>(R.id.chipSeafood)
        val eggProductsChip = bottomSheetView.findViewById<Chip>(R.id.chipEggProducts)
        val othersChip = bottomSheetView.findViewById<Chip>(R.id.chipOthers)

        val applyBtn = bottomSheetView.findViewById<Button>(R.id.applyButton)

        applyBtn.setOnClickListener {
            var filteredIngredients = ingredientViewModel.ingredientList.value ?: emptyList()

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)


            if (expired.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.expiryDate.before(today.time)
                }
            }

            if (notExpired.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.expiryDate.after(today.time) || ingredient.expiryDate == today.time
                }
            }

            if (isGoal.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientGoalId != null
                }
            }

            if (isNotGoal.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientGoalId == null
                }
            }

            if (vegChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Vegetables"
                }
            }

            if (meatChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Meat"
                }
            }

            if (fruitsChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Fruits"
                }
            }

            if (seafoodChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Seafood"
                }
            }

            if (eggProductsChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Egg Products"
                }
            }

            if (othersChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Others"
                }
            }

            adapter.setIngredient(filteredIngredients)
            bottomSheetDialog.dismiss()
        }
    }




//    private fun applyFilter(filterOption: String, adapter: IngredientAdapter) {
//
//
//        val filteredIngredients = when (filterOption) {
//            "Expired" -> allIngredients.filter { ingredient ->
//                ingredient.expiryDate.before(Date()) // Ingredients that have expired
//            }
//            "Not Expired" -> allIngredients.filter { ingredient ->
//                ingredient.expiryDate.after(Date()) // Ingredients that haven't expired
//            }
//            "Not Attached to Goal" -> allIngredients.filter { ingredient ->
//                ingredient.ingredientGoalId == null // Ingredients not attached to a goal
//            }
//            "Grains", "Fruits", "Vegetables" -> allIngredients.filter { ingredient ->
//                ingredient.ingredientCategory == filterOption // Ingredients with selected category
//            }
//            "All" -> allIngredients // Show all ingredients
//            else -> emptyList() // Default case, no matching filter option
//        }
//
//        binding.notFoundText.visibility = if (filteredIngredients.isEmpty()) View.VISIBLE else View.GONE
//        binding.allIngredientsTextView.visibility = if (filteredIngredients.isEmpty()) View.GONE else View.VISIBLE
//
//
//    }


    private fun getUsername(){
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_username) + "?userId=${auth.currentUser?.uid}"
        Log.d("uid", auth.currentUser?.uid.toString())
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()


                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonUser: JSONObject = jsonArray.getJSONObject(i)
                                val getUserId = jsonUser.getString("userId")
                                val getUserName = jsonUser.getString("userName")
                                val stayLoggedIn = jsonUser.getInt("stayLoggedIn")
                                Log.d("username", getUserName)
                                binding.username.text = getUserName
                            }
                        }


                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")

                } catch (e: java.lang.Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")

                }
            },
            { error ->
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reminder(ingredients: List<Ingredient>) {
        val notificationManager = NotificationManagerCompat.from(requireContext())

        if (ingredients.isNotEmpty()) {
            val channelId = CHANNEL_ID
            val notificationId = NOTIF_ID

            var absDaysLeft: Long? = null
            var count: Int = 0

            for (ingredient in ingredients){
                val expiryDate: LocalDate? = ingredient.expiryDate.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                val currentDate: LocalDate = LocalDate.now()
                absDaysLeft = ChronoUnit.DAYS.between(currentDate, expiryDate)
                count += 1
            }




            val expiryMessage = if(absDaysLeft == 0L){
                "$count ingredient is expiring today."
            }else{
                return
            }

            val notificationBuilder = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Expiry Date Alert")
                .setContentText(expiryMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            // Load the ingredientImage using Glide and convert it to a Bitmap
            // For simplicity, you can omit the image in the summary notification

            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                lightColor = Color.GREEN
                enableLights(true)

            }
            val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    private fun sortIngredient(adapter: IngredientAdapter) {
        var sortMode = SortMode.BY_EXPIRY_DATE_ASC

        binding.sortBtn.setOnClickListener {
            when (sortMode) {
                SortMode.BY_EXPIRY_DATE_ASC -> {
                    ingredientViewModel.sortByExpiryDate().observe(viewLifecycleOwner, Observer {

                        Log.d("expSortAsc", it.toString())
                        adapter.setIngredient(it)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable._023915_sort_ascending_fill_icon)
                    sortMode = SortMode.BY_EXPIRY_DATE_DESC
                }
                SortMode.BY_EXPIRY_DATE_DESC -> {
                    ingredientViewModel.sortByExpiryDateDesc().observe(viewLifecycleOwner, Observer {
                        Log.d("expSortDesc", it.toString())
                        adapter.setIngredient(it)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable._023914_sort_descending_fill_icon)
                    sortMode = SortMode.BY_DATE_ADDED
                }
                SortMode.BY_DATE_ADDED -> {
                    ingredientViewModel.sortByDateAdded().observe(viewLifecycleOwner,  Observer {
                        Log.d("dateAddedsort", it.toString())
                        adapter.setIngredient(it)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable.baseline_sort_24)
                    sortMode = SortMode.BY_EXPIRY_DATE_ASC
                }
            }
        }
    }

    enum class SortMode {
        BY_EXPIRY_DATE_ASC,
        BY_EXPIRY_DATE_DESC,
        BY_DATE_ADDED
    }




    fun loadIngredient(adapter: IngredientAdapter?) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Loading...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_ingredient) + "?userId=${auth.currentUser?.uid}"
        Log.d("uid", auth.currentUser?.uid.toString())
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
                                    Log.d("IngredientCategory", ingredient.ingredientCategory)
                                }
                            }
                        }

                        // Dismiss the progress dialog when finished loading ingredients
                        progressDialog?.dismiss()

                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                    progressDialog?.dismiss()
                } catch (e: Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")
                    progressDialog?.dismiss()
                }
            },
            { error ->
                //i think is when there is nothing to return then it will return 404
                ingredientViewModel.deleteAllIngredients()
                binding.recyclerview.visibility = View.INVISIBLE
                binding.emptyHereContent.visibility  = View.VISIBLE
                //binding.ingredientSearchView.visibility = View.INVISIBLE
                //binding.labels.visibility = View.INVISIBLE
                binding.notFoundText.visibility = View.INVISIBLE
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

    private fun uiInitialization(size: Int?) {
        logg("ingredientListSize: $size")

//        if (ingredientList!! > 0) {
//            binding.sortBtn.visibility = View.VISIBLE
//            binding.allIngredientsTextView.visibility = View.VISIBLE
//            binding.recyclerview.visibility = View.VISIBLE
//            binding.emptyHereContent.visibility = View.GONE
//            binding.ingredientSearchView.visibility = View.VISIBLE
//            binding.labels.visibility = View.VISIBLE
//            binding.notFoundText.visibility = View.INVISIBLE
//        }
//        else{
//            binding.sortBtn.visibility = View.GONE
//            binding.allIngredientsTextView.visibility = View.GONE
//            binding.recyclerview.visibility = View.GONE
//            binding.emptyHereContent.visibility = View.VISIBLE
//            binding.ingredientSearchView.visibility = View.GONE
//            binding.labels.visibility = View.GONE
//            binding.notFoundText.visibility = View.GONE
//        }
    }
    private fun searchIngredient(adapter: IngredientAdapter) {
        binding.ingredientSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val filteredIngredients = ingredientViewModel.ingredientList.value?.filter { ingredient ->
                    ingredient.ingredientName.contains(newText, ignoreCase = true)
                }

                if (filteredIngredients.isNullOrEmpty()) {
                    binding.notFoundText.visibility = View.VISIBLE
                    binding.allIngredientsTextView.visibility = View.VISIBLE // Add this line
                } else {
                    binding.notFoundText.visibility = View.INVISIBLE
                    binding.allIngredientsTextView.visibility = View.VISIBLE // Add this line
                }

                adapter.setIngredient(filteredIngredients ?: emptyList())

                return true
            }
        })
    }

    private fun navigateBack() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to Exit the app?").setCancelable(false)
                    .setPositiveButton("Exit") { dialog, id ->
                        requireActivity().finish()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )
    }

    private fun greeting() {
        val greeting = binding.greeting
        if (isEvening()) {
            greeting.text = "Good evening!"
        }
        if (isMorning()) {
            greeting.text = "Good morning!"
        }
        if (isAfternoon()) {
            greeting.text = "Good afternoon!"
        }
    }

    private fun delete(adapter: IngredientAdapter) {
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
                val goalName = checkDependency(viewHolder.adapterPosition, adapter)
                val builder = AlertDialog.Builder(requireContext())
                if(goalName.isEmpty()){
                    builder.setMessage("Are you sure you want to Delete?").setCancelable(false)
                        .setPositiveButton("Delete") { dialog, id ->
                            progressDialog = ProgressDialog(requireContext())
                            progressDialog?.setMessage("Deleting...")
                            progressDialog?.setCancelable(false)
                            progressDialog?.show()
                            val position = viewHolder.adapterPosition
                            val deletedIngredient = adapter.getIngredientAt(position)
                            ingredientViewModel.deleteIngredient(deletedIngredient)
                            val url = getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + deletedIngredient.ingredientId
                            Log.d("id:::::::::::::", deletedIngredient.ingredientId.toString())
                            val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                                { response ->
                                    // Handle successful deletion response, if required
                                    progressDialog?.dismiss()

                                    toast("Ingredient deleted.")
                                },
                                { error ->
                                    // Handle error response, if required
                                    Log.d("FK", "Error Response: ${error.message}")
                                }
                            )

                            requestQueue.add(jsonObjectRequest)
                        }.setNegativeButton("Cancel") { dialog, id ->
                            adapter.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }
                else{
                    builder.setMessage("This ingredient is currently in the goal \"$goalName\". " +
                            "\n\nDeleting this ingredient will also delete goal \"$goalName\"." +
                    "\n\n Are you sure want to delete?").setCancelable(false)
                        .setPositiveButton("Delete") { dialog, id ->
                            progressDialog = ProgressDialog(requireContext())
                            progressDialog?.setMessage("Deleting goal \"$goalName\"...")
                            progressDialog?.setCancelable(false)
                            progressDialog?.show()
                            val position = viewHolder.adapterPosition
                            val deletedIngredient = adapter.getIngredientAt(position)
                            //ingredientViewModel.deleteIngredient(deletedIngredient)

                            val urlDeleteGoal = getString(R.string.url_server) + getString(R.string.url_delete_goal) + "?goalId=" + deletedIngredient.ingredientGoalId
                            val jsonObjectRequestDeleteGoal = JsonObjectRequest(Request.Method.POST, urlDeleteGoal, null,
                                { response ->
                                    // Handle successful deletion response, if required
//                                Toast.makeText(requireContext(), "Goal is deleted successfully.", Toast.LENGTH_SHORT).show()

                                    clearGoalIdForIngredient(deletedIngredient)
                                    adapter.notifyDataSetChanged()
                                },
                                { error ->
                                    // Handle error response, if required
                                    Log.d("Errorrrrrr", "Error Response: ${error.message}")
                                }
                            )
                            requestQueue.add(jsonObjectRequestDeleteGoal)


                        }.setNegativeButton("Cancel") { dialog, id ->
                            adapter.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }


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
        itemTouchHelper.attachToRecyclerView(binding.recyclerview)
    }

    private fun clearGoalIdForIngredient(ingredient: Ingredient) {
        progressDialog?.setMessage("Deleting ingredient \"${ingredient.ingredientName}\"...")
        progressDialog?.setCancelable(false)
        val url = getString(R.string.url_server) + getString(R.string.url_clearGoalIdForIngredient_goal) + "?goalId=" + ingredient.ingredientGoalId
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->

                val url = getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + ingredient.ingredientId
                val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                    { response ->
                        // Handle successful deletion response, if required

                        progressDialog!!.dismiss()
                        toast("Ingredient and goal has deleted successfully.")



                    },
                    { error ->
                        // Handle error response, if required
                        Log.d("FK", "Error Response: ${error.message}")
                    }
                )

                requestQueue.add(jsonObjectRequest)
            },
            { error ->
                // Handle error response, if required
                Log.d("FK", "Error Response: ${error.message}")
            }
        )
        requestQueue.add(jsonObjectRequest)
    }


    private fun checkDependency(position: Int, adapter: IngredientAdapter): String {
        logg("position: $position")
        goalViewModel.goalList.value?.map {
            if(adapter.getIngredientAt(position).ingredientGoalId == it.goalId){
                logg("Comparison: ${adapter.getIngredientAt(position).ingredientGoalId} == ${it.goalId}")
                return it.goalName
            }

        }
        return ""

    }

    private fun logg(msg: String){
        Log.d("IngredientFragment", msg)
    }

    override fun onIngredientClick(ingredient: Ingredient) {
        findNavController().navigate(R.id.action_ingredientFragment_to_ingredientDetailFragment)

        //pass data to detail fragment
        setFragmentResult("requestName", bundleOf("name" to ingredient.ingredientName))
        setFragmentResult(
            "requestDate", bundleOf(
                "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ingredient.expiryDate)
            )
        )
        setFragmentResult("requestId", bundleOf("id" to ingredient.ingredientId))
        setFragmentResult("requestImage", bundleOf("image" to ingredient.ingredientImage))
        setFragmentResult("requestCategory", bundleOf("category" to ingredient.ingredientCategory))
        disableBtmNav()
    }

    private fun isMorning(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in 0..11 // Assuming morning is between 6 AM and 11 AM
    }

    private fun isAfternoon(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in 12..14 // Assuming afternoon is between 12 PM and 2 PM
    }

    private fun isEvening(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in 15..23 // Assuming evening is between 3 PM and 11 PM
    }

    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
