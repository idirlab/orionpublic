//package viiq.otherClassifiers.svd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class HttpRequest {

	private static void sendGet(String url, int[] features, int[] candidates) throws Exception {
 
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setReadTimeout(10000);
        con.setConnectTimeout(15000);
		con.setRequestMethod("GET");
		con.setDoInput(true);
		con.setDoOutput(true);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				
		for (int i=0; i<features.length; i++)
			params.add(new BasicNameValuePair("features", Integer.toString(features[i])));
		
		for (int i=0; i<candidates.length; i++)
			params.add(new BasicNameValuePair("candidates", Integer.toString(candidates[i])));

		String t = getQuery(params);
		System.out.println(t);
		OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
		
		int responseCode = con.getResponseCode();
		
		
		//output part
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		System.out.println(response.toString());
 
	}
 
	private static String getQuery(List<NameValuePair> params)
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
	
	public static void main(String[] args) throws Exception {
		int[] features = new int[]{-30000, -30129, -30648, -30112, -30043, -30047, -30100, -30137, -30070, -30012, -30001, -30141, -30003, -30048, -30004, -30733, -28182, -28181, -28179, -28176, -28157, -27447, -27442, -27437, -27424, -27407, -27406, -27404, -27403, -27400, -27392, -27390, -27388, -27386, -27376, -27375, -27374, -27373, -27364, -27277, -26384, 27449, 30002};
		int[] candidates = new int[]{27399, 27382, 27446, 27843, 30596, 30275, 30723, 27869};

		sendGet("http://127.0.0.1:5000/rank_candidates", features, candidates);
	}

}
