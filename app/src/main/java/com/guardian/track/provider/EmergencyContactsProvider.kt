package com.guardian.track.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.room.Room
import com.guardian.track.data.local.GuardianDatabase
import com.guardian.track.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.runBlocking


class EmergencyContactsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.guardian.track.provider"
        const val PATH_CONTACTS = "emergency_contacts"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CONTACTS")

        private const val CODE_CONTACTS = 1
        private const val CODE_CONTACT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_CONTACTS, CODE_CONTACTS)
            addURI(AUTHORITY, "$PATH_CONTACTS/#", CODE_CONTACT_ID)
        }
    }

    private lateinit var database: GuardianDatabase

    override fun onCreate(): Boolean {
        database = Room.databaseBuilder(
            context!!.applicationContext,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        ).build()
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            CODE_CONTACTS -> {
                val cursor = database.emergencyContactDao().getAllContactsCursor()
                cursor.setNotificationUri(context?.contentResolver, uri)
                cursor
            }
            CODE_CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                val cursor = database.emergencyContactDao().getContactCursorById(id)
                cursor.setNotificationUri(context?.contentResolver, uri)
                cursor
            }
            else -> throw IllegalArgumentException("URI inconnue: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CODE_CONTACTS) {
            throw IllegalArgumentException("URI d'insertion invalide: $uri")
        }

        val name = values?.getAsString("name")
            ?: throw IllegalArgumentException("Le champ 'name' est requis")
        val phoneNumber = values.getAsString("phone_number")
            ?: throw IllegalArgumentException("Le champ 'phone_number' est requis")

        // Input sanitization: only digits in phone number
        val sanitizedPhone = phoneNumber.filter { it.isDigit() || it == '+' }

        val entity = EmergencyContactEntity(
            name = name.trim(),
            phoneNumber = sanitizedPhone
        )

        val id = runBlocking {
            database.emergencyContactDao().insertContact(entity)
        }

        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            CODE_CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                runBlocking {
                    database.emergencyContactDao().deleteContact(id)
                }
                context?.contentResolver?.notifyChange(uri, null)
                1
            }
            else -> throw IllegalArgumentException("URI de suppression invalide: $uri")
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        // Update not required by spec, but included for completeness
        throw UnsupportedOperationException("Update non supporté. Supprimez et réinsérez.")
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            CODE_CONTACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_CONTACTS"
            CODE_CONTACT_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CONTACTS"
            else -> throw IllegalArgumentException("URI inconnue: $uri")
        }
    }
}
