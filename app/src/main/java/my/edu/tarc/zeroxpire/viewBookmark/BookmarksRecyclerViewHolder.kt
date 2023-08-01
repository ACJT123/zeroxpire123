package my.edu.tarc.zeroxpire.viewBookmark

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R

class BookmarksRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val view: ConstraintLayout

    init {
        super.itemView
        view = itemView.findViewById(R.id.viewBookmarksRecyclerViewConstraintLayout)
    }

    fun getView(): ConstraintLayout {
        return view
    }
}