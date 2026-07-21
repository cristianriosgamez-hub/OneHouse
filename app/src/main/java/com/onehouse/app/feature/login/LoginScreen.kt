package com.onehouse.app.feature.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseFingerprintRow
import com.onehouse.app.design.OneHousePrimaryButton
import com.onehouse.app.design.OneHouseTextField
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario

@Composable
fun LoginScreen(
    onLoginCorrecto: () -> Unit
) {
    val context = LocalContext.current

    var usuario by rememberSaveable {
        mutableStateOf("knxuser")
    }

    var contrasena by rememberSaveable {
        mutableStateOf("")
    }

    var recordarCredenciales by rememberSaveable {
        mutableStateOf(true)
    }

    var mostrarContrasena by rememberSaveable {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FondoSuperior,
                        FondoInferior,
                        Color.Black
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "⌂",
                color = AzulClaro,
                fontSize = 62.sp,
                fontWeight = FontWeight.Light
            )

            Text(
                text = "OneHouse",
                color = TextoPrincipal,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Tu casa, inteligente.",
                color = AzulClaro,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Bienvenido",
                color = TextoPrincipal,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Inicia sesión para acceder a tu instalación",
                color = TextoSecundario,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OneHouseTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                    },
                    label = "Usuario"
                )

                OneHouseTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                    },
                    label = "Contraseña",
                    visualTransformation = if (mostrarContrasena) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingContent = {
                        IconButton(
                            onClick = {
                                mostrarContrasena = !mostrarContrasena
                            }
                        ) {
                            Text(
                                text = if (mostrarContrasena) {
                                    "🙈"
                                } else {
                                    "👁"
                                },
                                fontSize = 20.sp
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordarCredenciales,
                        onCheckedChange = {
                            recordarCredenciales = it
                        }
                    )

                    Text(
                        text = "Recordar mis credenciales",
                        color = TextoPrincipal,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OneHousePrimaryButton(
                text = "Iniciar sesión",
                onClick = {
                    if (
                        usuario.trim() == "knxuser" &&
                        contrasena == "onehouse"
                    ) {
                        Toast.makeText(
                            context,
                            "Bienvenido a OneHouse",
                            Toast.LENGTH_SHORT
                        ).show()

                        onLoginCorrecto()
                    } else {
                        Toast.makeText(
                            context,
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "o",
                color = TextoSecundario,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OneHouseFingerprintRow(
                title = "Iniciar sesión con huella",
                subtitle = "Disponible próximamente",
                onClick = {
                    Toast.makeText(
                        context,
                        "La huella se activará próximamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = "OneHouse v1.1.8",
                color = TextoDesactivado,
                fontSize = 12.sp
            )
        }
    }
}