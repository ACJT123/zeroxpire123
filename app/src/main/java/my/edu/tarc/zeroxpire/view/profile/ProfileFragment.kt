package my.edu.tarc.zeroxpire.view.profile

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentProfileBinding
import my.edu.tarc.zeroxpire.model.IngredientDatabase

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding

    private lateinit var auth: FirebaseAuth
    private var profilePictureUrl: String? = ""
    private var username: String? = ""

    private val ingredientDatabase by lazy {
        IngredientDatabase.getDatabase(requireContext()).ingredientDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        if(auth.currentUser != null){
            val user = Firebase.auth.currentUser
            val photoUrl = user?.photoUrl
            profilePictureUrl = photoUrl?.toString()
            if (profilePictureUrl != null) {
                Glide.with(this)
                    .load(profilePictureUrl)
                    .into(binding.profilePicture)
            }
            username = user?.displayName
            binding.username.text = username.toString()
            val email = user?.email
            binding.email.text = email.toString()

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.termsAndConditionsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_termsAndConditionsFragment)
            disableBtmNav()
        }

        binding.notificationsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_notificationsFragment)
            disableBtmNav()
        }

        binding.editProfileCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            setFragmentResult("requestName", bundleOf("name" to username))
            setFragmentResult("requestProfilePic", bundleOf("profilePic" to profilePictureUrl))
            disableBtmNav()
        }

        binding.logoutBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to Logout?")
                .setCancelable(false)
                .setPositiveButton("Logout") { dialog, id ->
                    logout()
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        binding.deleteAccBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to Delete?")
                .setCancelable(false)
                .setPositiveButton("Delete") { dialog, id ->
                    deleteAcc()
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

    }

    private fun deleteAcc() {
        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if(task.isSuccessful){
                findNavController().navigate(R.id.loginFragment)
                lifecycleScope.launch(Dispatchers.IO) {
                    ingredientDatabase.deleteAllIngredient()
                }
                disableBtmNav()
                Toast.makeText(requireContext(), "Account is deleted successfully", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(requireContext(), task.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableBtmNav(){
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun logout(){
        Firebase.auth.signOut()
        disableBtmNav()
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        findNavController().clearBackStack(R.id.loginFragment)
    }


}