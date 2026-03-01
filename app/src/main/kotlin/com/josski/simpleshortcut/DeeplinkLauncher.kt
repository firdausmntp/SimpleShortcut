package com.josski.simpleshortcut

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Handles launching any kind of shortcut intent:
 *
 * | packageName | deeplink         | Behaviour                                     |
 * |-------------|------------------|-----------------------------------------------|
 * | filled      | filled           | ACTION_VIEW with deeplink + setPackage target |
 * | filled      | empty            | Launch app home screen via getLaunchIntent    |
 * | empty       | filled (http/s)  | Open URL in default browser / handler         |
 * | empty       | filled (scheme)  | Broadcast to any app that handles the scheme  |
 * | empty       | empty            | No-op / toast                                 |
 */
object DeeplinkLauncher {

    fun launch(context: Context, packageName: String, deeplink: String) {
        val intent = buildIntent(context, packageName.trim(), deeplink.trim())
        if (intent == null) {
            Toast.makeText(context, "Deeplink atau package name tidak boleh kosong semua", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (packageName.isNotBlank()) {
                openPlayStore(context, packageName.trim())
            } else {
                Toast.makeText(context, context.getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Tidak punya izin untuk membuka app ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildIntent(context: Context, pkg: String, link: String): Intent? {
        return when {
            // Case 1: Both filled — explicit deeplink to specific package
            pkg.isNotBlank() && link.isNotBlank() -> {
                try {
                    Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                        setPackage(pkg)
                    }
                } catch (e: Exception) {
                    // Malformed URI — fall back to launch intent
                    context.packageManager.getLaunchIntentForPackage(pkg)
                }
            }
            // Case 2: Package only — open home screen of the target app
            pkg.isNotBlank() && link.isBlank() -> {
                context.packageManager.getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(pkg)
                    }
            }
            // Case 3: Deeplink only (no package) — let the OS resolve to any handler
            pkg.isBlank() && link.isNotBlank() -> {
                try {
                    Intent(Intent.ACTION_VIEW, Uri.parse(link))
                } catch (e: Exception) {
                    null
                }
            }
            // Case 4: Both empty
            else -> null
        }
    }

    private fun openPlayStore(context: Context, packageName: String) {
        Toast.makeText(context, context.getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show()
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}