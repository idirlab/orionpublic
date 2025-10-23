package viiq.barcelonaToFreebase;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

class DbpediaQueryLogProcessorThread implements Runnable {
	//private final Object counterLock = new Object();
	String inputFilePath = "";
	String outputFilePath = "";
	HashMap<String, Integer> propMap = new HashMap<String, Integer>();
	public DbpediaQueryLogProcessorThread(String filepath, HashMap<String, Integer> map) {
		inputFilePath = filepath;
		outputFilePath = inputFilePath + ".out";
		propMap = map;
	}
	
	private String correctLink(String link) {
		String correct = "";
		correct = link.replace("property", "ontology");
		correct = link.replace("resource", "ontology");
		if(correct.contains("<http<http") && correct.contains("///")) {
/*			synchronized (counterLock) {
			System.out.print(correct);
			}*/
			correct = correct.replace("<http<http", "<http");
			int slashindex = correct.indexOf("///");
			int rightindex = correct.indexOf(">");
			correct = correct.replace(correct.substring(slashindex, rightindex+1), "/");
/*			synchronized (counterLock) {
			System.out.println("\t" + correct);
			}*/
		}
		return correct;
	}
	
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(",");
				MutableString ms = new MutableString();
				for(String link : spl) {
					String url = correctLink(link.trim().toLowerCase());
					if(propMap.containsKey(url)) {
						ms.append(propMap.get(url)).append(",");
					}
				}
				if(!ms.toString().isEmpty())
					bw.write(ms.toString()+"\n");
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

public class DbpediaQueryLogProcessor {
	Config conf = null;
	
	private HashMap<String, Integer> populatePropMap() {
		HashMap<String, Integer> propMap = new HashMap<String, Integer>();
		try {
			String filepath = conf.getInputFilePath(PropertyKeys.propertiesMapFile);
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split("\t");
				propMap.put(spl[1].trim().toLowerCase(), Integer.parseInt(spl[0].trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return propMap;
	}
	
	private void processQueryLog() {
		HashMap<String, Integer> propMap = populatePropMap();
		String logfolder = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/dbpedia/dbpedia_log/";
//		String logfolder = "/home/nj/Desktop/tfold/";
		File[] listOfInputQueryFiles = new File(logfolder).listFiles();
		try {
			ArrayList<Thread> at = new ArrayList<Thread>();
			int nthreads = 0;
			for(int i=0; i<listOfInputQueryFiles.length; i++) {
				// Each file must contain the partial query graph and the target query graph.
				if(listOfInputQueryFiles[i].isFile()) {
					DbpediaQueryLogProcessorThread dt = new DbpediaQueryLogProcessorThread(listOfInputQueryFiles[i].getAbsolutePath(), propMap);
					Thread thr = new Thread(dt);
					at.add(thr);
					thr.setName(i+"");
					nthreads++;
					thr.start();
				}
			}
			for(int i=0; i<nthreads; i++) {
				at.get(i).join();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		DbpediaQueryLogProcessor btf = new DbpediaQueryLogProcessor();
		if(args.length < 1) {
			System.out.println("Need an input properties file! Exiting program...");
			return;
		} else {
			try {
				btf.conf = new Config(args[0]);
			} catch(ConfigurationException ce) {
				System.out.println("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			} catch(IOException ioe) {
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		System.out.println("Started");
		btf.processQueryLog();
		System.out.println("Done!");
	}
}
