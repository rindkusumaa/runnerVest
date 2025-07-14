package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
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
        database = FirebaseDatabase.getInstance().reference

        progressBar = findViewById(R.id.progressBar)
        emailField = findViewById(R.id.emailEditText)
        passwordField = findViewById(R.id.passwordEditText)
        loginBtn = findViewById(R.id.loginButton)
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility)

        val registerTextView: TextView = findViewById(R.id.registerTextView)
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show()
            }
        }

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
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val dbRef = FirebaseDatabase.getInstance().reference

                    dbRef.child("users").child(uid).child("profile").child("role")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val role = snapshot.getValue(String::class.java)

                                when (role) {
                                    "atlet" -> {
                                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                                        finish()
                                    }
                                    "pelatih" -> {
                                        startActivity(Intent(this@LoginActivity, DaftarAtletActivity::class.java))
                                        finish()
                                    }
                                    null -> {
                                        Toast.makeText(this@LoginActivity, "Role belum diatur. Silakan registrasi ulang.", Toast.LENGTH_SHORT).show()
                                        FirebaseAuth.getInstance().signOut()
                                    }
                                    else -> {
                                        Toast.makeText(this@LoginActivity, "Role tidak dikenali: $role", Toast.LENGTH_SHORT).show()
                                        FirebaseAuth.getInstance().signOut()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@LoginActivity, "Gagal membaca role", Toast.LENGTH_SHORT).show()
                            }
                        })
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
