/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
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

package com.owncloud.android.settings.security

import android.content.Intent
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OCSettingsPatternLock {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(PatternLockActivity::class.java, true, false)
    val errorMessage = "PatternLock Activity error"
    val intent = Intent()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        //Clean preferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun patternLockView(){
        //Open Activity in pattern creation mode
        openPatternActivity(PatternLockActivity.ACTION_REQUEST_WITH_RESULT)

        onView(withText(R.string.pattern_configure_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.pattern_configure_your_pattern_explanation)).check(matches(isDisplayed()))
        onView(withId(R.id.pattern_lock_view)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel_pattern)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelPatternLock(){
        //Open Activity in pattern creation mode
        openPatternActivity(PatternLockActivity.ACTION_REQUEST_WITH_RESULT)

        onView(withId(R.id.cancel_pattern)).perform(click())
        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun removePatternLock() {
        //Save a pattern in Preferences
        storePattern()

        //Open Activity in pattern deletion mode
        openPatternActivity(PatternLockActivity.ACTION_CHECK_WITH_RESULT)

        onView(withText(R.string.pattern_remove_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.pattern_no_longer_required)).check(matches(isDisplayed()))
    }

    private fun storePattern(){
        var appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        appPrefs.putString(PatternLockActivity.KEY_PATTERN, "AAAAAAA")
        appPrefs.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)
        appPrefs.apply()
    }

    private fun openPatternActivity (mode: String) {
        intent.setAction(mode)
        activityRule.launchActivity(intent)
    }

}
