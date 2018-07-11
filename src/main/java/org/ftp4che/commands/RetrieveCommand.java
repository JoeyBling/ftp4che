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
package org.ftp4che.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

import org.ftp4che.io.ReplyWorker;
import org.ftp4che.reply.Reply;
import org.ftp4che.util.ftpfile.FTPFile;

public class RetrieveCommand extends DataConnectionCommand {
    
    public static final int FILE_BASED = 0;
    public static final int STREAM_BASED = 1;
    public static final int BYTEBUFFER_BASED = 2;

    private FTPFile fromFile;

    private FTPFile toFile;
    
    private PipedInputStream inputPipe;
    
    private ByteBuffer byteBuffer;
    
    private long resumePosition = -1;
    
    // TODO: throw Exception if fromFile not Exists

    public RetrieveCommand(String command, FTPFile fromFile) {
        super(command, fromFile.getName());
        setFromFile(fromFile);
    }

    public RetrieveCommand(String command, FTPFile fromFile, FTPFile toFile) {
        super(command, fromFile.toString());
        setFromFile(fromFile);
        setToFile(toFile);
    }
    
    public RetrieveCommand(String command, FTPFile fromFile, PipedInputStream is) {
        super(command, fromFile.toString());
        setFromFile(fromFile);
        setInputPipe(is);
    }
    
    public RetrieveCommand(String command, FTPFile fromFile, ByteBuffer bb) {
        super(command, fromFile.toString());
        setFromFile(fromFile);
        setByteBuffer(bb);
    }
 
    
    public Reply fetchDataConnectionReply() throws FileNotFoundException,
    IOException {
        return fetchDataConnectionReply(RetrieveCommand.FILE_BASED);
    }

    public Reply fetchDataConnectionReply(int method) throws FileNotFoundException,
            IOException {
        ReplyWorker worker = null;
        
        switch ( method ) {
            case RetrieveCommand.STREAM_BASED:
                worker = new ReplyWorker(getDataSocket(), this, this.inputPipe, method);
                break;
            case RetrieveCommand.BYTEBUFFER_BASED:
                worker = new ReplyWorker(getDataSocket(), this, this.byteBuffer, method);
                break;
            default:
                worker = new ReplyWorker(getDataSocket(), this, null, RetrieveCommand.FILE_BASED);
                break;
        }
        
        worker.start();
        while (worker.getStatus() == ReplyWorker.UNKNOWN) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ie) {
            }
        }
        if (worker.getStatus() == ReplyWorker.FINISHED) {
            return worker.getReply();
        } else {
            if (worker.getCaughtException() instanceof FileNotFoundException)
                throw (FileNotFoundException) worker.getCaughtException();
            else
                throw (IOException) worker.getCaughtException();
        }
    }

    /**
     * @return Returns the file.
     */
    public FTPFile getFromFile() {
        return fromFile;
    }

    /**
     * @param file
     *            The file to set.
     */
    public void setFromFile(FTPFile file) {
        this.fromFile = file;
    }

    /**
     * @return Returns the toFile.
     */
    public FTPFile getToFile() {
        return toFile;
    }

    /**
     * @param toFile
     *            The toFile to set.
     */
    public void setToFile(FTPFile toFile) {
        this.toFile = toFile;
    }

	public long getResumePosition() {
		return resumePosition;
	}

	public void setResumePosition(long resumePosition) {
		this.resumePosition = resumePosition;
	}

    /**
     * @param inputPipe The inputPipe to set.
     */
    public void setInputPipe(PipedInputStream inputPipe) {
        this.inputPipe = inputPipe;
    }

    /**
     * @param byteBuffer The byteBuffer to set.
     */
    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

}
