package com.joaolucas.spendguard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidEmail(email: String) = EMAIL_REGEX.matches(email.trim())

@Composable
fun AuthScreen(
    userRepository: UserRepository,
    onAuthSuccess: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }

    if (isLogin) {
        LoginScreen(
            userRepository  = userRepository,
            onAuthSuccess   = onAuthSuccess,
            onSwitchToRegister = { isLogin = false }
        )
    } else {
        RegisterScreen(
            userRepository = userRepository,
            onAuthSuccess  = onAuthSuccess,
            onSwitchToLogin = { isLogin = true }
        )
    }
}

@Composable
fun LoginScreen(
    userRepository: UserRepository,
    onAuthSuccess: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val gold  = Color(0xFFFFD700)
    val black = Color(0xFF121212)

    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading        by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Bem-vindo de volta",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Entre para acessar seu guardião financeiro",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = gold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = email.isNotEmpty() && !isValidEmail(email),
                supportingText = {
                    if (email.isNotEmpty() && !isValidEmail(email))
                        Text("E-mail inválido", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = outlinedTextFieldColorsGold(gold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = gold) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = gold.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColorsGold(gold)
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() ->
                            errorMessage = "Preencha todos os campos"
                        !isValidEmail(email) ->
                            errorMessage = "Digite um e-mail válido"
                        else -> {
                            loading = true
                            scope.launch {
                                val result = userRepository.signInWithEmail(email.trim(), password)
                                loading = false
                                result.fold(
                                    onSuccess = { onAuthSuccess() },
                                    onFailure = { errorMessage = friendlyError(it.message) }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = black, strokeWidth = 2.dp)
                } else {
                    Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                Text("  ou  ", color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    loading = true
                    scope.launch {
                        val result = userRepository.signInWithGoogle { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        loading = false
                        result.onFailure { errorMessage = friendlyError(it.message) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !loading,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.4f))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar com Google", color = Color.White, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Não tem conta? ", color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSwitchToRegister) {
                    Text("Criar conta", color = gold, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RegisterScreen(
    userRepository: UserRepository,
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val gold  = Color(0xFFFFD700)
    val black = Color(0xFF121212)

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading         by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Criar sua conta",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Seus dados sincronizados em todos os dispositivos",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = gold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = email.isNotEmpty() && !isValidEmail(email),
                supportingText = {
                    if (email.isNotEmpty() && !isValidEmail(email))
                        Text("E-mail inválido", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = outlinedTextFieldColorsGold(gold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Senha (mínimo 8 caracteres)") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = gold) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = gold.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = password.isNotEmpty() && password.length < 8,
                supportingText = {
                    if (password.isNotEmpty() && password.length < 8)
                        Text("Senha muito curta", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColorsGold(gold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirmar senha") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = gold) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && confirmPassword != password)
                        Text("Senhas não coincidem", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColorsGold(gold)
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() ->
                            errorMessage = "Preencha todos os campos"
                        !isValidEmail(email) ->
                            errorMessage = "Digite um e-mail válido"
                        password.length < 8 ->
                            errorMessage = "Senha deve ter pelo menos 8 caracteres"
                        password != confirmPassword ->
                            errorMessage = "As senhas não coincidem"
                        else -> {
                            loading = true
                            scope.launch {
                                val result = userRepository.signUpWithEmail(email.trim(), password)
                                loading = false
                                result.fold(
                                    onSuccess = { onAuthSuccess() },
                                    onFailure = { errorMessage = friendlyError(it.message) }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = black, strokeWidth = 2.dp)
                } else {
                    Text("Criar conta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Já tem conta? ", color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSwitchToLogin) {
                    Text("Entrar", color = gold, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun outlinedTextFieldColorsGold(gold: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor     = gold,
    focusedLabelColor      = gold,
    focusedLeadingIconColor = gold,
    cursorColor            = gold,
    unfocusedBorderColor   = Color.White.copy(alpha = 0.3f),
    unfocusedLabelColor    = Color.White.copy(alpha = 0.5f),
    focusedTextColor       = Color.White,
    unfocusedTextColor     = Color.White
)

fun friendlyError(message: String?): String {
    return when {
        message == null -> "Erro desconhecido. Tente novamente."
        message.contains("Invalid login credentials")  -> "E-mail ou senha incorretos."
        message.contains("Email not confirmed")         -> "Confirme seu e-mail antes de entrar."
        message.contains("User already registered")    -> "Este e-mail já está cadastrado."
        message.contains("Password should be at least") -> "Senha deve ter pelo menos 8 caracteres."
        message.contains("Unable to validate email")   -> "E-mail inválido."
        message.contains("network") || message.contains("connect") -> "Sem conexão. Verifique sua internet."
        else -> "Erro ao autenticar. Tente novamente."
    }
}