package com.example.mybrainlive.bluetooth

data class PluxFrame(
    val sequence: Int,
    val digitalInputs: List<Int>,
    val analogChannels: List<Int>,
    val rawBytesHex: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTelemetry(): EegTelemetry {
        return EegTelemetry(
            deviceType = EegDeviceType.PLUX,
            signalQuality = 0,
            pluxChannels = analogChannels,
            sequenceNumber = sequence,
            rawBytesHex = rawBytesHex,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * Parses a raw byte buffer chunk into a PluxFrame structure or raw frame preview.
         * PLUX Revolution / BITalino standard frame format:
         * - Last byte contains sequence number in upper bits (seq = (crcSeq >> 4) & 0x0F)
         * - Analog channels mapped across multi-byte packed bits.
         */
        fun parse(bytes: ByteArray, length: Int): PluxFrame {
            val hexString = bytes.take(length).joinToString(" ") { String.format("%02X", it) }
            val seq = if (length > 0) (bytes[length - 1].toInt() shr 4) and 0x0F else 0

            // Extract basic analog channel approximations if standard multi-byte packet
            val channels = mutableListOf<Int>()
            if (length >= 4) {
                for (i in 0 until (length - 1) step 2) {
                    if (i + 1 < length) {
                        val val16 = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                        channels.add(val16)
                    }
                }
            }

            val digital = listOf(
                if (length > 0) (bytes[0].toInt() shr 7) and 0x01 else 0,
                if (length > 0) (bytes[0].toInt() shr 6) and 0x01 else 0
            )

            return PluxFrame(
                sequence = seq,
                digitalInputs = digital,
                analogChannels = channels,
                rawBytesHex = hexString
            )
        }
    }
}
