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

package com.owncloud.android.capabilities.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
abstract class OCCapabilityDao {
    @Query(
        "SELECT * from " + ProviderTableMeta.CAPABILITIES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " = :accountName"
    )
    abstract fun getCapabilityForAccount(
        accountName: String
    ): LiveData<OCCapability>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocCapability: OCCapability): Long
}
