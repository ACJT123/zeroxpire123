package my.edu.tarc.zeroxpire.recipe.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.fragment.BookmarksFragmentDirections
import my.edu.tarc.zeroxpire.recipe.viewHolder.BookmarksRecyclerViewHolder

class BookmarksRecyclerViewAdapter(
    private val recipeArrayList: ArrayList<Recipe>,
    ) : RecyclerView.Adapter<BookmarksRecyclerViewHolder>() {

    // declaration
    private lateinit var parentContext: Context
    private lateinit var auth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarksRecyclerViewHolder {
        parentContext = parent.context
        auth = FirebaseAuth.getInstance()

        val view = LayoutInflater.from(parentContext).inflate(viewType, parent, false)
        return BookmarksRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.bookmark_frame
    }

    override fun onBindViewHolder(holder: BookmarksRecyclerViewHolder, position: Int) {
        // declaration: views
        val currentRecipe = recipeArrayList[position]

        val bookmarkConstraintLayout : ConstraintLayout = holder.getView().findViewById(R.id.bookmarkConstraintLayout)
        val bookmarkTitleTextview: TextView = holder.getView().findViewById(R.id.bookmarkTitleTextview)
        val bookmarkIngredientsTextview: TextView = holder.getView().findViewById(R.id.bookmarkIngredientsTextview)
        val bookmarkImageView : ImageView = holder.getView().findViewById(R.id.bookmarkImageView)

        bookmarkConstraintLayout.setOnClickListener{
            val action = BookmarksFragmentDirections.actionBookmarksToRecipeDetails(currentRecipe.recipeID.toString())
            Navigation.findNavController(holder.getView()).navigate(action)
        }

        bookmarkTitleTextview.text = currentRecipe.title

        bookmarkIngredientsTextview.text = currentRecipe.ingredientNames

        Picasso.get().load(currentRecipe.imageLink).into(bookmarkImageView)
    }

    override fun getItemCount(): Int {
        return recipeArrayList.size
    }
}