package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var togglePasswordVisibility: View
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        progressBar = findViewById(R.id.progressBar)

        // Cek apakah user sudah login
        val user = auth.currentUser
        if (user != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }


        emailField = findViewById(R.id.emailEditText)
        passwordField = findViewById(R.id.passwordEditText)
        loginBtn = findViewById(R.id.loginButton)
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility)

        val registerTextView: TextView = findViewById(R.id.registerTextView)

        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }


        loginBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE  // Tampilkan progress bar saat login
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle password visibility
        togglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                passwordField.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility)
            } else {
                passwordField.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility)
            }

            passwordField.setSelection(passwordField.text.length)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE  // Sembunyikan progress bar setelah proses login selesai
                if (task.isSuccessful) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Login gagal: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
