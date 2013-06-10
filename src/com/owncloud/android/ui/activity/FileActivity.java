/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.Log_OC;
import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.datamodel.OCFile;

/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud {@link Account}s .
 * 
 * @author David A. Velasco
 */
public abstract class FileActivity extends SherlockFragmentActivity {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    
    public static final String TAG = FileActivity.class.getSimpleName(); 
    
    
    /** OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located. */
    private Account mAccount;
    
    /** Main {@link OCFile} handled by the activity.*/
    private OCFile mFile;
    
    /** Flag to signal that the activity will is finishing to enforce the creation of an ownCloud {@link Account} */
    private boolean mRedirectingToSetupAccount = false;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /// Load of saved instance state: keep this always before initDataFromCurrentAccount()
        if(savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        } else {
            mAccount = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
        }
        
        if (mAccount != null && AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), mAccount.name)) {
            onAccountChanged();
        }
    }

    
    /**
     * Validate the ownCloud {@link Account} associated to the Activity any time it is 
     * started, and if not valid tries to move to a different Account.
     */
    @Override
    protected void onStart() {
        Log_OC.e(TAG, "onStart en FileActivity");
        super.onStart();
        /// Validate account, and try to fix if wrong
        if (mAccount == null || !AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), mAccount.name)) {
            if (!AccountUtils.accountsAreSetup(getApplicationContext())) {
                /// no account available: force account creation
                mAccount = null;
                createFirstAccount();
                mRedirectingToSetupAccount = true;
                
            } else {
                /// get 'last current account' as default account
                mAccount = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
                onAccountChanged();
            }
        }
    }
    
        
    /**
     * Launches the account creation activity. To use when no ownCloud account is available
     */
    private void createFirstAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(AccountAuthenticator.ACCOUNT_TYPE, 
                        AccountAuthenticator.AUTH_TOKEN_TYPE_PASSWORD,
                        null, 
                        null, 
                        this, 
                        new AccountCreationCallback(),                        
                        null);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, mFile);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
    }
    
    
    /**
     * Getter for the main {@link OCFile} handled by the activity.
     * 
     * @return  Main {@link OCFile} handled by the activity.
     */
    public OCFile getFile() {
        return mFile;
    }

    
    /**
     * Setter for the main {@link OCFile} handled by the activity.
     * 
     * @param file  Main {@link OCFile} to be handled by the activity.
     */
    public void setFile(OCFile file) {
        mFile = file;
    }

    
    /**
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     * 
     * @return  OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     */
    public Account getAccount() {
        return mAccount;
    }

    
    /**
     * @return  'True' when the Activity is finishing to enforce the setup of a new account.
     */
    protected boolean isRedirectingToSetupAccount() {
        return mRedirectingToSetupAccount;
    }
    
    
    /**
     * Helper class handling a callback from the {@link AccountManager} after the creation of
     * a new ownCloud {@link Account} finished, successfully or not.
     * 
     * At this moment, only called after the creation of the first account.
     * 
     * @author David A. Velasco
     */
    public class AccountCreationCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            FileActivity.this.mRedirectingToSetupAccount = false;
            if (future != null) {
                try {
                    Bundle result;
                    result = future.getResult();
                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    if (AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name)) {
                        FileActivity.this.mAccount = new Account(name, type);
                        FileActivity.this.onAccountChanged();
                    }
                } catch (OperationCanceledException e) {
                    Log_OC.e(TAG, "Account creation canceled");
                    
                } catch (Exception e) {
                    Log_OC.e(TAG, "Account creation finished in exception: ", e);
                }
                    
            } else {
                Log_OC.e(TAG, "Account creation callback with null bundle");
            }
            if (mAccount == null) {
                finish();
            }
        }
        
    }


    /**
     *  Called when the ownCloud {@link Account} associated to the Activity was just updated.
     * 
     *  Child classes must grant that state depending on the {@link Account} is updated.
     */
    protected abstract void onAccountChanged();
}
