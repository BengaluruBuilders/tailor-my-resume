package com.tailormyresume.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tailormyresume.core.database.json.GapAnalysisDto
import com.tailormyresume.core.database.json.JobRequirementDto
import com.tailormyresume.core.database.json.TailoredResumeDto
import com.tailormyresume.core.model.ApplicationStatus
import kotlin.time.Instant

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey
    val id: String,
    val jobTitle: String,
    val company: String,
    val rawText: String,
    val requirements: List<JobRequirementDto>,
    val status: ApplicationStatus,
    val notes: String = "",
    val gapAnalysis: GapAnalysisDto?,
    val tailoredResume: TailoredResumeDto?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val legacyStatus: String? = null,
    @ColumnInfo(defaultValue = "''")
    val location: String = "",
    val appliedOn: Instant? = null,
    val coverageNow: Int? = null,
    val coverageUpTo: Int? = null,
    val coverageFinal: Int? = null,
    val exportFileName: String? = null,
    val quickAnswerRequirementId: String? = null,
    val quickAnswerChoice: String? = null,
    val quickAnswerDetail: String? = null,
    val changesAcceptedAt: Instant? = null,
)
