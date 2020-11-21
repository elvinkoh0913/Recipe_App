package com.example.recipeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var loadGif: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        loadGif = findViewById(R.id.loadingGif)
        var btnLogin = findViewById<Button>(R.id.btLogin)
        var btnSignup = findViewById<Button>(R.id.btRegister)

        btnLogin.setOnClickListener {
            val email = et_Email.text.toString().trim()
            val password = et_Password.text.toString().trim()
            if (validate()) {
                loadGif.isVisible = true
                doLogin(email, password)
            }
        }

        btnSignup.setOnClickListener {
            if (validate()) {
                loadGif.isVisible = true
                signUpUser()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (auth.currentUser == null){
            finishAffinity()
            finish()
        }
    }

    //LOGIN FUNCTION
    private fun doLogin(email: String, password: String) {

        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (auth.currentUser!!.isEmailVerified) {
                        loadGif.isVisible = false
                        startActivity(Intent(this, MainActivity::class.java))
                    }else {
                        loadGif.isVisible = false
                        Toast.makeText(baseContext, "Please verify your email address", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadGif.isVisible = false
                    Toast.makeText(baseContext, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private  fun  validate():Boolean {
        val email = et_Email.text.toString().trim()
        val password = et_Password.text.toString().trim()

        if (email.isEmpty()) {
            et_Email.error = "Please enter email"
            et_Email.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            et_Email.error = "Invalid Email"
            et_Email.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            et_Password.error = "Please enter password"
            et_Password.requestFocus()
            return false
        }
        if (password.length < 6) {
            et_Password.error = "at least 6 characters"
            et_Password.requestFocus()
            return false
        }
        return true
    }

    private fun signUpUser() {
        auth.createUserWithEmailAndPassword(et_Email.text.toString(), et_Password.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user!!.sendEmailVerification()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                loadGif.isVisible = false
                                Toast.makeText(baseContext, "Verification email is sent please check your email", Toast.LENGTH_LONG).show()
                            }else{
                                loadGif.isVisible = false
                                Toast.makeText(baseContext, task.exception?.message.toString(), Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    loadGif.isVisible = false
                    Toast.makeText(baseContext, task.exception.toString(),
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
