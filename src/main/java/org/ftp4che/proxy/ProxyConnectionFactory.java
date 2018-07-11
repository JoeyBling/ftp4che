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
package org.ftp4che.proxy;

import java.util.Properties;

import org.ftp4che.exception.ConfigurationException;

public class ProxyConnectionFactory {

    public static final String HTTP = "HTTP";

    public static final String SOCKS4 = "SOCKS4";

    public static final String SOCKS4A = "SOCKS4A";

    public static final String SOCKS5 = "SOCKS5";

    public static Proxy getInstance(String host, int port, String user,
            String pass, String type) throws ConfigurationException {
        if (type.equalsIgnoreCase(SOCKS4))
            return new Socks4(host, port, user);

        throw new ConfigurationException("Unkown proxy type.");
    }

    public static Proxy getInstance(Properties config)
            throws ConfigurationException {
        return ProxyConnectionFactory.getInstance(config
                .getProperty("proxy.host"), Integer.parseInt(config
                .getProperty("proxy.port")), config.getProperty("proxy.user"),
                config.getProperty("proxy.password"), config
                        .getProperty("proxy.type"));
    }
}
