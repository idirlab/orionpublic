package viiq.TestWikipedia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class QuickClean {
	
	public static void main(String[] args) {
		QuickClean qc = new QuickClean();
		qc.clean();
	}
	
	private void clean() {
		String file1 = "../data/input/freebase/hmmsrc";
		String file2 = "../data/input/freebase/hmmmobj";
		HashSet<String> killsrc = new HashSet<String>();
		HashSet<String> killobj = new HashSet<String>();
		try {
			FileReader fr1 = new FileReader(file1);
			BufferedReader br1 = new BufferedReader(fr1);
			FileReader fr2 = new FileReader(file2);
			BufferedReader br2 = new BufferedReader(fr2);
			String line = null;
			while((line = br1.readLine()) != null) {
				killsrc.add(line);
			}
			line = null;
			while((line = br2.readLine()) != null) {
				killobj.add(line);
			}
			
			FileReader fr3 = new FileReader("../data/input/freebase/freebase_datagraph_withoutTypesDomains_source");
			BufferedReader br3 = new BufferedReader(fr3);
			FileWriter fw1 = new FileWriter("../data/input/freebase/freebase_datagraph_withoutTypesDomains_source-clean");
			BufferedWriter bw1 = new BufferedWriter(fw1);
			line = null;
			while((line = br3.readLine()) != null) {
				if(killsrc.contains(line))
					continue;
				bw1.write(line + "\n");
			}
			
			
			FileReader fr4 = new FileReader("../data/input/freebase/freebase_datagraph_withoutTypesDomains_object");
			BufferedReader br4 = new BufferedReader(fr4);
			FileWriter fw2 = new FileWriter("../data/input/freebase/freebase_datagraph_withoutTypesDomains_object-clean");
			BufferedWriter bw2 = new BufferedWriter(fw2);
			line = null;
			while((line = br4.readLine()) != null) {
				if(killobj.contains(line))
					continue;
				bw2.write(line+"\n");
			}
			br1.close();
			br2.close();
			br3.close();
			br4.close();
			bw1.close();
			bw2.close();
		} catch(IOException ioe) {
			
		}
	}

}
