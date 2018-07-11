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
package org.ftp4che.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.ftp4che.FTPConnection;
import org.ftp4che.commands.Command;
import org.ftp4che.event.FTPEvent;
import org.ftp4che.exception.AuthenticationNotSupportedException;
import org.ftp4che.exception.FtpIOException;
import org.ftp4che.exception.FtpWorkflowException;
import org.ftp4che.exception.NotConnectedException;
import org.ftp4che.io.ReplyWorker;
import org.ftp4che.io.SocketProvider;
import org.ftp4che.reply.Reply;
import org.ftp4che.reply.ReplyCode;

/**
 * @author arnold
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class NormalFTPConnection extends FTPConnection {

    Logger log = Logger.getLogger(NormalFTPConnection.class.getName());

    public NormalFTPConnection() {
        super();
    }

    
    public void connect() throws NotConnectedException, IOException,
            AuthenticationNotSupportedException, FtpIOException,
            FtpWorkflowException {
        
        setConnectionStatusLock(FTPConnection.CSL_INDIRECT_CALL);
        
        socketProvider = new SocketProvider();
        // Only for logging
        String hostAndPort = getAddress().getHostName() + ":"
                + getAddress().getPort();
        try {
            socketProvider.connect(getAddress(), getProxy(),
                    getDownloadBandwidth(), getUploadBandwidth());
            log.debug("connected to:" + hostAndPort);
            socketProvider.socket().setSoTimeout(getTimeout());
            socketProvider.socket().setKeepAlive(true);
        } catch (IOException ioe) {
            String error = "Error connection to:" + hostAndPort;
            log.error(error, ioe);
            throw new NotConnectedException(error);
        }
        (ReplyWorker.readReply(socketProvider)).dumpReply();
        Reply reply = sendCommand(new Command(Command.USER, getUser()));
        reply.dumpReply();
        reply.validate();
        if (getPassword() != null && getPassword().length() > 0) {
            reply = sendCommand(new Command(Command.PASS, getPassword()));
            reply.dumpReply();
            reply.validate();
        }
        if (getAccount() != null && getAccount().length() > 0) {
            reply = sendCommand(new Command(Command.ACCT, getAccount()));
            reply.dumpReply();
            reply.validate();
        }
      
        checkFeatures();
        
        this.setConnectionStatus(FTPConnection.CONNECTED);
        this.setConnectionStatus(FTPConnection.IDLE);

        checkSystem();
        
        setConnectionStatusLock(FTPConnection.CSL_DIRECT_CALL);
    }
}
