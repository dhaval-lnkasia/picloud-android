/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.data.sharing.shares.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_PRIVATE_SHARE
import com.owncloud.android.testutil.OC_PUBLIC_SHARE
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.testutil.OC_SHAREE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OCShareRepositoryTest {

    private val localShareDataSource = mockk<LocalShareDataSource>(relaxUnitFun =  true)
    private val remoteShareDataSource = mockk<RemoteShareDataSource>(relaxUnitFun = true)
    private val ocShareRepository = OCShareRepository(localShareDataSource, remoteShareDataSource)

    private val listOfShares = listOf(OC_PRIVATE_SHARE, OC_PUBLIC_SHARE)
    private val filePath = "/Images"
    private val password = "password"
    private val permissions = 1
    private val expiration = RemoteShare.INIT_EXPIRATION_DATE_IN_MILLIS

    @Test
    fun `insertPrivateShare inserts a private OCShare correctly`() {
        every {
            remoteShareDataSource.insert(
                remoteFilePath = filePath,
                shareType = OC_PRIVATE_SHARE.shareType,
                shareWith = OC_SHAREE.shareWith,
                permissions = permissions,
                name = "",
                password = "",
                expirationDate = expiration,
                accountName = OC_ACCOUNT_NAME
            )
        } returns OC_PRIVATE_SHARE

        every {
            localShareDataSource.insert(OC_PRIVATE_SHARE)
        } returns 1

        ocShareRepository.insertPrivateShare(filePath, OC_PRIVATE_SHARE.shareType, OC_SHAREE.shareWith, permissions, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.insert(
                remoteFilePath = filePath,
                shareType = OC_PRIVATE_SHARE.shareType,
                shareWith = OC_SHAREE.shareWith,
                permissions = permissions,
                name = "",
                password = "",
                expirationDate = expiration,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.insert(OC_PRIVATE_SHARE)
        }
    }

    @Test
    fun `updatePrivateShare updates a private OCShare correctly`() {
        every {
            remoteShareDataSource.updateShare(
                remoteId = OC_PRIVATE_SHARE.remoteId,
                name = "",
                password = "",
                expirationDateInMillis = expiration,
                permissions = permissions,
                accountName = OC_ACCOUNT_NAME
            )
        } returns OC_PRIVATE_SHARE

        every {
            localShareDataSource.update(OC_PRIVATE_SHARE)
        } returns 1

        ocShareRepository.updatePrivateShare(OC_PRIVATE_SHARE.remoteId, permissions, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                remoteId = OC_PRIVATE_SHARE.remoteId,
                name = "",
                password = "",
                expirationDateInMillis = expiration,
                permissions = permissions,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.update(OC_PRIVATE_SHARE)
        }
    }

    @Test
    fun `insertPublicShare inserts a public OCShare correctly`() {
        every {
            remoteShareDataSource.insert(
                remoteFilePath = filePath,
                shareType = OC_PUBLIC_SHARE.shareType,
                shareWith = "",
                permissions = permissions,
                name = OC_PUBLIC_SHARE.name!!,
                password = password,
                expirationDate = expiration,
                accountName = OC_ACCOUNT_NAME
            )
        } returns OC_PUBLIC_SHARE

        every {
            localShareDataSource.insert(OC_PUBLIC_SHARE)
        } returns 1

        ocShareRepository.insertPublicShare(filePath, permissions, OC_PUBLIC_SHARE.name!!, password, expiration, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.insert(
                remoteFilePath = filePath,
                shareType = OC_PUBLIC_SHARE.shareType,
                shareWith = "",
                permissions = permissions,
                name = OC_PUBLIC_SHARE.name!!,
                password = password,
                expirationDate = expiration,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.insert(OC_PUBLIC_SHARE)
        }
    }

    @Test
    fun `updatePublicShare updates a public OCShare correctly`() {
        every {
            remoteShareDataSource.updateShare(
                remoteId = OC_PUBLIC_SHARE.remoteId,
                name = OC_PUBLIC_SHARE.name!!,
                password = password,
                expirationDateInMillis = expiration,
                permissions = permissions,
                accountName = OC_ACCOUNT_NAME
            )
        } returns OC_PUBLIC_SHARE

        every {
            localShareDataSource.update(OC_PUBLIC_SHARE)
        } returns 1

        ocShareRepository.updatePublicShare(OC_PUBLIC_SHARE.remoteId, OC_PUBLIC_SHARE.name!!, password, expiration, permissions, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                remoteId = OC_PUBLIC_SHARE.remoteId,
                name = OC_PUBLIC_SHARE.name!!,
                password = password,
                expirationDateInMillis = expiration,
                permissions = permissions,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.update(OC_PUBLIC_SHARE)
        }
    }

    @Test
    fun `getSharesAsLiveData returns a LiveData with a list of OCShares`() {
        val sharesLiveDataList: LiveData<List<OCShare>> = MutableLiveData(listOfShares)

        every {
            localShareDataSource.getSharesAsLiveData(
                filePath = filePath,
                accountName = OC_ACCOUNT_NAME,
                shareTypes = listOf(ShareType.PUBLIC_LINK, ShareType.USER, ShareType.GROUP, ShareType.FEDERATED)
            )
        } returns sharesLiveDataList

        val sharesResult = ocShareRepository.getSharesAsLiveData(filePath, OC_ACCOUNT_NAME)
        assertEquals(sharesResult, sharesLiveDataList)

        verify(exactly = 1) {
            localShareDataSource.getSharesAsLiveData(
                filePath = filePath,
                accountName = OC_ACCOUNT_NAME,
                shareTypes = listOf(ShareType.PUBLIC_LINK, ShareType.USER, ShareType.GROUP, ShareType.FEDERATED)
            )
        }
    }

    @Test
    fun `getShareAsLiveData returns a LiveData with an OCShare`() {
        val shareLiveData: LiveData<OCShare> = MutableLiveData(OC_SHARE)

        every {
            localShareDataSource.getShareAsLiveData(OC_SHARE.remoteId)
        } returns shareLiveData

        val shareResult = ocShareRepository.getShareAsLiveData(OC_SHARE.remoteId)
        assertEquals(shareResult, shareLiveData)

        verify(exactly = 1) {
            localShareDataSource.getShareAsLiveData(OC_SHARE.remoteId)
        }
    }

    @Test
    fun `refreshSharesFromNetwork refreshes shares correctly when the list of shares is not empty`() {
        every {
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = OC_ACCOUNT_NAME
            )
        } returns listOfShares

        every {
            localShareDataSource.replaceShares(listOfShares)
        } returns listOf(1, 1, 1, 1)

        ocShareRepository.refreshSharesFromNetwork(filePath, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.replaceShares(listOfShares)
        }
    }

    @Test
    fun `refreshSharesFromNetwork refreshes shares correctly when the list of shares is empty`() {
        every {
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = OC_ACCOUNT_NAME
            )
        } returns emptyList()

        every {
            localShareDataSource.replaceShares(emptyList())
        } returns emptyList()

        ocShareRepository.refreshSharesFromNetwork(filePath, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = OC_ACCOUNT_NAME
            )
            localShareDataSource.deleteSharesForFile(filePath, OC_ACCOUNT_NAME)
            localShareDataSource.replaceShares(emptyList())
        }
    }

    @Test
    fun `deleteShare deletes a share correctly`() {
        every {
            localShareDataSource.deleteShare(OC_SHARE.remoteId)
        } returns 1

        ocShareRepository.deleteShare(OC_SHARE.remoteId, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteShareDataSource.deleteShare(OC_SHARE.remoteId, OC_ACCOUNT_NAME)
            localShareDataSource.deleteShare(OC_SHARE.remoteId)
        }
    }

}
