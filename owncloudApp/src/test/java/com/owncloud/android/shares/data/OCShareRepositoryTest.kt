/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.shares.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.data.datasources.LocalSharesDataSource
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.domain.OCShareRepository
import com.owncloud.android.util.InstantAppExecutors
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.mock
import com.owncloud.android.vo.Resource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class OCShareRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val filePath = "/Photos/"

    private val localSharesDataSource = mock(LocalSharesDataSource::class.java)

    private val remoteShares = arrayListOf(
        TestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = filePath,
            isFolder = true,
            name = "Photos folder link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = "${filePath}img",
            isFolder = true,
            name = "Photos folder link 1",
            shareLink = "http://server:port/s/2"
        ),
        TestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = filePath,
            isFolder = true,
            name = "Photos folder link 2",
            shareLink = "http://server:port/s/3"
        ),
        TestUtil.createRemoteShare(
            shareType = ShareType.USER.value, // Private share
            path = filePath,
            isFolder = true,
            shareWith = "username",
            sharedWithDisplayName = "John"
        ),
        TestUtil.createRemoteShare(
            shareType = ShareType.GROUP.value, // Private share
            path = filePath,
            isFolder = true,
            shareWith = "username2",
            sharedWithDisplayName = "Sophie"
        )
    )

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val privateShare = listOf(
        TestUtil.createPrivateShare(
            path = filePath,
            isFolder = true,
            shareWith = "username2",
            sharedWithDisplayName = "Sophie"
        )
    )

    private val privateShareTypes = listOf(
        ShareType.USER, ShareType.GROUP, ShareType.FEDERATED
    )

    @Test
    fun loadPrivateSharesForFileFromNetwork() {
        val localData = MutableLiveData<List<OCShare>>() // Local shares

        val remoteOperationResult =
            TestUtil.createRemoteOperationResultMock(ShareParserResult(remoteShares), true) // Remote shares

        val privateSharesAsLiveData = loadPrivateSharesAsLiveData(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        privateSharesAsLiveData.observeForever(observer)

        localData.postValue(null)

        // Get private shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving shares from server...

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).replaceSharesForFile(
            remoteShares.map { remoteShare ->
                OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )

        // Observe changes in database livedata when there's a new public share
        localData.postValue(
            privateShare
        )

        verify(observer).onChanged(Resource.success(privateShare))
    }

    @Test
    fun loadEmptyPrivateSharesForFileFromNetwork() {
        val localData = MutableLiveData<List<OCShare>>()

        val remoteOperationResult =
            TestUtil.createRemoteOperationResultMock(ShareParserResult(arrayListOf()), true)

        val data = loadPrivateSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving public shares from server...

        // When there's no shares in server for a specific file, delete them locally
        verify(localSharesDataSource).deleteSharesForFile(filePath, "admin@server")

        // Observe changes in database livedata when the list of shares is empty
        localData.postValue(listOf())

        verify(observer).onChanged(Resource.success(listOf()))
    }

    @Test
    fun loadPrivateSharesForFileFromNetworkWithError() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = privateShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val data = loadPrivateSharesAsLiveData(localData, remoteOperationResult)

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesForFileAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, localData.value, exception = exception
            )
        )
    }

    private fun loadPrivateSharesAsLiveData(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<List<OCShare>>> {
        val ocShareRepository = createRepositoryWithPrivateData(localData, remoteOperationResult)
        return ocShareRepository.getPrivateSharesForFile(filePath)
    }

    private fun createRepositoryWithPrivateData(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): OCShareRepository =
        createShareRepositoryWithDataSources(
            localData, remoteOperationResult, privateShareTypes
        )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val publicShare = listOf(
        TestUtil.createPublicShare(
            path = filePath,
            isFolder = true,
            name = "Photos folder link",
            shareLink = "http://server:port/s/1"
        )
    )

    @Test
    fun loadPublicSharesForFileFromNetworkSuccessfully() {
        val localData = MutableLiveData<List<OCShare>>()

        val remoteOperationResult =
            TestUtil.createRemoteOperationResultMock(ShareParserResult(remoteShares), true)

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving shares from server...

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).replaceSharesForFile(
            remoteShares.map { remoteShare ->
                OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )

        // Observe changes in database livedata when there's a new public share
        localData.postValue(
            publicShare
        )

        verify(observer).onChanged(Resource.success(publicShare))
    }

    @Test
    fun loadEmptyPublicSharesForFileFromNetwork() {
        val localData = MutableLiveData<List<OCShare>>()

        val remoteOperationResult =
            TestUtil.createRemoteOperationResultMock(ShareParserResult(arrayListOf()), true)

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // When there's no shares in server for a specific file, delete them locally
        verify(localSharesDataSource).deleteSharesForFile(filePath, "admin@server")

        // Observe changes in database livedata when the list of shares is empty
        localData.postValue(listOf())

        verify(observer).onChanged(Resource.success(listOf()))
    }

    @Test
    fun loadPublicSharesForFileFromNetworkWithError() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = publicShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesForFileAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, localData.value, exception = exception
            )
        )
    }

    @Test
    fun insertPublicShareForFileOnNetwork() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = publicShare

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[1])), true
        )

        val data = insertPublicShare(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).insert(
            arrayListOf(remoteShares[1]).map { remoteShare ->
                OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )
    }

    @Test
    fun insertPublicShareForFileOnNetworkWithError() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = publicShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.SHARE_NOT_FOUND,
            exception = exception
        )

        val data = insertPublicShare(localData, remoteOperationResult)

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND, exception = exception
            )
        )
    }

    @Test
    fun updatePublicShareForFileOnNetwork() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = publicShare

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[2])), true
        )

        val data = updatePublicShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and updated in database
        verify(localSharesDataSource).update(
            OCShare.fromRemoteShare(remoteShares[2]).also { it.accountOwner = "admin@server" }
        )
    }

    @Test
    fun deletePublicShareForFileOnNetwork() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = publicShare

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()), true
        )

        val data = deletePublicShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Retrieving public shares from server...
        verify(localSharesDataSource).deleteShare(
            1
        )
    }

    private fun loadPublicSharesAsLiveData(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<List<OCShare>>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)
        return ocShareRepository.getPublicSharesForFile(filePath)
    }

    private fun insertPublicShare(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)

        return ocShareRepository.insertPublicShareForFile(
            filePath,
            1,
            "Photos folder link 3",
            "1234",
            -1,
            true
        )
    }

    private fun updatePublicShare(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)

        return ocShareRepository.updatePublicShareForFile(
            1,
            "Photos folder link updated",
            "123456",
            2000,
            1,
            false
        )
    }

    private fun deletePublicShare(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)
        return ocShareRepository.deletePublicShare(
            1
        )
    }

    private fun createShareRepositoryWithPublicData(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): OCShareRepository =
        createShareRepositoryWithDataSources(localData, remoteOperationResult, listOf(ShareType.PUBLIC_LINK))

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun createShareRepositoryWithDataSources(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>,
        shareTypes: List<ShareType>
    ): OCShareRepository {
        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                filePath, "admin@server", shareTypes
            )
        ).thenReturn(
            localData
        )

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        return OCShareRepository(
            InstantAppExecutors(),
            localSharesDataSource,
            remoteSharesDataSource,
            "admin@server"
        )
    }
}
