package my.edu.tarc.zeroxpire.recipe

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.setPadding

class Utilities {

    fun createNewLinearLayout(
        view: View,
        left: Int = 0,
        right: Int = 0,
        top: Int = 0,
        bottom: Int = 0
    ): LinearLayout {
        val newLinearLayout = LinearLayout(view.context, null)

        //apply attributes
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(left,top,right,bottom)

        newLinearLayout.layoutParams = layoutParams
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        return newLinearLayout
    }

    fun createNewCheckBox(
        view: View,
        text: String = "",
        typeface: Int = Typeface.NORMAL
    ): CheckBox {
        val newCheckBox = CheckBox(view.context)

        //apply attributes
        newCheckBox.text = text
        newCheckBox.textSize = 24F
        newCheckBox.setTypeface(null, typeface)
        newCheckBox.isVisible = true

        return newCheckBox
    }

    fun createNewEditText(view: View, text: String = "", hint: String = ""): EditText {
        val newEditText = EditText(view.context)
        //apply attributes
        newEditText.background = null
        newEditText.gravity = Gravity.START or Gravity.TOP
        newEditText.hint = hint
        newEditText.isSingleLine = false
        newEditText.imeOptions = EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE
        newEditText.textSize = 24F
        newEditText.setText(text)

        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0,0,0,0)
        newEditText.setPadding(0)
        newEditText.layoutParams = layoutParams

        return newEditText
    }

    fun createNewTextView(
        view: View,
        text: String = "",
        typeface: Int = Typeface.NORMAL,
        textSize: Float = 24F): TextView {
        val newTextView = TextView(view.context)

        //apply attributes
        newTextView.text = text
        newTextView.textSize = textSize
        newTextView.setTypeface(null, typeface)
        newTextView.isVisible = true

        return newTextView
    }
}