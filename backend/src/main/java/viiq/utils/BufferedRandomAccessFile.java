/**
 * This code to improve the random file access performance was obtained from:
 * http://www.javaworld.com/article/2077523/build-ci-sdlc/java-tip-26--how-to-improve-java-s-i-o-performance.html
 */
package viiq.utils;

import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.TreeMap;

import viiq.backendHelper.NodeLabelID;
import viiq.commons.ObjNodeIntProperty;
import viiq.graphQuerySuggestionMain.Config;

public class BufferedRandomAccessFile extends RandomAccessFile {

	byte buffer[];
	int buf_end = 0;
	int buf_pos = 0;
	long real_pos = 0;
	int BUF_SIZE = 0;
	long numberOfLines = 0;
	final Charset charset = Charset.forName("UTF-8");
	//final Charset charset = Charset.forName("Cp1252");

	class LabelIndex {
		long fileIndex;
		String label;
	}

	public BufferedRandomAccessFile(String filename, String mode, long nol, Config conf) throws IOException {
		super(filename, mode);
		numberOfLines = nol;
		BUF_SIZE = Integer.parseInt(conf.getProp(PropertyKeys.datagraphAlignmentLength)) + 1;
		invalidate();
		buffer = new byte[BUF_SIZE];
	}

	public BufferedRandomAccessFile(String filename, String mode, long nol, int bufsize) throws IOException {
		super(filename, mode);
		numberOfLines = nol;
		invalidate();
		BUF_SIZE = bufsize;
		buffer = new byte[BUF_SIZE];
	}

	public final int read() throws IOException {
		if(buf_pos >= buf_end) {
			if(fillBuffer() < 0)
				return -1;
		}
		if(buf_end == 0) {
			return -1;
		} else {
			return buffer[buf_pos++];
		}
	}

	private int fillBuffer() throws IOException {
		int n = super.read(buffer, 0, BUF_SIZE);
		if(n >= 0) {
			real_pos +=n;
			buf_end = n;
			buf_pos = 0;
		}
		return n;
	}

	private void invalidate() throws IOException {
		buf_end = 0;
		buf_pos = 0;
		real_pos = super.getFilePointer();
	}

	public int read(byte b[], int off, int len) throws IOException {
		int leftover = buf_end - buf_pos;
		if(len <= leftover) {
			System.arraycopy(buffer, buf_pos, b, off, len);
			buf_pos += len;
			return len;
		}
		for(int i = 0; i < len; i++) {
			int c = this.read();
			if(c != -1)
				b[off+i] = (byte)c;
			else {
				return i;
			}
		}
		return len;
	}

	public long getFilePointer() throws IOException {
		long l = real_pos;
		return (l - buf_end + buf_pos) ;
	}

	public void seek(long pos) throws IOException {
		int n = (int)(real_pos - pos);
		if(n >= 0 && n <= buf_end) {
			buf_pos = buf_end - n;
		} else {
			super.seek(pos);
			invalidate();
		}
	}

	public final String getNextLine() throws IOException {
		String str = null;
		if(buf_end-buf_pos <= 0) {
			if(fillBuffer() < 0) {
				return null;
				//throw new IOException("error in filling buffer!");
			}
		}
		int lineend = -1;
		for(int i = buf_pos; i < buf_end; i++) {
			if(buffer[i] == '\n') {
				lineend = i;
				break;
			}
		}
		if(lineend < 0) {
			StringBuffer input = new StringBuffer(256);
			int c;
			while (((c = read()) != -1) && (c != '\n')) {
				input.append((char)c);
			}
			if ((c == -1) && (input.length() == 0)) {
				return null;
			}
			return input.toString();
		}

		if(lineend > 0 && buffer[lineend-1] == '\r') {
			str = new String(buffer, buf_pos, lineend-buf_pos-1, charset);
			//	str = new String(buffer, 0, buf_pos, lineend - buf_pos -1);
		}
		else {
			str = new String(buffer, buf_pos, lineend-buf_pos, charset);
			//	str = new String(buffer, 0, buf_pos, lineend - buf_pos);
		}
		buf_pos = lineend +1;
		return str;
	}

	/**
	 * Method that finds the subset of entities of a particular type that match a particular keyword.. the subset is a function of
	 * windowNumber and windowSize
	 * @param node
	 * @param keyword
	 * @param nodeValues
	 * @param windowNumber
	 * @param windowSize
	 */
	public int getNodeIndexValuesFilteredKeyword(int node, String keyword, ArrayList<String> keywordStrings, int linesToIgnore, int newWindowSize,
			int actualWindowSize, int labelStartColumn) {
		boolean found = false;
		long first = 0;
		long last = 0;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		int totalNumberOfLines = 0;
		//	System.out.println("Searching for " + keyword + " " + keyword.length());
		try {
			while(mid != lastMid && low <= high) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//		System.out.println("low, MID, high = " + low + " " + mid + " " + high);
				//System.out.println("xxx"+line+"xxx");
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[0].trim()) == node)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				first = mid;
				last = getLastOccurrence(mid, node);
				// Now do a keyword search from "first" to "last" lines only. The keyword is in the 2nd column.
				// "2" in the parameter list specifies which column in the file has the keywords.
				totalNumberOfLines = completeKeyword(keyword, keywordStrings, linesToIgnore, newWindowSize, actualWindowSize,
						first, last, labelStartColumn);
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
		return totalNumberOfLines;
	}
	/**
	 * Get last occurrence of a particular integer node ID (this is for search filtered on a domain or type)
	 * @param mid
	 * @param node
	 * @return
	 */
	private long getLastOccurrence(long mid, int node) {
		long last = numberOfLines - 1;
		long low = mid;
		long lastMid = -1;
		long high = numberOfLines - 1;
		mid = (low+high) >>> 1;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//		System.out.println("low, MID, high = " + low + " " + mid + " " + high);
				//		System.out.println(line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == numberOfLines-1) {
						break;
					}
					long nextFileIndex = (mid+1)*BUF_SIZE;
					this.seek(nextFileIndex);
					String nextLine = this.getNextLine();
					String[] nextSplit = nextLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(nextSplit[0].trim()) == node)
						low = mid+1;
					else {
						last = mid;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Finding the last occurrence of a node");
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			ioe.printStackTrace();
		}
		return last;
	}

	/**
	 * This method finds returns only a subset of results back, based on the windowNumber and windowSize.
	 * If windowNumber= 1 and windowSize = 100, this method returns back the first 100 matches for "node".
	 * If windowNumber= 5 and windowSize = 100, this method returns back the fifth 100 matches for "node".
	 * @param node
	 * @param nodeValues
	 * @param windowNumber
	 * @param windowSize
	 */
	public void getNodeIndexValues(int node, ArrayList<String> nodeValues, int windowNumber, int windowSize) {
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
       				//System.out.println("line length = " + line.length());
				//System.out.println("mid = "+mid);
				//System.out.println("xxx"+line+"xxx");
				String[] split = line.split(",");
				//System.out.println(split[0]+" "+split[1]);
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(prevSplit[0].trim()) == node)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				long start = mid + (windowNumber*windowSize);
				fileLineIndex = (start)*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				while(start++ < numberOfLines && (line = this.getNextLine()) != null && windowSize-- > 0) {
					String[] split = line.split(",");
					MutableString ms = new MutableString();
					//	System.out.println(" 3rd parseint --> " + line);
					int v = Integer.parseInt(split[0].trim());
					if(v != node)
						break;
					ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[2].trim());
					nodeValues.add(ms.toString());
				}
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
	}


	public void getNodeIndexValues(int node, HashMap<Integer,String> nodeValues) {
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
       				//System.out.println("line length = " + line.length());
				//System.out.println("mid = "+mid);
				//System.out.println("xxx"+line+"xxx");
				String[] split = line.split(",");
				//System.out.println(split[0]+" "+split[1]);
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(prevSplit[0].trim()) == node)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				long start = mid;
				fileLineIndex = (start)*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				while(start++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					MutableString ms = new MutableString();
					//	System.out.println(" 3rd parseint --> " + line);
					int v = Integer.parseInt(split[0].trim());
					if(v != node)
						break;
					nodeValues.put(Integer.parseInt(split[1].trim()), split[2].trim());
				}
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
	}

	/**
	 * node = Entity; returns back all type IDs.
	 * @param node
	 * @param numOfLines
	 * @return
	 */
	public HashSet<Integer> getEntityTypes(int node) {
		HashSet<Integer> types = new HashSet<Integer>();
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//	System.out.println(line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(prevSplit[0].trim()) == node)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					int v = Integer.parseInt(split[0].trim());
					if(v != node)
						break;
					types.add(Integer.parseInt(split[1].trim()));
				}
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
		return types;
	}

	/**
	 * This method is specific for finding all instances of a type, or all types of an entity.
	 * case 1: node = TYPE; returns back all entity IDs and their string values separated by ','.
	 * case 2: node = Entity; returns back all type IDs and their string values separated by ','.
	 * @param node
	 * @param numOfLines
	 * @return
	 */
	public void getNodeIndexValues(int node, ArrayList<String> nodeValues) {
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//	System.out.println(line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(prevSplit[0].trim()) == node)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					MutableString ms = new MutableString();
					//	System.out.println(" 3rd parseint --> " + line);
					int v = Integer.parseInt(split[0].trim());
					if(v != node)
						break;
					ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[2].trim());
					nodeValues.add(ms.toString());
				}
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
	}

	/**
	 * IMPORTANT ASSUMPTION: This assumes there are only two columns separated by comma ',' in each line of the input files
	 * @param nodeValues
	 * @param windowNumber
	 * @param windowSize
	 */
	public void getAllNodesValue(ArrayList<String> nodeValues, int windowNumber, int windowSize) {
		long fileLineIndex = 0;
		long start = windowNumber*windowSize;
		try {
			fileLineIndex = start*BUF_SIZE;
			this.seek(fileLineIndex);
			long lnum = 0;
			String line = null;
			while(start++ < numberOfLines && (line = this.getNextLine()) != null && windowSize > 0) {
				lnum++;
				/*edited by ss*/
				String[] temp = line.split(",");
				String[] split = new String[2];
                                split[0] = temp[0];
				for(int i = 1; i < temp.length - 1; i++) {
					split[0] += ("," + temp[i]);
				}
				split[1] = temp[temp.length - 1];
				/*
				if(split.length != 2) {
					System.out.println("ERROR IN : " + lnum + " : " + line);
					continue;
				}*/
				windowSize--;
				MutableString ms = new MutableString();
				ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[0].trim());
				nodeValues.add(ms.toString());
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/*private void completeKeyword(String keyword, ArrayList<String> keywordStrings, long first, long last, int column,
			int windowNumber, int windowSize) {
		boolean found = false;
		long low = first;
		long high = last;
		long mid = (low+high) >>> 1;
		long lastMid = 0;
		int keyLen = keyword.length();
		//System.out.println("\n******************************************************************************* " + keyword);
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
		//		System.out.println("low, MID, high = " + low + " " + mid + " " + high);
		//		System.out.println(line);
				String[] fullStr = line.split(",");
				String str;
				if(fullStr[column].trim().length() < keyLen)
					str = fullStr[column].trim();
				else
					str = fullStr[column].trim().substring(0, keyLen);
				if(str.equalsIgnoreCase(keyword)) {
					if(mid == first) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(prevSplit[column].trim().length() >= keyLen && prevSplit[column].trim().substring(0, keyLen).equalsIgnoreCase(keyword))
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(str.compareToIgnoreCase(keyword) < 0) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
		//		System.out.println("\n********************************** OKOKOK ********************************************* \n\n");
				long start = mid + (windowNumber*windowSize);
				fileLineIndex = start*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				while(start < last+1 && start < numberOfLines && (line = this.getNextLine()) != null && windowSize-- > 0) {
					start++;
					String[] split = line.split(",");
					MutableString ms = new MutableString();
					String str;
					if(split[column].trim().length() < keyLen)
						str = split[column].trim();
					else
						str = split[column].trim().substring(0, keyLen);
				//	System.out.println(line);
				//	System.out.println(keyword + "  " + str + "  " + str.compareToIgnoreCase(keyword));

					if(str.compareToIgnoreCase(keyword) > 0)
						break;
					if(str.compareToIgnoreCase(keyword) == 0) {
						ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[column].trim());
						keywordStrings.add(ms.toString());
					}
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}*/

	/**
	 * This method gets all entity/type string values that begin with "keyword", based on window parameters
	 * IMPORTANT ASSUMPTION: This assumes there are only two columns separated by comma ',' in each line of the input files
	 * @param keyword
	 * @param keywordStrings
	 */
	/*public void completeKeyword(String keyword, ArrayList<String> keywordStrings, int windowNumber, int windowSize) {
		//IMPORTANT ASSUMPTION: This assumes there are only two columns separated by comma ',' in each line of the input files
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		int keyLen = keyword.length();
		long fileLineIndex = 0;
		long lnum = 0;
	//	System.out.println("Trying to find " + keyword + " of len = " + keyLen + " bufsize = " + BUF_SIZE);
		try {
			while(mid != lastMid && low <= high) {
				lnum++;
				lastMid = mid;
		//		System.out.println("low, MID, high = " + low + " " + mid + " " + high);
				fileLineIndex = mid*BUF_SIZE;
		//		System.out.println(fileLineIndex);
				this.seek(fileLineIndex);
				String line = this.getNextLine();
		//		System.out.println(line + " size  of line = " + line.length());
				String[] fullStr = line.split(",");
				if(fullStr.length != 2) {
					System.out.println("ERROR IN : " + lnum + " : " + line);
					continue;
				}
				String str;
				if(fullStr[0].trim().length() < keyLen)
					str = fullStr[0].trim();
				else
					str = fullStr[0].trim().substring(0, keyLen);
			//	System.out.println();
			//	System.out.println(low + " " + mid + " " + high);


				if(str.equalsIgnoreCase(keyword)) {
			//	System.out.println("equal");
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(prevSplit.length != 2) {
						System.out.println("ERROR IN : " + line);
						continue;
					}
					if(prevSplit[0].trim().length() >= keyLen && prevSplit[0].trim().substring(0, keyLen).equalsIgnoreCase(keyword))
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(str.compareToIgnoreCase(keyword) < 0) {
			//	System.out.println("LOW");
					low = mid+1;
				}
				else {
			//	System.out.println("HIGH");
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
	//		System.out.println("*****> " + found);
			if(found) {
				long start = mid + (windowNumber*windowSize);
				fileLineIndex = (start)*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				while(start++ < numberOfLines && (line = this.getNextLine()) != null && windowSize > 0) {
					String[] split = line.split(",");
					if(split.length != 2) {
						System.out.println("ERROR IN : " + line);
						continue;
					}
					windowSize--;
					MutableString ms = new MutableString();
					String str;
					if(split[0].trim().length() < keyLen)
						str = split[0].trim();
					else
						str = split[0].trim().substring(0, keyLen);
					if(str.compareToIgnoreCase(keyword) > 0)
						break;
					ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[0].trim());
					keywordStrings.add(ms.toString());
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}*/
	/**
	 * This method gets all entity/type string values that begin with "keyword", based on window parameters
	 * The keyword search is not based only on 	 * values starting with the keyword, but also on the keyword starting at
	 * 2, 3, 4, 5 or 6th column of the entire string value. (The file has string values beginning at the very first column)
	 * AND HENCE fullStr[0] in the code below.
	 * IMPORTANT ASSUMPTION: This assumes there are only TWO (2) columns separated by comma ',' in each line of the input files
	 */
	public int completeKeyword(String keyword, ArrayList<String> keywordStrings, int linesToIgnore, int newWindowSize,
			int actualWindowSize, int labelStartColumn) {
		//IMPORTANT ASSUMPTION: This assumes there are only TWO (2) columns separated by comma ',' in each line of the input files
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		int keyLen = keyword.length();
		long fileLineIndex = 0;
		long lnum = 0;
		int totalNumberOfLines = 0;
		long first = 0;
		try {
			while(low <= high) {
				lnum++;
				mid = (low+high) >>> 1;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				/*edited by ss*/
				//String[] fullStr = line.split(",", 2);
                                String[] temp = line.split(",");
                                String[] fullStr = new String[2];
                                fullStr[0] = temp[0];
                                for(int i = 1; i < temp.length - 1; i++) {
                                        fullStr[0] += ("," + temp[i]);
                                }
                                fullStr[1] = temp[temp.length - 1];
				/*
				if(fullStr.length != 2) {
					System.out.println("ERROR IN : " + lnum + " : " + line);
					high--;
					continue;
				}*/
				String str;
				if(labelStartColumn == 0) {
					if(fullStr[0].trim().length() < keyLen)
						str = fullStr[0].trim();
					else
						str = fullStr[0].trim().substring(0, keyLen);
				} else {
					int col = 0;
					int fromIndex = 0;
					String substr = fullStr[0].trim();
					while(col++ < labelStartColumn) {
						fromIndex = substr.indexOf(" ", fromIndex)+1;
					}
					substr = substr.substring(fromIndex);
					//	System.out.println("label start col = " + labelStartColumn + " -------- " + substr);
					if(substr.trim().length() < keyLen)
						str = substr.trim();
					else
						str = substr.trim().substring(0, keyLen);
				}

				if(str.equalsIgnoreCase(keyword)) {
					if(mid == 0) {
						found = true;
						first = 0;
						break;
					}
					high = mid-1;
					first = mid;
					found = true;
				}
				else if(str.compareToIgnoreCase(keyword) < 0) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
			}
			if(found) {
				long start = first + linesToIgnore;
				fileLineIndex = (start)*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				while(start++ < numberOfLines && (line = this.getNextLine()) != null && newWindowSize > 0) {
					/*edited by ss*/
	                                String[] temp = line.split(",");
        	                        String[] split = new String[2];
                	                split[0] = temp[0];
                        	        for(int i = 1; i < temp.length - 1; i++) {
                                	        split[0] += ("," + temp[i]);
                                	}
                                	split[1] = temp[temp.length - 1];


					/*
					if(split.length != 2) {
						System.out.println("ERROR IN : " + line);
						continue;
					}*/
					newWindowSize--;
					MutableString ms = new MutableString();
					String str;
					if(labelStartColumn == 0) {
						if(split[0].trim().length() < keyLen)
							str = split[0].trim();
						else
							str = split[0].trim().substring(0, keyLen);
					} else {
						int col = 0;
						int fromIndex = 0;
						String substr = split[0].trim();
						while(col++ < labelStartColumn) {
							fromIndex = substr.indexOf(" ", fromIndex)+1;
						}
						substr = substr.substring(fromIndex);
						//	System.out.println("label start col = " + labelStartColumn + " ========= " + substr);
						if(substr.trim().length() < keyLen)
							str = substr.trim();
						else
							str = substr.trim().substring(0, keyLen);
					}

					if(str.compareToIgnoreCase(keyword) > 0)
						break;
					ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[0].trim());
					keywordStrings.add(ms.toString());
				}
				if(keywordStrings.size() < actualWindowSize+1) {
					// this is either empty or has less than the required number of entries, so find the actual number of lines
					// of a particular keyword in this file, i.e., find the end-start.
					long last = getLastOccurrence(first, -1, keyword, 0, labelStartColumn);
					totalNumberOfLines = (int)(last-first);
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return totalNumberOfLines;
	}


	public long findEntity(String entityName, int id){
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long fileLineIndex = 0;
		try {
			while(low <= high) {
				mid = (low+high) >>> 1;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] temp = line.split(",");
				int tid = Integer.parseInt(temp[temp.length-1].trim());
				String tname = temp[0];
				for(int i=1; i<temp.length-2; i++){
					tname += ","+temp[i];
				}
				if(tname.equalsIgnoreCase(entityName)){
					if(tid == id){
						return mid;
					}
					else if(tid > id){
						high = mid - 1;
					}
					else{
						low = mid+1;
					}

				}
				else if(tname.compareToIgnoreCase(entityName) < 0) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return -1;
	}


	public int findEntityWithType(int node, int entityId, String entityName) {
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		long startIndex = 0;
		try {
			while(low <= high){
				mid = (low+high) >>> 1;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				System.out.println("*"+line+":"+node);
       	String[] split = line.split(",");
				//System.out.println(split[0]+" "+split[1]);
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(mid == 0){
						break;
					}
					else{
						fileLineIndex = (mid-1)*BUF_SIZE;
						this.seek(fileLineIndex);
						String line1 = this.getNextLine();
						String[] split1 = line1.split(",");
						int v1 = Integer.parseInt(split1[0].trim());
						if(v!=v1){
							startIndex = mid;
							break;
						}
						else{
							high = mid - 1;
						}
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
			}
			low = 0;
			high = numberOfLines-1;
			mid = (numberOfLines-1)/2;
			while(low <= high) {
				numberOfSteps++;
				mid = (low+high) >>> 1;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				System.out.println(line+":"+node);
       	String[] split = line.split(",");
				//System.out.println(split[0]+" "+split[1]);
				int v = Integer.parseInt(split[0].trim());
				if(v == node) {
					if(split[2].trim().equalsIgnoreCase(entityName)) {
						int eid = Integer.parseInt(split[1].trim());
						if(eid == entityId){
							System.out.println("entity pos:"+mid+", start:"+startIndex);
							return (int)(mid-startIndex);
						}
						else if(eid > entityId){
							high = mid - 1;
						}
						else{
							low = mid + 1;
						}
					}
					else if(split[2].trim().compareToIgnoreCase(entityName) < 0){
						low = mid + 1;
					}
					else{
						high = mid - 1;
					}
				}
				else if(v < node) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}

			}
			System.out.println("Number of steps:"+numberOfSteps);
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return 0;
	}



	/**
	 * this method searches for keyword, filtered on an ID (either a domain or a type ID). The keyword search is not based only on
	 * values starting with the keyword, but also on the keyword starting at 2, 3, 4, 5 or 6th column of the entire string value.
	 *
	 * The string value begins at the 3rd column (or 2nd if counted from 0) in the input file.
	 * HENCE the fullStr[2] in the code below.
	 * IMPORTANT ASSUMPTION: This assumes there are only THREE (3) columns separated by comma ',' in each line of the input files
	 */
	public int completeKeyword(String keyword, ArrayList<String> keywordStrings, int linesToIgnore, int newWindowSize,
			int actualWindowSize, long startRow, long endRow, int labelStartColumn) {
		//IMPORTANT ASSUMPTION: This assumes there are only THREE (3) columns separated by comma ',' in each line of the input files
		boolean found = false;
		long low = startRow;
		long high = endRow;
		long mid = (low+high) >>> 1;
		System.out.println("low = "+low+" high = "+high);
						int keyLen = keyword.length();
						long fileLineIndex = 0;
						long lnum = 0;
						int totalNumberOfLines = 0;
						long first = 0;
						try {
							while(low <= high) {
								lnum++;
								mid = (low+high) >>> 1;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//System.out.println("yyy"+line+"yyy");
				//String[] fullStr = line.split(",");
				String[] fullStr = line.split(",", 3);
				//commented by ss
				/*
				if(fullStr.length != 3) {
					System.out.println("ERROR IN : " + lnum + " : " + line);
					high--;
					continue;
				}*/
				String str;
				if(labelStartColumn == 0) {
					if(fullStr[2].trim().length() < keyLen)
						str = fullStr[2].trim();
					else
						str = fullStr[2].trim().substring(0, keyLen);
				} else {
					int col = 0;
					int fromIndex = 0;
					String substr = fullStr[2].trim();
					while(col++ < labelStartColumn) {
						fromIndex = substr.indexOf(" ", fromIndex)+1;
					}
					substr = substr.substring(fromIndex);
					//	System.out.println("label start col = " + labelStartColumn + " -------- " + substr);
					if(substr.trim().length() < keyLen)
						str = substr.trim();
					else
						str = substr.trim().substring(0, keyLen);
				}

				if(str.equalsIgnoreCase(keyword)) {
					if(mid == 0) {
						found = true;
						first = 0;
						break;
					}
					high = mid-1;
					first = mid;
					found = true;
				}
				else if(str.compareToIgnoreCase(keyword) < 0) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
							}
							if(found) {
								long start = first + linesToIgnore;
								fileLineIndex = (start)*BUF_SIZE;
								this.seek(fileLineIndex);
								String line = null;
								while(start++ <= endRow && (line = this.getNextLine()) != null && newWindowSize > 0) {
									String[] split = line.split(",", 3);
									/*
									if(split.length != 3) {
										System.out.println("ERROR IN : " + line);
										continue;
									}*/
									newWindowSize--;
									MutableString ms = new MutableString();
									String str;
									if(labelStartColumn == 0) {
										if(split[2].trim().length() < keyLen)
											str = split[2].trim();
										else
											str = split[2].trim().substring(0, keyLen);
									} else {
										int col = 0;
										int fromIndex = 0;
										String substr = split[2].trim();
										while(col++ < labelStartColumn) {
											fromIndex = substr.indexOf(" ", fromIndex)+1;
										}
										substr = substr.substring(fromIndex);
										//		System.out.println("label start col = " + labelStartColumn + " ========= " + substr);
										if(substr.trim().length() < keyLen)
											str = substr.trim();
										else
											str = substr.trim().substring(0, keyLen);
									}

									if(str.compareToIgnoreCase(keyword) > 0)
										break;
									ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[2].trim());
									keywordStrings.add(ms.toString());
								}
								if(keywordStrings.size() < actualWindowSize+1) {
									// this is either empty or has less than the required number of entries, so find the actual number of lines
									// of a particular keyword in this file, i.e., find the end-start.
									long last = getLastOccurrence(first, endRow, keyword, 2, labelStartColumn);
									totalNumberOfLines = (int)(last-first);
								}
							}
						} catch(IOException ioe) {
							ioe.printStackTrace();
						}
						return totalNumberOfLines;
	}

	/**
	 * Searches for the last occurrence of a keyword.
	 * IF lastRowToConsider == -1, then the last line to consider is numberOfRows-1
	 * @param low
	 * @param lastRowToConsider
	 * @param keyword
	 * @param stringValueColumn
	 * @param labelStartColumn
	 * @return
	 */
	private long getLastOccurrence(long low, long lastRowToConsider, String keyword, int stringValueColumn, int labelStartColumn) {
		long maxLineNumber = lastRowToConsider < 0 ? (numberOfLines-1) : lastRowToConsider;
		long last = maxLineNumber;
		long high = maxLineNumber;
		long mid = (low+high) >>> 1;
								int keyLen = keyword.length();
								long fileLineIndex = 0;
								long lnum = low;
								int numOfCols = stringValueColumn == 0 ? 2 : 3;
								try {
									while(low <= high) {
										mid = (low+high) >>> 1;
										fileLineIndex = mid*BUF_SIZE;
										this.seek(fileLineIndex);
										String line = this.getNextLine();
										/*edited by ss*/
										String[] fullStr = new String[numOfCols];
										if(numOfCols == 3)
											fullStr = line.split(",", 3);
										else {
											String[] temp = line.split(",", 2);

                                							fullStr[0] = temp[0];
                                							for(int i = 1; i < temp.length - 1; i++) {
                                        							fullStr[0] += ("," + temp[i]);
                                							}
                               	 							fullStr[1] = temp[temp.length - 1];
										}
										/*
										if(fullStr.length != numOfCols) {
											System.out.println("ERROR IN : " + (lnum++) + " : " + line);
											high--;
											continue;
										}*/
										String str;
										if(labelStartColumn == 0) {
											if(fullStr[stringValueColumn].trim().length() < keyLen)
												str = fullStr[stringValueColumn].trim();
											else
												str = fullStr[stringValueColumn].trim().substring(0, keyLen);
										} else {
											int col = 0;
											int fromIndex = 0;
											String substr = fullStr[stringValueColumn].trim();
											while(col++ < labelStartColumn) {
												fromIndex = substr.indexOf(" ", fromIndex)+1;
											}
											substr = substr.substring(fromIndex);
											if(substr.trim().length() < keyLen)
												str = substr.trim();
											else
												str = substr.trim().substring(0, keyLen);
										}

										if(str.equalsIgnoreCase(keyword)) {
											last = mid;
											if(mid == maxLineNumber) {
												break;
											}
											low = mid+1;
										}
										else if(str.compareToIgnoreCase(keyword) < 0) {
											low = mid+1;
										}
										else {
											high = mid-1;
										}

									}
								} catch(NumberFormatException nfe) {
									System.out.println("Exception around line ID ======= " + fileLineIndex);
									nfe.printStackTrace();
								} catch(IOException ioe) {
									System.out.println("Finding the last occurrence of a node");
									System.out.println("Line ID =========== " + fileLineIndex);
									System.out.println("Low, mid, high = " + low + " " + mid + " " + high);
									ioe.printStackTrace();
								}
								return last;
	}

	/**
	 * This method gets all entity/type string values that begin with "keyword"
	 * @param keyword
	 * @param keywordStrings
	 */
	/*public void completeKeyword(String keyword, ArrayList<String> keywordStrings) {
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		int keyLen = keyword.length();
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] fullStr = line.split(",");
				String str;
				if(fullStr[0].trim().length() < keyLen)
					str = fullStr[0].trim();
				else
					str = fullStr[0].trim().substring(0, keyLen);
			//	System.out.println();
			//	System.out.println(low + " " + mid + " " + high);
			//	System.out.println(line);

				if(str.equalsIgnoreCase(keyword)) {
			//	System.out.println("equal");
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(prevSplit[0].trim().length() >= keyLen && prevSplit[0].trim().substring(0, keyLen).equalsIgnoreCase(keyword))
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(str.compareToIgnoreCase(keyword) < 0) {
			//	System.out.println("LOW");
					low = mid+1;
				}
				else {
			//	System.out.println("HIGH");
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					MutableString ms = new MutableString();
					String str;
					if(split[0].trim().length() < keyLen)
						str = split[0].trim();
					else
						str = split[0].trim().substring(0, keyLen);
					if(str.compareToIgnoreCase(keyword) > 0)
						break;
					ms = ms.append(Integer.parseInt(split[1].trim())).append(",").append(split[0].trim());
					keywordStrings.add(ms.toString());
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}*/

	/**
	 * This method returns back the string values of a vertex, if it exists.
	 * "vertex" can either be a type, or an entity. The correct file has to be used while creating the object of this class.
	 * @param vertex
	 * @return
	 */
	public void getAllNodesLabel(int[] vertices, String keyword, ArrayList<NodeLabelID> labels) {
		long low = 0;
		int cnt = 0;
		System.out.println("starting to get labels!");
		for(int vertex : vertices) {
			LabelIndex li = getNodeLabel(vertex, low);
			if(li.label != null) {
				if(keyword.isEmpty()) {
					cnt++;
					NodeLabelID nli = new NodeLabelID();
					nli.setId(vertex);
					nli.setLabel(li.label);
					labels.add(nli);
				} else {
					if(li.label.length() > keyword.length() && keyword.compareToIgnoreCase(li.label.substring(0, keyword.length())) == 0) {
						NodeLabelID nli = new NodeLabelID();
						nli.setId(vertex);
						nli.setLabel(li.label);
						labels.add(nli);
					}
				}
				low = li.fileIndex;
			}
		}
		System.out.println("total vertices = " + vertices.length + " total labels found = " + cnt);
	}

	/**
	 * This method returns back the string values of a vertex, if it exists.
	 * "vertex" can either be a type, or an entity. The correct file has to be used while creating the object of this class.
	 * @param vertex
	 * @return
	 */
	public LabelIndex getNodeLabel(int vertex, long start) {
		LabelIndex li = new LabelIndex();
		li.label = null;
		long low = start;
		long high = numberOfLines-1;
		long mid = 0;
		long fileLineIndex = 0;
		try {
			while(low <= high) {
				mid = (low+high) >>> 1;
											fileLineIndex = mid*BUF_SIZE;
											this.seek(fileLineIndex);
											String line = this.getNextLine();
											String[] split = line.split(",");
											int v;
											try {
												v = Integer.parseInt(split[0].trim());
											} catch(NumberFormatException nfe) {
												System.out.println("Number format exception!!!!!!!!!!! " + mid + " --> " + line);
												high--;
												mid = (low+high) >>> 1;
					continue;
											}
											if(v == vertex) {
												this.seek(fileLineIndex);
												String l = this.getNextLine();
												li.label = l.split(",")[1].trim();
												break;
											}
											else if(v < vertex) {
												low = mid+1;
											}
											else {
												high = mid-1;
											}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		li.fileIndex = mid;
		return li;
	}

	/**
	 * This method returns back the string values of a vertex, if it exists.
	 * "vertex" can either be a type, or an entity. The correct file has to be used while creating the object of this class.
	 * @param vertex
	 * @return
	 */
	public String getNodeLabel(int vertex) {
		String label = null;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] split = line.split(",");
				int v;
				try {
					v = Integer.parseInt(split[0].trim());
				} catch(NumberFormatException nfe) {
					System.out.println("Number format exception!!!!!!!!!!! " + mid + " --> " + line);
					high--;
					mid = (low+high) >>> 1;
					continue;
				}
				if(v == vertex) {
					this.seek(fileLineIndex);
					String l = this.getNextLine();
					label = l.split(",")[1].trim();
					break;
				}
				else if(v < vertex) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return label;
	}

	/**
	 * This method returns back the number of edges incident to an entity in datagraph.
	 * @param vertex
	 * @return number of incident edges
	 */
	public int getEntityEdgeCount(int vertex) {
		int count = 0;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] split = line.split(",");
				int v;
				try {
					v = Integer.parseInt(split[0].trim());
				} catch(NumberFormatException nfe) {
					System.out.println("Number format exception!!!!!!!!!!! " + mid + " --> " + line);
					high--;
					mid = (low+high) >>> 1;
					continue;
				}
				if(v == vertex) {
					this.seek(fileLineIndex);
					String l = this.getNextLine();
					count = Integer.parseInt(l.split(",")[1].trim());
					break;
				}
				else if(v < vertex) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return count;
	}

	/**
	 * This method is specific to finding all the neighbors of a node in the data graph.
	 * @param vertex
	 * @return
	 */
	public ArrayList<ObjNodeIntProperty> getVertexNeighbors(int vertex) {
		ArrayList<ObjNodeIntProperty> neighbors = null;
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				if(line == null) {
					found = false;
					break;
				}
				//System.out.println("The line is: "+line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[1].trim());
				if(v == vertex) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[1].trim()) == vertex)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < vertex) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				neighbors = new ArrayList<ObjNodeIntProperty>();
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					ObjNodeIntProperty onip = new ObjNodeIntProperty();
					int v = Integer.parseInt(split[1].trim());
					if(v != vertex)
						break;
					onip.tupleId = Integer.parseInt(split[0].trim());
					onip.prop = Integer.parseInt(split[2].trim());
					onip.dest = Integer.parseInt(split[3].trim());
					neighbors.add(onip);
					/* stopping after found neighbor count exceeds threshold*/
					//if(neighbors.size() > 100) break;
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return neighbors;
	}

	/**
	 * This method is specific to finding all the neighbors of a node in the data graph for a specific edge
	 * @param vertex, edge
	 * @return
	 */
	public LinkedHashSet<Integer> getVertexNeighborsForSpecificEdge(int vertex, int edge) {
		LinkedHashSet<Integer> neighbors = null;
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//System.out.println("The line is: "+line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[1].trim());
				int e = Integer.parseInt(split[2].trim());
				if(v == vertex && e == edge) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[1].trim()) == vertex && Integer.parseInt(prevSplit[2].trim()) == edge)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < vertex || (v == vertex && e < edge)) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				neighbors = new LinkedHashSet<Integer>();
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					int v = Integer.parseInt(split[1].trim());
					int e = Integer.parseInt(split[2].trim());
					if(v != vertex || e != edge)
						break;
					neighbors.add(Integer.parseInt(split[3].trim()));
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return neighbors;
	}

	/**
	 * This method is specific to finding all ends of edge instances in the data graph for a specific edgetype
	 * @param edge
	 * @return
	 */
	public HashSet<Integer> getEntitiesForEdge(int edge) {
		HashSet<Integer> entities = null;
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();

				//System.out.println("The line is: "+line);
				String[] split = line.split(",");
				int e = Integer.parseInt(split[2].trim());
				if(e == edge) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[2].trim()) == edge)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(e < edge) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
				//System.out.println(line+" --> "+low+","+mid+","+high);
			}
			//System.out.println(found+" "+mid);
			if(found) {
				entities = new HashSet<Integer>();
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					int s = Integer.parseInt(split[1].trim());
					int e = Integer.parseInt(split[2].trim());
					int o = Integer.parseInt(split[3].trim());
					if(e != edge)
						break;
					entities.add(s);
					entities.add(o);
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return entities;
	}

	class Boundary {
		long start;
		long end;
	}
	/**
	 * Find all entities of a given type, but this is for a special GUI feature, which is to find all entities of a "?" mark type node.
	 * All entity IDs and their labels are captured only if:
	 * 1) it matches the keyword
	 * 2) if the found entity of a type is already in the "entitiesOfOtherTypesAlreadyFound". This is essentially to find the intersection of multiple type-instances.
	 * @param type
	 * @param entitiesOfOtherTypesAlreadyFound
	 * @param keyword
	 * @return
	 */
	public ArrayList<String> getInstancesOfMultipleTypesFilteredOnKeyword(int type,
			ArrayList<String> entitiesOfOtherTypesAlreadyFound, String keyword, int typeSetSize, int windowSize, int windowNumber, boolean smallestSet, int entityId) {
		ArrayList<String> newTypes = new ArrayList<String>();
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		long numberOfSteps = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				numberOfSteps++;
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				//	System.out.println(line);
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == type) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					//	System.out.println(" 2nd parseint --> " + line);
					if(Integer.parseInt(prevSplit[0].trim()) == type)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < type) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}

			if(found) {
				long actualStartLine;
				long actualEndLine;
				if(keyword.isEmpty()) {
					actualStartLine = mid;
					actualEndLine = numberOfLines;
				} else {
					Boundary bound = rightBinarySearchForKeywordInTypeInstanceFile(type, mid, keyword);
					if(bound == null) {
						actualStartLine = mid;
						actualEndLine = mid;
					} else {
						actualStartLine = bound.start;
						actualEndLine = bound.end+1;
					}
				}
				fileLineIndex = actualStartLine*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = null;
				//for node with only 1 type, it is enough to generate first windowSize*(windowNumber+1) entities. For multiple types, we need all entities because the combined entity list needs to be sorted
				int numberOfEntitiesToFind = windowSize*(windowNumber+1);


				long start = System.currentTimeMillis();
				System.out.println("number of entities to iterate = "+(actualEndLine-actualStartLine));



				// /**************************** sort-merge join***********************************/
				// int foundEntitiesIndex = 0;
				// line = this.getNextLine();
				// while(true) {
				// 	if(line == null || actualStartLine == actualEndLine || (numberOfEntitiesToFind == 0 && typeSetSize == 1) || (entitiesOfOtherTypesAlreadyFound != null && foundEntitiesIndex == entitiesOfOtherTypesAlreadyFound.size()))
				// 		break;
				// 	String[] split = line.split(",");
				// 	int v = Integer.parseInt(split[0].trim());
				// 	if(v != type)
				// 		break;
				// 	String label = split[2].trim();
				// 	String id = split[1].trim();
				//
				// 	// if(entitiesOfOtherTypesAlreadyFound != null)
				// 	// 	System.out.println(id+","+label+" ---> "+entitiesOfOtherTypesAlreadyFound.get(foundEntitiesIndex));
				//
				// 	boolean keywordMatch = false;
				// 	if(keyword.isEmpty()) {
				// 		keywordMatch = true;
				// 	} else {
				// 		if(label.length() > keyword.length() && keyword.compareToIgnoreCase(label.substring(0, keyword.length())) == 0) {
				// 			keywordMatch = true;
				// 		}
				// 	}
				//
				// 	if(entitiesOfOtherTypesAlreadyFound == null) {
				// 		if(keywordMatch) {
				// 			newTypes.add(id+","+label);
				// 			numberOfEntitiesToFind--;
				// 			line = this.getNextLine();
				// 			actualStartLine++;
				// 		}
				// 	}	else {
				// 		String foundEntity = entitiesOfOtherTypesAlreadyFound.get(foundEntitiesIndex);
				// 		int compareLabel = label.compareToIgnoreCase(foundEntity.split(",")[1]);
				// 		if(compareLabel == 0) {
				// 			int compareId = Integer.parseInt(id)-Integer.parseInt(foundEntity.split(",")[0]);
				// 			if(compareId == 0) {
				// 				if(keywordMatch) {
				// 					newTypes.add(id+","+label);
				// 					numberOfEntitiesToFind--;
				// 					line = this.getNextLine();
				// 					actualStartLine++;
				// 					foundEntitiesIndex++;
				// 				}
				// 			} else if(compareId > 0){
				// 				foundEntitiesIndex++;
				// 			} else {
				// 				line = this.getNextLine();
				// 				actualStartLine++;
				// 			}
				// 		} else if(compareLabel < 0) {
				// 			line = this.getNextLine();
				// 			actualStartLine++;
				// 		} else {
				// 			foundEntitiesIndex++;
				// 		}
				// 	}
				// }

				HashSet<String> entitiesOfOtherTypesAlreadyFoundSet = new HashSet<String>(entitiesOfOtherTypesAlreadyFound);

				while(actualStartLine++ < actualEndLine && (line = this.getNextLine()) != null && (numberOfEntitiesToFind-- > 0 || typeSetSize > 1 || entityId != -1)) {
					String[] split = line.split(",");
					//	System.out.println(" 3rd parseint --> " + line);
					int v = Integer.parseInt(split[0].trim());
					if(v != type)
						break;
					String label = split[2].trim();
					String entity = split[1].trim();
					boolean keywordMatch = false;
					if(keyword.isEmpty()) {
						keywordMatch = true;
					} else {
						if(label.length() >= keyword.length() && keyword.compareToIgnoreCase(label.substring(0, keyword.length())) == 0) {
							keywordMatch = true;
						}
					}
					if(keywordMatch) {
						if(smallestSet || entitiesOfOtherTypesAlreadyFoundSet.contains(entity+","+label)) {
							newTypes.add(entity+","+label);
						}
					}
				}
				System.out.println("Elapsed time 4 = "+(System.currentTimeMillis()-start));
			}
		} catch(NumberFormatException nfe) {
			System.out.println("Exception around line ID ======= " + fileLineIndex);
			nfe.printStackTrace();
		} catch(IOException ioe) {
			System.out.println("Line ID =========== " + fileLineIndex);
			System.out.println("Low, mid, lastmid, high = " + low + " " + mid + " " + lastMid + " " + high);
			System.out.println("Number of steps taken = " + numberOfSteps);
			ioe.printStackTrace();
		}
		return newTypes;
	}
	/**
	 * This is a specialized method only to find the start and end lines of a given entity-keyword for a type-id.
	 * For a given type, find all entities that start with the "keyword". This is to be used for the node edit feature in the GUI
	 * where one tries to search for entity values a "?" node.
	 * @param type
	 * @param start
	 * @param keyword
	 * @return
	 */
	private Boundary rightBinarySearchForKeywordInTypeInstanceFile(int type, long start, String keyword) {
		Boundary bound = null;
		long low = start;
		long high = getLastOccurrence(start, type);
		long endRow = high;
		long mid = (low+high) >>> 1;
		int keyLen = keyword.length();
		long fileLineIndex = 0;
		//long lnum = 0;
		long first = 0;
		boolean found = false;
		try {
			while(low <= high) {
				mid = low + (high - low)/2;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] fullStr = line.split(",", 3);

				String str;
				if(fullStr[2].trim().length() < keyLen)
					str = fullStr[2].trim();
				else
					str = fullStr[2].trim().substring(0, keyLen);
				System.out.println("---------> "+line);

				if(str.compareToIgnoreCase(keyword) >= 0) {
					high = mid-1;
					if(str.compareToIgnoreCase(keyword) == 0) {
						first = mid;
						found = true;
					}
				} else {
					low = mid + 1;
				}
			}

			if(found) {
				low = first;
				high = endRow;
				long last = first;
				while(low <= high) {
					//lnum++;
					mid = low + (high - low)/2;
					fileLineIndex = mid*BUF_SIZE;
					this.seek(fileLineIndex);
					String line = this.getNextLine();
					String[] fullStr = line.split(",", 3);

					String str;
					if(fullStr[2].trim().length() < keyLen)
						str = fullStr[2].trim();
					else
						str = fullStr[2].trim().substring(0, keyLen);

					if(str.compareToIgnoreCase(keyword) <= 0) {
						low = mid + 1;
						if(str.compareToIgnoreCase(keyword) == 0) {
							last = mid;
						}
					} else {
						high = mid-1;
					}
				}
				bound = new Boundary();
				bound.start = first;
				bound.end = last;

			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return bound;
	}

	/**
	 * This method gets all the instance IDs associated with a type.
	 * @param type
	 * @return
	 */
	public HashSet<Integer> getTypeInstances(int type) {
		HashSet<Integer> instances = null;
		boolean found = false;
		long low = 0;
		long high = numberOfLines-1;
		long mid = (numberOfLines-1)/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] split = line.split(",");
				int v = Integer.parseInt(split[0].trim());
				if(v == type) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[0].trim()) == type)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < type) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high) >>> 1;
			}
			if(found) {
				instances = new HashSet<Integer>();
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numberOfLines && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					int v = Integer.parseInt(split[0].trim());
					if(v != type)
						break;
					instances.add(Integer.parseInt(split[1].trim()));
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return instances;
	}

	private ArrayList<String> getVertexNeighborsTEST(int vertex) throws IOException {
		ArrayList<String> neighbors = null;//new ArrayList<ObjNodeIntProperty>();
		//int lineSize = 28;//Integer.parseInt(getProp(PropertyKeys.datagraphAlignmentLength));
		//BufferedRandomAccessFile raf = new BufferedRandomAccessFile("/home/nj/Desktop/testnewsrc", "r", lineSize);
		long numOfTotalEdges = 10;
		boolean found = false;
		long low = 0;
		long high = 10;
		long mid = 10/2;
		long lastMid = 0;
		long fileLineIndex = 0;
		try {
			while(mid != lastMid && low <= high && high >= low) {
				lastMid = mid;
				fileLineIndex = mid*BUF_SIZE;
				this.seek(fileLineIndex);
				String line = this.getNextLine();
				String[] split = line.split(",");
				int v = Integer.parseInt(split[1].trim());
				if(v == vertex) {
					if(mid == 0) {
						found = true;
						break;
					}
					long prevFileIndex = (mid-1)*BUF_SIZE;
					this.seek(prevFileIndex);
					String prevLine = this.getNextLine();
					String[] prevSplit = prevLine.split(",");
					if(Integer.parseInt(prevSplit[1].trim()) == vertex)
						high = mid-1;
					else {
						found = true;
						break;
					}
				}
				else if(v < vertex) {
					low = mid+1;
				}
				else {
					high = mid-1;
				}
				mid = (low+high)/2;
			}
			if(found) {
				//		System.out.println("FOUND");
				this.seek(fileLineIndex);
				String line = null;
				while(mid++ < numOfTotalEdges && (line = this.getNextLine()) != null) {
					String[] split = line.split(",");
					int v = Integer.parseInt(split[1].trim());
					if(v != vertex)
						break;
					//System.out.println(line);
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return neighbors;
	}

	public static void main(String[] args) {
		try {
			// Example code on how to access the methods in this class.
			// The max length of a line is 33 in freebase_datagraph,BUT MUST add +1, because the last character is "\n"
			/*String filepath = "../data/input/freebase/freebase_edgetypes-idsorted_instances_lang_en-clean-nounicode-padded";
			int len = 275;
			long numOfLines = 16440913;
			int findele = 47412424;*/


			String filepath = "../data/input/freebase/freebase_domain-idsorted_instances_lang_en-clean-nounicode-padded";
			int len = 275;
			long numOfLines = 16440621;
			int findele = 116864;

			/*String filepath = "../data/input/freebase/freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded";
			int len = 68;
			long numOfLines = 3351;
			int findele = 49614525;*/


			/*String filepath = "../data/input/freebase/freebase_instances-idsorted_edgetypes_lang_en-nounicode-padded";
			int len = 68;
			long numOfLines = 30463040;
			int findele = 3807589;*/

			/*String filepath = "../data/input/freebase/freebase_lang_en_first_nodes-nounicode-padded";
			int len = 267;
			long numOfLines = 23798293;*/

			/*String filepath = "../data/input/freebase/freebase_entities_idsorted_label_lang_en-clean-nounicode-padded";
			int len = 266;
			long numOfLines = 10041467;
			int findele = 49730804;*/

			/*String filepath = "../data/input/freebase/freebase_entities_labelsorted_first_id_lang_en-clean-nounicode-padded";
			int len = 267;
			long numOfLines = 10041467;*/
			//int findele = 49730804;

			/*String filepath = "../data/input/freebase/freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded";
			int len = 60;
			long numOfLines = 3357;*/


			BufferedRandomAccessFile raf = new BufferedRandomAccessFile(filepath, "r", numOfLines, len);

			// read the first line
			long index = 2*raf.BUF_SIZE;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());
			// read the 4th line.
			index = 4*raf.BUF_SIZE;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());

			index = 6*raf.BUF_SIZE;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());
			index = 7*raf.BUF_SIZE;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());
			index = 1678*raf.BUF_SIZE;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());
			index = 1679*len;
			System.out.println(index);
			raf.seek(index);
			System.out.println(raf.getNextLine());
			System.out.println("---------------------");
			ArrayList<String> arr = new ArrayList<String>();
			//raf.getNodeIndexValues(findele, arr);
			//raf.getAllNodesValue(arr, 0, 5);
			//raf.completeKeyword("forr", arr, 0, 1500);
			//raf.getNodeLabel(findele);
			// raf.getNodeIndexValuesFilteredKeyword(findele, "forrest g", arr, 0, 100);
			System.out.println("\n----------------------------------- Printing Final Answers ------------------------------------ \n");
			if(arr != null) {
				for(String a : arr) {
					System.out.println(a);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
