package com.tailormyresume.core.database.util

import androidx.room.TypeConverter
import com.tailormyresume.core.database.json.EvidenceBulletDto
import com.tailormyresume.core.database.json.GapAnalysisDto
import com.tailormyresume.core.database.json.JobRequirementDto
import com.tailormyresume.core.database.json.TailoredResumeDto
import com.tailormyresume.core.database.json.TmrJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

internal class JsonConverters {
    @TypeConverter
    fun ledgerKindToString(kind: com.tailormyresume.core.model.CreditLedgerKind): String = kind.name

    @TypeConverter
    fun stringToLedgerKind(value: String): com.tailormyresume.core.model.CreditLedgerKind =
        com.tailormyresume.core.model.CreditLedgerKind.entries.firstOrNull { it.name == value } ?: com.tailormyresume.core.model.CreditLedgerKind.UNKNOWN

    @TypeConverter
    fun stringListToJson(value: List<String>): String =
        TmrJson.encode(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(json: String): List<String> =
        TmrJson.decode(ListSerializer(String.serializer()), json)

    @TypeConverter
    fun evidenceBulletsToJson(value: List<EvidenceBulletDto>): String =
        TmrJson.encode(ListSerializer(EvidenceBulletDto.serializer()), value)

    @TypeConverter
    fun jsonToEvidenceBullets(json: String): List<EvidenceBulletDto> =
        TmrJson.decode(ListSerializer(EvidenceBulletDto.serializer()), json)

    @TypeConverter
    fun jobRequirementsToJson(value: List<JobRequirementDto>): String =
        TmrJson.encode(ListSerializer(JobRequirementDto.serializer()), value)

    @TypeConverter
    fun jsonToJobRequirements(json: String): List<JobRequirementDto> =
        TmrJson.decode(ListSerializer(JobRequirementDto.serializer()), json)

    @TypeConverter
    fun gapAnalysisToJson(value: GapAnalysisDto?): String? =
        value?.let { TmrJson.encode(GapAnalysisDto.serializer(), it) }

    @TypeConverter
    fun jsonToGapAnalysis(json: String?): GapAnalysisDto? =
        json?.let { TmrJson.decode(GapAnalysisDto.serializer(), it) }

    @TypeConverter
    fun tailoredResumeToJson(value: TailoredResumeDto?): String? =
        value?.let { TmrJson.encode(TailoredResumeDto.serializer(), it) }

    @TypeConverter
    fun jsonToTailoredResume(json: String?): TailoredResumeDto? =
        json?.let { TmrJson.decode(TailoredResumeDto.serializer(), it) }
}
