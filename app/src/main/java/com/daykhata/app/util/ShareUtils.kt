package com.daykhata.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object ShareUtils {

    private const val PACKAGE_WHATSAPP = "com.whatsapp"
    private const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Shares home message to WhatsApp (personal or business), or falls back to system share chooser.
     */
    fun shareToWhatsApp(context: Context, message: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        when {
            isPackageInstalled(context, PACKAGE_WHATSAPP) -> {
                sendIntent.setPackage(PACKAGE_WHATSAPP)
                context.startActivity(sendIntent)
            }
            isPackageInstalled(context, PACKAGE_WHATSAPP_BUSINESS) -> {
                sendIntent.setPackage(PACKAGE_WHATSAPP_BUSINESS)
                context.startActivity(sendIntent)
            }
            else -> {
                val chooser = Intent.createChooser(sendIntent, "Share Daily Expenses")
                context.startActivity(chooser)
            }
        }
    }

    /**
     * Sends message directly to a saved phone number via WhatsApp API wa.me link.
     */
    fun sendToWhatsAppNumber(context: Context, phone: String, message: String) {
        val encodedMessage = Uri.encode(message)
        val cleanPhone = phone.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMessage")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        when {
            isPackageInstalled(context, PACKAGE_WHATSAPP) -> {
                intent.setPackage(PACKAGE_WHATSAPP)
            }
            isPackageInstalled(context, PACKAGE_WHATSAPP_BUSINESS) -> {
                intent.setPackage(PACKAGE_WHATSAPP_BUSINESS)
            }
        }
        context.startActivity(intent)
    }

    /**
     * Copies text to Android clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "DayKhata Expenses") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
