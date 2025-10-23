import orion.backend.src.main.java.viiq.clientServer.server.LoadData;
import orion.backend.src.main.java.viiq.utils.PropertyKeys;
import orion.backend.src.main.java.viiq.graphQuerySuggestionMain.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CreatePropertyTables {

  public void propertyTableCreator(String inputFilePath, String outputDirPath) {
    try {
      FileReader fr = new FileReader(inputFilePath);
      BufferedReader br = new BufferedReader(fr);

      String line = null;
      String prevEdge = null;
      FileWriter fw = null;
      BufferedWriter bw = null;
      while((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        String source = tokens[1];
        String edge = tokens[2];
        String target = tokens[3];

        if(prevEdge == null || !edge.equals(prevEdge)) {
          if(prevEdge != null) {
            bw.close();
          }
          fw = new FileWriter(outputDirPath+edge);
          bw = new BufferedWriter(fw);
        }
        prevEdge = edge;
        String s = source +","+ target;
        int paddingLength = 17-s.length();
        while(paddingLength-- > 0) {
          s += " ";
        }
        bw.write(s + "\n");
      }
      br.close();
      bw.close();
    } catch(IOException ioe) {
      ioe.printStackTrace();
      System.out.println(ioe);
    } catch(Exception e) {
      e.printStackTrace();
      System.out.println(e);
    }
  }

  public static void main(String[] args) {
    CreatePropertyTables cpt = new CreatePropertyTables();
    cpt.propertyTableCreator(args[0]+"freebase_datagraph_sorted_by_predicate_source", args[0]+"sourcePropertyTables/");
    cpt.propertyTableCreator(args[0]+"freebase_datagraph_sorted_by_predicate_target", args[0]+"targetPropertyTables/");
  }
}
