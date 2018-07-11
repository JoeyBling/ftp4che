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
package org.ftp4che.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.ftp4che.exception.ProxyConnectionException;

public class Socks4 implements Proxy {

    // protocol constants
    static final int PROTOCOL_VERSION = 4;

    static final int DEFAULT_PORT = 1080;

    static final int DEFAULT_TIMEOUT = 20000;

    static final int CONNECT = 1;

    static final int BIND = 2;

    static final int REQUEST_OK = 90;

    static final int REQUEST_REJECTED_OR_FAILED = 91;

    static final int IDENTD_CONNECT_FAILED = 92;

    static final int DIFFERENT_UIDS_GIVEN = 93;

    // log4j logger
    public static final Logger log = Logger.getLogger(Socks4.class.getName());

    // instances
    private String host;

    private String user;

    private int port = DEFAULT_PORT;

    private int primaryConnectionPort;

    private int timeout = DEFAULT_TIMEOUT;

    private Socket socket = null;
    
    private InetSocketAddress bindAddress;

    public Socks4(String proxyHost, String proxyUser) {
        this(proxyHost, -1, -1, proxyUser);
    }

    public Socks4(String proxyHost, int proxyPort, String proxyUser) {
        this(proxyHost, proxyPort, -1, proxyUser);
    }

    public Socks4(String proxyHost, int proxyPort, int timeout, String proxyUser) {
        setHost(proxyHost);
        setPort(proxyPort);
        setUser(proxyUser);
        setTimeout(timeout);
    }

    private void connectToProxy() throws IOException {
        this.socket = new Socket();
        this.socket.setSoTimeout(getTimeout());
        this.socket.connect((SocketAddress) new InetSocketAddress(InetAddress
                .getByName(getHost()), getPort()), getTimeout());
    }

    /**
     * public Socket connect(String host, int port) throws IOException
     * 
     * establishing connection to the given host, over the socks4 server
     * 
     * @param host
     *            the wanted hostname / ip for connection
     * @param port
     *            the connection port
     * 
     * @return Socket the connection socket over proxy
     */
    public Socket connect(String host, int port)
            throws ProxyConnectionException {
        setPrimaryConnectionPort(port);

        InetSocketAddress isa = null;

        try {
            isa = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK4 - IOException: "
                    + ioe.getMessage());
        }

        InetAddress addr = isa.getAddress();
        byte[] hostbytes = addr.getAddress();
        byte[] requestPacket = new byte[300];

        requestPacket[0] = PROTOCOL_VERSION; // (field VN)
        requestPacket[1] = CONNECT; // (field CD)
        requestPacket[2] = new Integer((port & 0xff00) >> 8).byteValue(); // (field
                                                                            // DSTPORT)
        requestPacket[3] = new Integer((port & 0x00ff)).byteValue(); // (field
                                                                        // DSTPORT)

        // adding the host adress bytes to the packet (field DSTIP)
        System.arraycopy(hostbytes, 0, requestPacket, 4, 4);

        // adding user id to packet (field USERID)
        System.arraycopy(getUser().getBytes(), 0, requestPacket, 8, getUser()
                .getBytes().length);

        // terminate the packet
        requestPacket[9 + getUser().length()] = 0;

        byte[] response = new byte[8];

        // connect the socket
        OutputStream out = null;
        InputStream in = null;
        try {
            connectToProxy();

            out = socket.getOutputStream();
            in = socket.getInputStream();

            out.write(requestPacket, 0, 9 + getUser().getBytes().length);
            in.read(response, 0, 8);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK4 - IOException: "
                    + ioe.getMessage());
        }

        if (response[0] != 0 && response[0] != 4)
            throw new ProxyConnectionException(-3,
                    "SOCKS4 wrong protocol version reply");

        ProxyConnectionException pce = null;
        switch (response[1]) {
        case REQUEST_OK:
            break; // connect successfull
        case REQUEST_REJECTED_OR_FAILED:
            pce = new ProxyConnectionException(REQUEST_REJECTED_OR_FAILED,
                    "SOCKS4 request rejected or failed");
            break;
        case IDENTD_CONNECT_FAILED:
            pce = new ProxyConnectionException(
                    IDENTD_CONNECT_FAILED,
                    "SOCKS4 request rejected becasue SOCKS server cannot connect to identd on the client");
            break;
        case DIFFERENT_UIDS_GIVEN:
            pce = new ProxyConnectionException(
                    DIFFERENT_UIDS_GIVEN,
                    "SOCKS4 request rejected because the client program and identd report different user-ids.");
            break;
        default:
            pce = new ProxyConnectionException(-1,
                    "SOCKS4 unknown proxy response");
            break;
        }

        if (pce != null) {
            try {
                out.close();
                in.close();
                this.socket.close();
            } catch (IOException ioe) {
            }
            throw pce;
        }

        return socket;
    }

    public Socket bind(InetSocketAddress isa) throws IOException {

        InetAddress addr = isa.getAddress();
        byte[] hostbytes = addr.getAddress();
        byte[] requestPacket = new byte[300];

        requestPacket[0] = PROTOCOL_VERSION; // means socks4 (field VN)
        requestPacket[1] = BIND; // means bind (field CD)
        requestPacket[2] = (byte) (getPrimaryConnectionPort() >> 8); // (field
                                                                        // DSTPORT)
        requestPacket[3] = (byte) (getPrimaryConnectionPort() & 0x00ff); // (field
                                                                            // DSTPORT)

        // adding the host adress bytes to the packet (field DSTIP)
        System.arraycopy(hostbytes, 0, requestPacket, 4, 4);

        // adding user id to packet (field USERID)
        System.arraycopy(getUser().getBytes(), 0, requestPacket, 8, getUser()
                .getBytes().length);

        // terminate the packet
        requestPacket[9 + getUser().getBytes().length] = 0;

        byte[] response = new byte[8];
        // connect the socket
        OutputStream out = null;
        InputStream in = null;
        try {
            connectToProxy();

            out = socket.getOutputStream();
            in = socket.getInputStream();

            out.write(requestPacket, 0, 9 + getUser().getBytes().length);
            in.read(response, 0, 8);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK4 - IOException: "
                    + ioe.getMessage());
        }

        if (response[0] != 0 && response[0] != 4)
            throw new ProxyConnectionException(-3,
                    "SOCKS4 wrong protocol version reply");

        ProxyConnectionException pce = null;
        switch (response[1]) {
        case REQUEST_OK:
            break; // bind successfull
        case REQUEST_REJECTED_OR_FAILED:
            pce = new ProxyConnectionException(REQUEST_REJECTED_OR_FAILED,
                    "SOCKS4 request rejected or failed");
            break;
        case IDENTD_CONNECT_FAILED:
            pce = new ProxyConnectionException(
                    IDENTD_CONNECT_FAILED,
                    "SOCKS4 request rejected becasue SOCKS server cannot connect to identd on the client");
            break;
        case DIFFERENT_UIDS_GIVEN:
            pce = new ProxyConnectionException(
                    DIFFERENT_UIDS_GIVEN,
                    "SOCKS4 request rejected because the client program and identd report different user-ids.");
            break;
        default:
            pce = new ProxyConnectionException(-1,
                    "SOCKS4 unknown proxy response");
            break;
        }

        if (pce != null) {
            try {
                out.close();
                in.close();
                this.socket.close();
            } catch (IOException ioe) {
            }
            throw pce;
        }

        int aPort = response[2];
        int bPort = response[3];
        aPort = (aPort < 0 ? (aPort + 256) : aPort);
        bPort = (bPort < 0 ? (bPort + 256) : bPort);

        int bindPort = (aPort << 8) + (bPort);

        byte[] bindAddr = { response[4], response[5], response[6], response[7] };
        bindAddress = new InetSocketAddress(InetAddress.getByAddress(bindAddr), bindPort);

        return this.socket;
    }

    /**
     * @return Returns the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     *            The port to set.
     */
    public void setPort(int port) {
        if (port > 0)
            this.port = port;
        else
            this.port = DEFAULT_PORT;
    }

    /**
     * @return Returns the user.
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user
     *            The user to set.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return Returns the primaryConnectionPort.
     */
    public int getPrimaryConnectionPort() {
        return primaryConnectionPort;
    }

    /**
     * @param primaryConnectionPort
     *            The primaryConnectionPort to set.
     */
    public void setPrimaryConnectionPort(int primaryConnectionPort) {
        this.primaryConnectionPort = primaryConnectionPort;
    }

    /**
     * @return Returns the timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout
     *            The timeout to set.
     */
    public void setTimeout(int timeout) {
        if (timeout > 0)
            this.timeout = timeout;
        else
            this.timeout = DEFAULT_TIMEOUT;
    }

    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

	public InetSocketAddress getProxyAddress() {
		return new InetSocketAddress(getHost(), getPort());
	}
}
