/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.IO;

import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** A collection of utility functions useful for files */
public final class FileUtil {

	  /**
	   * Given a file name, return the path of this name.
	   * This also converts backward slashes to forward slashes.
	   * @param filename the name of the file
	   * @return the path of the file, if no path, then an empty string is returned.
	   */
	  public static String get_path(String filename) {
		  return filename.substring(0,filename.replaceAll("\\\\", "/").lastIndexOf("/") + 1);
	  }
	  
	  /**
	   * Given a file name return the extension of this name.
	   * @param filename the name of the file
	   * @return the extension--part of filename after last period (.)
	   */
	  public static String get_extension(String filename) {
		  return filename.substring(filename.lastIndexOf(".") + 1);
	  }
	  
	  /**
	   * Given a file name return the Java version of this name.
	   * This converts backward slashes to forward slashes.
	   * @param filename the name of the file
	   * @return the Java-native version of the filename
	   */
	  public static String unifyFileName(String filename) {
		  return filename.replaceAll("\\\\", "/");
	  }
	  
	/**
	 * Returns the base (no path) file name. 
	 * <ul> 
	 * <li>a/b/c.txt = c.txt
	 * <li>a.txt = a.txt
	 * <li>a/b/ = ""
	 * </ul>
	 * This also converts backward slashes to forward slashes.
	 * @param filename  filename string
	 * @return filename without path information
	 */
	public static String no_path(String filename) {
		if (filename == null) {
			return null;
		}
		// replace all \ (dos) with / (unix), then find last
		return filename.substring(filename.replaceAll("\\\\", "/").lastIndexOf("/") + 1);
	}

	/**
	 * Returns the file name without an extension (such as, ".txt").  Also removes path information. 
	 * @param filename  filename string
	 * @return filename without extension information
	 */
	public static String no_extension(String filename) {
		if (filename == null) {
			return null;
		}
		// replace all \ (dos) with / (unix), then find last
		String newname = no_path(filename);
		return newname.substring(0,newname.lastIndexOf("."));
	}

	/**
	 * Return an absolute path string to file targ that is relative to the src file's path.
	 * If targ is already absolute, return itself, otherwise expand targ's path.
	 * If src cannot be parsed, it is treated as the current location.
	 * If src is a file that exists, src is treated as that file's directory.
	 * Otherwise src is treated as a directory.
	 * (Keep in mind that this resolves targ against the absolute path to src.
	 * Also this uses the lexical rules of the file system and the result may not fit in with 
	 * the actual directory structure.)  
	 * This may be needed if you are loading files from a configuration file.
	 * @param src the source file to be referenced
	 * @param targ the target file to construct the absolute path to.
	 * @return absolute path to targ
	 */
	public static String absolute_path(String src, String targ) {
		Path targPath1 = Paths.get(targ, new String[0]);
		if (targPath1.isAbsolute()) return targ;
		if (src == null || src.equals("")) src = ".";
		Path srcPath = Paths.get(src, new String[0]).toAbsolutePath(); // treat as directory
		if (file_exists(src)) {
			srcPath = Paths.get(src, new String[0]).toAbsolutePath().getParent(); // get parent
		}
		Path targPath2 = srcPath.resolve(targ).toAbsolutePath().normalize();
		return targPath2.toString();
		// if there are any problems, comment out the above and uncomment this line:
		//return targ;
	}
	
	/**
	 * Return the absolute path to the referred to directory.
	 * If src is an existing readable file, return it's parent directory.
	 * @param src directory name
	 * @return absolute path
	 */
	public static String dir_absolute_path(String src) {
		if (src == null || src.equals("")) src = ".";
		Path srcPath = Paths.get(src, new String[0]).toAbsolutePath(); // treat as directory
		if (file_exists(src)) {
			srcPath = Paths.get(src, new String[0]).toAbsolutePath().getParent(); // get parent
		}
		return srcPath.toString();
	}

	/**
	 * Determine if the file exists, as a file.  
	 * Directories (i.e., folders) are not considered files.
	 * 
	 * @param name filename, possibly including a path.
	 * @return true, it the file exists
	 */
	public static boolean file_exists(String name) {
		boolean ret = false;
		File file = new File(name);
		if (file.canRead() && ! file.isDirectory()) {
			ret = true;
		}
		file = null;

		return ret;
	}

	/**
	 * Determine if a directory exists.  
	 * 
	 * @param name directory name, possibly including a path.
	 * @return true, it the directory exists (and can be read)
	 */
	public static boolean is_directory(String name) {
		boolean ret = false;
		File file = new File(name);
		if (file.canRead() && file.isDirectory()) {
			ret = true;
		}
		file = null;

		return ret;
	}

	/**
	 * Search for a filename in a list of possible locations.  The user must include the current directory in the search if it is desired.
	 * @param fileName filename to look for, possibly including path
	 * @param searchPaths list of directories to search relative to.  If an element refers to a file that exists, then that file's directory will be used.
	 * @return string with absolute path to the file found, or null if none found
	 */
	public static String file_search(String fileName, List<String> searchPaths) {
		for (int i = 0; i < searchPaths.size(); i++) {
			String absName = absolute_path(searchPaths.get(i), fileName);
			if (file_exists(absName)) {
				return absName;
			}
		}
		return null;
	}

	/**
	 * Search for a filename in one of several possible locations: 
	 * <ul>
	 * <li> relative to the srcFile,
	 * <li> in a <tt>subdir</tt> relative to srcFile, 
	 * <li> in a <tt>subDir</tt> relative to the current dir,
	 * <li> in a <tt>\scenarios\subDir</tt> relative to the current dir, or 
	 * <li> the current dir.
	 * </ul>
	 * @param fileName filename of file one is looking for
	 * @param srcFile source filename
	 * @param subdir sub-directory
	 * @return path to filename, or null if not found
	 */
	public static String file_search(String fileName, String srcFile, String subdir) {
		ArrayList<String> paths = new ArrayList<String>();
		paths.add(srcFile);
		paths.add(dir_absolute_path(srcFile)+File.separator+subdir);
		paths.add("."+File.separator+subdir);
		paths.add("."+File.separator+"scenarios"+File.separator+subdir);
		paths.add(".");
		return file_search(fileName, paths);
	}

	/**
	 * Given a filename (perhaps with a path), return the next filename, that does not already exist.
	 * If the files test_1.txt test_2.txt and test_3.txt exist, then get_next_filename("test_1.txt") will
	 * return "test_4.txt".
	 * 
	 * @param name the candidate file name
	 * @return the next file name
	 */
	public static String get_next_filename(String name) {
		String path = get_path(name);
		String ext = "."+get_extension(name);
		
		String short_name = no_extension(no_path(name));
		int idx = short_name.lastIndexOf("_");
		
		String base = short_name;
		int num = 0;
		
		if ( idx >= 0) { // is there an _ ?
			if (short_name.length() - 1 == idx) { // is _ the last character?
				base = short_name.substring(0,idx); // strip off final _
				num = 0;
			} else {
				String number = short_name.substring(idx+1);
				if (Util.is_int(number)) { // is the stuff after the _ a number?
					base = short_name.substring(0,idx);
					num = Util.parse_int(number);
				} 
			}
		}
		
		String new_name;
		do { 
			new_name = path + base + "_" + num + ext;
			num++;
		} while ( file_exists(new_name) );
		
		return new_name;		
	}

	  
	  /** Given a list of names that may include files or directories,
	   * return a list of files that contains (1) all of the files in
	   * the original list and (2) all files ending with given extension in
	   * directories from the original list. 
	   * 
	   * @param names a list of names of files or directories
	   * @param extension the extension to search the directory for (include the period, use ".txt" not "txt")
	   * @return a list of file names
	   */
	  public static List<String> getFileNames(String[] names, String extension) {
		  ArrayList<String> txtFiles = new ArrayList<String>(names.length);
		  for (int i=0; i < names.length; i++) {
			  txtFiles.addAll(getFileName(names[i], extension));
		  }
		  return txtFiles;	  
	  }
	  
	  /** Given a list of names that may include files or directories,
	   * return a list of files that contains (1) all of the files in
	   * the original list and (2) all files ending with given extension in
	   * directories from the original list. 
	   * 
	   * @param names a list of names of files or directories
	   * @param extension the extension to search the directory for (include the period, use ".txt" not "txt")
	   * @return a list of file names
	   */
	  public static List<String> getFileNames(List<String> names, String extension) {
		  ArrayList<String> txtFiles = new ArrayList<String>(names.size());
		  for (String name: names) {
			  txtFiles.addAll(getFileName(name, extension));
		  }
		  return txtFiles;	  
	  }
	  
	  /** 
	   * Given a name that may be a file or directory,
	   * return a list of files that contains (1) the file provided as a
	   * parameter or (2) all files ending with the given extension in the
	   * directory provided as a parameter. 
	   * 
	   * @param name the name of a file or directory
	   * @param extension the extension to search the directory for (include the period, use ".txt" not "txt")
	   * @return a list of file names
	   */
	  public static List<String> getFileName(String name, String extension) {
		  ArrayList<String> txtFiles = new ArrayList<String>();
		  File file = new File(name);
		  if (file.canRead()) {
			  if (file.isDirectory()) {
				  final String ext = extension; 
				  File[] fs=file.listFiles(new FilenameFilter() {
					  public boolean accept(File f, String name) {
						  return name.endsWith(ext);
					  }                       
				  }); 
				  for (File txtfile:fs) {
					  txtFiles.add(txtfile.getPath());
				  }
			  } else {
				  txtFiles.add(file.getPath());
			  }
		  }
		  return txtFiles;
	  }
	  
	  /**
	   * Writes a key and value to a file. This can be used for persistence 
	   * (i.e., when an application starts up, the last file is known). If they key is already
	   * in the file, this will rewrite the old value with the new one. If the key is not in the
	   * file, this will add the key,value pair to the file
	   * 
	   * @param configFile the name of the configuration file
	   * @param key the key
	   * @param value the value
	   */
	  public static void write_persistent(String configFile, String key, String value) {
		  ParameterData param = FileUtil.read_persistent(configFile);
		  param.set(key, value);
		  FileUtil.write_persistent(configFile,param);
	  }
 
	  /**
	   * Writes a ParameterData to a file. This can be used for persistence 
	   * (i.e., when an application starts up, the last file is known).
	   * 
	   * @param configFile the name of the configuration file
	   * @param pd ParameterData to write
	   */
	  public static void write_persistent(String configFile, ParameterData pd) {
		try {
			PrintWriter pw = open_PrintWriter(configFile);
			pw.println(pd.toString());
			pw.close();
		} catch (Exception e2) {
			// do nothing
		}
	  }

	  /**
	   * Removes a key and its value from file. 
	   * 
	   * @param configFile the name of the configuration file
	   * @param key the key
	   */
	  public static void remove_key_persistent(String configFile, String key) {
		  ParameterData param = FileUtil.read_persistent(configFile);
		  param.remove(key);;
		  FileUtil.write_persistent(configFile,param);
	  }

	  /**
	   * Given a file with a key/value parameter, return the value of the given key.
	   * This 
	   * can be used for persistence (i.e., when an application starts up, the last file is known)
	   * 
	   * @param configFile the name of the file to read
	   * @param key the key in the configuration file
	   * @return the value of the key, empty string if the key is not found
	   */
	  public static String read_persistent(String configFile, String key) {
		  String rtn = "";
		  SeparatedInput cr;
		  try {
			  cr = new SeparatedInput(new BufferedReader(new FileReader(configFile)));
			  cr.readLine();
			  if ( ! cr.hasMessage()) {
				  rtn = cr.getParametersRef().getString(key);
			  }
			  return rtn;
		  } catch (FileNotFoundException e1) {
			  return rtn;
		  } 
	  }

	  /**
	   * Given a file with one or more key/value pairs, return the ParameterData containing those pairs.
	   * @param configFile
	   * @return parameter database
	   */
	  public static ParameterData read_persistent(String configFile) {
		  ParameterData rtn = ParameterData.make();
		  SeparatedInput cr;
		  try {
			  cr = new SeparatedInput(new BufferedReader(new FileReader(configFile)));
			  cr.readLine();			  
			  if ( ! cr.hasError()) {
				  rtn = cr.getParametersRef();
			  }
			  return rtn;
		  } catch (FileNotFoundException e1) {
			  return rtn;
		  } 
	  }
	  
	  
	  public static PrintWriter open_PrintWriter(String outFileName) {
		  PrintWriter outFile;
		  try {
			  // Create the output stream.
			  outFile = new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
		  }
		  catch (IOException e) {
//			  System.err.println("Can't open file " + outFileName + "!");
//			  System.err.println("Error: " + e);
			  return null; 
		  }
		  return outFile;
	  }

	  private static Object FileLocker = new Object() {};
	  /**
	   *  Add to the given file a line represented by the list of strings (using UTF-8 character encoding).  This method
	   *  will work even if multiple programs (or multiple threads within the same program) are trying to write to the 
	   *  same file at the same time.  If the file is already opened by another thread, then this method will wait until
	   *  the lock is released, then it will append its line.  (Warning: there is a possibility for deadlock).<p>
	   *  
	   *  For threads within the same program, the guarantee that two threads will not write at the same time is
	   *  only valid if this method is used.  If the file is opened with regular Java IO calls, then there is no
	   *  guarantee of single access.<p>
	   *  
	   * @param pathname the name of the file to append a line (including, possibly, a path)
	   * @param list a list of strings to write to the file
	   * @param delimiter the string to place between each element of the list of string (a comma or a tab character are typical choices)  
	   * @throws FileNotFoundException if the file cannot be opened (because it is a directory, etc.)
	   */
	  public static void append_file(String pathname, List<String> list, String delimiter) throws FileNotFoundException {

		  synchronized (FileLocker) {
			  FileOutputStream in = new FileOutputStream(pathname, true);
			  try {
				  java.nio.channels.FileLock lock = in.getChannel().lock();
				  try {
					  PrintWriter writer = new PrintWriter(new OutputStreamWriter(in, "UTF-8"));
					  writer.println(f.list2str(list,delimiter));
					  writer.close();
				  } finally {
					  lock.release();
				  }
			  } catch (IOException exp) {
				  // do nothing?
			  } finally {
				  try { 
					  in.close();
				  } catch (IOException exp) {
					  // do nothing
				  }
			  }
		  }
	  }

	  /**
	   * Write the given string to a file with the given name.
	   * @param outputFileName filename
	   * @param s string
	   * @throws IOException if a file error occurs
	   */
	  public static void dumpStrToFile(String outputFileName, String s) throws IOException {
		  java.io.PrintWriter pw = 
				  new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(outputFileName)));
		  pw.println(s);
		  pw.flush();
		  pw.close();
		  pw.flush();
	  }
	  
//	  // parse CDCylinder(1000, "ft", 450, "ft")
//	 public static Pair<Double,Double> CDCylinderParser(String str) {
//		 String[] terms = str.split(", ");
//		 if (terms.length < 4) return new Pair(-1.0,-1.0);
//		 else {			 
//			String term_0 = terms[0].replaceAll("\\(","");
//			String term_1 = terms[1].replaceAll("\"","");
//			String term_2 = terms[2];
//			String term_3 = terms[3].replaceAll("\\)","").replaceAll("\"","");	 
//			double D = Units.from(term_1,Double.parseDouble(term_0));
//			double H = Units.from(term_3,Double.parseDouble(term_2));
//			return new Pair<Double,Double>(D,H);
//		 }	 
//	 }

}
