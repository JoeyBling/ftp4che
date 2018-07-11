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

import org.ftp4che.io.ReplyWorker;
import org.ftp4che.reply.Reply;

public class ListCommand extends DataConnectionCommand {

    public ListCommand(String parameter) {
        super(Command.LIST, parameter);
    }

    public ListCommand() {
        this(".");
    }

    // TODO: what todo if you get exception from replyworker ?
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

}
