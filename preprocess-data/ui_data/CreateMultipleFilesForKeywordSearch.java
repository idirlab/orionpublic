//package viiq.prepareTrainingData;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.nio.charset.StandardCharsets;
//import org.apache.commons.configuration.ConfigurationException;

//import viiq.graphQuerySuggestionMain.Config;
//import viiq.utils.PropertyKeys;

public class CreateMultipleFilesForKeywordSearch {
//	Config conf = null;

	public static void main(String[] args) {
		CreateMultipleFilesForKeywordSearch cm = new CreateMultipleFilesForKeywordSearch();
		/*
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else {
			try	{
				cm.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce) {
				System.out.println("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe) {
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		System.out.println("Starting creating multiple files!");
		/**
		 * Create the -col-2/3/4/5/6 files using the original file
		 */

		String filePath = args[0];

		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode", 0);
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_entities_labelsorted_first_id_lang_en-clean-nounicode", 0);
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_entities_labelsorted_first_id_lang_en-clean-nounicode_desc", 0);

		
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode", 2);
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode", 2);
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_domain-idsorted_instances_lang_en-clean-nounicode", 2);

		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode_desc", 2);
		cm.createFilesSortedOnDifferentColumns(filePath+"freebase_domain-idsorted_instances_lang_en-clean-nounicode_desc", 2);


		/**
		 * For already created -col-2/3/4/5/6 files, delete lines that do not have the correct number of words (this is after
		 * the multiple space issue was resolved using sed on the -col- files).
		 */
		/*
		cm.editExistingColumnFiles(cm.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFile), 0);
		cm.editExistingColumnFiles(cm.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFile), 0);
		cm.editExistingColumnFiles(cm.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFile), 2);
		cm.editExistingColumnFiles(cm.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEn), 2);
		cm.editExistingColumnFiles(cm.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEn), 2);
		*/
		System.out.println("Done with all files");
	}

	/**
	 * The files normally have an integer ID along with its corresponding string value (label). Mention the column in which the label starts.
	 * @param fileName
	 * @param labelStartColumn
	 */
	void createFilesSortedOnDifferentColumns(String fileName, int labelStartColumn) {
		// the original file is anyway sorted on the first word. Start from the second word onwards.
		int labelTokenNumber = 2;
		int linesWritten = 0;
		System.out.println("Input file = " + fileName);
		do {
			linesWritten = 0;
			try {
				BufferedReader br = new BufferedReader(new FileReader(fileName));
				BufferedWriter bw = new BufferedWriter(new FileWriter(getFileName(fileName, labelTokenNumber)));
				//BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
				//BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFileName(fileName, labelTokenNumber)), "UTF-8"));

				String line = null;
				String id = null;
				List<String[]> outputSet = new ArrayList<String[]>();
				while(true) {
					line = br.readLine();
					String[] label = null;
					if(line != null)
						label = line.split("," , labelStartColumn+1);
					if(line == null || (id != null && !id.equals(label[0]))){
                                                Collections.sort(outputSet, new Comparator<String[]>() {
                                                        public int compare(String[] strings, String[] otherStrings) {
                                                                return strings[1].toLowerCase().compareTo(otherStrings[1].toLowerCase());
                                                        }
                                                });
                                                for(int i = 0; i < outputSet.size(); i++) {
                                                        bw.write(outputSet.get(i)[0]+outputSet.get(i)[1]+"\n");
                                        	}
						outputSet.clear();
						if(line ==null)
							break;
					}
					//System.out.println(linesWritten);
					if(label.length <= labelStartColumn) {
						System.out.println("ERROR line: " + line);
						continue;
					}
					String[] tokens = label[labelStartColumn].trim().split(" ");
					//System.out.println(tokens);
					String[] output = new String[2];
					output[0] = output[1] = "";
					for(int i = 0; i < labelStartColumn; i++)
						output[0] += (label[i] + ",");
					for(int i = 0; i < tokens.length; i++) {
						String s = tokens[i];
						if(i < (tokens.length - 1))
							s += " ";
						if(i < labelTokenNumber - 1)
							output[0] += s;
						else
							output[1] += s;
					}

                                        //System.out.println(output[0]+output[1]);

					if(tokens != null && tokens.length < labelTokenNumber) {
						//System.out.println("ss");
						continue;
					}
					outputSet.add(output);


					linesWritten++;
					if(labelStartColumn > 0)
						id = label[0];
				}
				bw.close();
				br.close();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
			System.out.println("New file = " + getFileName(fileName, labelTokenNumber) + " :: # of lines = " + linesWritten);
			labelTokenNumber++;
		} while(linesWritten > 0 && labelTokenNumber <= 6);
		System.out.println();
	}

	String getFileName(String fname, int col) {
		return fname + "-col-" + col;
	}

	/**
	 * ONLY to work on already existing -col- files!
	 * The files normally have an integer ID along with its corresponding string value (label). Mention the column in which the label starts.
	 * @param fileName
	 * @param labelStartColumn
	 */

	void editExistingColumnFiles(String fileName, int labelStartColumn) {
		// the original file is anyway sorted on the first word. Start from the second word onwards.
		int labelTokenNumber = 2;
		int linesWritten = 0;
		System.out.println("Input file = " + fileName);
		do {
			linesWritten = 0;
			try {
				BufferedReader br = new BufferedReader(new FileReader(getFileName(fileName, labelTokenNumber)));
				BufferedWriter bw = new BufferedWriter(new FileWriter(getFileName(fileName, labelTokenNumber)+".t"));
				String line = null;
				while((line = br.readLine()) != null) {
					String[] label = line.split(",");
					if(label.length <= labelStartColumn) {
						System.out.println("ERROR line: " + line);
						continue;
					}
					String[] tokens = label[labelStartColumn].trim().split(" ");
					if(tokens != null && tokens.length < labelTokenNumber)
						continue;
					bw.write(line + "\n");
					linesWritten++;
				}
				bw.close();
				br.close();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
			System.out.println("New file = " + getFileName(fileName, labelTokenNumber) + " :: # of lines = " + linesWritten);
			labelTokenNumber++;
		} while(linesWritten > 0 && labelTokenNumber <= 6);
		System.out.println();
	}

}
