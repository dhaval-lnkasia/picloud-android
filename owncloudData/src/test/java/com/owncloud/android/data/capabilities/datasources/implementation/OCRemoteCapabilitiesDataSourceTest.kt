/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.data.capabilities.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.lib.resources.status.services.implementation.OCCapabilityService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteCapabilitiesDataSourceTest {
    private lateinit var ocRemoteCapabilitiesDataSource: OCRemoteCapabilitiesDataSource

    private val ocCapabilityService: OCCapabilityService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)
    private val remoteCapabilityMapper = RemoteCapabilityMapper()

    @Before
    fun setUp() {
        every { clientManager.getCapabilityService(any()) } returns ocCapabilityService

        ocRemoteCapabilitiesDataSource =
            OCRemoteCapabilitiesDataSource(
                clientManager,
                remoteCapabilityMapper
            )
    }

    @Test
    fun readRemoteCapabilities() {
        val accountName = OC_ACCOUNT_NAME

        val remoteCapability = remoteCapabilityMapper.toRemote(OC_CAPABILITY)!!

        val getRemoteCapabilitiesOperationResult = createRemoteOperationResultMock(remoteCapability, true)

        every { ocCapabilityService.getCapabilities() } returns getRemoteCapabilitiesOperationResult

        // Get capability from remote datasource
        val capabilities = ocRemoteCapabilitiesDataSource.getCapabilities(accountName)

        assertNotNull(capabilities)

        assertEquals(OC_CAPABILITY.accountName, capabilities.accountName)
        assertEquals(OC_CAPABILITY.versionMajor, capabilities.versionMajor)
        assertEquals(OC_CAPABILITY.versionMinor, capabilities.versionMinor)
        assertEquals(OC_CAPABILITY.versionMicro, capabilities.versionMicro)

        verify(exactly = 1) {
            ocCapabilityService.getCapabilities()
        }
    }
}
