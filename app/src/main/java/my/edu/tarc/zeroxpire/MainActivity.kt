package my.edu.tarc.zeroxpire

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.impl.Observable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.databinding.ActivityMainBinding
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.view.ingredient.ScannerFragment
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel

class MainActivity : AppCompatActivity(), IngredientClickListener{
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    private lateinit var networkConnection: NetworkConnection
    private lateinit var networkConnectionObserver: Observable.Observer<Boolean>

    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var goalViewModel: GoalViewModel

    private lateinit var ingredientAdapter: IngredientAdapter

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var recyclerView: RecyclerView

    private val selectedIngredients: MutableList<Ingredient> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Internet connection
        //TODO: cannot be done currently due to the lead of navigation state lost


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()


        //initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)
        goalViewModel = ViewModelProvider(this).get(GoalViewModel::class.java)

        // Initialize IngredientAdapter
        ingredientAdapter = IngredientAdapter(this, goalViewModel)
        // Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        // Find reference to bottom navigation view
        val navView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Hook your navigation controller to bottom navigation view
        navView.setupWithNavController(navController)

        navView.background = null
        navView.menu.getItem(2).isEnabled = false

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.ingredientFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.barcode_scan_icon_137911)
                    binding.fab.setOnClickListener {
                        navController.clearBackStack(R.id.ingredientFragment)
                        navController.navigate(R.id.action_ingredientFragment_to_scannerFragment)
                        disableBtmNav()
                        requestCameraAndStartScanner()
                    }
                }
                R.id.goalFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.baseline_add_24)
                    binding.fab.setOnClickListener{
//                        showBottomSheetDialog(ingredientAdapter)
                        navController.navigate(R.id.action_goalFragment_to_createGoalFragment)
                        disableBtmNav()
                    }
                }
                R.id.recipeFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.baseline_book_24)
                    binding.fab.setOnClickListener{
                        navController.navigate(R.id.action_recipeFragment_to_createRecipe)
                        disableBtmNav()
                    }
                }
                R.id.profileFragment -> {
                    if(auth.currentUser == null){
                        Toast.makeText(this, "No user", Toast.LENGTH_SHORT).show()
                        disableBtmNav()
                        navController.navigate(R.id.loginFragment)
                        navController.clearBackStack(R.id.loginFragment)
                    }
//                    else{
//                        navController.navigate(R.id.)
//                        Toast.makeText(this, "User", Toast.LENGTH_SHORT).show()
//                    }
//                    disableBtmNav()
//                    binding.bottomAppBar.visibility = View.VISIBLE
//                    binding.bottomAppBar.fabCradleMargin = -50f
                }
            }
        }
    }

//    fun showBottomSheetDialog(ingredientAdapter: IngredientAdapter) {
//        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
//        val size = ingredientViewModel.ingredientList.value?.size
//        val noRecordedTextView = bottomSheetView.findViewById<ConstraintLayout>(R.id.noIngredientHasRecordedDialog)
//        val searchView = bottomSheetView.findViewById<SearchView>(R.id.ingredientSearchViewForGoal)
//        val selectTextView = bottomSheetView.findViewById<TextView>(R.id.selectTV)
//        Log.d("Size: ", size.toString())
//
//        if (size != null) {
//            if (size == 0) {
//                searchView.visibility = View.GONE
//                noRecordedTextView.visibility = View.VISIBLE
//                selectTextView.visibility = View.INVISIBLE
//            } else {
//                searchView.visibility = View.VISIBLE
//                noRecordedTextView.visibility = View.INVISIBLE
//                selectTextView.visibility = View.VISIBLE
//            }
//        }
//
//        bottomSheetDialog = BottomSheetDialog(this)
//        bottomSheetDialog.setContentView(bottomSheetView)
//        bottomSheetDialog.show()
//
//        recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recyclerviewNumIngredientChoosed)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = this.ingredientAdapter
//
//        this.ingredientAdapter.setIngredient(ingredientViewModel.ingredientList.value ?: emptyList())
//
//        val nextBtn = bottomSheetView.findViewById<Button>(R.id.nextBtn)
//
//        nextBtn.isEnabled = false
//
//        nextBtn.setOnClickListener {
//            // Create a new Bundle to pass data to the next fragment
//            val bundle = Bundle()
//
//            // Create an ArrayList to store the ingredient IDs
//            val ingredientIds = ArrayList<String>()
//
//            for (i in 0 until selectedIngredients.size) {
//                // Add each ingredient ID to the ArrayList
//                ingredientIds.add(selectedIngredients[i].ingredientId.toString())
//            }
//
//            // Put the ArrayList of ingredient IDs into the Bundle
//            bundle.putStringArrayList("ingredientIds", ingredientIds)
//
//            navController.navigate(R.id.action_goalFragment_to_createGoalFragment, bundle)
//
//            // Clear the selectedIngredients list after navigation
//            selectedIngredients.clear()
//
//            bottomSheetDialog.dismiss()
//            disableBtmNav()
//
//        }
////
////        if(selectedIngredients.isEmpty()){
////            nextBtn.isEnabled = false
////        }
////        if(selectedIngredients.isNotEmpty()){
////            nextBtn.isEnabled = true
////        }
//
//
//        searchIngredient(ingredientAdapter)
//
//    }
//
//    private fun searchIngredient(adapter: IngredientAdapter) {
//        val searchView = bottomSheetView.findViewById<SearchView>(R.id.ingredientSearchViewForGoal)
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String): Boolean {
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String): Boolean {
//                val filteredIngredients = ingredientViewModel.ingredientList.value?.filter { ingredient ->
//                    ingredient.ingredientName.contains(newText, ignoreCase = true)
//                }
//
//                adapter.setIngredient(filteredIngredients ?: emptyList())
//
//                return true
//            }
//        })
//    }


//    override fun onResume() {
//        super.onResume()
//        Toast.makeText(this, "On resuming", Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onRestart() {
//        super.onRestart()
//        Toast.makeText(this, "On restarting", Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onStart() {
//        super.onStart()
//        Toast.makeText(this, "On starting", Toast.LENGTH_SHORT).show()
////        // Check if user is signed in (non-null) and update UI accordingly.
////        val currentUser = auth.currentUser
////        if (currentUser != null) {
////            Toast.makeText(this, "Already Signed In", Toast.LENGTH_SHORT).show()
////        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        Toast.makeText(this, "On pausing", Toast.LENGTH_SHORT).show()
//    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "On destroying", Toast.LENGTH_SHORT).show()
    }

    private val cameraPermission = android.Manifest.permission.CAMERA
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanner()
        }
    }

    private fun requestCameraAndStartScanner(){
        if(isPermissionGranted(cameraPermission)){
            startScanner()
        }
        else{
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        when{
            shouldShowRequestPermissionRationale(cameraPermission) ->{
                cameraPermissionRequest{
                    openPermissionSetting()
                }
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }

    private fun startScanner(){
        ScannerFragment.startScanner(this){
        }
    }

    private fun disableBtmNav() {
        binding.bottomAppBar.visibility = View.INVISIBLE
        binding.fab.visibility = View.INVISIBLE
    }

    private fun enableBtmNav() {
        binding.bottomAppBar.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ResourceAsColor")
    override fun onIngredientClick(ingredient: Ingredient) {
        val layoutManager = recyclerView.layoutManager

        if (layoutManager is LinearLayoutManager) {
            val clickedItemPosition = ingredientAdapter.getPosition(ingredient)
            val clickedItemView = layoutManager.findViewByPosition(clickedItemPosition)

            val isItemSelected = clickedItemView?.tag as? Boolean ?: false

            if (isItemSelected) {
                // Reset the background color of the clicked item to the default color
                clickedItemView?.setBackgroundColor(Color.WHITE)
                clickedItemView?.tag = false

                // Deselect the ingredient if it was selected
                selectedIngredients.remove(ingredient)
            } else {
                // Change the background color of the clicked item to the selected color
                clickedItemView?.setBackgroundColor(ContextCompat.getColor(this, R.color.btnColor))
                clickedItemView?.tag = true

                // Select the ingredient if it was deselected
                selectedIngredients.add(ingredient)
            }
        }

        val nextBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        nextBtn.isEnabled = selectedIngredients.isNotEmpty()

        Log.d("Ingredients", selectedIngredients.toString())
    }







}