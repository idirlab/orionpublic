package viiq.otherClassifiers.cba;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class Cba {

	private ArrayList<Integer> sendGet(String url, int[] features, int[] candidates) throws Exception {
 
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setReadTimeout(25000000);
        con.setConnectTimeout(25000000);
		con.setRequestMethod("GET");
		con.setDoInput(true);
		con.setDoOutput(true);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				
		for (int i=0; i<features.length; i++)
			params.add(new BasicNameValuePair("features", Integer.toString(features[i])));
		
		for (int i=0; i<candidates.length; i++)
			params.add(new BasicNameValuePair("candidates", Integer.toString(candidates[i])));

		String t = getQuery(params);
//		System.out.println(t);
		OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
		
		int responseCode = con.getResponseCode();

		//output part
//		System.out.println("\nSending 'GET' request to URL : " + url);
//		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		ArrayList<Integer> rankedEdges = new ArrayList<Integer>();
		while ((inputLine = in.readLine()) != null) {
			inputLine = inputLine.trim();
			if(inputLine.startsWith("}") || inputLine.startsWith("{") || inputLine.startsWith("\"result") || inputLine.startsWith("]"))
				continue;
			if(inputLine.endsWith(",")) 
				inputLine = inputLine.substring(0, inputLine.length()-1);
			rankedEdges.add(Integer.parseInt(inputLine));
			response.append(inputLine);
		}
		in.close();
		//System.out.println(response.toString());
		return rankedEdges;
	}
 
	private String getQuery(List<NameValuePair> params)
	        throws UnsupportedEncodingException {
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (NameValuePair pair : params) {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }

	    return result.toString();
	}
	
	public int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		int[] features = new int[history.size()];
		for(int i=0; i<features.length; i++) {
			features[i] = history.get(i);
		}
		int[] candidates = new int[candidateEdges.size()];
		Iterator<Integer> iter = candidateEdges.iterator();
		int i=0;
		while(iter.hasNext()) 
			candidates[i++] = iter.next();
		ArrayList<Integer> rankedEdges = null;
		try {
			rankedEdges = sendGet("http://127.0.0.1:5000/rank_candidates", features, candidates);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if(rankedEdges != null && !rankedEdges.isEmpty())
			return rankedEdges.get(0);
		else
			return -1;
	}
	
	public ArrayList<Integer> rankCandidateEdges(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		int[] features = new int[history.size()];
		for(int i=0; i<features.length; i++) {
			features[i] = history.get(i);
		}
		int[] candidates = new int[candidateEdges.size()];
		Iterator<Integer> iter = candidateEdges.iterator();
		int i=0;
		while(iter.hasNext()) 
			candidates[i++] = iter.next();
		ArrayList<Integer> rankedEdges = null;
		try {
			rankedEdges = sendGet("http://127.0.0.1:5000/rank_candidates", features, candidates);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rankedEdges;
	}
	
	public void learnModel() {
		// call code that loads the model file and invokes the python web service.
		System.out.println("PLEASE MAKE SURE THE PYTHON WEB SERVICE IS UP AND RUNNING. Go to src-maven directory and execute run_launchPythonSVDWebServer.sh");
		/*String launchPythonWebService = "python ./src/main/java/viiq/otherClassifiers/svd/app.py";
		try {
			Runtime.getRuntime().exec(launchPythonWebService);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}*/
	}
	
	public static void main(String[] args) throws Exception {
		int[] features = new int[]{-30000, -30129, -30648, -30112, -30043, -30047, -30100, -30137, -30070, -30012, -30001, -30141, -30003, -30048, -30004, -30733, -28182, -28181, -28179, -28176, -28157, -27447, -27442, -27437, -27424, -27407, -27406, -27404, -27403, -27400, -27392, -27390, -27388, -27386, -27376, -27375, -27374, -27373, -27364, -27277, -26384, 27449, 30002};
		int[] candidates = new int[]{27399, 27382, 27446, 27843, 30596, 30275, 30723, 27869};
		Cba cba = new Cba();
//		rs.learnModel();
//		System.out.println("Launched python web service");
		cba.sendGet("http://127.0.0.1:5000/rank_candidates", features, candidates);
	}

}
