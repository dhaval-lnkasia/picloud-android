/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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

package com.owncloud.android.presentation.viewmodels.settings

import android.content.Intent
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest : ViewModelTest() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()

        settingsViewModel = SettingsViewModel(
            preferencesProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `is pattern set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any())} returns true

        val patternSet = settingsViewModel.isPatternSet()

        assertTrue(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `is pattern set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val patternSet = settingsViewModel.isPatternSet()

        assertFalse(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `handle enable passcode - ok`() {
        val data: Intent = mockk()
        val passcode = "1111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passCodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passCodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
            for (i in 1..4) {
                preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                    passcode.substring(i - 1, i))
            }
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        }
    }

    @Test
    fun `handle enable passcode - ko - data intent is null`() {
        val passCodeEnableOk = settingsViewModel.handleEnablePasscode(null)

        assertFalse(passCodeEnableOk)
    }

    @Test
    fun `handle enable passcode - ko - passcode is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any())} returns null

        val passCodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passCodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
        }
    }

    @Test
    fun `handle enable passcode - ko - passcode has length 3`() {
        val data: Intent = mockk()
        val passcode = "111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passCodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passCodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
        }
    }

    @Test
    fun `handle enable passcode - ko - passcode has length 5`() {
        val data: Intent = mockk()
        val passcode = "11111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passCodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passCodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
        }
    }

    @Test
    fun `is passcode set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any())} returns true

        val passcodeSet = settingsViewModel.isPasscodeSet()

        assertTrue(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `is passcode set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val passcodeSet = settingsViewModel.isPasscodeSet()

        assertFalse(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }


}
