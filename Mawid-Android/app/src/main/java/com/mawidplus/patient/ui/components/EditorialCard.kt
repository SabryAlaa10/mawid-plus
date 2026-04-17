package com.mawidplus.patient.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

/**
 * بطاقة بزوايا غير متماثلة (مطابقة لتصميم Editorial) مع ظل خفيف جداً.
 */
@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 32.dp,
        bottomStart = 32.dp,
        bottomEnd = 16.dp
    )
    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = androidx.compose.ui.graphics.Color(0xFF191C1D).copy(alpha = 0.06f),
                spotColor = androidx.compose.ui.graphics.Color(0xFF191C1D).copy(alpha = 0.06f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        content = content
    )
}
