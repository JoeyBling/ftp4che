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
package org.ftp4che.commands;

/**
 * @author arnold
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class Command {
    // Constants
    public final static String delimiter = "\r\n";

    public final static String TYPE_A = "TYPE A";

    public final static String TYPE_I = "TYPE I";

    public final static String ACCT = "ACCT";

    public final static String APPE = "APPE";

    public final static String CWD = "CWD";

    public final static String CDUP = "CDUP";

    public final static String DELE = "DELE";

    public final static String FEAT = "FEAT";

    public final static String MKD = "MKD";

    public final static String PASV = "PASV";

    public final static String PASS = "PASS";

    public final static String PORT = "PORT";

    public final static String PWD = "PWD";

    public final static String QUIT = "QUIT";

    public final static String RMD = "RMD";

    public final static String REST = "REST";

    public final static String RETR = "RETR";

    public final static String RNTO = "RNTO";

    public final static String RNFR = "RNFR";

    public final static String SITE = "SITE";

    public final static String STOR = "STOR";

    public final static String SYST = "SYST";

    public final static String USER = "USER";

    public final static String TYPE = "TYPE";

    public final static String LIST = "LIST";

    public final static String NOOP = "NOOP";

    public final static String STAT = "STAT";

    public final static String PROT = "PROT";

    public final static String PBSZ = "PBSZ";

    public final static String SSCN = "SSCN";
    
    public final static String MDTM = "MDTM";
    
    public final static String CLNT = "CLNT";

    public final static String XCRC = "XCRC";
    
    public final static String XMD5 = "XMD5";
  
    public final static String MLSD = "MLSD";

    public final static String PRET = "PRET";
    
    public final static String CPSV = "CPSV";
    
    String command;

    String[] parameter;

	public static final String SSCN_ON = "ON";

	public static final String SSCN_OFF = "OFF";
    
    private static final String[] nullString = {};

    public Command(String command) {
        this(command, nullString);
    }

    public Command(String command, String[] parameter) {
        setCommand(command);
        setParameter(parameter);
    }
    
    public Command(String command,String singleParameter)
    {
        this(command,new String[] {singleParameter});
    }

    /**
     * @return Returns the command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command
     *            The command to set.
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return Returns the parameter.
     */
    public String[] getParameter() {
        return parameter;
    }

    /**
     * @param parameter
     *            The parameter to set.
     */
    public void setParameter(String[] parameter) {
        this.parameter = parameter;
    }

    public String toString() {
        String returnValue = getCommand();
        for(int i = 0; i < getParameter().length; i++)
        {
            returnValue += " " + getParameter()[i];
        }

        return returnValue.trim() + delimiter;
    }

}
