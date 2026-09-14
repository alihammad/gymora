package com.gymora.data.local

import com.gymora.domain.model.EquipmentItem
import com.gymora.domain.model.EquipmentType
import com.gymora.domain.model.EquipmentUsageType
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.MuscleGroupRef
import com.gymora.domain.model.MuscleGroupType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Codec for the structured exercise metadata (instructions / muscle groups /
 * equipment) persisted as JSON text columns on [com.gymora.data.local.entity.ExerciseEntity].
 *
 * Uses the Android org.json implementation (no extra dependency) and stores enum
 * values by name so the encoded form is stable and human-readable.
 */
object ExerciseMetadataCodec {

    // --- Instructions ------------------------------------------------------

    fun encodeInstructions(instructions: List<String>): String? =
        if (instructions.isEmpty()) null else JSONArray(instructions).toString()

    fun decodeInstructions(json: String?): List<String> =
        json?.let { raw ->
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } ?: emptyList()

    // --- Muscle groups -----------------------------------------------------

    fun encodeMuscleGroups(groups: List<MuscleGroupRef>): String? =
        if (groups.isEmpty()) {
            null
        } else {
            JSONArray(groups.map { group ->
                JSONObject()
                    .put("group", group.group.name)
                    .put("type", group.type.name)
            }).toString()
        }

    fun decodeMuscleGroups(json: String?): List<MuscleGroupRef> =
        json?.let { raw ->
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.getJSONObject(index)
                val group = runCatching { MuscleGroup.valueOf(obj.getString("group")) }.getOrNull()
                val type = runCatching { MuscleGroupType.valueOf(obj.getString("type")) }.getOrNull()
                if (group != null && type != null) MuscleGroupRef(group, type) else null
            }
        } ?: emptyList()

    // --- Equipment ---------------------------------------------------------

    fun encodeEquipment(items: List<EquipmentItem>): String? =
        if (items.isEmpty()) {
            null
        } else {
            JSONArray(items.map { item ->
                JSONObject()
                    .put("name", item.name)
                    .put("type", item.type.name)
                    .put("usageType", item.usageType.name)
            }).toString()
        }

    fun decodeEquipment(json: String?): List<EquipmentItem> =
        json?.let { raw ->
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.getJSONObject(index)
                val type = runCatching { EquipmentType.valueOf(obj.getString("type")) }.getOrNull()
                val usage = runCatching {
                    EquipmentUsageType.valueOf(obj.getString("usageType"))
                }.getOrNull()
                if (type != null && usage != null) {
                    EquipmentItem(name = obj.getString("name"), type = type, usageType = usage)
                } else {
                    null
                }
            }
        } ?: emptyList()
}
