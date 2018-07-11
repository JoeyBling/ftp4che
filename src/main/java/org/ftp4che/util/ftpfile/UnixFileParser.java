package org.ftp4che.util.ftpfile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class UnixFileParser implements FileParser {
    private static final Logger log = Logger.getLogger(UnixFileParser.class);
    
    private Locale locale;
    
    private static final String[] formatString = { 	new String("MMM dd yyyy HH:mm"),
    												new String("MMM dd yyyy") };
    
    
	public UnixFileParser(Locale locale) {
		this.locale = locale;
	}

	public FTPFile parse(String serverString, String parentDirectory) throws ParseException {
		if ( serverString == null )
			throw new ParseException("Did not get a line - will skip parsing!", 0);
		
		// validate the given line 
		char start = serverString.charAt(0);
		if ( start != 'd' && start != 'l' && start != '-')
			return null;
		else if ( start == '+' )
			throw new ParseException("Looks like this one is the wrong parser, but found EPLF format!", 0);
		
		FTPFile file = new FTPFile(FTPFile.UNIX, parentDirectory, null, serverString);
		String[] tokens = serverString.split(" ++");
		
		// are there enough tokens in the line
		if ( tokens.length < 8 )
			throw new ParseException("The given line is unparseable for UnixFileParser - there are too less tokens in, need at least 8 ones!", serverString.length());
		
		file.setMode( tokens[0] );
		file.setDirectory( tokens[0].startsWith("d") );
		file.setLink( tokens[0].startsWith("l") );
		
		try {
			file.setLinkCount( Integer.parseInt(tokens[1]) );
		}catch(Exception e) {
			throw new ParseException("Found unparseable field: " + tokens[1], 0);
		}
		
		file.setOwner( tokens[2] );
		file.setGroup( tokens[3] );
		
		try {
			file.setSize( Long.parseLong(tokens[4]) );			
		}catch(Exception e) {
			// maybe group token was missing
			try {
				file.setSize( Long.parseLong(tokens[3]) );
				file.setGroup("");
			}catch(Exception e2) {
				throw new ParseException("Could not parse the file size from token: " + tokens[4] + "   Also one token before: " + tokens[3], 0);
			}
		}
		
		/* look which date format is used
		 * 
		 * 1. format: MMM dd hh:mm
		 * 2. format: MMM dd yyyy
		 */ 
		SimpleDateFormat sdf = null;
		if ( tokens[7].indexOf(":") > 0 ) { // no year given - the format is MMM-dd hh:mm
            try {
            	sdf = new SimpleDateFormat(formatString[0],locale);
                file.setDate(sdf.parse( tokens[5] + " " + // month
                						tokens[6] + " " + // day
                						Calendar.getInstance(this.locale).get(Calendar.YEAR) + " " + // no year given - using current
                						tokens[7] ));
            } catch (ParseException pe) {
            	sdf = new SimpleDateFormat(formatString[0], Locale.ENGLISH);
                file.setDate(sdf.parse( tokens[5] + " " + // month
										tokens[6] + " " + // day
										Calendar.getInstance(Locale.ENGLISH).get(Calendar.YEAR) + " " + // no year given - using current
										tokens[7] ));
                this.locale = Locale.ENGLISH;
            }
            
            // check if it is possible that the year from filedate is the current
            Calendar today = Calendar.getInstance(this.locale);
            if (file.getDate().after(today.getTime())) {
            	today.setTime(file.getDate());
            	today.set(Calendar.YEAR, today.get(Calendar.YEAR) - 1);
            	file.setDate(today.getTime());
            }
		}else {
            try {
            	sdf = new SimpleDateFormat(formatString[1],locale);
                file.setDate(sdf.parse( tokens[5] + " " + // month
                						tokens[6] + " " + // day
                						tokens[7] )); // year
            } catch (ParseException pe) {
            	sdf = new SimpleDateFormat(formatString[1], Locale.ENGLISH);
                file.setDate(sdf.parse( tokens[5] + " " + // month
                						tokens[6] + " " + // day
                						tokens[7] )); // year
                this.locale = Locale.ENGLISH;
            }
		}

		String name = "";
		for (int i=8; i<tokens.length; i++)
			name += tokens[i] + " ";
		name = name.trim();
		
		if ( !file.isLink() ) {
			file.setName(name);
		}else { // be care of the -> in the string
			file.setName(name.split("->")[0].trim());
			file.setLinkedName(name.split("->")[1].trim());
		}
		
		return file;
	}
}


