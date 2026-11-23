package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import java.net.URLEncoder

object ShareAndIntents {
    private const val CHANNEL_ID = "khatamitra_reminders"

    fun buildReminderMessage(
        customerName: String,
        balance: Double,
        businessName: String
    ): String {
        return "Hello $customerName, your outstanding balance is ${Formatters.formatRupee(balance)} with $businessName. Kindly make your payment when convenient. Thank you for your business."
    }

    fun shareTextViaWhatsAppOrSystem(context: Context, phone: String?, message: String) {
        val cleanPhone = phone?.filter { it.isDigit() }
        if (!cleanPhone.isNullOrBlank() && cleanPhone.length >= 10) {
            val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            val encoded = URLEncoder.encode(message, "UTF-8")
            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=$encoded"))
            try {
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // Fall back to system share sheet
            }
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
        } catch (_: Exception) {
            Toast.makeText(context, "No sharing app available", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSmsReminder(context: Context, phone: String?, message: String) {
        val uri = if (!phone.isNullOrBlank()) Uri.parse("smsto:$phone") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialPhone(context: Context, phone: String?) {
        if (phone.isNullOrBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFileUri(context: Context, uri: Uri, mimeType: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, title))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to share file", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchUpiPayIntent(context: Context, upiUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
        try {
            context.startActivity(Intent.createChooser(intent, "Pay with UPI App"))
        } catch (_: Exception) {
            Toast.makeText(context, "No UPI app installed on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildUpiPaymentUri(upiId: String, businessName: String, amount: Double?, note: String): String {
        val pa = URLEncoder.encode(upiId.trim(), "UTF-8")
        val pn = URLEncoder.encode(businessName.trim().ifBlank { "KhataMitra Merchant" }, "UTF-8")
        val tn = URLEncoder.encode(note.take(40).ifBlank { "Khata Payment" }, "UTF-8")
        val amPart = if (amount != null && amount > 0) "&am=${"%.2f".format(amount)}" else ""
        return "upi://pay?pa=$pa&pn=$pn$amPart&cu=INR&tn=$tn"
    }

    fun showLocalNotification(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KhataMitra Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
