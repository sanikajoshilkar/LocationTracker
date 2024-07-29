package com.codecrafters.locationtracker.authentication
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.codecrafters.locationtracker.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
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
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val firstName = findViewById<TextInputEditText>(R.id.ptxt_firstname)
        val lastName = findViewById<TextInputEditText>(R.id.ptxt_lastname)
        val email = findViewById<TextInputEditText>(R.id.ptxt_email)
        val password = findViewById<TextInputEditText>(R.id.ptxtpassword)
        val confirmPassword = findViewById<TextInputEditText>(R.id.ptxt_cpassword)
        val createUserButton = findViewById<MaterialButton>(R.id.btn_createuser)
        val idTVLogin = findViewById<TextView>(R.id.idTVLogin)

        createUserButton.setOnClickListener {
            val firstNameText = firstName.text.toString().trim()
            val lastNameText = lastName.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            if (firstNameText.isEmpty() || lastNameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(emailText)) {
                setEmailError(email, "Invalid email format")
                return@setOnClickListener
            }

            if (!isValidPassword(passwordText)) {
                setPasswordError(password, "Password must contain at least one uppercase, one lowercase, and one special character")
                return@setOnClickListener
            }

            if (passwordText != confirmPasswordText) {
                setConfirmPasswordError(confirmPassword, "Passwords do not match")
                return@setOnClickListener
            }

            signUpUser(emailText, passwordText, firstNameText, lastNameText)
        }

        idTVLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        addTextWatchers(firstName, lastName, email, password, confirmPassword)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$"
        return Pattern.compile(passwordPattern).matcher(password).matches()
    }

    private fun signUpUser(email: String, password: String, firstName: String, lastName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val uid = it.uid
                        val userMap = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email
                        )
                        database.child("users").child(uid).setValue(userMap)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "User created successfully. Please login", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setEmailError(email: TextInputEditText, error: String) {
        val emailLayout = email.parent.parent as TextInputLayout
        emailLayout.error = error
   //     emailLayout.setBackgroundColor(Color.RED)
    }

    private fun setPasswordError(password: TextInputEditText, error: String) {
        val passwordLayout = password.parent.parent as TextInputLayout
        passwordLayout.error = error
      //  passwordLayout.setBackgroundColor(Color.RED)
    }

    private fun setConfirmPasswordError(confirmPassword: TextInputEditText, error: String) {
        val confirmPasswordLayout = confirmPassword.parent.parent as TextInputLayout
        confirmPasswordLayout.error = error
      //  confirmPasswordLayout.setBackgroundColor(Color.RED)
    }

    private fun clearError(textInputEditText: TextInputEditText) {
        val layout = textInputEditText.parent.parent as TextInputLayout
        layout.error = null
        layout.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun addTextWatchers(
        firstName: TextInputEditText,
        lastName: TextInputEditText,
        email: TextInputEditText,
        password: TextInputEditText,
        confirmPassword: TextInputEditText
    ) {
        firstName.addTextChangedListener(createTextWatcher(firstName))
        lastName.addTextChangedListener(createTextWatcher(lastName))
        email.addTextChangedListener(createTextWatcher(email))
        password.addTextChangedListener(createTextWatcher(password))
        confirmPassword.addTextChangedListener(createTextWatcher(confirmPassword))
    }

    private fun createTextWatcher(textInputEditText: TextInputEditText): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                clearError(textInputEditText)
            }
        }
    }
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
