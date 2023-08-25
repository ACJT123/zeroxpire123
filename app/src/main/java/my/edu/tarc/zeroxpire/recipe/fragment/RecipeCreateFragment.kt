package my.edu.tarc.zeroxpire.recipe.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import java.io.File
import java.net.UnknownHostException
import java.util.*


class RecipeCreateFragment : Fragment(), IngredientClickListener {
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

    private val instructionStepsArrayList = ArrayList<TextView>()
    private val instructionDetailsArrayList = ArrayList<EditText>()
    private val instructionDetailsLinearLayoutArrayList = ArrayList<LinearLayout>()
    private var numInstructions = 0

    private var firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference
    private lateinit var auth: FirebaseAuth

    private lateinit var currentView : View

    private val ingredientsCheckBoxArrayList = ArrayList<CheckBox>()

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetRecyclerView: RecyclerView
    private val goalViewModel : GoalViewModel by activityViewModels()
    private var selectedIngredientsTemporary: MutableList<Ingredient> = mutableListOf()
    private var selectedIngredients: MutableList<Ingredient> = mutableListOf()
    private var getFromStoredIngredients: MutableList<Ingredient> = mutableListOf()
    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private lateinit var selectedIngredientAdapter: IngredientAdapter
    private lateinit var bottomSheetIngredientAdapter: IngredientAdapter

    private lateinit var fileUri: Uri

    private lateinit var userID : String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_recipe_create, container, false)
        auth = FirebaseAuth.getInstance()

        selectedIngredientAdapter = IngredientAdapter(object : IngredientClickListener {
            override fun onIngredientClick(ingredient: Ingredient) {
                // Do nothing here, as this is a dummy click listener
            }
        }, goalViewModel)

        userID = auth.currentUser?.uid.toString()

        // instantiation
        fileUri = Uri.EMPTY
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

        //choose image
        recipeImgImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
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
            if (fileUri == Uri.EMPTY) {
                Toast.makeText(currentView.context, "Add an image for the recipe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = recipeTitleEditText.text.toString()
            if (title.isBlank()){
                Toast.makeText(currentView.context, "Give the recipe a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedIngredients.isEmpty()) {
                Toast.makeText(currentView.context, "Select some ingredients", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //create array with instruction text
            val instructionsArray : Array<String> = Array(instructionDetailsArrayList.size) {
                    i -> instructionDetailsArrayList[i].text.toString()
            }

            //create text file with ingredients at temp.txt
            val instructionsPathString = "recipeInstructions/${recipeTitleEditText.text}.txt"
            val instructionsUploadTask = storeTxtToFireBase(instructionsArray, instructionsPathString)

            val imagePathString = "recipeImage/${recipeTitleEditText.text}.jpg"
            val imageUploadTask = storeImageToFireBase(imagePathString)

            instructionsUploadTask.addOnSuccessListener {
                firebaseStorageReference.child(instructionsPathString).downloadUrl.addOnSuccessListener {instructionsDownloadUrl ->
                    imageUploadTask.addOnSuccessListener {
                        firebaseStorageReference.child(imagePathString).downloadUrl.addOnSuccessListener { imageDownloadUrl ->
                            val instructionsAccessToken = cleanedAccessToken(instructionsDownloadUrl.toString(), title, "txt")

                            val imageAccessToken = cleanedAccessToken(imageDownloadUrl.toString(), title, "jpg")

                            //get note
                            var note = noteEditText.text.toString()
                            if (note.isBlank()){
                                note = ""
                            }

                            //upload to webHost when everything is ready
                            storeRecipeToDatabase(title, instructionsAccessToken, imageAccessToken, note)
                        }
                    }
                }
            }
        }

        createRecipeUpBtnImageView.setOnClickListener {
            findNavController().popBackStack()
        }

        return currentView
    }

    private fun storeTxtToFireBase(
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

    private fun storeImageToFireBase(
        pathString: String
    ): UploadTask {
        val storageRef = firebaseStorageReference.child(pathString)
        var uploadTask: UploadTask

        fileUri.let {uri ->
            uploadTask = storageRef.putFile(uri)
        }

        //store to firebase
        return uploadTask
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

    private fun createNewLinearLayout(left: Int = 0, right: Int = 0, top: Int = 0, bottom: Int = 0): LinearLayout {
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

    private fun storeRecipeToDatabase(title: String, instructionsLink: String, imageLink: String, note: String) {
        //create string including the arguments
        //to create ingredients array
        val ingredients = StringBuilder()
        selectedIngredients.forEach {
            ingredients.append("&ingredientIDArr[]=${it.ingredientId}")
        }

        val url = StringBuilder()
            .append(getString(R.string.url_server))
            .append(getString(R.string.recipeCreateURL))
            .append("?title=")
            .append(title)
            .append("&instructionsLink=")
            .append(instructionsLink)
            .append("&imageLink=")
            .append(imageLink)
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
                            findNavController().popBackStack()
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
                Toast.makeText(currentView.context, "Failed to upload recipe, please try again later", Toast.LENGTH_SHORT).show()
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
            if(selectedIngredients.isNotEmpty()){
                Log.d("Selected is not empty", selectedIngredients.size.toString())
                displayIngredients()
            }
            else {
                Log.d("Selected is empty", selectedIngredients.size.toString())

            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data!!
            try {
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, fileUri)
                recipeImgImageView.setImageBitmap(bitmap)
                recipeImgImageView.setPadding(0, 0, 0, 0)
                recipeImgImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun cleanedAccessToken(accessToken: String, title: String, fileType: String): String {
        val tokenLength = 36
        return java.lang.StringBuilder()
            .append(title)
            .append(".$fileType?alt=media%26token=")
            .append(accessToken.substring(accessToken.length-tokenLength, accessToken.length))
            .toString()
    }

    private fun displayIngredients() {
        val recipeDetailsIngredientsLinearLayout = currentView.findViewById<LinearLayout>(R.id.ingredientsLinearLayout)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                selectedIngredients.forEach {
                    val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    layoutParams.setMargins(80, 0, 80, 0)
                    val newCheckBox = createNewCheckBox(it.ingredientName, layoutParams=layoutParams)
                    ingredientsCheckBoxArrayList.add(newCheckBox)
                    recipeDetailsIngredientsLinearLayout.addView(newCheckBox)
                }
            }
        }
    }

    private fun createNewCheckBox(text: String = "", typeface: Int = Typeface.NORMAL, layoutParams: LayoutParams): CheckBox {
        val newCheckBox = CheckBox(currentView.context)

        //apply attributes
        newCheckBox.text = text
        newCheckBox.textSize = 24F
        newCheckBox.setTypeface(null, typeface)
        newCheckBox.isVisible = true
        newCheckBox.layoutParams = layoutParams

        return newCheckBox
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
