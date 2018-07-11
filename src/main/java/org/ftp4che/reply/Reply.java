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
package org.ftp4che.reply;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.ftp4che.exception.FtpFileNotFoundException;
import org.ftp4che.exception.FtpIOException;
import org.ftp4che.exception.FtpWorkflowException;
import org.ftp4che.exception.NotConnectedException;

public class Reply {
    List lines = new ArrayList();

    Logger log = Logger.getLogger(Reply.class.getName());

    public Reply() {

    }

    public Reply(List lines) {
        setLines(lines);
    }

    /**
     * @return Returns the lines.
     */
    public List getLines() {
        return lines;
    }

    /**
     * @param lines
     *            The lines to set.
     */
    public void setLines(List lines) {
        this.lines = new ArrayList(lines);
    }

    public void dumpReply() {
        for (Iterator it = lines.iterator(); it.hasNext();) {
            String line = (String)it.next();
            log.info(line.substring(0, line.length() - 1));
        }
    }

    public String getReplyCode() {
        return ((String)getLines().get(getLines().size() - 1)).substring(0, 3);
    }

    public String getReplyMessage() {
        return ((String)getLines().get(getLines().size() - 1)).substring(4);
    }

    public void validate() throws FtpWorkflowException, FtpIOException {
    	if (this.getLines().size() <= 0)
    		throw new FtpIOException("000", "Did not receive any reply!");
    		
        if (ReplyCode.isPermanentNegativeCompletionReply(this)) {
            if (getReplyCode().intern() == ReplyCode.REPLY_530.intern())
                throw new NotConnectedException(getReplyMessage());
            if (getReplyCode().intern() == ReplyCode.REPLY_550.intern())
                throw new FtpFileNotFoundException(getReplyMessage());
            throw new FtpWorkflowException(this.getReplyCode(), this
                    .getReplyMessage());
        } else if (ReplyCode.isTransientNegativeCompletionReply(this)) {
            throw new FtpIOException(this.getReplyCode(), this
                    .getReplyMessage());
        }
    }

}
