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
package org.ftp4che.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;
import org.ftp4che.FTPConnection;
import org.ftp4che.proxy.Proxy;

public class SocketProvider {

    private SSLSupport supporter;

    private int sslMode = FTPConnection.FTP_CONNECTION;

    private Socket socket = null;

    private static final Logger log = Logger.getLogger(SocketProvider.class
            .getName());

    private boolean isControllConnection = true;

    private OutputStream out = null;

    private InputStream in = null;

    private byte[] readArray = new byte[16384];

    int maxDownload, maxUpload;

    public SocketProvider() {
        socket = new Socket();
    }

    public SocketProvider(boolean isControllConnection) throws IOException {
        this();
        setControllConnection(isControllConnection);
    }

    public SocketProvider(Socket socket, boolean isControllConnection)
            throws IOException {
        this(socket, isControllConnection,
                FTPConnection.MAX_DOWNLOAD_BANDWIDTH,
                FTPConnection.MAX_UPLOAD_BANDWIDTH);
    }

    // public SocketProvider( Socket socket ) throws IOException{
    // this(socket, true, FTPConnection.MAX_DOWNLOAD_BANDWIDTH,
    // FTPConnection.MAX_UPLOAD_BANDWIDTH);
    // }

    public SocketProvider(Socket socket, boolean isControllConnection,
            int maxDownload, int maxUpload) throws IOException {
        setControllConnection(isControllConnection);
        this.maxDownload = maxDownload;
        this.maxUpload = maxUpload;
        this.socket = socket;
        initStreams();
    }

    private void initStreams() throws IOException {
        if (out == null) {
            if (maxUpload == FTPConnection.MAX_UPLOAD_BANDWIDTH
                    || isControllConnection()) {
                out = socket.getOutputStream();
            } else {
                out = new BandwidthControlledOutputStream(socket
                        .getOutputStream(), maxUpload);
            }
        }
        if (in == null) {
            if (maxDownload == FTPConnection.MAX_DOWNLOAD_BANDWIDTH
                    || isControllConnection()) {
                in = socket.getInputStream();
            } else {
                in = new BandwidthControlledInputStream(
                        socket.getInputStream(), maxDownload);
            }
        }
    }

    // public void connect( SocketAddress remote ) throws IOException {
    // connect(remote, FTPConnection.MAX_DOWNLOAD_BANDWIDTH,
    // FTPConnection.MAX_UPLOAD_BANDWIDTH);
    // }

    public void connect(SocketAddress remote, Proxy proxy, int maxDownload,
            int maxUpload) throws IOException {

        if (proxy == null) {
            socket.connect(remote);
        } else {
            InetSocketAddress isa = (InetSocketAddress) remote;
            socket = proxy.connect(isa.getAddress().getHostAddress(), isa
                    .getPort());
        }

        this.maxDownload = maxDownload;
        this.maxUpload = maxUpload;
        initStreams();
    }

    public Socket socket() {
        return socket;
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public boolean needsCrypt() {
        return ((this.sslMode == FTPConnection.AUTH_SSL_FTP_CONNECTION || this.sslMode == FTPConnection.AUTH_TLS_FTP_CONNECTION || this.sslMode == FTPConnection.IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION || this.sslMode == FTPConnection.IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION) && !isControllConnection())
                || this.sslMode != FTPConnection.FTP_CONNECTION
                && isControllConnection();
    }

    public void close() throws IOException {

        if (needsCrypt()) {
            if (supporter != null)
                supporter.close();
        }
        socket.close();
    }

    public int write(ByteBuffer src) throws IOException {
        if (needsCrypt()) {
            return supporter.write(src);
            // throw new IOException("SSL NOT IMPLEMENTED YET");
        }
        int byteCount = src.remaining();
        out.write(src.array(), 0, byteCount);
        return byteCount;
    }

    public int read(ByteBuffer dst) throws IOException {
        if (needsCrypt()) {
            return supporter.read(dst);
        }
        int byteCount = 0;
        if (isControllConnection()) {
            byteCount = in.read(readArray, 0, dst.remaining());
        } else {
            byteCount = in.read(readArray);
        }
        log.debug("Read -> " + byteCount + " byte");
        if (byteCount <= 0)
            return byteCount;
        dst.put(readArray, dst.position(), byteCount);
        return byteCount;
    }

    public String toString() {
        return socket.getInetAddress().getHostAddress() + ":"
                + socket.getPort();
    }

    /**
     * @return Returns the sslMode.
     */
    public int getSSLMode() {
        return sslMode;
    }

    /**
     * @param sslMode
     *            The sslMode to set.
     */
    public void setSSLMode(int sslMode) {
        this.sslMode = sslMode;
    }

    /**
     * @return Returns the isControllConnection.
     */
    public boolean isControllConnection() {
        return isControllConnection;
    }

    /**
     * @param isControllConnection
     *            The isControllConnection to set.
     */
    public void setControllConnection(boolean isControllConnection) {
        this.isControllConnection = isControllConnection;
    }

    public void negotiate(TrustManager[] trustManagers,KeyManager[] keyManagers) {
        try {
            supporter = new SSLSupport(socket, getSSLMode(),
                    isControllConnection(), maxDownload, maxUpload);
            supporter.setTrustManagers(trustManagers);
            supporter.setKeyManagers(keyManagers);
            supporter.initEngineAndBuffers();
            supporter.handshake();
            // TODO: throw exception and handle it !!
        } catch (Exception e) {
            log.fatal(e, e);
        }
    }

}
