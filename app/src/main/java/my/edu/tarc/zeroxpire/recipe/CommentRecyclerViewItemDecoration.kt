package my.edu.tarc.zeroxpire.recipe

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CommentRecyclerViewItemDecoration(space: Int) : RecyclerView.ItemDecoration() {
    private var space: Int? = space

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = space!!
    }
}