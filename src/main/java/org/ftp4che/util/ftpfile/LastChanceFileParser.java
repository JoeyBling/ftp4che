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
package org.ftp4che.util.ftpfile;


import java.text.ParseException;
import java.util.Locale;

import org.apache.log4j.Logger;

public class LastChanceFileParser implements FileParser {
    public static Logger log = Logger.getLogger(LastChanceFileParser.class);
	private Locale locale;
	
	public LastChanceFileParser(Locale locale)
	{
		this.locale = locale;
	}
	
	public FTPFile parse(String serverString, String parentDirectory)
			throws ParseException {
        log.fatal("LIST reply line -> " + serverString + " parentDirectory -> " + parentDirectory);
		return new FTPFile("THIS SHOULD NEVER HAPPEN","THIS IS AN ERROR");
		
	}
}
