package my.edu.tarc.zeroxpire.view.others

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
            val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
            view.visibility = View.VISIBLE

            val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            add.visibility = View.VISIBLE
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
                val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
                view.visibility = View.VISIBLE

                val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                add.visibility = View.VISIBLE
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)


        val numberPicker: NumberPicker = binding.numPicker
        val textView: TextView = binding.daysReminder
        numberPicker.minValue = 1
        numberPicker.maxValue = 30


        numberPicker.setOnValueChangedListener { _, _, new ->
            textView.text = "Remind you when things are expired within $new days"
        }

        // Set initial text based on the initial value of the NumberPicker
        textView.text = "Remind you when things are expired within ${numberPicker.value} days"
    }
}
