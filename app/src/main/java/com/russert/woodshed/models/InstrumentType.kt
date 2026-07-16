package com.russert.woodshed.models

enum class InstrumentType(val displayName: String, val emoji: String) {
    BANJO("Banjo", "🪘"),
    FIDDLE("Fiddle", "🎻"),
    GUITAR("Guitar", "🎸"),
    MANDOLIN("Mandolin", "🎵"),
    DOBRO("Dobro", "🎸"),
    BASS("Bass", "🎸"),
    DULCIMER("Dulcimer", "🎵"),
    AUTOHARP("Autoharp", "🎵"),
    OTHER("Other", "🎵");

    companion object {
        fun fromDisplayName(name: String?): InstrumentType =
            entries.firstOrNull { it.displayName == name } ?: OTHER
    }
}
