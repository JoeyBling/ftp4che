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

public class Socks5 implements Proxy {

    // constants
    static final int PROTOCOL_VERSION = 5;

    static final int DEFAULT_PORT = 1080;

    static final int DEFAULT_TIMEOUT = 20000;

    static final int NO_AUTH = 0;

    static final int GSSAPI = 1;

    static final int USER_PASS = 2;

    static final int CONNECT = 1;

    static final int BIND = 2;

    static final int UDP = 3;

    static final int IPV4 = 1;

    static final int DOMAIN_NAME = 3;

    static final int IPV6 = 4;

    static final int REQUEST_OK = 0;

    static final int GENERAL_FAILURE = 1;

    static final int NOT_ALLOWED = 2;

    static final int NET_UNREACHABLE = 3;

    static final int HOST_UNREACHABLE = 4;

    static final int CONNECTION_REFUSED = 5;

    static final int TTL_EXPIRED = 6;

    static final int CMD_NOT_SUPPORTED = 7;

    static final int ADDR_TYPE_NOT_SUPPORTED = 8;

    public static final Logger log = Logger.getLogger(Socks5.class.getName());

    private String host;

    private String user;

    private String pass;

    private int port = DEFAULT_PORT;

    private int primaryConnectionPort;

    private int timeout = DEFAULT_TIMEOUT;

    private Socket socket = null;
    
    private InetSocketAddress bindAddress;

    public Socks5(String proxyHost, String proxyUser, String proxyPass) {
        this(proxyHost, -1, -1, proxyUser, proxyPass);
    }

    public Socks5(String proxyHost, int proxyPort, String proxyUser,
            String proxyPass) {
        this(proxyHost, proxyPort, -1, proxyUser, proxyPass);
    }

    public Socks5(String proxyHost, int proxyPort, int timeout,
            String proxyUser, String proxyPass) {
        setHost(proxyHost);
        setPort(proxyPort);
        setUser(proxyUser);
        setPass(proxyPass);
    }

    /**
     * public Socket connect(String host, int port) throws IOException
     * 
     * establishing connection to the given host, over the socks5 server
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
            throw new ProxyConnectionException(-2, "SOCK5 - IOException: "
                    + ioe.getMessage());
        }

        InetAddress addr = isa.getAddress();
        byte[] hostbytes = addr.getAddress();
        byte[] requestPacket = new byte[300];
        byte[] response = new byte[2];
        byte methodCount = 2;

        requestPacket[0] = PROTOCOL_VERSION; // means socks5 (field VN)
        requestPacket[1] = methodCount; // methods count (field NMETHODS)
        requestPacket[2] = NO_AUTH; // X'00' NO AUTHENTICATION REQUIRED
        // requestPacket[3] = GSSAPI; // X'01' GSSAPI
        if (getUser() != null && getPass() != null)
            requestPacket[3] = USER_PASS; // X'02' USERNAME/PASSWORD

        try {
            connectToProxy();

            this.socket.getOutputStream().write(requestPacket, 0,
                    methodCount + 2);
            this.socket.getInputStream().read(response, 0, 2);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK5 - IOException: "
                    + ioe.getMessage());
        }

        // negotiation for connect method completed
        log.debug("Using method: " + response[1]);

        if (response[1] == 0) { // NO AUTHENTICATION REQUIRED
            return request(hostbytes);
            // }else if (response[1] == 1) { // GSSAPI

        } else if (response[1] == 2) { // USERNAME/PASSWORD
            if (authUserPass(hostbytes))
                return request(hostbytes);
        }

        return null;
    }

    private Socket request(byte[] hostbytes) throws ProxyConnectionException {
        byte[] requestPacket = new byte[1024];
        byte[] response = new byte[10];

        requestPacket[0] = PROTOCOL_VERSION; // means socks5 (field VN)
        requestPacket[1] = CONNECT; // connect (field CMD)
        requestPacket[2] = 0; // reserved (field RSV)
        requestPacket[3] = IPV4; // IPv4 (field ATYP)

        // adding the host adress bytes to the packet (field DST.ADDR)
        System.arraycopy(hostbytes, 0, requestPacket, 4, 4);

        requestPacket[8] = (byte) (getPrimaryConnectionPort() >> 8); // (field
                                                                        // DST.PORT)
        requestPacket[9] = (byte) (getPrimaryConnectionPort() & 0x00ff); // (field
                                                                            // DST.PORT)

        OutputStream out = null;
        InputStream in = null;
        try {
            out = socket.getOutputStream();
            in = socket.getInputStream();

            out.write(requestPacket, 0, 10);
            in.read(response, 0, 10);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK5 - IOException: "
                    + ioe.getMessage());
        }

        ProxyConnectionException pce = null;
        switch (response[1]) {
        case REQUEST_OK:
            break; // request successfull
        case GENERAL_FAILURE:
            pce = new ProxyConnectionException(GENERAL_FAILURE,
                    "SOCKS5 general SOCKS server failure");
            break;
        case NOT_ALLOWED:
            pce = new ProxyConnectionException(NOT_ALLOWED,
                    "SOCKS5 connection not allowed by ruleset");
            break;
        case NET_UNREACHABLE:
            pce = new ProxyConnectionException(NET_UNREACHABLE,
                    "SOCKS5 Network unreachable");
            break;
        case HOST_UNREACHABLE:
            pce = new ProxyConnectionException(HOST_UNREACHABLE,
                    "SOCKS5 Host unreachable");
            break;
        case CONNECTION_REFUSED:
            pce = new ProxyConnectionException(CONNECTION_REFUSED,
                    "SOCKS5 Connection refused");
            break;
        case TTL_EXPIRED:
            pce = new ProxyConnectionException(TTL_EXPIRED,
                    "SOCKS5 TTL expired");
            break;
        case CMD_NOT_SUPPORTED:
            pce = new ProxyConnectionException(CMD_NOT_SUPPORTED,
                    "SOCKS5 Command not supported");
            break;
        case ADDR_TYPE_NOT_SUPPORTED:
            pce = new ProxyConnectionException(ADDR_TYPE_NOT_SUPPORTED,
                    "SOCKS5 Address type not supported");
            break;
        default:
            pce = new ProxyConnectionException(-1,
                    "SOCKS5 unknown proxy response");
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

        return this.socket;
    }

    private boolean authUserPass(byte[] hostbytes)
            throws ProxyConnectionException {
        byte[] requestPacket = new byte[1024];
        byte[] response = new byte[2];
        byte userLen = (byte) getUser().getBytes().length;
        byte passLen = (byte) getPass().getBytes().length;

        requestPacket[0] = 1;
        requestPacket[1] = userLen;

        // adding the username to the packet
        System.arraycopy(getUser().getBytes(), 0, requestPacket, 2, userLen);
        requestPacket[2 + userLen] = passLen;

        // adding the password to the packet
        System.arraycopy(getPass().getBytes(), 0, requestPacket, 3 + userLen,
                passLen);

        OutputStream out = null;
        InputStream in = null;
        try {
            out = socket.getOutputStream();
            in = socket.getInputStream();

            out.write(requestPacket, 0, 3 + userLen + passLen);
            in.read(response, 0, 2);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK5 - IOException: "
                    + ioe.getMessage());
        }

        ProxyConnectionException pce = null;
        switch (response[1]) {
        case REQUEST_OK:
            return true;// request successfull
        case GENERAL_FAILURE:
            pce = new ProxyConnectionException(GENERAL_FAILURE,
                    "SOCKS5 general SOCKS server failure");
            break;
        case NOT_ALLOWED:
            pce = new ProxyConnectionException(NOT_ALLOWED,
                    "SOCKS5 connection not allowed by ruleset");
            break;
        case NET_UNREACHABLE:
            pce = new ProxyConnectionException(NET_UNREACHABLE,
                    "SOCKS5 Network unreachable");
            break;
        case HOST_UNREACHABLE:
            pce = new ProxyConnectionException(HOST_UNREACHABLE,
                    "SOCKS5 Host unreachable");
            break;
        case CONNECTION_REFUSED:
            pce = new ProxyConnectionException(CONNECTION_REFUSED,
                    "SOCKS5 Connection refused");
            break;
        case TTL_EXPIRED:
            pce = new ProxyConnectionException(TTL_EXPIRED,
                    "SOCKS5 TTL expired");
            break;
        case CMD_NOT_SUPPORTED:
            pce = new ProxyConnectionException(CMD_NOT_SUPPORTED,
                    "SOCKS5 Command not supported");
            break;
        case ADDR_TYPE_NOT_SUPPORTED:
            pce = new ProxyConnectionException(ADDR_TYPE_NOT_SUPPORTED,
                    "SOCKS5 Address type not supported");
            break;
        default:
            pce = new ProxyConnectionException(-1,
                    "SOCKS5 unknown proxy response");
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

        return false;
    }

    private void connectToProxy() throws IOException {
        this.socket = new Socket();
        this.socket.setSoTimeout(getTimeout());
        this.socket.connect((SocketAddress) new InetSocketAddress(InetAddress
                .getByName(getHost()), getPort()), getTimeout());
    }

    public Socket bind(InetSocketAddress isa) throws ProxyConnectionException {
        setPrimaryConnectionPort(port);

        InetAddress addr = isa.getAddress();
        byte[] hostbytes = addr.getAddress();
        byte[] requestPacket = new byte[300];
        byte[] response = new byte[2];
        byte methodCount = 2;

        requestPacket[0] = PROTOCOL_VERSION; // means socks5 (field VN)
        requestPacket[1] = methodCount; // methods count (field NMETHODS)
        requestPacket[2] = NO_AUTH; // X'00' NO AUTHENTICATION REQUIRED
        // requestPacket[3] = GSSAPI; // X'01' GSSAPI
        if (getUser() != null && getPass() != null)
            requestPacket[3] = USER_PASS; // X'02' USERNAME/PASSWORD

        try {
            connectToProxy();

            this.socket.getOutputStream().write(requestPacket, 0,
                    methodCount + 2);
            this.socket.getInputStream().read(response, 0, 2);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK4 - IOException: "
                    + ioe.getMessage());
        }

        requestPacket = new byte[1024];
        requestPacket[0] = PROTOCOL_VERSION; // means socks5 (field VN)
        requestPacket[1] = BIND; // connect (field CMD)
        requestPacket[2] = 0; // reserved (field RSV)
        requestPacket[3] = IPV4; // IPv4 (field ATYP)

        // adding the host adress bytes to the packet (field DST.ADDR)
        System.arraycopy(hostbytes, 0, requestPacket, 4, 4);

        requestPacket[8] = (byte) (getPrimaryConnectionPort() >> 8); // (field
                                                                        // DST.PORT)
        requestPacket[9] = (byte) (getPrimaryConnectionPort() & 0x00ff); // (field
                                                                            // DST.PORT)

        response = new byte[10];
        OutputStream out = null;
        InputStream in = null;
        try {
            out = socket.getOutputStream();
            in = socket.getInputStream();

            out.write(requestPacket, 0, 10);
            in.read(response, 0, 10);
        } catch (IOException ioe) {
            throw new ProxyConnectionException(-2, "SOCK5 - IOException: "
                    + ioe.getMessage());
        }

        ProxyConnectionException pce = null;
        switch (response[1]) {
        case REQUEST_OK:
            break; // request successfull
        case GENERAL_FAILURE:
            pce = new ProxyConnectionException(GENERAL_FAILURE,
                    "SOCKS5 general SOCKS server failure");
            break;
        case NOT_ALLOWED:
            pce = new ProxyConnectionException(NOT_ALLOWED,
                    "SOCKS5 connection not allowed by ruleset");
            break;
        case NET_UNREACHABLE:
            pce = new ProxyConnectionException(NET_UNREACHABLE,
                    "SOCKS5 Network unreachable");
            break;
        case HOST_UNREACHABLE:
            pce = new ProxyConnectionException(HOST_UNREACHABLE,
                    "SOCKS5 Host unreachable");
            break;
        case CONNECTION_REFUSED:
            pce = new ProxyConnectionException(CONNECTION_REFUSED,
                    "SOCKS5 Connection refused");
            break;
        case TTL_EXPIRED:
            pce = new ProxyConnectionException(TTL_EXPIRED,
                    "SOCKS5 TTL expired");
            break;
        case CMD_NOT_SUPPORTED:
            pce = new ProxyConnectionException(CMD_NOT_SUPPORTED,
                    "SOCKS5 Command not supported");
            break;
        case ADDR_TYPE_NOT_SUPPORTED:
            pce = new ProxyConnectionException(ADDR_TYPE_NOT_SUPPORTED,
                    "SOCKS5 Address type not supported");
            break;
        default:
            pce = new ProxyConnectionException(-1,
                    "SOCKS5 unknown proxy response");
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
     * @return Returns the user.
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param user
     *            The user to set.
     */
    public void setPass(String pass) {
        this.pass = pass;
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
        // @todo Auto-generated method stub
        return null;
    }

	public InetSocketAddress getProxyAddress() {
		return new InetSocketAddress(getHost(), getPort());
	}
}
