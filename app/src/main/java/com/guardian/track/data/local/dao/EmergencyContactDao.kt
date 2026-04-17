package com.guardian.track.data.local.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardian.track.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    suspend fun getAllContactsList(): List<EmergencyContactEntity>

    // For ContentProvider: returns a Cursor for cross-process queries
    @Query("SELECT id AS _id, name, phoneNumber AS phone_number FROM emergency_contacts ORDER BY name ASC")
    fun getAllContactsCursor(): Cursor

    @Query("SELECT id AS _id, name, phoneNumber AS phone_number FROM emergency_contacts WHERE id = :id")
    fun getContactCursorById(id: Long): Cursor
}
