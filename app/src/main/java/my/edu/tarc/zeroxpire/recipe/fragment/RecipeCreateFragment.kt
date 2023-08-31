package my.edu.tarc.zeroxpire.recipe.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.adapter.InstructionRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.RecipeDetailsViewModel
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.*

class RecipeCreateFragment : Fragment(), IngredientClickListener {

    private lateinit var currentView: View
    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    private var recipe = Recipe()

    private val recipeDetailsViewModel = RecipeDetailsViewModel()

    private lateinit var titleTextInputEditText: TextInputEditText
    private lateinit var noteTextInputEditText: TextInputEditText
    private lateinit var recipeImageView: ImageView
    private lateinit var upBtn: ImageView
    private lateinit var saveImageView: ImageView
    private lateinit var addIngredientDialogBtn: Button
    private lateinit var createBtn: Button
    private lateinit var instructionRecyclerView: RecyclerView

    private lateinit var instructionRecyclerViewAdapter: InstructionRecyclerViewAdapter

    private val instructionsArrayList = ArrayList<String>()

    private lateinit var fileUri: Uri

    private lateinit var dragHelper: ItemTouchHelper
    private lateinit var swipeHelper: ItemTouchHelper


    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetRecyclerView: RecyclerView
    private lateinit var ingredientRecyclerView: RecyclerView

    private var selectedIngredients: MutableList<Ingredient> = mutableListOf()
    private var selectedIngredientsTemporary: MutableList<Ingredient> = mutableListOf()
    private var getFromStoredIngredients: MutableList<Ingredient> = mutableListOf()

    private lateinit var bottomSheetIngredientAdapter: IngredientAdapter
    private lateinit var selectedIngredientAdapter: IngredientAdapter

    private val goalViewModel : GoalViewModel by activityViewModels()
    private val ingredientViewModel: IngredientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val currentContext = this
        currentView = inflater.inflate(R.layout.fragment_recipe_create, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()

        val args: RecipeCreateFragmentArgs by navArgs()
        recipe.recipeID = args.recipeID.toInt()

        Log.d("test","recipeid: ${recipe.recipeID}")

        titleTextInputEditText = currentView.findViewById(R.id.titleTextInputEditText)
        noteTextInputEditText = currentView.findViewById(R.id.noteTextInputEditText)
        recipeImageView = currentView.findViewById(R.id.recipeImageView)
        saveImageView = currentView.findViewById(R.id.saveImageView)
        upBtn = currentView.findViewById(R.id.upBtn)
        createBtn = currentView.findViewById(R.id.createBtn)
        addIngredientDialogBtn = currentView.findViewById(R.id.addIngredientDialogBtn)
        instructionRecyclerView = currentView.findViewById(R.id.instructionRecyclerView)

        fileUri = Uri.EMPTY

        selectedIngredientAdapter = IngredientAdapter(object : IngredientClickListener {
            override fun onIngredientClick(ingredient: Ingredient) {
                // Do nothing here, as this is a dummy click listener
            }
        }, goalViewModel)
        bottomSheetIngredientAdapter = IngredientAdapter(this, goalViewModel)

        selectedIngredients.clear()
        selectedIngredientsTemporary.clear()
        getFromStoredIngredients.clear()

        getFromStoredIngredients =  ingredientViewModel.ingredientList.value as MutableList<Ingredient>

        Log.d("recipeID create", "${recipe.recipeID}")
        if (recipe.recipeID >= 0) {
            recipeDetailsViewModel.getRecipeById(userID, recipe.recipeID.toString(), currentView) {
                recipe = it

                Picasso.get().load(recipe.imageLink)
                    .into(currentView.findViewById<ImageView>(R.id.recipeImageView))

                titleTextInputEditText.setText(recipe.title)

                Log.d("getFromStoredIngredients", "$getFromStoredIngredients")

                val tempArr = ArrayList<Ingredient>()
                recipe.ingredientIDArrayList.forEach {
                    getFromStoredIngredients.map { ingredient ->
                        if (ingredient.ingredientId == it.toInt()) {
                            selectedIngredients.add(ingredient)
                            tempArr.add(ingredient)
                        }
                    }
                }

                selectedIngredientsTemporary.addAll(selectedIngredients)
                selectedIngredientAdapter.setIngredient(selectedIngredients)


                CoroutineScope(Dispatchers.IO).launch {
                    val httpClient = OkHttpClient()
                    val request = Request.Builder().url(recipe.instructionsLink).build()
                    val response = httpClient.newCall(request).execute()
                    val instructions = response.body?.string().toString()
                    val reader = BufferedReader(StringReader(instructions))

                    withContext(Dispatchers.Main) {

                        reader.forEachLine {
                            instructionsArrayList.add(it)
                        }
                    }

                    Log.d("move instructions", "done")


                    activity!!.runOnUiThread {
                        instructionRecyclerView.layoutManager = LinearLayoutManager(currentView.context)

                        instructionRecyclerViewAdapter = InstructionRecyclerViewAdapter(currentContext, instructionsArrayList)
                        instructionRecyclerView.adapter = instructionRecyclerViewAdapter
                        setUpSwipeHelper()
                        setupDragHelper()
                    }
                }


                noteTextInputEditText.setText(recipe.note)
            }
        }else {
            instructionsArrayList.add("")

            instructionRecyclerView.layoutManager = LinearLayoutManager(currentView.context)

            instructionRecyclerViewAdapter = InstructionRecyclerViewAdapter(currentContext, instructionsArrayList)
            instructionRecyclerView.adapter = instructionRecyclerViewAdapter
            setUpSwipeHelper()
            setupDragHelper()
        }

        addIngredientDialogBtn.setOnClickListener {
            showBottomSheetDialog(bottomSheetIngredientAdapter)
        }

        upBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        recipeImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 1)
        }

        selectedIngredientRecyclerReview(selectedIngredientAdapter)

        saveImageView.setOnClickListener {
            createOrEditRecipe()
        }

        createBtn.setOnClickListener {
            createOrEditRecipe()
        }

        return currentView
    }


    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data!!
            Log.d("select image" ,"$fileUri")
            try {
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, fileUri)
                recipeImageView.setImageBitmap(bitmap)
                recipeImageView.setPadding(0, 0, 0, 0)
                recipeImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startDragging(holder: RecyclerView.ViewHolder) {
        dragHelper.startDrag(holder)
    }

    private fun setUpSwipeHelper() {
        swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val instructionTIED = viewHolder.itemView.findViewById<TextInputEditText>(R.id.instructionTextInputEditText)
                instructionTIED.clearFocus()

                //save text for undo
                val text = instructionRecyclerViewAdapter.instructionArrayList[pos]

                //remove from arraylist and update adapter
                instructionsArrayList.removeAt(pos)
                if (instructionsArrayList.isEmpty()) {
                    instructionsArrayList.add("")
                }
                val size = instructionsArrayList.size
                instructionRecyclerViewAdapter.notifyItemRemoved(pos)
                instructionRecyclerViewAdapter.notifyItemRangeChanged(pos, size)

                val snackBar = Snackbar.make(currentView, "Removed step $pos", Snackbar.LENGTH_SHORT)
                snackBar.setAction("UNDO",
                    UndoListener {
                        instructionRecyclerViewAdapter.addInstruction(pos, text)
                    }
                )
                snackBar.show()

            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                ).addBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.secondaryColor
                    )
                ).addActionIcon(R.drawable.baseline_delete_24).create().decorate()
                super.onChildDraw(
                    canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }
        })
        swipeHelper.attachToRecyclerView(instructionRecyclerView)
    }

    private fun setupDragHelper() {
        dragHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                viewHolder.itemView.elevation = 16F

                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                Collections.swap(instructionsArrayList, from, to)
                instructionRecyclerViewAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder?.itemView?.elevation = 0F

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    var i = 0
                    instructionRecyclerView.forEach {
                        val instructionTIED = it.findViewById<TextInputEditText>(R.id.instructionTextInputEditText)
                        instructionsArrayList[i] = instructionTIED.text.toString()
                        i++
                    }
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        })

        dragHelper.attachToRecyclerView(instructionRecyclerView)
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
            Log.d("minus",getFromStoredIngredients.minus(selectedIngredients).toString())
            bottomSheetDialog.setOnDismissListener {
                selectedIngredientsTemporary.clear()
                Log.d("minus",getFromStoredIngredients.minus(selectedIngredients).toString())

            }
            if(selectedIngredients.isNotEmpty()){
                Log.d("Selected is not empty", selectedIngredients.size.toString())
                currentView.findViewById<ConstraintLayout>(R.id.noIngredientHasRecordedLayout).isGone = true
                ingredientRecyclerView.isGone = false

                currentView.findViewById<TextView>(R.id.numOfSelectedIngredientsTextView).text = "Total: ${selectedIngredients.size} ingredient"
            }
            else {
                Log.d("Selected is empty", selectedIngredients.size.toString())
                currentView.findViewById<ConstraintLayout>(R.id.noIngredientHasRecordedLayout).isGone = false
                ingredientRecyclerView.isGone = true

                currentView.findViewById<TextView>(R.id.numOfSelectedIngredientsTextView).visibility = View.INVISIBLE
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

            // Check if the ingredient is not already in the selectedIngredientsTemporary list
            if (!selectedIngredientsTemporary.contains(ingredient)) {
                // Change the background color of the clicked item to the selected color
                clickedItemView?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.btnColor))
                clickedItemView?.tag = true

                // Select the ingredient
                selectedIngredientsTemporary.add(ingredient)
            } else {
                // If the ingredient is already in the list, remove it to toggle the selection
                clickedItemView?.setBackgroundColor(Color.WHITE)
                clickedItemView?.tag = false
                selectedIngredientsTemporary.remove(ingredient)
            }
//            }
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

    private fun selectedIngredientRecyclerReview(adapter: IngredientAdapter){
        ingredientRecyclerView = currentView.findViewById(R.id.ingredientRecyclerView)
        ingredientRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        ingredientRecyclerView.adapter = adapter

        adapter.setIngredient(selectedIngredients)
        deleteSelectedIngredient(adapter, ingredientRecyclerView)
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
                        val position = viewHolder.bindingAdapterPosition
                        selectedIngredients.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        dialog.dismiss()
                        if (selectedIngredients.isEmpty()) {
                            currentView.findViewById<ConstraintLayout>(R.id.noIngredientHasRecordedLayout).isGone = false
                            ingredientRecyclerView.isGone = true

                            currentView.findViewById<TextView>(R.id.numOfSelectedIngredientsTextView).visibility = View.INVISIBLE
                        } else {
                            currentView.findViewById<ConstraintLayout>(R.id.noIngredientHasRecordedLayout).isGone = true
                            ingredientRecyclerView.isGone = false

                            currentView.findViewById<TextView>(R.id.numOfSelectedIngredientsTextView).text = "Total: ${selectedIngredients.size} ingredient"
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

    private fun createOrEditRecipe() {
        val instructionArrayList = ArrayList<String>()

        instructionRecyclerView.forEach {
            val instructionTextInputEditText = it.findViewById<TextInputEditText>(R.id.instructionTextInputEditText)
            instructionArrayList.add(instructionTextInputEditText.text.toString())
        }

        val recipeTemp = Recipe(
            title = titleTextInputEditText.text.toString(),
            note = noteTextInputEditText.text.toString(),
        )

        if (recipe.recipeID > 0) {
            recipeTemp.recipeID = recipe.recipeID
            if (fileUri != Uri.EMPTY) {
                recipeDetailsViewModel.editRecipe(
                    ingredientsMutableList = selectedIngredients,
                    instructionsArrayList = instructionsArrayList,
                    recipe = recipeTemp,
                    imageUri = fileUri,
                    view = currentView
                ) {
                    Snackbar.make(currentView, "Recipe edited successfully", Snackbar.LENGTH_SHORT)
                        .show()
                    findNavController().popBackStack()
                }
            }else {
                recipeTemp.imageLink = recipe.imageLink
                recipeDetailsViewModel.editRecipeWithoutImage(
                    ingredientsMutableList = selectedIngredients,
                    instructionsArrayList = instructionsArrayList,
                    recipe = recipeTemp,
                    oldTitle = recipe.title,
                    view = currentView
                ) {
                    Snackbar.make(currentView, "Recipe edited successfully", Snackbar.LENGTH_SHORT)
                        .show()
                    findNavController().popBackStack()
                }
            }
        }else {
            recipeDetailsViewModel.createRecipe(
                userId = userID,
                ingredientsMutableList = selectedIngredients,
                instructionsArrayList = instructionsArrayList,
                recipe = recipeTemp,
                imageUri = fileUri,
                view = currentView
            ) {
                Snackbar.make(currentView, "Recipe created successfully", Snackbar.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            }
        }
    }

    class UndoListener(
        private val callback: (Boolean) -> Unit

    ) : View.OnClickListener {
        override fun onClick(v: View) {
            callback(true)
        }
    }
}