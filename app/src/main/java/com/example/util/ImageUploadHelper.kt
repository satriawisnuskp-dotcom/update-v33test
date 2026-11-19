package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUploadHelper {

    private const val MAX_ICON_DIMENSION_PX = 320
    private const val MAX_BASE64_CHARS = 550_000

    fun getCustomIconsDir(context: Context): File {
        val dir = File(context.filesDir, "custom_icons")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isCustomIconPath(rawIconOrUri: String): Boolean {
        val trimmed = rawIconOrUri.trim()
        if (trimmed.isBlank()) return false
        return trimmed.startsWith("custom:") ||
            trimmed.startsWith("/") ||
            trimmed.startsWith("file:") ||
            trimmed.contains("custom_icons")
    }

    /**
     * Copies and downscales (max 320px) the content URI image data into custom_icons/
     * so custom logos look sharp while staying lightweight for multi-device Firestore sync.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String = "icon"): String? {
        return try {
            val dir = getCustomIconsDir(context)
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")

            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null && rawBytes.isNotEmpty()) {
                val compressed = compressImageBytesForIcon(rawBytes, MAX_ICON_DIMENSION_PX)
                if (compressed != null) {
                    FileOutputStream(file).use { output ->
                        output.write(compressed)
                        output.flush()
                    }
                    return file.absolutePath
                }
                FileOutputStream(file).use { output ->
                    output.write(rawBytes)
                    output.flush()
                }
                return file.absolutePath
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImageBytesForIcon(rawBytes: ByteArray, maxDim: Int): ByteArray? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDim * 2 || (bounds.outHeight / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

            val maxSide = maxOf(decoded.width, decoded.height)
            val scaled = if (maxSide > maxDim) {
                val ratio = maxDim.toFloat() / maxSide.toFloat()
                val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, w, h, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream()
            if (scaled.hasAlpha()) {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
                if (baos.size() > 260_000) {
                    baos.reset()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                }
            } else {
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            }
            scaled.recycle()
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads a file and converts its binary content to a Base64 string.
     * Automatically downsizes/recompresses oversized legacy icon files so they fit safely in Firestore (<1MB).
     */
    fun fileToBase64(file: File): String? {
        return try {
            if (!file.exists() || !file.isFile || file.length() <= 0L) return null
            val bytes = file.readBytes()
            val rawBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (rawBase64.length <= MAX_BASE64_CHARS) {
                rawBase64
            } else {
                val compressed = compressImageBytesForIcon(bytes, 256)
                    ?: compressImageBytesForIcon(bytes, 160)
                    ?: return null
                val compressedB64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                if (compressedB64.length <= MAX_BASE64_CHARS) compressedB64 else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restores an image file from a Base64 string into custom_icons/ directory.
     * Returns the File on success or null.
     */
    fun base64ToFile(context: Context, fileName: String, base64Str: String): File? {
        return try {
            val cleanName = File(fileName).name
            if (cleanName.isBlank() || base64Str.isBlank()) return null
            val dir = getCustomIconsDir(context)
            val file = File(dir, cleanName)
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            if (file.exists() && file.length() == bytes.size.toLong()) {
                return file
            }
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts the local File from an icon string (e.g. "custom:/path/to/icon.png", "file:/path", or "/path").
     * Returns null if not a custom file path or empty.
     */
    fun extractCustomIconFile(rawIconOrUri: String): File? {
        val trimmed = rawIconOrUri.trim()
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("custom:") -> File(trimmed.removePrefix("custom:"))
            trimmed.startsWith("file://") -> File(trimmed.removePrefix("file://"))
            trimmed.startsWith("file:") -> File(trimmed.removePrefix("file:"))
            trimmed.startsWith("/") -> File(trimmed)
            else -> null
        }
    }

    /**
     * Resolves an existing local File for a custom icon string, falling back to this device's custom_icons/
     * directory by filename when the stored absolute path originated on a different device or install.
     */
    fun resolveExistingCustomIconFile(context: Context, rawIconOrUri: String): File? {
        if (!isCustomIconPath(rawIconOrUri)) return null
        val directFile = extractCustomIconFile(rawIconOrUri)
        if (directFile != null && directFile.exists() && directFile.isFile && directFile.length() > 0L) {
            return directFile
        }
        val fileName = extractFileName(rawIconOrUri)
        if (fileName.isBlank()) return null
        val localDirFile = File(getCustomIconsDir(context), fileName)
        return if (localDirFile.exists() && localDirFile.isFile && localDirFile.length() > 0L) {
            localDirFile
        } else null
    }

    /**
     * Normalizes a custom icon string to point to this device's internal custom_icons/<fileName> path.
     */
    fun normalizeCustomIconPathForLocalDevice(context: Context, rawIconOrUri: String): String {
        val trimmed = rawIconOrUri.trim()
        if (!isCustomIconPath(trimmed)) return trimmed
        val fileName = extractFileName(trimmed)
        if (fileName.isBlank()) return trimmed
        val targetFile = File(getCustomIconsDir(context), fileName)
        return "custom:${targetFile.absolutePath}"
    }

    /**
     * Extracts the clean filename from a raw custom icon string.
     */
    fun extractFileName(rawIconOrUri: String): String {
        val file = extractCustomIconFile(rawIconOrUri)
        return file?.name ?: File(rawIconOrUri).name
    }

    /**
     * Collects all custom logo files present in custom_icons/ and encodes them to a Map of fileName -> Base64 string.
     * Also checks any additional referenced custom file paths and includes them.
     */
    fun collectAllCustomLogos(
        context: Context,
        additionalReferencedPaths: List<String> = emptyList()
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val dir = getCustomIconsDir(context)
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        fileToBase64(file)?.let { b64 ->
                            result[file.name] = b64
                        }
                    }
                }
            }

            additionalReferencedPaths.forEach { rawPath ->
                val f = resolveExistingCustomIconFile(context, rawPath) ?: extractCustomIconFile(rawPath)
                if (f != null && f.exists() && f.isFile && !result.containsKey(f.name)) {
                    fileToBase64(f)?.let { b64 ->
                        result[f.name] = b64
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Resolves an icon string when restoring.
     * If it's a custom icon:
     * 1. If base64Map contains its data, ensures the file is restored into custom_icons/ and returns "custom:$newPath".
     * 2. If it already exists locally in custom_icons/, returns "custom:$localPath".
     * 3. If file exists at original path, returns "custom:$originalPath".
     * Otherwise returns raw icon (e.g. built-in name like "Bank", "Cash", etc.).
     */
    fun resolveOrRestoreIconPath(
        context: Context,
        rawIconOrUri: String,
        base64Map: Map<String, String>? = null
    ): String {
        val trimmed = rawIconOrUri.trim()
        if (trimmed.isBlank()) return ""
        if (!isCustomIconPath(trimmed)) {
            return trimmed
        }

        val fileName = extractFileName(trimmed)
        val dir = getCustomIconsDir(context)
        val targetFile = File(dir, fileName)

        val b64 = base64Map?.get(fileName)
        if (!b64.isNullOrBlank()) {
            base64ToFile(context, fileName, b64)
            if (targetFile.exists()) {
                return "custom:${targetFile.absolutePath}"
            }
        }

        if (targetFile.exists()) {
            return "custom:${targetFile.absolutePath}"
        }

        val originalFile = extractCustomIconFile(trimmed)
        if (originalFile != null && originalFile.exists()) {
            return "custom:${originalFile.absolutePath}"
        }

        return "custom:${targetFile.absolutePath}"
    }
}
