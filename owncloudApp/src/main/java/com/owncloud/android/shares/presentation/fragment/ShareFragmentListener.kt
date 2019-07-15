/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.shares.presentation.fragment

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.shares.domain.OCShare

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in fragments handling [OCShare]s
 * to be communicated to the parent activity and potentially other fragments
 * contained in that activity.
 *
 *
 * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
 */
interface ShareFragmentListener {
    fun refreshAllShares()
    fun refreshCapabilities(shouldFetchFromNetwork: Boolean = true)

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    fun refreshPrivateShares()

    fun refreshPrivateShare(remoteId: Long)

    fun copyOrSendPrivateLink(file: OCFile)

    fun showSearchUsersAndGroups()

    fun showEditPrivateShare(share: OCShare)

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int
    )

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    fun showAddPublicShare(defaultLinkName: String)

    fun createPublicShare(
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    )

    fun showEditPublicShare(share: OCShare)

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    )

    fun showRemovePublicShare(share: OCShare)

    fun copyOrSendPublicLink(share: OCShare)

    /**************************************************************************************************************
     *************************************************** COMMON ***************************************************
     **************************************************************************************************************/

    fun removeShare(shareRemoteId: Long)
}
