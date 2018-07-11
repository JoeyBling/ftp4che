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
package org.ftp4che.examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ftp4che.FTPConnection;
import org.ftp4che.FTPConnectionFactory;
import org.ftp4che.exception.ConfigurationException;
import org.ftp4che.exception.NotConnectedException;
import org.ftp4che.util.ftpfile.FTPFile;

public class DownloadStreamTest {
    public static void main(String args[]) {
        Logger log = Logger.getLogger("MAIN");

        Properties pt = new Properties();
        pt.setProperty("connection.host", "127.0.0.1");
        pt.setProperty("connection.port", "21");
        pt.setProperty("user.login", "kurt");
        pt.setProperty("user.password", "Bez3et4");
        pt.setProperty("connection.type", "FTP_CONNECTION");
        pt.setProperty("connection.timeout", "10000");
        pt.setProperty("connection.passive", "true");

        try {
            FTPConnection connection = FTPConnectionFactory.getInstance(pt);

            try {
                connection.connect();
                FTPFile fromFile = new FTPFile("/home/kurt", "token.jpg");
                InputStream pis = null;
                FileOutputStream fos = new FileOutputStream("/tmp/download.stream");
                
                pis = connection.downloadStream(fromFile);
                
                int len = -1;
                byte[] buf = new byte[pis.available()];
                while ( (len = pis.read(buf)) != -1 ) {
                    fos.write(buf, 0, len);
                }
                
                pis.close();
                fos.close();
                
                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
                nce.printStackTrace();
            } catch (IOException ioe) {
                log.error(ioe);
                ioe.printStackTrace();
            } catch (Exception e) {
                log.error(e);
                e.printStackTrace();
            }
        } catch (ConfigurationException ce) {
            log.error(ce);
        }
    }
}