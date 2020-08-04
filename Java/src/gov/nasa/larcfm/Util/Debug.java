/*
 * Copyright (c) 2013-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

/**
 * <p>This class contains a small set of tools to help in debugging.  First is a set of tools to instrument the code
 * each type of instrumentation attempts to address a different type of bug.  Any messages go to the Standard error channel.
 * messages come out in the format "<code>&lt;tag&gt; message</code>" so they can easily be found. For errors, the tag
 * is prepopulated with "ERROR!", for warnings the tag is user specified.</p> 
 * 
 * 
 * The main instrumentation methods are
 * <ul>
 * <li><code>checkError(condition, msg)</code> If the condition is violated, then the message is output.  If the <i>FAIL_FAST</i> flag is true, 
 * the program will exit. 
 * <li><code>error(msg)</code> This method will always display the <i>msg</i> to 
 * the console. This should be reserved for true errors, not curious or questionable situations.    If the 
 * <i>FAIL_FAST</i> flag is true, then this method will force an immediate program exit.
 * <li><code>checkWarning(condition, tag, msg)</code> If the condition is violated, then the message is displayed to the console.
 * <li><code>warning(tag, msg)</code> Writes the message to the console (in the format described above).  Could be thought of as
 * checkWarning(false, tag, msg)
 * <li><code>pln(tag, msg)</code> This provides intermediate program state information.  If the Debug class is in "verbose" mode, 
 * then these messages will be output, if Debug is in "silent" mode, then these messages will be suppressed.  Behaves
 * as if it was implemented with checkWarning( ! VERBOSE, tag, msg)
 * </ul>
 *
 * Usage scenarios
 * <ul>
 * <li>Ensure that conditions that should "never happen" truly never happen.  Instrument the areas of the code that should never
 * be reached with <code>checkError</code> or <code>error</code> methods.  While in development the <i>FAIL_FAST</i> flag should be true.  When the software is
 * ready for distribution, it should be set to true.  If a field report comes back with unusual behavior, search the log for any
 * messages that start with "&lt;ERROR!&gt;".
 * <li>An external user can't get the software to work.  Instruct them to call <code>setVerbose(true)</code>.  Then let them
 * examine the log to see if they can determine the issue, and likewise the log can be sent to developers. 
 * </ul>
 * 
 * Future work
 * <ul>
 * <li> Send this information to log files instead of the console.
 * </ul>
 */
public class Debug {

	public static final boolean FAIL_FAST = true;  //!!!!! set this to false for distribution; true for local debugging

	private static boolean VERBOSE = false; // note, this is not final.  There is a performance penalty for every call (see pln()) using this flag (even if it is false)
	
	/**
	 * Turn on output of messages helpful during debugging.  
	 * 
	 * @param b true, if verbose mode
	 */
	public static void setVerbose(boolean b) {
		VERBOSE = b;
	}

	public static boolean getVerbose() {
		return VERBOSE;
	}
	
	private static void output(String tag, String msg) {
		String[] lines = msg.split("\\n");
		System.out.flush(); // helps make sure any queued data also comes out.
		for (int i = 0; i < lines.length; i++) {
			System.err.print("<"+tag+"> ");
			System.err.println(lines[i]);
		}
	}
	
	/** 
	 * Output the <i>msg</i> to the console, indicating that an error has occurred.  If in 
	 * fail-fast mode, then the program will exit (perhaps
	 * with a stack trace).
	 * 
	 * @param msg message
	 */
	public static void error(String msg) {
		output("ERROR!", msg);
		if (FAIL_FAST) {
			//throw new AssertionError(msg);
			halt();
		}
	}

	/**
	 * Check if the <i>condition</i> is false, then indicate there
	 * is an error.  Also, in fail-fast mode, then the program will exit (perhaps
	 * with a stack trace).
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
	 * Check if the <i>condition</i> is <b>false</b>, then 
	 * output the msg to the console with the prepended <i>tag</i>.  This version is
	 * immune to the FAIL_FAST flag (see notes for Debug).
	 * 
	 * @param condition representation of the nominal or correct state (NO warning condition)
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void checkWarning(boolean condition, String tag, String msg) {
		if (! condition) {
			output(tag, msg);
		}
	}
	
	/**
	 * Output the <i>msg</i> to the console with the prepended <i>tag</i>.  This version is
	 * immune to the FAIL_FAST flag (see notes for Debug).
	 * 
	 * @param condition condition that is expected to be true
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void warning(String tag, String msg) {
		output(tag, msg);
	}
	
	/**
	 * Print out a debugging message <i>msg</i>, with each line prepended with tag.
	 * The output will only come out if Debug is in "verbose" mode.  This method
	 * behaves as if if was implemented as <code>checkWarning( ! VERBOSE, tag, msg);</code>
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg the debug message
	 */
	public static void pln(String tag, String msg) {
		checkWarning( ! VERBOSE, tag, msg);
	}


	/**
	 * Force the program to end and, on some platforms, print backtrace.
	 * In general, this method should not be used, use {@link error} instead.
	 *  */
	public static void halt() {
		Thread.dumpStack();
		System.exit(1);
	}
	
	public static void printTrace() {
		new Throwable().printStackTrace();
	}
		
	public static void printCallingMethod() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		System.out.println(" $$$ calling method = "+stackTrace[1].getMethodName()+" "+stackTrace[1].getLineNumber());  // 0 is this method
		System.out.println(" $$$                = "+stackTrace[2].getMethodName()+" "+stackTrace[2].getLineNumber());  // 0 is this method
	}


}

