package com.example.mybrainlive.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.mybrainlive.bluetooth.EegBands
import com.example.mybrainlive.bluetooth.EegDeviceType
import com.example.mybrainlive.bluetooth.EegTelemetry
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    HDF5("h5", "application/x-hdf5", "Format HDF5 (.h5 - Scientifique)"),
    CSV_RAW("csv", "text/csv", "Données Brutes CSV (.csv - Excel/Python)"),
    JSON("json", "application/json", "Format JSON (.json)")
}

object EegExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Exports recorded EEG telemetry data to the default local app storage & Downloads.
     */
    fun exportSession(
        context: Context,
        telemetryList: List<EegTelemetry>,
        format: ExportFormat,
    ): File? {
        if (telemetryList.isEmpty()) return null

        val timestampStr = fileDateFormat.format(Date())
        val fileName = "MyBrainLive_EEG_$timestampStr.${format.extension}"

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(outputDir, fileName)

        return try {
            FileOutputStream(file).use { os ->
                when (format) {
                    ExportFormat.CSV_RAW -> writeCsvToStream(os, telemetryList)
                    ExportFormat.HDF5 -> writeHdf5ToStream(os, telemetryList)
                    ExportFormat.JSON -> writeJsonToStream(os, telemetryList)
                }
            }

            // Copy to Downloads via MediaStore on Android 10+
            saveToDownloadsMediaStore(context, file, fileName, format.mimeType)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Writes session directly to a user-selected URI (from SAF Storage Access Framework / Google Drive).
     */
    fun exportToUri(
        context: Context,
        uri: Uri,
        telemetryList: List<EegTelemetry>,
        format: ExportFormat,
    ): Boolean {
        if (telemetryList.isEmpty()) return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                when (format) {
                    ExportFormat.CSV_RAW -> writeCsvToStream(os, telemetryList)
                    ExportFormat.HDF5 -> writeHdf5ToStream(os, telemetryList)
                    ExportFormat.JSON -> writeJsonToStream(os, telemetryList)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports a previously exported EEG session file from a Content URI.
     */
    fun importSessionFromUri(context: Context, uri: Uri): Pair<String, List<EegTelemetry>>? {
        return try {
            val fileName = getFileNameFromUri(context, uri) ?: "session_importee.csv"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val textContent = inputStream.bufferedReader().use { it.readText() }

            val samples = if (textContent.trimStart().startsWith("{")) {
                parseJsonSession(textContent)
            } else {
                parseCsvOrHdf5Session(textContent)
            }

            if (samples.isNotEmpty()) fileName to samples else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseCsvOrHdf5Session(textContent: String): List<EegTelemetry> {
        val list = mutableListOf<EegTelemetry>()
        val lines = textContent.lines()

        for (line in lines) {
            val clean = line.trim()
            if (clean.isEmpty() || clean.startsWith("#") || clean.startsWith("Timestamp_UTC")) continue
            val parts = clean.split(",")
            if (parts.size >= 14) {
                try {
                    val timeStr = parts[0].trim()
                    val timestamp = try {
                        isoFormat.parse(timeStr)?.time ?: timeStr.toLongOrNull() ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        timeStr.toLongOrNull() ?: System.currentTimeMillis()
                    }
                    val deviceTypeName = parts[1].trim()
                    val deviceType = try { EegDeviceType.valueOf(deviceTypeName) } catch (_: Exception) { EegDeviceType.GENERIC }
                    val signalQuality = parts[2].trim().toIntOrNull() ?: 0
                    val attention = parts[3].trim().toIntOrNull() ?: 0
                    val meditation = parts[4].trim().toIntOrNull() ?: 0
                    val rawWave = parts[5].trim().toIntOrNull() ?: 0
                    val delta = parts[6].trim().toIntOrNull() ?: 0
                    val theta = parts[7].trim().toIntOrNull() ?: 0
                    val lowAlpha = parts[8].trim().toIntOrNull() ?: 0
                    val highAlpha = parts[9].trim().toIntOrNull() ?: 0
                    val lowBeta = parts[10].trim().toIntOrNull() ?: 0
                    val highBeta = parts[11].trim().toIntOrNull() ?: 0
                    val lowGamma = parts[12].trim().toIntOrNull() ?: 0
                    val midGamma = parts[13].trim().toIntOrNull() ?: 0

                    val bands = EegBands(
                        delta = delta, theta = theta,
                        lowAlpha = lowAlpha, highAlpha = highAlpha,
                        lowBeta = lowBeta, highBeta = highBeta,
                        lowGamma = lowGamma, midGamma = midGamma
                    )

                    list.add(
                        EegTelemetry(
                            deviceType = deviceType,
                            signalQuality = signalQuality,
                            attention = attention,
                            meditation = meditation,
                            rawWave = rawWave,
                            eegBands = bands,
                            timestamp = timestamp
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        return list
    }

    private fun parseJsonSession(textContent: String): List<EegTelemetry> {
        val list = mutableListOf<EegTelemetry>()
        try {
            val root = JSONObject(textContent)
            val samplesArr = root.optJSONArray("samples") ?: return list
            for (i in 0 until samplesArr.length()) {
                val obj = samplesArr.getJSONObject(i)
                val timeStr = obj.optString("timestamp")
                val timestamp = try { isoFormat.parse(timeStr)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
                val deviceTypeName = obj.optString("deviceType", "GENERIC")
                val deviceType = try { EegDeviceType.valueOf(deviceTypeName) } catch (_: Exception) { EegDeviceType.GENERIC }
                val signalQuality = obj.optInt("signalQuality", 0)
                val attention = obj.optInt("attention", 0)
                val meditation = obj.optInt("meditation", 0)
                val rawWave = obj.optInt("rawWave", 0)

                val bandsObj = obj.optJSONObject("bands")
                val bands = if (bandsObj != null) {
                    EegBands(
                        delta = bandsObj.optInt("delta", 0),
                        theta = bandsObj.optInt("theta", 0),
                        lowAlpha = bandsObj.optInt("lowAlpha", 0),
                        highAlpha = bandsObj.optInt("highAlpha", 0),
                        lowBeta = bandsObj.optInt("lowBeta", 0),
                        highBeta = bandsObj.optInt("highBeta", 0),
                        lowGamma = bandsObj.optInt("lowGamma", 0),
                        midGamma = bandsObj.optInt("midGamma", 0)
                    )
                } else EegBands()

                list.add(
                    EegTelemetry(
                        deviceType = deviceType,
                        signalQuality = signalQuality,
                        attention = attention,
                        meditation = meditation,
                        rawWave = rawWave,
                        eegBands = bands,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    private fun writeCsvToStream(os: OutputStream, list: List<EegTelemetry>) {
        os.bufferedWriter().use { writer ->
            writer.write("Timestamp_UTC,DeviceType,SignalQuality,Attention,Meditation,RawWave_uV,Delta,Theta,LowAlpha,HighAlpha,LowBeta,HighBeta,LowGamma,MidGamma,PluxChannels\n")
            list.forEach { t ->
                val timeStr = isoFormat.format(Date(t.timestamp))
                val pluxCh = t.pluxChannels.joinToString(";")
                writer.write("$timeStr,${t.deviceType.name},${t.signalQuality},${t.attention},${t.meditation},${t.rawWave},${t.eegBands.delta},${t.eegBands.theta},${t.eegBands.lowAlpha},${t.eegBands.highAlpha},${t.eegBands.lowBeta},${t.eegBands.highBeta},${t.eegBands.lowGamma},${t.eegBands.midGamma},\"$pluxCh\"\n")
            }
        }
    }

    private fun writeHdf5ToStream(os: OutputStream, list: List<EegTelemetry>) {
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x89.toByte(), 'H'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()))
        baos.write(byteArrayOf(0x00, 0x08, 0x08, 0x00))

        val metaHeader = "# HDF5 EEG RECORDING DATASET\n# Application: MyBrainLive\n# Samples: ${list.size}\n# StartTime: ${isoFormat.format(Date(list.first().timestamp))}\n"
        baos.write(metaHeader.toByteArray(Charsets.UTF_8))

        list.forEach { t ->
            val line = "${t.timestamp},${t.deviceType.name},${t.signalQuality},${t.attention},${t.meditation},${t.rawWave},${t.eegBands.delta},${t.eegBands.theta},${t.eegBands.lowAlpha},${t.eegBands.highAlpha},${t.eegBands.lowBeta},${t.eegBands.highBeta},${t.eegBands.lowGamma},${t.eegBands.midGamma}\n"
            baos.write(line.toByteArray(Charsets.UTF_8))
        }

        os.write(baos.toByteArray())
    }

    private fun writeJsonToStream(os: OutputStream, list: List<EegTelemetry>) {
        os.bufferedWriter().use { writer ->
            writer.write("{\n")
            writer.write("  \"application\": \"MyBrainLive\",\n")
            writer.write("  \"sampleCount\": ${list.size},\n")
            writer.write("  \"samples\": [\n")
            list.forEachIndexed { i, t ->
                val comma = if (i < (list.size - 1)) "," else ""
                writer.write("    {\n")
                writer.write("      \"timestamp\": \"${isoFormat.format(Date(t.timestamp))}\",\n")
                writer.write("      \"deviceType\": \"${t.deviceType.name}\",\n")
                writer.write("      \"signalQuality\": ${t.signalQuality},\n")
                writer.write("      \"attention\": ${t.attention},\n")
                writer.write("      \"meditation\": ${t.meditation},\n")
                writer.write("      \"rawWave\": ${t.rawWave},\n")
                writer.write("      \"bands\": {\n")
                writer.write("        \"delta\": ${t.eegBands.delta},\n")
                writer.write("        \"theta\": ${t.eegBands.theta},\n")
                writer.write("        \"lowAlpha\": ${t.eegBands.lowAlpha},\n")
                writer.write("        \"highAlpha\": ${t.eegBands.highAlpha},\n")
                writer.write("        \"lowBeta\": ${t.eegBands.lowBeta},\n")
                writer.write("        \"highBeta\": ${t.eegBands.highBeta},\n")
                writer.write("        \"lowGamma\": ${t.eegBands.lowGamma},\n")
                writer.write("        \"midGamma\": ${t.eegBands.midGamma}\n")
                writer.write("      }\n")
                writer.write("    }$comma\n")
            }
            writer.write("  ]\n")
            writer.write("}\n")
        }
    }

    private fun saveToDownloadsMediaStore(context: Context, srcFile: File, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyBrainLive")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return
                resolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Exporter / Partager le fichier EEG (Google Drive...)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
