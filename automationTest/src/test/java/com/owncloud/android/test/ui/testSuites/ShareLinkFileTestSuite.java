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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.FilesView;;

public class ShareLinkFileTestSuite{

	AndroidDriver driver;
	Common common;

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testShareLinkFileByGmail () throws Exception {	
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		//if the file already exists, delete in case it is already sharedByLink
		AndroidElement file = filesView.getElement(Config.fileToTest);
		if(file!=null){
			Actions.deleteElement(Config.fileToTest,filesView, driver);
			assertNull(filesView.getElement(Config.fileToTest));
		}
		//now we are sure that we are going to delete it remote and locally
		filesView = Actions.uploadFile(Config.fileToTest, filesView);

		file = filesView.getElement(Config.fileToTest);
		assertTrue(file.isDisplayed());

		Actions.shareLinkElementByGmail(
				Config.fileToTest,filesView,driver,common);

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getSharedElementIndicator()))));

		assertTrue(filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getSharedElementIndicator()))
				.isDisplayed());
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testShareLinkFileByCopyLink () throws Exception {	
		AndroidElement sharedElementIndicator;
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		//if the file already exists, delete in case it is already sharedByLink
		AndroidElement file = filesView.getElement(Config.fileToTest);
		if(file!=null){
			Actions.deleteElement(Config.fileToTest,filesView, driver);
			assertNull(filesView.getElement(Config.fileToTest));
		}
		filesView = Actions.uploadFile(Config.fileToTest, filesView);

		assertTrue(filesView.getElement(Config.fileToTest).isDisplayed());

		sharedElementIndicator = Actions.shareLinkElementByCopyLink(
				Config.fileToTest,filesView,driver,common);
		
		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getSharedElementIndicator()))));

		assertTrue(sharedElementIndicator.isDisplayed());
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUnshareLinkFile () throws Exception {	
		AndroidElement sharedElementIndicator;
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		//if the file already exists, do not upload 
		//(we do not care if it is already share)
		AndroidElement file = filesView.getElement(Config.fileToTest);
		if(file==null){
			filesView = Actions.uploadFile(Config.fileToTest, filesView);
			file = filesView.getElement(Config.fileToTest);
		}

		assertTrue(file.isDisplayed());

		sharedElementIndicator = Actions.shareLinkElementByCopyLink(
				Config.fileToTest,filesView,driver,common);
		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getSharedElementIndicator()))));

		assertTrue(sharedElementIndicator.isDisplayed());
		
		Actions.unshareLinkElement(Config.fileToTest,filesView,driver,common);
		
		assertFalse(sharedElementIndicator.isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());

		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.fileToTest,filesView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
}
