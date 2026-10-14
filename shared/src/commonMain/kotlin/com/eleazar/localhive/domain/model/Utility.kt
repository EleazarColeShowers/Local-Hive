package com.eleazar.localhive.domain.model

/** A supply neighbours report on. [key] is the document id it's stored under. */
enum class UtilityKind(val key: String, val label: String) {
    POWER("power", "Light"),
    WATER("water", "Water");

    companion object {
        fun fromKey(key: String?): UtilityKind? = entries.firstOrNull { it.key == key }
    }
}

/** Where a supply stands, from the latest report. */
data class UtilityStatus(
    val kind: UtilityKind,
    val isOn: Boolean,
    /** When it last went on or off, as opposed to the latest report, which may only confirm it. */
    val since: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedByName: String = ""
)

/** One neighbour saying a supply is on or off. */
data class UtilityReport(
    val id: String = "",
    val kind: UtilityKind = UtilityKind.POWER,
    val isOn: Boolean = false,
    val byId: String = "",
    val byName: String = "",
    val at: Long = 0L
)
