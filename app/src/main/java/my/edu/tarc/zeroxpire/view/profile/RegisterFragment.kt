package my.edu.tarc.zeroxpire.view.profile

import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.zeroxpire.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //register -> login
        setClickListeners()

        binding.createAccBtn.setOnClickListener {
            val username = binding.enterUsername.text.toString()
            val email = binding.enterEmail.text.toString()
            val password = binding.enterPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                createUserWithEmailAndPassword(email, password)
            }
            else {
                if (username.isEmpty()) {
                    binding.enterUsername.error = "Please enter the username."
                    binding.enterUsername.requestFocus()
                }
                if (email.isEmpty()) {
                    binding.enterEmail.error = "Please enter the email address."
                    binding.enterEmail.requestFocus()
                }
                if (password.isEmpty()) {
                    binding.enterPasswordLayout.endIconMode = TextInputLayout.END_ICON_NONE
                    binding.enterPassword.error = "Please enter the password."
                    binding.enterPassword.requestFocus()

                    //enable back when the text field is filled
                    binding.enterPassword.doAfterTextChanged {
                        binding.enterPasswordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    }
                }
            }
        }
    }

    //register -> login 
    private fun handleBackPressed() {
        findNavController().navigateUp()
    }
    private fun setClickListeners() {
        binding.upBtn.setOnClickListener {
            handleBackPressed()
        }

        binding.loginBtn.setOnClickListener {
            handleBackPressed()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            handleBackPressed()
        }
    }

    private fun createUserWithEmailAndPassword(email: String, password: String) {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Creating...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    findNavController().navigateUp()
                    showToast("Account created successfully!")
                } else {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    showToast("Failed to create the account.")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
