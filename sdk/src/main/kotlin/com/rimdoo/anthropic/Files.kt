@file:JvmName("AnthropicKt")
@file:JvmMultifileClass

package com.rimdoo.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicException as RawAnthropicException
import com.anthropic.models.beta.files.FileDeleteParams
import com.anthropic.models.beta.files.FileMetadata as RawFileMetadata
import com.anthropic.models.beta.files.FileUploadParams
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val FILES_API_BETA = "files-api-2025-04-14"

class FileInfo internal constructor(
    internal val raw: RawFileMetadata,
) {
    val id: String get() = raw.id()
    val filename: String get() = raw.filename()
    val mimeType: String get() = raw.mimeType()
    val sizeBytes: Long get() = raw.sizeBytes()
    val createdAt: OffsetDateTime get() = raw.createdAt()
    val downloadable: Boolean? get() = raw.downloadable().orElse(null)
}

suspend fun AnthropicClient.uploadFile(bytes: ByteArray): FileInfo =
    withContext(Dispatchers.IO) {
        try {
            val params = FileUploadParams.builder()
                .file(bytes)
                .addBeta(FILES_API_BETA)
                .build()
            FileInfo(beta().files().upload(params))
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

suspend fun AnthropicClient.retrieveFileMetadata(fileId: String): FileInfo =
    withContext(Dispatchers.IO) {
        try {
            FileInfo(beta().files().retrieveMetadata(fileId))
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

suspend fun AnthropicClient.deleteFile(fileId: String) =
    withContext(Dispatchers.IO) {
        try {
            beta().files().delete(
                FileDeleteParams.builder()
                    .fileId(fileId)
                    .addBeta(FILES_API_BETA)
                    .build()
            )
            Unit
        } catch (e: RawAnthropicException) {
            throw e.toAnthropicException()
        }
    }

fun AnthropicClient.listFiles(): Flow<FileInfo> = flow {
    try {
        var page = beta().files().list()
        while (true) {
            page.data().forEach { emit(FileInfo(it)) }
            if (!page.hasNextPage()) break
            page = page.nextPage()
        }
    } catch (e: RawAnthropicException) {
        throw e.toAnthropicException()
    }
}.flowOn(Dispatchers.IO)
