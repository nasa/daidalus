/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.IO;

import gov.nasa.larcfm.Util.ErrorLog;
import gov.nasa.larcfm.Util.ErrorReporter;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.ParameterProvider;
import gov.nasa.larcfm.Util.ParameterReader;
import gov.nasa.larcfm.Util.Units;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This object will read in and store a set of configuration parameters.  The parameters can be listed 
 * in the conventional way, such as: <code>param = 10 [NM]</code>.  In addition, the parameters can
 * be listed in a table format: </p>
 * <pre>
 * param1 param2 param3
 * 1      5      100
 * 2      10     99 
 * 3      20     98
 * 4      10     97
 * 5      5      96
 * </pre>
 * 
 * <p>If the parameters are stored in this way, then every call to the "next()" method will update the parameters
 * (for this example, the parameters: param1, param2, and param3) with the next values in the file. </p>
 *   
 * <p>Instead of next(), the nextIterate() method can be called which will return all of the param1 values with
 * each of the param2 values, and so on.  In this case the parameters may require different numbers of
 * values; so, the columns of values should be augmented with an invalid value that will
 * be ignored, for example</p>
 * <pre>
 * param1 param2 param3
 * 1      5      100
 * 2      10     99 
 * 3      20     x
 * 4      x      x
 * 5      x      x
 * </pre>
 * 
 * <p>A special parameter called "importConfigFile = file.cfg"
 * will read the parameters from that file and include those parameters as if they were parameters in this file.
 * The file imported may also have an "importConfigFile" option.
 * Only one importConfigFile option is allowed per file.
 * Parameters in the parent file take precedence over the values in an imported file.
 * </p>
 * <p>
 * It is possible to programmatically add additional import keys to a reader through the addIncludeName() method, allowing for multiple potential imports per file.
 * The key importConfigFile is always first in the list of imports read, with others read in the order the keys were added. 
 * </p>
 */
public final class ConfigReader implements ParameterReader, ParameterProvider, ErrorReporter {
	private static final String INCLUDE_FILE = "importConfigFile";
	
	private ErrorLog error;
	private String[] param_var;
	private boolean[] param_isValue;
	private ArrayList<String[]> param_val;
	private int count;
	private int[] count_itr;
	private boolean caseSensitive;
	private ParameterData pd;
	private List<String> includeNames;
  
	private String preambleImage;
 
    /** A new, empty ConfigReader.  This may be used to store parameters, but nothing else. */
	public ConfigReader() {
		error = new ErrorLog("ConfigReader()");
		param_var = new String[0];
		param_isValue = new boolean[0];
		param_val = new ArrayList<String[]>(10);
		count_itr = new int[0];
		count = 0;
		caseSensitive = false;
		pd = ParameterData.make();
		preambleImage = "";
		includeNames = new ArrayList<String>(10);
		includeNames.add(INCLUDE_FILE);
	}

	/** 
	 * Read a new file into an existing ConfigReader.  
	 * Parameters are preserved if they are not specified in the file.
	 * 
	 * @param filename file name
	 */
	public void open(String filename) {
		if (filename == null || filename.equals("")) {
			error.addError("No filename given");
			return;
		}
		Reader fr;
		try {
			fr = new BufferedReader(new FileReader(filename));
			String srcPath = FileUtil.absolute_path("", filename);
			open(fr, srcPath);
			fr.close();
		} catch (FileNotFoundException e) {
			error.addError("File "+filename+" read protected or not found");
			if (param_var != null) {
				param_var = new String[0];
				param_val.clear();
			}
		} catch (IOException e) {
			error.addError("On close: "+e.getMessage());
			if (param_var != null) {
				param_var = new String[0];
				param_val.clear();
			}
		}
	}
	
	public void open(Reader r) {
		open(r, "");
	}
	
	public void open(Reader r, String srcPath) {
		if (r == null) {
			error.addError("null given for open(Reader)");
			return;
		}
		
		SeparatedInput si;
		si = new SeparatedInput(r);
		si.setCaseSensitive(caseSensitive);
		
		loadfile(si, srcPath);
		if (si.hasError()) {
			error.addError(si.getMessage());
		}
		if (si.hasMessage()) {
			error.addWarning(si.getMessage());
		}
		
		count = 0;
		for (int i=0; i < count_itr.length; i++) {
			count_itr[i] = 0;
		}
	}	

	private void processParameters(SeparatedInput input) {
		pd.copy(input.getParametersRef(), true);
		param_var = new String[input.size()];
		param_isValue = new boolean[input.size()];
		param_val = new ArrayList<>(10);
		count_itr = new int[input.size()];
		for (int i = 0; i < input.size(); i++) {
			if ( ! pd.contains(input.getHeading(i))) {
				pd.set(input.getHeading(i),"0.0 ["+input.getUnit(i)+"]");						
			}
			param_var[i] = input.getHeading(i);
			param_isValue[i] = true;
			if (Double.MAX_VALUE == Units.parse(input.getColumnString(i), Double.MAX_VALUE)) {
				param_isValue[i] = false;
			}
		}
	}
	
	private void processTableOfParameters(SeparatedInput input) {
		String[] values = new String[input.size()];
		for(int i = 0; i < input.size(); i++) {
			String s = input.getColumnString(i);
			String unit = input.getUnit(i);
			if (unit.equals("unspecified")) {
				unit = pd.getUnit(param_var[i]);
			}
			try {
				values[i] = s+" ["+unit+"]";
				Double.parseDouble(s);
			} catch (NumberFormatException e) {
				values[i] = s;  // if not a double, then just add the string
			}
		}
		param_val.add(values);			
	}
	
	private void loadfile(SeparatedInput input, String srcPath) {
		boolean hasReadParameters = false;
		input.setCaseSensitive(caseSensitive);
      
		while ( ! input.readLine()) {
			// look for each possible heading
			if ( ! hasReadParameters) {
				processParameters(input);
				hasReadParameters = true;
			} 

			processTableOfParameters(input);
		}
		if ( ! hasReadParameters) {
			pd.copy(input.getParametersRef(), true);
		}
		for (String name: includeNames) {
			if (pd.contains(name)) { // only attempt to read a file if the key exists in the source
				String file = FileUtil.file_search(pd.getString(name), srcPath, ".");
				if (file == null) {
					error.addWarning("Could not find include file '"+pd.getString(name)+"', ignoring.");
				} else {
					loadIncludeFile(file);
				}
			}
		}
		preambleImage = input.getPreambleImage();
		
	}
	
	private void loadIncludeFile(String file) {
		if (file == null || file.isEmpty()) {
			error.addWarning("Include file name was empty or null, ignoring.");
			return;
		}
		ConfigReader cr = new ConfigReader();
		cr.open(file);
		if (cr.hasError()) {
			error.addError(cr.getMessage());
		}
		if (cr.hasMessage()) {
			error.addWarning(cr.getMessage());
		}
		ParameterData p = cr.getParameters();
		pd.copy(p,false);
		for (String name: includeNames) {
			String file2 = FileUtil.file_search(pd.getString(name), FileUtil.get_path(file), ".");
			if (file2 == null) {
				error.addWarning("Could not find include file '"+pd.getString(name)+"', ignoring.");
			} else {
				if ( ! file.equals(file2)) {
					loadIncludeFile(file2);
				}
			}
		}
	}

	/**
	 * Add an additional import key to this reader.
	 * If the file read contains this key, the reader will interpret its value as a file name to be imported.
	 * Files are imported in the order these keys were defined.  
	 * In the case of import duplication, the earlier definitions will take precedence.
	 * @param name new import directive key
	 */
	public void addIncludeName(String name) {
		if (name != null && ! name.isEmpty()) {
			includeNames.add(name);			
		}
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * If set to true, headers and parameter keys will be stored in their input case.
	 * If set to false, headers and parameter keys will be stored in lower case.
	 * This should not affect ParameterData behavior, but it may make header matching more picky.
	 * Defaults to false.
	 * @param b
	 */
	public void setCaseSensitive(boolean b) {
		caseSensitive = b;
	}

    /** Return the number of parameters configurations in the file 
     * @return number of parameters
     * */
	public int size() {
		return param_var.length;
	}

	/** Gets the next set of parameters from a line in the file.  
	 * @return Returns true, if the end of file has been reached. 
	 */
	public boolean next() {
		if (count < param_val.size()) {
			String[] values = param_val.get(count);
			for (int i = 0; i < values.length; i++) {
				pd.set(param_var[i],values[i]);
			}
			count++;
			return false; // means not EOF
		}
		return true; // means EOF
	}

	/** Gets the next set of parameters.  If there is more than one column of parameters in the 
	 * file, then each member of one column are iterated through all members of all the other columns.
	 * @return Returns true, if the end of file has been reached. 
	 */
	public boolean nextIterate() {
		if (count_itr.length > 0 && count_itr[0] >= param_val.size()) {
			return true; // means EOF
		}
		
		// Get the parameters
		for (int j=0; j<count_itr.length; j++) {
			pd.set(param_var[j],param_val.get(count_itr[j])[j]);
		}
		
		// Update the counts, start at the rightmost column and move in
		//   Note, if it gets to this point, then a set of valid
		//   parameters have been loaded into the ParameterData structure.
		//   Therefore, from this point on, only a "false" should be returned
		//   from this call to nextIterate().  This means
		//   the user should use the parameters that have been set.
		//   The most this code should do is set it up so the next call to
		//   nextIterate() should return true (meaning there are no more
		//   iterations on parameters available).
		for (int j=count_itr.length-1; j >= 0; j--) {
			if (count_itr[j] < param_val.size()-1) { // there are more values in this column
				count_itr[j]++;

				// if a valid value, then we are done
				if ( ! param_isValue[j] || Double.MAX_VALUE != Units.parse(param_val.get(count_itr[j])[j], Double.MAX_VALUE)) {
					return false;
				}
			}
			// there are no more values in this column so reset the counter	
			count_itr[j] = 0;
		}
		if (count_itr[0] == 0) { // if we have reset the first column, then set it up so next call to nextIterate will return EOF
			count_itr[0] = param_val.size();
		}
		return false;
	}

	/** Return the number of columns. Must call "open" first 
	 * @return number of columns
	 * */ 
	public int getNumberColumns() {
		return param_var.length;
	}

	/** Return the heading for the given column.  Must call "open" first.
	 * 
	 * @param i column index
	 * @return heading for column i
	 * */ 
	public String getHeading(int i) {
      if (i < 0 || i >= param_var.length) {
        return "";
      }
		return param_var[i];
	}

	public String getPreambleImage() {
		return preambleImage;
	}
	
	/**
	 * Return the parameter database
	 */
	public ParameterData getParametersRef() {
		return pd;
	}

	public ParameterData getParameters() {
		return new ParameterData(pd);
	}

	public void updateParameterData(ParameterData p) {
		p.copy(pd,true);
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
	
}
