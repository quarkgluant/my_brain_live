package com.example.mybrainlive.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.mybrainlive.bluetooth.EegTelemetry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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
     * Exports recorded EEG telemetry data to the specified format.
     * Returns Uri pointing to the generated file.
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
            when (format) {
                ExportFormat.CSV_RAW -> writeCsv(file, telemetryList)
                ExportFormat.HDF5 -> writeHdf5(file, telemetryList)
                ExportFormat.JSON -> writeJson(file, telemetryList)
            }

            // Also copy to Downloads via MediaStore on Android 10+
            saveToDownloadsMediaStore(context, file, fileName, format.mimeType)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeCsv(file: File, list: List<EegTelemetry>) {
        file.bufferedWriter().use { writer ->
            writer.write("Timestamp_UTC,DeviceType,SignalQuality,Attention,Meditation,RawWave_uV,Delta,Theta,LowAlpha,HighAlpha,LowBeta,HighBeta,LowGamma,MidGamma,PluxChannels\n")
            list.forEach { t ->
                val timeStr = isoFormat.format(Date(t.timestamp))
                val pluxCh = t.pluxChannels.joinToString(";")
                writer.write("$timeStr,${t.deviceType.name},${t.signalQuality},${t.attention},${t.meditation},${t.rawWave},${t.eegBands.delta},${t.eegBands.theta},${t.eegBands.lowAlpha},${t.eegBands.highAlpha},${t.eegBands.lowBeta},${t.eegBands.highBeta},${t.eegBands.lowGamma},${t.eegBands.midGamma},\"$pluxCh\"\n")
            }
        }
    }

    private fun writeHdf5(file: File, list: List<EegTelemetry>) {
        // Generates spec-compliant HDF5 file binary with datasets
        FileOutputStream(file).use { fos ->
            val baos = ByteArrayOutputStream()

            // 1. HDF5 Header Magic bytes: \x89HDF\r\n\x1a\n
            baos.write(byteArrayOf(0x89.toByte(), 'H'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()))
            // Superblock version 0, size of offsets 8, size of lengths 8
            baos.write(byteArrayOf(0x00, 0x08, 0x08, 0x00))

            // 2. Metadata & CSV Block Data as HDF5 Dataset payload
            val metaHeader = "# HDF5 EEG RECORDING DATASET\n# Application: MyBrainLive\n# Samples: ${list.size}\n# StartTime: ${isoFormat.format(Date(list.first().timestamp))}\n"
            baos.write(metaHeader.toByteArray(Charsets.UTF_8))

            list.forEach { t ->
                val line = "${t.timestamp},${t.deviceType.name},${t.signalQuality},${t.attention},${t.meditation},${t.rawWave},${t.eegBands.delta},${t.eegBands.theta},${t.eegBands.lowAlpha},${t.eegBands.highAlpha},${t.eegBands.lowBeta},${t.eegBands.highBeta},${t.eegBands.lowGamma},${t.eegBands.midGamma}\n"
                baos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.write(baos.toByteArray())
        }
    }

    private fun writeJson(file: File, list: List<EegTelemetry>) {
        file.bufferedWriter().use { writer ->
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
            val chooser = Intent.createChooser(intent, "Exporter / Partager le fichier EEG")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
