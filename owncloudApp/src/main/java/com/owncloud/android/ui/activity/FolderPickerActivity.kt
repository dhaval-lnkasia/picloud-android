/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author David González Verdugo
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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.LinearLayout
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.presentation.files.filelist.MainFileListFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

open class FolderPickerActivity : FileActivity(),
    FileFragment.ContainerActivity,
    OnClickListener,
    MainFileListFragment.FileActions {

    private lateinit var cancelButton: Button
    private lateinit var chooseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate() start")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.files_folder_picker) // Beware - inflated in other activities too

        // Allow or disallow touches with other visible windows
        val filesFolderPickerLayout = findViewById<LinearLayout>(R.id.filesFolderPickerLayout)
        filesFolderPickerLayout.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        if (savedInstanceState == null) {
            initAndShowListOfFilesFragment()
        }

        // Set callback listeners for UI elements
        initPickerListeners()
        
        // Action bar setup
        setupStandardToolbar(
            title = null,
            displayHomeAsUpEnabled = false,
            homeButtonEnabled = false,
            displayShowTitleEnabled = true,
        )

        // Set action button text
        setActionButtonText()

        Timber.d("onCreate() end")
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)

        if (account != null) {
            updateFileFromDB()

            var folder = file
            if (folder == null || !folder.isFolder) {
                // Fall back to root folder
                file = storageManager.getFileByPath(OCFile.ROOT_PATH)
                folder = file
            }

            if (!stateWasRecovered) {
                val listOfFolders = getListOfFilesFragment()
                listOfFolders?.navigateToFolder(folder)
            }

            updateNavigationElementsInActionBar()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnValue = true
        when (item.itemId) {
            android.R.id.home -> {
                val currentDir = getCurrentFolder()
                if (currentDir != null && currentDir.parentId != 0L) {
                    onBackPressed()
                }
            }
            else -> returnValue = super.onOptionsItemSelected(item)
        }

        return returnValue
    }

    override fun onBackPressed() {
        val listOfFiles = getListOfFilesFragment()
        if (listOfFiles != null) {  // Should never be null, indeed
            val fileBeforeBrowsingUp = listOfFiles.getCurrentFile()
            if (fileBeforeBrowsingUp.parentId != null && fileBeforeBrowsingUp.parentId == OCFile.ROOT_PARENT_ID) {
                // If we are already at root, let's finish the picker. No sense to keep browsing up.
                finish()
                return
            }
            listOfFiles.onBrowseUp()
            file = listOfFiles.getCurrentFile()
            updateNavigationElementsInActionBar()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            cancelButton -> finish()
            chooseButton -> {
                val data = Intent().apply {
                    val targetFiles = intent.getParcelableArrayListExtra<OCFile>(EXTRA_FILES)
                    putExtra(EXTRA_FOLDER, getCurrentFolder())
                    putParcelableArrayListExtra(EXTRA_FILES, targetFiles)
                }
                setResult(RESULT_OK, data)

                finish()
            }
        }
    }

    override fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace?) {
        updateNavigationElementsInActionBar()
        file = newCurrentFolder
    }

    override fun initDownloadForSending(file: OCFile) {
        // Nothing to do. Downloading files is not allowed.
    }

    override fun cancelFileTransference(files: ArrayList<OCFile>) {
        // Nothing to do. Transferring files is not allowed.
    }

    override fun setBottomBarVisibility(isVisible: Boolean) {
        // Nothing to do. No changes will be done in the bottom bar visibility.
    }

    override fun onFileClicked(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun onShareFileClicked(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun syncFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun openFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun sendDownloadedFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun showDetails(file: OCFile) {
        // Nothing to do. Details can't be opened here.
    }

    private fun initAndShowListOfFilesFragment() {
        val safeInitialFolder = if (file == null) {
            val fileDataStorageManager = FileDataStorageManager(this, account, contentResolver)
            fileDataStorageManager.getFileByPath(OCFile.ROOT_PATH)
        } else {
            file
        }

        safeInitialFolder?.let {
            val mainListOfFiles = MainFileListFragment.newInstance(it, true, FileListOption.ALL_FILES)
            mainListOfFiles.fileActions = this
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, mainListOfFiles, TAG_LIST_OF_FOLDERS)
            transaction.commit()
        }
    }

    /**
     * Set per-view controllers
     */
    private fun initPickerListeners() {
        cancelButton = findViewById(R.id.folder_picker_btn_cancel)
        cancelButton.setOnClickListener(this)
        chooseButton = findViewById(R.id.folder_picker_btn_choose)
        chooseButton.setOnClickListener(this)
    }

    private fun setActionButtonText() {
        val pickerMode = intent.getSerializableExtra(EXTRA_PICKER_OPTION) as PickerMode
        chooseButton.text = getString(pickerMode.getButtonString())
    }

    protected fun getListOfFilesFragment(): MainFileListFragment? {
        val listOfFiles = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FOLDERS)
        if (listOfFiles != null) {
            return listOfFiles as MainFileListFragment
        }
        Timber.e("Access to unexisting list of files fragment!!")
        return null
    }

    private fun getCurrentFolder(): OCFile? {
        val listOfFiles = getListOfFilesFragment()
        if (listOfFiles != null) {  // should never be null, indeed
            return listOfFiles.getCurrentFile()
        }
        return null
    }

    protected fun updateNavigationElementsInActionBar() {
        val currentDir = try {
            getCurrentFolder()
        } catch (e: NullPointerException) {
            file
        }

        val atRoot = (currentDir == null || currentDir.parentId == 0L)
        updateStandardToolbar(
            title = if (atRoot) getString(R.string.default_display_name_for_root_folder) else currentDir!!.fileName,
            displayHomeAsUpEnabled = !atRoot,
            homeButtonEnabled = !atRoot,
        )
    }

    enum class PickerMode {
        MOVE, COPY, CAMERA_FOLDER;

         fun getButtonString(): Int {
            return when (this) {
                MOVE -> R.string.folder_picker_move_here_button_text
                COPY -> R.string.folder_picker_copy_here_button_text
                CAMERA_FOLDER -> R.string.folder_picker_choose_button_text
            }
        }
    }

    companion object {
        const val EXTRA_FOLDER = "FOLDER_PICKER_EXTRA_FOLDER"
        const val EXTRA_FILES = "FOLDER_PICKER_EXTRA_FILES"
        const val EXTRA_PICKER_OPTION = "FOLDER_PICKER_EXTRA_PICKER_OPTION"
        private const val TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS"
    }
}
