/**
 *   ownCloud Android client application
 *
 *   @author purigarcia
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.test.ui.testSuites;

import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.NewFolderPopUp;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFileTestSuite{

	AndroidDriver driver;
	Common common;
	private Boolean fileHasBeenCreated = false;
	//private final String OLD_FILE_NAME = Config.fileToTest;
	private final String FILE_NAME = "newNameFile";
	private String CurrentCreatedFile = "";
	
	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testRenameFile () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(Config.fileToTest, fileListView);

		//check if the file with the new name already exists, if true delete it
		Actions.deleteElement(FILE_NAME, fileListView, driver);

		assertTrue(fileHasBeenCreated = fileListViewAfterUploadFile
				.getFileElement(Config.fileToTest).isDisplayed());
		CurrentCreatedFile = Config.fileToTest;
		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListViewAfterUploadFile.getProgressCircular(), 1000);
		
		common.wait.until(ExpectedConditions.visibilityOf(
				fileListViewAfterUploadFile
				.getFileElementLayout(Config.fileToTest)
				.findElement(By.id(FileListView.getLocalFileIndicator()))));
		
		ElementMenuOptions menuOptions = fileListViewAfterUploadFile
				.longPressOnElement(Config.fileToTest);
		
		NewFolderPopUp newFolderPopUp = menuOptions.clickOnRename();
		
		newFolderPopUp.typeNewFolderName(FILE_NAME);
		
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp
				.clickOnNewFolderOkButton();
		
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);
		
		AndroidElement file = fileListViewAfterUploadFile
				.getFileElement(FILE_NAME);
		
		assertNotNull(file);
		assertTrue(file.isDisplayed());	
		assertEquals(FILE_NAME , file.getText());
		CurrentCreatedFile = FILE_NAME;
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if (fileHasBeenCreated) {
			FileListView fileListView = new FileListView(driver);
			Actions.deleteElement(CurrentCreatedFile,fileListView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
