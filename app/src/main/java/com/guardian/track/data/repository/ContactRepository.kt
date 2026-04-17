package com.guardian.track.data.repository

import com.guardian.track.data.local.dao.EmergencyContactDao
import com.guardian.track.data.local.entity.EmergencyContactEntity
import com.guardian.track.data.mapper.toDomain
import com.guardian.track.data.mapper.toDomainContactList
import com.guardian.track.data.mapper.toEntity
import com.guardian.track.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: EmergencyContactDao
) {
    fun getAllContacts(): Flow<List<EmergencyContact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.toDomainContactList()
        }
    }

    suspend fun addContact(contact: EmergencyContact) {
        contactDao.insertContact(contact.toEntity())
    }

    suspend fun deleteContact(id: Long) {
        contactDao.deleteContact(id)
    }
}
