package com.codecrafters.locationtracker.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.codecrafters.locationtracker.R
import com.codecrafters.locationtracker.authentication.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var updateButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        firstNameEditText = view.findViewById(R.id.first_name_edit_text)
        lastNameEditText = view.findViewById(R.id.last_name_edit_text)
        updateButton = view.findViewById(R.id.update_button)
        logoutButton = view.findViewById(R.id.logout_button)

        val user = auth.currentUser
        if (user != null) {
            val userRef = database.child(user.uid)
            userRef.child("firstName").get().addOnSuccessListener {
                firstNameEditText.setText(it.value as String?)
            }
            userRef.child("lastName").get().addOnSuccessListener {
                lastNameEditText.setText(it.value as String?)
            }
        }

        updateButton.setOnClickListener {
            updateProfile()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    requireActivity().onBackPressed()
                    val sharedPreferences = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()
                    Toast.makeText(requireActivity(), "Logout successfully", Toast.LENGTH_SHORT).show()

                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        return view
    }

    private fun updateProfile() {
        val user = auth.currentUser
        if (user != null) {
            val userRef = database.child(user.uid)
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()

            userRef.child("firstName").setValue(firstName)
            userRef.child("lastName").setValue(lastName).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
