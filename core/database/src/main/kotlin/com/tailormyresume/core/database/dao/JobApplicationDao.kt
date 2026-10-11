package com.tailormyresume.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY updatedAt DESC, id ASC")
    fun observeApplications(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    fun observeApplication(id: String): Flow<JobApplicationEntity?>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    suspend fun getApplication(id: String): JobApplicationEntity?

    @Upsert
    suspend fun upsertApplication(application: JobApplicationEntity)

    @Transaction
    suspend fun upsertKeepingLegacyColumns(application: JobApplicationEntity) {
        val stored = getApplication(application.id)
        upsertApplication(
            if (stored == null) application else application.copy(notes = stored.notes, legacyStatus = stored.legacyStatus),
        )
    }

    @Query("UPDATE job_applications SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ApplicationStatus, updatedAt: Instant)

    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteApplication(id: String)
}
