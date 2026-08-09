package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.ParsedFileInfo
import com.example.data.parser.FileNameParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaScanner(private val context: Context) {

    private val supportedExtensions = setOf("mkv", "mp4", "avi", "webm", "mov", "m4v", "ts", "flv", "wmv", "3gp")

    suspend fun scanLocalMediaFiles(
        customFolderPath: String? = null,
        includeDemoFallback: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaMap = LinkedHashMap<String, MediaItem>()

        try {
            // 1. Query MediaStore for Video Files (Lightning fast indexed query)
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED
            )

            val selection = "${MediaStore.Video.Media.MIME_TYPE} LIKE 'video/%'"
            
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = cursor.getString(dataColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) * 1000L

                    val parsed = FileNameParser.parse(name)
                    val mediaId = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                    mediaMap[path] = MediaItem(
                        id = mediaId,
                        filePath = path,
                        fileName = name,
                        fileSize = size,
                        lastModified = if (dateModified > 0) dateModified else System.currentTimeMillis(),
                        parsedInfo = parsed
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scan Custom Folder, SAF Tree URI, or App Internal Media Dir
        if (!customFolderPath.isNullOrBlank()) {
            if (customFolderPath.startsWith("content://")) {
                try {
                    val treeUri = Uri.parse(customFolderPath)
                    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                    if (!rootDocId.isNullOrBlank()) {
                        scanSafTreeRecursively(treeUri, rootDocId, depth = 0, maxDepth = 4, mediaMap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Also check if URI segment refers to local storage path e.g. primary:Download
                val pathSegment = Uri.parse(customFolderPath).lastPathSegment ?: ""
                if (pathSegment.contains(":")) {
                    val subPath = pathSegment.substringAfter(":")
                    if (subPath.isNotBlank()) {
                        val fileObj = File("/storage/emulated/0/$subPath")
                        if (fileObj.exists() && fileObj.isDirectory) {
                            scanDirectoryRecursively(fileObj, depth = 0, maxDepth = 4, mediaMap)
                        }
                    }
                }
            } else {
                val customFile = File(customFolderPath)
                if (customFile.exists() && customFile.isDirectory) {
                    scanDirectoryRecursively(customFile, depth = 0, maxDepth = 4, mediaMap)
                }
            }
        } else {
            context.getExternalFilesDir(null)?.let { dir ->
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryRecursively(dir, depth = 0, maxDepth = 4, mediaMap)
                }
            }
        }

        val resultList = mediaMap.values.toMutableList()

        // 3. Fallback only if explicitly requested or if scan returned 0 items and fallback is allowed
        if (resultList.isEmpty() && includeDemoFallback) {
            resultList.addAll(getDemoScannedFiles())
        }

        resultList
    }

    private fun scanDirectoryRecursively(
        dir: File,
        depth: Int,
        maxDepth: Int,
        mediaMap: MutableMap<String, MediaItem>
    ) {
        if (depth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                scanDirectoryRecursively(file, depth + 1, maxDepth, mediaMap)
            } else if (file.isFile && isVideoFile(file.name)) {
                if (!mediaMap.containsKey(file.absolutePath)) {
                    val parsed = FileNameParser.parse(file.name)
                    mediaMap[file.absolutePath] = MediaItem(
                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                        filePath = file.absolutePath,
                        fileName = file.name,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        parsedInfo = parsed
                    )
                }
            }
        }
    }

    private fun scanSafTreeRecursively(
        treeUri: Uri,
        parentDocId: String,
        depth: Int,
        maxDepth: Int,
        mediaMap: MutableMap<String, MediaItem>
    ) {
        if (depth > maxDepth) return
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanSafTreeRecursively(treeUri, docId, depth + 1, maxDepth, mediaMap)
                    } else if (isVideoFile(name) || mime.startsWith("video/")) {
                        val fileUriStr = docUri.toString()
                        if (!mediaMap.containsKey(fileUriStr)) {
                            val parsed = FileNameParser.parse(name)
                            mediaMap[fileUriStr] = MediaItem(
                                id = UUID.nameUUIDFromBytes(fileUriStr.toByteArray()).toString(),
                                filePath = fileUriStr,
                                fileName = name,
                                fileSize = size,
                                lastModified = if (date > 0) date else System.currentTimeMillis(),
                                parsedInfo = parsed
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isVideoFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return supportedExtensions.contains(ext)
    }

    fun getDemoScannedFiles(): List<MediaItem> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        val demoFiles = listOf(
            Triple("[SubGroup] Jujutsu_Kaisen_S02E12_1080p_HEVC.mkv", 1450000000L, now - day),
            Triple("Oppenheimer.2023.2160p.UHD.BluRay.x265-GROUP.mkv", 18200000000L, now - 2 * day),
            Triple("Sousou_no_Frieren_E28_[1080p][x264].mkv", 980000000L, now - 3 * day),
            Triple("Breaking.Bad.S05E14.1080p.WEB-DL.mkv", 1200000000L, now - 4 * day),
            Triple("Solo_Leveling_S01E08_1080p_AAC.mkv", 850000000L, now - 5 * day),
            Triple("Cyberpunk_Edgerunners_S01E10_1080p.mkv", 1100000000L, now - 6 * day),
            Triple("VID_20240112_01.mp4", 320000000L, now - 7 * day)
        )

        return demoFiles.mapIndexed { index, (fileName, size, date) ->
            val parsed = FileNameParser.parse(fileName)
            MediaItem(
                id = "demo_file_$index",
                filePath = "/storage/emulated/0/Download/$fileName",
                fileName = fileName,
                fileSize = size,
                lastModified = date,
                parsedInfo = parsed
            )
        }
    }
}

