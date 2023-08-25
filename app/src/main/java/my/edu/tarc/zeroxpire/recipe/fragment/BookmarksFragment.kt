package my.edu.tarc.zeroxpire.recipe.fragment

import android.graphics.Canvas
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.adapter.BookmarksRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.BookmarkViewModel


class BookmarksFragment : Fragment() {
    // declaration
    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    private lateinit var currentView: View


    private lateinit var upBtn: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var appBarEditText: EditText

    private var recipeArrayList = ArrayList<Recipe>()

    private val bookmarkViewModel = BookmarkViewModel()

    private lateinit var swipeHelper: ItemTouchHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe_bookmarks, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()
        disableBtmNav()

        // assign views
        upBtn = currentView.findViewById(R.id.upBtn)
        recyclerView = currentView.findViewById(R.id.bookmarksRecyclerView)
        appBarEditText = currentView.findViewById(R.id.appBarEditText)

        // navigation
        upBtn.setOnClickListener{
            findNavController().popBackStack()
        }

        appBarEditText.inputType = InputType.TYPE_NULL

        //setup recycler view
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(currentView.context)


        loadBookmarks()

        return currentView
    }


    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun setUpItemTouchHelper() {
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
                val recipeID = recipeArrayList[pos].recipeID.toString()

                bookmarkViewModel.removeFromBookmarks(
                    userID,
                    recipeID,
                    currentView
                ) {
                    val snackBar = Snackbar.make(currentView, "Removed from bookmarks", Snackbar.LENGTH_SHORT)
                    snackBar.setAction("UNDO",
                        UndoListener(userID,
                            recipeID,
                            bookmarkViewModel) {
                            loadBookmarks()
                        }
                    )
                    snackBar.show()
                }

                recipeArrayList.removeAt(pos)
                recyclerView?.adapter = BookmarksRecyclerViewAdapter(recipeArrayList)
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
        swipeHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadBookmarks() {
        bookmarkViewModel.getBookmarksByUserID(userID, currentView) {
            recipeArrayList = it
            recyclerView.adapter = BookmarksRecyclerViewAdapter(recipeArrayList)

            setUpItemTouchHelper()
        }
    }

    class UndoListener(
        private val userId: String,
        private val recipeID: String,
        private val bookmarkViewModel: BookmarkViewModel,
        private val callback: (Boolean) -> Unit

    ) : View.OnClickListener {
        override fun onClick(v: View) {
            bookmarkViewModel.addToBookmark(userId, recipeID, v) {
                callback(true)
            }
        }
    }

}