package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LedgerTransaction
import com.example.data.model.PartyType
import com.example.data.model.TxnStatus
import com.example.data.model.TxnType
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.util.Formatters
import com.example.util.QrCodeGenerator
import kotlin.math.abs

@Composable
fun BalanceChip(
    balance: Double,
    isCustomer: Boolean,
    modifier: Modifier = Modifier
) {
    val khataColors = LocalKhataColors.current
    val (label, bg, fg) = when {
        abs(balance) < 0.01 -> Triple(
            "Settled",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        isCustomer && balance > 0 -> Triple(
            "You'll Get",
            khataColors.creditGreenContainer,
            khataColors.creditGreen
        )
        isCustomer && balance < 0 -> Triple(
            "You'll Give",
            khataColors.debitRedContainer,
            khataColors.debitRed
        )
        !isCustomer && balance > 0 -> Triple(
            "Payable",
            khataColors.debitRedContainer,
            khataColors.debitRed
        )
        else -> Triple(
            "Advance",
            khataColors.creditGreenContainer,
            khataColors.creditGreen
        )
    }

    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        Text(
            text = Formatters.formatRupee(abs(balance)),
            style = moneyTextStyle(16),
            color = fg
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.img_empty_ledger,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun QrCodeCanvas(
    payload: String,
    size: Dp = 210.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { QrCodeGenerator.encode(payload) }
    val moduleColor = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
            .size(size)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.size(size - 28.dp)) {
            val dim = matrix.size
            val cellSize = this.size.width / dim
            for (y in 0 until dim) {
                for (x in 0 until dim) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = moduleColor,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    txn: LedgerTransaction,
    onClick: () -> Unit
) {
    val khataColors = LocalKhataColors.current
    val txnType = TxnType.fromRaw(txn.txnType)
    val partyType = PartyType.fromRaw(txn.partyType)
    val isInflow = when (txnType) {
        TxnType.PAYMENT_RECEIVED, TxnType.CREDIT_ADJUSTMENT -> true
        TxnType.REVERSAL -> txn.balanceImpact < 0
        else -> false
    }
    val accentColor = when {
        txn.status == TxnStatus.REVERSED.name -> MaterialTheme.colorScheme.onSurfaceVariant
        isInflow -> khataColors.creditGreen
        else -> khataColors.debitRed
    }
    val containerColor = when {
        txn.status == TxnStatus.REVERSED.name -> MaterialTheme.colorScheme.surfaceVariant
        isInflow -> khataColors.creditGreenContainer
        else -> khataColors.debitRedContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        txn.status == TxnStatus.REVERSED.name -> Icons.Filled.History
                        isInflow -> Icons.Filled.ArrowDownward
                        else -> Icons.Filled.ArrowUpward
                    },
                    contentDescription = txnType.label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = txn.partyName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (txn.status != TxnStatus.ACTIVE.name) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(khataColors.saffronGoldContainer)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = txn.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = khataColors.saffronGold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${txnType.label} • ${txn.paymentMode} • ${Formatters.formatDateTime(txn.txnTimeMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!txn.description.isNullOrBlank()) {
                    Text(
                        text = txn.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatRupee(txn.amount),
                    style = moneyTextStyle(16),
                    color = accentColor
                )
                Text(
                    text = if (partyType == PartyType.SUPPLIER) "Supplier" else "Customer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = actionLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
