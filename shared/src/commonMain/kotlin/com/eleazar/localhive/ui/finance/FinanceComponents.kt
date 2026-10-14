package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.ui.theme.HoneyInk

/** What a resident sees for their standing on a levy. No payment yet reads as "Unpaid". */
fun paymentLabel(status: PaymentStatus?): String = when (status) {
    PaymentStatus.PAID -> "Paid"
    PaymentStatus.PENDING -> "Awaiting confirmation"
    PaymentStatus.REJECTED -> "Not accepted"
    null -> "Unpaid"
}

/** Green for paid, honey while waiting, red when proof was turned down, grey when nothing's in. */
@Composable
fun paymentColor(status: PaymentStatus?): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        PaymentStatus.PAID -> MaterialTheme.colorScheme.primary
        PaymentStatus.PENDING -> if (dark) MaterialTheme.colorScheme.secondary else HoneyInk
        PaymentStatus.REJECTED -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun PaymentStatusLabel(status: PaymentStatus?, modifier: Modifier = Modifier) {
    Text(
        paymentLabel(status),
        color = paymentColor(status),
        fontSize = 14.sp,
        fontWeight = if (status == null) FontWeight.Normal else FontWeight.SemiBold,
        modifier = modifier
    )
}

/** A proof-of-payment screenshot or receipt, shown whole so amounts and names can be read. */
@Composable
fun ProofImage(url: Any, contentDescription: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    )
}
