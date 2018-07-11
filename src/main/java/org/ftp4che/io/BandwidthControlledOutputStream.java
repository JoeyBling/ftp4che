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
package org.ftp4che.io;

import java.io.IOException;
import java.io.OutputStream;

public class BandwidthControlledOutputStream extends OutputStream {

    private int bandwidth = 512;

    private OutputStream out;

    private static int SLEEP_TIME = 1000;

    private int bytesWritten;

    public BandwidthControlledOutputStream(OutputStream out, int bandwidth) {
        if (bandwidth > this.bandwidth)
            this.bandwidth = bandwidth;
        this.out = out;
    }

    public void write(int b) throws IOException {
        bytesWritten++;
        out.write(b);
        sleep();
    }

    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        int position = off;
        int capacity = len;

        while (position < capacity) {
            int bytes2write;
            if ((capacity - position) > bandwidth)
                bytes2write = bandwidth;
            else
                bytes2write = capacity - position;
            out.write(bytes, position, bytes2write);
            position += bytes2write;
            bytesWritten += bytes2write;
            sleep();
        }
    }

    private void sleep() {
        if (bytesWritten >= bandwidth) {
            try {
                // System.out.println("sleeping");
                Thread.sleep(SLEEP_TIME);
                bytesWritten = 0;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
    }
}