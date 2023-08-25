package my.edu.tarc.zeroxpire.recipe.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.Visibility
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.RecipeRecyclerViewItemDecoration
import my.edu.tarc.zeroxpire.recipe.adapter.RecipeRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.RecipeViewModel
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel


class RecipeFragment : Fragment(), IngredientClickListener {
    // declaration
    private lateinit var bookmarksImageView: ImageView
    private lateinit var recipeSelectIngredientsTextview: TextView
    private lateinit var recipeGoogleSearchTextView: TextView
    private lateinit var recipeSearchView: SearchView
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeWebView: WebView
    private lateinit var recipeWebViewLinearLayout: LinearLayout
    private lateinit var recipeRecyclerViewLinearLayout: LinearLayout

    private lateinit var currentView: View

    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    private var recipeArrayList = ArrayList<Recipe>()
    private val recipeViewModel = RecipeViewModel()


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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()

        initView()

        recipeRecyclerViewLinearLayout.visibility = View.GONE
        recipeGoogleSearchTextView.visibility = View.GONE
        recipeWebViewLinearLayout.visibility = View.GONE


        recipeRecyclerView.setHasFixedSize(true)

        bottomSheetIngredientAdapter = IngredientAdapter(this, goalViewModel)

        val layoutManager =
            StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)

        recipeRecyclerView.layoutManager = layoutManager
        recipeRecyclerView.addItemDecoration(RecipeRecyclerViewItemDecoration(50))

        ingredientViewModel.ingredientList.observe(viewLifecycleOwner){ingredients->
            getFromStoredIngredients = ingredients as MutableList<Ingredient>
            Log.d("Stored ingredients", getFromStoredIngredients.toString())
        }

        selectedIngredientAdapter = IngredientAdapter(object : IngredientClickListener {
            override fun onIngredientClick(ingredient: Ingredient) {
                // Do nothing here, as this is a dummy click listener
            }
        }, goalViewModel)

        // recommend recipe based on ingredients
        recommendRecipes()

        // navigation
        bookmarksImageView.setOnClickListener {
            findNavController().navigate(R.id.action_recipeFragment_to_bookmarks)
        }

        // select ingredients
        recipeSelectIngredientsTextview.setOnClickListener {
            showBottomSheetDialog(bottomSheetIngredientAdapter)
        }

        // search on google
        recipeGoogleSearchTextView.setOnClickListener {
            recipeWebView.webViewClient = MyBrowser()
            recipeWebView.settings.loadsImagesAutomatically = true
            recipeWebView.settings.javaScriptEnabled = true
            recipeWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            recipeWebView.loadUrl("https://www.google.com/search?q=${recipeSearchView.query}")

            recipeRecyclerViewLinearLayout.visibility = View.GONE
            recipeGoogleSearchTextView.visibility = View.GONE
            recipeWebViewLinearLayout.visibility = View.VISIBLE
        }

        // searchView
        recipeSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrBlank()) {
                    recommendRecipes()
                }else {
                    recipeViewModel.searchInDatabase(
                        query.toString(),
                        selectedIngredients,
                        currentView.context
                    ) {
                        recipeArrayList.clear()
                        recipeArrayList = it
                        if (it.isEmpty()) {
                            nothingFound()
                        } else {
                            initializeRecyclerView()
                        }
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    recommendRecipes()
                }else {
                    recipeViewModel.searchInDatabase(
                        newText.toString(),
                        selectedIngredients,
                        currentView.context
                    ) {
                        recipeArrayList.clear()
                        recipeArrayList = it
                        if (it.isEmpty()) {
                            nothingFound()
                        } else {
                            initializeRecyclerView()
                        }
                    }
                }
                return false
            }
        })

        return currentView
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
            recipeViewModel.searchInDatabase(recipeSearchView.query.toString(), selectedIngredients, currentView.context) {
                recipeArrayList.clear()
                recipeArrayList = it
                if (it.isEmpty()) {
                    nothingFound()
                }else {
                    initializeRecyclerView()
                }
            }
        }
        bottomSheetDialog.show()

        adapter.setIngredient(getFromStoredIngredients.minus(selectedIngredients.toSet()))
        adapter.notifyDataSetChanged()
    }

    private fun displayIngredients() {
        val recipeSearchSelectedIngredientsTextView = currentView.findViewById<TextView>(R.id.recipeSelectedIngredientsTextView)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val ingredients =
                    StringBuilder(getString(R.string.selected_ingredients))
                        .append(" ")
                        .append(selectedIngredients.joinToString(", ") {
                            it.ingredientName
                        })
                recipeSearchSelectedIngredientsTextView.text = ingredients
            }
        }
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

    private class MyBrowser : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }

    private fun initializeRecyclerView() {
        recipeRecyclerView.adapter = RecipeRecyclerViewAdapter(recipeArrayList)

        recipeWebViewLinearLayout.visibility = View.GONE
        recipeGoogleSearchTextView.visibility = View.GONE
        recipeRecyclerViewLinearLayout.visibility = View.VISIBLE
    }

    private fun recommendRecipes() {
        val url =
            StringBuilder(getString(R.string.url_server))
                .append(getString(R.string.recipeGetRecommendURL))
                .append("?userID=$userID")
                .toString()
        Log.d("recommend url", url)
        recipeViewModel.getRecommend(url, currentView) {
            recipeArrayList.clear()
            recipeArrayList = it
            initializeRecyclerView()
        }
    }

    private fun initView() {
        bookmarksImageView = currentView.findViewById(R.id.bookmarksImageView)
        recipeSelectIngredientsTextview = currentView.findViewById(R.id.recipeSelectIngredientsTextview)
        recipeGoogleSearchTextView = currentView.findViewById(R.id.recipeGoogleSearchTextView)
        recipeSearchView = currentView.findViewById(R.id.recipeSearchView)
        recipeRecyclerView = currentView.findViewById(R.id.recipeRecyclerView)
        recipeWebView = currentView.findViewById(R.id.recipeWebView)
        recipeWebViewLinearLayout = currentView.findViewById(R.id.recipeWebViewLinearLayout)
        recipeRecyclerViewLinearLayout = currentView.findViewById(R.id.recipeRecyclerViewLinearLayout)
    }



        private fun nothingFound() {
        recipeWebViewLinearLayout.visibility = View.GONE
        recipeRecyclerViewLinearLayout.visibility = View.GONE
        recipeGoogleSearchTextView.visibility = View.VISIBLE
    }
}