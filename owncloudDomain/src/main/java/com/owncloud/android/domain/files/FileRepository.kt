/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Christian Schabesberger
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

package com.owncloud.android.domain.files

import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface FileRepository {
    fun getUrlToOpenInWeb(openWebEndpoint: String, fileId: String): String
    fun createFolder(remotePath: String, parentFolder: OCFile)
    fun copyFile(listOfFilesToCopy: List<OCFile>, targetFolder: OCFile)
    fun getFileById(fileId: Long): OCFile?
    fun getFileByIdAsFlow(fileId: Long): Flow<OCFile?>
    fun getFileByRemotePath(remotePath: String, owner: String): OCFile?
    fun getSearchFolderContent(fileListOption: FileListOption, folderId: Long, search: String): List<OCFile>
    fun getFolderContent(folderId: Long): List<OCFile>
    fun getFolderContentWithSyncInfoAsFlow(folderId: Long): Flow<List<OCFileWithSyncInfo>>
    fun getFolderImages(folderId: Long): List<OCFile>
    fun getSharedByLinkWithSyncInfoForAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile>
    fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile>
    fun moveFile(listOfFilesToMove: List<OCFile>, targetFile: OCFile)
    fun readFile(remotePath: String, accountName: String): OCFile
    fun refreshFolder(remotePath: String, accountName: String): List<OCFile>
    fun deleteFile(listOfFilesToDelete: List<OCFile>, removeOnlyLocalCopy: Boolean)
    fun renameFile(ocFile: OCFile, newName: String)
    fun saveFile(file: OCFile)
    fun saveConflict(fileId: Long, eTagInConflict: String)
    fun cleanConflict(fileId: Long)
    fun saveUploadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun saveDownloadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun cleanWorkersUuid(fileId: Long)

    fun disableThumbnailsForFile(fileId: Long)
    fun updateFileWithNewAvailableOfflineStatus(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus)
    fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String)
}
