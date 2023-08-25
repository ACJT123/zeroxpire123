package my.edu.tarc.zeroxpire.recipe.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.widget.ImageViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.*
import my.edu.tarc.zeroxpire.recipe.adapter.CommentRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.BookmarkViewModel
import my.edu.tarc.zeroxpire.recipe.viewModel.CommentViewModel
import my.edu.tarc.zeroxpire.recipe.viewModel.RecipeDetailsViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader


class RecipeDetailsFragment : Fragment() {
    private val requestCode = 1232
    private var numSteps = 1

    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String


    private lateinit var instructions : String
    private lateinit var currentView: View
    private var recipe = Recipe()
    private var utilities = Utilities()

    private lateinit var editImageView: ImageView
    private lateinit var deleteImageView: ImageView
    private lateinit var bookmarkImageView: ImageView
    private lateinit var printImageView: ImageView
    private lateinit var saveImageView: ImageView
    private lateinit var addIngredientImageView: ImageView
    private lateinit var addInstructionsImageView: ImageView
    private lateinit var closeBtn: ImageView
    private lateinit var recipeDetailsLinearLayout: LinearLayout
    private lateinit var recipeDetailsInstructionsLinearLayout: LinearLayout
    private lateinit var appBarEditText: EditText
    private lateinit var noteEditText: EditText
    private lateinit var noCommentTextView: TextView
    private lateinit var commentRecyclerView: RecyclerView

    //drawer
    private lateinit var commentEditText: EditText
    private lateinit var sendImageView: ImageView

    private val ingredientsCheckBoxArrayList = ArrayList<CheckBox>()
    private val instructionsEditTextArrayList = ArrayList<EditText>()
    private val linearLayoutArrayList = ArrayList<LinearLayout>()

    private val recipeDetailsViewModel = RecipeDetailsViewModel()

    private lateinit var drawerLayout: DrawerLayout
    private val commentViewModel = CommentViewModel()
    private var commentArrayList = ArrayList<Comment>()

    private val bookmarkViewModel = BookmarkViewModel()

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe_details, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()
        disableBtmNav()

        initView()

        commentRecyclerView.setHasFixedSize(true)
        commentRecyclerView.layoutManager = LinearLayoutManager(currentView.context)
        commentRecyclerView.addItemDecoration(CommentRecyclerViewItemDecoration(50))

        val upBtn = currentView.findViewById<ImageView>(R.id.upBtn)
        val appBar = currentView.findViewById<AppBarLayout>(R.id.appBar)

        val args: RecipeDetailsFragmentArgs by navArgs()
        val recipeID = args.recipeID

        appBarEditText.inputType = InputType.TYPE_NULL
        noteEditText.inputType = InputType.TYPE_NULL

        recipeDetailsViewModel.getRecipeById(userID, recipeID, currentView) {
            recipe = it

            if (recipe.authorID == userID) {
                deleteImageView.isGone = false
                editImageView.isGone = false
            }else {
                deleteImageView.isGone = true
                editImageView.isGone = true
            }

            if (recipe.isBookmarked) {
                bookmarkImageView.setImageResource(R.drawable.baseline_favorite_24)
                ImageViewCompat.setImageTintList(bookmarkImageView, ColorStateList.valueOf(
                    ContextCompat.getColor(currentView.context, R.color.favoriteBtnColor)))
            }else {
                bookmarkImageView.setImageResource(R.drawable.baseline_favorite_border_24)
                ImageViewCompat.setImageTintList(bookmarkImageView, ColorStateList.valueOf(
                    ContextCompat.getColor(currentView.context, R.color.favoriteBtnColor)))
            }

            displayIngredients()
            displayInstructions()

            //set title and note
            appBarEditText.setText(recipe.title)
            noteEditText.setText(recipe.note)

            //display image
            Picasso.get().load(recipe.imageLink).into(currentView.findViewById<ImageView>(R.id.recipeDescImageView))
        }

        commentViewModel.getComments(recipeID.toInt(), currentView) {
            commentArrayList = it
            if (commentArrayList.isEmpty()) {
                commentRecyclerView.isGone = true
                noCommentTextView.isGone = false
            }else {
                commentRecyclerView.isGone = false
                noCommentTextView.isGone = true
                commentRecyclerView.adapter = CommentRecyclerViewAdapter(commentArrayList)
            }
        }


        //top bar back button
        upBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        printImageView.setOnClickListener {
            //setup layout
            askPermissions()
            appBar.isGone = true

            val recipeDetailsTitleTextView = currentView.findViewById<TextView>(R.id.recipeDetailsTitleTextView)
            recipeDetailsTitleTextView.text = recipe.title
            recipeDetailsTitleTextView.isGone = false

            val displayMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                currentView.display.getRealMetrics(displayMetrics)
            } else activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

            val fileName = "${recipe.title} recipe.pdf"

            recipeDetailsViewModel.convertXmlToPdf(currentView, displayMetrics, fileName)

            //return to normal layout
            appBar.isGone = false
            recipeDetailsTitleTextView.isGone = true
        }

        bookmarkImageView.setOnClickListener {
            if (recipe.isBookmarked) {
                bookmarkImageView.setImageResource(R.drawable.baseline_favorite_border_24)
                ImageViewCompat.setImageTintList(bookmarkImageView, ColorStateList.valueOf(
                    ContextCompat.getColor(currentView.context, R.color.favoriteBtnColor)))

                bookmarkViewModel.removeFromBookmarks(userID, recipeID, currentView) {

                    if (it) {
                        recipe.isBookmarked = false

                        // display result and undo button
                        val snackBar = Snackbar.make(currentView, "Removed from bookmarks", Snackbar.LENGTH_SHORT)
                        snackBar.setAction("UNDO",
                            UndoListener {
                                bookmarkViewModel.addToBookmark(userID, recipeID, currentView) {
                                    bookmarkImageView.setImageResource(R.drawable.baseline_favorite_24)
                                    ImageViewCompat.setImageTintList(bookmarkImageView, ColorStateList.valueOf(
                                        ContextCompat.getColor(currentView.context, R.color.favoriteBtnColor)))
                                    recipe.isBookmarked = true
                                }

                            }
                        )
                        snackBar.show()
                    }

                }
            }else {
                recipe.isBookmarked = true
                bookmarkImageView.setImageResource(R.drawable.baseline_favorite_24)
                ImageViewCompat.setImageTintList(bookmarkImageView, ColorStateList.valueOf(
                    ContextCompat.getColor(currentView.context, R.color.favoriteBtnColor)))
                bookmarkViewModel.addToBookmark(userID, recipeID, currentView) {
                    Snackbar.make(currentView, "Bookmarked successfully", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        editImageView.setOnClickListener {
            // --- setup edit layout
            // appbar icons
            printImageView.isGone = true
            editImageView.isGone = true
            deleteImageView.isGone = true
            bookmarkImageView.isGone = true
            saveImageView.isGone = false

            // add ingredient and instruction icon
            addIngredientImageView.isGone = false
            addInstructionsImageView.isGone = false

            // appbar title and note
            appBarEditText.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
            noteEditText.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)

            // instructions
            instructionsEditTextArrayList.forEach {
                it.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
            }

            val layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(24,0,24,0)


            //TODO: bottomsheet

            addInstructionsImageView.setOnClickListener {
                createNewStep(hint = getString(R.string.recipe_description), editable = true)
            }

            // --- upload edited recipe
            //TODO: ingredientId arraylist give value
            val ingredientIDArrayList = ArrayList<Int>()

            recipe.title = appBarEditText.text.toString()
            recipe.note = noteEditText.text.toString()
            recipe.ingredientNames = ""
            recipe.ingredientNamesArrayList = ArrayList()
//            recipe.imageLink =

            saveImageView.setOnClickListener {
                recipeDetailsViewModel.editRecipe(recipe, ingredientIDArrayList, currentView) {
                    if (it) {
                        Toast.makeText(currentView.context, "recipe edited successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        deleteImageView.setOnClickListener {
            val rootView = activity?.findViewById<View>(android.R.id.content)
            recipeDetailsViewModel.deleteRecipe(recipeID, 1, currentView) {
                if (it) {
                    val snackBar = Snackbar.make(currentView, "Deleted recipe successfully", Snackbar.LENGTH_SHORT)
                    snackBar.setAction("UNDO",
                        UndoListener {
                            recipeDetailsViewModel.deleteRecipe(recipeID, 0, currentView) {
                                if (rootView != null) {
                                    Snackbar.make(rootView, "Recipe restored successfully", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    snackBar.show()
                    findNavController().popBackStack()
                }else {
                    Snackbar.make(currentView, "Failed to delete recipe", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        // drawer layout
        recipeDetailsLinearLayout.setOnTouchListener(object : OnSwipeTouchListener(currentView.context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
                drawerLayout.openDrawer(GravityCompat.END)
            }
        })

        sendImageView.setOnClickListener {
            //TODO: reply to
            val comment = Comment(
                recipeID=recipeID.toInt(),
                userID=userID,
                comment=commentEditText.text.toString(),
                replyTo = ""
            )
            commentViewModel.createComment(comment, currentView) {
            }
        }

        closeBtn.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        return currentView
    }


    private fun displayIngredients() {
        val recipeDetailsIngredientsLinearLayout = currentView.findViewById<LinearLayout>(R.id.recipeDetailsIngredientsLinearLayout)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                recipe.ingredientNamesArrayList.forEach {
                    val newCheckBox = utilities.createNewCheckBox(currentView, it)
                    ingredientsCheckBoxArrayList.add(newCheckBox)
                    recipeDetailsIngredientsLinearLayout.addView(newCheckBox)
                }
            }
        }
    }

    private fun displayInstructions() {
        CoroutineScope(Dispatchers.IO).launch {
            val httpClient = OkHttpClient()
            val request = Request.Builder().url(recipe.instructionsLink).build()
            val response = httpClient.newCall(request).execute()
            instructions = response.body?.string().toString()
            val reader = BufferedReader(StringReader(instructions))

            withContext(Dispatchers.Main) {
                reader.forEachLine {
                    createNewStep(it, currentView.context.getString(R.string.recipe_description), false)
                }
            }
        }
    }


    private fun askPermissions() {
        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            requestCode
        )
    }


    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun createNewStep(text: String = "", hint: String, editable: Boolean) {
        val newStepTextView = utilities.createNewTextView(currentView, "Step $numSteps: ", Typeface.BOLD)

        val newInstructionEditText = utilities.createNewEditText(currentView, text, hint)
        instructionsEditTextArrayList.add(newInstructionEditText)
        if (!editable) {
            newInstructionEditText.inputType = InputType.TYPE_NULL
        }

        val newLinearLayout = utilities.createNewLinearLayout(currentView)
        linearLayoutArrayList.add(newLinearLayout)
        newLinearLayout.addView(newStepTextView)
        newLinearLayout.addView(newInstructionEditText)

        recipeDetailsInstructionsLinearLayout.addView(newLinearLayout)
        numSteps++
    }

    private fun initView() {
        appBarEditText = currentView.findViewById(R.id.appBarEditText)
        noteEditText = currentView.findViewById(R.id.noteEditText)
        recipeDetailsLinearLayout = currentView.findViewById(R.id.recipeDetailsLinearLayout)
        recipeDetailsInstructionsLinearLayout = currentView.findViewById(R.id.recipeDetailsInstructionsLinearLayout)
        printImageView = currentView.findViewById(R.id.printImageView)
        deleteImageView = currentView.findViewById(R.id.deleteImageView)
        editImageView = currentView.findViewById(R.id.editImageView)
        bookmarkImageView = currentView.findViewById(R.id.bookmarkImageView)
        saveImageView = currentView.findViewById(R.id.saveImageView)
        addIngredientImageView = currentView.findViewById(R.id.addIngredientImageView)
        addInstructionsImageView = currentView.findViewById(R.id.addInstructionsImageView)
        drawerLayout = currentView.findViewById(R.id.drawerLayout)
        commentEditText = currentView.findViewById(R.id.commentEditText)
        noCommentTextView = currentView.findViewById(R.id.noCommentTextView)
        sendImageView = currentView.findViewById(R.id.sendImageView)
        commentRecyclerView = currentView.findViewById(R.id.commentRecyclerView)
        closeBtn = currentView.findViewById(R.id.closeBtn)
    }


    class UndoListener(
        private val callback: (Boolean) -> Unit

    ) : View.OnClickListener {
        override fun onClick(v: View) {
            callback(true)
        }
    }

}