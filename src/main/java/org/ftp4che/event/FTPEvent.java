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
package org.ftp4che.event;

import java.util.EventObject;

import org.ftp4che.reply.Reply;
import org.ftp4che.util.ftpfile.FTPFile;

public class FTPEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** use status constants from class FTPConnection */
    private int connectionStatus;

    private Reply reply;

    private FTPFile fromFile;

    private FTPFile toFile;

    public FTPEvent(Object source, int connectionStatus) {
        super(source);
        setConnectionStatus(connectionStatus);
    }

    public FTPEvent(Object source, int connectionStatus, Reply reply) {
        super(source);
        setConnectionStatus(connectionStatus);
        setReply(reply);
    }

    public FTPEvent(Object source, int connectionStatus, FTPFile fromFile,
            FTPFile toFile) {
        super(source);
        setConnectionStatus(connectionStatus);
        setFromFile(fromFile);
        setToFile(toFile);
    }

    public FTPEvent(Object source, int connectionStatus, Reply reply,
            FTPFile fromFile, FTPFile toFile) {
        super(source);
        setConnectionStatus(connectionStatus);
        setReply(reply);
        setFromFile(fromFile);
        setToFile(toFile);
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }

    public FTPFile getFromFile() {
        return fromFile;
    }

    public void setFromFile(FTPFile fromFile) {
        this.fromFile = fromFile;
    }

    public FTPFile getToFile() {
        return toFile;
    }

    public void setToFile(FTPFile toFile) {
        this.toFile = toFile;
    }
}
