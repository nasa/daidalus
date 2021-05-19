/* 
 * SeparatedInput
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.IO;

import gov.nasa.larcfm.Util.Constants;
import gov.nasa.larcfm.Util.ErrorLog;
import gov.nasa.larcfm.Util.ErrorReporter;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.ParameterReader;
import gov.nasa.larcfm.Util.Units;

import java.io.Reader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * A class to parse files with input that are in columns 
 * separated by commas, spaces, or tabs.  <p>
 * 
 * General notes on the file:
 * <ul>
 * <li>All blank lines are skipped
 * <li>All lines that begin with a '#' are skipped
 * <li>The first section of the file contains parameters.  This
 * parameter section must be before the data
 * <li>The next section is the main data of the file.  It contains 
 * columns of data.
 * <li>The file can be read with case sensitive or insensitive strings.
 * Case sensitivity applies to header names and parameter keys (if case 
 * insensitive, these will be converted to lower case internally)
 * <li>The first line after the column name heading may be an optional line 
 * specifying nonstandard units for each column.  These are represented as 
 * [nmi], [fpm], [deg], etc.  If so defined, then values 
 * stored will be converted from these units into internal units.  
 * Otherwise it is assumed that values are already in internal units.  
 * If the default units as understood by the end user are not internal 
 * units (for example using degrees for latitude instead of radians), 
 * then further processing of the stored data may be necessary upon 
 * retrieval. Use [unspecified] for any column that has no applicable units 
 * (e.g. strings or booleans).
 * A dash (-) in the units field will be interpreted as "unitless".
 * </ul>
 * 
 * Notes on the columns of data
 * <ul>
 * <li>The columns of data may be separated by any number of commas, spaces, 
 * or tabs (but see {@link setColumnDelimiters})
 * <li>The first line of the columns of data is considered the "headings."
 * These are strings which can be accessed with the getHeading() method.
 * <li>The next line is optional.  It contains the a string representing the 
 * units for a column.  A line is considered a line of units if at least half 
 * of the fields read in are interpreted as valid units.
 * <li>The columns of data can be strings or double-precision floating point 
 * values
 * <li>If the values are doubles, then they are assumed to be in internal units.
 * If a unit type is specified, their values will be converted into internal units
 * and stored.  The end user is responsible for converting values if the unspecified
 * default units are something other than the internal ones.
 * <li>This format essentially follows RFC 4180 (when using commas)   
 * </ul>
 * 
 * Parameters are essentially a "key/value" pair.  Once an parameter is entered
 * (either from the setParameter method or from the file), then that parameter
 * name is reserved.  If any future updates to the parameter (either from 
 * the setParameter method or the file), then the previous value will be 
 * overwritten.  Parameters have the following rules:
 * <ul>
 * <li>Parameters have the basic form "key = value"
 * <li>Parameter keys can be any combination of letters and numbers.
 * <li>There must be spaces around the equals sign
 * <li>The value may be a string, a double-precision value, or a boolean value.
 * <li>If the value is a double precision value, then units may be added.  
 * For example,  "param = 10 [nmi]"
 * <li>The units are optional.
 * <li>Boolean values are represented by T, F, true, false, TRUE, or FALSE.
 * <li>If a parameter has not been set and it is read, something will be 
 * returned.  It is not defined what that something is.  To avoid this,
 * check is a parameter exists with the containsParameter() method.
 * </ul>
 */
public final class SeparatedInput implements ParameterReader, ErrorReporter {

	private static class SeparatedInputException extends Exception {
		static final long serialVersionUID = 0;

		SeparatedInputException(String name) {
			super(name);
		}
	}


	private LineNumberReader reader;

	private ErrorLog error;

	private boolean header;         // header line read in
	private String[] header_str;    // header line raw string
	private boolean bunits;         // has the units line (line after header) been read? (not does this file have a units line)
	private String[] units_str;     // Units type
	private double[] units_factor;  // Units conversion value
	private String[] line_str;      // raw line

	private boolean fixed_width;    // Instead of using a delimiter, use fixed width columns
	private int[] width_int;        // The width of columns

	private boolean caseSensitive;

	private Character quoteCharacter; 	// If a non-empty value, use that character to delimit complex string tokens

	private String patternStr;

	private StringBuilder preambleImage; // this is an image of all parameters and comments in the original preamble (not the column labels or units, if present)

	private ParameterData parameters;

	/** Create an "empty" separated input that can only store parameters */
	public SeparatedInput() {
		reader = null;
		header = false;
		bunits = false;
		error = new ErrorLog("SeparatedInput");
		parameters = ParameterData.make();
		caseSensitive = true;
		quoteCharacter = null;
		patternStr = Constants.wsPatternBase;
		fixed_width = false;
		header_str = new String[0];
		preambleImage = new StringBuilder();
	}

	/** 
	 * Create a new SeparatedInput from the given reader 
	 * @param r 
	 */
	public SeparatedInput(Reader r) {
		reader = new LineNumberReader(r);
		header = false;
		bunits = false;
		error = new ErrorLog("SeparatedInput(Reader)");
		parameters = ParameterData.make();
		caseSensitive = true;
		quoteCharacter = null;
		patternStr = Constants.wsPatternBase;
		fixed_width = false;
		header_str = new String[0];
		preambleImage = new StringBuilder();
	}

	/** 
	 * Return the heading for the given column 
	 * @param i column index
	 * @return heading
	 */ 
	public String getHeading(int i) {
		if (i < 0 || i >= header_str.length) { // was line_str.length
			error.addWarning("getHeading index "+i+", line "+reader.getLineNumber()+" out of bounds");
			return "";
		}
		return header_str[i];
	}

	/** Return the number of columns 
	 * @return number of columns 
	 */ 
	public int size() {
		return header_str.length;
	}

	/** 
	 * Find the index of the column with given heading.  If 
	 * the heading is not found, then -1 is returned.<p>
	 *  
	 * Note: If you are getting some oddly large indexes, there are probably some nonstandard characters in the input.
	 * 
	 * @param headings list of one or more alternate names for the heading
	 * @return index of heading
	 */
	public int findHeading(String... headings) {
		for (String heading : headings) {
			if ( ! caseSensitive) {
				heading = heading.toLowerCase();
			}
			for (int i = 0; i < header_str.length; i++) {
				String heading_col = (caseSensitive ? header_str[i] : header_str[i].toLowerCase());
				if (heading.equals(heading_col)) {
					return i;
				}
			}
		}
		return -1;
	}


	/** 
	 * Returns the units string for the i-th column. If an invalid 
	 * column is entered, then "unspecified" is returned. 
	 * @param i column index
	 * @return unit
	 */
	public String getUnit(int i) {
		if ( i < 0 || i >= units_str.length) {
			return "unspecified";
		}
		return units_str[i];
	}

	private double getUnitFactor(int i) {
		if ( i < 0 || i >= units_str.length) {
			return Units.unspecified;
		}
		return units_factor[i];
	}

	/** If set to false, all read-in headers and parameters will be converted to lower case. 
	 * @param b false means ignore case
	 */
	public void setCaseSensitive(boolean b) {
		caseSensitive = b;
	}

	/** If true, headers and parameters will be case sensitive.  If false, all headers and 
	 * parameters will be converted to lower case. 
	 * @return false if case is ignored 
	 */
	public boolean getCaseSensitive() {
		return caseSensitive;
	}  

	/**
	 * <p>This sets the 'quote' character.  The quote character is a way to contain a delimiter within a field of the data.
	 * So if the delimiter is a comma and ' is the quote character, then 'hello, folks' is a valid field.</p>
	 * 
	 * <p>A quote character does not need to be set, the default is null, meaning, don't use a quote characters.  
	 * </p>
	 * 
	 * <p>A quote character can be put in the field by duplicating it.  So if the delimiter is a comma and ' is a quote character, then
	 * ''hello'', fol''ks will be two fields: 'hello' and fol'ks.  Be careful with nested quotes 'a''b' is interpreted as a'b not as two 
	 * quoted strings: a and b.  However, 'a' 'b' will be interpreted as the string: a b
	 * </p>
	 * 
	 * Notes
	 * <ul>
	 * <li>Quotes do not apply to parameters
	 * <li>This will generate an error if the quote character matches the delimiters
	 * <li>This is not used for parameters or fixed-width columns.
	 * </ul>
	 * 
	 * @param q quotation character, if set to null do not treat quote characters specially
	 */
	public void setQuoteCharacter(Character q) {
		// the easy case
		if (q == null) {
			quoteCharacter = null;
			return;
		}
		if (q.toString().matches(patternStr)) {
			error.addError("setQuoteCharacter: quote character is in list of column delimiters.");
			quoteCharacter = null; // ignore this setQuoteCharacter()
			return;				
		}
		quoteCharacter = q;
	}

    /**
     * This returns 0 if no character is defined.
     * @return the character used as a quote
     */
	public Character getQuoteCharacter() {
		return quoteCharacter;
	}

	public ParameterData getParametersRef() {
		return parameters;
	}

	/**
	 * Return true if the column entry has some (nonempty) value, otherwise return false.
	 * A value of hyphen (-) is considered a lack of a value.
	 * @param i column index
	 * @return true if column has a value
	 */
	public boolean columnHasValue(int i) {
		return (i >= 0 && i < line_str.length && !line_str[i].equals("") && !line_str[i].equals("-"));    	
	}

	/**
	 * Returns the raw string of the given column read.
	 * @param i column index
	 * @return column value (as a string)
	 */
	public String getColumnString(int i) {
		if (i < 0 || i >= line_str.length) {
			error.addWarning("getColumnString index "+i+", line "+reader.getLineNumber()+" out of bounds");
			return "";
		}
		return line_str[i];
	}

	/**
	 * Returns the value of the given column (as a double) in internal units.
	 * @param i column index
	 * @param verbose if true, log a message on errors, if false, fail silently and return an arbitrary value
	 * @param defaultValue this is the value returned if there is some error
	 * @return value of column
	 */
	public double getColumn(int i, double defaultValue, boolean verbose) {
		if (i < 0 || i >= line_str.length) {
			if (verbose) error.addWarning("getColumn index "+i+", line "+reader.getLineNumber()+" out of bounds");
			return defaultValue;
		}
		double rtn = defaultValue;
		try {
			rtn = Units.from(getUnitFactor(i), Double.parseDouble(line_str[i]));
		} catch (NumberFormatException e) {
			if (verbose) error.addWarning("could not parse as a double in getColumn("+i+"), line "+reader.getLineNumber()+": "+line_str[i]);
		}
		return rtn;
	}

	/**
	 * Returns the value of the given column (as a double) in internal units.  This call will log errors, and has an arbitrary default value.
	 * @param i column index
	 * @return value of column
	 */
	public double getColumn(int i) {
		return getColumn(i, 0.0, true);
	}

	/** 
	 * Returns the value of the given column (as a double) in internal units.  If 
	 * no units were specified in the file, then this value is assumed to be
	 * in the given default units and an appropriate conversion takes place.  
	 * @param i column index
	 * @param default_unit unit, if none is specified
	 * @return value of column
	 */
	public double getColumn(int i, String default_unit) {
		if (getUnit(i).equals("unspecified")) {
			return Units.from(default_unit, getColumn(i, 0.0, true));
		}
		return getColumn(i, 0.0, true);
	}

	/** 
	 * Sets the regular expression used to divide each line into columns.  If the supplied parameter 
	 * is not a valid regular expression, then the current delimiter is retained.  This should be set 
	 * before the first "readLine" is called. 
	 * @param delim 
	 * 
	 */
	public void setColumnDelimiters(String delim) {
		try {
			String s = "This is a test";
			s.split(delim);  // ignore output, just checking for an exception
			patternStr = delim;
		} catch (PatternSyntaxException e) {
			error.addWarning("invalid delimiter string: "+delim+" retained original: "+patternStr);
			// ignore the attempt to set an invalid delimiter string
		}
	}
	
	/**
	 * Return the regular expression used to divide each line into columns.
	 * @return pattern
	 */
	public String getColumnDelimiters() {
		return patternStr;
	}

	/**
	 * Sets this object to formally read comma-separated-value (CSV) input.  This format the 
	 * delimiter is a comma and the quote character is a double-quote.  Any amount of 
	 * whitespace before or after a comma is removed.
	 * So the string a,  "b " ,c will be read three fields: a, b with a trailing space, and c.
	 * Like any quote character, if you to put the quote in the field, duplicate it, so the 
	 * line: a, b"", c will be three fields: a, b" and c. 
	 */
	public void setCsv() {
		setColumnDelimiters("[ \t]*,[ \t]*");
		setQuoteCharacter('"');
	}

	public void setFixedColumn(String widths, String nameList, String unitList) {
		try {
			String[] fields;
			fields = widths.split(",");
			width_int = new int[fields.length];
			for (int i = 0; i < fields.length; i++) {
				width_int[i] = Integer.parseInt(fields[i]);
			}

			fields = nameList.split(",");
			if (width_int.length != fields.length) {
				throw new SeparatedInputException("In parsing fixed width file, number of names does not match number of widths");
			}
			header_str = new String[fields.length];
			System.arraycopy(fields, 0, header_str, 0, fields.length);

			fields = unitList.split(",");
			if (width_int.length != fields.length) {
				throw new SeparatedInputException("In parsing fixed width file, number of units does not match number of widths");
			}
			units_str = new String[fields.length];
			units_factor = new double[fields.length];
			for (int i = 0; i < fields.length; i++) {
				if (Units.isUnit(fields[i])) {
					units_str[i] = fields[i];
					units_factor[i] = Units.getFactor(fields[i]);	
				} else if (fields[i].equals("-")) {
					units_str[i] = "unitless";
					units_factor[i] = Units.unitless;
				} else {
					units_str[i] = "unspecified";
					units_factor[i] = Units.unspecified;
				}
			}

			fixed_width = true;
			header = true;
			bunits = true;
		} catch (SeparatedInputException e) {
			error.addError(e.getMessage());
		}
	}

	public void skipLines(int numLines) {
		if (reader == null) return;
		try {
			for (int i = 0; i < numLines; i++) {
				if (reader.readLine() == null) {
					break;
				}
			}
		} catch (IOException e) { 
			error.addError("*** An IO error occured at line "+reader.getLineNumber()
			+ "The error was:"+e.getMessage());
		}
	}

	private List<String> processQuotes(String str) {
		List<String> rtn = new ArrayList<>();
		StringBuilder temp = new StringBuilder((int)(str.length()*1.1));
		
		if (quoteCharacter == null) return rtn;
		boolean squote = true;  // true means the next quote character is a starting quote
		String[] fields_i = str.split("["+quoteCharacter+"]["+quoteCharacter+"]");
		for (int i=0; i<fields_i.length; i++) {
		    String str_i = fields_i[i];
			if ( ! str_i.isEmpty()) {
				String[] fields_j = str_i.split("["+quoteCharacter+"]",-1);
				for (int j=0; j<fields_j.length; j++) {
					String str_j =fields_j[j];
					if ( ! str_j.isEmpty()) {
						if (! squote) {
							temp.append(str_j);
						} else {
							String[] fields_k = str_j.split(patternStr,-1);
							for (int k=0; k<fields_k.length; k++) {
								String str_k = fields_k[k];
								temp.append(str_k);
								if (fields_k.length - 1 != k) {
									rtn.add(temp.toString());
									temp.setLength(0);
								}
							}
						}						
					}
					if (fields_j.length>1 && fields_j.length - 1 != j) {
						squote = ! squote;
					}  
				}
			}
			if (fields_i.length - 1 != i) {
				temp.append(quoteCharacter.toString());
			}
		}
		if ( temp.length() != 0) {
			rtn.add(temp.toString());
		}
		return rtn;
	}

	/**
	 * Reads a line of the input.  The first call to readLine will read the column headings, units, etc.
	 * 
	 * @return true if end of file
	 */
	public boolean readLine() {
		String str = null;
		try {
			while ((str = readFullLine(reader)) != null) {
				String lineRead = str + System.lineSeparator();

				// Remove comments from line
				int comment_num = str.indexOf('#');
				if (comment_num >= 0) {
					str = str.substring(0,comment_num);
				}
				str = str.trim();
				
				if ( str.isEmpty()) {
					if ( ! header) preambleImage.append(lineRead); // store the line just read
					continue;
				}
				
				if ( ! header) {
					header = process_preamble(str);
					if ( ! header) preambleImage.append(lineRead); // store the line just read
					continue;
				} 
				
				boolean hasUnits = false;
				if ( ! bunits) {
					bunits = true;
					hasUnits = process_units(str);  // if false, then this is a regular line, so use default units
				}
				if ( ! hasUnits) {
					process_line(str);
					break;
				}
			}//while    
		} catch (IOException e) { 
			error.addError("*** An IO error occured at line "+reader.getLineNumber()
			+ "The error was:"+e.getMessage());
			str = null;
		}
		return (str == null);
	}
	
	private String readFullLine(LineNumberReader r) throws IOException {
		StringBuilder t1 = new StringBuilder(100);
		String v = r.readLine(); 
		t1.append(v);
		if (v == null || quoteCharacter == null) return v;
			
		do {
			int count = 0;
			for (int i = 0; i < t1.length(); i++) {
				if (t1.charAt(i) == quoteCharacter) count++;
			}
			if (count %2 == 0) break;
			String t2 = r.readLine();
			if (t2 == null) break;
			t1.append(System.lineSeparator());
			t1.append(t2);
		} while (true);
		return t1.toString();
	}

	/** Returns the number of the most recently read in line 
	 * @return line number 
	 * */
	public int lineNumber() {
		return reader.getLineNumber();
	}

	private boolean process_preamble(String str) {
		String[] fields = str.split("=",2);

		// parameter keys are lower case only
		if (fields.length == 2 && fields[0].length() > 0) {
			String id = fields[0].trim();
			if ( ! caseSensitive) id = id.toLowerCase();
			parameters.set(id,fields[1].trim());
			return false;
		} else if (fields.length == 1 && str.contains("=")) {
			String id = fields[0].trim();
			parameters.set(id,"");
			return false;
		} else {
			fields = str.split(patternStr);
			if ( ! caseSensitive) {
				for (int i = 0; i < fields.length; i++) {
					fields[i] = fields[i].toLowerCase().trim();
				} 
			}
			header_str = fields;
			return true;
		} 
	}
	
	private boolean process_units(String str) {
		String[] fields = str.split(patternStr);

		// if units are optional, we need to determine if any were read in...
		// A line is considered a unit line when either (A) AT LEASE HALF of the fields 
		// are valid or (B) not all the units are invalid or dashes,  
		// a dash ('-') is an abbreviation meaning unitless.
		int notFound = 0;
		int dash = 0;

		units_str = new String[fields.length];
		units_factor = new double[fields.length];
		for (int i=0; i < fields.length; i++) {
			// interpret dash
			String fstr = fields[i];
			if (fstr.trim().equals("-")) {
				fstr = "unitless";
				dash++;
			}
			units_str[i] = Units.clean(fstr);
			units_factor[i] = Units.getFactor(units_str[i]); // should never throw exception since this comes from Units.clean
			if (units_str[i].equals("unspecified")) {
				notFound++;
			}
		}
		if (notFound > fields.length/2 || dash+notFound == fields.length) {
			if (dash > 0 && dash+notFound == fields.length) {
				for (int i = 0 ; i < units_str.length ; ++i) {
					if (units_str[i].equals("unitless")) {
						units_str[i] = "unspecified";
					}
				}
			}
			return false;
		}
		return true;
	}

	private void process_line(String str) {
		String[] fields;
		if (fixed_width) {
			int idx = 0;
			fields = new String[width_int.length];
			for (int i = 0; i < width_int.length; i++) {
				int end = idx+width_int[i];
				if (idx < str.length() && end <= str.length()) {
					fields[i] = str.substring(idx, idx+width_int[i]);					
				} else if (idx < str.length() && end > str.length()) {
					fields[i] = str.substring(idx, str.length());										
				} else {
					fields[i] = "";
				}
				idx = idx + width_int[i];
			}
		} else {
			if (quoteCharacter != null) {
				List<String> f = processQuotes(str);
				fields = new String[f.size()];
				int count = 0;
				for (String s: f) {
					fields[count++] = s;
				}
			} else {
				fields = str.split(patternStr);
			}
		}		
		line_str = fields;
	}

	/** Return the last line read as a comma-delineated string 
	 * @return line 
	 */
	public String getLine() {
		String s = "";
		if (line_str.length > 0) {
			s = line_str[0];
		}
		for (int i = 1; i < line_str.length; i++) {
			s += ", "+line_str[i].trim();
		}
		return s;
	}

	public void close() {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				error.addError("IO Exception in close(): "+e.getMessage());
			}
		}
	}

	/**
	 * Return the raw header information for the file.  
	 * This includes any comments or excess whitespace, before the column definition line.
	 * @return preamble
	 */
	public String getPreambleImage() {
		return preambleImage.toString();
	}

	// ErrorReporter Interface Methods

	public boolean hasError() {
		return error.hasError();
	}
	public boolean hasMessage() {
		return error.hasMessage();
	}
	public String getMessage() {
		return error.getMessage();
	}
	public String getMessageNoClear() {
		return error.getMessageNoClear();
	}


	public String toString() {
		StringBuilder str = new StringBuilder(100);
		str.append("SeparateInput: \n header_str:");
		for (int i=0; i < header_str.length; i++) {
			str.append(", ");
			str.append(header_str[i]);  
		}

		str.append("\n units_str:");
		for (int i=0; i < units_str.length; i++) {
			str.append(", ");
			str.append(units_str[i]);  
		}

		str.append("\n line_str:");
		for (int i=0; i < line_str.length; i++) {
			str.append(", ");
			str.append(line_str[i]);  
		}

		return str.toString();
	}

}
