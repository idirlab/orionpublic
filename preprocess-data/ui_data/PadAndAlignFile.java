//import it.unimi.dsi.lang.MutableString;

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

public class PadAndAlignFile {

	public void generateSortedDatagraphFile(String inputFilepath, String outputFilepath) {
		int maxLength = 0;
		try {
			FileReader fr = new FileReader(inputFilepath);
			BufferedReader br = new BufferedReader(fr);
			//BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilepath), StandardCharsets.UTF_8));

			int lines = 0;
			String line = null;
			while((line = br.readLine()) != null) {
				lines++;
				if(line.getBytes("UTF-8").length > maxLength)
					maxLength = line.getBytes("UTF-8").length;
				//	if(line.length() > 100)
				//		System.out.println(line);
			}
			System.out.println("Maxlength for file " + outputFilepath + " = " + maxLength);
			System.out.println("Number of lines for file " + inputFilepath + " = " + lines);
			br.close();
			lines = 0;
			FileReader fr1= new FileReader(inputFilepath);
			BufferedReader br1 = new BufferedReader(fr1);
			//BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilepath), StandardCharsets.UTF_8));
			line = null;

			FileWriter fw1 = new FileWriter(outputFilepath);
			BufferedWriter bw1 = new BufferedWriter(fw1);
			//BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilepath), StandardCharsets.UTF_8));
			while((line = br1.readLine()) != null) {
				lines++;
				int len = line.getBytes("UTF-8").length;
				String ms = new String();
				if(len < maxLength) {
						while(maxLength-len > 0) {
						ms += " ";
						len++;
					}
				}
				bw1.write(line + ms + "\n");
	//			System.out.println(line+" "+(int)(line.length()+ms.length()));
			}

			System.out.println("Number of lines for file " + outputFilepath + " = " + lines + "\n");
			br1.close();
			bw1.close();
		} catch(IOException ioe) {

		} catch(Exception e) {

		}
	}


	public static void main(String[] args) {
		PadAndAlignFile pad = new PadAndAlignFile();
		/*
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
		} */



		String[] fileNames = {
					"freebase_datagraph_sorted_by_predicate_source",
					"entity_edge_cnt",
					"freebase_datagraph_withoutTypesDomains_source-clean_only_entity_intermediate",
					"freebase_datagraph_withoutTypesDomains_object-clean_only_entity_intermediate",
					"freebase_instances-idsorted_edgetypes_lang_en-clean-nounicode",
					"freebase_entities_idsorted_label_lang_en-clean-nounicode",
					"freebase_entities_idsorted_label_lang_en-clean-nounicode_desc",

					"freebase_entities_labelsorted_first_id_lang_en-clean-nounicode",
					"freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode",
					"freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode",
					"freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode",
					"freebase_domain-idsorted_instances_lang_en-clean-nounicode",
					"freebase_domain-idsorted_instances_lang_en-clean-nounicode_desc",
					"freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode_desc",
					"freebase_entities_labelsorted_first_id_lang_en-clean-nounicode_desc"
				};

    String filePath = args[0];

		for(int i = 0; i < fileNames.length; i++) {
			if(i > 0) continue;
			String fn = filePath+fileNames[i];
			pad.generateSortedDatagraphFile(fn, fn+"-padded" );

			if(i > 6) {
				for(int j = 2; j <= 6; j++) {
					pad.generateSortedDatagraphFile(fn+"-col-"+(char)(j+'0'), fn+"-padded-col-"+(char)(j+'0'));
				}
			}
		}

		System.out.println("Done!");
	}
}
