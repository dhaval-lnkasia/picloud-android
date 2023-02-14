/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.MoveIntoDescendantException
import com.owncloud.android.domain.exceptions.MoveIntoSameFolderException
import com.owncloud.android.domain.exceptions.MoveIntoAnotherSpaceException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

/**
 * Move a list of files with the SAME hierarchy to a target folder.
 *
 * Moving files to a descendant, moving files to the same directory
 * or moving files to another space will throw an exception.
 */
class MoveFileUseCase(
    private val fileRepository: FileRepository
) : BaseUseCaseWithResult<Unit, MoveFileUseCase.Params>() {

    override fun run(params: Params) {
        validateOrThrowException(params.listOfFilesToMove, params.targetFolder)

        return fileRepository.moveFile(
            listOfFilesToMove = params.listOfFilesToMove,
            targetFile = params.targetFolder
        )
    }

    @Throws(IllegalArgumentException::class, MoveIntoSameFolderException::class, MoveIntoDescendantException::class, MoveIntoAnotherSpaceException::class)
    fun validateOrThrowException(listOfFilesToMove: List<OCFile>, targetFolder: OCFile) {
        require(listOfFilesToMove.isNotEmpty())
        if (listOfFilesToMove[0].spaceId != targetFolder.spaceId) {
            throw MoveIntoAnotherSpaceException()
        } else if (listOfFilesToMove.any { targetFolder.remotePath.startsWith(it.remotePath) }) {
            throw MoveIntoDescendantException()
        } else if (listOfFilesToMove.any { it.parentId == targetFolder.id }) {
            throw MoveIntoSameFolderException()
        }
    }

    data class Params(
        val listOfFilesToMove: List<OCFile>,
        val targetFolder: OCFile
    )
}
