/**
 * Given an input file, this class creates a padded and aligned output file. This ASSUMES THE INPUT IS A SORTED FILE ACCORDING TO NECESSITY.
 */
package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class PadAndAlignFile {

	final Logger logger = Logger.getLogger(getClass());
	Config conf = null;

	public void generateSortedDatagraphFile(String inputFilepath, String outputFilepath) {
		int maxLength = 0;
		try {
			FileReader fr = new FileReader(inputFilepath);
			BufferedReader br = new BufferedReader(fr);
			int lines = 0;
			String line = null;
			while((line = br.readLine()) != null) {
				lines++;
				if(line.length() > maxLength)
					maxLength = line.length();
				//	if(line.length() > 100)
				//		System.out.println(line);
			}
			System.out.println("Maxlength for file " + outputFilepath + " = " + maxLength);
			System.out.println("Number of lines for file " + inputFilepath + " = " + lines);
			br.close();
			lines = 0;
			FileReader fr1= new FileReader(inputFilepath);
			BufferedReader br1 = new BufferedReader(fr1);
			line = null;

			FileWriter fw1 = new FileWriter(outputFilepath);
			BufferedWriter bw1 = new BufferedWriter(fw1);

			while((line = br1.readLine()) != null) {
				lines++;
				int len = line.length();
				MutableString ms = new MutableString();
				if(len < maxLength) {
					while(maxLength-len > 0) {
						ms = ms.append(" ");
						len++;
					}
				}
				bw1.write(line + ms + "\n");
			}
			System.out.println("Number of lines for file " + outputFilepath + " = " + lines + "\n");
			br1.close();
			bw1.close();
		} catch(IOException ioe) {

		} catch(Exception e) {

		}
	}

	/*public void generateSortedActualDatagraphFile(String inputFilepath, String outputFilepath, int nol) {
		int maxLength = 0;
		try {
			int lines = 0;
			String line = null;
			if(nol == -1) {
				FileReader fr = new FileReader(inputFilepath);
				BufferedReader br = new BufferedReader(fr);
				while((line = br.readLine()) != null) {
					lines++;
					if(line.length() > maxLength)
						maxLength = line.length();
				}
				System.out.println("Maxlength for file " + outputFilepath + " = " + maxLength);
				System.out.println("Number of lines for file " + inputFilepath + " = " + lines);
				br.close();
				lines = 0;
				line = null;
			}
			FileReader fr1= new FileReader(inputFilepath);
			BufferedReader br1 = new BufferedReader(fr1);

			FileWriter fw1 = new FileWriter(outputFilepath);
			BufferedWriter bw1 = new BufferedWriter(fw1);

			while((line = br1.readLine()) != null) {
				lines++;
				int len = line.length();
				MutableString ms = new MutableString();
				if(len < maxLength) {
					while(maxLength-len > 0) {
						ms = ms.append(" ");
						len++;
					}
				}
				bw1.write(line + ms + "\n");
			}
			System.out.println("Number of lines for file " + outputFilepath + " = " + lines + "\n");
			br1.close();
			bw1.close();
		} catch(IOException ioe) {

		} catch(Exception e) {

		}
	}*/

	public static void main(String[] args) {
		PadAndAlignFile pad = new PadAndAlignFile();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			pad.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else {
			try	{
				pad.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce) {
				System.out.println("Error in properties file configuration! Exiting program...");
				pad.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe) {
				System.out.println("IO exception while reading the properties file! Exiting program...");
				pad.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		// this is for the data graph
		/*
		 * UNTESTED CODE
		 * pad.generateSortedActualDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.datagraphFileOnlyEntityIntermediate), 
				pad.conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile), -1);
		pad.generateSortedActualDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.datagraphFileOnlyEntityIntermediate), 
				pad.conf.getInputFilePath(PropertyKeys.datagraphObjectAlignedFile), 
				Integer.parseInt(pad.conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines)));*/
		//////////////////////////
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFile), 
					pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFileCol2), 
				pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol2));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFileCol3), 
				pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol3));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFileCol4), 
				pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol4));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFileCol5), 
				pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol5));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFileCol6), 
				pad.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol6));
		System.out.println("==================================================================\n");
		
		
		/*pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesIdSortedInstancesIdSorted), 
				pad.conf.getInputFilePath(PropertyKeys.typesIdSortedInstancesIdSortedPadded));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndex), 
				pad.conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndexPaddedFile));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFile), 
					pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFile));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFileCol2), 
				pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol2));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFileCol3), 
				pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol3));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFileCol4), 
				pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol4));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFileCol5), 
				pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol5));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedFileCol6), 
				pad.conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol6));
		System.out.println("===================================================\n");*/
		
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFile), 
				pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFile));
	//	pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFileCol2), 
		//		pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol2));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFileCol3), 
				pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol3));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFileCol4), 
				pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol4));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFileCol5), 
				pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol5));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedFileCol6), 
				pad.conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol6));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.instancesIdsortedLangEn), 
				pad.conf.getInputFilePath(PropertyKeys.instancesIdsortedLangEnPaddedFile));
		System.out.println("=======================================================\n");
		
		//pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.typesIdSortedLangEn), 
			//	pad.conf.getInputFilePath(PropertyKeys.typesIdSortedLangEnPaddedFile));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEn), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPadded));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnCol2), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol2));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnCol3), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol3));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnCol4), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol4));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnCol5), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol5));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnCol6), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol6));
		System.out.println("================================================================\n");
		
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEn), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPadded));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnCol2), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol2));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnCol3), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol3));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnCol4), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol4));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnCol5), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol5));
		pad.generateSortedDatagraphFile(pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnCol6), 
				pad.conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol6));
		System.out.println("Done!");
	}
}
