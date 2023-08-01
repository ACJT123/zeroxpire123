package my.edu.tarc.zeroxpire.viewBookmark

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.viewRecipe.RecipeAdapter


class ViewBookmarks : Fragment() {
    // declaration
    private var imageButtonBookmarksBack: ImageButton? = null
    private var recyclerView: RecyclerView? = null
    private lateinit var c: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        c = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_bookmarks, container, false)

        // assign views
        imageButtonBookmarksBack = view.findViewById(R.id.imageButtonBookmarksBack)
        recyclerView = view.findViewById(R.id.viewBookmarksRecyclerView)

        // navigation
        imageButtonBookmarksBack!!.setOnClickListener{
            findNavController().popBackStack()
        }

        //setup recycler view

        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(view.context)
        recyclerView?.adapter = BookmarksAdapter()

//        val callback: ItemTouchHelper.Callback = ItemMoveCallback(mAdapter)
//        val touchHelper = ItemTouchHelper(callback)
//        touchHelper.attachToRecyclerView(recyclerView)
//
//        recyclerView?.adapter = mAdapter


        return view
    }
}