/*
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.viewmodels.files

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoFragment
import com.owncloud.android.usecases.transfers.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.GetLiveDataForDownloadingFileUseCase
import kotlinx.coroutines.launch
import java.util.UUID

class FileDetailsViewModel(
    private val cancelDownloadForFileUseCase: CancelDownloadForFileUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val getLiveDataForDownloadingFileUseCase: GetLiveDataForDownloadingFileUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val workManager: WorkManager,
) : ViewModel() {

    val pendingDownloads = MediatorLiveData<WorkInfo?>()

    private val _ongoingDownload = MediatorLiveData<WorkInfo?>()
    val ongoingDownload: LiveData<WorkInfo?> = _ongoingDownload

    fun startListeningToDownloadsFromAccountAndFile(account: Account, file: OCFile) {
        pendingDownloads.addSource(
            getLiveDataForDownloadingFileUseCase.execute(GetLiveDataForDownloadingFileUseCase.Params(account, file))
        ) { workInfo ->
            if (workInfo != null) {
                startListeningToWorkInfo(uuid = workInfo.id)
                pendingDownloads.postValue(workInfo)
            }
        }
    }

    private fun startListeningToWorkInfo(uuid: UUID) {
        _ongoingDownload.addSource(
            workManager.getWorkInfoByIdLiveData(uuid)
        ) {
            _ongoingDownload.postValue(it)
        }
    }

    fun isDownloadPending(account: Account, file: OCFile): Boolean =
        workManager.isDownloadPending(account, file)

    fun cancelCurrentDownload(file: OCFile) {
        cancelDownloadForFileUseCase.execute(CancelDownloadForFileUseCase.Params(file))
    }

    // TODO: I don't like this at all. Move navigation to a common place.
    fun navigateToPreviewOrOpenFile(fileDisplayActivity: FileDisplayActivity, file: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = file.id!!))
            val fileWaitingToPreview = useCaseResult.getDataOrNull()
            viewModelScope.launch(coroutinesDispatcherProvider.main) {
                when {
                    PreviewAudioFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startAudioPreview(fileWaitingToPreview!!, 0)
                    }
                    PreviewVideoFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startVideoPreview(fileWaitingToPreview!!, 0)
                    }
                    PreviewTextFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startTextPreview(fileWaitingToPreview)
                    }
                    else -> fileDisplayActivity.fileOperationsHelper.openFile(fileWaitingToPreview)
                }
            }
        }
    }
}
