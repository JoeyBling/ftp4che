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
package org.ftp4che.exception;

import java.net.ConnectException;

public class ProxyConnectionException extends ConnectException {

    private int status = 0;

    private String msg = "";

    public ProxyConnectionException() {
        super();
    }

    public ProxyConnectionException(String msg) {
        super(msg);
        setMsg(msg);
    }

    public ProxyConnectionException(int status, String msg) {
        this(msg);
        setStatus(status);
    }

    /**
     * @return Returns the msg.
     */
    public String getMsg() {
        return msg;
    }

    /**
     * @param msg
     *            The msg to set.
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * @return Returns the status.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     *            The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

}
