package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class CreateLangEnEdgeLabels {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	public static void main(String[] args) {
		CreateLangEnEdgeLabels cl = new CreateLangEnEdgeLabels();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			cl.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cl.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				cl.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				cl.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		cl.createEdgeLabels();
	}
	
	private void createEdgeLabels() {
		try{
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.propertiesMapFile));
			BufferedReader br = new BufferedReader(fr);
			FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.propertiesLangEn));
			BufferedWriter bw = new BufferedWriter(fw);
			String line;
			while((line = br.readLine()) != null) {
				String[] idLabel = line.split("\t");
				int id = Integer.parseInt(idLabel[0]);
				String label_langen;
				if(idLabel[1].contains("-")) {
					label_langen = getConcatenatedPropLabel(idLabel[1].trim());
				}
				else {
					label_langen = getPropLabel(idLabel[1].trim());
				}
				bw.write(id + "\t" + label_langen + "\n");
			}
			br.close();
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String getConcatenatedPropLabel(String label) {
		String[] labels = label.split("-");
		return (getLabelAfterLastSlash(labels[0]) + "-" + getLabelAfterLastSlash(labels[1]));
	}
	
	private String getPropLabel(String label) {
		int lastButOneIndex = StringUtils.ordinalIndexOf(label, "/", 2);
		return(label.substring(lastButOneIndex+1, label.length()));
		//return getLabelAfterLastSlash(label);
	}
	
	private String getLabelAfterLastSlash(String label) {
		int lastIndex = label.lastIndexOf("/");
		return(label.substring(lastIndex+1, label.length()));
	}
}
