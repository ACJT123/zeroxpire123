package my.edu.tarc.zeroxpire.adapters

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.goal.GoalClickListener
import my.edu.tarc.zeroxpire.model.Goal
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class GoalAdapter(private val clickListener: GoalClickListener, private val ingredientViewModel: IngredientViewModel) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {
    private var goalList = emptyList<Goal>()


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewGoalName: TextView = view.findViewById(R.id.goalName)
        val textViewDaysLeft: TextView = view.findViewById(R.id.daysLeft)
        val goalStateImage: ImageView = view.findViewById(R.id.goalStateImage)
        val ingredientContainer: LinearLayout = view.findViewById(R.id.listOfIngredientImages) // Replace with the actual ID of your LinearLayout
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.goal_recycler_view, parent, false)
        return ViewHolder(view)
    }

    internal fun setGoal(goal: List<Goal>) {
        goalList = goal
        notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goalList[position]
        holder.textViewGoalName.text = goal.goalName
//        val targetCompletionDate: Date = goal.targetCompletionDate
//        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
//        val formattedDate = targetCompletionDate.let { dateFormatter.format(it) } ?: "Date Not Set"

        val targetCompletionDate: LocalDate? = goal.targetCompletionDate.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val currentDate: LocalDate = LocalDate.now()
        val absoluteDaysLeft: Long = ChronoUnit.DAYS.between(currentDate, targetCompletionDate)

        val daysLeftText: String = when {
            absoluteDaysLeft < 0 -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.uncompleted_goal)
                    .into(holder.goalStateImage)
                "Overdue"
            }
            absoluteDaysLeft == 0L -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.active_goal)
                    .into(holder.goalStateImage)
                "Due today"
            }
            else -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.active_goal)
                    .into(holder.goalStateImage)
                when (absoluteDaysLeft) {
                    1L -> "Due tomorrow"
                    else -> "Due in $absoluteDaysLeft days"
                }
            }
        }

        val isCompleted: Boolean = goal.completedDate != null

        holder.textViewDaysLeft.text = if(isCompleted){
            holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
            holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
            Glide.with(holder.itemView.context)
                .load(R.drawable.completed_goal)
                .into(holder.goalStateImage)
            "Completed"
        }
        else{
            daysLeftText
        }

        // Load ingredient images based on the goal ID using the IngredientViewModel
        loadIngredientImages(goal.goalId, holder)

        holder.itemView.setOnClickListener {
            clickListener.onGoalClick(goal)
        }
//        holder.textViewDaysLeft.text = "Target Completion Date: $formattedDate"

        // Set other data to the views as needed
    }

    private fun loadIngredientImages(goalId: Int, holder: ViewHolder) {
        // Load ingredients using the IngredientViewModel
        ingredientViewModel.getIngredientImagesForGoal(goalId).observe(holder.itemView.context as LifecycleOwner) { ingredients ->
            holder.ingredientContainer.removeAllViews() // Clear existing views
            holder.ingredientContainer

            Log.d("sizeeeeeeeeeeeeeeee: ", ingredients.size.toString())
            for (ingredient in ingredients) {
                val ingredientImageView = ImageView(holder.itemView.context)
                // Customize the ImageView properties, such as layout parameters, scale type, etc.
                // Load the ingredient image using Glide and set it to the ImageView
                Glide.with(holder.itemView.context)
                    .load(ingredient.ingredientImage)
                    .into(ingredientImageView)

                // Add the ImageView to the LinearLayout
                holder.ingredientContainer.addView(ingredientImageView)
            }
        }
    }


    override fun getItemCount(): Int {
        return goalList.size
    }

    fun getGoalAt(position: Int): Goal {
        return goalList[position]
    }

    fun getPosition(goal: Goal): Int{
        return goalList.indexOf(goal)
    }
}
