package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateDBpediaDataGraphFromDump {
	public static void main(String[] args) {
		CreateDBpediaDataGraphFromDump btf = new CreateDBpediaDataGraphFromDump();
		btf.cleanDump();
	}
	
	private void cleanDump() {
		System.out.println("Started");
		String folder = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/dbpedia/";
		String inputfile = folder+"mappingbased-properties_en";
		String newDumpOutFile = folder+"dbpedia_datagraph_withoutTypesDomains";
		String newDumpRawFile = folder+"mappingbased-properties_en-clean";
		String langenFile = folder+"dbpedia_entities_idsorted_label_lang_en-clean-nounicode";
		String propsLangEnFile = folder+"dbpedia_propertiesMappingFile_lang_en";
		HashMap<String, Integer> propIntMap = new HashMap<String, Integer>();
		HashMap<String, Integer> entityIntMap = new HashMap<String, Integer>();
		int propID = 1;
		int entID = 10;
		int tupleID = 1;
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputfile));
			BufferedWriter bwdump = new BufferedWriter(new FileWriter(newDumpOutFile));
			BufferedWriter bwraw = new BufferedWriter(new FileWriter(newDumpRawFile));
			
			String seealsodelim = "<http://www.w3.org/2000/01/rdf-schema#seeAlso>";
			String namedelim = "<http://xmlns.com/foaf/0.1/name>";
			String diffformdelim = "<http://www.w3.org/2002/07/owl#differentFrom>";
			String intermediatedelim = "__\\d+>";
			Pattern p = Pattern.compile(intermediatedelim);
		//	String strdelim = "\"";
			String entitydelim = "<http://dbpedia.org/resource/";
			
			String line = null;
			while((line = br.readLine()) != null) {
				String[] split = line.split(" ");
				//System.out.println(line + " " + split.length);
				if(split[1].trim().equals(namedelim) || split[1].trim().equals(seealsodelim) || split[1].trim().equals(diffformdelim))
					continue;
				if(!split[2].trim().startsWith(entitydelim) || !split[0].trim().startsWith(entitydelim))
					continue;
				Matcher msrc = p.matcher(split[0].trim());
				Matcher mobj = p.matcher(split[2].trim());
				if(msrc.find() || mobj.find()) 
					continue;
				// now we have a valid line to keep.
				bwraw.write(line + "\n");
				
				int src;
				int prop;
				int obj;
				if(entityIntMap.containsKey(split[0].trim())) 
					src = entityIntMap.get(split[0].trim());
				else {
					src = entID++;
					entityIntMap.put(split[0].trim(), src);
				}
				
				if(entityIntMap.containsKey(split[2].trim())) 
					obj = entityIntMap.get(split[2].trim());
				else {
					obj = entID++;
					entityIntMap.put(split[2].trim(), obj);
				}
				
				if(propIntMap.containsKey(split[1].trim())) 
					prop = propIntMap.get(split[1].trim());
				else {
					prop = propID++;
					propIntMap.put(split[1].trim(), prop);
				}
				MutableString ms = new MutableString();
				ms.append(tupleID++).append(",").append(src).append(",").append(prop).append(",").append(obj).append("\n");
				bwdump.write(ms.toString());
			}
			br.close();
			bwdump.close();
			bwraw.close();
			
			BufferedWriter bwentlangen = new BufferedWriter(new FileWriter(langenFile));
			Iterator<String> iter = entityIntMap.keySet().iterator();
			while(iter.hasNext()) {
				String label = iter.next();
				bwentlangen.write(entityIntMap.get(label) + "\t" + label + "\n");
			}
			
			BufferedWriter bwproplangen = new BufferedWriter(new FileWriter(propsLangEnFile));
			Iterator<String> iter1 = propIntMap.keySet().iterator();
			while(iter1.hasNext()) {
				String label = iter1.next();
				bwproplangen.write(propIntMap.get(label) + "\t" + label + "\n");
			}
			
			bwentlangen.close();
			bwproplangen.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Done!");
	}
}
