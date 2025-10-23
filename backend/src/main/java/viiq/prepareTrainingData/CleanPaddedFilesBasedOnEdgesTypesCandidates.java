package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class CleanPaddedFilesBasedOnEdgesTypesCandidates {
	Config conf = null;
	HashSet<Integer> typesToConsider = new HashSet<Integer>(); 

	public static void main(String[] args) {
		CleanPaddedFilesBasedOnEdgesTypesCandidates cp = new CleanPaddedFilesBasedOnEdgesTypesCandidates();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		cp.deleteUnwantedTypesFromAllFiles();
	}

	private void readTypesWithCandidateEdges() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.typesWithCandidateEdges));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				typesToConsider.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private int deleteLines(String inputFilename, String outputFileName, int typeCol) {
		int linesKept = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilename));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(",");
				try {
					if(typesToConsider.contains(Integer.parseInt(spl[typeCol].trim()))) {
						bw.write(line + "\n");
						linesKept++;
					}
				} catch(Exception nfe) {
					nfe.printStackTrace();
				}
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return linesKept;
	}

	public void deleteUnwantedTypesFromAllFiles() {
		readTypesWithCandidateEdges();
		System.out.println("done loading types to consider");

		String inFolder = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase/originalPaddedFiles/";

		String filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded";
		String inFilename = inFolder + filename;
		String outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPadded);
		int lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded-col-2";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol2);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded-col-3";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol3);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded-col-4";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol4);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded-col-5";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol5);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded-col-6";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol6);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded-col-2";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol2);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded-col-3";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol3);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded-col-4";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol4);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded-col-5";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol5);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode-padded-col-6";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol6);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes-idsorted_instances-idsorted-clean-nounicode-padded";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesIdSortedInstancesIdSortedPadded);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_idsorted_label_lang_en-clean-nounicode-padded";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesIdSortedLangEnPaddedFile);
		lines = deleteLines(inFilename, outFilename, 0);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFile);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded-col-2";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol2);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded-col-3";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol3);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded-col-4";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol4);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded-col-5";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol5);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode-padded-col-6";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol6);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		filename = "freebase_instances-idsorted_edgetypes_lang_en-clean-nounicode-padded";
		inFilename = inFolder + filename;
		outFilename = conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndexPaddedFile);
		lines = deleteLines(inFilename, outFilename, 1);
		System.out.println(filename + " : " + lines);

		System.out.println("done!");
	}
}
