/* 
 * SeparatedInput
 *
 * Contact: Jeff Maddalon (j.m.maddalon@nasa.gov), Cesar Munoz, George Hagen
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 *
 */

#ifndef SEPARATEDINPUT_H
#define SEPARATEDINPUT_H

#include "Units.h"
#include "ErrorLog.h"
#include "ErrorReporter.h"
#include "ParameterReader.h"
#include "Quad.h"
#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <map>
#include <stdexcept>

namespace larcfm {
   
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
  class SeparatedInput : public ParameterReader, ErrorReporter {
  public:
    static const int maxLineSize = 800;

    class SeparatedInputException : public std::logic_error
    {
    public:
      explicit SeparatedInputException (const std::string& s);
    };
    
    /** Create an "empty" separated input that can only store parameters */
    SeparatedInput();
    
    /** Create a new SeparatedInput from the given stream */
    explicit SeparatedInput(std::istream *fs);

    /** Copy Constructor.  This should not be used. */
    SeparatedInput(const SeparatedInput& x);

    /** Assignment Operator. */
    SeparatedInput& operator=(const SeparatedInput& x);

	/** 
	 * Sets the regular expression used to divide each line into columns.  If the supplied parameter 
	 * is not a valid regular expression, then the current delimiter is retained.  This should be set 
	 * before the first "readLine" is called. 
	 * @param delim 
	 * 
	 */
    void setColumnDelimiters(const std::string& delim);

	/**
	 * Return the regular expression used to divide each line into columns.
	 * @return pattern
	 */
    std::string getColumnDelimiters() const;

    void setFixedColumn(const std::string& widths, const std::string& nameList, const std::string& unitList);
      
	/** 
	 * Return the heading for the given column 
	 * @param i column index
	 * @return heading
	 */ 
    std::string getHeading(int i) const;
 
	/** Return the number of columns 
	 * @return number of columns 
	 */
    int size() const;

	/** 
	 * Find the index of the column with given heading.  If 
	 * the heading is not found, then -1 is returned.<p>
	 *  
	 * Note: If you are getting some oddly large indexes, there are probably some nonstandard characters in the input.
	 * 
	 * @param heading name of heading
	 * @return index of heading
	 */
    int findHeading(const std::string& heading) const;

	/** 
	 * Returns the units string for the i-th column. If an invalid 
	 * column is entered, then "unspecified" is returned. 
	 * @param i column index
	 * @return unit
	 */
    std::string getUnit(int i) const;

	/**
	 * Returns true if a line defining column units was detected.
	 * @return true means a units line exists
	 */
    bool unitFieldsDefined() const;

	/** If set to false, all read-in headers and parameters will be converted to lower case. 
	 * @param b false means ignore case
	 */
    void setCaseSensitive(bool b);

	/** If true, headers and parameters will be case sensitive.  If false, all headers and 
	 * parameters will be converted to lower case. 
	 * @return false if case is ignored 
	 */
    bool getCaseSensitive() const;
  

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
	void setQuoteCharacter(char q);

	/**
	 * Sets this object to formally read comma-separated-value (CSV) input.  This format the 
	 * delimiter is a comma and the quote character is a double-quote.  Any amount of 
	 * whitespace before or after a comma is removed.
	 * So the string a,  "b " ,c will be read three fields: a, b with a trailing space, and c.
	 * Like any quote character, if you to put the quote in the field, duplicate it, so the 
	 * line: a, b"", c will be three fields: a, b" and c. 
	 */
    void setCsv();

	void setQuoteCharacter(char q, const std::vector<char>& delims, const std::string& sub);

    /**
     * This returns 0 if no character is defined.
     * @return the character used as a quote
     */
    char getQuoteCharacter() const;

    ParameterData& getParametersRef();

    ParameterData getParameters() const;

	/**
	 * Return true if the column entry has some (nonempty) value, otherwise return false.
	 * A value of hyphen (-) is considered a lack of a value.
	 * @param i column index
	 * @return true if column has a value
	 */
    bool columnHasValue(int i) const;
  
	/**
	 * Returns the raw string of the given column read.
	 * @param i column index
	 * @return column value (as a string)
	 */
    std::string getColumnString(int i) const;
	
	/**
	 * Returns the value of the given column (as a double) in internal units.  This call will log errors, and has an arbitrary default value.
	 * @param i column index
	 * @return value of column
	 */
    double getColumn(int i) const;

	/** 
	 * Returns the value of the given column (as a double) in internal units.  If 
	 * no units were specified in the file, then this value is assumed to be
	 * in the given default units and an appropriate conversion takes place.  
	 * @param i column index
	 * @param default_unit unit, if none is specified
	 * @return value of column
	 */
	double getColumn(int i, const std::string& default_unit) const;


	/**
	 * Returns the value of the given column (as a double) in internal units.
	 * @param i column index
	 * @param verbose if true, log a message on errors, if false, fail silently and return an arbitrary value
	 * @param defaultValue this is the value returned if there is some error
	 * @return value of column
	 */
	double getColumn(int i, double defaultValue, bool verbose) const;

    /**
     * Reads a line of the input.  The first call to readLine will read the column headings, units, etc.
     * 
     * @return true if end of file
     */
    bool readLine();	
    
	/** Returns the number of the most recently read in line 
	 * @return line number 
	 * */
    int lineNumber() const;

	/** Return the last line read as a comma-delineated string 
	 * @return line 
	 */
    std::string getLine() const;

	/**
	 * Return the raw header information for the file.  
	 * This includes any comments or excess whitespace, before the column definition line.
	 * @return preamble
	 */
    std::string getPreambleImage() const;

    // ErrorReporter Interface Methods
    
    bool hasError() const {
      return error.hasError();
    }
    bool hasMessage() const {
      return error.hasMessage();
    }
    std::string getMessage() {
      return error.getMessage();
    }
    std::string getMessageNoClear() const {
      return error.getMessageNoClear();
    }    
    

  private:
    std::istream* reader;
    
    mutable ErrorLog error;
    bool header;         // header line read in
    std::vector<std::string> header_str;    // header line raw string
    bool bunits;          // units line read in
    bool first_bunits;          // units line read in
    std::vector<std::string> units_str;     // Units type
    std::vector<double> units_factor;  // Units conversion value
    std::vector<std::string> line_str;      // raw line

    bool fixed_width;    // Instead of using a delimiter, use fixed width columns
    std::vector<int> width_int;        // The width of columns

	char quoteCharacter; 	// If a non-empty value, use that character to delimit complex string tokens
	bool quoteCharDefined;	// because we don't have null

    int linenum;
    
    bool caseSensitive;
    
    std::string patternStr;
    
    std::string preambleImage;

//    typedef std::map<std::string, Quad<std::string,double,std::string,bool> > paramtype;
//    paramtype parameters;

    ParameterData parameters;
    
    double getUnitFactor(int i) const;
    bool process_units(const std::string& str);
    bool process_preamble(std::string str);
    void process_line(const std::string& str);
	
    std::vector<std::string> processQuotes(const std::string& str) const; 
	std::string readFullLine(std::istream* reader);  
  };
}

#endif // SEPARATEDINPUT_H
