package my.edu.tarc.zeroxpire.createRecipe

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storageMetadata
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.UnknownHostException
import java.util.*
import kotlin.collections.ArrayList


class CreateRecipe : Fragment(), IngredientClickListener {
    // declaration
    private lateinit var recipeImgImageView : ImageView
    private lateinit var addIngredientImageView : ImageView
    private lateinit var addInstructionImageView: ImageView
    private lateinit var noteEditText: EditText
    private lateinit var recipeTitleEditText: EditText
    private lateinit var createRecipeImageView: ImageView
    private lateinit var ingredientsLinearLayout: LinearLayout
    private lateinit var instructionsLinearLayout: LinearLayout
    private lateinit var createRecipeUpBtnImageView: ImageView
    private lateinit var img : Drawable

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val instructionStepsArrayList = ArrayList<TextView>()
    private val instructionDetailsArrayList = ArrayList<EditText>()
    private val instructionDetailsLinearLayoutArrayList = ArrayList<LinearLayout>()
    private var numInstructions = 0

    private var firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference
    private var firebaseDatabase: FirebaseDatabase? = FirebaseDatabase.getInstance("https://zeroxpire-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private var recipesDatabaseReference: DatabaseReference? = firebaseDatabase!!.getReference("Recipes")
    private var numRecipes : Long = -1

    private lateinit var currentView : View

    private val ingredientIDArrayList = ArrayList<Int>()
    private val ingredientNameArrayList = ArrayList<String>()

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetRecyclerView: RecyclerView
    private val goalViewModel : GoalViewModel by activityViewModels()
    private var selectedIngredientsTemporary: MutableList<Ingredient> = mutableListOf()
    private var selectedIngredients: MutableList<Ingredient> = mutableListOf()
    private lateinit var bottomSheetIngredientAdapter: IngredientAdapter
    private var getFromStoredIngredients: MutableList<Ingredient> = mutableListOf()
    private lateinit var selectedIngredientAdapter: IngredientAdapter
    private val ingredientViewModel: IngredientViewModel by activityViewModels()

    //TODO: change to get userid
    private var userID: Int = 1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_create_recipe, container, false)

        // instantiation
        spinnerAdapter = loadIngredient()
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        bottomSheetIngredientAdapter = IngredientAdapter(this, goalViewModel)

        createRecipeUpBtnImageView = currentView.findViewById(R.id.createRecipeUpBtnImageView)
        recipeImgImageView = currentView.findViewById(R.id.recipeImgImageView)
        noteEditText = currentView.findViewById(R.id.recipeNoteEditText)
        recipeTitleEditText = currentView.findViewById(R.id.recipeTitleEditText)
        addIngredientImageView = currentView.findViewById(R.id.addIngredientImageView)
        addInstructionImageView = currentView.findViewById(R.id.addInstructionImageView)
        createRecipeImageView = currentView.findViewById(R.id.createRecipeImageView)

        ingredientsLinearLayout = currentView.findViewById(R.id.ingredientsLinearLayout)
        instructionsLinearLayout = currentView.findViewById(R.id.instructionsLinearLayout)

        img = getDrawable(currentView.context, R.drawable.baseline_check_box_outline_blank_24)!!

        ingredientViewModel.ingredientList.observe(viewLifecycleOwner){ingredients->
            getFromStoredIngredients = ingredients as MutableList<Ingredient>
            Log.d("Stored ingredients", getFromStoredIngredients.toString())
        }


        //get number of recipes that exist in the database
        recipesDatabaseReference!!.child("numRecipes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                numRecipes = snapshot.value as Long
            }

            override fun onCancelled(error: DatabaseError) {
                numRecipes = -1
            }
        })

        //choose image
        recipeImgImageView.setOnClickListener {
            //TODO: get image from gallery or camera
        }

        addIngredientImageView.setOnClickListener {
            showBottomSheetDialog(bottomSheetIngredientAdapter)
        }

        //add step
        addInstructionImageView.setOnClickListener {
            //-- step textView
            //create new textView
            val newTextView = createNewTextView(getString(R.string.step, numInstructions+1))
            instructionStepsArrayList.add(newTextView)

            //-- step editText
            //create new editText
            val newEditText = createNewEditText(getString(R.string.recipe_description))
            instructionDetailsArrayList.add(newEditText)

            //create new linearLayout
            val newLinearLayout = createNewLinearLayout(80,80,0,0)
            instructionDetailsLinearLayoutArrayList.add(newLinearLayout)

            //add new textView and new editText
            //to new linearLayout
            newLinearLayout.addView(instructionStepsArrayList[numInstructions])
            newLinearLayout.addView(instructionDetailsArrayList[numInstructions])

            instructionsLinearLayout.addView(instructionDetailsLinearLayoutArrayList[numInstructions])
            numInstructions++
        }

        createRecipeImageView.setOnClickListener {
            //get title
            val title = recipeTitleEditText.text.toString()
            if (title.isBlank()){
                Toast.makeText(currentView.context, "Please give the recipe a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //TODO: if no ingredients are selected, display error msg

            //TODO: get ingredients id

            //create array with instruction text
            val instructionsArray : Array<String> = Array(instructionDetailsArrayList.size) {
                    i -> instructionDetailsArrayList[i].text.toString()
            }

            //create text file with ingredients at temp.txt
            val pathString = "recipe instructions/${recipeTitleEditText.text}.txt"
            val instructionsUploadTask = storeToFireBase(instructionsArray, pathString)

            instructionsUploadTask.addOnSuccessListener {
                firebaseStorageReference.child(pathString).downloadUrl.addOnSuccessListener {downloadUrl ->
                    val accessToken = downloadUrl.toString()
                    val tokenLength = 36
                    val passedAccessToken =
                        java.lang.StringBuilder()
                            .append(title)
                            .append(".txt?alt=media%26token=")
                            .append(accessToken.substring(accessToken.length-tokenLength, accessToken.length))
                            .toString()

                    //get note
                    var note = noteEditText.text.toString()
                    if (note.isBlank()){
                        note = ""
                    }

                    //upload to webHost when everything is ready
                    instructionsUploadTask.addOnCompleteListener{
                        storeRecipeToDatabase(title, passedAccessToken,note)
                    }
                }
            }
        }

        createRecipeUpBtnImageView.setOnClickListener {
            findNavController().popBackStack()
        }

        return currentView
    }

    private fun storeToFireBase(
        array: Array<String>,
        pathString: String
    ): UploadTask {
        //create file
        File(currentView.context.filesDir, "temp.txt").bufferedWriter().use { out ->
            array.forEach {
                out.write("${it}\n")
            }
        }

        val storageRef = firebaseStorageReference.child(pathString)
        val byte = File(view!!.context.filesDir, "temp.txt").readBytes()
        val metadata = storageMetadata {
            contentType = "text/plain"

        }

        //store to firebase
        return storageRef.putBytes(byte, metadata)
    }

    private fun loadIngredient(): ArrayAdapter<String> {
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_ingredient)
        ingredientNameArrayList.add(getString(R.string.select_ingredient))
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
                            ingredientNameArrayList.add(jsonIngredient.getString("ingredientName"))
                            ingredientIDArrayList.add(jsonIngredient.getInt("ingredientId"))
                        }
                    }
                } catch (e: UnknownHostException) {
                    Log.d("CreateRecipe", "Unknown Host: ${e.message}")
                } catch (e: Exception) {
                    Log.d("CreateRecipe", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("CreateRecipe", "Error Response: ${error.message}")
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

    private fun createNewTextView(text: String): TextView {
        val newTextView = TextView(currentView.context)

        //apply attributes
        newTextView.text = text
        newTextView.textSize = 24F
        newTextView.setTypeface(null, Typeface.BOLD)
        newTextView.isVisible = true

        return newTextView
    }

    private fun createNewEditText(hint: String): EditText {
        val newEditText = EditText(currentView.context)
        //apply attributes
        newEditText.background = null
        newEditText.gravity = Gravity.START or Gravity.TOP
        newEditText.hint = hint
        newEditText.isSingleLine = false
        newEditText.imeOptions = EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE
        newEditText.minHeight = 48
        newEditText.textSize = 24F

        return newEditText
    }

    private fun createNewLinearLayout(left: Int, right: Int, top: Int, bottom: Int): LinearLayout {
        val newLinearLayout = LinearLayout(currentView.context, null, R.style.RecipeDetails)

        //apply attributes
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT)
        layoutParams.setMargins(left,top,right,bottom)

        newLinearLayout.layoutParams = layoutParams
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        return newLinearLayout
    }

    private fun storeRecipeToDatabase(title: String, instructionsLink: String, note: String) {
        //create string including the arguments
        //to create ingredients array
        val ingredients = StringBuilder()
        for (i in 0..1){
            ingredients.append("&ingredientIDArr[]=")
            ingredients.append(i+1)
        }
        val url = StringBuilder()
            .append(getString(R.string.url_server))
            .append(getString(R.string.recipeCreateURL))
            .append("?title=")
            .append(title)
            .append("&instructionsLink=")
            .append(instructionsLink)
            .append("&note=")
            .append(note)
            .append("&author=")
            .append(userID)
            .append(ingredients)
            .toString()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        //get response
                        if (response.getBoolean("success")) {
                            Toast.makeText(currentView.context, "Successfully uploaded recipe", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(currentView.context, "Failed to upload recipe", Toast.LENGTH_SHORT).show()
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

    private fun showBottomSheetDialog(adapter: IngredientAdapter) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetRecyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        bottomSheetRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bottomSheetRecyclerView.adapter = adapter

        val addBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        addBtn.isEnabled = selectedIngredientsTemporary.isNotEmpty()

        addBtn.setOnClickListener {
            selectedIngredients = selectedIngredientsTemporary.toMutableList()
            Log.d("Temporary -> Selected", selectedIngredients.toString())
            bottomSheetDialog.dismiss()

            // Notify the selectedIngredientAdapter about the data change
            selectedIngredientAdapter.setIngredient(selectedIngredients)
            Log.d("minus",getFromStoredIngredients.minus(selectedIngredients.toSet()).toString())
            bottomSheetDialog.setOnDismissListener {
                selectedIngredientsTemporary.clear()
                Log.d("minus",getFromStoredIngredients.minus(selectedIngredients.toSet()).toString())

            }
//            if(selectedIngredients.isNotEmpty()){
//                Log.d("Selected is not empty", selectedIngredients.size.toString())
//                binding.noIngredientHasRecordedLayout.visibility = View.INVISIBLE
//                binding.numOfSelectedIngredientsTextView.text = "Total: ${selectedIngredients.size} ingredient"
//            }
//            else {
//                Log.d("Selected is empty", selectedIngredients.size.toString())
//                binding.noIngredientHasRecordedLayout.visibility = View.VISIBLE
//                binding.numOfSelectedIngredientsTextView.visibility = View.INVISIBLE
//            }
        }


//        // Remove ingredients that are already stored in selectedIngredients from getFromStoredIngredients
//        getFromStoredIngredients.removeAll(selectedIngredients)

        bottomSheetDialog.show()

        adapter.setIngredient(getFromStoredIngredients.minus(selectedIngredients.toSet()))
        adapter.notifyDataSetChanged()

    }

    override fun onIngredientClick(ingredient: Ingredient) {
        bottomSheetRecyclerView = bottomSheetView.findViewById(R.id.recyclerviewNumIngredientChoosed)
        val layoutManager = bottomSheetRecyclerView.layoutManager

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


//        //add ingredient checkbox
//        addIngredientImageView.setOnClickListener {
//            //-- ingredient editText
//            //create new editText
//            val newSpinner = createNewSpinner()
//            newSpinner.adapter = spinnerAdapter
//            ingredientsArrayList.add(newSpinner)
//
//            newSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                    val selectedItem = parent.getItemAtPosition(position).toString()
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>) {
//                    // Do nothing when nothing is selected
//                }
//            }
//            //add new spinner to ingredients linearLayout
//            ingredientsLinearLayout.addView(newSpinner)
//            numIngredients++
//        }

//    private fun createNewSpinner(): Spinner {
//        val newSpinner = Spinner(currentView.context)
//
//        //apply attributes
//        val layoutParams = LayoutParams(
//            LayoutParams.MATCH_PARENT,
//            LayoutParams.MATCH_PARENT)
//        layoutParams.setMargins(80,0 ,80,0)
//        newSpinner.layoutParams = layoutParams
//
//        return newSpinner
//    }
