package viiq.TestWikipedia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

public class ParseToyWiki 
{
	public static void main(String[] args)
	{
		ParseToyWiki ptw = new ParseToyWiki();
		/*String inputFilePath = "/home/nj/NJ/PROJECTS/DataSet/toyWiki.xml";
		System.out.println(inputFilePath);
		//String inputFilePath = "/home/nj/Desktop/internship/data/DBLP-Scholar/DBLP1.csv";
		String outputFilePath = "/home/nj/NJ/PROJECTS/DataSet/toyWikiCopy.xml";
		//ptw.parseWikiFile(inputFilePath, outputFilePath);
		ptw.testCollections();*/
		ptw.splitcheck();
	}
	
	private void splitcheck() {
		System.out.println("check check");
		String edges = "30000,0|27376,0|27449,0|28173,1";
		String[] eachedge = edges.split("\\|");
		for(String edge : eachedge) {
			System.out.println("--> " + edge);
		}
	}
	
	private void testCollections()
	{
		ArrayList<Integer> a = new ArrayList<Integer>();
		a.add(1);
		a.add(10);
		a.add(12);
		a.add(8);
		a.add(1);
		a.add(10);
		a.add(12);
		a.add(8);
		//Collections.sort(a, arg1);
		Collections.sort(a);
		System.out.println("done");
	}
	
	private void parseWikiFile(String inputFilePath, String outputFilePath)
	{
		try
		{
			int lineID = 1;
			FileWriter fw = new FileWriter(outputFilePath);
			BufferedWriter bw = new BufferedWriter(fw);
			
			FileReader filer = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(filer);
			String line = null;
			while((line = br.readLine()) != null)
			{
				System.out.println(line);
				String[] sents = line.split(".");
				for(int i=0; i<sents.length; i++)
				{
					String sent = sents[i];
					bw.write(sent);
				}
				bw.write(line);
				lineID++;
			}
			br.close();
			bw.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
