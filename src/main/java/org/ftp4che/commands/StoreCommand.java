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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.ftp4che.io.ReplyWorker;
import org.ftp4che.reply.Reply;
import org.ftp4che.util.ftpfile.FTPFile;

public class StoreCommand extends DataConnectionCommand {

	  FTPFile toFile;

	    FTPFile fromFile;
	    
	    InputStream stream;

	    private long resumePosition = -1;
	    
	    // TODO: throw Exception if fromFile not Exists

	    public StoreCommand(String command, FTPFile toFile) {
	        super(command, toFile.toString());
	        setToFile(toFile);
	    }

	    public StoreCommand(String command, FTPFile fromFile, FTPFile toFile) {
	        this(command, toFile);
	        setFromFile(fromFile);
	    }
	    
	    public StoreCommand(String command, InputStream upStream, FTPFile toFile) {
	        this(command, toFile);
	        setStream(upStream);
	    }
	    
	    public StoreCommand(String command, Object upSrc, FTPFile toFile) throws IllegalArgumentException {
	        this(command, toFile);
	        
	        if (upSrc instanceof FTPFile)
	            setFromFile((FTPFile) upSrc);
	        else if (upSrc instanceof InputStream)
	            setStream((InputStream) upSrc);
	        else
	            throw new IllegalArgumentException("The upSrc either must be a FTPFile or an InputStream!");
	    }

	    public Reply fetchDataConnectionReply() throws FileNotFoundException,
	            IOException {
	        ReplyWorker worker = new ReplyWorker(getDataSocket(), this);
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
	    public FTPFile getToFile() {
	        return toFile;
	    }

	    /**
	     * @param file
	     *            The file to set.
	     */
	    public void setToFile(FTPFile file) {
	        this.toFile = file;
	    }

	    /**
	     * @return Returns the fromFile.
	     */
	    public FTPFile getFromFile() {
	        return fromFile;
	    }

	    /**
	     * @param fromFile
	     *            The fromFile to set.
	     */
	    public void setFromFile(FTPFile fromFile) {
	        this.fromFile = fromFile;
	    }

		public long getResumePosition() {
			return resumePosition;
		}

		public void setResumePosition(long resumePosition) {
			this.resumePosition = resumePosition;
		}
	    
	    public InputStream getStream() throws FileNotFoundException {
	        if (stream != null)
	            return stream;
	        else if(fromFile != null && fromFile.getName().length() > 0)
	            return new FileInputStream(fromFile.getFile());
	        
	        throw new FileNotFoundException();
	    }
	    
	    public void setStream(InputStream upStream) {
	        this.stream = upStream;
	    }
}
