package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class CreateDbpediaWikiMapping {
	public static void main(String[] args) {
		CreateDbpediaWikiMapping cd = new CreateDbpediaWikiMapping();
		cd.createWikiMap();
	}
	
	private void createWikiMap() {
		System.out.println("Started");
		String freebaseInput = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase/enwikiFreebase_new";
		String dbpediaEntityMap = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/dbpedia/dbpedia_entities_idsorted_label_lang_en-clean-nounicode";
		String wikiout = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/dbpedia/enwikiDbpedia";
		
		try {
			BufferedReader brfb = new BufferedReader(new FileReader(freebaseInput));
			BufferedReader brdb = new BufferedReader(new FileReader(dbpediaEntityMap));
			String wikiprefix = "http://en.wikipedia.org/wiki/";
			String dbpediaprefix = "http://dbpedia.org/resource/";
			HashMap<String, Integer> fbLinkToId = new HashMap<String, Integer>();
			HashMap<Integer, ArrayList<String>> fbIdToLinks = new HashMap<Integer, ArrayList<String>>();
			String line = null;
			while((line = brfb.readLine()) != null) {
				String[] spl = line.split("\t");
				int id = Integer.parseInt(spl[0].trim());
				fbLinkToId.put(spl[1].trim(), id);
				if(fbIdToLinks.containsKey(id)) {
					fbIdToLinks.get(id).add(spl[1].trim());
				} else {
					ArrayList<String> arr = new ArrayList<String>();
					arr.add(spl[1].trim());
					fbIdToLinks.put(id, arr);
				}
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(wikiout));
			line = null;
			while((line = brdb.readLine()) != null) {
				String[] spl = line.split("\t");
				int id = Integer.parseInt(spl[0].trim());
				String label = spl[1].trim();
				if(label.contains("<") && label.contains(">")) {
					label = label.replace("<", "");
					label = label.replace(">", "");
				}
				label = label.replace(dbpediaprefix, wikiprefix);
				if(fbLinkToId.containsKey(label)) {
					int fbid = fbLinkToId.get(label);
					ArrayList<String> wikilabels = fbIdToLinks.get(fbid);
					for(String link : wikilabels) {
						bw.write(id + "\t" + link + "\n");
					}
				} else {
					bw.write(id + "\t" + label + "\n");
				}
			}
			brfb.close();
			brdb.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Done!");
	}
}
