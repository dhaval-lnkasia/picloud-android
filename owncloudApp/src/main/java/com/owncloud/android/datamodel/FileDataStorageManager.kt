/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 *
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import androidx.core.util.Pair
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT
import com.owncloud.android.datamodel.OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_DAV_CHUNKING_VERSION
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_UNDELETE
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_VERSIONING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_RESHARING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_EDITION
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_STRING
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CONTENT_URI
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.CONTENT_URI_CAPABILITIES
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.FILE_KEEP_IN_SYNC
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta.FILE_PATH
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.extensions.getIntFromColumnOrThrow
import com.owncloud.android.extensions.getLongFromColumnOrThrow
import com.owncloud.android.extensions.getStringFromColumnOrEmpty
import com.owncloud.android.extensions.getStringFromColumnOrThrow
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_AUDIO
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.MIME_PREFIX_VIDEO
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFilesSharedByLinkUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetFolderImagesUseCase
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.extensions.getIntFromColumnOrThrow
import com.owncloud.android.extensions.getStringFromColumnOrEmpty
import com.owncloud.android.extensions.getStringFromColumnOrThrow
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.util.Vector

class FileDataStorageManager : KoinComponent {

    private var contentResolver: ContentResolver? = null
    private var contentProviderClient: ContentProviderClient? = null
    var account: Account
    private var mContext: Context? = null

    constructor(activity: Context, account: Account, cr: ContentResolver) {
        contentProviderClient = null
        contentResolver = cr
        this.account = account
        mContext = activity
    }

    constructor(activity: Context, account: Account, cp: ContentProviderClient) {
        contentProviderClient = cp
        contentResolver = null
        this.account = account
        mContext = activity
    }

    /**
     * Get a collection with all the files set by the user as available offline, from all the accounts
     * in the device, putting away the folders
     *
     *
     * This is the only method working with a NULL account in [.mAccount]. Not something to do often.
     *
     * @return List with all the files set by the user as available offline.
     */
    fun getAvailableOfflineFilesFromEveryAccount(): List<Pair<OCFile, String>> {
        return listOf()
        // FIXME: 13/10/2020 : New_arch: Av.Offline
//        val result = ArrayList<Pair<OCFile, String>>()
//        var cursorOnKeptInSync: Cursor? = null
//        try {
//            cursorOnKeptInSync = performQuery(
//                uri = CONTENT_URI,
//                projection = null,
//                selection = "$FILE_KEEP_IN_SYNC = ? OR $FILE_KEEP_IN_SYNC = ?",
//                selectionArgs = arrayOf(AVAILABLE_OFFLINE.value.toString(), AVAILABLE_OFFLINE_PARENT.value.toString()),
//                sortOrder = null,
//                performWithContentProviderClient = false
//            )
//
//            if (cursorOnKeptInSync != null && cursorOnKeptInSync.moveToFirst()) {
//                var file: OCFile?
//                var accountName: String
//                do {
//                    file = createFileInstance(cursorOnKeptInSync)
//                    accountName =
//                        cursorOnKeptInSync.getStringFromColumnOrEmpty(FILE_ACCOUNT_OWNER)
//                    if (!file!!.isFolder && AccountUtils.exists(accountName, mContext)) {
//                        result.add(Pair(file, accountName))
//                    }
//                } while (cursorOnKeptInSync.moveToNext())
//            } else {
//                Timber.d("No available offline files found")
//            }
//
//        } catch (e: Exception) {
//            Timber.e(e, "Exception retrieving all the available offline files")
//
//        } finally {
//            cursorOnKeptInSync?.close()
//        }
//
//        return result
    }

    /**
     * Get a collection with all the files set by the user as available offline, from current account
     * putting away files whose parent is also available offline
     *
     * @return List with all the files set by current user as available offline.
     */
    val availableOfflineFilesFromCurrentAccount: Vector<OCFile>
        get() {
            return Vector()
            // FIXME: 13/10/2020 : New_arch: Av.Offline
//            val result = Vector<OCFile>()
//
//            var cursorOnKeptInSync: Cursor? = null
//            try {
//                cursorOnKeptInSync = performQuery(
//                    uri = CONTENT_URI,
//                    projection = null,
//                    selection = "($FILE_KEEP_IN_SYNC = ? AND NOT $FILE_KEEP_IN_SYNC = ? ) AND $FILE_ACCOUNT_OWNER = ? ",
//                    selectionArgs = arrayOf(
//                        AVAILABLE_OFFLINE.value.toString(),
//                        AVAILABLE_OFFLINE_PARENT.value.toString(),
//                        account.name
//                    ),
//                    sortOrder = null,
//                    performWithContentProviderClient = false
//                )
//
//                if (cursorOnKeptInSync != null && cursorOnKeptInSync.moveToFirst()) {
//                    var file: OCFile?
//                    do {
//                        file = createFileInstance(cursorOnKeptInSync)
//                        result.add(file)
//                    } while (cursorOnKeptInSync.moveToNext())
//                } else {
//                    Timber.d("No available offline files found")
//                }
//
//            } catch (e: Exception) {
//                Timber.e(e, "Exception retrieving all the available offline files")
//
//            } finally {
//                cursorOnKeptInSync?.close()
//            }
//
//            return result.apply { sort() }
        }

    fun sharedByLinkFilesFromCurrentAccount(): List<OCFile>? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFilesSharedByLinkUseCase: GetFilesSharedByLinkUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFilesSharedByLinkUseCase.execute(GetFilesSharedByLinkUseCase.Params(account.name))
        }.getDataOrNull() ?: emptyList()
        result
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFileByPath(path: String): OCFile? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(account.name, path))
        }.getDataOrNull()
        result
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFileById(id: Long): OCFile? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFileByIdUseCase: GetFileByIdUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFileByIdUseCase.execute(GetFileByIdUseCase.Params(id))
        }.getDataOrNull()
        result
    }

    fun fileExists(id: Long): Boolean = getFileById(id) != null

    fun fileExists(path: String): Boolean = getFileByPath(path) != null

    fun getFolderContent(f: OCFile?): List<OCFile> {
        return if (f != null && f.isFolder && f.id != -1L) {
            // TODO: Remove !!
            getFolderContent(f.id!!)
        } else {
            listOf()
        }
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFolderImages(folder: OCFile?): List<OCFile> = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFolderImagesUseCase: GetFolderImagesUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            // TODO: Remove !!
            getFolderImagesUseCase.execute(GetFolderImagesUseCase.Params(folderId = folder!!.id!!))
        }.getDataOrNull()
        result ?: listOf()
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun saveFile(file: OCFile): Boolean {
        runBlocking(CoroutinesDispatcherProvider().io) {
            val saveFileOrFolderUseCase: SaveFileOrFolderUseCase by inject()

            val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
                // TODO: We may need to return if it was updated or not.
                saveFileOrFolderUseCase.execute(SaveFileOrFolderUseCase.Params(file))
            }.getDataOrNull()
        }

        return true
        // FIXME: 29/10/2020 : New_arch: Conflicts
//        var overriden = false
//        val cv = ContentValues().apply {
//            put(FILE_MODIFIED, file.modificationTimestamp)
//            put(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, file.modificationTimestampAtLastSyncForData)
//            put(FILE_CREATION, file.creationTimestamp)
//            put(FILE_CONTENT_LENGTH, file.fileLength)
//            put(FILE_CONTENT_TYPE, file.mimetype)
//            put(FILE_NAME, file.fileName)
//            put(FILE_PARENT, file.parentId)
//            put(FILE_PATH, file.remotePath)
//            if (!file.isFolder) put(FILE_STORAGE_PATH, file.storagePath)
//            put(FILE_ACCOUNT_OWNER, account.name)
//            put(FILE_LAST_SYNC_DATE, file.lastSyncDateForProperties)
//            put(FILE_LAST_SYNC_DATE_FOR_DATA, file.lastSyncDateForData)
//            put(FILE_ETAG, file.etag)
//            put(FILE_TREE_ETAG, file.treeEtag)
//            put(FILE_SHARED_VIA_LINK, if (file.isSharedViaLink) 1 else 0)
//            put(FILE_SHARED_WITH_SHAREE, if (file.isSharedWithSharee) 1 else 0)
//            put(FILE_PERMISSIONS, file.permissions)
//            put(FILE_REMOTE_ID, file.remoteId)
//            put(FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail())
//            put(FILE_IS_DOWNLOADING, file.isDownloading)
//            put(FILE_ETAG_IN_CONFLICT, file.etagInConflict)
//            put(FILE_PRIVATE_LINK, file.privateLink)
//        }
//
//        val sameRemotePath = fileExists(file.remotePath)
//        if (sameRemotePath || fileExists(file.id!!)) {  // for renamed files; no more delete and create
//
//            val oldFile: OCFile?
//            if (sameRemotePath) {
//                oldFile = getFileByPath(file.remotePath)
//                file.fileId = oldFile!!.fileId
//            } else {
//                oldFile = getFileById(file.fileId)
//            }
//
//            overriden = true
//            try {
//                performUpdate(
//                    uri = CONTENT_URI,
//                    contentValues = cv,
//                    where = "$_ID=?",
//                    selectionArgs = arrayOf(file.fileId.toString())
//                ).let { Timber.d("Rows updated: $it") }
//            } catch (e: Exception) {
//                Timber.e(e, "Fail to insert insert file to database ${e.message}")
//            }
//
//        } else {
//            // new file
//            setInitialAvailableOfflineStatus(file, cv)
//
//            val resultUri: Uri? =
//                try {
//                    performInsert(CONTENT_URI_FILE, cv)
//                } catch (e: RemoteException) {
//                    Timber.e(e, "Fail to insert insert file to database ${e.message}")
//                    null
//                }
//            resultUri?.let {
//                file.fileId = it.pathSegments[1].toLong()
//            }
//        }
//
//        return overriden
    }

    /**
     * Inserts or updates the list of files contained in a given folder.
     *
     *
     * CALLER IS THE RESPONSIBLE FOR GRANTING RIGHT UPDATE OF INFORMATION, NOT THIS METHOD.
     * HERE ONLY DATA CONSISTENCY SHOULD BE GRANTED
     *
     * @param folder
     * @param updatedFiles
     * @param filesToRemove
     */
    fun saveFolder(
        folder: OCFile, updatedFiles: Collection<OCFile>, filesToRemove: Collection<OCFile>
    ) {
        Timber.d("Saving folder ${folder.remotePath} with ${updatedFiles.size} children and ${filesToRemove.size} files to remove")
        // FIXME: 29/10/2020 : New_arch: Conflicts
//        val operations = ArrayList<ContentProviderOperation>(updatedFiles.size)
//
//        // prepare operations to insert or update files to save in the given folder
//        for (file in updatedFiles) {
//            val cv = ContentValues().apply {
//                put(FILE_MODIFIED, file.modificationTimestamp)
//                put(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, file.modificationTimestampAtLastSyncForData)
//                put(FILE_CREATION, file.creationTimestamp)
//                put(FILE_CONTENT_LENGTH, file.fileLength)
//                put(FILE_CONTENT_TYPE, file.mimetype)
//                put(FILE_NAME, file.fileName)
//                put(FILE_PARENT, folder.fileId)
//                put(FILE_PATH, file.remotePath)
//                if (!file.isFolder) put(FILE_STORAGE_PATH, file.storagePath)
//                put(FILE_ACCOUNT_OWNER, account.name)
//                put(FILE_LAST_SYNC_DATE, file.lastSyncDateForProperties)
//                put(FILE_LAST_SYNC_DATE_FOR_DATA, file.lastSyncDateForData)
//                put(FILE_ETAG, file.etag)
//                put(FILE_TREE_ETAG, file.treeEtag)
//                put(FILE_SHARED_VIA_LINK, if (file.isSharedViaLink) 1 else 0)
//                put(FILE_SHARED_WITH_SHAREE, if (file.isSharedWithSharee) 1 else 0)
//                put(FILE_PERMISSIONS, file.permissions)
//                put(FILE_REMOTE_ID, file.remoteId)
//                put(FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail())
//                put(FILE_IS_DOWNLOADING, file.isDownloading)
//                put(FILE_ETAG_IN_CONFLICT, file.etagInConflict)
//                put(FILE_PRIVATE_LINK, file.privateLink)
//            }
//
//            val existsByPath = fileExists(file.remotePath)
//            if (existsByPath || fileExists(file.fileId)) {
//                // updating an existing file
//                operations.add(
//                    ContentProviderOperation.newUpdate(CONTENT_URI).withValues(cv).withSelection(
//                        "$_ID=?",
//                        arrayOf(file.fileId.toString())
//                    )
//                        .build()
//                )
//            } else {
//                // adding a new file
//                setInitialAvailableOfflineStatus(file, cv)
//                operations.add(ContentProviderOperation.newInsert(CONTENT_URI).withValues(cv).build())
//            }
//        }
//
//        // prepare operations to remove files in the given folder
//        val where = "$FILE_ACCOUNT_OWNER=? AND $FILE_PATH=?"
//        var whereArgs: Array<String>?
//        for (file in filesToRemove) {
//            if (file.parentId == folder.fileId) {
//                whereArgs = arrayOf(account.name, file.remotePath)
//                if (file.isFolder) {
//                    operations.add(
//                        ContentProviderOperation.newDelete(
//                            ContentUris.withAppendedId(CONTENT_URI_DIR, file.fileId)
//                        ).withSelection(where, whereArgs).build()
//                    )
//
//                    val localFolder = File(FileStorageUtils.getDefaultSavePathFor(account.name, file))
//                    if (localFolder.exists()) {
//                        removeLocalFolder(localFolder)
//                    }
//                } else {
//                    operations.add(
//                        ContentProviderOperation.newDelete(
//                            ContentUris.withAppendedId(CONTENT_URI_FILE, file.fileId)
//                        ).withSelection(where, whereArgs).build()
//                    )
//
//                    if (file.isDown) {
//                        val path = file.storagePath
//                        File(path).delete()
//                        triggerMediaScan(path) // notify MediaScanner about removed file
//                    }
//                }
//            }
//        }
//
//        // update metadata of folder
//        val cv = ContentValues().apply {
//            put(FILE_MODIFIED, folder.modificationTimestamp)
//            put(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, folder.modificationTimestampAtLastSyncForData)
//            put(FILE_CREATION, folder.creationTimestamp)
//            put(FILE_CONTENT_LENGTH, folder.fileLength)
//            put(FILE_CONTENT_TYPE, folder.mimetype)
//            put(FILE_NAME, folder.fileName)
//            put(FILE_PARENT, folder.parentId)
//            put(FILE_PATH, folder.remotePath)
//            put(FILE_ACCOUNT_OWNER, account.name)
//            put(FILE_LAST_SYNC_DATE, folder.lastSyncDateForProperties)
//            put(FILE_LAST_SYNC_DATE_FOR_DATA, folder.lastSyncDateForData)
//            put(FILE_ETAG, folder.etag)
//            put(FILE_TREE_ETAG, folder.treeEtag)
//            put(FILE_SHARED_VIA_LINK, if (folder.isSharedViaLink) 1 else 0)
//            put(FILE_SHARED_WITH_SHAREE, if (folder.isSharedWithSharee) 1 else 0)
//            put(FILE_PERMISSIONS, folder.permissions)
//            put(FILE_REMOTE_ID, folder.remoteId)
//            put(FILE_PRIVATE_LINK, folder.privateLink)
//        }
//
//        operations.add(
//            ContentProviderOperation.newUpdate(CONTENT_URI).withValues(cv).withSelection(
//                "$_ID=?", arrayOf(folder.fileId.toString())
//            )
//                .build()
//        )
//
//        // apply operations in batch
//        var results: Array<ContentProviderResult>? = null
//        Timber.d("Sending ${operations.size} operations to FileContentProvider")
//        try {
//            results =
//                if (contentResolver != null) {
//                    contentResolver!!.applyBatch(MainApp.authority, operations)
//                } else {
//                    contentProviderClient!!.applyBatch(operations)
//                }
//
//        } catch (e: OperationApplicationException) {
//            Timber.e(e, "Exception in batch of operations ${e.message}")
//
//        } catch (e: RemoteException) {
//            Timber.e(e, "Exception in batch of operations ${e.message}")
//        }
//
//        // update new id in file objects for insertions
//        if (results != null) {
//            val filesIt = updatedFiles.iterator()
//            var file: OCFile?
//            for (i in results.indices) {
//                file = if (filesIt.hasNext()) {
//                    filesIt.next()
//                } else {
//                    null
//                }
//                results[i].uri?.let { newId ->
//                    file?.fileId = newId.pathSegments[1].toLong()
//                }
//            }
//        }
    }

    /**
     * Adds the appropriate initial value for FILE_KEEP_IN_SYNC to
     * passed [ContentValues] instance.
     *
     * @param file [OCFile] which av-offline property will be set.
     * @param cv   [ContentValues] instance where the property is added.
     */
    private fun setInitialAvailableOfflineStatus(file: OCFile, cv: ContentValues) {
        // set appropriate av-off folder depending on ancestor
        val inFolderAvailableOffline = isAnyAncestorAvailableOfflineFolder(file)
        if (inFolderAvailableOffline) {
            cv.put(FILE_KEEP_IN_SYNC, AVAILABLE_OFFLINE_PARENT.value)
        } else {
            cv.put(FILE_KEEP_IN_SYNC, NOT_AVAILABLE_OFFLINE.value)
        }
    }

    fun migrateLegacyToScopedPath(
        legacyStorageDirectoryPath: String,
        rootStorageDirectoryPath: String,
    ) {
        val filesToUpdatePath: MutableList<OCFile> = mutableListOf()

        val cursor: Cursor? =
            try {
                performQuery(
                    uri = CONTENT_URI,
                    projection = null,
                    sortOrder = "$FILE_PATH ASC ",
                    selection = "$FILE_ACCOUNT_OWNER = ? ",
                    selectionArgs = arrayOf(account.name),
                )
            } catch (e: RemoteException) {
                Timber.e(e)
                null
            }

        cursor?.let { allFilesCursor ->
            if (allFilesCursor.moveToFirst()) {
                do {
                    val ocFile = createFileInstance(allFilesCursor)
                    ocFile?.let {
                        if (it.storagePath != null) {
                            filesToUpdatePath.add(it)
                        }
                    }
                } while (allFilesCursor.moveToNext())
            }
            cursor.close()
        }

        val filesWithPathUpdated = filesToUpdatePath.map {
            it.apply { storagePath = storagePath?.replace(legacyStorageDirectoryPath, rootStorageDirectoryPath) }
        }

        filesWithPathUpdated.forEach {
            saveFile(it)
        }

        Timber.d("Updated path for ${filesWithPathUpdated.size} downloaded files")
    }

    /**
     * Updates available-offline status of OCFile received as a parameter, with its current value.
     *
     *
     * Saves the new value property for the given file in persistent storage.
     *
     *
     * If the file is a folder, updates the value of all its known descendants accordingly.
     *
     * @param file File which available-offline status will be updated.
     * @return 'true' if value was updated, 'false' otherwise.
     */
    fun saveLocalAvailableOfflineStatus(file: OCFile): Boolean {
        return false
        // FIXME: 13/10/2020 : New_arch: Av.Offline
//        if (!fileExists(file.fileId)) {
//            return false
//        }
//
//        val newStatus = file.availableOfflineStatus
//        require(AVAILABLE_OFFLINE_PARENT != newStatus) {
//            "Forbidden value, AVAILABLE_OFFLINE_PARENT is calculated, cannot be set"
//        }
//
//        val cv = ContentValues()
//        cv.put(FILE_KEEP_IN_SYNC, file.availableOfflineStatus.value)
//
//        var updatedCount: Int
//        try {
//            updatedCount = performUpdate(
//                uri = CONTENT_URI,
//                contentValues = cv,
//                where = "$_ID=?",
//                selectionArgs = arrayOf(file.fileId.toString())
//            )
//
//            // Update descendants
//            if (file.isFolder && updatedCount > 0) {
//                val descendantsCv = ContentValues()
//                if (newStatus == AVAILABLE_OFFLINE) {
//                    // all descendant files MUST be av-off due to inheritance, not due to previous value
//                    descendantsCv.put(FILE_KEEP_IN_SYNC, AVAILABLE_OFFLINE_PARENT.value)
//                } else {
//                    // all descendant files MUST be not-available offline
//                    descendantsCv.put(FILE_KEEP_IN_SYNC, NOT_AVAILABLE_OFFLINE.value)
//                }
//                val selectDescendants = selectionForAllDescendantsOf(file)
//                updatedCount += performUpdate(
//                    uri = CONTENT_URI,
//                    contentValues = descendantsCv,
//                    where = selectDescendants.first,
//                    selectionArgs = selectDescendants.second
//                )
//            }
//
//        } catch (e: RemoteException) {
//            Timber.e(e, "Fail updating available offline status")
//            return false
//        }
//
//        return updatedCount > 0
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFolderContent(parentId: Long): List<OCFile> = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFolderContentUseCase: GetFolderContentUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFolderContentUseCase.execute(GetFolderContentUseCase.Params(parentId))
        }.getDataOrNull()
        result ?: listOf()
    }

    /**
     * Checks if it is favorite or it is inside a favorite folder
     *
     * @param file [OCFile] which ancestors will be searched.
     * @return true/false
     */
    // FIXME: 13/10/2020 : New_arch: Av.Offline
    private fun isAnyAncestorAvailableOfflineFolder(file: OCFile) = false //getAvailableOfflineAncestorOf(file) != null

    /**
     * Returns ancestor folder with available offline status AVAILABLE_OFFLINE.
     *
     * @param file [OCFile] which ancestors will be searched.
     * @return Ancestor folder with available offline status AVAILABLE_OFFLINE, or null if
     * does not exist.
     */
    // FIXME: 13/10/2020 : New_arch: Av.Offline
    private fun getAvailableOfflineAncestorOf(file: OCFile): OCFile? {
        return null
//        var avOffAncestor: OCFile? = null
//        val parent = getFileById(file.parentId)
//        if (parent != null && parent.isFolder) {  // file is null for the parent of the root folder
//            if (parent.availableOfflineStatus == AVAILABLE_OFFLINE) {
//                avOffAncestor = parent
//            } else if (parent.fileName != ROOT_PATH) {
//                avOffAncestor = getAvailableOfflineAncestorOf(parent)
//            }
//        }
//        return avOffAncestor
    }

    // FIXME: 13/10/2020 : New_arch: Migration
    private fun createFileInstance(c: Cursor?): OCFile? = null//c?.let {
//        OCFile(it.getStringFromColumnOrThrow(FILE_PATH)).apply {
//            fileId = it.getLongFromColumnOrThrow(_ID)
//            parentId = it.getLongFromColumnOrThrow(FILE_PARENT)
//            mimetype = it.getStringFromColumnOrThrow(FILE_CONTENT_TYPE)
//            if (!isFolder) {
//                storagePath = it.getStringFromColumnOrThrow(FILE_STORAGE_PATH)
//                if (storagePath == null) {
//                    // try to find existing file and bind it with current account;
//                    // with the current update of SynchronizeFolderOperation, this won't be
//                    // necessary anymore after a full synchronization of the account
//                    val f = File(FileStorageUtils.getDefaultSavePathFor(account.name, this))
//                    if (f.exists()) {
//                        storagePath = f.absolutePath
//                        lastSyncDateForData = f.lastModified()
//                    }
//                }
//            }
//            fileLength = it.getLongFromColumnOrThrow(FILE_CONTENT_LENGTH)
//            creationTimestamp = it.getLongFromColumnOrThrow(FILE_CREATION)
//            modificationTimestamp = it.getLongFromColumnOrThrow(FILE_MODIFIED)
//            modificationTimestampAtLastSyncForData = it.getLongFromColumnOrThrow(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)
//            lastSyncDateForProperties = it.getLongFromColumnOrThrow(FILE_LAST_SYNC_DATE)
//            lastSyncDateForData = it.getLongFromColumnOrThrow(FILE_LAST_SYNC_DATE_FOR_DATA)
//            availableOfflineStatus = fromValue(it.getIntFromColumnOrThrow(FILE_KEEP_IN_SYNC))
//            etag = it.getStringFromColumnOrThrow(FILE_ETAG)
//            treeEtag = it.getStringFromColumnOrThrow(FILE_TREE_ETAG)
//            isSharedViaLink = it.getIntFromColumnOrThrow(FILE_SHARED_VIA_LINK) == 1
//            isSharedWithSharee = it.getIntFromColumnOrThrow(FILE_SHARED_WITH_SHAREE) == 1
//            permissions = it.getStringFromColumnOrThrow(FILE_PERMISSIONS)
//            remoteId = it.getStringFromColumnOrThrow(FILE_REMOTE_ID)
//            setNeedsUpdateThumbnail(it.getIntFromColumnOrThrow(FILE_UPDATE_THUMBNAIL) == 1)
//            isDownloading = it.getIntFromColumnOrThrow(FILE_IS_DOWNLOADING) == 1
//            etagInConflict = it.getStringFromColumnOrThrow(FILE_ETAG_IN_CONFLICT)
//            privateLink = it.getStringFromColumnOrThrow(FILE_PRIVATE_LINK)
//        }
//    }

    // FIXME: 13/10/2020 : New_arch: Conflicts
    fun saveConflict(file: OCFile, eTagInConflictFromParameter: String?) {
//        var eTagInConflict = eTagInConflictFromParameter
//        if (!file.isDown) {
//            eTagInConflict = null
//        }
//        val cv = ContentValues()
//        cv.put(FILE_ETAG_IN_CONFLICT, eTagInConflict)
//        val updated =
//            try {
//                performUpdate(
//                    uri = CONTENT_URI_FILE,
//                    contentValues = cv,
//                    where = "$_ID=?",
//                    selectionArgs = arrayOf(file.fileId.toString())
//                )
//            } catch (e: RemoteException) {
//                Timber.e(e, "Failed saving conflict in database ${e.message}")
//                0
//            }
//
//        Timber.d("Number of files updated with CONFLICT: $updated")
//
//        if (updated > 0) {
//            if (eTagInConflict != null) {
//                /// set conflict in all ancestor folders
//
//                var parentId = file.parentId
//                val ancestorIds = HashSet<String>()
//                while (parentId != ROOT_PARENT_ID.toLong()) {
//                    ancestorIds.add(parentId.toString())
//                    parentId = getFileById(parentId)!!.parentId
//                }
//
//                if (ancestorIds.size > 0) {
//                    val whereBuffer = StringBuffer()
//                    whereBuffer.append(_ID).append(" IN (")
//                    for (i in 0 until ancestorIds.size - 1) {
//                        whereBuffer.append("?,")
//                    }
//                    whereBuffer.append("?")
//                    whereBuffer.append(")")
//
//                    try {
//                        performUpdate(
//                            uri = CONTENT_URI_FILE,
//                            contentValues = cv,
//                            where = whereBuffer.toString(),
//                            selectionArgs = ancestorIds.toTypedArray()
//                        )
//                    } catch (e: RemoteException) {
//                        Timber.e(e, "Failed saving conflict in database ${e.message}")
//                    }
//                } // else file is ROOT folder, no parent to set in conflict
//
//            } else {
//                /// update conflict in ancestor folders
//                // (not directly unset; maybe there are more conflicts below them)
//                var parentPath = file.remotePath
//                if (parentPath.endsWith(File.separator)) {
//                    parentPath = parentPath.substring(0, parentPath.length - 1)
//                }
//                parentPath = parentPath.substring(0, parentPath.lastIndexOf(File.separator) + 1)
//
//                Timber.d("checking parents to remove conflict; STARTING with $parentPath")
//                while (parentPath.isNotEmpty()) {
//
//                    val whereForDescendantsInConflict = FILE_ETAG_IN_CONFLICT + " IS NOT NULL AND " +
//                            FILE_CONTENT_TYPE + " != 'DIR' AND " +
//                            FILE_ACCOUNT_OWNER + " = ? AND " +
//                            FILE_PATH + " LIKE ?"
//                    val descendantsInConflict: Cursor? =
//                        try {
//                            performQuery(
//                                uri = CONTENT_URI_FILE,
//                                projection = arrayOf(_ID),
//                                selection = whereForDescendantsInConflict,
//                                selectionArgs = arrayOf(account.name, "$parentPath%"),
//                                sortOrder = null
//                            )
//                        } catch (e: RemoteException) {
//                            Timber.e(e, "Failed querying for descendants in conflict ${e.message}")
//                            null
//                        }
//
//                    if (descendantsInConflict == null || descendantsInConflict.count == 0) {
//                        Timber.d("NO MORE conflicts in $parentPath")
//
//                        try {
//                            performUpdate(
//                                uri = CONTENT_URI_FILE,
//                                contentValues = cv,
//                                where = "$FILE_ACCOUNT_OWNER=? AND $FILE_PATH=?",
//                                selectionArgs = arrayOf(account.name, parentPath)
//                            )
//                        } catch (e: RemoteException) {
//                            Timber.e(e, "Failed saving conflict in database ${e.message}")
//                        }
//
//                    } else {
//                        Timber.d("STILL ${descendantsInConflict.count} in $parentPath")
//                    }
//
//                    descendantsInConflict?.close()
//
//                    parentPath = parentPath.substring(0, parentPath.length - 1)  // trim last /
//                    parentPath = parentPath.substring(0, parentPath.lastIndexOf(File.separator) + 1)
//                    Timber.d("checking parents to remove conflict; NEXT $parentPath")
//                }
//            }
//        }
    }

    fun saveCapabilities(capability: RemoteCapability): RemoteCapability {

        // Prepare capabilities data
        val cv = ContentValues().apply {
            put(CAPABILITIES_ACCOUNT_NAME, account.name)
            put(CAPABILITIES_VERSION_MAYOR, capability.versionMayor)
            put(CAPABILITIES_VERSION_MINOR, capability.versionMinor)
            put(CAPABILITIES_VERSION_MICRO, capability.versionMicro)
            put(CAPABILITIES_VERSION_STRING, capability.versionString)
            put(CAPABILITIES_VERSION_EDITION, capability.versionEdition)
            put(CAPABILITIES_CORE_POLLINTERVAL, capability.corePollinterval)
            put(CAPABILITIES_DAV_CHUNKING_VERSION, capability.chunkingVersion)
            put(CAPABILITIES_SHARING_API_ENABLED, capability.filesSharingApiEnabled.value)
            put(CAPABILITIES_SHARING_PUBLIC_ENABLED, capability.filesSharingPublicEnabled.value)
            put(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED, capability.filesSharingPublicPasswordEnforced.value)
            put(
                CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY,
                capability.filesSharingPublicPasswordEnforcedReadOnly.value
            )
            put(
                CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE,
                capability.filesSharingPublicPasswordEnforcedReadWrite.value
            )
            put(
                CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY,
                capability.filesSharingPublicPasswordEnforcedUploadOnly.value
            )
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED, capability.filesSharingPublicExpireDateEnabled.value)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS, capability.filesSharingPublicExpireDateDays)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED, capability.filesSharingPublicExpireDateEnforced.value)
            put(CAPABILITIES_SHARING_PUBLIC_UPLOAD, capability.filesSharingPublicUpload.value)
            put(CAPABILITIES_SHARING_PUBLIC_MULTIPLE, capability.filesSharingPublicMultiple.value)
            put(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY, capability.filesSharingPublicSupportsUploadOnly.value)
            put(CAPABILITIES_SHARING_RESHARING, capability.filesSharingResharing.value)
            put(CAPABILITIES_SHARING_FEDERATION_OUTGOING, capability.filesSharingFederationOutgoing.value)
            put(CAPABILITIES_SHARING_FEDERATION_INCOMING, capability.filesSharingFederationIncoming.value)
            put(CAPABILITIES_FILES_BIGFILECHUNKING, capability.filesBigFileChunking.value)
            put(CAPABILITIES_FILES_UNDELETE, capability.filesUndelete.value)
            put(CAPABILITIES_FILES_VERSIONING, capability.filesVersioning.value)
        }

        if (capabilityExists(account.name)) {
            try {
                performUpdate(
                    uri = CONTENT_URI_CAPABILITIES,
                    contentValues = cv,
                    where = "$CAPABILITIES_ACCOUNT_NAME=?",
                    selectionArgs = arrayOf(account.name)
                )
            } catch (e: RemoteException) {
                Timber.e("Fail to insert insert file to database ${e.message}")
            }
        } else {
            val resultUri: Uri? =
                try {
                    performInsert(CONTENT_URI_CAPABILITIES, cv)
                } catch (e: RemoteException) {
                    Timber.e("Fail to insert insert capability to database ${e.message}")
                    null
                }
            resultUri?.let {
                capability.accountName = account.name
            }
        }

        return capability
    }

    private fun capabilityExists(accountName: String): Boolean {
        val c = getCapabilityCursorForAccount(accountName)
        var exists = false
        if (c != null) {
            exists = c.moveToFirst()
            c.close()
        }
        return exists
    }

    private fun getCapabilityCursorForAccount(accountName: String): Cursor? =
        try {
            performQuery(
                uri = CONTENT_URI_CAPABILITIES,
                projection = null,
                selection = "$CAPABILITIES_ACCOUNT_NAME=? ",
                selectionArgs = arrayOf(accountName),
                sortOrder = null
            )
        } catch (e: RemoteException) {
            Timber.e("Couldn't determine capability existence, assuming non existence: ${e.message}")
            null
        }

    fun getCapability(accountName: String): OCCapability? {
        var capability: OCCapability? = null
        val cursor = getCapabilityCursorForAccount(accountName)

        // default value with all UNKNOWN
        cursor?.use {
            if (it.moveToFirst()) {
                capability = createCapabilityInstance(it)
            }
        }
        return capability
    }

    private fun createCapabilityInstance(c: Cursor): OCCapability {
        return OCCapability(
            accountName = c.getStringFromColumnOrThrow(CAPABILITIES_ACCOUNT_NAME),
            versionMayor = c.getIntFromColumnOrThrow(CAPABILITIES_VERSION_MAYOR),
            versionMinor = c.getIntFromColumnOrThrow(CAPABILITIES_VERSION_MINOR),
            versionMicro = c.getIntFromColumnOrThrow(CAPABILITIES_VERSION_MICRO),
            versionString = c.getStringFromColumnOrThrow(CAPABILITIES_VERSION_STRING),
            versionEdition = c.getStringFromColumnOrThrow(CAPABILITIES_VERSION_EDITION),
            corePollInterval = c.getIntFromColumnOrThrow(CAPABILITIES_CORE_POLLINTERVAL),
            davChunkingVersion = c.getStringFromColumnOrEmpty(CAPABILITIES_DAV_CHUNKING_VERSION),
            filesSharingApiEnabled = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_API_ENABLED)),
            filesSharingPublicEnabled = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_ENABLED)),
            filesSharingPublicPasswordEnforced = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)
            ),
            filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)
            ),
            filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)
            ),
            filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)
            ),
            filesSharingPublicExpireDateEnabled = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)
            ),
            filesSharingPublicExpireDateDays = c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS),
            filesSharingPublicExpireDateEnforced = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)
            ),
            filesSharingPublicUpload = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_UPLOAD)),
            filesSharingPublicMultiple = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_MULTIPLE)),
            filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.fromValue(
                c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY)
            ),
            filesSharingResharing = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_RESHARING)),
            filesSharingFederationOutgoing = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_FEDERATION_OUTGOING)),
            filesSharingFederationIncoming = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_SHARING_FEDERATION_INCOMING)),
            filesSharingUserProfilePicture = CapabilityBooleanType.UNKNOWN,
            filesBigFileChunking = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_FILES_BIGFILECHUNKING)),
            filesUndelete = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_FILES_UNDELETE)),
            filesVersioning = CapabilityBooleanType.fromValue(c.getIntFromColumnOrThrow(CAPABILITIES_FILES_VERSIONING))
        )
    }

    // FIXME: 13/10/2020 : New_arch: Av.Offline
    private fun selectionForAllDescendantsOf(file: OCFile): Pair<String, Array<String>> {
        val selection = "$FILE_ACCOUNT_OWNER=? AND $FILE_PATH LIKE ? "
        val selectionArgs = arrayOf(account.name, "${file.remotePath}_%") // one or more characters after remote path
        return Pair(selection, selectionArgs)
    }

    private fun performQuery(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        performWithContentResolver: Boolean = true,
        performWithContentProviderClient: Boolean = true
    ): Cursor? {
        val withContentResolver = contentResolver != null && performWithContentResolver
        val withContentProvider = contentProviderClient != null && performWithContentProviderClient
        return when {
            withContentResolver -> contentResolver?.query(uri, projection, selection, selectionArgs, sortOrder)
            withContentProvider -> contentProviderClient?.query(uri, projection, selection, selectionArgs, sortOrder)
            else -> null
        }
    }

    private fun performUpdate(
        uri: Uri,
        contentValues: ContentValues?,
        where: String?,
        selectionArgs: Array<String>?
    ): Int {
        val withContentResolver = contentResolver != null
        val withContentProvider = contentProviderClient != null
        return when {
            withContentResolver -> contentResolver?.update(uri, contentValues, where, selectionArgs) ?: 0
            withContentProvider -> contentProviderClient?.update(uri, contentValues, where, selectionArgs) ?: 0
            else -> 0
        }
    }

    private fun performInsert(
        url: Uri,
        contentValues: ContentValues?
    ): Uri? {
        val withContentResolver = contentResolver != null
        val withContentProvider = contentProviderClient != null
        return when {
            withContentResolver -> contentResolver?.insert(url, contentValues)
            withContentProvider -> contentProviderClient?.insert(url, contentValues)
            else -> null
        }
    }

    private fun performDelete(
        url: Uri,
        where: String?,
        selectionArgs: Array<String>?
    ): Int {
        val withContentResolver = contentResolver != null
        val withContentProvider = contentProviderClient != null
        return when {
            withContentResolver -> contentResolver?.delete(url, where, selectionArgs) ?: 0
            withContentProvider -> contentProviderClient?.delete(url, where, selectionArgs) ?: 0
            else -> 0
        }
    }

    companion object {
        const val ROOT_PARENT_ID = 0
    }
}
