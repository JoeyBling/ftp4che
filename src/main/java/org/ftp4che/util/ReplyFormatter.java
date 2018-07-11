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
package org.ftp4che.util;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.ftp4che.exception.UnkownReplyStateException;
import org.ftp4che.reply.Reply;

public class ReplyFormatter {
    public static Logger log = Logger.getLogger(ReplyFormatter.class.getName());

    public static String parsePWDReply(Reply pwdReply)
            throws UnkownReplyStateException {
        List lines = pwdReply.getLines();
        if (lines.size() != 1)
            throw new UnkownReplyStateException(
                    "PWD Reply has to have a size of 1 entry but it has: "
                            + lines.size());
        String line = (String)lines.get(0);
        // LINE: 257 "/" is current directory.
        return line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
    }
    
public static Date parseMDTMReply(Reply mdtmReply) throws ParseException
    {
    	  List lines = mdtmReply.getLines();
          if (lines.size() != 1)
              throw new UnkownReplyStateException(
                      "MDTM Reply has to have a size of 1 entry but it has: "
                              + lines.size());
          String line = (String)lines.get(0);
          SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
          return formatter.parse(line.substring(line.indexOf(' ') + 1));
    }

    public static String parseXCRCReply(Reply crcReply) 
    {
  	  List lines = crcReply.getLines();
      if (lines.size() != 1)
          throw new UnkownReplyStateException(
                  "XCRC Reply has to have a size of 1 entry but it has: "
                          + lines.size());
      String line = (String)lines.get(0);
      return line.substring(line.indexOf(' ') + 1);
    }
    
    public static String parseXMD5Reply(Reply md5Reply) 
    {
  	  List lines = md5Reply.getLines();
      if (lines.size() != 1)
          throw new UnkownReplyStateException(
                  "XMD5 Reply has to have a size of 1 entry but it has: "
                          + lines.size());
      String line = (String)lines.get(0);
      return line.substring(line.indexOf(' ') + 1);
    }

    public static InetSocketAddress parsePASVCommand(Reply pasvReply)
            throws UnkownReplyStateException {
        List lines = pasvReply.getLines();
        if (lines.size() != 1)
            throw new UnkownReplyStateException(
                    "PASV Reply has to have a size of 1 entry but it has: "
                            + lines.size());
        String line = (String)lines.get(0);
        line = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
        String[] host = line.split(",");
        log.debug("Parsed host:"
                        + host[0]
                        + "."
                        + host[1]
                        + "."
                        + host[2]
                        + "."
                        + host[3]
                        + " port: "
                        + ((Integer.parseInt(host[4]) << 8) + Integer
                                .parseInt(host[5])));
        return new InetSocketAddress(host[0] + "." + host[1] + "." + host[2]
                + "." + host[3], (Integer.parseInt(host[4]) << 8)
                + Integer.parseInt(host[5]));
    }
    
    public static String displayBytes(byte[] bytes)
    {
      if (bytes == null || bytes.length == 0)
           return "[]";
     
      StringBuffer buf = new StringBuffer();
      buf.append('[');
     
      for (int i = 0; i < bytes.length; i++) {        
            buf.append(bytes[i]);
            buf.append(",");
      }
      buf.deleteCharAt(buf.length() - 1);
      buf.append("]");
      return buf.toString();
    }
}
