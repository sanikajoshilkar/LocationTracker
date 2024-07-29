package com.codecrafters.locationtracker.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.codecrafters.locationtracker.activities.MainActivity
import com.codecrafters.locationtracker.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
//To ask about confirmation of the exit
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                super.onBackPressed()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        if (isLoggedIn()) {
            navigateToMain()
            return
        }

        val email = findViewById<TextInputEditText>(R.id.input_email)
        val password = findViewById<TextInputEditText>(R.id.input_password)
        val loginButton = findViewById<MaterialButton>(R.id.btn_login)
        val registerTextView = findViewById<TextView>(R.id.registerTV)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgotPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressbar)

        loginButton.setOnClickListener {
            val emailText = email.text.toString()
            val passwordText = password.text.toString()

            if (emailText.isNotEmpty() && passwordText.isNotEmpty()) {
                progressBar.visibility = ProgressBar.VISIBLE
                signInUser(emailText, passwordText, progressBar)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signInUser(email: String, password: String, progressBar: ProgressBar) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = ProgressBar.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    saveLoginState(true)
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    //SHared preference
    private fun saveLoginState(isLoggedIn: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.apply()
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish the LoginActivity so the user cannot navigate back to it using back button
    }
    // Forgot password Dialog
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")

        val input = TextInputEditText(this)
        input.hint = "Enter your email"
        builder.setView(input)

        builder.setPositiveButton("Send") { dialog, _ ->
            val email = input.text.toString()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // Impementation for hiding the keboard
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard(v)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun Context.hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
