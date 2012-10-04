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
package com.owncloud.android.ui.fragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.DisplayUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.OwnCloudVersion;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailFragment extends SherlockFragment implements
        OnClickListener, ConfirmationDialogFragment.ConfirmationDialogFragmentListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private FileDetailFragment.ContainerActivity mContainerActivity;
    
    private int mLayout;
    private View mView;
    private OCFile mFile;
    private Account mAccount;
    private ImageView mPreview;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;

    private static final String TAG = "FileDetailFragment";
    public static final String FTAG = "FileDetails"; 
    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";

    
    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        mFile = null;
        mAccount = null;
        mLayout = R.layout.file_details_empty;
    }
    
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FileDetailFragment(OCFile fileToDetail, Account ocAccount){
        mFile = fileToDetail;
        mAccount = ocAccount;
        mLayout = R.layout.file_details_empty;
        
        if(fileToDetail != null && ocAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FileListFragment.ContainerActivity");
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_ACCOUNT);
        }
        
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        mView = view;
        
        if (mLayout == R.layout.file_details_fragment) {
            mView.findViewById(R.id.fdKeepInSync).setOnClickListener(this);
            mView.findViewById(R.id.fdRenameBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdDownloadBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdOpenBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdRemoveBtn).setOnClickListener(this);
            //mView.findViewById(R.id.fdShareBtn).setOnClickListener(this);
            mPreview = (ImageView)mView.findViewById(R.id.fdPreview);
        }
        
        updateFileDetails();
        return view;
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        Log.i(getClass().toString(), "onSaveInstanceState() end");
    }

    
    @Override
    public void onResume() {
        super.onResume();
        
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(
                FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mDownloadFinishReceiver, filter);
        
        mUploadFinishReceiver = new UploadFinishReceiver();
        filter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mUploadFinishReceiver, filter);
        
        mPreview = (ImageView)mView.findViewById(R.id.fdPreview);
    }

    @Override
    public void onPause() {
        super.onPause();
        
        getActivity().unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
        
        getActivity().unregisterReceiver(mUploadFinishReceiver);
        mUploadFinishReceiver = null;
        
        if (mPreview != null) {
            mPreview = null;
        }
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdDownloadBtn: {
                Intent i = new Intent(getActivity(), FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
                i.putExtra(FileDownloader.EXTRA_REMOTE_PATH, mFile.getRemotePath());
                i.putExtra(FileDownloader.EXTRA_FILE_PATH, mFile.getRemotePath());
                i.putExtra(FileDownloader.EXTRA_FILE_SIZE, mFile.getFileLength());
                
                // update ui 
                setButtonsForTransferring();
                
                getActivity().startService(i);
                mContainerActivity.onFileStateChanged();    // this is not working; it is performed before the fileDownloadService registers it as 'in progress'
                break;
            }
            case R.id.fdKeepInSync: {
                CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
                mFile.setKeepInSync(cb.isChecked());
                FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
                fdsm.saveFile(mFile);
                if (mFile.keepInSync()) {
                    onClick(getView().findViewById(R.id.fdDownloadBtn));
                } else {
                    mContainerActivity.onFileStateChanged();    // put inside 'else' to not call it twice (here, and in the virtual click on fdDownloadBtn)
                }
                /*
                Intent intent = new Intent(getActivity().getApplicationContext(),
                                           FileObserverService.class);
                intent.putExtra(FileObserverService.KEY_FILE_CMD,
                           (cb.isChecked()?
                                   FileObserverService.CMD_ADD_OBSERVED_FILE:
                                   FileObserverService.CMD_DEL_OBSERVED_FILE));
                intent.putExtra(FileObserverService.KEY_CMD_ARG, mFile.getStoragePath());
                getActivity().startService(intent);
                */
                break;
            }
            case R.id.fdRenameBtn: {
                EditNameFragment dialog = EditNameFragment.newInstance(mFile.getFileName());
                dialog.show(getFragmentManager(), "nameeditdialog");
                dialog.setOnDismissListener(this);
                break;
            }   
            case R.id.fdRemoveBtn: {
                ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                        R.string.confirmation_remove_alert,
                        new String[]{mFile.getFileName()},
                        mFile.isDown() ? R.string.confirmation_remove_remote_and_local : R.string.confirmation_remove_remote,
                        mFile.isDown() ? R.string.confirmation_remove_local : -1,
                        R.string.common_cancel);
                confDialog.setOnConfirmationListener(this);
                confDialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                break;
            }
            case R.id.fdOpenBtn: {
                String storagePath = mFile.getStoragePath();
                String encodedStoragePath = WebdavUtils.encodePath(storagePath);
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mFile.getMimetype());
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(i);
                    
                } catch (Throwable t) {
                    Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mFile.getMimetype());
                    boolean toastIt = true; 
                    String mimeType = "";
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                        if (mimeType != null && !mimeType.equals(mFile.getMimetype())) {
                            i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mimeType);
                            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(i);
                            toastIt = false;
                        }
                        
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                        
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                        
                    } catch (Throwable th) {
                        Log.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                        
                    } finally {
                        if (toastIt) {
                            Toast.makeText(getActivity(), "There is no application to handle file " + mFile.getFileName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                }
                break;
            }
            default:
                Log.e(TAG, "Incorrect view clicked!");
        }
        
        /* else if (v.getId() == R.id.fdShareBtn) {
            Thread t = new Thread(new ShareRunnable(mFile.getRemotePath()));
            t.start();
        }*/
    }
    
    
    @Override
    public void onConfirmation(String callerTag) {
        if (callerTag.equals(FTAG_CONFIRMATION)) {
            FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
            if (fdsm.getFileById(mFile.getFileId()) != null) {
                new Thread(new RemoveRunnable(mFile, mAccount, new Handler())).start();
                boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
            }
        }
    }
    
    @Override
    public void onNeutral(String callerTag) {
        FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
        File f = null;
        if (mFile.isDown() && (f = new File(mFile.getStoragePath())).exists()) {
            f.delete();
            mFile.setStoragePath(null);
            fdsm.saveFile(mFile);
            updateFileDetails(mFile, mAccount);
        }
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log.d(TAG, "REMOVAL CANCELED");
    }
    
    
    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return mLayout == R.layout.file_details_empty;
    }

    
    /**
     * Can be used to get the file that is currently being displayed.
     * @return The file on the screen.
     */
    public OCFile getDisplayedFile(){
        return mFile;
    }
    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        mFile = file;
        mAccount = ocAccount;
        updateFileDetails();
    }
    

    /**
     * Updates the view with all relevant details about that file.
     */
    public void updateFileDetails() {

        if (mFile != null && mAccount != null && mLayout == R.layout.file_details_fragment) {
            
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mFile
                    .getMimetype()));
            setFilesize(mFile.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(mFile.getCreationTimestamp());
            }
           
            setTimeModified(mFile.getModificationTimestamp());
            
            CheckBox cb = (CheckBox)getView().findViewById(R.id.fdKeepInSync);
            cb.setChecked(mFile.keepInSync());

            // configure UI for depending upon local state of the file
            if (FileDownloader.isDownloading(mAccount, mFile.getRemotePath()) || FileUploader.isUploading(mAccount, mFile.getRemotePath())) {
                setButtonsForTransferring();
                
            } else if (mFile.isDown()) {
                // Update preview
                if (mFile.getMimetype().startsWith("image/")) {
                    BitmapLoader bl = new BitmapLoader();
                    bl.execute(new String[]{mFile.getStoragePath()});
                }
                
                setButtonsForDown();
                
            } else {
                setButtonsForRemote();
            }
        }
    }
    
    
    /**
     * Updates the filename in view
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    /**
     * Updates the MIME type in view
     * @param mimetype to set
     */
    private void setFiletype(String mimetype) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null)
            tv.setText(mimetype);
    }

    /**
     * Updates the file size in view
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize));
    }
    
    /**
     * Updates the time that the file was created in view
     * @param milliseconds Unix time to set
     */
    private void setTimeCreated(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdCreated);
        TextView tvLabel = (TextView) getView().findViewById(R.id.fdCreatedLabel);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
            tv.setVisibility(View.VISIBLE);
            tvLabel.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the time that the file was last modified
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }
    
    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            //downloadButton.setText(R.string.filedetails_download_in_progress);    // ugly
            downloadButton.setEnabled(false);   // TODO replace it with a 'cancel download' button
        
            // let's protect the user from himself ;)
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(false);
        }
    }
    
    /**
     * Enables or disables buttons for a file locally available 
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            //downloadButton.setText(R.string.filedetails_redownload);      // ugly
            downloadButton.setEnabled(true);
        
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(true);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available 
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            //downloadButton.setText(R.string.filedetails_download);    // unnecessary
            downloadButton.setEnabled(true);
            
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(true);
        }
    }
    

    /**
     * In ownCloud 3.X.X and 4.X.X there is a bug that SabreDAV does not return
     * the time that the file was created. There is a chance that this will
     * be fixed in future versions. Use this method to check if this version of
     * ownCloud has this fix.
     * @return True, if ownCloud the ownCloud version is supporting creation time
     */
    private boolean ocVersionSupportsTimeCreated(){
        /*if(mAccount != null){
            AccountManager accManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
            OwnCloudVersion ocVersion = new OwnCloudVersion(accManager
                    .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            if(ocVersion.compareTo(new OwnCloudVersion(0x030000)) < 0) {
                return true;
            }
        }*/
        return false;
    }
    
    
    /**
     * Interface to implement by any Activity that includes some instance of FileDetailFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when the detail fragment wants to notice its container 
         * activity about a relevant state the file shown by the fragment.
         * 
         * Added to notify to FileDisplayActivity about the need of refresh the files list. 
         * 
         * Currently called when:
         *  - a download is started;
         *  - a rename is completed;
         *  - a deletion is completed;
         *  - the 'inSync' flag is changed;
         */
        public void onFileStateChanged();
        
    }
    

    /**
     * Once the file download has finished -> update view
     * @author Bartek Przybylski
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);

            if (!isEmpty() && accountName.equals(mAccount.name)) {
                boolean downloadWasFine = intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false);
                String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
                if (mFile.getRemotePath().equals(downloadedRemotePath)) {
                    if (downloadWasFine) {
                        mFile.setStoragePath(intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH));    // updates the local object without accessing the database again
                    }
                    updateFileDetails();    // it updates the buttons; must be called although !downloadWasFine
                }
            }
        }
    }
    
    
    /**
     * Once the file upload has finished -> update view
     * 
     * Being notified about the finish of an upload is necessary for the next sequence:
     *   1. Upload a big file.
     *   2. Force a synchronization; if it finished before the upload, the file in transfer will be included in the local database and in the file list
     *      of its containing folder; the the server includes it in the PROPFIND requests although it's not fully upload. 
     *   3. Click the file in the list to see its details.
     *   4. Wait for the upload finishes; at this moment, the details view must be refreshed to enable the action buttons.
     */
    private class UploadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);

            if (!isEmpty() && accountName.equals(mAccount.name)) {
                boolean uploadWasFine = intent.getBooleanExtra(FileUploader.EXTRA_UPLOAD_RESULT, false);
                String uploadRemotePath = intent.getStringExtra(FileUploader.EXTRA_REMOTE_PATH);
                if (mFile.getRemotePath().equals(uploadRemotePath)) {
                    if (uploadWasFine) {
                        FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
                        mFile = fdsm.getFileByPath(mFile.getRemotePath());
                    }
                    updateFileDetails();    // it updates the buttons; must be called although !uploadWasFine; interrupted uploads still leave an incomplete file in the server
                }
            }
        }
    }
    

    // this is a temporary class for sharing purposes, it need to be replaced in transfer service
    @SuppressWarnings("unused")
    private class ShareRunnable implements Runnable {
        private String mPath;

        public ShareRunnable(String path) {
            mPath = path;
        }
        
        public void run() {
            AccountManager am = AccountManager.get(getActivity());
            Account account = AccountUtils.getCurrentOwnCloudAccount(getActivity());
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            String url = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + AccountUtils.getWebdavPath(ocv);

            Log.d("share", "sharing for version " + ocv.toString());

            if (ocv.compareTo(new OwnCloudVersion(0x040000)) >= 0) {
                String APPS_PATH = "/apps/files_sharing/";
                String SHARE_PATH = "ajax/share.php";

                String SHARED_PATH = "/apps/files_sharing/get.php?token=";
                
                final String WEBDAV_SCRIPT = "webdav.php";
                final String WEBDAV_FILES_LOCATION = "/files/";
                
                WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(account, getActivity().getApplicationContext());
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setMaxConnectionsPerHost(wc.getHostConfiguration(), 5);

                //wc.getParams().setParameter("http.protocol.single-cookie-header", true);
                //wc.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

                PostMethod post = new PostMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + APPS_PATH + SHARE_PATH);

                post.addRequestHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8" );
                post.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                Log.d("share", mPath+"");
                formparams.add(new BasicNameValuePair("sources",mPath));
                formparams.add(new BasicNameValuePair("uid_shared_with", "public"));
                formparams.add(new BasicNameValuePair("permissions", "0"));
                post.setRequestEntity(new StringRequestEntity(URLEncodedUtils.format(formparams, HTTP.UTF_8)));

                int status;
                try {
                    PropFindMethod find = new PropFindMethod(url+"/");
                    find.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    Log.d("sharer", ""+ url+"/");
                    
                    for (org.apache.commons.httpclient.Header a : find.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    
                    int status2 = wc.executeMethod(find);

                    Log.d("sharer", "propstatus "+status2);
                    
                    GetMethod get = new GetMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + "/");
                    get.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    
                    status2 = wc.executeMethod(get);

                    Log.d("sharer", "getstatus "+status2);
                    Log.d("sharer", "" + get.getResponseBodyAsString());
                    
                    for (org.apache.commons.httpclient.Header a : get.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }

                    status = wc.executeMethod(post);
                    for (org.apache.commons.httpclient.Header a : post.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    for (org.apache.commons.httpclient.Header a : post.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }
                    String resp = post.getResponseBodyAsString();
                    Log.d("share", ""+post.getURI().toString());
                    Log.d("share", "returned status " + status);
                    Log.d("share", " " +resp);
                    
                    if(status != HttpStatus.SC_OK ||resp == null || resp.equals("") || resp.startsWith("false")) {
                        return;
                     }

                    JSONObject jsonObject = new JSONObject (resp);
                    String jsonStatus = jsonObject.getString("status");
                    if(!jsonStatus.equals("success")) throw new Exception("Error while sharing file status != success");
                    
                    String token = jsonObject.getString("data");
                    String uri = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + SHARED_PATH + token; 
                    Log.d("Actions:shareFile ok", "url: " + uri);   
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            } else if (ocv.compareTo(new OwnCloudVersion(0x030000)) >= 0) {
                
            }
        }
    }
    
    public void onDismiss(EditNameFragment dialog) {
        if (dialog instanceof EditNameFragment) {
            if (((EditNameFragment)dialog).getResult()) {
                String newFilename = ((EditNameFragment)dialog).getNewFilename();
                Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
                if (!newFilename.equals(mFile.getFileName())) {
                    FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
                    if (fdsm.getFileById(mFile.getFileId()) != null) {
                        OCFile newFile = new OCFile(fdsm.getFileById(mFile.getParentId()).getRemotePath() + newFilename);
                        newFile.setCreationTimestamp(mFile.getCreationTimestamp());
                        newFile.setFileId(mFile.getFileId());
                        newFile.setFileLength(mFile.getFileLength());
                        newFile.setKeepInSync(mFile.keepInSync());
                        newFile.setLastSyncDate(mFile.getLastSyncDate());
                        newFile.setMimetype(mFile.getMimetype());
                        newFile.setModificationTimestamp(mFile.getModificationTimestamp());
                        newFile.setParentId(mFile.getParentId());
                        boolean localRenameFails = false;
                        if (mFile.isDown()) {
                            File f = new File(mFile.getStoragePath());
                            Log.e(TAG, f.getAbsolutePath());
                            localRenameFails = !(f.renameTo(new File(f.getParent() + File.separator + newFilename)));
                            Log.e(TAG, f.getParent() + File.separator + newFilename);
                            newFile.setStoragePath(f.getParent() + File.separator + newFilename);
                        }
                        
                        if (localRenameFails) {
                            Toast msg = Toast.makeText(getActivity(), R.string.rename_local_fail_msg, Toast.LENGTH_LONG); 
                            msg.show();
                            
                        } else {
                            new Thread(new RenameRunnable(mFile, newFile, mAccount, new Handler())).start();
                            boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                            getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                        }

                    }
                }
            }
        } else {
            Log.e(TAG, "Unknown dialog instance passed to onDismissDalog: " + dialog.getClass().getCanonicalName());
        }
        
    }
    
    private class RenameRunnable implements Runnable {
        
        Account mAccount;
        OCFile mOld, mNew;
        Handler mHandler;
        
        public RenameRunnable(OCFile oldFile, OCFile newFile, Account account, Handler handler) {
            mOld = oldFile;
            mNew = newFile;
            mAccount = account;
            mHandler = handler;
        }
        
        public void run() {
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getSherlockActivity().getApplicationContext());
            AccountManager am = AccountManager.get(getSherlockActivity());
            String baseUrl = am.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            String webdav_path = AccountUtils.getWebdavPath(ocv);
            Log.d("ASD", ""+baseUrl + webdav_path + WebdavUtils.encodePath(mOld.getRemotePath()));

            Log.e("ASD", Uri.parse(baseUrl).getPath() == null ? "" : Uri.parse(baseUrl).getPath() + webdav_path + WebdavUtils.encodePath(mNew.getRemotePath()));
            LocalMoveMethod move = new LocalMoveMethod(baseUrl + webdav_path + WebdavUtils.encodePath(mOld.getRemotePath()),
                                             Uri.parse(baseUrl).getPath() == null ? "" : Uri.parse(baseUrl).getPath() + webdav_path + WebdavUtils.encodePath(mNew.getRemotePath()));
            
            boolean success = false;
            try {
                int status = wc.executeMethod(move);
                success = move.succeeded();
                move.getResponseBodyAsString(); // exhaust response, although not interesting
                Log.d(TAG, "Move returned status: " + status);
                
            } catch (HttpException e) {
                Log.e(TAG, "HTTP Exception renaming file " + mOld.getRemotePath() + " to " + mNew.getRemotePath(), e);
                
            } catch (IOException e) {
                Log.e(TAG, "I/O Exception renaming file " + mOld.getRemotePath() + " to " + mNew.getRemotePath(), e);
                
            } catch (Exception e) {
                Log.e(TAG, "Unexpected exception renaming file " + mOld.getRemotePath() + " to " + mNew.getRemotePath(), e);
                
            } finally {
               move.releaseConnection();
            } 
            
            if (success) {
                FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
                fdsm.removeFile(mOld, false);
                fdsm.saveFile(mNew);
                mFile = mNew;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { 
                        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                        updateFileDetails(mFile, mAccount);
                        mContainerActivity.onFileStateChanged();
                    }
                });
                
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // undo the local rename
                        if (mNew.isDown()) {
                            File f = new File(mNew.getStoragePath());
                            if (!f.renameTo(new File(mOld.getStoragePath()))) {
                                // the local rename undoing failed; last chance: save the new local storage path in the old file
                                mFile.setStoragePath(mNew.getStoragePath());
                                FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
                                fdsm.saveFile(mFile);
                            }
                        }
                        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                        try {
                            Toast msg = Toast.makeText(getActivity(), R.string.rename_server_fail_msg, Toast.LENGTH_LONG); 
                            msg.show();
                            
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        private class LocalMoveMethod extends DavMethodBase {

            public LocalMoveMethod(String uri, String dest) {
                super(uri);
                addRequestHeader(new org.apache.commons.httpclient.Header("Destination", dest));
            }

            @Override
            public String getName() {
                return "MOVE";
            }

            @Override
            protected boolean isSuccess(int status) {
                return status == 201 || status == 204;
            }
            
        }
    }
    
    private static class EditNameFragment extends SherlockDialogFragment implements OnClickListener {

        private String mNewFilename;
        private boolean mResult;
        private FileDetailFragment mListener;
        
        static public EditNameFragment newInstance(String filename) {
            EditNameFragment f = new EditNameFragment();
            Bundle args = new Bundle();
            args.putString("filename", filename);
            f.setArguments(args);
            return f;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.edit_box_dialog, container, false);

            String currentName = getArguments().getString("filename");
            if (currentName == null)
                currentName = "";
            
            ((Button)v.findViewById(R.id.cancel)).setOnClickListener(this);
            ((Button)v.findViewById(R.id.ok)).setOnClickListener(this);
            ((TextView)v.findViewById(R.id.user_input)).setText(currentName);
            ((TextView)v.findViewById(R.id.user_input)).requestFocus();
            getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            mResult = false;
            return v;
        }
        
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ok: {
                    mNewFilename = ((TextView)getView().findViewById(R.id.user_input)).getText().toString();
                    mResult = true;
                }
                case R.id.cancel: { // fallthought
                    dismiss();
                    mListener.onDismiss(this);
                }
            }
        }
        
        void setOnDismissListener(FileDetailFragment listener) {
            mListener = listener;
        }
        
        public String getNewFilename() {
            return mNewFilename;
        }
        
        // true if user click ok
        public boolean getResult() {
            return mResult;
        }
        
    }
    
    private class RemoveRunnable implements Runnable {
        
        Account mAccount;
        OCFile mFileToRemove;
        Handler mHandler;
        
        public RemoveRunnable(OCFile fileToRemove, Account account, Handler handler) {
            mFileToRemove = fileToRemove;
            mAccount = account;
            mHandler = handler;
        }
        
        public void run() {
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getSherlockActivity().getApplicationContext());
            AccountManager am = AccountManager.get(getSherlockActivity());
            String baseUrl = am.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            String webdav_path = AccountUtils.getWebdavPath(ocv);
            Log.d("ASD", ""+baseUrl + webdav_path + WebdavUtils.encodePath(mFileToRemove.getRemotePath()));

            DeleteMethod delete = new DeleteMethod(baseUrl + webdav_path + WebdavUtils.encodePath(mFileToRemove.getRemotePath()));
            
            boolean success = false;
            int status = -1;
            try {
                status = wc.executeMethod(delete);
                success = (delete.succeeded());
                delete.getResponseBodyAsString();   // exhaust the response, although not interesting
                Log.d(TAG, "Delete: returned status " + status);
                
            } catch (HttpException e) {
                Log.e(TAG, "HTTP Exception removing file " + mFileToRemove.getRemotePath(), e);
                
            } catch (IOException e) {
                Log.e(TAG, "I/O Exception removing file " + mFileToRemove.getRemotePath(), e);
                
            } catch (Exception e) {
                Log.e(TAG, "Unexpected exception removing file " + mFileToRemove.getRemotePath(), e);
                
            } finally {
                delete.releaseConnection();
            }
            
            if (success) {
                FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
                fdsm.removeFile(mFileToRemove, true);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                        try {
                            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
                            msg.show();
                            if (inDisplayActivity) {
                                // double pane
                                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null)); // empty FileDetailFragment
                                transaction.commit();
                                mContainerActivity.onFileStateChanged();
                                
                            } else {
                                getActivity().finish();
                            }
                            
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                        try {
                            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
                            msg.show();
                            
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        
    }
    
    class BitmapLoader extends AsyncTask<String, Void, Bitmap> {
        @SuppressLint({ "NewApi", "NewApi", "NewApi" }) // to avoid Lint errors since Android SDK r20
		@Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            if (params.length != 1) return result;
            String storagePath = params[0];
            try {

                BitmapFactory.Options options = new Options();
                options.inScaled = true;
                options.inPurgeable = true;
                options.inJustDecodeBounds = true;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                    options.inPreferQualityOverSpeed = false;
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    options.inMutable = false;
                }

                result = BitmapFactory.decodeFile(storagePath, options);
                options.inJustDecodeBounds = false;

                int width = options.outWidth;
                int height = options.outHeight;
                int scale = 1;
                if (width >= 2048 || height >= 2048) {
                    scale = (int) Math.ceil((Math.ceil(Math.max(height, width) / 2048.)));
                    options.inSampleSize = scale;
                }
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                int screenwidth;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                    display.getSize(size);
                    screenwidth = size.x;
                } else {
                    screenwidth = display.getWidth();
                }

                Log.e("ASD", "W " + width + " SW " + screenwidth);

                if (width > screenwidth) {
                    scale = (int) Math.ceil((float)width / screenwidth);
                    options.inSampleSize = scale;
                }

                result = BitmapFactory.decodeFile(storagePath, options);

                Log.e("ASD", "W " + options.outWidth + " SW " + options.outHeight);

            } catch (OutOfMemoryError e) {
                result = null;
                Log.e(TAG, "Out of memory occured for file with size " + storagePath);
                
            } catch (NoSuchFieldError e) {
                result = null;
                Log.e(TAG, "Error from access to unexisting field despite protection " + storagePath);
                
            } catch (Throwable t) {
                result = null;
                Log.e(TAG, "Unexpected error while creating image preview " + storagePath, t);
            }
            return result;
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && mPreview != null) {
                mPreview.setImageBitmap(result);
            }
        }
        
    }
    

}
