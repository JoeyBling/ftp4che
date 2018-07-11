package org.ftp4che.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ftp4che.FTPConnection;
import org.ftp4che.FTPConnectionFactory;
import org.ftp4che.exception.NotConnectedException;

public class ConnectionTest {
    public static void main(String args[]) {
        Logger log = Logger.getLogger("MAIN");

        Properties pt = new Properties();
        try
        {
        	pt.load(new FileInputStream(args[0]));
        }catch (IOException ioe)
        {
        	log.fatal("Couldn't load the config file!");
        	log.fatal("Usage: ConnectionTest configfile");
        	System.exit(1);
        }
        try {
            FTPConnection connection = FTPConnectionFactory.getInstance(pt);

            
                connection.connect();
                connection.disconnect();
            } catch (NotConnectedException nce) {
                log.error(nce);
            } catch (IOException ioe) {
                log.error(ioe);
            } catch (Exception e) {
                log.error(e);
            }
    }
}