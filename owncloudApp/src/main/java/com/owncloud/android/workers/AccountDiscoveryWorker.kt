/**
 * ownCloud Android client application
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
package com.owncloud.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AccountDiscoveryWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    private val refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase by inject()
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase by inject()
    private val getPersonalAndProjectSpacesForAccountUseCase: GetPersonalAndProjectSpacesForAccountUseCase by inject()
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase by inject()

    override suspend fun doWork(): Result {
        val accountName = workerParameters.inputData.getString(KEY_PARAM_DISCOVERY_ACCOUNT)
        val account = AccountUtils.getOwnCloudAccountByName(appContext, accountName)
        Timber.d("Account Discovery for account: $accountName and accountName: ${account.name}")

        if (accountName.isNullOrBlank() || account == null) return Result.failure()

        // 1. Refresh capabilities for account
        refreshCapabilitiesFromServerAsyncUseCase.execute(RefreshCapabilitiesFromServerAsyncUseCase.Params(accountName))
        val capabilities = getStoredCapabilitiesUseCase.execute(GetStoredCapabilitiesUseCase.Params(accountName))

        val spacesAvailableForAccount = capabilities?.isSpacesAllowed() == true && AccountUtils.isFeatureSpacesAllowedForAccount(appContext, account)

        val rootFoldersToDiscover = mutableListOf<OCFile>()
        // 2.1 Account does not support spaces
        if (!spacesAvailableForAccount) {
            val rootLegacyFolder = getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH, null)).getDataOrNull()
            rootLegacyFolder?.let {
                rootFoldersToDiscover.add(it)
            }
        } else {
            // 2.2 Account does support spaces
            refreshSpacesFromServerAsyncUseCase.execute(RefreshSpacesFromServerAsyncUseCase.Params(accountName))
            val spaces = getPersonalAndProjectSpacesForAccountUseCase.execute(GetPersonalAndProjectSpacesForAccountUseCase.Params(accountName))
            spaces.forEach { space ->
                // Create the root file for each space.
                val rootFolderForSpace =
                    getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH, space.root.id)).getDataOrNull()
                rootFolderForSpace?.let {
                    rootFoldersToDiscover.add(it)
                }

            }
        }

        // 3. Refresh only the root folder for the moment. Discovering the entire account may not be the best solution. High-Demanding
        rootFoldersToDiscover.forEach {
            synchronizeFolderUseCase.execute(
                SynchronizeFolderUseCase.Params(
                    accountName = it.owner,
                    remotePath = it.remotePath,
                    spaceId = it.spaceId,
                    syncMode = SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER
                )
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_PARAM_DISCOVERY_ACCOUNT = "KEY_PARAM_DISCOVERY_ACCOUNT"
    }
}
