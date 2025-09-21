package org.example

import com.garmin.fit.*
import java.io.File
import java.io.FileInputStream

/**
 * ZwiftToGarminConverter - Modifies Zwift FIT files to appear as Garmin Edge 840 activities
 *
 * Purpose: Zwift virtual rides don't count towards Garmin badges and challenges because they
 * are tagged as coming from Zwift rather than a Garmin device. This tool modifies the device
 * information in FIT files to make them appear as if they came from a Garmin Edge 840,
 * allowing them to count for Garmin Connect challenges and badges.
 *
 * The tool preserves all ride data while only changing the device metadata.
 */
class ZwiftToGarminConverter {

    companion object {
        private const val TARGET_MANUFACTURER = Manufacturer.GARMIN
        private const val TARGET_DEVICE_PRODUCT = GarminProduct.EDGE_840
        private const val OUTPUT_SUFFIX = "_edge840"

        // Target device info for spoofing
        private val targetProductName = GarminProduct.getStringFromValue(TARGET_DEVICE_PRODUCT)
    }

    /**
     * Converts a Zwift FIT file to appear as a Garmin Edge 840 activity
     *
     * @param inputPath Path to the original FIT file
     * @param outputPath Path where the modified FIT file will be saved
     * @return true if conversion was successful, false otherwise
     */
    fun convertFitFile(inputPath: String, outputPath: String): Boolean {
        return try {
            val inputFile = validateInputFile(inputPath)
            verifyFileIntegrity(inputFile)
            processFileWithDeviceModification(inputFile, outputPath)

            println("✅ Successfully converted FIT file: $inputPath -> $outputPath")
            println("   Activities from this file will now count for Garmin badges and challenges")
            true
        } catch (e: Exception) {
            println("❌ Error converting FIT file: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Validates that the input file exists and is readable
     */
    private fun validateInputFile(inputPath: String): File {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            throw IllegalArgumentException("FIT file not found: $inputPath")
        }
        if (!inputFile.canRead()) {
            throw IllegalArgumentException("Cannot read FIT file: $inputPath")
        }
        return inputFile
    }

    /**
     * Verifies the FIT file integrity before processing
     * Uses a separate stream as recommended by Garmin SDK documentation
     */
    private fun verifyFileIntegrity(inputFile: File) {
        FileInputStream(inputFile).use { integrityStream ->
            val decode = Decode()
            if (!decode.checkFileIntegrity(integrityStream)) {
                throw RuntimeException("FIT file integrity check failed - file may be corrupted")
            }
        }
        println("✓ File integrity verified")
    }

    /**
     * Processes the FIT file using streaming approach to preserve message order
     *
     * This follows the official Garmin DecodeExample pattern:
     * 1. Create a streaming listener that modifies messages on-the-fly
     * 2. Write messages immediately to preserve chronological order
     * 3. Handle developer fields and error cases properly
     */
    private fun processFileWithDeviceModification(inputFile: File, outputPath: String) {
        val outputFile = File(outputPath)
        val encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)

        var messageCount = 0
        var deviceMetadataModified = false

        FileInputStream(inputFile).use { inputStream ->
            val decode = Decode()
            val mesgBroadcaster = MesgBroadcaster(decode)

            // Create streaming listener that modifies device metadata and writes immediately
            val deviceModifyingListener = createDeviceModifyingListener(encoder) { isDeviceRelated ->
                messageCount++
                if (isDeviceRelated) {
                    deviceMetadataModified = true
                }
            }

            // Set up message processing pipeline
            mesgBroadcaster.addListener(deviceModifyingListener)
            decode.addListener(createDeveloperFieldListener())

            // Process the file with robust error handling
            processFileWithErrorHandling(decode, inputStream, mesgBroadcaster)
        }

        encoder.close()

        // Report processing results
        reportProcessingResults(inputFile, outputFile, messageCount, deviceMetadataModified)
    }

    /**
     * Creates a listener that modifies device-related information on-the-fly
     *
     * Key operations:
     * - Changes manufacturer to GARMIN and product to EDGE_840 for FileIdMesg and DeviceInfoMesg
     * - FileIdMesg: Overall file metadata (what created the file)
     * - DeviceInfoMesg: Individual device information during the activity
     * - Removes expanded/computed fields to match original file structure
     * - Writes each message immediately to preserve chronological order
     */
    private fun createDeviceModifyingListener(
        encoder: FileEncoder,
        onMessageProcessed: (isDeviceRelated: Boolean) -> Unit
    ): MesgListener {
        return object : MesgListener {
            override fun onMesg(mesg: Mesg) {
                var isDeviceRelated = false
                var messageToWrite: Mesg = mesg

                // Modify File ID information (overall file metadata)
                if (mesg.num == MesgNum.FILE_ID) {
                    val fileIdMessage = FileIdMesg(mesg)
                    println("🔍 Found FileIdMesg - before: manufacturer=${fileIdMessage.manufacturer}, product=${fileIdMessage.product}")

                    fileIdMessage.manufacturer = TARGET_MANUFACTURER
                    fileIdMessage.product = TARGET_DEVICE_PRODUCT
                    isDeviceRelated = true

                    println("🔧 Modified file ID - after: manufacturer=${fileIdMessage.manufacturer}, product=${fileIdMessage.product}")
                    messageToWrite = fileIdMessage
                }

                // Modify device information (individual device data during activity)
                if (mesg.num == MesgNum.DEVICE_INFO) {
                    val deviceInfoMessage = DeviceInfoMesg(mesg)
                    println("🔍 Found DeviceInfoMesg - before: manufacturer=${deviceInfoMessage.manufacturer}, product=${deviceInfoMessage.product}")

                    deviceInfoMessage.manufacturer = TARGET_MANUFACTURER
                    deviceInfoMessage.product = TARGET_DEVICE_PRODUCT
                    deviceInfoMessage.productName = targetProductName
                    isDeviceRelated = true

                    println("🔧 Modified device info - after: manufacturer=${deviceInfoMessage.manufacturer}, product=${deviceInfoMessage.product}")
                    messageToWrite = deviceInfoMessage
                }

                // Remove computed fields that weren't in the original file
                // This is crucial for maintaining proper file structure and size
                messageToWrite.removeExpandedFields()

                // Write the correct message (either original or modified typed message)
                encoder.write(messageToWrite)

                // Report progress
                onMessageProcessed(isDeviceRelated)
            }
        }
    }

    /**
     * Creates a listener for developer field descriptions (Zwift custom data)
     * Silently preserves developer fields without logging (they're handled automatically)
     */
    private fun createDeveloperFieldListener(): DeveloperFieldDescriptionListener {
        return object : DeveloperFieldDescriptionListener {
            override fun onDescription(description: DeveloperFieldDescription) {
                // Silently preserve developer fields - no logging needed
                // The fields are automatically preserved in the message stream
            }
        }
    }

    /**
     * Processes the file with robust error handling as shown in Garmin's DecodeExample
     */
    private fun processFileWithErrorHandling(
        decode: Decode,
        inputStream: FileInputStream,
        mesgBroadcaster: MesgBroadcaster
    ) {
        try {
            decode.read(inputStream, mesgBroadcaster, mesgBroadcaster)
        } catch (e: FitRuntimeException) {
            // Handle files with invalid data size in header (rare edge case)
            if (decode.invalidFileDataSize) {
                println("⚠️  Invalid file data size detected, attempting recovery...")
                decode.nextFile()
                decode.read(inputStream, mesgBroadcaster, mesgBroadcaster)
            } else {
                System.err.println("Failed to decode FIT file: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Reports the results of the file processing operation
     */
    private fun reportProcessingResults(
        inputFile: File,
        outputFile: File,
        messageCount: Int,
        deviceMetadataModified: Boolean
    ) {
        println("📈 Processing summary:")
        println("   Messages processed: $messageCount")
        println("   Device metadata modified: ${if (deviceMetadataModified) "Yes" else "No"}")

        val originalSize = inputFile.length()
        val newSize = outputFile.length()
        val sizeRatio = (newSize.toDouble() / originalSize * 100)

        println("   Original file size: ${formatBytes(originalSize)}")
        println("   New file size: ${formatBytes(newSize)} (${String.format("%.1f", sizeRatio)}%)")

        if (sizeRatio < 90) {
            println("   ℹ️  File size reduction is normal due to removal of computed fields")
        }
    }

    /**
     * Generates output filename by adding suffix before .fit extension
     */
    fun generateOutputPath(inputPath: String): String {
        return if (inputPath.endsWith(".fit", ignoreCase = true)) {
            inputPath.replace(Regex("\\.fit$", RegexOption.IGNORE_CASE), "$OUTPUT_SUFFIX.fit")
        } else {
            "${inputPath}$OUTPUT_SUFFIX.fit"
        }
    }

    /**
     * Formats byte count into human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }
}

/**
 * Main entry point for the command-line application
 */
fun main(args: Array<String>) {
    println("🚴 Zwift to Garmin Challenge Converter")
    println("     Makes your Zwift rides count for Garmin badges and challenges!")
    println()

    if (args.isEmpty()) {
        printUsage()
        return
    }

    val converter = ZwiftToGarminConverter()
    val inputPath = args[0]
    val outputPath = converter.generateOutputPath(inputPath)

    val success = converter.convertFitFile(inputPath, outputPath)

    if (success) {
        println()
        println("🎉 Conversion completed! Upload '$outputPath' to Garmin Connect.")
    } else {
        println()
        println("💥 Conversion failed. Please check the error messages above.")
        kotlin.system.exitProcess(1)
    }
}

/**
 * Prints usage instructions
 */
private fun printUsage() {
    println("Usage: java -jar zwift-to-garmin-converter.jar <input-fit-file>")
    println()
    println("Examples:")
    println("  java -jar zwift-to-garmin-converter.jar my_zwift_ride.fit")
    println("  java -jar zwift-to-garmin-converter.jar \"Zwift - Race on London.fit\"")
    println()
    println("Output:")
    println("  Creates a new file with '_edge840' suffix (e.g., my_zwift_ride_edge840.fit)")
    println("  Upload this new file to Garmin Connect to count for challenges!")
}
