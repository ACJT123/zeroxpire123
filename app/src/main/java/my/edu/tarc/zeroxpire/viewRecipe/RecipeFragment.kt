package my.edu.tarc.zeroxpire.viewRecipe

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import my.edu.tarc.zeroxpire.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecipeFragment : Fragment() {
    // declaration
    private var imageButtonSearch: ImageButton? = null
    private var imageButtonViewBookmarks: ImageButton? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipe, container, false)

        // assign views
        imageButtonSearch = view.findViewById(R.id.imageButtonSearch)
        imageButtonViewBookmarks = view.findViewById(R.id.imageButtonViewBookmarks)
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)


        // navigation
        imageButtonSearch!!.setOnClickListener{
            findNavController().navigate(R.id.action_recipeFragment_to_recipe_search)
        }

        imageButtonViewBookmarks!!.setOnClickListener{
            findNavController().navigate(R.id.action_recipeFragment_to_viewBookmarks)
        }

        //setup recycler view
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(view.context)
        recyclerView?.adapter = RecipeAdapter()

        return view
    }
}