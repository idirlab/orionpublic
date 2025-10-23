package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class CreateQueryFiles {
	public static void main(String[] args) {
		CreateQueryFiles cq = new CreateQueryFiles();
		cq.createFiles();
	}
	private void createFiles() {
		String input = "queries.txt";
		String output = "out1/";
		HashMap<Integer, Integer> filenum = new HashMap<Integer, Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(input));
			String line = null;
			while((line = br.readLine()) != null) {
				String fullstr = "";
				String query = "";
				String[] q = line.split(";");
				int cnt = 1;
				if(filenum.containsKey(q.length)) {
					cnt = filenum.get(q.length);
					cnt++;
				}
				filenum.put(q.length, cnt);
				String filename = output + q.length + "-" + cnt + ".txt";
				BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
				for(int i=0; i<q.length-1; i++) {
					String edge = q[i].trim();
					String[] tuple = edge.split(",");
					query += tuple[1].trim() + "\t" + tuple[0].trim() + "\t" + tuple[2].trim() + "\n";
					if(i == 0)
						fullstr += query + "+++++\n";
				}
				String[] tuple = q[q.length-1].trim().split(",");
				query = query + tuple[1].trim() + "\t" + tuple[0].trim() + "\t" + tuple[2].trim() + "\n";
				bw.write(fullstr+query);
				bw.close();
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
