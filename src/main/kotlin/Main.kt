package org.example

import com.garmin.fit.*
import java.io.File
import java.io.FileInputStream

class FitFileModifier {
    private val messages = mutableListOf<Mesg>()

    fun modifyFitFile(inputPath: String, outputPath: String) {
        try {
            loadFitFile(inputPath)
            modifyDeviceInfo()
            saveFitFile(outputPath)
            println("Successfully modified FIT file: $inputPath -> $outputPath")
        } catch (e: Exception) {
            println("Error processing FIT file: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadFitFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("FIT file not found: $filePath")
        }

        // First check file integrity with a separate stream
        FileInputStream(file).use { integrityStream ->
            val decode = Decode()
            if (!decode.checkFileIntegrity(integrityStream)) {
                throw RuntimeException("FIT file integrity check failed")
            }
        }

        // Then decode with a fresh stream
        FileInputStream(file).use { decodeStream ->
            val decode = Decode()
            val broadcaster = MesgBroadcaster(decode)

            broadcaster.addListener(MesgListener { message -> messages.add(message) })

            decode.read(decodeStream, broadcaster)
        }

        println("Loaded ${messages.size} messages from FIT file")
    }

    private fun modifyDeviceInfo() {
        messages.forEach { message ->
            if (message is DeviceInfoMesg) {
                message.manufacturer = Manufacturer.GARMIN
                message.product = GarminProduct.EDGE_840
                message.productName = GarminProduct.getStringFromValue(message.product)
                println("Modified device info to Garmin Edge 840")
            }
        }
    }

    private fun saveFitFile(outputPath: String) {
        val outputFile = File(outputPath)
        val encode = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)

        messages.forEach { message ->
            encode.write(message)
        }

        encode.close()

        println("Saved modified FIT file to: $outputPath")
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar app.jar <input-fit-file>")
        println("Example: java -jar app.jar activity.fit")
        return
    }

    val inputFile = args[0]
    val outputFile = if (inputFile.endsWith(".fit")) {
        inputFile.replace(".fit", "_edge840.fit")
    } else {
        "${inputFile}_edge840.fit"
    }

    val modifier = FitFileModifier()
    modifier.modifyFitFile(inputFile, outputFile)
}
