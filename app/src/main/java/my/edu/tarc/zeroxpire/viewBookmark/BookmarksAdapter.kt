package my.edu.tarc.zeroxpire.viewBookmark

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import my.edu.tarc.zeroxpire.R


class BookmarksAdapter : RecyclerView.Adapter<BookmarksRecyclerViewHolder>() {
    // declaration
    //TODO: get user id
    private val userId: String = "1"
    private lateinit var context: Context
    private var firebaseDatabase: FirebaseDatabase? = FirebaseDatabase.getInstance("https://zeroxpire-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private var recipesDatabaseReference: DatabaseReference? = firebaseDatabase!!.getReference("Recipes")
    private var userDatabaseReference: DatabaseReference? = firebaseDatabase!!.getReference("Users").child(userId)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarksRecyclerViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(viewType, parent, false)

        return BookmarksRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.bookmark_frame
    }

    override fun onBindViewHolder(holder: BookmarksRecyclerViewHolder, position: Int) {
        // declaration
        var recipeID : String
        var currentRecipe: DatabaseReference?

        // declaration: views
        val recipeDescConstraintLayout : ConstraintLayout = holder.getView().findViewById(R.id.bookmarkRecipeDescConstraintLayout)
        val titleTextView: TextView = holder.getView().findViewById(R.id.bookmarkTitleTextview)
        val ingredientsTextView: TextView = holder.getView().findViewById(R.id.bookmarkIngredientsTextview)
        val recipeImageView : ImageView = holder.getView().findViewById(R.id.bookmarkImageView)
        val shareButton : Button = holder.getView().findViewById(R.id.shareButton)
        val bookmarkButton : Button = holder.getView().findViewById(R.id.bookmarkButton)

        // instantiation
        userDatabaseReference?.
        child("bookmarks")?.child(holder.bindingAdapterPosition.toString())?.get()?.addOnCompleteListener { recipeIDTemp ->
            recipeID = recipeIDTemp.result.value.toString()
            currentRecipe = recipesDatabaseReference?.child(recipeID)

            // navigation
            recipeDescConstraintLayout.setOnClickListener {
                val action = ViewBookmarksDirections.actionViewBookmarksToRecipeDetails(recipeID)
                Navigation.findNavController(holder.getView()).navigate(action)
            }

            //title
            currentRecipe?.child("title")?.get()?.addOnCompleteListener { recipeTitle ->
                titleTextView.text = recipeTitle.result.value.toString()
            }

            //ingredients
            currentRecipe?.child("ingredients")?.get()?.addOnCompleteListener { recipeIngredients ->
                ingredientsTextView.text = recipeIngredients.result.value.toString()
            }

            //image
            currentRecipe?.child("image")?.addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // getting a DataSnapshot for the location at the
                        // specified relative path and getting in the link variable
                        val link = dataSnapshot.getValue(String::class.java)
                        if (link != null) {
                            // loading that data into recipeImageView
                            // variable which is ImageView
                            Picasso.get().load(link).into(recipeImageView)
                        }else {
                            recipeImageView.setImageResource(R.drawable.baseline_image_24)
                        }
                    }

                    // this will called when any problem occurs in getting data
                    override fun onCancelled(databaseError: DatabaseError) {
                        recipeImageView.setImageResource(R.drawable.baseline_broken_image_24)
                    }
                }
            )

            //bookmark button
            bookmarkButton.setOnClickListener{
                userDatabaseReference?.child(userId)?.child("bookmarks")?.child(recipeID.toString())?.
                setValue("true")?.addOnCompleteListener{
                    Toast.makeText(context,"Added to bookmarks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 50
    }
}