package com.example.mybrainlive.bluetooth

enum class EegDeviceType(
    val displayName: String,
    val manufacturer: String,
) {
    PLUX("PLUX Revolution BT", "PLUX Biosignals"),
    MINDWAVE_MOBILE("MindWave Mobile 2", "NeuroSky"),
    BRAINLINK_LITE("BrainLink Lite V2.0", "Macrotellect"),
    GENERIC("Capteur Bluetooth SPP", "Générique");

    companion object {
        fun detectType(deviceName: String?): EegDeviceType {
            if ((deviceName == null) || deviceName.trim().isEmpty()) return GENERIC
            val lower = deviceName.lowercase()
            return when {
                lower.contains("plux") ||
                lower.contains("bitalino") ||
                lower.contains("biosignals") ||
                lower.contains("bt-block") ||
                lower.contains("revolution") -> PLUX

                lower.contains("mindwave") ||
                lower.contains("neurosky") ||
                lower.contains("mw2") ||
                lower.contains("mw-") -> MINDWAVE_MOBILE

                lower.contains("brainlink") ||
                lower.contains("macrotellect") ||
                lower.contains("bl_") ||
                lower.contains("lite") -> BRAINLINK_LITE

                else -> GENERIC
            }
        }
    }
}


