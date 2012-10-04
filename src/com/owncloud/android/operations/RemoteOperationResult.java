/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.operations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;

import com.owncloud.android.network.CertificateCombinedException;


/**
 * The result of a remote operation required to an ownCloud server.
 * 
 * Provides a common classification of remote operation results for all the application. 
 * 
 * @author David A. Velasco
 */
public class RemoteOperationResult {
    
    public enum ResultCode { 
        OK,
        OK_SSL,
        OK_NO_SSL,
        UNHANDLED_HTTP_CODE,
        FILE_NOT_FOUND, 
        INSTANCE_NOT_CONFIGURED, 
        UNKNOWN_ERROR, 
        WRONG_CONNECTION,  
        TIMEOUT, 
        INCORRECT_ADDRESS, 
        HOST_NOT_AVAILABLE, 
        NO_NETWORK_CONNECTION, 
        SSL_ERROR,
        SSL_RECOVERABLE_PEER_UNVERIFIED,
        BAD_OC_VERSION 
    }

    private boolean mSuccess = false;
    private int mHttpCode = -1;
    private Exception mException = null;
    private ResultCode mCode = ResultCode.UNKNOWN_ERROR;
    
    public RemoteOperationResult(ResultCode code) {
        mCode = code;
        mSuccess = (code == ResultCode.OK || code == ResultCode.OK_SSL || code == ResultCode.OK_NO_SSL);
    }
    
    public RemoteOperationResult(boolean success, int httpCode) {
        mSuccess = success; 
        mHttpCode = httpCode;

        if (success) {
            mCode = ResultCode.OK;
            
        } else if (httpCode > 0) {
            switch (httpCode) {
                case HttpStatus.SC_NOT_FOUND:
                    mCode = ResultCode.FILE_NOT_FOUND;
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    mCode = ResultCode.INSTANCE_NOT_CONFIGURED;
                    break;
                default:
                    mCode = ResultCode.UNHANDLED_HTTP_CODE;
            }
        }
    }
    
    public RemoteOperationResult(Exception e) {
        mException = e; 
        
        if (e instanceof SocketException) {  
            mCode = ResultCode.WRONG_CONNECTION;
        
        } else if (e instanceof SocketTimeoutException) {
            mCode = ResultCode.TIMEOUT;
        
        } else if (e instanceof ConnectTimeoutException) {
            mCode = ResultCode.TIMEOUT;
            
        } else if (e instanceof MalformedURLException) {
            mCode = ResultCode.INCORRECT_ADDRESS;
        
        } else if (e instanceof UnknownHostException) {
            mCode = ResultCode.HOST_NOT_AVAILABLE;
        
        } else if (e instanceof SSLException || e instanceof RuntimeException) {
            CertificateCombinedException se = getCertificateCombinedException(e);
            if (se != null) {
                mException = se;
                if (se.isRecoverable()) { 
                    mCode = ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
                }
                
            } else { 
                mCode = ResultCode.SSL_ERROR;
            }
            
        } else {
            mCode = ResultCode.UNKNOWN_ERROR;
        }
        
    }
    
    
    public boolean isSuccess() {
        return mSuccess;
    }
    
    public int getHttpCode() {
        return mHttpCode;
    }
    
    public ResultCode getCode() {
        return mCode;
    }
    
    public Exception getException() {
        return mException;
    }

    public boolean isSslRecoverableException() {
        return mCode == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
    }
    
    private CertificateCombinedException getCertificateCombinedException(Exception e) {
        CertificateCombinedException result = null;
        if (e instanceof CertificateCombinedException) {
            return (CertificateCombinedException)e;
        }
        Throwable cause = mException.getCause();
        Throwable previousCause = null;
        while (cause != null && cause != previousCause && !(cause instanceof CertificateCombinedException)) {
            previousCause = cause;
            cause = cause.getCause();
        }
        if (cause != null && cause instanceof CertificateCombinedException) {
            result = (CertificateCombinedException)cause; 
        }
        return result;
    }
    
    
    public String getLogMessage() {
        
        if (mException != null) {
            if (mException instanceof SocketException) {  
                return "Socket exception";
        
            } else if (mException instanceof SocketTimeoutException) {
                return "Socket timeout exception";
        
            } else if (mException instanceof ConnectTimeoutException) {
                return "Connect timeout exception";
            
            } else if (mException instanceof MalformedURLException) {
                return "Malformed URL exception";
        
            } else if (mException instanceof UnknownHostException) {
                return "Unknown host exception";
        
            } else if (mException instanceof SSLException) {
                if (mCode == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)
                    return "SSL recoverable exception";
                else
                    return "SSL exception";

            } else if (mException instanceof HttpException) {
                return "HTTP violation";

            } else if (mException instanceof IOException) {
                return "Unrecovered transport exception";

            } else {
                return "Unexpected exception";
            }
        }
        
        if (mCode == ResultCode.INSTANCE_NOT_CONFIGURED) {
            return "The ownCloud server is not configured!";
            
        } else if (mCode == ResultCode.NO_NETWORK_CONNECTION) {
            return "No network connection";
            
        } else if (mCode == ResultCode.BAD_OC_VERSION) {
            return "No valid ownCloud version was found at the server";
        }
        
        return "Operation finished with HTTP status code " + mHttpCode + " (" + (isSuccess()?"success":"fail") + ")";

    }

}
