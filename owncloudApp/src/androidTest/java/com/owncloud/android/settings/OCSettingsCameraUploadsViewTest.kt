/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.settings

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OCSettingsCameraUploadsViewTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    @Before
    fun setUp() {
    }

    @Test
    fun checkTitle() {
        onView(withText(R.string.actionbar_settings)).check(matches(isDisplayed()))
    }

    @Test
    fun testPictureUploadsView() {
        onView(withText(R.string.prefs_camera_picture_upload)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_picture_upload_summary)).check(matches(isDisplayed()))
    }

    @Test
    fun testVideoUploadsView() {
        onView(withText(R.string.prefs_camera_video_upload)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_video_upload_summary)).check(matches(isDisplayed()))
    }

}