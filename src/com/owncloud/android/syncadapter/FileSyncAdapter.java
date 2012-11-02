/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android.syncadapter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.jackrabbit.webdav.DavException;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
//<<<<<<< HEAD
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UpdateOCVersionOperation;
/*=======
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileObserverService;
import com.owncloud.android.utils.OwnCloudVersion;
>>>>>>> origin/master*/

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 * 
 * @author Bartek Przybylski
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

    private final static String TAG = "FileSyncAdapter";

    /** 
     * Maximum number of failed folder synchronizations that are supported before finishing the synchronization operation
     */
    private static final int MAX_FAILED_RESULTS = 3; 
    
    private long mCurrentSyncTime;
    private boolean mCancellation;
    private boolean mIsManualSync;
    private int mFailedResultsCounter;    
    private RemoteOperationResult mLastFailedResult;
    private SyncResult mSyncResult;
    
    public FileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
            String authority, ContentProviderClient provider,
            SyncResult syncResult) {

        mCancellation = false;
        mIsManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        mFailedResultsCounter = 0;
        mLastFailedResult = null;
        mSyncResult = syncResult;
        
        this.setAccount(account);
        this.setContentProvider(provider);
        this.setStorageManager(new FileDataStorageManager(account, getContentProvider()));
        try {
            this.initClientForCurrentAccount();
        } catch (UnknownHostException e) {
            /// the account is unknown for the Synchronization Manager, or unreachable for this context; don't try this again
            mSyncResult.tooManyRetries = true;
            notifyFailedSynchronization();
            return;
        }
        
        Log.d(TAG, "Synchronization of ownCloud account " + account.name + " starting");
        sendStickyBroadcast(true, null, null);  // message to signal the start of the synchronization to the UI
        
        try {
            updateOCVersion();
            mCurrentSyncTime = System.currentTimeMillis();
            if (!mCancellation) {
                fetchData(OCFile.PATH_SEPARATOR, DataStorageManager.ROOT_PARENT_ID);
                
            } else {
                Log.d(TAG, "Leaving synchronization before any remote request due to cancellation was requested");
            }
            
            
        } finally {
            // it's important making this although very unexpected errors occur; that's the reason for the finally
            
            if (mFailedResultsCounter > 0 && mIsManualSync) {
                /// don't let the system synchronization manager retries MANUAL synchronizations
                //      (be careful: "MANUAL" currently includes the synchronization requested when a new account is created and when the user changes the current account)
                mSyncResult.tooManyRetries = true;
                
                /// notify the user about the failure of MANUAL synchronization
                notifyFailedSynchronization();
            }
            sendStickyBroadcast(false, null, mLastFailedResult);        // message to signal the end to the UI
        }
        
    }
    
    
    
    /**
     * Called by system SyncManager when a synchronization is required to be cancelled.
     * 
     * Sets the mCancellation flag to 'true'. THe synchronization will be stopped when before a new folder is fetched. Data of the last folder
     * fetched will be still saved in the database. See onPerformSync implementation.
     */
    @Override
    public void onSyncCanceled() {
        Log.d(TAG, "Synchronization of " + getAccount().name + " has been requested to cancel");
        mCancellation = true;
        super.onSyncCanceled();
    }
    
    
    /**
     * Updates the locally stored version value of the ownCloud server
     */
    private void updateOCVersion() {
        UpdateOCVersionOperation update = new UpdateOCVersionOperation(getAccount(), getContext());
        RemoteOperationResult result = update.execute(getClient());
        if (!result.isSuccess()) {
            mLastFailedResult = result; 
        }
    }

    
    
    /**
     * Synchronize the properties of files and folders contained in a remote folder given by remotePath.
     * 
     * @param remotePath        Remote path to the folder to synchronize.
     * @param parentId          Database Id of the folder to synchronize.
     */
    private void fetchData(String remotePath, long parentId) {
        
        if (mFailedResultsCounter > MAX_FAILED_RESULTS || isFinisher(mLastFailedResult))
            return;
        
        // perform folder synchronization
        SynchronizeFolderOperation synchFolderOp = new SynchronizeFolderOperation(  remotePath, 
                                                                                    mCurrentSyncTime, 
                                                                                    parentId, 
                                                                                    getStorageManager(), 
                                                                                    getAccount(), 
                                                                                    getContext()
                                                                                  );
        RemoteOperationResult result = synchFolderOp.execute(getClient());
        
        
        // synchronized folder -> notice to UI - ALWAYS, although !result.isSuccess
        sendStickyBroadcast(true, remotePath, null);
        
        if (result.isSuccess()) {
            // synchronize children folders 
            List<OCFile> children = synchFolderOp.getChildren();
            fetchChildren(children);    // beware of the 'hidden' recursion here!
            
//<<<<<<< HEAD
        } else {
            if (result.getCode() == RemoteOperationResult.ResultCode.UNAUTHORIZED) {
                mSyncResult.stats.numAuthExceptions++;
                
            } else if (result.getException() instanceof DavException) {
                mSyncResult.stats.numParseExceptions++;
                
            } else if (result.getException() instanceof IOException) { 
                mSyncResult.stats.numIoExceptions++;
/*=======
                // insertion or update of files
                List<OCFile> updatedFiles = new Vector<OCFile>(resp.getResponses().length - 1);
                for (int i = 1; i < resp.getResponses().length; ++i) {
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[i], getUri().getPath());
                    OCFile file = fillOCFile(we);
                    file.setParentId(parentId);
                    if (getStorageManager().getFileByPath(file.getRemotePath()) != null &&
                            getStorageManager().getFileByPath(file.getRemotePath()).keepInSync() &&
                            file.getModificationTimestamp() > getStorageManager().getFileByPath(file.getRemotePath())
                                                                         .getModificationTimestamp()) {
                        // first disable observer so we won't get file upload right after download
                        Log.d(TAG, "Disabling observation of remote file" + file.getRemotePath());
                        Intent intent = new Intent(getContext(), FileObserverService.class);
                        intent.putExtra(FileObserverService.KEY_FILE_CMD, FileObserverService.CMD_ADD_DOWNLOADING_FILE);
                        intent.putExtra(FileObserverService.KEY_CMD_ARG, file.getRemotePath());
                        getContext().startService(intent);
                        intent = new Intent(this.getContext(), FileDownloader.class);
                        intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
                        intent.putExtra(FileDownloader.EXTRA_FILE, file);
                        file.setKeepInSync(true);
                        getContext().startService(intent);
                    }
                    if (getStorageManager().getFileByPath(file.getRemotePath()) != null)
                        file.setKeepInSync(getStorageManager().getFileByPath(file.getRemotePath()).keepInSync());
>>>>>>> origin/master*/
                
            }
            mFailedResultsCounter++;
            mLastFailedResult = result;
        }
            
    }

    /**
     * Checks if a failed result should terminate the synchronization process immediately, according to
     * OUR OWN POLICY
     * 
     * @param   failedResult        Remote operation result to check.
     * @return                      'True' if the result should immediately finish the synchronization
     */
    private boolean isFinisher(RemoteOperationResult failedResult) {
        if  (failedResult != null) {
            RemoteOperationResult.ResultCode code = failedResult.getCode();
            return (code.equals(RemoteOperationResult.ResultCode.SSL_ERROR) ||
                    code.equals(RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) ||
                    code.equals(RemoteOperationResult.ResultCode.BAD_OC_VERSION) ||
                    code.equals(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED));
        }
        return false;
    }

    /**
     * Synchronize data of folders in the list of received files
     * 
     * @param files         Files to recursively fetch 
     */
    private void fetchChildren(List<OCFile> files) {
        int i;
        for (i=0; i < files.size() && !mCancellation; i++) {
            OCFile newFile = files.get(i);
            if (newFile.isDirectory()) {
                fetchData(newFile.getRemotePath(), newFile.getFileId());
            }
        }
        if (mCancellation && i <files.size()) Log.d(TAG, "Leaving synchronization before synchronizing " + files.get(i).getRemotePath() + " because cancelation request");
    }

    
    /**
     * Sends a message to any application component interested in the progress of the synchronization.
     * 
     * @param inProgress        'True' when the synchronization progress is not finished.
     * @param dirRemotePath     Remote path of a folder that was just synchronized (with or without success)
     */
    private void sendStickyBroadcast(boolean inProgress, String dirRemotePath, RemoteOperationResult result) {
        Intent i = new Intent(FileSyncService.SYNC_MESSAGE);
        i.putExtra(FileSyncService.IN_PROGRESS, inProgress);
        i.putExtra(FileSyncService.ACCOUNT_NAME, getAccount().name);
        if (dirRemotePath != null) {
            i.putExtra(FileSyncService.SYNC_FOLDER_REMOTE_PATH, dirRemotePath);
        }
        if (result != null) {
            i.putExtra(FileSyncService.SYNC_RESULT, result);
        }
        getContext().sendStickyBroadcast(i);
    }

    
    
    /**
     * Notifies the user about a failed synchronization through the status notification bar 
     */
    private void notifyFailedSynchronization() {
        Notification notification = new Notification(R.drawable.icon, getContext().getString(R.string.sync_fail_ticker), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // TODO put something smart in the contentIntent below
        notification.contentIntent = PendingIntent.getActivity(getContext().getApplicationContext(), (int)System.currentTimeMillis(), new Intent(), 0);
        notification.setLatestEventInfo(getContext().getApplicationContext(), 
                                        getContext().getString(R.string.sync_fail_ticker), 
                                        String.format(getContext().getString(R.string.sync_fail_content), getAccount().name), 
                                        notification.contentIntent);
        ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(R.string.sync_fail_ticker, notification);
    }

    

}
