package my.edu.tarc.zeroxpire.adapters

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import androidx.lifecycle.Observer
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import java.time.temporal.ChronoUnit
import java.util.*

class IngredientAdapter(private val clickListener: IngredientClickListener, private val goalViewModel: GoalViewModel): RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {
    private var ingredientList = emptyList<Ingredient>()
    private var originalIngredientList = emptyList<Ingredient>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textViewIngredientName: TextView = view.findViewById(R.id.ingredientName)
        val textViewDaysLeft: TextView = view.findViewById(R.id.daysLeft)
        val textViewDateAdded: TextView = view.findViewById(R.id.dateAdded)
        val isAddedToGoalImage: ImageView = view.findViewById(R.id.isAddedToGoalImage)
        val imageViewIngredientImage: ImageView = view.findViewById(R.id.ingredientImage)
    }

    internal fun setIngredient(ingredient: List<Ingredient>) {
        originalIngredientList = ingredient
        ingredientList = ingredient
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_design, parent, false)
        return ViewHolder(view)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]

        holder.textViewIngredientName.text = ingredient.ingredientName

        val expiryDate: LocalDate? = ingredient.expiryDate?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()

        if (expiryDate != null) {
            val currentDate: LocalDate = LocalDate.now()
            val absoluteDaysLeft: Long = ChronoUnit.DAYS.between(currentDate, expiryDate)

            val daysLeftText: String = when {
                absoluteDaysLeft < 0 -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                    "Expired"
                }
                absoluteDaysLeft == 0L -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                    "Expires Today"
                }
                else -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
                    when (absoluteDaysLeft) {
                        1L -> "Expired in $absoluteDaysLeft day"
                        else -> "Expired in $absoluteDaysLeft days"
                    }
                }
            }
            holder.textViewDaysLeft.text = daysLeftText
        } else {
            holder.textViewDaysLeft.text = "Expiration Date Not Set"
        }

        val addedDate: Date? = ingredient.dateAdded
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
        val formattedDate = addedDate?.let { dateFormatter.format(it) } ?: "Date Not Set"
        holder.textViewDateAdded.text = "Date Added: $formattedDate"
        Log.d("Ingredient Detail", ingredient.toString())
        Log.d("IngredientGoalId", ingredient.ingredientGoalId.toString())

        Log.d("IngredientImage", ingredient.ingredientImage.toString())
        if(!ingredient.ingredientImage.isNullOrEmpty()){
            Glide.with(holder.itemView.context).load(ingredient.ingredientImage).into(holder.imageViewIngredientImage)
        }

        if(ingredient.ingredientGoalId != null){
            // Observe the LiveData for goal details
            //TODO: use webhost to join table
            goalViewModel.goalList.observe(holder.itemView.context as LifecycleOwner, Observer { goals ->
                for (goal in goals){
                    if(goal.completedDate != null){
                        Glide.with(holder.itemView.context)
                            .load(R.drawable.completed_goal)
                            .into(holder.isAddedToGoalImage)
                    }
                    else{
                        Glide.with(holder.itemView.context)
                            .load(R.drawable.active_goal)
                            .into(holder.isAddedToGoalImage)
                    }
                }
            })

        }
        else{

        }

        holder.itemView.setOnClickListener {
            clickListener.onIngredientClick(ingredient)
            //Toast.makeText(it.context, ingredient.ingredientName, Toast.LENGTH_SHORT).show()
        }

        // Convert the byte array to a Bitmap


// Convert the byte array to a Bitmap
//        val imageByteArray = ingredient.ingredientImage
//        Log.d("byteArray", imageByteArray.toString())
//        val bmp: Bitmap? = imageByteArray?.let { BitmapFactory.decodeByteArray(imageByteArray, 0, it.size) }
//        Log.d("bitmap", bmp.toString())



// Load the Bitmap into the ImageView using Glide
//        Glide.with(holder.itemView.context)
//            .load(imageBitmap)
//            .centerCrop()
//            .into(holder.imageViewIngredientImage)

    }


    override fun getItemCount(): Int {
        return ingredientList.size
    }

    fun getIngredientAt(position: Int): Ingredient {
        return ingredientList[position]
    }

    fun getPosition(ingredient: Ingredient): Int{
        return ingredientList.indexOf(ingredient)
    }
}
