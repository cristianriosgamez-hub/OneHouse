package com.onehouse.app.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.R

@Composable
fun OneHouseCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable {
            onClick()
        }
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        color = FondoTarjeta,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = BordeTarjeta
        )
    ) {
        content()
    }
}

@Composable
fun OneHouseSecondaryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable {
            onClick()
        }
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        color = FondoTarjetaSecundaria,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = BordeTarjeta
        )
    ) {
        content()
    }
}

@Composable
fun OneHousePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AzulOneHouse,
            contentColor = TextoPrincipal,
            disabledContainerColor = TextoDesactivado,
            disabledContentColor = TextoSecundario
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OneHouseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingContent: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingContent,
        shape = RoundedCornerShape(14.dp),
        colors = oneHouseTextFieldColors()
    )
}

@Composable
fun OneHouseInfoCard(
    title: String,
    value: String,
    detail: String,
    symbol: String,
    modifier: Modifier = Modifier,
    symbolColor: Color = AzulClaro,
    valueColor: Color = TextoPrincipal,
    onClick: (() -> Unit)? = null
) {
    OneHouseCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = symbol,
                color = symbolColor,
                fontSize = 26.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                color = TextoSecundario,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = detail,
                color = TextoDesactivado,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun OneHouseSceneCard(
    name: String,
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OneHouseSecondaryCard(
        modifier = modifier.height(94.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = symbol,
                color = AzulClaro,
                fontSize = 25.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = name,
                color = TextoPrincipal,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OneHouseStatusItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextoPrincipal
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextoSecundario,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OneHouseSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        color = TextoPrincipal,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun OneHouseHeader(
    title: String,
    subtitle: String,
    badgeText: String? = "OH",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = TextoPrincipal,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = TextoSecundario,
                fontSize = 15.sp
            )
        }

        if (!badgeText.isNullOrBlank()) {
            Surface(
                color = FondoTarjetaSecundaria,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    width = 1.dp,
                    color = BordeTarjeta
                )
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(12.dp),
                    color = AzulClaro,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OneHouseFingerprintRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OneHouseCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.fingerprint_modern),
                contentDescription = "Acceso con huella",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color = AzulClaro,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun oneHouseTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AzulOneHouse,
        unfocusedBorderColor = BordeTarjeta,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedLabelColor = AzulOneHouse,
        unfocusedLabelColor = TextoSecundario,
        cursorColor = AzulOneHouse
    )
}