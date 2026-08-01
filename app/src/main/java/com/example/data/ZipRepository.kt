package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ZipRepository(private val zipDao: ZipDao) {

    val allZipFiles: Flow<List<ZipFileEntity>> = zipDao.getAllZipFiles()

    suspend fun getZipFileById(id: Int): ZipFileEntity? {
        return withContext(Dispatchers.IO) {
            zipDao.getZipFileById(id)
        }
    }

    suspend fun updateZipFile(zipFile: ZipFileEntity) {
        withContext(Dispatchers.IO) {
            zipDao.updateZipFile(zipFile)
        }
    }

    /**
     * Checks whether a ZIP file with the same name and size already exists in the vault.
     * Returns the existing matching entity, or null if no duplicate was found.
     */
    suspend fun checkForDuplicate(context: Context, uri: Uri): ZipFileEntity? {
        return withContext(Dispatchers.IO) {
            val (name, size) = getUriMetadata(context, uri)
            if (size <= 0) {
                // Can't reliably compare without a known size; skip duplicate check.
                null
            } else {
                zipDao.findByNameAndSize(name, size)
            }
        }
    }

    /**
     * Imports a ZIP file from a Uri (either shared in or picked by document picker).
     * Copies the file to internal storage and creates a database record.
     */
    suspend fun importZipFile(context: Context, uri: Uri): Result<ZipFileEntity> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Metadata (name, size)
                val (name, sizeFromUri) = getUriMetadata(context, uri)
                
                // 2. Prepare destination directory in internal storage
                val vaultDir = File(context.filesDir, "vault_zips")
                if (!vaultDir.exists()) {
                    vaultDir.mkdirs()
                }

                // Create a secure, unique local file name
                val uniqueName = "${System.currentTimeMillis()}_${name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}"
                val destFile = File(vaultDir, uniqueName)

                // 3. Copy Stream
                var bytesCopied = 0L
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = inputStream.read(buffer)
                        }
                    }
                } ?: return@withContext Result.failure(Exception("Could not open input stream from Uri"))

                // Use the copied bytes as size if URI metadata size is 0 or unavailable
                val finalSize = if (sizeFromUri > 0) sizeFromUri else bytesCopied

                // 4. Save to Database
                val entity = ZipFileEntity(
                    name = name,
                    filePath = destFile.absolutePath,
                    size = finalSize,
                    dateAdded = System.currentTimeMillis(),
                    tags = ""
                )
                val id = zipDao.insertZipFile(entity)
                val savedEntity = entity.copy(id = id.toInt())

                Result.success(savedEntity)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Deletes a ZIP file from local disk storage and Room Database.
     */
    suspend fun deleteZipFile(zipFile: ZipFileEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Delete actual file
                val file = File(zipFile.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                // Delete database entry
                zipDao.deleteZipFileById(zipFile.id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = ""
        var size = 0L

        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex) ?: ""
                        }
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "imported_file.zip"
        }

        if (!name.lowercase().endsWith(".zip")) {
            name += ".zip"
        }

        return Pair(name, size)
    }
}
