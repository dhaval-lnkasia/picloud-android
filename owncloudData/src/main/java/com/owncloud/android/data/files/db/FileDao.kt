/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.data.files.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus.AVAILABLE_OFFLINE
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
import com.owncloud.android.domain.ext.isOneOf
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import kotlinx.coroutines.flow.Flow
import java.io.File.separatorChar
import java.util.UUID

@Dao
abstract class FileDao {

    @Query(SELECT_FILE_WITH_ID)
    abstract fun getFileById(
        id: Long
    ): OCFileEntity?

    @Query(SELECT_FILE_WITH_ID)
    abstract fun getFileWithSyncInfoById(
        id: Long
    ): OCFileAndFileSync?

    @Query(SELECT_FILE_WITH_ID)
    abstract fun getFileByIdAsStream(
        id: Long
    ): Flow<OCFileEntity?>

    @Query(SELECT_FILE_FROM_OWNER_WITH_REMOTE_PATH)
    abstract fun getFileByOwnerAndRemotePath(
        owner: String,
        remotePath: String
    ): OCFileEntity?

    @Query(SELECT_FILE_WITH_REMOTE_ID)
    abstract fun getFileByRemoteId(
        remoteId: String
    ): OCFileEntity?

    @Query(SELECT_FILTERED_FOLDER_CONTENT)
    abstract fun getSearchFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FILTERED_AVAILABLE_OFFLINE_FOLDER_CONTENT)
    abstract fun getSearchAvailableOfflineFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FILTERED_SHARED_BY_LINK_FOLDER_CONTENT)
    abstract fun getSearchSharedByLinkFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FOLDER_CONTENT)
    abstract fun getFolderContent(
        folderId: Long
    ): List<OCFileEntity>

    @Query(SELECT_FOLDER_CONTENT)
    abstract fun getFolderContentWithSyncInfo(
        folderId: Long
    ): List<OCFileAndFileSync>

    @Transaction
    @Query(SELECT_FOLDER_CONTENT)
    abstract fun getFolderContentWithSyncInfoAsStream(
        folderId: Long
    ): Flow<List<OCFileAndFileSync>>

    @Query(SELECT_FOLDER_BY_MIMETYPE)
    abstract fun getFolderByMimeType(
        folderId: Long,
        mimeType: String
    ): List<OCFileEntity>

    @Transaction
    @Query(SELECT_FILES_SHARED_BY_LINK)
    abstract fun getFilesWithSyncInfoSharedByLinkAsStream(
        accountOwner: String
    ): Flow<List<OCFileAndFileSync>>

    @Transaction
    @Query(SELECT_FILES_AVAILABLE_OFFLINE_FROM_ACCOUNT)
    abstract fun getFilesWithSyncInfoAvailableOfflineFromAccountAsStream(
        accountOwner: String
    ): Flow<List<OCFileAndFileSync>>

    @Query(SELECT_FILES_AVAILABLE_OFFLINE_FROM_ACCOUNT)
    abstract fun getFilesAvailableOfflineFromAccount(
        accountOwner: String
    ): List<OCFileEntity>

    @Query(SELECT_FILES_AVAILABLE_OFFLINE_FROM_EVERY_ACCOUNT)
    abstract fun getFilesAvailableOfflineFromEveryAccount(): List<OCFileEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insertOrIgnore(ocFileEntity: OCFileEntity): Long

    @Update
    protected abstract fun update(ocFileEntity: OCFileEntity)

    @Transaction
    open fun upsert(ocFileEntity: OCFileEntity) = com.owncloud.android.data.upsert(
        item = ocFileEntity,
        insert = ::insertOrIgnore,
        update = ::update
    )

    @Transaction
    open fun updateSyncStatusForFile(id: Long, workerUuid: UUID?) {
        val fileWithSyncInfoEntity = getFileWithSyncInfoById(id)

        if ((fileWithSyncInfoEntity?.file?.parentId != ROOT_PARENT_ID) &&
            ((workerUuid == null) != (fileWithSyncInfoEntity?.fileSync?.downloadWorkerUuid == null))) {
            val fileSyncEntity = if (workerUuid == null) {
                OCFileSyncEntity(
                    fileId = id,
                    uploadWorkerUuid = null,
                    downloadWorkerUuid = null,
                    isSynchronizing = false
                )
            } else {
                OCFileSyncEntity(
                    fileId = id,
                    uploadWorkerUuid = null,
                    downloadWorkerUuid = workerUuid,
                    isSynchronizing = true
                )
            }
            insertFileSync(fileSyncEntity)

            // Check if there is any more file synchronizing in this folder, in such case don't update parent's sync status
            var cleanSyncInParent = true
            if (workerUuid == null) {
                val folderContent = getFolderContentWithSyncInfo(fileWithSyncInfoEntity?.file?.parentId!!)
                var indexFileInFolder = 0
                while (cleanSyncInParent && indexFileInFolder < folderContent.size) {
                    val child = folderContent[indexFileInFolder]
                    if (child.fileSync?.isSynchronizing == true) {
                        cleanSyncInParent = false
                    }
                    indexFileInFolder++
                }
            }
            if (workerUuid != null || cleanSyncInParent) {
                updateSyncStatusForFile(fileWithSyncInfoEntity?.file?.parentId!!, workerUuid)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertFileSync(ocFileSyncEntity: OCFileSyncEntity): Long

    /**
     * Make sure that the ids are set properly. We don't take care of conflicts and that stuff here.
     *
     * return folder content
     */
    @Transaction
    open fun insertFilesInFolderAndReturnThem(
        folder: OCFileEntity,
        folderContent: List<OCFileEntity>,
    ): List<OCFileEntity> {
        var folderId = insertOrIgnore(folder)
        // If it was already in database
        if (folderId == -1L) folderId = folder.id

        folderContent.forEach { fileToInsert ->
            upsert(fileToInsert.apply {
                parentId = folderId
                availableOfflineStatus = getNewAvailableOfflineStatus(folder.availableOfflineStatus, fileToInsert.availableOfflineStatus)
            })
        }
        return getFolderContent(folderId)
    }

    @Transaction
    open fun mergeRemoteAndLocalFile(
        ocFileEntity: OCFileEntity
    ): Long {
        val localFile: OCFileEntity? = getFileByOwnerAndRemotePath(
            owner = ocFileEntity.owner,
            remotePath = ocFileEntity.remotePath
        )
        return if (localFile == null) {
            insertOrIgnore(ocFileEntity)
        } else {
            insertOrIgnore(ocFileEntity.copy(
                parentId = localFile.parentId,
                lastSyncDateForData = localFile.lastSyncDateForData,
                modifiedAtLastSyncForData = localFile.modifiedAtLastSyncForData,
                storagePath = localFile.storagePath,
                treeEtag = localFile.treeEtag,
                etagInConflict = localFile.etagInConflict,
                availableOfflineStatus = localFile.availableOfflineStatus,
            ).apply {
                id = localFile.id
            })
        }
    }

    @Transaction
    open fun copy(
        sourceFile: OCFileEntity,
        targetFolder: OCFileEntity,
        finalRemotePath: String,
        remoteId: String?
    ) {
        // 1. Update target size
        upsert(
            targetFolder.copy(
                length = targetFolder.length + sourceFile.length
            ).apply { id = targetFolder.id }
        )

        // 2. Insert a new file with common attributes and retrieved remote id
        upsert(
            OCFileEntity(
                parentId = targetFolder.id,
                owner = targetFolder.owner,
                remotePath = finalRemotePath,
                remoteId = remoteId,
                length = sourceFile.length,
                modificationTimestamp = sourceFile.modificationTimestamp,
                mimeType = sourceFile.mimeType,
                name = null,
                needsToUpdateThumbnail = true,
                etag = "",
                creationTimestamp = null,
                permissions = null,
                treeEtag = "",
                availableOfflineStatus = NOT_AVAILABLE_OFFLINE.ordinal,
            )
        )
    }

    @Transaction
    open fun moveFile(
        sourceFile: OCFileEntity,
        targetFolder: OCFileEntity,
        finalRemotePath: String,
        finalStoragePath: String
    ) {
        // 1. Update target size
        upsert(
            targetFolder.copy(
                length = targetFolder.length + sourceFile.length
            ).apply { id = targetFolder.id }
        )

        // 2. Update source
        if (sourceFile.isFolder) {
            // Update remote path and storage path when moving a folder
            moveFolder(
                sourceFolder = sourceFile,
                targetFolder = targetFolder,
                targetRemotePath = finalRemotePath,
                targetStoragePath = finalStoragePath
            )
        } else {
            // Update remote path, storage path, parent file when moving a file
            moveSingleFile(
                sourceFile = sourceFile,
                targetFolder = targetFolder,
                finalRemotePath = finalRemotePath,
                finalStoragePath = sourceFile.storagePath?.let { finalStoragePath }
            )
        }
    }

    @Query(DELETE_FILE_WITH_ID)
    abstract fun deleteFileWithId(id: Long)

    @Query(UPDATE_FILES_STORAGE_DIRECTORY)
    abstract fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String)

    @Transaction
    open fun updateAvailableOfflineStatusForFile(ocFile: OCFile, newAvailableOfflineStatus: Int) {
        if (ocFile.isFolder) {
            updateFolderWithNewAvailableOfflineStatus(ocFile.id!!, newAvailableOfflineStatus)
        } else {
            updateFileWithAvailableOfflineStatus(ocFile.id!!, newAvailableOfflineStatus)
        }
    }

    private fun updateFolderWithNewAvailableOfflineStatus(ocFolderId: Long, newAvailableOfflineStatus: Int) {
        updateFileWithAvailableOfflineStatus(ocFolderId, newAvailableOfflineStatus)

        val newStatusForChildren = if (newAvailableOfflineStatus == NOT_AVAILABLE_OFFLINE.ordinal) {
            NOT_AVAILABLE_OFFLINE.ordinal
        } else {
            AVAILABLE_OFFLINE_PARENT.ordinal
        }
        val folderContent = getFolderContent(ocFolderId)
        folderContent.forEach { folderChild ->
            if (folderChild.isFolder) {
                updateFolderWithNewAvailableOfflineStatus(folderChild.id, newStatusForChildren)
            } else {
                updateFileWithAvailableOfflineStatus(folderChild.id, newStatusForChildren)
            }
        }
    }

    @Query(UPDATE_FILE_WITH_NEW_AVAILABLE_OFFLINE_STATUS)
    abstract fun updateFileWithAvailableOfflineStatus(id: Long, availableOfflineStatus: Int)

    @Transaction
    open fun updateConflictStatusForFile(id: Long, eTagInConflict: String?) {
        val fileEntity = getFileById(id)

        if (fileEntity?.parentId != ROOT_PARENT_ID) {
            updateFileWithConflictStatus(id, eTagInConflict)

            // Check if there is any more file with conflicts in this folder, in such case don't update parent's conflict status
            var cleanConflictInParent = true
            if (eTagInConflict == null) {
                val folderContent = getFolderContent(fileEntity?.parentId!!)
                var indexFileInFolder = 0
                while (cleanConflictInParent && indexFileInFolder < folderContent.size) {
                    val child = folderContent[indexFileInFolder]
                    if (child.etagInConflict != null) {
                        cleanConflictInParent = false
                    }
                    indexFileInFolder++
                }
            }
            if (eTagInConflict != null || cleanConflictInParent) {
                updateConflictStatusForFile(fileEntity?.parentId!!, eTagInConflict)
            }
        }
    }

    @Query(UPDATE_FILE_WITH_NEW_CONFLICT_STATUS)
    abstract fun updateFileWithConflictStatus(id: Long, eTagInConflict: String?)

    @Query(DISABLE_THUMBNAILS_FOR_FILE)
    abstract fun disableThumbnailsForFile(fileId: Long)

    @Query(DELETE_FILES_FOR_ACCOUNT)
    abstract fun deleteFilesForAccount(accountName: String)

    private fun moveSingleFile(
        sourceFile: OCFileEntity,
        targetFolder: OCFileEntity,
        finalRemotePath: String,
        finalStoragePath: String?
    ) {
        upsert(
            sourceFile.copy(
                parentId = targetFolder.id,
                remotePath = finalRemotePath,
                storagePath = finalStoragePath,
                availableOfflineStatus = getNewAvailableOfflineStatus(targetFolder.availableOfflineStatus, sourceFile.availableOfflineStatus)
            ).apply { id = sourceFile.id }
        )
    }

    private fun moveFolder(
        sourceFolder: OCFileEntity,
        targetFolder: OCFileEntity,
        targetRemotePath: String,
        targetStoragePath: String?
    ) {
        // 1. Move the folder
        val folderRemotePath =
            targetRemotePath.trimEnd(separatorChar).plus(separatorChar)
        val folderStoragePath =
            targetStoragePath?.trimEnd(separatorChar)?.plus(separatorChar)

        moveSingleFile(
            sourceFile = sourceFolder,
            targetFolder = targetFolder,
            finalRemotePath = folderRemotePath,
            finalStoragePath = sourceFolder.storagePath?.let { folderStoragePath }
        )

        // 2. Move its content
        val folderContent = getFolderContent(sourceFolder.id)

        folderContent.forEach { file ->
            val remotePathForChild = folderRemotePath.plus(file.name)
            val storagePathForChild = folderStoragePath?.plus(file.name)

            if (file.isFolder) {
                moveFolder(
                    sourceFolder = file,
                    targetFolder = sourceFolder,
                    targetRemotePath = remotePathForChild,
                    targetStoragePath = storagePathForChild
                )
            } else {
                moveSingleFile(
                    sourceFile = file,
                    targetFolder = sourceFolder,
                    finalRemotePath = remotePathForChild,
                    finalStoragePath = storagePathForChild
                )
            }
        }
    }

    /**
     * If folder is available offline, the child gets the AVAILABLE_OFFLINE_PARENT status
     * If child was available offline because of the previous parent, it won't be av offline anymore
     * Otherwise, keep the child available offline status
     */
    private fun getNewAvailableOfflineStatus(
        parentFolderAvailableOfflineStatus: Int?,
        currentFileAvailableOfflineStatus: Int?,
    ): Int {
        return if ((parentFolderAvailableOfflineStatus != null) &&
            parentFolderAvailableOfflineStatus.isOneOf(AVAILABLE_OFFLINE.ordinal, AVAILABLE_OFFLINE_PARENT.ordinal)
        ) {
            AVAILABLE_OFFLINE_PARENT.ordinal
        } else if (currentFileAvailableOfflineStatus == AVAILABLE_OFFLINE.ordinal) {
            AVAILABLE_OFFLINE.ordinal
        } else NOT_AVAILABLE_OFFLINE.ordinal
    }

    companion object {
        private const val SELECT_FILE_WITH_ID =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE id = :id"

        private const val SELECT_FILE_WITH_REMOTE_ID =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE remoteId = :remoteId"

        private const val SELECT_FILE_FROM_OWNER_WITH_REMOTE_PATH =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :owner " +
                    "AND remotePath = :remotePath"

        private const val DELETE_FILE_WITH_ID =
            "DELETE FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE id = :id"

        private const val SELECT_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId"

        private const val SELECT_FILTERED_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND remotePath LIKE '%' || :search || '%'"

        private const val SELECT_FILTERED_AVAILABLE_OFFLINE_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND keepInSync = '1' " +
                    "AND remotePath LIKE '%' || :search || '%'"

        private const val SELECT_FILTERED_SHARED_BY_LINK_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND remotePath LIKE '%' || :search || '%'" +
                    "AND sharedByLink LIKE '%1%' "

        private const val SELECT_FOLDER_BY_MIMETYPE =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND mimeType LIKE :mimeType || '%' "

        private const val SELECT_FILES_SHARED_BY_LINK =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :accountOwner " +
                    "AND sharedByLink LIKE '%1%' "

        private const val SELECT_FILES_AVAILABLE_OFFLINE_FROM_ACCOUNT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :accountOwner " +
                    "AND keepInSync = '1'"

        private const val SELECT_FILES_AVAILABLE_OFFLINE_FROM_EVERY_ACCOUNT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE keepInSync = '1'"

        private const val UPDATE_FILE_WITH_NEW_AVAILABLE_OFFLINE_STATUS =
            "UPDATE ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "SET keepInSync = :availableOfflineStatus " +
                    "WHERE id = :id"

        private const val UPDATE_FILE_WITH_NEW_CONFLICT_STATUS =
            "UPDATE ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "SET etagInConflict = :eTagInConflict " +
                    "WHERE id = :id"

        private const val DISABLE_THUMBNAILS_FOR_FILE =
            "UPDATE ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "SET needsToUpdateThumbnail = false " +
                    "WHERE id = :fileId"

        private const val UPDATE_FILES_STORAGE_DIRECTORY =
            "UPDATE ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "SET storagePath = `REPLACE`(storagePath, :oldDirectory, :newDirectory) " +
                    "WHERE storagePath IS NOT NULL"

        private const val DELETE_FILES_FOR_ACCOUNT =
            "DELETE FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :accountName"
    }
}
