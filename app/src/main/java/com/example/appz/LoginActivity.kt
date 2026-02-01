package com.example.appz
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth


val DarkBrownLogin = Color(0xFF40351E)

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {

            AppzTheme {
                AppContent(modifier = Modifier)
            }
        }
    }

    @Composable
    fun AppContent(modifier: Modifier) {
        var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

        if (!isLoggedIn) {
            LogRegScreen(
                modifier = modifier,
                onLoginSuccess = {
                    isLoggedIn = true
                }
            )
        } else {
            navigateToMainScreen()
        }
    }
    private fun navigateToMainScreen(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun LogRegScreen(modifier: Modifier, onLoginSuccess: () -> Unit,) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(

        modifier = Modifier.fillMaxSize().background(AppGradientBrush)
    ){
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))


            StrokedText(
                text = "Wirtualny Alkomat",
                fillColor = Color.White,
                strokeColor = Color.Black,
                strokeWidth = 6f,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                shadowColor = Color.Black.copy(alpha = 0.7f),
                shadowBlurRadius = 10f
            )


            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DarkBrownLogin,
                    unfocusedTextColor = DarkBrownLogin,
                    focusedBorderColor = DarkBrownLogin,
                    unfocusedBorderColor = DarkBrownLogin,
                    cursorColor = DarkBrownLogin
                ),
                label = {  StrokedText(
                    text = "Email",
                    fillColor = DarkBrownLogin,
                    strokeColor = DarkBrownLogin,
                    strokeWidth = 1f,
                    style = MaterialTheme.typography.titleSmall,
                    shadowColor = Color.Black,
                    shadowOffset = Offset(1f, 1f),
                    shadowBlurRadius = 1f
                ) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DarkBrownLogin,
                    unfocusedTextColor = DarkBrownLogin,
                    focusedBorderColor = DarkBrownLogin,
                    unfocusedBorderColor = DarkBrownLogin,
                    cursorColor = DarkBrownLogin
                ),
                label = {  StrokedText(
                    text = "Hasło",
                    fillColor = DarkBrownLogin,
                    strokeColor = DarkBrownLogin,
                    strokeWidth = 1f,
                    style = MaterialTheme.typography.titleSmall,
                    shadowColor = Color.Black,
                    shadowOffset = Offset(1f, 1f),
                    shadowBlurRadius = 1f
                ) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Błąd logowania: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
            ) {
                StrokedText(
                    text = "Zaloguj",
                    fillColor = DarkBrownLogin,
                    strokeColor = DarkBrownLogin,
                    strokeWidth = 1f,
                    style = MaterialTheme.typography.titleSmall,
                    shadowColor = Color.Black,
                    shadowOffset = Offset(1f, 1f),
                    shadowBlurRadius = 1f
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
            ) {
                StrokedText(
                    text = "Zarejestruj",
                    fillColor = DarkBrownLogin,
                    strokeColor = DarkBrownLogin,
                    strokeWidth = 1f,
                    style = MaterialTheme.typography.titleSmall,
                    shadowColor = Color.Black,
                    shadowOffset = Offset(1f, 1f),
                    shadowBlurRadius = 1f
                )
            }
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}