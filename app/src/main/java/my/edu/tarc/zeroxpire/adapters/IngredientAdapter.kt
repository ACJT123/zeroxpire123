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
        val textViewExpiryDate: TextView = view.findViewById(R.id.expiryDate)
        val textViewDateAdded: TextView = view.findViewById(R.id.dateAdded)
        val isAddedToGoalImage: ImageView = view.findViewById(R.id.isAddedToGoalImage)
        val imageViewIngredientCategoryImage: ImageView = view.findViewById(R.id.ingredientCategoryImage)
        val imageViewIngredientImage: ImageView = view.findViewById(R.id.ingredientImage)
    }

    internal fun setIngredient(ingredient: List<Ingredient>) {
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


        when(ingredient.ingredientCategory){
            "Vegetables" ->
                Glide.with(holder.itemView.context)
                .load(R.drawable.vegetable)
                .into(holder.imageViewIngredientCategoryImage)
            "Fruits" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.fruits)
                    .into(holder.imageViewIngredientCategoryImage)
            "Meat" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.meat)
                    .into(holder.imageViewIngredientCategoryImage)
            "Seafood" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.seafood)
                    .into(holder.imageViewIngredientCategoryImage)
            "Dairy" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.dairy)
                    .into(holder.imageViewIngredientCategoryImage)
            "Grains" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.grains)
                    .into(holder.imageViewIngredientCategoryImage)
            "Herbs" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.herbs)
                    .into(holder.imageViewIngredientCategoryImage)
            "Oils" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.oil)
                    .into(holder.imageViewIngredientCategoryImage)
            "Legumes" ->
                Glide.with(holder.itemView.context)
                    .load(R.drawable.legumes)
                    .into(holder.imageViewIngredientCategoryImage)
        }


        val expiryDate: LocalDate? = ingredient.expiryDate?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()

        holder.textViewExpiryDate.text = "Expiry Date: ${expiryDate.toString()}"

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
            Glide.with(holder.itemView.context)
                .load(R.drawable.goal)
                .into(holder.isAddedToGoalImage)
        }
        else{
            holder.isAddedToGoalImage.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener {
            clickListener.onIngredientClick(ingredient)
            //Toast.makeText(it.context, ingredient.ingredientName, Toast.LENGTH_SHORT).show()
        }

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
