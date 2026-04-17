package com.mawidplus.patient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ErrorCard(
    message: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error",
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = Color(0xFFB71C1C)
            )
            onRetry?.let {
                androidx.compose.material3.Button(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
