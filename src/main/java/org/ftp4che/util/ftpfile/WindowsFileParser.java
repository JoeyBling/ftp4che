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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

public class WindowsFileParser implements FileParser {
    private SimpleDateFormat formatter;

    private static final String DIR = "<DIR>";

    private static final int MIN_COUNT = 4;

    private static final String formatString[] = {"MM-dd-yy hh:mma"};
    
    private Locale locale;
    
    public WindowsFileParser(Locale locale) {
        this.locale = locale;
    }

    public FTPFile parse(String serverLine, String parentDirectory)
            throws ParseException {
        StringTokenizer st = new StringTokenizer(serverLine, " ");

        if (MIN_COUNT > st.countTokens())
            throw new ParseException("Wrong number of fields: " + st.countTokens()
                    + " expected minimum:" + MIN_COUNT, 0);
        Date date = null;
        String dateToken = st.nextToken();
        String timeToken = st.nextToken();
        boolean formatted = false;
        for(int i = 0; i < formatString.length; i++)
        {
        	try
        	{
        		formatter = new SimpleDateFormat(formatString[i], locale);
        		date = formatter.parse(dateToken + " " + timeToken);
        		formatted = true;
        	}catch (ParseException pe)
        	{
        		try
        		{
        			formatter = new SimpleDateFormat(formatString[i], Locale.ENGLISH);
        			date = formatter.parse(dateToken + " " + timeToken);
        			this.locale = Locale.ENGLISH;
        			formatted = true;
        		}catch(ParseException pe2)
				{
					//Ignore this exception
					formatted = false;
				}
        	}
        	if(formatted)
        		break;
        }
        boolean directory = false;
        long size = -1;
        String dirSizeToken = st.nextToken();
        if (dirSizeToken.equalsIgnoreCase(DIR))
            directory = true;
        else {
            try {
                size = Long.parseLong(dirSizeToken);
            } catch (NumberFormatException ex) {
                throw new ParseException("Failed to parse size: " + dirSizeToken,
                        0);
            }
        }
        String name = "";
        while(st.hasMoreTokens())
        {
        	name += st.nextToken() + " ";
        }
		FTPFile file = new FTPFile(FTPFile.WINDOWS, parentDirectory, name.trim(),
				serverLine);
		file.setSize(size);
		file.setDate(date);
		file.setDirectory(directory);
		return file;
    }

}
