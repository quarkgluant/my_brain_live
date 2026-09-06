package com.example.mybrainlive.bluetooth

class NeuroSkyParser(private val deviceType: EegDeviceType) {

    private var currentTelemetry = EegTelemetry(deviceType = deviceType)
    private val buffer = ByteArray(2048)
    private var bufferHead = 0

    fun parseBytes(bytes: ByteArray, length: Int): EegTelemetry {
        // Append new bytes to ring buffer
        if ((bufferHead + length) < buffer.size) {
            System.arraycopy(bytes, 0, buffer, bufferHead, length)
            bufferHead += length
        } else {
            // Buffer overflow protection: reset
            bufferHead = 0
            return currentTelemetry
        }

        var i = 0
        while (i < (bufferHead - 3)) {
            // Check ThinkGear sync bytes: 0xAA 0xAA
            if (((buffer[i].toInt() and 0xFF) == 0xAA) && ((buffer[i + 1].toInt() and 0xFF) == 0xAA)) {
                val pLength = buffer[i + 2].toInt() and 0xFF
                if (pLength in 1..169) {
                    val packetEnd = i + 3 + pLength + 1
                    if (packetEnd <= bufferHead) {
                        // Validate checksum
                        var sum = 0
                        for (j in (i + 3) until (i + 3 + pLength)) {
                            sum += (buffer[j].toInt() and 0xFF)
                        }
                        val calculatedChecksum = (sum.inv()) and 0xFF
                        val actualChecksum = buffer[i + 3 + pLength].toInt() and 0xFF

                        if (calculatedChecksum == actualChecksum) {
                            // Extract hex preview
                            val hexString = buffer.copyOfRange(i, packetEnd)
                                .joinToString(" ") { String.format("%02X", it) }

                            // Parse payload content
                            parsePayload(buffer, i + 3, pLength, hexString)

                            // Advance buffer past packet
                            i = packetEnd
                            continue
                        }
                    } else {
                        // Incomplete packet in buffer, wait for next bytes
                        break
                    }
                }
            }
            i++
        }

        // Shift unprocessed bytes to start of buffer
        if (i in 1..bufferHead) {
            val remaining = bufferHead - i
            if (remaining > 0) {
                System.arraycopy(buffer, i, buffer, 0, remaining)
            }
            bufferHead = remaining
        }

        return currentTelemetry
    }

    private fun parsePayload(buf: ByteArray, offset: Int, length: Int, rawHex: String) {
        var idx = offset
        val end = offset + length

        var poorSignal = currentTelemetry.signalQuality
        var attention = currentTelemetry.attention
        var meditation = currentTelemetry.meditation
        var rawWave = currentTelemetry.rawWave
        var blink = currentTelemetry.blinkStrength
        var bands = currentTelemetry.eegBands

        while (idx < end) {
            val code = buf[idx].toInt() and 0xFF
            idx++

            when (code) {
                0x02 -> { // Poor Signal Quality
                    if (idx < end) {
                        poorSignal = buf[idx].toInt() and 0xFF
                        idx++
                    }
                }
                0x04 -> { // eSense Attention
                    if (idx < end) {
                        attention = buf[idx].toInt() and 0xFF
                        idx++
                    }
                }
                0x05 -> { // eSense Meditation
                    if (idx < end) {
                        meditation = buf[idx].toInt() and 0xFF
                        idx++
                    }
                }
                0x16 -> { // Blink Strength
                    if (idx < end) {
                        blink = buf[idx].toInt() and 0xFF
                        idx++
                    }
                }
                0x80 -> { // Raw Wave
                    if (idx + 2 <= end) {
                        val pLen = buf[idx].toInt() and 0xFF // Length byte (usually 2)
                        idx++
                        if (pLen == 2 && idx + 2 <= end) {
                            rawWave = ((buf[idx].toInt() and 0xFF) shl 8) or (buf[idx + 1].toInt() and 0xFF)
                            if (rawWave > 32767) rawWave -= 65536 // Convert unsigned to 16-bit signed
                            idx += 2
                        }
                    }
                }
                0x83 -> { // ASIC EEG Power Bands (8 x 3-byte unsigned integers)
                    if (idx < end) {
                        val pLen = buf[idx].toInt() and 0xFF // Length byte (usually 24)
                        idx++
                        if (pLen >= 24 && idx + 24 <= end) {
                            val delta = read24BitUnsigned(buf, idx)
                            val theta = read24BitUnsigned(buf, idx + 3)
                            val lowAlpha = read24BitUnsigned(buf, idx + 6)
                            val highAlpha = read24BitUnsigned(buf, idx + 9)
                            val lowBeta = read24BitUnsigned(buf, idx + 12)
                            val highBeta = read24BitUnsigned(buf, idx + 15)
                            val lowGamma = read24BitUnsigned(buf, idx + 18)
                            val midGamma = read24BitUnsigned(buf, idx + 21)

                            bands = EegBands(
                                delta = delta,
                                theta = theta,
                                lowAlpha = lowAlpha,
                                highAlpha = highAlpha,
                                lowBeta = lowBeta,
                                highBeta = highBeta,
                                lowGamma = lowGamma,
                                midGamma = midGamma,
                            )
                            idx += 24
                        }
                    }
                }
                else -> {
                    // Skip unknown EXCODE bytes or standard values
                    if (code > 0x7F && idx < end) {
                        val skipLen = buf[idx].toInt() and 0xFF
                        idx += 1 + skipLen
                    }
                }
            }
        }

        currentTelemetry = currentTelemetry.copy(
            deviceType = deviceType,
            signalQuality = poorSignal,
            attention = attention,
            meditation = meditation,
            rawWave = rawWave,
            blinkStrength = blink,
            eegBands = bands,
            rawBytesHex = rawHex,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun read24BitUnsigned(buf: ByteArray, offset: Int): Int {
        val b0 = buf[offset].toInt() and 0xFF
        val b1 = buf[offset + 1].toInt() and 0xFF
        val b2 = buf[offset + 2].toInt() and 0xFF
        return (b0 shl 16) or (b1 shl 8) or b2
    }
}
