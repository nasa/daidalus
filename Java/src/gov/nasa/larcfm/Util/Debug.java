/*
 *  Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * Copyright (c) 2013-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.nio.charset.StandardCharsets;

/**
 * <p>This class contains a small set of tools to help in debugging.  First is a set of tools to instrument the code
 * each type of instrumentation attempts to address a different type of bug.  Any messages go to the Standard output channel.
 * messages come out in the format "<code>&lt;tag&gt; message</code>" so they can easily be found. For errors, the tag
 * is pre-populated with "ERROR!", for warnings the tag is user specified.  For Status messages, the tag is optional.</p>
 * 
 * <p>Unfortunately, this implementation every Debug method added still costs execution time, regardless if
 * the given verbosity level means no message will be produced.  So Debug.pln(lvl, big-complicated-string-operation)
 * means the big string operation will be computed every time, even when the Debug.pln is never triggered.</p>
 * 
 *  <p>Each of these debugging messages approximately means</p>
 * <ul>
 * <li>Severe (Error) - Usually indicates a software error.  Something where the program is confused.  The requested operation will need to be ignored 
 *     (or perhaps the program must exit). 
 * <li>Warning - Usually indicates a condition that should not be reached, but the software can "fix" the situation.  
 *     The fix may or may not be the intent, hence the warning.
 * <li>Status - Anything else
 * </ul>
 * 
 * The main instrumentation methods are
 * <ul>
 * <li><code>checkError(condition, msg)</code> If the condition is violated, then the message is output.  If the <i>FAIL_FAST</i> flag is true, 
 * the program will exit. 
 * <li><code>error(msg)</code> This method will always display the <i>msg</i> to 
 * the console. This should be reserved for true errors, not curious or questionable situations.    If the 
 * <i>FAIL_FAST</i> flag is true, then this method will force an immediate program exit.
 * <li><code>checkWarning(condition, tag, msg)</code> If the condition is violated, then the message is displayed to the console.
 * <li><code>warning(tag, msg)</code> Writes the message to the console (in the format described above).  
 * <li><code>pln(tag, msg)</code> This provides intermediate program state information.  If the Debug class is in "verbose" mode, 
 * then these messages will be output, if Debug is in "silent" mode, then these messages will be suppressed.  
 * <li><code>pln(msg)</code> This provides intermediate program state information.  Only the message comes out, there is no "tag"
 * <li><code>pln(msg, verbose)</code> Same as above, except it does not rely Debug's notion of a verbosity level.  If the verbose flag is true, then the
 * message is output.
 * <li><code>pln(lvl, msg)</code> This provides information at a user-specified level ({@code >= 2}).  Only the message comes out, there is no "tag"
 * <li><code>pln(lvl, tag, msg)</code> Same as above, along with a user-specified tag.
 * </ul>
 * 
 * Usage scenarios
 * <ul>
 * <li>Ensure that conditions that should "never happen" truly never happen.  Instrument the areas of the code that should never
 * be reached with <code>checkError</code> or <code>error</code> methods.  While in development the <i>FAIL_FAST</i> flag should be true.  When the software is
 * ready for distribution, it should be set to true.  If a field report comes back with unusual behavior, search the log for any
 * messages that start with "&lt;ERROR!&gt;".
 * <li>An external user can't get the software to work.  Instruct them to call <code>setVerbose(2)</code>.  (I presume you will have
 * some way in the user interface to set this flag). Then let them
 * examine the log to see if they can determine the issue, and likewise the log can be sent to developers. 
 * </ul>
 * 
 * Future work
 * <ul>
 * <li> Send this information to log files instead of the console.
 * <li> Store the previous message in a string so a GUI can pick it up
 * </ul>
 */
public class Debug {

	private static final int INFO_LEVEL = 2;      // DO NOT CHANGE. Should be 2, should never need to be public/protected/package scope  
	public static final boolean FAIL_FAST = false;  //!!!!! set this to false for distribution; true for local debugging
	private static ByteArrayOutputStream buffer = null;
	
	private static final Logger LOGGER;
	private static final DebugFormatter FORMAT;
	private static final Handler STDOUT_HANDLER;
	private static final Handler BUFFER_HANDLER;
	
	static class DebugFormatter extends Formatter {
		private static final String NL = System.lineSeparator();
		private boolean useLevel = false;
	    public void setLevel(boolean useLevel) {
			this.useLevel = useLevel;
		}
		@Override
	    public String format(LogRecord record) {
	    	if (useLevel) {
	    		String s;
//	    		if (record.getLevel() == Level.SEVERE) {
//	    			s = "ERROR!";
//	    		} else {
	    			s = record.getLevel().toString();
//	    		}
		        return "<"+s+"> " + record.getMessage()+NL;
	    	} else {
		        return record.getMessage()+NL;
	    	}
	    }
	}

	static class DebugHandler extends StreamHandler {
		Formatter fmt;
		OutputStream os;
		public DebugHandler(Formatter fmt, OutputStream os) {
			setOutputStream(os);
			setFormatter(fmt);
			this.fmt = fmt;
			this.os = os;
		}
		@Override public synchronized void publish(LogRecord r) {
			String s = fmt.format(r);
			try {
				os.write(s.getBytes());
			} catch (IOException e) {
				// do nothing
			}
		}
	}
	
	static {
		LOGGER = Logger.getGlobal();
        LOGGER.setLevel(Level.INFO);  // default, can be changed

        // create a TXT formatter
        FORMAT = new DebugFormatter();
        STDOUT_HANDLER = new DebugHandler(FORMAT, System.out);
		buffer = new ByteArrayOutputStream();
        BUFFER_HANDLER = new DebugHandler(FORMAT, buffer);
        LOGGER.addHandler(STDOUT_HANDLER);
        LOGGER.setUseParentHandlers(false); // shut off standard handler
	}
	
	// don't allow construction
	private Debug() {
	}
	
	/**
	 * Sets the destination of where log information should go.  This method should
	 * either never be called (that is, left as the default standard output), or it 
	 * should be called very early after a program starts.
	 * 
	 * <ul>
	 * <li> 1 - standard output only
	 * <li> 2 - memory buffer only
	 * <li> 3 - standard output and memory buffer
	 * <li> 4 - file, with the provided filename
	 * <li> 5 - file and standard out
	 * <li> 6 - file and memory buffer
	 * <li> 7 - file, memory buffer, and standard out
	 * </ul>
	 * @param where number indicating where Debug log should be sent
	 * @param file  OutputStream for file output.  Consider wrapping a FileOutputStream in a BufferedOutputStream to improve performance. Can be null, if file output is not used.
	 */
	public static synchronized void setupDestination(int where, OutputStream file) {
		if (where <= 0) {
			return;
		}
		if (file == null && where >= 4) {
			where = where - 4;
		}
		
        LOGGER.removeHandler(STDOUT_HANDLER);
        LOGGER.removeHandler(BUFFER_HANDLER);

		if (where == 5 || where == 6 || where == 7) {
			Handler fileHandler = new DebugHandler(FORMAT, file);
			LOGGER.addHandler(fileHandler);
		} 
		if (where == 2 || where == 3 || where == 6 || where == 7) {
			LOGGER.addHandler(BUFFER_HANDLER);
		}
		if (where == 1 || where == 3 || where == 5 || where == 7) {
			LOGGER.addHandler(STDOUT_HANDLER);
		}
	}
	

	
	/**
	 * Set the verbosity level for debuggging
	 * <ul>
	 * <li> {@literal <}0 - Off
	 * <li> 0 - Errors only
	 * <li> 1 - Errors and Warnings
	 * <li> 2 - Errors, Warnings, and Info
	 * <li> {@literal >}2 - All the above, plus user-specified levels
	 * </ul>
	 * 
	 * @param level verbosity level
	 */
	public static synchronized void setVerbose(int level) {
		if (level < 0) {
			LOGGER.setLevel(Level.OFF);
		} else if (level == 0) {
			LOGGER.setLevel(Level.SEVERE);
		} else if (level == 1) {
			LOGGER.setLevel(Level.WARNING);
		} else if (level == 2) { // INFO_LEVEL
			LOGGER.setLevel(Level.INFO);
		} else if (level == 3) {
			LOGGER.setLevel(Level.CONFIG);
		} else if (level == 4) {
			LOGGER.setLevel(Level.FINE);
		} else if (level == 5) {
			LOGGER.setLevel(Level.FINER);
		} else { // level >= 6
			LOGGER.setLevel(Level.FINEST);
		}
	}

	/**
	 * Return the verbosity level
	 * <ul>
	 * <li> {@literal <}0 - Off 
	 * <li> 0 - Errors only
	 * <li> 1 - Errors and Warnings
	 * <li> 2 - Errors, Warnings, and Info
	 * <li> {@literal >}2 - All the above, plus user-specified levels
	 * </ul>
	 * 
	 * @return verbosity level
	 */
	public static synchronized int getVerbose() {
		return getLevel(LOGGER.getLevel());
	}

	private static int getLevel(Level l) {
		if (l == Level.OFF) {
			return -1;
		} else if (l == Level.SEVERE) {
			return 0;
		} else if (l == Level.WARNING) {
			return 1;
		} else if (l == Level.INFO) {
			return 2;
		} else if (l == Level.CONFIG) {
			return 3;
		} else if (l == Level.FINE) {
			return 4;
		} else if (l == Level.FINER) {
			return 5;
		} else { // Level.FINEST or higher
			return 6;
		}
	}

	/** Clear and return the contents of buffer.  Buffer contains previous Debug messages.
	 * 
	 * @return contents of buffer.
	 */
	public static String getBuffer() {
		String ret;
		// ret = buffer.toString(StandardCharsets.UTF_8);
		try {
			ret = buffer.toString(StandardCharsets.UTF_8.name());
		} catch (java.io.UnsupportedEncodingException err) {
			System.err.println("[Debug.getBuffer()] Warning: Unable to convert buffer to UTF-8, using default encoding as fallback.");
			System.err.println(err);
			ret = buffer.toString();
		}
		buffer.reset();
		return ret;
	}
	
	private static synchronized void output(int level, String tag, String msg) {
		String[] lines = msg.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			String s = formatTag(tag,lines[i]);
			switch (level) {
			case 0: FORMAT.setLevel(true); LOGGER.severe(s); break;
			case 1: FORMAT.setLevel(true); LOGGER.warning(s); break;
			case 2: FORMAT.setLevel(false); LOGGER.info(s); break;
			case 3: FORMAT.setLevel(false); LOGGER.config(s); break;
			case 4: FORMAT.setLevel(false); LOGGER.fine(s); break;
			case 5: FORMAT.setLevel(false); LOGGER.finer(s); break;
			default: 
				if (level > INFO_LEVEL-1) FORMAT.setLevel(false); LOGGER.finest(s); break;
				//note: if level < 0 then ignore any output.
			}
		}
	}
	
	private static synchronized void output(int level, Supplier<String> f) {
		switch (level) {
		case 0: FORMAT.setLevel(true); LOGGER.severe(f); break;
		case 1: FORMAT.setLevel(true); LOGGER.warning(f); break;
		case 2: FORMAT.setLevel(false); LOGGER.info(f); break;
		case 3: FORMAT.setLevel(false); LOGGER.config(f); break;
		case 4: FORMAT.setLevel(false); LOGGER.fine(f); break;
		case 5: FORMAT.setLevel(false); LOGGER.finer(f); break;
		default: 
			if (level > INFO_LEVEL-1) FORMAT.setLevel(false); LOGGER.finest(f); break;
				//note: if level < 0 then ignore any output.
		}
	}
	
	/** 
	 * Output the message <i>msg</i>, indicating that an error has occurred.  If in 
	 * fail-fast mode, then the program will exit (perhaps
	 * with a stack trace).
	 * 
	 * @param msg message
	 */
	public static void error(String msg) {
		error(msg, FAIL_FAST);
	}

	/** 
	 * Output the message <i>msg</i>, indicating that an error has occurred.  
	 * 
	 * @param msg message
	 * @param fail_fast if true, then halt program.  If false, continue. 
	 */
	public static void error(String msg, boolean fail_fast) {
		output(INFO_LEVEL-2, "", msg);
		if (fail_fast) {
			halt();
		}
	}
	
	/**
	 * <p>Check the <i>condition</i>, if true, do nothing. If false, then output the error message.
	 * Also, in fail-fast mode, then the program will exit (perhaps
	 * with a stack trace). </p>
	 * 
	 * <p>
	 * In general this method should not be used.  Instead use, <i>error</i>. This method
	 * conditionally prints an error message.  In general, one wants the error messages
	 * to always come out for errors.
	 * </p>
	 * 
	 * @param condition representation of the nominal or correct state (NO error condition)
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void checkError(boolean condition, String msg) {
		if (! condition) {
			error(msg);
		}
	}

	/**
	 * <p>Check the <i>condition</i>, if true, do nothing. If false, then get the output message
	 * from the supplier. Also, in fail-fast mode, then the program will exit (perhaps
	 * with a stack trace). </p>
	 * 
	 * <p>
	 * In general this method should not be used.  Instead use, <i>error</i>. This method
	 * conditionally prints an error message.  In general, one wants the error messages
	 * to always come out for errors.
	 * </p>
	 * 
	 * @param condition representation of the nominal or correct state (NO error condition)
	 * @param f a function that produces a string through the Supplier interface.
	 */
	public static void checkErrorLazy(boolean condition, Supplier<String> f) {
		if (! condition) {
			error(f.get());
		}
	}
	
	private static String formatTag(String tag, String msg) {
		if (tag.isEmpty()) {
			return msg;
		} else {
			return "<"+tag+"> "+msg;
		}
	}
	/**
	 * Output the <i>msg</i> to the console with the prepended <i>tag</i>.  Warnings are 
	 * output, provided the verbose level is 1 or greater, and should never cause a program termination.
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void warning(String tag, String msg) {
		output(INFO_LEVEL-1, tag,msg);
	}
	
	/**
	 * Output the <i>msg</i> to the console with the prepended <i>WARNING</i> tag.  Warnings are 
	 * output, provided the verbose level is 1 or greater, and should never cause a program termination.
	 * 
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void warning(String msg) {
		output(INFO_LEVEL-1, "", msg);
	}
	
	/**
	 * Print out a status message <i>msg</i>, with each line prepended with tag.
	 * The output will only if "verbose" is true.  
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg the status message
	 * @param verbose if true, then display status message
	 */
	public static void pln(String tag, String msg, boolean verbose) {
		pln(formatTag(tag,msg), verbose);
	}

	/**
	 * <p>Print out a status message <i>msg</i>, with each line prepended with tag.
	 * The output will only come out if "verbose" is true.  This version is lazy, in that the message
	 * captured in the supplier is only evaluated if the verbose flag is true.
	 * This should be used when constructing the string message is a complicated
	 * operation unto itself AND it is executed in a part of the code where
	 * the extra trouble of building a supplier object is worth the trouble.</p>
	 * 
	 * <p>A supplier object should look like:</p>
	 * <code>
	 * class MyClass implements Supplier{@literal <}String{@literal >} {
	 *   {@literal @}override
	 *   public String get() {
	 *     return complex_string_construction_operation_here();
	 *   }
	 * }
	 * </code>
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param f a supplier object that contains the status message
	 * @param verbose if true, then display status message
	 */
	public static void plnLazy(String tag, Supplier<String> f, boolean verbose) {
		if (verbose) {
			int l = getVerbose();
			if (l > INFO_LEVEL) {
				l = INFO_LEVEL;
			}
			output(l, tag, f.get());
		}
	}

	/**
	 * Print out a status message <tt>msg</tt>.
	 * The output will only come out if <tt>verbose</tt> is true and if 
	 * the verbose level ({@link #setVerbose} must be 2 to larger.  
	 * 
	 * @param verbose if true, the display status message
	 * @param msg the status message
	 */
	public static void pln(boolean verbose, String msg) {
		pln(msg, verbose);
	}

	/**
	 * Print out a status message <tt>msg</tt>.
	 * The output will only come out if <tt>verbose</tt> is true and if 
	 * the verbose level ({@link #setVerbose} must be 2 to larger.  
	 * 
	 * @param msg the status message
	 * @param verbose if true, the display status message
	 */
	public static void pln(String msg, boolean verbose) {
		if (verbose) {
			int l = getVerbose();
			if (l > INFO_LEVEL) {
				l = INFO_LEVEL;
			}
			pln(l, msg);
		}
	}
	
	/**
	 * Print out a status message <i>msg</i>, with each line prepended with tag.
	 * The output will only come out if Debug is in "verbose" mode. Note: 
	 * There is a (small) performance penalty for every call, even if the VERBOSE level
	 * indicates no message will come out.
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg the status message
	 */
	public static void pln(String tag, String msg) {
		pln(INFO_LEVEL, tag, msg);
	}

	/**
	 * Print out a status message <i>msg</i>.
	 * The output will only come out if 2 is below the Debug verbosity level. Note: 
	 * There is a (small) performance penalty for every call, even if the VERBOSE level
	 * indicates no message will come out.
	 * 
	 * @param msg the status message
	 */
	public static void pln(String msg) {
		pln(INFO_LEVEL, "", msg);
	}

	/**
	 * Print out a status message <i>msg</i>, with each line prepended with tag as a specified error level.
	 * The output will only come out if level is below the Debug verbosity level. Note: 
	 * There is a (small) performance penalty for every call, even if the VERBOSE level
	 * indicates no message will come out.
	 * 
	 * @param level the message error level, values less than 2 will be treated as 2
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg the status message
	 */
	public static void pln(int level, String tag, String msg) {
		output(level, tag, msg);
	}

	/**
	 * <p>Print out a status message <i>msg</i>, with each line prepended with tag.
	 * The output will only come out if level is below the Debug verbosity level.  This version is lazy, in that the message
	 * captured in the supplier is only evaluated if the verbose level is low enough.
	 * This should be used when constructing the string message is a complicated
	 * operation unto itself AND it is executed in a part of the code where
	 * the extra trouble of building a supplier object is worth the trouble.</p>
	 * 
	 * <p>A supplier object should look like:</p>
	 * <code style="display:block; white-space:pre-wrap">
	 * class MyClass implements Supplier{@literal <}String{@literal >} { <br>
	 *   public String get() { <br>
	 *     return complex_string_construction_operation_here(); <br>
	 *   }<br>
	 * }<br>
	 * </code>
	 * 
	 * @param level verbosity level
	 * @param tag ignored parameter, kept to maintain backward compability
	 * @param f a supplier object that contains the status message
	 */
	public static void plnLazy(int level, String tag, Supplier<String> f) {
		output(level, f);
	}
	
	/**
	 * Print out a message <i>msg</i> at a user-specified error level.
	 * The output will only come out if level is below the Debug verbosity level. Note: 
	 * There is a (small) performance penalty for every call, even if the VERBOSE level
	 * indicates no message will come out.
	 * 
	 * @param level the message error level, values less than 2 will be treated as 2
	 * @param msg the status message
	 */
	public static void pln(int level, String msg) {
		output(level, "", msg);
	}

	/**
	 * <p>Print out a status message <i>msg</i>.
	 * The output will only be displayed if level is below the Debug verbosity level.  This version is lazy, in that the message
	 * captured in the supplier is only evaluated if the verbose level is low enough.
	 * This should be used when constructing the string message is a complicated
	 * operation unto itself AND it is executed in a part of the code where
	 * the extra trouble of building a supplier object is worth the trouble.</p>
	 * 
	 * <p>A supplier object should look like:</p>
	 * <code>
	 * class MyClass implements Supplier{@literal <}String{@literal >} {
	 *   {@literal @}override
	 *   public String get() {
	 *     return complex_string_construction_operation_here();
	 *   }
	 * }
	 * </code>
	 * 
	 * @param f a supplier object that contains the status message
	 * @param level verbosity level
	 */
	public static void plnLazy(int level, Supplier<String> f) {
		output(level, f);
		
	}

	/**
	 * Indicate than something bad has happened and the program to needs to end now.  On some platforms, print backtrace.
	 * In general, this method should not be used, use {@link error} instead.
	 *  */
	public static void halt() {
		Thread.dumpStack();
		System.exit(1);
	}
	
	public static void printTrace() {
		new Throwable().printStackTrace();
	}
		
	public static String toStringCallingMethod() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		String s = " $$$ calling method = "+stackTrace[1].getMethodName()+" "+stackTrace[1].getLineNumber();  // 0 is this method
		s += " $$$                = "+stackTrace[2].getMethodName()+" "+stackTrace[2].getLineNumber();  // 0 is this method
		return s;
	}


}

