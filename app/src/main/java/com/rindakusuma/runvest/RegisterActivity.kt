package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val dbRef = FirebaseDatabase.getInstance().getReference("users")

                        val profileData = mapOf(
                            "name" to name,
                            "email" to email
                        )

                        uid?.let {
                            dbRef.child(it).child("profile").setValue(profileData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal simpan profil", Toast.LENGTH_SHORT).show()
                                }
                        } ?: run {
                            Toast.makeText(this, "UID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
