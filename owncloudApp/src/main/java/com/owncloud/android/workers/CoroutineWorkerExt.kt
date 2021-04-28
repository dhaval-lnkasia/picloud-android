/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import com.owncloud.android.R

fun CoroutineWorker.showNotificationWithProgress(
    progress: Int,
    maxValue: Int,
    contentTitle: String,
    contentText: String,
    notificationChannelId: String
) {

    val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notificationBuilder = NotificationCompat.Builder(
        applicationContext,
        notificationChannelId
    ).setContentTitle(contentTitle)
        .setSmallIcon(R.drawable.notification_icon)
        .setOngoing(true)
        .setWhen(System.currentTimeMillis())
        .setContentText(contentText)

    if (progress == maxValue) {
        notificationBuilder.setTimeoutAfter(1_000)
    } else {
        notificationBuilder.setProgress(maxValue, progress, false)
    }
    notificationManager.notify(123, notificationBuilder.build())
}
