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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

public class FTPFileFactory {

    private static Logger log = Logger.getLogger(FTPFileFactory.class);

    public static final String UNIX_IDENTIFICATION = "UNIX";

    public static final String WINDOWS_IDENTIFICATION = "WINDOWS";

    public static final String VMS_IDENTIFICATION = "VMS";

    private String system;

    private FileParser parser = null;
    
    public Locale locale;
    
    public FTPFileFactory(String system) {
        this.system = system.toUpperCase();
        this.locale = Locale.getDefault();
        parser = getParserInstance();
    }
    public FTPFileFactory(String system,Locale locale) {
        this.system = system.toUpperCase();
        this.locale = locale;
        parser = getParserInstance();
    }

    public String getSystem() {
        return system;
    }

    public FileParser getParserInstance() {
        if (system.indexOf(UNIX_IDENTIFICATION) >= 0)
        {
        	log.debug("Found UNIX identification, try to use UNIX file parser");
            return new UnixFileParser(locale);
        }
        else if (system.indexOf(WINDOWS_IDENTIFICATION) >= 0)
        {
        	log.debug("Found WINDOWS identification, try to use WINDOWS file parser");
            return new WindowsFileParser(locale);
        }
        else if (system.indexOf(VMS_IDENTIFICATION) >= 0) {
        	log.debug("Found VMS identification, try to use VMS file parser");
            return new VMSFileParser(locale);
        } else {
            log.warn("Unknown SYST '" + system + "', trying UnixFileParsers");
            return null;
        }
    }

    public List parse(List serverLines, String parentPath)
    {
        List files = new ArrayList(serverLines.size());
                
        for (Iterator it = serverLines.iterator(); it.hasNext();) {
        	FTPFile file = null;
            String line = (String) it.next();
            
        	try
        	{
                log.debug("Trying to parse line: " + line);
        		file = parser.parse(line, parentPath);
        	}catch (ParseException pe)
        	{
        		// Expected parser couldn't parse trying other parsers
        		try
        		{
        			log.warn("Previous file parser couldn't parse listing. Trying a UNIX file parser");
        			parser = new UnixFileParser(locale);
        			file = parser.parse(line, parentPath);
        		}catch (ParseException pe2)
        		{
        			try
        			{
        				log.warn("Previous file parser couldn't parse listing. Trying a EPLF file parser");
        				parser = new EPLFFileParser();
        				file = parser.parse(line, parentPath);
        			}catch (ParseException pe5)
        			{
        			try
            		{
        				log.warn("Previous file parser couldn't parse listing. Trying a netware file parser");
            			parser = new NetwareFileParser(locale);
            			file = parser.parse(line, parentPath);
            		}catch (ParseException pe3)
            		{
            			log.warn("Last chance!!! calling LastChanceFileParser");
            			parser = new LastChanceFileParser(locale);
            			try
            			{
            				file = parser.parse(line, parentPath);
            			}catch (ParseException pe4)
            			{
            				log.fatal("Couldn't parse the LIST reply");
            			}
            		}
        		}
        		}
        	}
            if (file != null)
                files.add(file);
        }
        return files;
    }

    
    public static void main(String args[])
    {
    	FTPFileFactory factory = new FTPFileFactory("VMS");
    	List list = new ArrayList();
//    	list.add("lrwxrwxrwx   1 root     root           11 Aug 18  2000 HEADER -> welcome.msg");
// 		list.add("drwxr-xr-x   2 root     wheel        4096 Nov 27  2000 bin");
// 		list.add("drwxr-xr-x   2 root     wheel        4096 Sep 18 20:00 pub");
// 		list.add("-rw-r--r--   1 root     root          312 Aug  1  1994 welcome msg");
// 		list.add("d [R----F--] supervisor            512       Jan 16 18:53    login");
// 		list.add("213-Status follows:");
// 		list.add("total 28");
// 		list.add("drwx------    5 503      503          4096 Oct 04 08:12 .");
// 		list.add("drwxr-xr-x    5 0        0            4096 Aug 22 09:16 ..");
// 		list.add("-rw-------    1 503      503          2626 Aug 26 08:39 .bash_history");
// 		list.add("213 End of status");
    	list.add("00README.TXT;1      2 30-DEC-1996 17:44 [SYSTEM] (RWED,RWED,RE,RE)");
    	list.add("CORE.DIR;1          1  8-SEP-1996 16:09 [SYSTEM] (RWE,RWE,RE,RE)");
    	list.add("CII-MANUAL.TEX;1  213/216  29-JAN-1996 03:33:12  [ANONYMOUS,ANONYMOUS]   (RWED,RWED,,)");
//    	list.add("02-13-01  08:02PM       <DIR>          bussys");
//    	list.add("11-27-00  11:28AM                    456 dirmap.htm");
//    	list.add("-------r--         326  1391972  1392298 Nov 22  1995 MegaPhone.sit");
//    	list.add("02-13-01  08:02PM       <DIR>          bussys box");
//    	list.add("05-21-01  03:41PM       <DIR>          deskapps");
//    	list.add("04-20-01  03:41PM       <DIR>          developr");
//    	list.add("11-27-00  11:28AM                    0 dirmap.htm");
//    	list.add("+i8388621.48594,m825718503,r,s280,	djb.html");
//    	list.add("+i8388621.50690,m824255907,/,	514");
//    	list.add("+i8388621.48598,m824253270,r,s612,	514.html");
 		List files = new ArrayList();
 		
 		files = factory.parse(list,"/tmp");
 		for(Iterator it = files.iterator(); it.hasNext();)
 		{
 		    FTPFile file = (FTPFile) it.next();
            
 			log.debug("Name: " + file.getName());
 			log.debug("Size: " + file.getSize());
 			log.debug("Group: " + file.getGroup());
 			log.debug("isLink: " + file.isLink);
 			log.debug("isDir: " + file.isDirectory());
 			log.debug("Modes:  " + file.getMode());
 			log.debug("points to:" + file.getLinkedname());
 			log.debug("------------------------------");
 		}
    }
}
