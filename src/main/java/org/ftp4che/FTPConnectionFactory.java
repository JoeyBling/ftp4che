/**                                                                         
 *  This file is part of ftp4che.                                            
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published
 *  by the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.ftp4che;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.ftp4che.exception.ConfigurationException;
import org.ftp4che.impl.SecureFTPConnection;
import org.ftp4che.impl.NormalFTPConnection;
import org.ftp4che.proxy.Proxy;
import org.ftp4che.proxy.Socks4;
import org.ftp4che.proxy.Socks5;


/**
 * @author arnold
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class FTPConnectionFactory {

    /**
     * This factory should be called to get you a new FTPConnection You can set
     * the connection information with a properties object.
     * 
     * @param pt
     *            you have to set the connection informations: connection.host =
     *            hostname to the server you want to connect (String)
     *            connection.port = port you want to connect to (String)
     *            user.login = login name (String) user.password = password
     *            (Sring). this parameter is optional user.account = Account
     *            Information (String). This parameter is optional
     *            connection.type = The connection you want to have (normal,auth
     *            ssl,auth tls,...). There are constants (int primitiv type) in
     *            FTPConnection. You have to give a Integer object.
     *            connection.timeout = The timeout that will be used (Long
     *            object) connection.passive = Should the DataConnection be
     *            established in passive mode (Boolean Object)
     *            connection.downloadbw = Maximum bytes / second that should be
     *            used for downloading connection.uploadbw = Maximum bytes /
     *            second that should be used for uploading
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(Properties pt)
            throws ConfigurationException {
        int port = 21;
        int connectionTimeout = 10000;
        int connectionType = FTPConnection.FTP_CONNECTION;
        boolean passive = true;
        int downloadBandwidth = Integer.MAX_VALUE;
        int uploadBandwidth = Integer.MAX_VALUE;
        String proxyType = null, proxyHost = null, proxyUser = null, proxyPass = null;
        int proxyPort = -1, proxyTimeout = -1;

        if (pt.getProperty("connection.port") != null)
            port = Integer.parseInt(pt.getProperty("connection.port"));
        if (pt.getProperty("connection.timeout") != null)
            connectionTimeout = Integer.parseInt(pt
                    .getProperty("connection.timeout"));
        String connectionTypeName = pt.getProperty("connection.type");
        if (connectionTypeName != null)
            if (connectionTypeName.equalsIgnoreCase("FTP_CONNECTION"))
                connectionType = FTPConnection.FTP_CONNECTION;
            else if (connectionTypeName
                    .equalsIgnoreCase("AUTH_TLS_FTP_CONNECTION"))
                connectionType = FTPConnection.AUTH_TLS_FTP_CONNECTION;
            else if (connectionTypeName
                    .equalsIgnoreCase("AUTH_SSL_FTP_CONNECTION"))
                connectionType = FTPConnection.AUTH_SSL_FTP_CONNECTION;
            else if (connectionTypeName
                    .equalsIgnoreCase("IMPLICIT_SSL_FTP_CONNECTION"))
                connectionType = FTPConnection.IMPLICIT_SSL_FTP_CONNECTION;
            else if (connectionTypeName
                    .equalsIgnoreCase("IMPLICIT_TLS_FTP_CONNECTION"))
                connectionType = FTPConnection.IMPLICIT_TLS_FTP_CONNECTION;
            else if (connectionTypeName
                    .equalsIgnoreCase("IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION"))
                    connectionType = FTPConnection.IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION;
            else if (connectionTypeName
                        .equalsIgnoreCase("IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION"))
                        connectionType = FTPConnection.IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION;
            else
                throw new ConfigurationException(
                        "Connection method not specified");
        if (pt.getProperty("connection.passive") != null) {
            if (pt.getProperty("connection.passive").equalsIgnoreCase("true"))
                passive = true;
            else
                passive = false;
            // passive =
            // Boolean.getBoolean(pt.getProperty("connection.passive").trim());
        }
        if (pt.getProperty("connection.downloadbw") != null)
            downloadBandwidth = Integer.parseInt(pt
                    .getProperty("connection.downloadbw"));
        if (pt.getProperty("connection.uploadbw") != null)
            uploadBandwidth = Integer.parseInt(pt
                    .getProperty("connection.uploadbw"));
        if (pt.getProperty("connection.downloadbw") != null)
            downloadBandwidth = Integer.parseInt(pt
                    .getProperty("connection.downloadbw"));
        if (pt.getProperty("connection.uploadbw") != null)
            uploadBandwidth = Integer.parseInt(pt
                    .getProperty("connection.uploadbw"));
        if (pt.getProperty("proxy.type") != null)
            proxyType = pt.getProperty("proxy.type");
        if (pt.getProperty("proxy.host") != null)
            proxyHost = pt.getProperty("proxy.host");
        if (pt.getProperty("proxy.port") != null)
            proxyPort = Integer.parseInt(pt.getProperty("proxy.port"));
        if (pt.getProperty("proxy.user") != null)
            proxyUser = pt.getProperty("proxy.user");
        if (pt.getProperty("proxy.pass") != null)
            proxyPass = pt.getProperty("proxy.pass");
        if (pt.getProperty("proxy.timeout") != null)
            proxyTimeout = Integer.parseInt(pt.getProperty("proxy.timeout"));
        boolean tryResume = false;
        if (pt.getProperty("connection.resume") != null && pt.getProperty("connection.resume").equals("true"))
        	tryResume = true;
        return FTPConnectionFactory.getInstance(pt
                .getProperty("connection.host"), port, pt
                .getProperty("user.login"), pt.getProperty("user.password"), pt
                .getProperty("user.account"), connectionTimeout,
                connectionType, passive, downloadBandwidth, uploadBandwidth,
                proxyType, proxyHost, proxyPort, proxyUser, proxyPass,
                proxyTimeout,tryResume);

    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param port =
     *            port you want to connect to
     * @param user =
     *            login name
     * @param password =
     *            password. this parameter is optional
     * @param account =
     *            Account Information. This parameter is optional
     * @param connectionType =
     *            The connection you want to have (normal,auth ssl,auth
     *            tls,...). There are constants (int primitiv type) in
     *            FTPConnection.
     * @param timeout =
     *            The timeout that will be used
     * @param passiveMode =
     *            Should the DataConnection be established in passive mode
     * @param maxDownloadBandwidth =
     *            Maximum bytes / second that should be used for downloading
     * @param maxUploadBandwidth =
     *            Maximum bytes / second that should be used for uploading
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, int port, String user,
            String password, String account, int timeout, int connectionType,
            boolean passiveMode) throws ConfigurationException {
        return FTPConnectionFactory.getInstance(host, port, user, password,
                null, 10000, connectionType, passiveMode,
                FTPConnection.MAX_DOWNLOAD_BANDWIDTH,
                FTPConnection.MAX_UPLOAD_BANDWIDTH, null, null, -1, null, null,
                -1,false);
    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param port =
     *            port you want to connect to
     * @param user =
     *            login name
     * @param password =
     *            password. this parameter is optional
     * @param account =
     *            Account Information. This parameter is optional
     * @param connectionType =
     *            The connection you want to have (normal,auth ssl,auth
     *            tls,...). There are constants (int primitiv type) in
     *            FTPConnection.
     * @param timeout =
     *            The timeout that will be used
     * @param passiveMode =
     *            Should the DataConnection be established in passive mode
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, int port, String user,
            String password, String account, int timeout, int connectionType,
            boolean passiveMode, int maxDownloadBandwidth,
            int maxUploadBandwidth, String proxyType, String proxyHost,
            int proxyPort, String proxyUser, String proxyPass, int proxyTimeout,boolean tryResume)
            throws ConfigurationException {
        FTPConnection connection = null;
        if (connectionType == FTPConnection.FTP_CONNECTION) {
            connection = new NormalFTPConnection();
        } else if (connectionType == FTPConnection.AUTH_TLS_FTP_CONNECTION
                || connectionType == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_SSL_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_TLS_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION) {
            connection = new SecureFTPConnection();
        } else {
            throw new ConfigurationException(
                    "No or unknown connection.type in properties");
        }
        connection.setConnectionType(connectionType);
        connection.setAddress(new InetSocketAddress(host, port));
        connection.setUser(user);
        connection.setPassword(password);
        connection.setAccount(account);
        connection.setTimeout(timeout);
        connection.setPassiveMode(passiveMode);
        connection.setDownloadBandwidth(maxDownloadBandwidth);
        connection.setUploadBandwidth(maxUploadBandwidth);
        connection.setTryResume(tryResume);
        
        Proxy proxy = null;
        if (proxyType != null) {
            if (proxyType.equalsIgnoreCase("SOCKS4")) {
                proxy = new Socks4(proxyHost, proxyPort, proxyTimeout,
                        proxyUser);
            } else if (proxyType.equalsIgnoreCase("SOCKS5")) {
                proxy = new Socks5(proxyHost, proxyPort, proxyUser, proxyPass);
            }
        }

        connection.setProxy(proxy);

        return connection;
    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param port =
     *            port you want to connect to
     * @param user =
     *            login name
     * @param password =
     *            password. this parameter is optional
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, int port, String user,
            String password) throws ConfigurationException {
        return FTPConnectionFactory.getInstance(host, port, user, password,
                null, 10000, FTPConnection.FTP_CONNECTION, false);
    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param port =
     *            port you want to connect to
     * @param user =
     *            login name
     * @param password =
     *            password. this parameter is optional
     * @param account =
     *            Account Information. This parameter is optional
     * @param passiveMode =
     *            Should the DataConnection be established in passive mode
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, int port, String user,
            String password, boolean passive) throws ConfigurationException {
        return FTPConnectionFactory.getInstance(host, port, user, password,
                null, 10000, FTPConnection.FTP_CONNECTION, passive);
    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param port =
     *            port you want to connect to
     * @param user =
     *            login name
     * @param password =
     *            password. this parameter is optional
     * @param connectionType =
     *            The connection you want to have (normal,auth ssl,auth
     *            tls,...). There are constants (int primitiv type) in
     *            FTPConnection.
     * @param passiveMode =
     *            Should the DataConnection be established in passive mode
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, int port, String user,
            String password, int connectionType, boolean passive)
            throws ConfigurationException {
        return FTPConnectionFactory.getInstance(host, port, user, password,
                null, 10000, connectionType, passive);
    }

    /**
     * This factory should be called to get you a new FTPConnection
     * 
     * @param host =
     *            hostname to the server you want to connect
     * @param user =
     *            login name
     * @return FTPConnection the ftpconnection. you can than do a connect() and
     *         login() to connect and login to the server
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     * @author arnold,kurt
     */
    public static FTPConnection getInstance(String host, String user)
            throws ConfigurationException {
        return FTPConnectionFactory.getInstance(host, 21, user, "", null,
                10000, FTPConnection.FTP_CONNECTION, false);
    }
}
