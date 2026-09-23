package com.example.bridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.ActionResult
import com.example.model.ContactMatch
import java.net.URLEncoder

class AndroidActionManager(private val context: Context) {

    fun openWhatsApp(): ActionResult {
        val pm = context.packageManager
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

        for (pkg in whatsappPackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(intent)
                    ActionResult(
                        actionType = "openWhatsApp",
                        isSuccess = true,
                        message = "WhatsApp opened successfully.",
                        details = "Package: $pkg"
                    )
                } catch (e: Exception) {
                    ActionResult(
                        actionType = "openWhatsApp",
                        isSuccess = false,
                        message = "Failed to launch WhatsApp: ${e.localizedMessage}"
                    )
                }
            }
        }

        // Fallback: try web/deep link
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ActionResult(
                actionType = "openWhatsApp",
                isSuccess = true,
                message = "WhatsApp app not found; opened WhatsApp web.",
                details = "Deep link: https://wa.me/"
            )
        } catch (e: Exception) {
            ActionResult(
                actionType = "openWhatsApp",
                isSuccess = false,
                message = "WhatsApp is not installed on this device."
            )
        }
    }

    fun openApp(appName: String): ActionResult {
        val trimmed = appName.trim().lowercase()

        // 1. Check known common app aliases
        when {
            trimmed.contains("whatsapp") -> return openWhatsApp()
            trimmed.contains("youtube") -> return launchPackageOrFallback(
                "com.google.android.youtube",
                "https://m.youtube.com",
                "YouTube"
            )
            trimmed.contains("instagram") -> return launchPackageOrFallback(
                "com.instagram.android",
                "https://instagram.com",
                "Instagram"
            )
            trimmed.contains("chrome") -> return launchPackageOrFallback(
                "com.android.chrome",
                "https://google.com",
                "Google Chrome"
            )
            trimmed.contains("settings") || trimmed.contains("setting") -> {
                return try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = true,
                        message = "Device Settings opened.",
                        details = "Action: ACTION_SETTINGS"
                    )
                } catch (e: Exception) {
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = false,
                        message = "Failed to open Settings: ${e.localizedMessage}"
                    )
                }
            }
            trimmed.contains("camera") -> {
                return try {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = true,
                        message = "Camera opened.",
                        details = "Action: ACTION_IMAGE_CAPTURE"
                    )
                } catch (e: Exception) {
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = false,
                        message = "Failed to open Camera: ${e.localizedMessage}"
                    )
                }
            }
            trimmed.contains("map") -> return launchPackageOrFallback(
                "com.google.android.apps.maps",
                "https://maps.google.com",
                "Google Maps"
            )
            trimmed.contains("spotify") -> return launchPackageOrFallback(
                "com.spotify.music",
                "https://open.spotify.com",
                "Spotify"
            )
            trimmed.contains("gmail") || trimmed.contains("mail") -> return launchPackageOrFallback(
                "com.google.android.gm",
                "https://mail.google.com",
                "Gmail"
            )
            trimmed.contains("clock") || trimmed.contains("alarm") -> {
                return try {
                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = true,
                        message = "Clock / Alarm opened.",
                        details = "Action: ACTION_SHOW_ALARMS"
                    )
                } catch (e: Exception) {
                    ActionResult(
                        actionType = "openApp",
                        isSuccess = false,
                        message = "Failed to open Clock: ${e.localizedMessage}"
                    )
                }
            }
        }

        // 2. Query all installed launchable apps and search by label
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        for (resolveInfo in resolveInfos) {
            val appLabel = resolveInfo.loadLabel(pm).toString()
            if (appLabel.lowercase().contains(trimmed) || trimmed.contains(appLabel.lowercase())) {
                val pkgName = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return try {
                        context.startActivity(launchIntent)
                        ActionResult(
                            actionType = "openApp",
                            isSuccess = true,
                            message = "$appLabel opened successfully.",
                            details = "Package: $pkgName"
                        )
                    } catch (e: Exception) {
                        ActionResult(
                            actionType = "openApp",
                            isSuccess = false,
                            message = "Error launching $appLabel: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }

        return ActionResult(
            actionType = "openApp",
            isSuccess = false,
            message = "App '$appName' was not found on this device."
        )
    }

    private fun launchPackageOrFallback(packageName: String, fallbackUrl: String, appDisplayName: String): ActionResult {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(launchIntent)
                ActionResult(
                    actionType = "openApp",
                    isSuccess = true,
                    message = "$appDisplayName opened.",
                    details = "Package: $packageName"
                )
            } catch (e: Exception) {
                ActionResult(
                    actionType = "openApp",
                    isSuccess = false,
                    message = "Could not open $appDisplayName: ${e.localizedMessage}"
                )
            }
        }

        // Browser fallback
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult(
                actionType = "openApp",
                isSuccess = true,
                message = "$appDisplayName app not installed; opened in browser.",
                details = "Fallback URL: $fallbackUrl"
            )
        } catch (e: Exception) {
            ActionResult(
                actionType = "openApp",
                isSuccess = false,
                message = "$appDisplayName is not installed on this device."
            )
        }
    }

    fun makeCall(phoneNumber: String): ActionResult {
        val sanitizedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (sanitizedNumber.isBlank()) {
            return ActionResult(
                actionType = "makeCall",
                isSuccess = false,
                message = "Invalid phone number provided: '$phoneNumber'"
            )
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            // Per Safety requirement 7: If CALL_PHONE is permitted, we can initiate directly or open dialer safely
            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$sanitizedNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitizedNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            ActionResult(
                actionType = "makeCall",
                isSuccess = true,
                message = "Calling $sanitizedNumber.",
                details = if (hasCallPermission) "Direct call initiated" else "Phone dialer opened with number"
            )
        } catch (e: Exception) {
            ActionResult(
                actionType = "makeCall",
                isSuccess = false,
                message = "Failed to initiate call: ${e.localizedMessage}"
            )
        }
    }

    fun callContact(contactName: String): ActionResult {
        val trimmed = contactName.trim()
        if (trimmed.isBlank()) {
            return ActionResult(
                actionType = "callContact",
                isSuccess = false,
                message = "Please specify a contact name to call."
            )
        }

        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) {
            return ActionResult(
                actionType = "callContact",
                isSuccess = false,
                message = "Contacts permission is required to search your contacts. Please grant Contacts permission.",
                details = "Permission READ_CONTACTS not granted"
            )
        }

        val matches = queryContacts(trimmed)

        return when {
            matches.isEmpty() -> {
                ActionResult(
                    actionType = "callContact",
                    isSuccess = false,
                    message = "I could not find any contact matching '$trimmed' in your contacts.",
                    details = "0 matches found"
                )
            }
            matches.size == 1 -> {
                val match = matches.first()
                val callResult = makeCall(match.phoneNumber)
                ActionResult(
                    actionType = "callContact",
                    isSuccess = callResult.isSuccess,
                    message = "Calling ${match.name} (${match.phoneNumber}).",
                    details = "Exact match: ${match.name}",
                    matchedContacts = matches
                )
            }
            else -> {
                // Multiple matches - prompt for clarification
                val namesList = matches.map { "${it.name} (${it.phoneNumber})" }.joinToString(", ")
                ActionResult(
                    actionType = "callContact",
                    isSuccess = false,
                    message = "I found ${matches.size} contacts matching '$trimmed': $namesList. Which one would you like me to call?",
                    details = "Multiple matches (${matches.size})",
                    matchedContacts = matches
                )
            }
        }
    }

    private fun queryContacts(query: String): List<ContactMatch> {
        val results = mutableListOf<ContactMatch>()
        val seenNumbers = mutableSetOf<String>()

        // Generate aliases for relational terms
        val searchTerms = mutableListOf(query.lowercase())
        when (query.lowercase()) {
            "mom", "mummy", "mami", "maa", "mother", "ammi", "amma" -> {
                searchTerms.addAll(listOf("mom", "mummy", "maa", "mother", "amma", "ammi"))
            }
            "dad", "papa", "daddy", "father", "pitaji", "abbu", "appa" -> {
                searchTerms.addAll(listOf("dad", "papa", "father", "daddy", "abbu", "appa"))
            }
            "bhai", "brother", "bhaiya", "bro" -> {
                searchTerms.addAll(listOf("bhai", "brother", "bhaiya", "bro"))
            }
            "didi", "sister", "behen", "sis" -> {
                searchTerms.addAll(listOf("didi", "sister", "behen", "sis"))
            }
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                while (c.moveToNext()) {
                    val name = if (nameIdx != -1) c.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx != -1) c.getString(numberIdx) ?: "" else ""
                    val contactId = if (idIdx != -1) c.getString(idIdx) ?: "" else ""

                    val lowerName = name.lowercase()
                    val matchesAny = searchTerms.any { term ->
                        lowerName.contains(term) || term.contains(lowerName)
                    }

                    if (matchesAny && number.isNotBlank()) {
                        val cleanNum = number.replace(Regex("[^0-9+]"), "")
                        if (!seenNumbers.contains(cleanNum)) {
                            seenNumbers.add(cleanNum)
                            results.add(ContactMatch(name = name, phoneNumber = number, contactId = contactId))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun openUrl(url: String): ActionResult {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            return ActionResult(
                actionType = "openUrl",
                isSuccess = false,
                message = "No URL provided."
            )
        }

        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                actionType = "openUrl",
                isSuccess = true,
                message = "Opened $cleanUrl in browser.",
                details = cleanUrl
            )
        } catch (e: Exception) {
            ActionResult(
                actionType = "openUrl",
                isSuccess = false,
                message = "Failed to open URL: ${e.localizedMessage}"
            )
        }
    }
}
