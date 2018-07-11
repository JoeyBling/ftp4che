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
package org.ftp4che.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ftp4che.commands.Command;
import org.ftp4che.commands.ListCommand;
import org.ftp4che.commands.RetrieveCommand;
import org.ftp4che.commands.StoreCommand;
import org.ftp4che.reply.Reply;
import org.ftp4che.util.ReplyFormatter;

public class ReplyWorker extends Thread {
    public static final Logger log = Logger.getLogger(ReplyWorker.class
            .getName());

    public static final int FINISHED = 1;

    public static final int ERROR_FILE_NOT_FOUND = 2;

    public static final int ERROR_IO_EXCEPTION = 3;

    public static final int UNKNOWN = -1;

    private Exception caughtException = null;

    private SocketProvider socketProvider;
    
    private PipedInputStream inputPipe;
    
    private PipedOutputStream outputPipe;
    
    private ByteBuffer downloadBuffer;
    
    private int downloadMethod = RetrieveCommand.FILE_BASED;
    
    private Command command;

    private Charset charset = Charset.forName("ISO8859-1");

    private CharsetDecoder charDecoder = charset.newDecoder();

    private ByteBuffer buffer = ByteBuffer.allocate(16384);

    private int status = ReplyWorker.UNKNOWN;

    private Reply reply;

    public ReplyWorker(SocketProvider sc, Command command) {
        setSocketProvider(sc);
        setCommand(command);
    }
    
    public ReplyWorker(SocketProvider sc, Command command, Object res, int method) throws IOException {
        setSocketProvider(sc);
        setCommand(command);
        setDownloadMethod(method);
        
        switch ( getDownloadMethod() ) {
            case RetrieveCommand.STREAM_BASED:
                setInputPipe((PipedInputStream) res);
                outputPipe = new PipedOutputStream();
                inputPipe.connect(outputPipe);
                break;
            
            case RetrieveCommand.BYTEBUFFER_BASED:
                setDownloadBuffer((ByteBuffer) res);
                break;
                
            default:
                break;
        }
    }

    public static Reply readReply(SocketProvider socketProvider) throws IOException {
        return ReplyWorker.readReply(socketProvider, false);
    }

    public static Reply readReply(SocketProvider socketProvider,
            boolean isListReply) throws IOException {
        List lines = new ArrayList();
        Charset charset = Charset.forName("ISO8859-1");
        CharsetDecoder charDecoder = charset.newDecoder();
        Logger log = Logger.getLogger(ReplyWorker.class.getName());

        String output = "";
		String out = "";
		ByteBuffer buf = null;
		if (!isListReply)
			buf = ByteBuffer.allocateDirect(1024);
		else
			buf = ByteBuffer.allocateDirect(16384);
		int amount;
		buf.clear();
		socketProvider.socket().setKeepAlive(true);
		boolean read = true;
		while (read && (amount = socketProvider.read(buf)) >= 0) {
			if (amount == 0) {
				try {
					sleep(50);
				} catch (InterruptedException ie) {
				}
				continue;
			}

			buf.flip();
			out = charDecoder.decode(buf).toString();
			log.debug("Read data from server (String) ->" + out);
			log.debug("Read data from server (bytes) -> "
					+ ReplyFormatter.displayBytes(out.getBytes()));
			output += out;
			buf.clear();
			String[] tmp = output.split("\n");

			if (!isListReply
					&& tmp.length > 0
					&& tmp[tmp.length - 1].length() > 3
					&& tmp[tmp.length - 1].endsWith("\r")
					&& tmp[tmp.length - 1].charAt(3) == ' '
					&& Pattern.matches("[0-9]+", tmp[tmp.length - 1].substring(
							0, 3))) {
				String[] stringLines = output.split("\n");

				for (int i = 0; i < stringLines.length; i++) {
					log
							.debug("Adding line to result list -> "
									+ stringLines[i]);
					lines.add(stringLines[i]);
				}
				read = false;
				output = "";
				buf.clear();
			}
			try {
				sleep(50);
			} catch (InterruptedException ie) {
			}
		}
		if (isListReply) {
			String[] stringLines = output.split("\r\n");

			for (int i = 0; i < stringLines.length; i++) {
				// Empty lines cause NoSuchElementException in
				// FTPFile org.ftp4che.util.FTPFile.parseLine(String line)
				// (unsave use of StringTokenizer.nextToken
				if (stringLines[i].length() > 0) {
					log.debug("LIST Reply lines -> " + stringLines[i]);
					lines.add(stringLines[i]);
				}
			}
			output = "";
			buf.clear();
			socketProvider.close();
		}

        return new Reply(lines);
    }

    public void run() {
        if (getCommand() == null)
            throw new IllegalArgumentException("Given command is null!");
        if (getSocketProvider() == null)
            throw new IllegalArgumentException("Given connection is not open!");

        if (getCommand() instanceof ListCommand) {
        	try {
        		setReply(ReplyWorker.readReply(getSocketProvider(), true));
        		setStatus(ReplyWorker.FINISHED);
        	}catch(IOException ioe) {
        		setCaughtException(ioe);
        		setStatus(ReplyWorker.ERROR_IO_EXCEPTION);
        	}
            
            return;
        } else if (getCommand() instanceof RetrieveCommand) {
            RetrieveCommand retrieveCommand = (RetrieveCommand) getCommand();

            // TODO: Add handling for TYPE A
            if (retrieveCommand.getFromFile().getTransferType().intern() == Command.TYPE_I
                    || retrieveCommand.getFromFile().getTransferType().intern() == Command.TYPE_A) {
                try {
                    log.debug("Download file: "
                            + retrieveCommand.getFromFile().toString());
                    FileOutputStream out = null;
                    FileChannel channel = null;
                    
                    if (getDownloadMethod() == RetrieveCommand.FILE_BASED) {
                        out = new FileOutputStream(retrieveCommand.getToFile().getFile());
                        channel = out.getChannel();
                        if(retrieveCommand.getResumePosition() != -1)
                        {
                        	try
                        	{
                        		channel.position(retrieveCommand.getResumePosition());
                        	}catch (IOException ioe)
                        	{
                        		 setCaughtException(ioe);
                                 setStatus(ReplyWorker.ERROR_IO_EXCEPTION);
                                 try
                         		 {
                         			channel.close();
                         		 }catch (IOException ioe2) {}
                         		 return;
                        	}
                        }
                    }else if (getDownloadMethod() == RetrieveCommand.BYTEBUFFER_BASED) {
                        // TODO: byte buffer handling for resume
                    }
                    int amount;
                    try {
                        while ((amount = getSocketProvider().read(buffer)) != -1) {
                            if (amount == 0) {
                                try {
                                    Thread.sleep(4);
                                } catch (InterruptedException e) {
                                }
                            }
                            buffer.flip();
                            while (buffer.hasRemaining()) {
                                if (getDownloadMethod() == RetrieveCommand.STREAM_BASED) {
                                    int rem = buffer.remaining();
                                    byte[] buf = new byte[rem];
                                    buffer.get(buf, 0, rem);
                                    this.outputPipe.write(buf, 0, rem);
                                }else if (getDownloadMethod() == RetrieveCommand.BYTEBUFFER_BASED) {
                                    // TODO: byte buffer handling for getting data
                                }else {
                                    channel.write(buffer);
                                }
                            }

                            buffer.clear();
                        }
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            if (getDownloadMethod() == RetrieveCommand.STREAM_BASED) {
                                int rem = buffer.remaining();
                                byte[] buf = new byte[rem];
                                buffer.get(buf, 0, rem);
                                this.outputPipe.write(buf, 0, rem);
                            }else if (getDownloadMethod() == RetrieveCommand.BYTEBUFFER_BASED) {
                                // TODO: byte buffer handling for getting data
                            }else {
                                channel.write(buffer);
                            }
                        }
                        buffer.clear();
                        setStatus(ReplyWorker.FINISHED);
                        
                        if (channel != null)
                            channel.close();
                        if (this.outputPipe != null)
                            this.outputPipe.close();
                        
                        getSocketProvider().close();
                    } catch (IOException ioe) {
                        setCaughtException(ioe);
                        setStatus(ReplyWorker.ERROR_IO_EXCEPTION);
                    }finally
                    {
                    	try
                    	{
                    		channel.close();
                            getSocketProvider().close();
                    	}catch (Exception e) {}
                    }

                } catch (FileNotFoundException fnfe) {
                    setCaughtException(fnfe);
                    setStatus(ReplyWorker.ERROR_FILE_NOT_FOUND);
                }
            } else
                throw new IllegalArgumentException(
                        "Unknown file transfer type for download!");
            return;
        } else if (getCommand() instanceof StoreCommand) {
            StoreCommand storeCommand = (StoreCommand) getCommand();
            // TODO: Add handling for TYPE A
            if (storeCommand.getToFile().getTransferType().intern() == Command.TYPE_I
                    || storeCommand.getToFile().getTransferType().intern() == Command.TYPE_A) {
                try {
                    log.debug("Upload file: " + storeCommand.getFromFile());
                    InputStream in = storeCommand.getStream();
                	int amount;
                	int socketWrite;
                	int socketAmount = 0;
                	
                    if(in instanceof FileInputStream)
                    {                  
                    	FileChannel channel = ((FileInputStream)in).getChannel();
                    	if(storeCommand.getResumePosition() != -1)
                    	{
                    		try
                    		{
                    			channel.position(storeCommand.getResumePosition());
                    		}catch (IOException ioe)
                    		{
                    			setCaughtException(ioe);
                    			setStatus(ReplyWorker.ERROR_IO_EXCEPTION);
                    			try
                    			{
                    				channel.close();
                    			}catch (IOException ioe2) {}
                    			return;
                    		}
                    	}
                    	try {
                    		while ((amount = channel.read(buffer)) != -1) {
                    			buffer.flip();
                    			socketWrite = 0;
                    			while ((socketWrite = getSocketProvider().write(
                    					buffer)) != -1) {
                    				socketAmount += socketWrite;
                    				if (amount <= socketAmount) {
                    					break;
                    				}
                    				if (socketWrite == 0) {
                    					try {
                    						Thread.sleep(4);
                    					} catch (InterruptedException e) {
                    				}	
                    				}
                    			}
                    			if (socketWrite == -1) {
                            		break;
                            	}
                            	socketAmount = 0;
                            	buffer.clear();
                    		}
                    		setStatus(ReplyWorker.FINISHED);
                    		channel.close();
                    		getSocketProvider().close();
                    		} catch (IOException ioe) {
                    			setCaughtException(ioe);
                    			setStatus(ReplyWorker.ERROR_IO_EXCEPTION);
                    		}finally
                    		{
                    			try
                    			{
                    				channel.close();
                    				getSocketProvider().close();
                    			}catch (Exception e) {}
                    		}
                    }
                    else
                    {
                    	try { 
                    			while ((amount = in.read(buffer.array())) != -1) { 
                    					buffer.flip(); 
                    					buffer.limit(amount); 
                    					socketWrite = 0; 
                    					while ((socketWrite = getSocketProvider().write(buffer)) != -1) { 
                    							socketAmount = socketWrite; 
                    							if (amount <= socketAmount) { 
                    								break; 
                    							} 
                    							if (socketWrite == 0) { 
                    								try { 
                    									Thread.sleep(4); 
                    								} catch (InterruptedException e) { 
                    								} 
                    							} 
                    					} 
                    					if (socketWrite == -1) { 
                    						break; 
                    					} 
                    					socketAmount = 0; 
                    					buffer.clear(); 
                    			} 
                    			setStatus(ReplyWorker.FINISHED); 
                    			in.close(); 
                    			getSocketProvider().close(); 
                    		 	} catch (IOException ioe) { 
                    		 		setCaughtException(ioe); 
                    		 		setStatus(ReplyWorker.ERROR_IO_EXCEPTION); 
                    		 	}finally 
                    		 	{ 
                    		 		try 
                    		 		{ 
                    		 			in.close(); 
                    		 			getSocketProvider().close(); 
                    		 		}catch (Exception e) {} 
                    		 } 
                    }
                } catch (FileNotFoundException fnfe) {
                    setCaughtException(fnfe);
                    setStatus(ReplyWorker.ERROR_FILE_NOT_FOUND);
                }
            } else
                throw new IllegalArgumentException(
                        "Unknown file transfer type for upload!");

        } else
            throw new IllegalArgumentException(
                    "Given command is not supported!");
    }

    /**
     * @param socketProvider
     *            The socketProvider to set.
     */
    public void setSocketProvider(SocketProvider socketProvider) {
        this.socketProvider = socketProvider;
    }

    /**
     * @return Returns the command.
     */
    public Command getCommand() {
        return command;
    }

    /**
     * @param command
     *            The command to set.
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * @return Returns the socketProvider.
     */
    public SocketProvider getSocketProvider() {
        return socketProvider;
    }

    /**
     * @return Returns the status.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     *            The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return Returns the caughtException.
     */
    public Exception getCaughtException() {
        return caughtException;
    }

    /**
     * @param caughtException
     *            The caughtException to set.
     */
    public void setCaughtException(Exception caughtException) {
        this.caughtException = caughtException;
    }

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }

    /**
     * @param inputPipe The inputPipe to set.
     */
    public void setInputPipe(PipedInputStream inputPipe) {
        this.inputPipe = inputPipe;
    }

    /**
     * @param downloadBuffer The downloadBuffer to set.
     */
    public void setDownloadBuffer(ByteBuffer downloadBuffer) {
        this.downloadBuffer = downloadBuffer;
    }

    /**
     * @return Returns the downloadMethod.
     */
    public int getDownloadMethod() {
        return downloadMethod;
    }

    /**
     * @param downloadMethod The downloadMethod to set.
     */
    public void setDownloadMethod(int downloadMethod) {
        this.downloadMethod = downloadMethod;
    }
}
