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
import java.io.InputStream;

public class BandwidthControlledInputStream extends InputStream {
    private int bandwidth = 512;

    private InputStream in;

    private static int SLEEP_TIME = 1000;

    private int bytesRead;

    private long startTime;

    private static final int CHECKS = 4;

    private int lastSum;

    public BandwidthControlledInputStream(InputStream in, int bandwidth) {
        if (bandwidth > this.bandwidth)
            this.bandwidth = bandwidth / CHECKS;
        this.in = in;
        startTime = System.currentTimeMillis();
    }

    public int read() throws IOException {
        int bytes = in.read();
        bytesRead++;
        return bytes;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        int sum = 0;
        int bytes2read;
        if ((len - off) > bandwidth)
            bytes2read = bandwidth;
        else
            bytes2read = len - off;
        int newBytes = in.read(b, off, bytes2read);
        sum += newBytes;
        if (newBytes == -1) {
            try {
                Thread.sleep(1000 * CHECKS / (bandwidth / lastSum));
            } catch (InterruptedException ie) {
            }
            return -1;
        }
        bytesRead += bytes2read;

        // System.out.println("bytes2read: " + bytes2read);
        // System.out.println("bytesRead: " + bytesRead);
        // System.out.println("sum: " + sum);
        // System.out.println("newBytes: " + newBytes);
        sleep();
        lastSum = sum;
        return sum;
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
    }

    private void sleep() {
        if (bytesRead >= bandwidth) {
            long duration = System.currentTimeMillis() - startTime;
            if (duration < 1000 / CHECKS) {
                try {
                    Thread.sleep(1000 / CHECKS - duration);
                } catch (InterruptedException ignore) {
                }
            }
            startTime = System.currentTimeMillis();
            bytesRead = 0;
        }
    }
}