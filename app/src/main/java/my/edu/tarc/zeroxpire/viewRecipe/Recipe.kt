package my.edu.tarc.zeroxpire.viewRecipe

data class Recipe(var recipeID: Int = -1,
                  var title: String = "",
                  var instructionsLink: String = "",
                  var note: String = "",
                  var author: String = "",
                  var ingredientArrayList: ArrayList<String> = ArrayList())