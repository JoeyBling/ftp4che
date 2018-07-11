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
package org.ftp4che.reply;

public class ReplyCode {

    // reply groups
    // meaning of first digit
    public static final String POSITIVE_PRELIMINARY_REPLY = "1";

    public static final String POSITIVE_COMPLETION_REPLY = "2";

    public static final String POSITIVE_INTERMIDIATE_REPLY = "3";

    public static final String TRANSIENT_NEGATIVE_COMPLETION_REPLY = "4";

    public static final String PERMANENT_NEGATIVE_COMPLETION_REPLY = "5";

    // reply information
    // meaning of second digit
    public static final String SYNTAX = "0";

    public static final String INFORMATION = "1";

    public static final String CONNECTIONS = "2";

    public static final String AUTHENTIFICATION = "3";

    public static final String UNSPECIFIED = "4";

    public static final String FILE_SYSTEM = "5";

    // reply codes
    public static final String REPLY_110 = "110"; // Restart marker reply.

    // In this case, the text is exact and not left to the
    // particular implementation; it must read:
    // MARK yyyy = mmmm
    // Where yyyy is User-process data stream marker, and mmmm
    // server's equivalent marker (note the spaces between markers and "=").
    public static final String REPLY_120 = "120"; // Service ready in nnn
                                                    // minutes.

    public static final String REPLY_125 = "125"; // Data connection already
                                                    // open; transfer starting.

    public static final String REPLY_150 = "150"; // File status okay; about
                                                    // to open data connection.

    public static final String REPLY_200 = "200"; // Command okay.

    public static final String REPLY_202 = "202"; // Command not implemented,
                                                    // superfluous at this site.

    public static final String REPLY_211 = "211"; // System status, or system
                                                    // help reply.

    public static final String REPLY_212 = "212"; // Directory status.

    public static final String REPLY_213 = "213"; // File status.

    public static final String REPLY_214 = "214"; // Help message.

    // On how to use the server or the meaning of a particular
    // non-standard command. This reply is useful only to the
    // human user.
    public static final String REPLY_215 = "215"; // NAME system type.

    // Where NAME is an official system name from the list in the
    // Assigned Numbers document.
    public static final String REPLY_220 = "220"; // Service ready for new
                                                    // user.

    public static final String REPLY_221 = "221"; // Service closing control
                                                    // connection.

    // Logged out if appropriate.
    public static final String REPLY_225 = "225"; // Data connection open; no
                                                    // transfer in progress.

    public static final String REPLY_226 = "226"; // Closing data connection.

    // Requested file action successful (for example, file
    // transfer or file abort).
    public static final String REPLY_227 = "227"; // Entering Passive Mode
                                                    // (h1,h2,h3,h4,p1,p2).

    public static final String REPLY_230 = "230"; // User logged in, proceed.

    public static final String REPLY_250 = "250"; // Requested file action
                                                    // okay, completed.

    public static final String REPLY_257 = "257"; // "PATHNAME" created.

    public static final String REPLY_331 = "331"; // User name okay, need
                                                    // password.

    public static final String REPLY_332 = "332"; // Need account for login.

    public static final String REPLY_350 = "350"; // Requested file action
                                                    // pending further
                                                    // information.

    public static final String REPLY_421 = "421"; // Service not available,
                                                    // closing control
                                                    // connection.

    // This may be a reply to any command if the service knows it
    // must shut down.
    public static final String REPLY_425 = "425"; // Can't open data
                                                    // connection.

    public static final String REPLY_426 = "426"; // Connection closed;
                                                    // transfer aborted.

    public static final String REPLY_450 = "450"; // Requested file action not
                                                    // taken.

    // File unavailable (e.g., file busy).
    public static final String REPLY_451 = "451"; // Requested action aborted:
                                                    // local error in
                                                    // processing.

    public static final String REPLY_452 = "452"; // Requested action not
                                                    // taken.

    // Insufficient storage space in system.
    public static final String REPLY_500 = "500"; // Syntax error, command
                                                    // unrecognized.

    // This may include errors such as command line too long.
    public static final String REPLY_501 = "501"; // Syntax error in
                                                    // parameters or arguments.

    public static final String REPLY_502 = "502"; // Command not implemented.

    public static final String REPLY_503 = "503"; // Bad sequence of commands.

    public static final String REPLY_504 = "504"; // Command not implemented
                                                    // for that parameter.

    public static final String REPLY_530 = "530"; // Not logged in.

    public static final String REPLY_532 = "532"; // Need account for storing
                                                    // files.

    public static final String REPLY_550 = "550"; // Requested action not
                                                    // taken.

    // File unavailable (e.g., file not found, no access).
    public static final String REPLY_551 = "551"; // Requested action aborted:
                                                    // page type unknown.

    public static final String REPLY_552 = "552"; // Requested file action
                                                    // aborted.

    // Exceeded storage allocation (for current directory or dataset).
    public static final String REPLY_553 = "553"; // Requested action not
                                                    // taken.

    // File name not allowed.

    public static boolean isPositivePreliminaryReply(Reply reply) {
    	if(reply.getLines().size() <= 0) return false;
        return (((String) reply.getLines().get(reply.getLines().size() - 1))
                .startsWith(POSITIVE_PRELIMINARY_REPLY) ? true : false);
    }

    public static boolean isPositiveCompletionReply(Reply reply) {
    	if(reply.getLines().size() <= 0) return false;
        return (((String) reply.getLines().get(reply.getLines().size() - 1))
                .startsWith(POSITIVE_COMPLETION_REPLY) ? true : false);
    }

    public static boolean isPositiveIntermidiateReply(Reply reply) {
    	if(reply.getLines().size() <= 0) return false;
        return (((String) reply.getLines().get(reply.getLines().size() - 1))
                .startsWith(POSITIVE_INTERMIDIATE_REPLY) ? true : false);
    }

    public static boolean isTransientNegativeCompletionReply(Reply reply) {
    	if(reply.getLines().size() <= 0) return false;
        return (((String) reply.getLines().get(reply.getLines().size() - 1))
                .startsWith(TRANSIENT_NEGATIVE_COMPLETION_REPLY) ? true : false);
    }

    public static boolean isPermanentNegativeCompletionReply(Reply reply) {
    	if(reply.getLines().size() <= 0) return false;
        return (((String) reply.getLines().get(reply.getLines().size() - 1))
                .startsWith(PERMANENT_NEGATIVE_COMPLETION_REPLY) ? true : false);
    }
}
