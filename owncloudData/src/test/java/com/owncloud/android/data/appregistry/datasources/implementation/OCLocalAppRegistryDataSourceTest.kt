/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.data.appregistry.datasources.implementation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.appregistry.db.AppRegistryDao
import com.owncloud.android.data.appregistry.db.AppRegistryEntity
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY_MIMETYPE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OCLocalAppRegistryDataSourceTest {
    private lateinit var ocLocalAppRegistryDataSource: OCLocalAppRegistryDataSource
    private val appRegistryDao = mockk<AppRegistryDao>(relaxUnitFun = true)
    private val mimetype = "DIR"
    private val ocAppRegistryEntity = AppRegistryEntity(
        accountName = OC_ACCOUNT_NAME,
        mimeType = mimetype,
        ext = "appRegistryMimeTypes.ext",
        appProviders = "null",
        name = "appRegistryMimeTypes.name",
        icon = "appRegistryMimeTypes.icon",
        description = "appRegistryMimeTypes.description",
        allowCreation = true,
        defaultApplication = "appRegistryMimeTypes.defaultApplication",
    )

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {

        ocLocalAppRegistryDataSource =
            OCLocalAppRegistryDataSource(
                appRegistryDao,
            )
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a flow with AppRegistryMimeType object`() = runTest {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } returns flowOf(ocAppRegistryEntity)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        val result = appRegistry.first()
        assertEquals(OC_APP_REGISTRY_MIMETYPE, result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns null when DAO no receive values from db`() = runTest {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } returns flowOf(null)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        val result = appRegistry.first()
        assertNull(result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForMimeTypeAsStream returns an Exception when DAO return an Exception`() = runTest {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } throws Exception()

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        val result = appRegistry.first()
        assertNull(result)
        verify(exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns a flow with a list of AppRegistryMimeType object`() = runTest {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(any()) } returns flowOf(listOf(ocAppRegistryEntity))

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        val result = appRegistry.first()
        assertEquals(listOf(OC_APP_REGISTRY_MIMETYPE), result)


        verify(exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns empty list when DAO return empty list`() = runTest {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(any()) } returns flowOf(emptyList())

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        val result = appRegistry.first()
        assertEquals(emptyList<AppRegistryMimeType>(), result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `saveAppRegistryForAccount should save the AppRegistry entities`() = runTest {
        val appRegistry = AppRegistry(
            OC_ACCOUNT_NAME, mutableListOf(
                AppRegistryMimeType("mime_type_1", "ext_1", emptyList(), "name_1", "icon_1", "description_1", true, "default_app_1"),
                AppRegistryMimeType("mime_type_2", "ext_2", emptyList(), "name_2", "icon_2", "description_2", true, "default_app_2")
            )
        )

        ocLocalAppRegistryDataSource.saveAppRegistryForAccount(appRegistry)

        verify(exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName) }
        verify(exactly = 1) { appRegistryDao.upsertAppRegistries(any()) }
    }

    @Test(expected = Exception::class)
    fun `saveAppRegistryForAccount should returns an Exception`() = runTest {
        val appRegistry = AppRegistry(
            OC_ACCOUNT_NAME, mutableListOf(
                AppRegistryMimeType("mime_type_1", "ext_1", emptyList(), "name_1", "icon_1", "description_1", true, "default_app_1"),
                AppRegistryMimeType("mime_type_2", "ext_2", emptyList(), "name_2", "icon_2", "description_2", true, "default_app_2")
            )
        )

        every { appRegistryDao.deleteAppRegistryForAccount(OC_ACCOUNT_NAME) } throws Exception()
        every { appRegistryDao.upsertAppRegistries(any()) } throws Exception()

        ocLocalAppRegistryDataSource.saveAppRegistryForAccount(appRegistry)

        verify(exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName) }
        verify(exactly = 1) { appRegistryDao.upsertAppRegistries(any()) }
    }

    @Test
    fun `deleteAppRegistryForAccount should delete appRegistry`() = runTest {

        ocLocalAppRegistryDataSource.deleteAppRegistryForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(any()) }
    }
}
