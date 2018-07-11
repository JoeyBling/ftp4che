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

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ftp4che.FTPConnection;
import org.ftp4che.FTPConnectionFactory;
import org.ftp4che.exception.ConfigurationException;
import org.ftp4che.exception.NotConnectedException;
import org.ftp4che.util.ftpfile.FTPFile;

public class DownloadTest {
    public static void main(String args[]) {
        Logger log = Logger.getLogger("MAIN");

        Properties pt = new Properties();
        pt.setProperty("connection.host", "172.25.13.187");
        pt.setProperty("connection.port", "21");
        pt.setProperty("user.login", "ftpuser");
        pt.setProperty("user.password", "ftp4che");
        pt.setProperty("connection.type", "FTP_CONNECTION");
        pt.setProperty("connection.timeout", "10000");
        pt.setProperty("connection.passive", "true");

        FTPFile fromFile = new FTPFile("/", "testfile");

        try {
            FTPConnection connection = FTPConnectionFactory.getInstance(pt);

            try {
                connection.connect();

                FTPFile toFile = new FTPFile("/tmp/", "testfile_normal");
                connection.downloadFile(fromFile, toFile);

                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }

            try {
                pt
                        .setProperty("connection.type",
                                "IMPLICIT_SSL_FTP_CONNECTION");
                pt.setProperty("connection.port", "990");
                connection = FTPConnectionFactory.getInstance(pt);
                connection.connect();
                FTPFile toFile = new FTPFile("/tmp/", "testfile_implicit_ssl");
                connection.downloadFile(fromFile, toFile);

                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }

            try {
                pt
                        .setProperty("connection.type",
                                "IMPLICIT_TLS_FTP_CONNECTION");
                pt.setProperty("connection.port", "990");
                connection = FTPConnectionFactory.getInstance(pt);
                connection.connect();
                FTPFile toFile = new FTPFile("/tmp/", "testfile_implicit_tls");
                connection.downloadFile(fromFile, toFile);

                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }

            try {
                pt.setProperty("connection.port", "21");
                pt.setProperty("connection.type", "AUTH_SSL_FTP_CONNECTION");
                connection = FTPConnectionFactory.getInstance(pt);
                connection.connect();

                FTPFile toFile = new FTPFile("/tmp/", "testfile_ssl");
                connection.downloadFile(fromFile, toFile);

                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }

            try {
                pt.setProperty("connection.type", "AUTH_TLS_FTP_CONNECTION");
                connection = FTPConnectionFactory.getInstance(pt);
                connection.connect();

                FTPFile toFile = new FTPFile("/tmp/", "testfile_tls");
                connection.downloadFile(fromFile, toFile);

                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }
        } catch (ConfigurationException ce) {
            log.error(ce);
        }
    }
}