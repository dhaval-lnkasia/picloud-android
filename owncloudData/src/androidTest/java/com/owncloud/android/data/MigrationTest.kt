/**
 *   ownCloud Android client application
 *
 *   @author Abel García de Prada
 *   Copyright (C) 2019 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class MigrationTest {

    @Rule
    @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OwncloudDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun startInVersion27_containsCorrectData() {
        // Create the database with version 27
        with(
            helper.createDatabase(
                TEST_DB_NAME,
                DB_VERSION_27
            )
        ) {

            // Insert some data
            insert(ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)

            // Close database
            close()
        }

        // Verify that the data is correct
        val dbCapability =
            getMigratedRoomDatabase().capabilityDao().getCapabilitiesForAccount(OC_CAPABILITY.accountName!!)
        assertEquals(dbCapability.accountName, cv.getAsString(CAPABILITIES_ACCOUNT_NAME))
        assertEquals(dbCapability.versionMayor, cv.getAsInteger(CAPABILITIES_VERSION_MAYOR))
        assertEquals(dbCapability.versionMinor, cv.getAsInteger(CAPABILITIES_VERSION_MINOR))
        assertEquals(dbCapability.versionMicro, cv.getAsInteger(CAPABILITIES_VERSION_MICRO))
        assertEquals(dbCapability.corePollInterval, cv.getAsInteger(CAPABILITIES_CORE_POLLINTERVAL))
        assertEquals(
            dbCapability.filesSharingPublicExpireDateDays,
            cv.getAsInteger(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate27To28() {
        with(
            helper.createDatabase(
                TEST_DB_NAME,
                DB_VERSION_27
            )
        ) {
            // Database has schema version 27. Insert some values to test if they are migrated successfully.
            // We cannot use DAO classes because they expect the latest schema and we may not have some fields there.
            insert(ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)
            close()
        }

        // Re-open the database with version 28 and provide
        // MIGRATION_27_28 as the migration process.
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            DB_VERSION_28,
            true,
            OwncloudDatabase.MIGRATION_27_28
        )
        // MigrationTestHelper automatically verifies the schema changes.
        // Verify that the data was migrated properly.
        val dbCapability =
            getMigratedRoomDatabase().capabilityDao().getCapabilitiesForAccount(OC_CAPABILITY.accountName!!)
        assertEquals(dbCapability.accountName, cv.getAsString(CAPABILITIES_ACCOUNT_NAME))
        assertEquals(dbCapability.versionMayor, cv.getAsInteger(CAPABILITIES_VERSION_MAYOR))
        assertEquals(dbCapability.versionMinor, cv.getAsInteger(CAPABILITIES_VERSION_MINOR))
        assertEquals(dbCapability.versionMicro, cv.getAsInteger(CAPABILITIES_VERSION_MICRO))
        assertEquals(dbCapability.corePollInterval, cv.getAsInteger(CAPABILITIES_CORE_POLLINTERVAL))
        assertEquals(
            dbCapability.filesSharingPublicExpireDateDays,
            cv.getAsInteger(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
        )
        // Field introduced in this version (DB_VERSION_28), so it should be null.
        assertNull(dbCapability.filesSharingSearchMinLength)
    }

    @Test
    fun startInVersion28_containsCorrectData() {
        with(
            helper.createDatabase(
                TEST_DB_NAME,
                DB_VERSION_28
            )
        ) {
            insert(ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)
            close()
        }

        val dbCapability = getMigratedRoomDatabase().capabilityDao().getCapabilitiesForAccount(OC_CAPABILITY.accountName!!)
        assertEquals(OC_CAPABILITY.accountName, dbCapability.accountName)
        assertEquals(OC_CAPABILITY.versionMayor, dbCapability.versionMayor)
        assertEquals(OC_CAPABILITY.versionMinor, dbCapability.versionMinor)
        assertEquals(OC_CAPABILITY.versionMicro, dbCapability.versionMicro)
        assertEquals(OC_CAPABILITY.corePollInterval, dbCapability.corePollInterval)
        assertEquals(OC_CAPABILITY.filesSharingPublicExpireDateDays, dbCapability.filesSharingPublicExpireDateDays)
    }

    private fun getMigratedRoomDatabase(): OwncloudDatabase {
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OwncloudDatabase::class.java,
            TEST_DB_NAME
        )
            .addMigrations(OwncloudDatabase.MIGRATION_27_28)
            .build()

        helper.closeWhenFinished(database)
        return database
    }

    companion object {

        private const val TEST_DB_NAME = "migration-test"

        private const val DB_VERSION_27 = 27
        // Added a new capability: "search_min_length"
        private const val DB_VERSION_28 = 28

        private val cv = ContentValues().apply {
            put(CAPABILITIES_ACCOUNT_NAME, OC_CAPABILITY.accountName)
            put(CAPABILITIES_VERSION_MAYOR, OC_CAPABILITY.versionMayor)
            put(CAPABILITIES_VERSION_MINOR, OC_CAPABILITY.versionMinor)
            put(CAPABILITIES_VERSION_MICRO, OC_CAPABILITY.versionMicro)
            put(CAPABILITIES_CORE_POLLINTERVAL, OC_CAPABILITY.corePollInterval)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS, OC_CAPABILITY.filesSharingPublicExpireDateDays)
        }
    }
}
