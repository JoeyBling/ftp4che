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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.ftp4che.commands.Command;
import org.ftp4che.commands.ListCommand;
import org.ftp4che.commands.RetrieveCommand;
import org.ftp4che.commands.StoreCommand;
import org.ftp4che.event.FTPEvent;
import org.ftp4che.event.FTPListener;
import org.ftp4che.exception.AuthenticationNotSupportedException;
import org.ftp4che.exception.ConfigurationException;
import org.ftp4che.exception.FtpFileNotFoundException;
import org.ftp4che.exception.FtpIOException;
import org.ftp4che.exception.FtpWorkflowException;
import org.ftp4che.exception.NotConnectedException;
import org.ftp4che.exception.UnkownReplyStateException;
import org.ftp4che.io.ReplyWorker;
import org.ftp4che.io.SocketProvider;
import org.ftp4che.proxy.Proxy;
import org.ftp4che.reply.Reply;
import org.ftp4che.reply.ReplyCode;
import org.ftp4che.util.ReplyFormatter;
import org.ftp4che.util.ftpfile.FTPFile;
import org.ftp4che.util.ftpfile.FTPFileFactory;

/**
 * @author arnold,kurt
 * 
 */
public abstract class FTPConnection {

    // TODO: support PRET command

    // event handling
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Constants for connection.type public static final int FTP_CONNECTION = 1;
     * public static final int IMPLICIT_SSL_FTP_CONNECTION = 2; public static
     * final int AUTH_SSL_FTP_CONNECTION = 3; public static final int
     * AUTH_TLS_FTP_CONNECTION = 4; public static final int
     * IMPLICIT_TLS_FTP_CONNECTION = 5;
     */
    public static final int FTP_CONNECTION = 1;

    public static final int IMPLICIT_SSL_FTP_CONNECTION = 2;

    public static final int AUTH_SSL_FTP_CONNECTION = 3;

    public static final int AUTH_TLS_FTP_CONNECTION = 4;

    public static final int IMPLICIT_TLS_FTP_CONNECTION = 5;
    
    public static final int IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION = 6;
    
    public static final int IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION = 7;

    /**
     * Constants for up-/download bandwidth public static final long
     * MAX_DOWNLOAD_BANDWIDTH = public static final long MAX_UPLOAD_BANDWIDTH =
     */
    public static final int MAX_DOWNLOAD_BANDWIDTH = Integer.MAX_VALUE;

    public static final int MAX_UPLOAD_BANDWIDTH = Integer.MAX_VALUE;

    /**
     * Connection status that are possbile public static final int CONNECTED =
     * 1001; public static final int DISCONNECTED = 1002; public static final
     * int IDLE = 1003; public static final int RECEIVING_FILE = 1004; public
     * static final int SENDING_FILE = 1005; public static final int FXP_FILE =
     * 1006; public static final int UNKNOWN = 9999;
     */

    public static final int CONNECTED = 1001;

    public static final int DISCONNECTED = 1002;

    public static final int IDLE = 1003;
    
    public static final int BUSY = 1010;

    public static final int RECEIVING_FILE = 1004;

    public static final int SENDING_FILE = 1005;

    public static final int FXP_FILE = 1006;

    public static final int UNKNOWN = 9999;
    
    public static final int ERROR = 1011;

    // download / upload / fxp stati
    public static final int RECEIVING_FILE_STARTED = 2001;

    public static final int RECEIVING_FILE_ENDED = 2002;

    public static final int SENDING_FILE_STARTED = 2003;

    public static final int SENDING_FILE_ENDED = 2004;

    public static final int FXPING_FILE_STARTED = 2005;

    public static final int FXPING_FILE_ENDED = 2006;

    public static final int CSL_DIRECT_CALL = 0;
    
    public static final int CSL_INDIRECT_CALL = 1;
    
    public static final int CSL_2WAY_INDIRECT_CALL= 2;

    /*
     * Member variables
     */
    private static final Logger log = Logger.getLogger(FTPConnection.class
            .getName());

    private int connectionType = FTPConnection.FTP_CONNECTION;

    private String secureFXPType = null;

    private boolean sscnActive = false;

    private InetSocketAddress address = null;

    private String user = "";

    private String password = "";

    private String account = "";

    private boolean passiveMode = false;

    private int timeout = 10000;

    private int downloadBandwidth = MAX_DOWNLOAD_BANDWIDTH;

    private int uploadBandwidth = MAX_UPLOAD_BANDWIDTH;

    private Charset charset = Charset.forName("ISO-8859-1");

    private CharsetEncoder encoder = charset.newEncoder();

    private CharBuffer controlBuffer = CharBuffer.allocate(1024);

    protected SocketProvider socketProvider = null;

    private int connectionStatus = UNKNOWN;
    
    private int connectionStatusLock = FTPConnection.CSL_DIRECT_CALL;

    private Proxy proxy = null;

    protected FTPFileFactory factory;

    private String workingDirectory = null;
    
    private TrustManager[] trustManagers = {};
    
    private KeyManager[] keyManagers = {};
    
    private boolean tryResume = false;
    
    private boolean pretSupport = false;
    
    private boolean cpsvSupport = false;
    
    private boolean sscnSupport = false;

    private String connectionTransferType = Command.TYPE_I;
    
    /**
     * @param address
     *            Set method for the address the FTPConnection will connect to
     *            if connect() is called
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @param address
     *            The address to set.
     */
    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    /**
     * @author arnold,kurt
     * @param password
     *            Get method for the password the FTPConnection will use if
     *            connect() is called
     */
    protected String getPassword() {
        return password;
    }

    /**
     * @author arnold,kurt
     * @param password
     *            Set method for the password the FTPConnection will use if
     *            connect() is called
     */
    protected void setPassword(String password) {
        this.password = password;
    }

    /**
     * @author arnold,kurt
     * @param user
     *            Get method for the user the FTPConnection will use if
     *            connect() is called
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     */
    public String getUser() {
        return user;
    }

    /**
     * @author arnold,kurt
     * @param user
     *            Set method for the user the FTPConnection will use if
     *            connect() is called
     * @throws ConfigurationException
     *             will be thrown if a parameter is missing or invalid
     */
    protected void setUser(String user) throws ConfigurationException {
        if (user == null || user.length() == 0)
            throw new ConfigurationException(
                    "user must no be null or has a length of 0");
        this.user = user;
    }

    /**
     * @author arnold,kurt
     * @param account
     *            Get method for the account the FTPConnection will use if
     *            connect() is called
     */
    public String getAccount() {
        return account;
    }

    /**
     * @author arnold,kurt
     * @param account
     *            Set method for the account the FTPConnection will use if
     *            connect() is called
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * This method is used to connect and login to the specified server.
     * 
     * @author arnold,kurt
     * @exception NotConnectedException
     *                will be thrown if it was not possible to establish a
     *                connection to the specified server
     * @exception IOException
     *                will be thrown if it there was a problem sending the
     *                LoginCommand to the server
     */
    public abstract void connect() throws NotConnectedException, IOException,
            AuthenticationNotSupportedException, FtpIOException,
            FtpWorkflowException;

    /**
     * This method is used to disconnect from the specified server.
     * 
     * @author arnold,kurt
     */
    public void disconnect() {
        try {
            Command command = new Command(Command.QUIT);
            sendCommand(command).dumpReply();
            socketProvider.close();
        } catch (IOException ioe) {
            log.warn("Error closing connection: " + getAddress().getHostName()
                    + ":" + getAddress().getPort(), ioe);
        }
        socketProvider = null;
        setConnectionStatusLock(FTPConnection.CSL_DIRECT_CALL);
        this.setConnectionStatus(FTPConnection.DISCONNECTED);
    }

    /**
     * This method is used to send commands (there is an implementation for each
     * possible command). You should call this method if you want to send a raw
     * command and get the full results or if there is no implemented
     * corresponding method.
     * 
     * @return Reply for the specific command. You will get a result for each
     *         server reply.
     * @author arnold,kurt
     * @exception IOException
     *                will be thrown if there was a communication problem with
     *                the server
     */
    public Reply sendCommand(Command cmd) throws IOException {
        
        if (getConnectionStatusLock() == CSL_DIRECT_CALL)
            setConnectionStatus(BUSY);
        
        controlBuffer.clear();
        log.debug("Sending command: "
                + cmd.toString().substring(0, cmd.toString().length() - 2));
        controlBuffer.put(cmd.toString());
        controlBuffer.flip();
        socketProvider.write(encoder.encode(controlBuffer));
        controlBuffer.clear();
        
        Reply reply = null;
        try {
        	reply = ReplyWorker.readReply(socketProvider);
        }catch(IOException ioe) {
            setConnectionStatus(ERROR);
            disconnect();
        	throw ioe;
        }
        
        if (getConnectionStatusLock() == CSL_DIRECT_CALL)
            setConnectionStatus(IDLE);
        
        if (reply != null)
            fireReplyMessageArrived(new FTPEvent(this, getConnectionStatus(), reply));
        return reply;
    }

    /**
     * 
     * This method is used to get the status of your connection
     * 
     * @return status there are constants in FTPConnection (f.e. CONNECTED /
     *         DISCONNECTED / IDLE ...) where you can identify the status of
     *         your ftp connection
     * @author arnold,kurt
     */
    public int getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * 
     * This method is used to set the status of your connection
     * 
     * @return status there are constants in FTPConnection (f.e. CONNECTED /
     *         DISCONNECTED / IDLE ...) where you can identify the status of
     *         your ftp connection
     * @author arnold,kurt
     */
    protected void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
        fireConnectionStatusChanged(new FTPEvent(this,connectionStatus));
    }

    /**
     * 
     * This method is used to set the status of your connection
     * 
     * @return status there are constants in FTPConnection (f.e. CONNECTED /
     *         DISCONNECTED / IDLE ...) where you can identify the status of
     *         your ftp connection
     * @author arnold,kurt
     */
    protected void setConnectionStatus(int connectionStatus, FTPFile fromFile, FTPFile toFile) {
        this.connectionStatus = connectionStatus;
        fireConnectionStatusChanged(new FTPEvent(this,connectionStatus, fromFile, toFile));
    }
    
    /**
     * 
     * This method is used to handle who sets the status
     * 
     * @return CSL_DIRECT_CALL (0)
     *         CSL_INDIRECT_CALL (1)
     *         CSL_2WAY_INDIRECT_CALL (2)
     * @author arnold,kurt
     */
    public int getConnectionStatusLock() {
        return connectionStatusLock;
    }

    /**
     * 
     * This method is used to handle who sets the status. This should avoid sending too much events if they are not needed.
     *
     * @param  CSL_DIRECT_CALL (0)
     *         CSL_INDIRECT_CALL (1)
     *         CSL_2WAY_INDIRECT_CALL (2)
     * @author arnold,kurt
     */
    protected void setConnectionStatusLock(int connectionStatusLock) {
        this.connectionStatusLock = connectionStatusLock;
    }
    
    /**
     * This method is used initaly to set the connection timeout. normal you
     * would set it to 10000 (10 sec.). if you have very slow servers try to set
     * it higher.
     * 
     * @param millis
     *            the milliseconds before a timeout will close the connection
     * @author arnold,kurt
     */
    protected void setTimeout(int millis) {
        this.timeout = millis;
    }

    /**
     * This method is used initaly to get the connection timeout. normal you
     * would set it to 10000 (10 sec.). if you have very slow servers try to set
     * it higher.
     * 
     * @param millis
     *            the milliseconds before a timeout will close the connection
     * @author arnold,kurt
     */
    protected int getTimeout() {
        return timeout;
    }

    /**
     * This method is used to change the working directory. it implements the
     * CWD ftp command
     * 
     * @param directory
     *            a string represanting the new working directory
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void changeDirectory(String directory) throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.CWD, directory);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
        workingDirectory = null;
    }

    /**
     * This method is used to get the working directory. it implements the PWD
     * ftp command
     * 
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws UnkownReplyStateException
     *             this indicates if the response from the server for the
     *             specific commando is not right (f.e. more than one reply line
     *             but only one expected)
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public String getWorkDirectory() throws IOException,
            UnkownReplyStateException, FtpWorkflowException, FtpIOException {
        if (workingDirectory == null) {
            Command command = new Command(Command.PWD);
            Reply reply = sendCommand(command);
            reply.dumpReply();
            reply.validate();
            workingDirectory = ReplyFormatter.parsePWDReply(reply);
        }
        return workingDirectory;
    }

    /**
     * This method is used to change to the parent directory. it implements the
     * CDUP ftp command
     * 
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void changeToParentDirectory() throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.CDUP);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
        workingDirectory = null;
    }

    /**
     * This method is used to create a new directory. it implements the MKD ftp
     * command
     * 
     * @param pathname
     *            a string represanting the directory to create
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void makeDirectory(String pathname) throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.MKD, pathname);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * This method is used to remove a specific directory. it implements the RMD
     * ftp command
     * 
     * @param pathname
     *            a string represanting the directory to remove
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void removeDirectory(String pathname) throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.RMD, pathname);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * This method is used to send a noop comand to the server i.g. for keep
     * alive purpose. it implements the NOOP ftp command
     * 
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void noOperation() throws IOException, FtpWorkflowException,
            FtpIOException {
        Command command = new Command(Command.NOOP);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * This method is used to go into passive mode. it implements the PASV ftp
     * command
     * 
     * @author arnold,kurt
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public InetSocketAddress sendPassiveMode() throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.PASV);
        try {
            Reply reply = sendCommand(command);
            reply.dumpReply();
            reply.validate();
            return ReplyFormatter.parsePASVCommand(reply);
        } catch (UnkownReplyStateException urse) {
            log.error("The state of the reply from pasv command is unknown!",
                    urse);
        }
        return null;
    }

    /**
     * This method is used initaly to set if passive mode should be used.
     * Default it is false
     * 
     * @param passive
     *            if true it will use passive mode
     * @author arnold,kurt
     */
    public void setPassiveMode(boolean mode) {
        this.passiveMode = mode;
    }

    /**
     * This method is used initaly to set if passive mode should be used.
     * Default it is false
     * 
     * @param passive
     *            if true it will use passive mode
     * @author arnold,kurt
     */
    public boolean isPassiveMode() {
        return passiveMode;
    }

    /**
     * This method is used to get a directory listing from the current working
     * directory
     * 
     * @return List of FTPFiles
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */

    public List getDirectoryListing() throws IOException,
            FtpWorkflowException, FtpIOException {
        return getDirectoryListing(getWorkDirectory());
    }
    
    /**
     * This method is used to get a directory listing from the current working
     * directory via STAT -LA command
     * 
     * @return List of FTPFiles
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */

    public List getFastDirectoryListing() throws IOException,
            FtpWorkflowException, FtpIOException {
        String workDirectory = getWorkDirectory();
        Command command = new Command(Command.STAT, "-LA");
        Reply reply = sendCommand(command);
        reply.validate();
        return factory.parse(reply.getLines(), workDirectory);
    }
    
    /**
     * This method is used to get a directory listing from the current working
     * directory via STAT -LA command
     * 
     * @return List of FTPFiles
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public List getFastDirectoryListing(String directory) throws IOException,
            FtpWorkflowException, FtpIOException {
        Command command = new Command(Command.STAT, "-LA " + directory);
        Reply reply = sendCommand(command);
        reply.validate();
        return factory.parse(reply.getLines(), directory);
    }

    /**
     * This method is used to get a directory listing from the specified
     * directory
     * 
     * @return List of FTPFiles
     * @param directory
     * 			   The directory where a LIST should be done
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */

    public List getDirectoryListing(String directory)
            throws IOException, FtpWorkflowException, FtpIOException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(FTPConnection.BUSY);
        
        ListCommand command = new ListCommand(directory);
        SocketProvider provider = null;
        String workDirectory = getWorkDirectory();
        if (getConnectionType() == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || getConnectionType() == FTPConnection.AUTH_TLS_FTP_CONNECTION) {
            Command pbsz = new Command(Command.PBSZ, "0");
            (sendCommand(pbsz)).dumpReply();
            Command prot = new Command(Command.PROT, "P");
            (sendCommand(prot)).dumpReply();
        }
        Reply commandReply = new Reply();
        if (isPassiveMode()) {
            provider = initDataSocket(command, commandReply);
        } else {
            provider = sendPortCommand(command, commandReply);
        }

        command.setDataSocket(provider);
        // INFO response from ControllConnection is ignored
        List parsedList = null;
        try {
        	parsedList = factory.parse(command
                .fetchDataConnectionReply().getLines(), workDirectory);
        }catch(IOException ioe) {
            setConnectionStatus(ERROR);
            disconnect();
        	throw ioe;
        }
        if (commandReply.getLines().size() == 1) {
        	try {
        		(ReplyWorker.readReply(socketProvider)).dumpReply();
            }catch(IOException ioe) {
                setConnectionStatus(ERROR);
                disconnect();
                throw ioe;
            }
        }
        
        setConnectionStatus(FTPConnection.IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
        
        return parsedList;
    }

    /**
     * This method is used to tell the server that we want to go in PORT mode
     * (means that we tell the server he should open a connection to a port)
     * 
     * @return SocketProvider with the established connection
     * @param The
     *            command that will follow after establishing the connection
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public SocketProvider sendPortCommand(Command command, Reply commandReply)
            throws IOException, FtpWorkflowException, FtpIOException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(FTPConnection.BUSY);
        
        SocketProvider provider = null;
        
        if (getProxy() == null) {
            ServerSocket server = ServerSocketFactory.getDefault()
                .createServerSocket();
            InetSocketAddress isa = new InetSocketAddress(socketProvider.socket()
                .getLocalAddress(), 0);
        
            server.bind(isa);
            int port = server.getLocalPort();

            StringBuffer modifiedHost = new StringBuffer();
            modifiedHost.append(server.getInetAddress().getHostAddress().replace(
                '.', ','));
            modifiedHost.append(",");
            modifiedHost.append(port >> 8);
            modifiedHost.append(",");
            modifiedHost.append(port & 0x00ff);

            Command portCommand = new Command(Command.PORT, modifiedHost.toString());
            Reply portReply = sendCommand(portCommand);
            portReply.dumpReply();
            portReply.validate();
            commandReply.setLines(sendCommand(command).getLines());
            commandReply.dumpReply();
            commandReply.validate();
            provider = new SocketProvider(server.accept(), false,
                getDownloadBandwidth(), getUploadBandwidth());
        } else {
            InetSocketAddress portSocketAddress = new InetSocketAddress(getAddress().getAddress(), 0);
            Socket proxySocket = getProxy().bind(portSocketAddress);
            
            int port = getProxy().getBindAddress().getPort();

            StringBuffer modifiedHost = new StringBuffer();
            modifiedHost.append(getProxy().getProxyAddress().getAddress().getHostAddress().replace(
                '.', ','));
            modifiedHost.append(",");
            modifiedHost.append(port >> 8);
            modifiedHost.append(",");
            modifiedHost.append(port & 0x00ff);

            Command portCommand = new Command(Command.PORT, modifiedHost.toString());
            Reply portReply = sendCommand(portCommand);
            portReply.dumpReply();
            portReply.validate();
            commandReply.setLines(sendCommand(command).getLines());
            commandReply.dumpReply();
            commandReply.validate();
            
            provider = new SocketProvider(proxySocket, false, getDownloadBandwidth(), getUploadBandwidth());            
        }
            
        provider.socket().setReceiveBufferSize(65536);
        provider.socket().setSendBufferSize(65536);
        provider.setSSLMode(getConnectionType());
        if (connectionType == FTPConnection.AUTH_TLS_FTP_CONNECTION
                || connectionType == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION)
            provider.negotiate(this.getTrustManagers(),this.getKeyManagers());
        
        setConnectionStatus(FTPConnection.IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
        
        return provider;

    }

    /**
     * This method is used to download a file from the server to a specifed
     * local file object
     * 
     * @param fromFile
     *            the file on the server
     * @param toFile
     *            the file object where the file should be stored (on the local
     *            computer)
     * @param resume
     * 			  set to true if you want the methode to look for an existing file on the server
     *            and if size differes, start a resume transfer
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FtpFileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             server
     */
    
    public void downloadFile(FTPFile fromFile, FTPFile toFile)
    throws IOException, FtpWorkflowException, FtpIOException {
    	downloadFile(fromFile,toFile,false);
    }
    
    public void downloadFile(FTPFile fromFile, FTPFile toFile,boolean resume)
            throws IOException, FtpWorkflowException, FtpIOException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(RECEIVING_FILE_STARTED, fromFile, toFile);
        setConnectionStatus(RECEIVING_FILE);

        RetrieveCommand command = new RetrieveCommand(Command.RETR, fromFile,
                toFile);
        SocketProvider provider = null;

        if (getConnectionType() == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || getConnectionType() == FTPConnection.AUTH_TLS_FTP_CONNECTION) {
            Command pbsz = new Command(Command.PBSZ, "0");
            (sendCommand(pbsz)).dumpReply();
            Command prot = new Command(Command.PROT, "P");
            (sendCommand(prot)).dumpReply();
        }
        if(resume || tryResume)
        {
        	if(fromFile.getSize() <= 0)
        	{
        		List list = getDirectoryListing(fromFile.getPath());
        		for(Iterator it = list.iterator(); it.hasNext();)
        		{
        			FTPFile file = (FTPFile)it.next();
        			if(file.getName().equals(fromFile.getName()))
        				fromFile.setSize(file.getSize());
        		}
        	}
        	if(fromFile.getSize() != toFile.getSize())
        	{
        		command.setResumePosition(toFile.getSize());
        		Command resumeCommand = new Command(Command.REST,""+toFile.getSize());
        		Reply resumeReply = sendCommand(resumeCommand);
        		resumeReply.dumpReply();
        		try
        		{
        			resumeReply.validate();
        		}catch(FtpWorkflowException fwe)
        		{
        			log.error("Couldn't resume file, error was: " + fwe.getMessage());
        		}
        		catch(FtpIOException fioe)
        		{
        			log.error("Couldn't resume file, error was: " + fioe.getMessage());
        		}
        	}else
                return;
        }
        //Send TYPE I
        Command commandType = new Command(connectionTransferType);
        (sendCommand(commandType)).dumpReply();
        Reply commandReply = new Reply();
        if (isPassiveMode()) {
            provider = initDataSocket(command, commandReply);
        } else {
            provider = sendPortCommand(command, commandReply);
        }

        command.setDataSocket(provider);
        // INFO response from ControllConnection is ignored
        try {
        	command.fetchDataConnectionReply();
        }catch(IOException ioe) {
            setConnectionStatus(ERROR);
            disconnect();
        	throw ioe;
        }
        if (commandReply.getLines().size() == 1) {
        	try {
        		(ReplyWorker.readReply(socketProvider)).dumpReply();
            }catch(IOException ioe) {
                setConnectionStatus(ERROR);
                disconnect();
            	throw ioe;
            }
        }

        setConnectionStatus(RECEIVING_FILE_ENDED, fromFile, toFile);
        setConnectionStatus(IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);        
    }

    public InputStream downloadStream(FTPFile fromFile) throws IOException, FtpWorkflowException, FtpIOException {
        
        PipedInputStream pis = new PipedInputStream();
        
        class DownStreamingThread extends Thread {
            FTPConnection connection;
            FTPFile fromFile;
            PipedInputStream pis;
            
            public DownStreamingThread(FTPConnection connection, FTPFile fromFile, PipedInputStream pis) {
                super();
                this.connection = connection;
                this.fromFile = fromFile;
                this.pis = pis;
            }

            public void run() {
                try {
                    connection.streamFile(fromFile, pis);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        (new DownStreamingThread(this, fromFile, pis)).start();
        
        // ensure that in/out pipes are connected already
        try {
        	for(int i=0; i<60 && pis.available() <= 0; i++)
        		Thread.sleep(1000);
        }catch(Exception e) {}
        
        return pis;
    }
    
    
    private void streamFile(FTPFile fromFile, PipedInputStream pis) throws IOException, FtpWorkflowException, FtpIOException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(RECEIVING_FILE_STARTED, fromFile, null);
        setConnectionStatus(RECEIVING_FILE);

        RetrieveCommand command = new RetrieveCommand(Command.RETR, fromFile, pis);
        SocketProvider provider = null;

        if (getConnectionType() == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || getConnectionType() == FTPConnection.AUTH_TLS_FTP_CONNECTION) {
            Command pbsz = new Command(Command.PBSZ, "0");
            (sendCommand(pbsz)).dumpReply();
            Command prot = new Command(Command.PROT, "P");
            (sendCommand(prot)).dumpReply();
        }
        
        // Send TYPE I
        Command commandType = new Command(connectionTransferType);
        (sendCommand(commandType)).dumpReply();
        Reply commandReply = new Reply();
        if (isPassiveMode()) {
            provider = initDataSocket(command, commandReply);
        } else {
            provider = sendPortCommand(command, commandReply);
        }

        command.setDataSocket(provider);
        // INFO response from ControllConnection is ignored
        try {
        	command.fetchDataConnectionReply(RetrieveCommand.STREAM_BASED);
        }catch(IOException ioe) {
            setConnectionStatus(ERROR);
            disconnect();
        	throw ioe;
        }
        if (commandReply.getLines().size() == 1) {
        	try {
        		(ReplyWorker.readReply(socketProvider)).dumpReply();
            }catch(IOException ioe) {
                setConnectionStatus(ERROR);
                disconnect();
            	throw ioe;
            }
        }

        setConnectionStatus(RECEIVING_FILE_ENDED, fromFile, null);
        setConnectionStatus(IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
    }
    
    /**
     * This method is used to download a directory from the server to a specifed
     * local directory object
     * 
     * @param srcDir
     *            the directory on the server
     * @param dstDir
     *            the directory object where the file should be stored (on the
     *            local computer)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FtpFileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             server
     */
    public void downloadDirectory(FTPFile srcDir, FTPFile dstDir)
            throws IOException, FtpWorkflowException, FtpIOException {
        if (!srcDir.isDirectory())
            throw new FtpFileNotFoundException("Downloading: "
                    + srcDir.getName()
                    + " is not possible, it's not a directory!");

        new File(dstDir.toString()).mkdir();

        
        String listDir = srcDir.toString();
        listDir = (listDir.endsWith("/") ? listDir.substring(0, listDir
                .length() - 1) : listDir);
        List files = getDirectoryListing(listDir);
//        List<FTPFile> files = getDirectoryListing(srcDir.toString());

        Collections.sort(files);

        for (Iterator it = files.iterator(); it.hasNext();) {
            FTPFile file = (FTPFile)it.next();
            file.setPath(srcDir.toString());
            if (!file.isDirectory()) {
                downloadFile(file, new FTPFile(dstDir.toString(), file.getName(), false));
            } else {
                downloadDirectory(file, new FTPFile(dstDir.toString(), file
                        .getName(), true));
            }
        }
    }

    /**
     * This method is used to upload a file to the server to a specifed FtpFile
     * 
     * @param fromFile
     *            the file on the local computer
     * @param toFile
     *            the file object where the file should be stored (on the
     *            server)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             local computer
     */
    
    
    public void uploadFile(FTPFile fromFile, FTPFile toFile)
    throws IOException, FtpWorkflowException, FtpIOException {
    	uploadFile(fromFile,toFile,false);
    }
    
    public void uploadFile(FTPFile fromFile, FTPFile toFile, boolean resume)
    throws IOException, FtpWorkflowException, FtpIOException {
        upload(fromFile, toFile, resume);
    }
    
    public void uploadStream(InputStream upStream, FTPFile toFile) throws IOException, FtpWorkflowException, FtpIOException {
        upload(upStream, toFile, false);
    }
    private void upload(Object upSrc, FTPFile toFile, boolean resume)
			throws IOException, FtpWorkflowException, FtpIOException {

		FTPFile srcInfo = null;
		if (upSrc instanceof FTPFile)
			srcInfo = (FTPFile) upSrc;
		else if (upSrc instanceof InputStream && resume) {
			resume = false;
			log.warn("No resume possible on uploadStream");
		}

		setConnectionStatusLock(CSL_INDIRECT_CALL);
		setConnectionStatus(SENDING_FILE_STARTED, srcInfo, toFile);
		setConnectionStatus(SENDING_FILE);

		StoreCommand command = new StoreCommand(Command.STOR, upSrc, toFile);
		SocketProvider provider = null;

		if (getConnectionType() == FTPConnection.AUTH_SSL_FTP_CONNECTION
				|| getConnectionType() == FTPConnection.AUTH_TLS_FTP_CONNECTION) {
			Command pbsz = new Command(Command.PBSZ, "0");
			(sendCommand(pbsz)).dumpReply();
			Command prot = new Command(Command.PROT, "P");
			(sendCommand(prot)).dumpReply();
		}
		if (resume || tryResume) {
			if (toFile.getSize() <= 0) {
				List list = getDirectoryListing(toFile.getPath());

				for (Iterator it = list.iterator(); it.hasNext();) {
					FTPFile file = (FTPFile) it.next();
					if (file.getName().equals(toFile.getName()))
						toFile.setSize(file.getSize());
				}
			}
			if (srcInfo.getSize() != toFile.getSize()) {
				command.setResumePosition(toFile.getSize());
				Command resumeCommand = new Command(Command.REST, ""
						+ toFile.getSize());
				Reply resumeReply = sendCommand(resumeCommand);
				resumeReply.dumpReply();
				try {
					resumeReply.validate();
				} catch (FtpWorkflowException fwe) {
					log.error("Couldn't resume file, error was: "
							+ fwe.getMessage());
				} catch (FtpIOException fioe) {
					log.error("Couldn't resume file, error was: "
							+ fioe.getMessage());
				}
			} else
				return;
		}
		// Send TYPE I
		Command commandType = new Command(connectionTransferType);
		(sendCommand(commandType)).dumpReply();

		Reply commandReply = new Reply();
		if (isPassiveMode()) {
			provider = initDataSocket(command, commandReply);
		} else {
			provider = sendPortCommand(command, commandReply);
		}

		command.setDataSocket(provider);
		// INFO response from ControllConnection is ignored
		try {
			command.fetchDataConnectionReply();
		} catch (IOException ioe) {
			setConnectionStatus(ERROR);
			disconnect();
			throw ioe;
		}
		if (commandReply.getLines().size() == 1) {
			try {
				(ReplyWorker.readReply(socketProvider)).dumpReply();
			} catch (IOException ioe) {
				setConnectionStatus(ERROR);
				disconnect();
				throw ioe;
			}
		}

		setConnectionStatus(SENDING_FILE_ENDED, srcInfo, toFile);
		setConnectionStatus(IDLE);
		setConnectionStatusLock(CSL_DIRECT_CALL);
	}

    /**
     * This method is used to upload a local directory to the server to a
     * specifed remote directory
     * 
     * @param srcDir
     *            the directory on the local computer
     * @param dstDir
     *            the directory object where the file should be stored (on the
     *            server)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             local computer
     */
    public void uploadDirectory(FTPFile srcDir, FTPFile dstDir)
            throws IOException, FtpWorkflowException, FtpIOException {
        if (!srcDir.isDirectory())
            throw new FtpFileNotFoundException("Uploading: " + srcDir.getName()
                    + " is not possible, it's not a directory!");

        makeDirectory(dstDir.toString());

        File[] files = srcDir.getFile().listFiles();
        List ftpFiles = new ArrayList();

        for (int i = 0; i < files.length; i++)
        {
            ftpFiles.add(new FTPFile(files[i]));
        }

        Collections.sort(ftpFiles);

        for (Iterator it = ftpFiles.iterator(); it.hasNext(); ) {
            FTPFile file = (FTPFile)it.next();
            if (!file.isDirectory()) {
                uploadFile(file, new FTPFile(dstDir.toString(), file.getName()));
            } else {
                uploadDirectory(file, new FTPFile(dstDir.toString(), file
                        .getName(), true));
            }
        }
    }

    /**
     * This method is used to fxp a directory from the server to a specifed
     * server directory object
     * 
     * @param destination
     *            the FTPConnection Object to the remote server
     * @param srcDir
     *            the directory on the server
     * @param dstDir
     *            the directory object where the file should be stored (on the
     *            remote server)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FtpFileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             server
     */
    public void fxpFile(FTPConnection destination, FTPFile fromFile,
            FTPFile toFile) throws IOException, FtpWorkflowException,
            FtpIOException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(FXPING_FILE_STARTED, fromFile, toFile);
        setConnectionStatus(FXP_FILE);
        // Need to send PRET (RETR/STOR) before PASV
        if (isPretSupport()) {
			Command pretCommand = new Command(Command.PRET, new String[] {
					Command.RETR,fromFile.toString()
			}
			);
			Reply pretReply = sendCommand(pretCommand);
			pretReply.dumpReply();
			pretReply.validate();
		}
		if (destination.isPretSupport()) {
			Command pretDestCommand = new Command(Command.PRET, new String[] {
					Command.STOR,fromFile.toString()
			}
			);
			Reply pretDestReply = destination.sendCommand(pretDestCommand);
			pretDestReply.dumpReply();
			pretDestReply.validate();
		}
        Command pasvCommand = new Command(Command.PASV);
        if (getSecureFXPType() == Command.SSCN && !sscnActive) {
            setSSCNFxp(true);
            sscnActive = true;
            pasvCommand = new Command(Command.PASV);
        }
        else if(getSecureFXPType() == Command.CPSV)
        {
        	pasvCommand = new Command(Command.CPSV);
        }

        // send PASV to source site
        Reply pasvReply = sendCommand(pasvCommand);
        pasvReply.dumpReply();
        pasvReply.validate();

        // parse the host and port from reply
        List lines = pasvReply.getLines();
        if (lines.size() != 1)
            throw new UnkownReplyStateException(
                    "PASV Reply has to have a size of 1 entry but it has: "
                            + lines.size());
        String line = (String)lines.get(0);
        line = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));

        // send PROT P to source and destination, needed for CPSV only
        if (getSecureFXPType() == Command.CPSV) {
            // destination
            Command protCommand = new Command(Command.PROT, "P");
            Reply protReply = destination.sendCommand(protCommand);
            protReply.dumpReply();
            protReply.validate();

            // source
            protReply = sendCommand(protCommand);
            protReply.dumpReply();
            protReply.validate();
        }
        
        // send PORT to destination site
        Command portCommand = new Command(Command.PORT, line);
        Reply portReply = destination.sendCommand(portCommand);
        portReply.dumpReply();
        portReply.validate();

        // send STOR command to destination site
        Command storeCommand = new Command(Command.STOR, toFile.toString());
        Reply storeReply = destination.sendCommand(storeCommand);
        storeReply.dumpReply();
        storeReply.validate();

        // send RETR command to source site
        Command retrCommand = new Command(Command.RETR, fromFile.toString());
        Reply retrReply = sendCommand(retrCommand);
        retrReply.dumpReply();
        retrReply.validate();

        // read the last control reply from source
        if (retrReply.getLines().size() == 1) {
        	try {
        		(ReplyWorker.readReply(socketProvider)).dumpReply();
            }catch(IOException ioe) {
                setConnectionStatus(ERROR);
                disconnect();
            	throw ioe;
            }
        }
        
        // read the last control reply from destination
        if (storeReply.getLines().size() == 1) {
            try {
                (ReplyWorker.readReply(destination.socketProvider)).dumpReply();
            }catch(IOException ioe) {
                destination.setConnectionStatus(ERROR);
                destination.disconnect();
                throw ioe;
            }
        }
        if (getSecureFXPType() == Command.CPSV) {
            // destination
            Command protCommand = new Command(Command.PROT, "O");
            Reply protReply = destination.sendCommand(protCommand);
            protReply.dumpReply();
            protReply.validate();

            // source
            protReply = sendCommand(protCommand);
            protReply.dumpReply();
            protReply.validate();
        }
        if (getSecureFXPType() == Command.SSCN && sscnActive) {

            setSSCNFxp(false);
            sscnActive = false;
        }

        setConnectionStatus(FXPING_FILE_ENDED, fromFile, toFile);
        setConnectionStatus(IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
    }

    /**
     * This method is used to fxp a directory from the server to a specifed
     * server directory object
     * 
     * @param destination
     *            the FTPConnection Object to the remote server
     * @param srcDir
     *            the directory on the server
     * @param dstDir
     *            the directory object where the file should be stored (on the
     *            remote server)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     * @throws FtpFileNotFountException
     *             will be thrown if the specified fromFile is not found on the
     *             server
     */
    public void fxpDirectory(FTPConnection destination, FTPFile srcDir,
            FTPFile dstDir) throws IOException, FtpWorkflowException,
            FtpIOException {
        if (!srcDir.isDirectory())
            throw new FtpFileNotFoundException("Downloading: "
                    + srcDir.getName()
                    + " is not possible, it's not a directory!");

        if (getSecureFXPType() == Command.SSCN && !sscnActive) {
            setSSCNFxp(true);
            sscnActive = true;
        }
   
        destination.makeDirectory(dstDir.toString());

        String listDir = srcDir.toString();
        listDir = (listDir.endsWith("/") ? listDir.substring(0, listDir
                .length() - 1) : listDir);
        List files = getDirectoryListing(listDir);

        Collections.sort(files);

        for (Iterator it = files.iterator(); it.hasNext(); ) {
            FTPFile file = (FTPFile)it.next();
            file.setPath(srcDir.toString());
            if (!file.isDirectory()) {
                fxpFile(destination, file, new FTPFile(dstDir.toString()
                        , file.getName()));
            } else {
                fxpDirectory(destination, file, new FTPFile(dstDir.toString(),
                        file.getName(), true));
            }
        }

        if (getSecureFXPType() == Command.SSCN && sscnActive) {
            setSSCNFxp(false);
            sscnActive = false;
        }
    }

    /**
     * This method is used to init the data socket
     * 
     * @param command
     *            the command you want to send (LIST,RETR,....)
     * @param commmandReply
     *            the reply from the server will be stored in this object
     * @return the new established socket provider
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    private SocketProvider initDataSocket(Command command, Reply commandReply)
            throws IOException, FtpIOException, FtpWorkflowException {
    	 
    	setConnectionStatusLock(CSL_INDIRECT_CALL);
         setConnectionStatus(FTPConnection.BUSY);
    	
    	if(isPretSupport())
    	{
    		Command pretCommand = new Command(Command.PRET,command.toString());
    		sendCommand(pretCommand).dumpReply();
    	}
        InetSocketAddress dataSocket = sendPassiveMode();
        SocketProvider provider = new SocketProvider(false);
        provider.connect(dataSocket, getProxy(), getDownloadBandwidth(),
                getUploadBandwidth());
        provider.setSSLMode(getConnectionType());

        commandReply.setLines(sendCommand(command).getLines());
        commandReply.dumpReply();
        commandReply.validate();

        if (connectionType == FTPConnection.AUTH_TLS_FTP_CONNECTION
                || connectionType == FTPConnection.AUTH_SSL_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_SSL_WITH_CRYPTED_DATA_FTP_CONNECTION
                || connectionType == FTPConnection.IMPLICIT_TLS_WITH_CRYPTED_DATA_FTP_CONNECTION)
            provider.negotiate(this.getTrustManagers(),this.getKeyManagers());
        
        setConnectionStatus(FTPConnection.IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
        
        return provider;
    }

    /**
     * @return Returns the connectionType.
     */
    public int getConnectionType() {
        return connectionType;
    }

    /**
     * @param connectionType
     *            The connectionType to set (FTP_CONNECTION,AUTH_SSL_FTP_CONNETION,...)
     */
    protected void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

 /**
     * This method is used to send a SITE command to the ftp server
     * 
     * @param commandParameter
     *            the argument you want to pass to the site command (f.e.: commandParameter=HELP -> SITE HELP)
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void sendSiteCommand(String commandParameter) throws IOException,
            FtpIOException, FtpWorkflowException {
        Command command = new Command(Command.SITE, commandParameter);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * @return Returns the downloadBandwidth.
     */
    protected int getDownloadBandwidth() {
        return downloadBandwidth;
    }

    /**
     * @param downloadBandwidth
     *            The downloadBandwidth to set.
     */
    protected void setDownloadBandwidth(int maxDownloadBandwidth) {
        this.downloadBandwidth = maxDownloadBandwidth;
    }

    /**
     * @return Returns the uploadBandwidth.
     */
    protected int getUploadBandwidth() {
        return uploadBandwidth;
    }

    /**
     * @param uploadBandwidth
     *            The uploadBandwidth to set.
     */
    protected void setUploadBandwidth(int maxUploadBandwidth) {
        this.uploadBandwidth = maxUploadBandwidth;
    }

    // listenerList methods
    private boolean isListener(Class c, FTPListener f) {
        boolean isListener = false;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == c && listeners[i + 1] == f) {
                isListener = true;
            }
        }
        return isListener;
    }

    /**
     * Adds a <code>FTPStatusListener</code> to the connection.
     * 
     * @param l
     *            the listener to be added
     */
    public void addFTPStatusListener(FTPListener l) {
        listenerList.add(FTPListener.class, l);
    }

    /**
     * Removes a FTPStatusListener from the connection.
     * 
     * @param l
     *            the listener to be removed
     */
    public void removeFTPStatusListener(FTPListener l) {
        listenerList.remove(FTPListener.class, l);
    }

    /**
     * Returns an array of all the <code>FTPStatusListener</code>s added to
     * this FTPConnection with addFTPStatusListener().
     * 
     * @return all of the <code>FTPStatusListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public FTPListener[] getFTPStatusListeners() {
        return (FTPListener[]) (listenerList.getListeners(FTPListener.class));
    }

    /**
     * Notifies all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created.
     * 
     * @param event
     *            The FTPEvent holding the connection status
     * @see EventListenerList
     */
    protected void fireConnectionStatusChanged(FTPEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FTPListener.class) {
                if (event == null)
                    event = new FTPEvent(this, getConnectionStatus(), null);
                ((FTPListener) listeners[i + 1]).connectionStatusChanged(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created.
     * 
     * @param event
     *            The FTPEvent holding the connection status
     * @see EventListenerList
     */
    protected void fireReplyMessageArrived(FTPEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FTPListener.class) {
                if (event == null)
                    event = new FTPEvent(this, getConnectionStatus(), null);
                ((FTPListener) listeners[i + 1]).replyMessageArrived(event);
            }
        }
    }

    /**
     * @return Returns the secureFXPType.
     */
    public String getSecureFXPType() {
        return secureFXPType;
    }

    /**
     * @param secureFXPType
     *            The secureFXPType to set.
     *            Possible values:	null -> no secure fxp
     *            					Command.SSCN -> SSCN fxp
     *            					Command.CPSV -> CPSV fxp
     */
    public void setSecureFXPType(String secureFXPType) {
        this.secureFXPType = secureFXPType;
    }

    /**
     * This method is used send the SSCN command (if SSCN is send and both servers support that a fxp will be done with crypted data channels)
     * 
     * @param active
     *            activated/deactivate SSCN
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     * @throws FtpWorkflowException
     *             will be thrown if there was a ftp reply class 5xx. in most
     *             cases wrong commands where send
     * @throws FtpIOException
     *             will be thrown if there was a ftp reply class 4xx. this
     *             should indicate some secific problems on the server
     */
    public void setSSCNFxp(boolean active) throws IOException,
            FtpIOException, FtpWorkflowException {
        Command command = null;

        if (active)
            command = new Command(Command.SSCN, Command.SSCN_ON);
        else
            command = new Command(Command.SSCN, Command.SSCN_OFF);

        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * This method is used indicate if SSCN is on or off
     * 
     * @return true if SSCN is on, false if SSCN is off
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     */
    public boolean isSecuredFxp() throws IOException {
        Command command = new Command(Command.SSCN);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        if(((String)reply.getLines().get(0)).indexOf("CLIENT") >= 0)
        	return true;
        return false;
    }

    /**
     * public void renameFile(FTPFile fromName, FTPFile toName)
     * 
     * renames a given FTPFile object to the given new name
     * 
     * @param fromName
     *            the file object to rename
     * @param toName
     *            the new name for the given FTPFile object
     * @throws IOException
     * @throws FtpIOException
     * @throws FtpWorkflowException
     */
    public void renameFile(FTPFile fromName, FTPFile toName) throws IOException,
            FtpIOException, FtpWorkflowException {
        
        setConnectionStatusLock(CSL_INDIRECT_CALL);
        setConnectionStatus(FTPConnection.BUSY);
        
        // send RNFR
        Command commandRenFr = new Command(Command.RNFR, fromName.toString());
        Reply replyRenFr = sendCommand(commandRenFr);
        replyRenFr.dumpReply();
        replyRenFr.validate();

        // send RNTO
        Command commandRenTo = new Command(Command.RNTO, toName.toString());
        Reply replyRenTo = sendCommand(commandRenTo);
        replyRenTo.dumpReply();
        replyRenTo.validate();
        
        setConnectionStatus(FTPConnection.IDLE);
        setConnectionStatusLock(CSL_DIRECT_CALL);
    }

    /**
     * public void deleteFile(FTPFile file)
     * 
     * deletes the given file on the server
     * 
     * @param file
     *            the file to delete
     * @throws IOException
     * @throws FtpIOException
     * @throws FtpWorkflowException
     */
    public void deleteFile(FTPFile file) throws IOException, FtpIOException,
            FtpWorkflowException {
        Command command = new Command(Command.DELE, file.toString());
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * public void deleteDirectory(FTPFile directory)
     * 
     * deletes the given directory recursivly, means the directory and all
     * subdirectories will be removed and all files deleted
     * 
     * @param directory
     * @throws IOException
     * @throws FtpIOException
     * @throws FtpWorkflowException
     */
    public void deleteDirectory(FTPFile directory) throws IOException,
            FtpIOException, FtpWorkflowException {
        if (!directory.isDirectory())
            throw new FtpFileNotFoundException("Deleting: "
                    + directory.getName()
                    + " is not possible, it's not a directory!");

        List files = getDirectoryListing(directory.toString());

        Collections.sort(files);

        for (Iterator it = files.iterator(); it.hasNext();) {
            FTPFile file = (FTPFile)it.next();
            file.setPath(directory.toString());
            if (!file.isDirectory()) {
                deleteFile(file);
            } else {
                deleteDirectory(file);
                // removeDirectory( file );
            }
        }

        removeDirectory(directory);
    }

    /**
     * public void removeDirectory(FTPFile directory)
     * 
     * removes an empty directory
     * 
     * @param directory
     *            the directory to remove
     * @throws IOException
     * @throws FtpIOException
     * @throws FtpWorkflowException
     */
    public void removeDirectory(FTPFile directory) throws IOException,
            FtpIOException, FtpWorkflowException {
        Command command = new Command(Command.RMD, directory.toString());
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * public void setTransferType(boolean type)
     * 
     * this method is used to set the transfer type to binary (TYPE I) or asccii
     * (TYPE A).
     * 
     * @param type
     *            true: TYPE I false: TYPE A
     * @throws IOException
     * @throws FtpIOException
     * @throws FtpWorkflowException
     */
    public void setTransferType(boolean type) throws IOException,
            FtpIOException, FtpWorkflowException {
    	connectionTransferType = (type ? Command.TYPE_I : Command.TYPE_A);
        Command command = new Command(connectionTransferType);
        Reply reply = sendCommand(command);
        reply.dumpReply();
        reply.validate();
    }

    /**
     * @return Returns the proxy.
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * @param proxy
     *            The proxy to set.
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

/**
     * This method is used to send the SYST command (will be used to identify the LIST encoding)
     * 
     * @throws IOException
     *             will be thrown if there was a communication problem with the
     *             server
     */
    public void checkSystem() throws IOException, FtpIOException {
        try {
            Command system = new Command(Command.SYST);
            Reply reply = sendCommand(system);
            reply.dumpReply();
            reply.validate();
            factory = new FTPFileFactory((String)reply.getLines().get(0));
        } catch (FtpWorkflowException fwe) {
            // ignore
            factory = new FTPFileFactory("UNKNOWN");
        }
    }

    /**
     * @return Returns the trustManagers.
     */
    public TrustManager[] getTrustManagers() {
        return trustManagers;
    }

    /**
     * @param trustManagers The trustManagers to set.
     */
    public void setTrustManagers(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    /**
     * @return Returns the keyManagers.
     */
    public KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    /**
     * @param keyManagers The keyManagers to set.
     */
    public void setKeyManagers(KeyManager[] keyManagers) {
        this.keyManagers = keyManagers;
    }

	public boolean isTryResume() {
		return tryResume;
	}

	public void setTryResume(boolean tryResume) {
		this.tryResume = tryResume;
	}
	
	public Date getModificationTime(FTPFile file) throws IOException,FtpIOException,FtpWorkflowException,ParseException
	{
		Command mdtm = new Command(Command.MDTM,file.toString());
		Reply reply = sendCommand(mdtm);
		reply.dumpReply();
		reply.validate();
		return ReplyFormatter.parseMDTMReply(reply);
	}
	
	public void setModificationTime(FTPFile file,Date date) throws IOException,FtpIOException,FtpWorkflowException,ParseException
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String dateString = formatter.format(date);
		Command mdtm = new Command(Command.MDTM,dateString + " " + file.toString());
		Reply reply = sendCommand(mdtm);
		reply.dumpReply();
		reply.validate();
	}
	
	public void setClientName(String name) throws FtpIOException,FtpWorkflowException,IOException
	{
		Command clnt = new Command(Command.CLNT,name);
		Reply reply = sendCommand(clnt);
		reply.dumpReply();
		reply.validate();
	}
	
	public String getCRC(FTPFile file)  throws FtpIOException,FtpWorkflowException,IOException
	{
		return getCRC(file,0,-1);
	}
	
	public String getCRC(FTPFile file,long startposition, long endposition)  throws FtpIOException,FtpWorkflowException,IOException
	{
		Command crc = new Command(Command.XCRC,"\"" + file.toString() + "\" " + startposition+ " " + endposition);
		Reply reply = sendCommand(crc);
		reply.dumpReply();
		reply.validate();
		return ReplyFormatter.parseXCRCReply(reply);
	}
	
	public String getMD5(FTPFile file)  throws FtpIOException,FtpWorkflowException,IOException
	{
		return getMD5(file,0,-1);
	}
	
	public String getMD5(FTPFile file,long startposition, long endposition)  throws FtpIOException,FtpWorkflowException,IOException
	{
		Command md5 = new Command(Command.XMD5,"\"" + file.toString() + "\" " + startposition + " " + endposition);
		Reply reply = sendCommand(md5);
		reply.dumpReply();
		reply.validate();
		return ReplyFormatter.parseXMD5Reply(reply);
	}

	public boolean isPretSupport() {
		return pretSupport;
	}

	public void setPretSupport(boolean pretSupport) {
		this.pretSupport = pretSupport;
	}
	
	public void checkFeatures() throws IOException
	{
		Reply reply = sendCommand(new Command(Command.FEAT));
		reply.dumpReply();
		if (ReplyCode.isPositiveCompletionReply(reply)) {
			List lines = reply.getLines();
			for (Iterator it = lines.iterator(); it.hasNext(); ) {
				String s = (String)it.next();
				if (s.indexOf(Command.SSCN) > -1) {
					setSscnSupport(true);
					setSecureFXPType(Command.SSCN);
				} else if (s.indexOf(Command.PRET) > -1) {
					setPretSupport(true);
				} else if (s.indexOf(Command.CPSV) > -1)
				{
					setCpsvSupport(true);
					setSecureFXPType(Command.CPSV);
				}
			}

		}
	}

	public boolean isCpsvSupport() {
		return cpsvSupport;
	}

	public void setCpsvSupport(boolean cpsvSupport) {
		this.cpsvSupport = cpsvSupport;
	}

	public boolean isSscnSupport() {
		return sscnSupport;
	}

	public void setSscnSupport(boolean sscnSupport) {
		this.sscnSupport = sscnSupport;
	}
	
	public void moveFile(FTPFile fromFile,FTPFile toFile) throws FtpWorkflowException,FtpIOException,IOException
	{
		renameFile(fromFile, toFile);
	}
}
