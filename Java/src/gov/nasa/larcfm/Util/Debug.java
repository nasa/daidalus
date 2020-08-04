/*
 *  Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * Copyright (c) 2013-2019 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.util.function.Supplier;

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
 * <li>Error - Usually indicates a software error.  Something where the program is confused.  The requested operation will need to be ignored 
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
 * <li><code>pln(lvl, msg)</code> This provides information at a user-specified level (>= 2).  Only the message comes out, there is no "tag"
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

	public static final boolean FAIL_FAST = true;  //!!!!! set this to false for distribution; true for local debugging
	private static int VERBOSE = 1;     // this is 0 for distribution, 1 for general development, 2 for local debugging.  However, this is not final.  
//	private static final boolean INCLUDE_METHOD_NAME = false; // if set to true, this will look at the current stack trace and include the calling method's name
	private static StringBuffer buffer = null;
	
	/**
	 * Set the verbosity level for debuggging
	 * <ul>
	 * <li> 0 - Errors only
	 * <li> 1 - Errors and Warnings
	 * <li> 2 - Errors, Warnings, and Status
	 * <li> >2 - All the above, plus user-specified levels
	 * </ul>
	 * 
	 * @param level verbosity level
	 */
	public static void setVerbose(int level) {
		if (level < 0) level = 0;
		VERBOSE = level;
	}

	public static void setBuffer(boolean use) {
		if (use) {
			buffer = new StringBuffer(1024);
		} else {
			buffer = null;
		}
	}
	
	public static String getBuffer() {
		String ret = buffer.toString();
		buffer.setLength(0);
		return ret;
	}
	
	/**
	 * Return the verbosity level
	 * <ul>
	 * <li> 0 - Errors only
	 * <li> 1 - Errors and Warnings
	 * <li> 2 - Errors, Warnings, and Status
	 * <li> >2 - All the above, plus user-specified levels
	 * </ul>
	 * 
	 * @return verbosity level
	 */
	public static int getVerbose() {
		return VERBOSE;
	}
	
	// Returns "simpleClassName.MethodName(lineNumber)" of calling method (outside of Debug)
	private static String getCallingMethodName() {
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		for (int i = 2; i < stes.length; i++) { // 0 is getStackTrace, 1 is this method
			String className = stes[i].getClassName();
			if (!className.startsWith("gov.nasa.larcfm.Util.Debug")) {
				String methodName = stes[i].getMethodName();
				String simpleClassName = className.substring(className.lastIndexOf('.')+1);
				return simpleClassName+"."+methodName+"("+stes[i].getLineNumber()+")";
			}
		}
		return "";
	}
	
	private static void output(String tag, String msg) {
		String[] lines = msg.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			if ( ! tag.isEmpty()) {
				if (buffer == null) {
					System.out.print("<");
					System.out.print(tag);
					System.out.print("> ");					
				} else {
					buffer.append("<");
					buffer.append(tag);
					buffer.append("> ");
				}
			}
			if (buffer == null) {
				System.out.println(lines[i]);
			} else {
				buffer.append(lines[i]);
				buffer.append(System.lineSeparator());
			}
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
		output("ERROR!", msg);
		if (fail_fast) {
			halt();
		}
	}
	
	/**
	 * <p>Check if the <i>condition</i> is false, then output the error message.
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

	public static void checkErrorLazy(boolean condition, Supplier<String> f) {
		if (! condition) {
			error(f.get());
		}
	}
	
	/**
	 * Potentially output the <i>msg>/i> to the console with the prepended <i>tag</i>.  This method
	 * is not subject to either the FAIL_FAST or the VERBOSE flag (see notes for Debug).
	 * 
	 * @param condition representation of the nominal or correct state (NO warning condition).
	 *                  Thus, if the <i>condition</i> is <b>false</b>, then output.
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void checkWarning(boolean condition, String tag, String msg) {
		pln(tag, msg, ! condition);
	}
	
	/**
	 * Output the <i>msg</i> to the console with the prepended <i>tag</i>.  Warnings are always
	 * output and never cause a program termination.
	 * 
	 * @param tag the tag to indicate the location of this debug message.
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void warning(String tag, String msg) {
		if (VERBOSE > 0) {
			output(tag, msg);
		}
	}
	
	/**
	 * Output the <i>msg</i> to the console with the prepended <i>WARNING</i> tag.  Warnings are always
	 * output and never cause a program termination.
	 * 
	 * @param msg message to indicate what has gone wrong.
	 */
	public static void warning(String msg) {
		warning("WARNING", msg);
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
		if (verbose) {
			output(tag, msg);
		}
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
	 * class MyClass implements Supplier<String> {
	 *   @override
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
			output(tag, f.get());
		}
	}
	
	/**
	 * Print out a status message <i>msg</i>.
	 * The output will only come out if Debug is in "verbose" mode.  
	 * 
	 * @param msg the status message
	 * @param verbose if true, the display status message
	 */
	public static void pln(String msg, boolean verbose) {
		//f.pln(" $$$$$$$$$$$ HERE I AM verbose = "+verbose+" msg = "+msg);
		pln("", msg, verbose);
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
		pln(2, tag, msg);
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
		pln(2, "", msg);
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
		pln(tag, msg, VERBOSE > 1 && VERBOSE >= level);
		
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
	 * class MyClass implements Supplier<String> { <br>
	 *   public String get() { <br>
	 *     return complex_string_construction_operation_here(); <br>
	 *   }<br>
	 * }<br>
	 * </code>
	 * 
	 * @param level verbosity level
	 * @param tag the tag to indicate the location of this debug message.
	 * @param f a supplier object that contains the status message
	 */
	public static void plnLazy(int level, String tag, Supplier<String> f) {
		plnLazy(tag, f, VERBOSE > 1 && VERBOSE >= level);
		
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
		pln(level, "", msg);
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
	 * class MyClass implements Supplier<String> {
	 *   @override
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
		plnLazy(level, "", f);
		
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
		
	public static void printCallingMethod() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		System.out.println(" $$$ calling method = "+stackTrace[1].getMethodName()+" "+stackTrace[1].getLineNumber());  // 0 is this method
		System.out.println(" $$$                = "+stackTrace[2].getMethodName()+" "+stackTrace[2].getLineNumber());  // 0 is this method
	}


}

