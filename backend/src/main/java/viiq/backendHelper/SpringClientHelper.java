/**
 * This class contains the GET and POST request implementations for cases which require reading
 * data from files and sending back the output.
 */
package viiq.backendHelper;


import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Date;

import com.sun.java.swing.plaf.windows.WindowsOptionPaneUI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import viiq.clientServer.client.EdgeExamplesResponseObject;
import viiq.clientServer.server.LoadData;
import viiq.commons.EdgeTypeInfo;
import viiq.commons.GuiEdgeInfo;
import viiq.commons.GuiEdgeStringInfo;
import viiq.commons.Pair;
import viiq.commons.QueryResult;
import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.BufferedRandomAccessFile;
import viiq.graphCompletionGuiMain.GenerateCandidatesNew;
import viiq.utils.PropertyKeys;


public class SpringClientHelper {

	Config conf = null;
	public SpringClientHelper(Config c) {
		conf = c;
	}

	private String getNodeLabel(HashMap<Integer, String> nodeLabel, int node) {
		String label = null;
		if(nodeLabel.containsKey(node))
			label = nodeLabel.get(node);
		return label;
	}

	public EdgeExamplesResponseObject getExamples(int source, int edge, int object) {
		// key = edge, value = (source vertex Type, dest vertex Type)
		HashMap<Integer, EdgeTypeInfo> edgeType = LoadData.getEdgeType();
		// key: node ID, value: node label
		HashMap<Integer, String> nodeLabel = LoadData.getNodeLabelIndex();
		EdgeExamplesResponseObject eer = new EdgeExamplesResponseObject();
		if(edgeType.containsKey(edge)) {
			int sourceType = edgeType.get(edge).source_type;
			int objectType = edgeType.get(edge).object_type;

			if(interchangeableTypes(sourceType, objectType))
				eer.isReversible = true;
			eer.sourceType = sourceType + "," + getNodeLabel(nodeLabel, sourceType);
			eer.objectType = objectType + "," + getNodeLabel(nodeLabel, objectType);

			/*
			// if both source and object are entities, no examples to send back.
			if(LoadData.isTypeNode(source) && LoadData.isTypeNode(object)) {
				if(interchangeableTypes(sourceType, objectType))
					eer.isReversible = true;
				eer.sourceType = sourceType + "," + getNodeLabel(nodeLabel, sourceType);
				eer.objectType = objectType + "," + getNodeLabel(nodeLabel, objectType);
			} else if(LoadData.isTypeNode(source)) {
				eer.sourceType = sourceType + "," + getNodeLabel(nodeLabel, sourceType);
			} else if(LoadData.isTypeNode(object)) {
				eer.objectType = objectType + "," + getNodeLabel(nodeLabel, objectType);
			}
			*/

		}
		return eer;
	}

	private boolean interchangeableTypes(int srctype, int objtype) {
		// keep it simple for now, can use ontology or class hierarchy to make it better.
		if(srctype == objtype)
			return true;
		return false;
	}


	public long getEntityPosition(String entityName, int entityId){

			String filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFile);
			int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLength)) + 1;
			int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLines));
			try{
				BufferedRandomAccessFile braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				return braf.findEntity(entityName, entityId);
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}


		 return 0;
	}

	public long getEntityPositionWithType(int type, String entityName, int entityId){

		String filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
		int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLength)) + 1;
		int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines));
		try{
				BufferedRandomAccessFile braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				return braf.findEntityWithType(type, entityId, entityName);
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}


		 return 0;
	}


	public long getTypePosition(String typeName, int typeId){
		String filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFile);
		int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLength)) + 1;
		int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLines));
		try{
				BufferedRandomAccessFile braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				return braf.findEntity(typeName, typeId);
		}
				catch(IOException ioe) {
			 	ioe.printStackTrace();
		 }
		 return 0;
	}

	/**
	 * Get entities (based on windowNumber and windowSize) and their string values, subject to various constraints!
	 * @param domain
	 * @param type
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @param entities
	 */
	public void getEntities(int domain, int type, String keyword, int windowNumber,
			int windowSize, ArrayList<String> entities) {
				System.out.println("Keyword entity:"+keyword);
		try {
			BufferedRandomAccessFile braf = null;
			entities.add("0,Select Entity...");
			if(domain == -1 && type == -1) {
				/**
				 * There is no filter on domain or type, get entities
				 */
				String filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFile);
				int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLength)) + 1;
				int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLines));
				braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				if(keyword.isEmpty()) {
					// Get "windowSize" number of entities, starting from windownNumber*windowSize
					braf.getAllNodesValue(entities, windowNumber, windowSize);
				}
				else {
					// filter based on keyword.
					// first 0 specifies we need to look at label sorted files. second 0 specifies this is for entities
					completeKeyword(keyword, entities, windowNumber, windowSize, braf, 0, 0);
				}
			}
			else if(type > 0) {
				/**
				 * A type value has been set, so filter based on the type value.
				 */
				String filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
				int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLength)) + 1;
				int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines));
				braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				if(keyword.isEmpty()) {
					// get all entities of a particular type
					braf.getNodeIndexValues(type, entities, windowNumber, windowSize);
				}
				else {
					// 1 specifies we need to look at id sorted, followed by label sorted files. 0 specifies this is to search for entities
					completeKeyword(keyword, type, entities, windowNumber, windowSize, braf, 1, 0);
				}
			}
			else if(domain > 0) {
				/**
				 * This condition is for only when domain is set and NOT type. filter the entities to return
				 * based on the domain ID used.
				 */
				String filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPadded);
				int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLength)) + 1;
				int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLines));
				braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				if(keyword.isEmpty()) {
					// No keyword filter
					braf.getNodeIndexValues(domain, entities, windowNumber, windowSize);
				}
				else {
					// 1 specifies we need to look at id sorted, followed by label sorted files. 2 specifies this is to search for ENTITIES (in domain id first sorted file)
					completeKeyword(keyword, domain, entities, windowNumber, windowSize, braf, 1, 2);
				}
			}
			if(braf != null)
				braf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public ArrayList<String>reOrderIdenticalEntities(ArrayList<String> entities) {
		ArrayList<GuiEdgeStringInfo> temp = new ArrayList<GuiEdgeStringInfo>();
		ArrayList<String> result = new ArrayList<String>();
		String prev_label = null;
		for(String entity : entities) {
			String[] tokens = entity.split(",", 2);
			if(prev_label != null && !tokens[1].toLowerCase().equals(prev_label)) {
				//System.out.println("size of temp list "+temp.size());
				//sorting the array here based on the count here
				Collections.sort(temp, new Comparator<GuiEdgeStringInfo>(){
					  public int compare(GuiEdgeStringInfo a, GuiEdgeStringInfo b) {
						  return Double.compare(b.score, a.score);
					  }
				});
				for(GuiEdgeStringInfo t : temp) {
					result.add(t.source);
				}
				temp.clear();
			}
			GuiEdgeStringInfo count = new GuiEdgeStringInfo();
			count.source = entity;
			count.score = getNumberOfEdgesForEntity(Integer.parseInt(tokens[0]));
			//System.out.println(count.source+ " "+count.score);
			//count.score = 1.0;
			temp.add(count);
			prev_label = tokens[1].toLowerCase();
		}

		Collections.sort(temp, new Comparator<GuiEdgeStringInfo>(){
				public int compare(GuiEdgeStringInfo a, GuiEdgeStringInfo b) {
					return Double.compare(b.score, a.score);
				}
		});
		for(GuiEdgeStringInfo t : temp) {
			result.add(t.source);
		}
		return result;
	}

	public double getNumberOfEdgesForEntity(int entity) {
		String filename = conf.getInputFilePath(PropertyKeys.entityEdgeCount);
		int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.entityEdgeCountAlignmentLength)) + 1;
		int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.entityEdgeCountNumberOfLines));

		try {
			BufferedRandomAccessFile braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
			double returnValue = (double)(braf.getEntityEdgeCount(entity));
			braf.close();
			return returnValue;
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return 0;
	}


	public String getWikiSummary(int nodeid){

		//add (domain:type) at the beginning of the preview
		// boolean isType = LoadData.isTypeNode(nodeid);
		// if(isType){
		// 	HashMap<Integer, Integer> typeDomainMap = LoadData.getTypeDomainMap();
		// 	HashMap<Integer, String> domainLabel = LoadData.getDomainLabelMap();
		// 	if(typeDomainMap.containsKey(nodeid)) {
		// 		return "Domain:"+domainLabel.get(typeDomainMap.get(nodeid));
		// 	}
		// }
		//else{

			HashMap<Integer, String> entityPreviewMap = LoadData.getEntityPreviewMap();
			String wikiSummary = "";

			// ArrayList<String> typeList = new ArrayList<String>();
			// getTypesForEntity(nodeid, typeList);
			// HashMap<Integer, Integer> typeDomainMap = LoadData.getTypeDomainMap();
			// HashMap<Integer, String> domainLabel = LoadData.getDomainLabelMap();
			// wikiSummary += "<b>(Domain:Type):</b>" ;
			// for(int i=1;i<typeList.size();i++){
			// 	String type = typeList.get(i);
			// 	String[] typeData = type.split(",");
			// 	int typeId = Integer.parseInt(typeData[0]);
			//
			// 	wikiSummary += "(" +domainLabel.get(typeDomainMap.get(typeId))+":"+ typeData[1] +"),";
			//
			// }
			// wikiSummary += "<br/><br/>";

			if(entityPreviewMap.containsKey(nodeid)) {
				wikiSummary += entityPreviewMap.get(nodeid);
			}

			return wikiSummary;

		// }
		// return "";
	}

	public ArrayList<String> getEdgePreview(int edgeid){
		HashMap<Integer, ArrayList<String>> edgePreviewMap = LoadData.getEdgePreviewMap();
		ArrayList<String> edgeInfo = new ArrayList<String>();
    if(edgePreviewMap.containsKey(edgeid)) {
			edgeInfo = edgePreviewMap.get(edgeid);
    }
		return edgeInfo;
	}
	/**
	 * Get types (based on windowNumber and windowSize) and their string values, subject to various constraints!
	 * @param domain
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @param entities
	 */
	public void getTypes(int domain, String keyword, int windowNumber, int windowSize, ArrayList<String> types) {
		types.add("0,Select Type...");
		try {
			BufferedRandomAccessFile braf = null;
			if(domain == -1) {
				/**
				 * Domain filter is not set.
				 */
				String filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFile);
				int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLength)) + 1;
				int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLines));
				braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				if(keyword.isEmpty()) {
					// no keyword filter. get all types
					braf.getAllNodesValue(types, windowNumber, windowSize);
				}
				else {
					// first 0 specifies we need to look at label sorted files. 1 specifies this is for types
					completeKeyword(keyword, types, windowNumber, windowSize, braf, 0, 1);
				}
			}
			else {
				/**
				 * Domain filter is set, get types of a particular domain.
				 */
				String filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPadded);
				int alingmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLength)) + 1;
				int numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLines));
				braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alingmentLen);
				if(keyword.isEmpty()) {
					// no keyword filter, get all types of a particular domain
					braf.getNodeIndexValues(domain, types, windowNumber, windowSize);
				}
				else {
					// 1 specifies we need to look at id sorted, followed by label sorted files. second 1 specifies this is to search for TYPES
					completeKeyword(keyword, domain, types, windowNumber, windowSize, braf, 1, 1);
				}
			}
			if(braf != null)
				braf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * This method is to search for keywords where files are sorted by the node label.
	 * @param keyword
	 * @param retValue
	 * @param windowNum
	 * @param windowSize
	 * @param braf
	 * @param fileFlag
	 * @param entityTypeFlag
	 */
	private void completeKeyword(String keyword, ArrayList<String> retValue, int windowNum, int windowSize, BufferedRandomAccessFile braf,
			int fileFlag, int entityTypeFlag) {
		//System.out.println("File flag = " + fileFlag + " entity type flag = " + entityTypeFlag);
		int numOfEntriesToIgnore = windowNum*windowSize;
		int newWindowSize = windowSize;
		int numberOfKeywordFiles = 6;
		int fileNumber = 1;
		int sizeInThisFile = 0;
		try {
			do{
				sizeInThisFile = braf.completeKeyword(keyword, retValue, numOfEntriesToIgnore, newWindowSize, windowSize, fileNumber-1);
				if(retValue.size() == windowSize+1) {
					// we got what we need, don't do anything here. just return back!
				} else {
					fileNumber++;
				//	System.out.println("In NEXT FILE : " + fileNumber);
					braf.close();
					if(fileNumber <= numberOfKeywordFiles) {
						braf = getFileHandlerLangEnSorted(fileNumber, fileFlag, entityTypeFlag);
						if(retValue.size() == 1) {
							numOfEntriesToIgnore -= sizeInThisFile;
						} else if(retValue.size() > 1) {
							numOfEntriesToIgnore = 0;
							newWindowSize = windowSize - retValue.size() + 1;
						}
					}
				}
			} while(fileNumber <= numberOfKeywordFiles && retValue.size() != windowSize+1);
			if(braf != null)
				braf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	/**
	 * This method is to search for keywords where files are sorted by an ID, and then by a label.
	 * @param keyword
	 * @param nodeToSearchFirst
	 * @param retValue
	 * @param windowNum
	 * @param windowSize
	 * @param braf
	 * @param fileFlag
	 * @param entityTypeFlag
	 */
	private void completeKeyword(String keyword, int nodeToSearchFirst, ArrayList<String> retValue, int windowNum, int windowSize,
			BufferedRandomAccessFile braf, int fileFlag, int entityTypeFlag) {
		int numOfEntriesToIgnore = windowNum*windowSize;
		int newWindowSize = windowSize;
		int numberOfKeywordFiles = 6;
		int fileNumber = 1;
		int sizeInThisFile = 0;
		try {
			do{
				sizeInThisFile = braf.getNodeIndexValuesFilteredKeyword(nodeToSearchFirst, keyword, retValue,
						numOfEntriesToIgnore, newWindowSize, windowSize, fileNumber-1);
				if(retValue.size() == windowSize+1) {
					// we got what we need, don't do anything here. just return back!
				} else {
					fileNumber++;
					braf.close();
					if(fileNumber <= numberOfKeywordFiles) {
						braf = getFileHandlerLangEnSorted(fileNumber, fileFlag, entityTypeFlag);
						if(retValue.size() == 1) {
							numOfEntriesToIgnore -= sizeInThisFile;
						} else if(retValue.size() > 1) {
							numOfEntriesToIgnore = 0;
							newWindowSize = windowSize - retValue.size() + 1;
						}
					}
				}
			} while(fileNumber <= numberOfKeywordFiles && retValue.size() != windowSize+1);
			if(braf != null)
				braf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * fileFlag: 0=files corresponding to label sorted (eg. instancesLangEnSortedPaddedFile, typesLangEnSortedPaddedFile)
	 * fileFlag: 1=files corresponding to id sorted, then label sorted
	 * 				(eg. typesSortedToInstancesIndexPaddedFile, domainsIdSortedInstancesLangEnPadded, domainsIdSortedTypesLangEnPadded)
	 * This will be 0, when called from completeKeyword, and 1 when called from getNodeIndexValuesFilteredKeyword
	 *
	 * entityTypeFlag: 0=corresponds to entities
	 * 				   1=corresponds to types
	 * 				   2=corresponds to domains
	 *
	 * (fileFlag, entityTypeFlag) : Meaning
	 * (0, 0): Search for keyword in label sorted file, for ENTITIES
	 * (0, 1): Search for keyword in label sorted file, for TYPES
	 *
	 * (1, 0): Search for keyword in typeid-instancelabel sorted file, for ENTITIES, in type-entity file
	 * (1, 1): Search for keyword in domainid-typelabel sorted file, for TYPES, in domain-type file
	 * (1, 2): Search for keyword in domainid-instancelabel sorted file, for ENTITIES, in domain-entity file
	 * @param fnum
	 * @param fileFlag
	 * @param entityTypeFlag
	 * @return
	 */
	private BufferedRandomAccessFile getFileHandlerLangEnSorted(int fnum, int fileFlag, int entityTypeFlag) {
		BufferedRandomAccessFile braf = null;
		try {
			String filename = "";
			int alignmentLen = 0;
			int numberOfLines = 0;
			if(fileFlag == 0) {
				// dealing with files where the rows are first sorted by the label.
				if(entityTypeFlag == 0) {
					// dealing with instancesLangEnSortedPaddedFile, now based on fnum.
					if(fnum == 2) {
						filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol2);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLengthCol2)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLinesCol2));
					} else if(fnum == 3) {
						filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol3);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLengthCol3)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLinesCol3));
					} else if(fnum == 4) {
						filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol4);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLengthCol4)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLinesCol4));
					} else if(fnum == 5) {
						filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol5);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLengthCol5)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLinesCol5));
					} else if(fnum == 6) {
						filename = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFileCol6);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLengthCol6)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLinesCol6));
					}
				} else if(entityTypeFlag == 1) {
					// dealing with typesLangEnSortedPaddedFile, now based on fnum
					if(fnum == 2) {
						filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol2);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLengthCol2)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLinesCol2));
					} else if(fnum == 3) {
						filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol3);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLengthCol3)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLinesCol3));
					} else if(fnum == 4) {
						filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol4);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLengthCol4)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLinesCol4));
					} else if(fnum == 5) {
						filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol5);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLengthCol5)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLinesCol5));
					} else if(fnum == 6) {
						filename = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFileCol6);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLengthCol6)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLinesCol6));
					}
				}
			} else if(fileFlag == 1) {
				// dealing with files where the rows are first sorted by id, and then by the label.
				if(entityTypeFlag == 0) {
					// dealing with files where it is first sorted by type id, and then sorted by entity label
					if(fnum == 2) {
						filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol2);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLengthCol2)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLinesCol2));
					} else if(fnum ==3) {
						filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol3);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLengthCol3)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLinesCol3));
					} else if(fnum ==4) {
						filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol4);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLengthCol4)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLinesCol4));
					} else if(fnum ==5) {
						filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol5);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLengthCol5)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLinesCol5));
					} else if(fnum ==6) {
						filename = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFileCol6);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLengthCol6)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLinesCol6));
					}
				} else if(entityTypeFlag == 1) {
					// dealing with files where it is first sorted by domain, and then sorted by type label
					if(fnum == 2) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol2);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLengthCol2)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLinesCol2));
					} else if(fnum ==3) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol3);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLengthCol3)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLinesCol3));
					} else if(fnum ==4) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol4);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLengthCol4)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLinesCol4));
					} else if(fnum ==5) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol5);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLengthCol5)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLinesCol5));
					} else if(fnum ==6) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPaddedCol6);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnAlignmentLengthCol6)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedTypesLangEnNumberOfLinesCol6));
					}
				} else if(entityTypeFlag == 2) {
					// dealing with files where it is first sorted by domain, and then sorted by entity label
					if(fnum == 2) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol2);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLengthCol2)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLinesCol2));
					} else if(fnum ==3) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol3);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLengthCol3)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLinesCol3));
					} else if(fnum ==4) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol4);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLengthCol4)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLinesCol4));
					} else if(fnum ==5) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol5);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLengthCol5)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLinesCol5));
					} else if(fnum ==6) {
						filename = conf.getInputFilePath(PropertyKeys.domainsIdSortedInstancesLangEnPaddedCol6);
						alignmentLen = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnAlignmentLengthCol6)) + 1;
						numberOfLines = Integer.parseInt(conf.getProp(PropertyKeys.domainsIdSortedInstancesLangEnNumberOfLinesCol6));
					}
				}
			}
		//	System.out.println("Filename = " + filename);
			braf = new BufferedRandomAccessFile(filename, "r", numberOfLines, alignmentLen);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return braf;
	}

	public void getEntitiesForType(int type, ArrayList<String> entities, int windowNumber, int windowSize) {
		try {
			entities.add("0,Select Entity...");
			String filePath = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);
			baf.getNodeIndexValues(type, entities, windowNumber, windowSize);
			baf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void getTypesForEntity(int entity, ArrayList<String> types) {
		try {
			types.add("0,Select Type...");
			String filePath = conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndexPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);
			baf.getNodeIndexValues(entity, types);
			baf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/*public ArrayList<String> getEntityKeywordCompletion(String keyword) {
		ArrayList<String> completedStrings = new ArrayList<String>();
		try {
			String filePath = conf.getInputFilePath(PropertyKeys.instancesLangEnSortedPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesLangEnSortedNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);
			baf.completeKeyword(keyword, completedStrings);
			baf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return completedStrings;
	}

	public ArrayList<String> getTypeKeywordCompletion(String keyword) {
		ArrayList<String> completedStrings = new ArrayList<String>();
		try {
			String filePath = conf.getInputFilePath(PropertyKeys.typesLangEnSortedPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesLangEnSortedNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);
			baf.completeKeyword(keyword, completedStrings);
			baf.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return completedStrings;
	}*/

	/**
	 * This returns back the type values for a ? marked node in the GUI. The GUI ignores if there is only one row in the returned arraylist.
	 * @param nodeID
	 * @param edges
	 * @return
	 */
	public ArrayList<String> nodeTypeValues(int nodeID, String edges) {
		ArrayList<String> returnValues = new ArrayList<String>();
		//boolean isType = LoadData.isTypeNode(nodeID);
		//if(!isType) {
		//	returnValues.add("0,Select Type...");
		//} else {
		HashMap<Integer, String> typeLabel = LoadData.getNodeLabelIndex();
		HashMap<Integer, EdgeTypeInfo> edgeTypes = LoadData.getEdgeType();
		HashSet<Integer> typesOfThisNode = new HashSet<Integer>();
		String[] eachEdge = edges.split("\\|");
		for(String edge : eachEdge) {
			String[] idflag = edge.split(",");
			int e = Integer.parseInt(idflag[0].trim());
			if(edgeTypes.containsKey(e)) {
				// the property is sent in the following format:
				// 28113,0|28114,1 etc.
				// The first id is the edge ID, 0 or 1 indicates if this node is the source of object of this edge respectively.
				int type = idflag[1].trim().equals("0") ? edgeTypes.get(e).source_type : edgeTypes.get(e).object_type;
				if(!typesOfThisNode.contains(type) && typeLabel.containsKey(type)) {
					typesOfThisNode.add(type);
					returnValues.add(type+","+typeLabel.get(type));
				}
			}
		}
		//}
		//if(returnValues.isEmpty())
		//	returnValues.add("0,Select Type...");
		return returnValues;
	}


	/**
	 * This returns back the type values for a ? marked node in the GUI. The GUI ignores if there is only one row in the returned arraylist.
	 * @param types
	 * @return
	 */
	public ArrayList<String> domainForTypes( String types) {
		ArrayList<String> returnValues = new ArrayList<String>();
		HashMap<Integer, Integer> typeDomainMap = LoadData.getTypeDomainMap();
		HashMap<Integer, String> domainLabel = LoadData.getDomainLabelMap();

		String[] typesList = types.split(",");

		HashSet<Integer> domainList = new HashSet<Integer>();
		for(String type: typesList){
			if(typeDomainMap.containsKey(Integer.parseInt(type))) {
				domainList.add(typeDomainMap.get(Integer.parseInt(type)));
			}
			else{
				System.out.println("Type id not found");
			}
		}

		ArrayList<NodeLabelID> labels = new ArrayList<NodeLabelID>();
		for(Integer i: domainList){
			NodeLabelID nl = new NodeLabelID();
			nl.id = i;
			nl.label = domainLabel.get(i);

			labels.add(nl);


		}

		Collections.sort(labels, new Comparator<NodeLabelID>() {
			public int compare(NodeLabelID nl1, NodeLabelID nl2) {
				return nl1.label.compareToIgnoreCase(nl2.label);
			}
		});

		returnValues.add("0,SELECT DOMAIN...");

		for(int i=0; i<labels.size() ; i++) {
			NodeLabelID nli = labels.get(i);
			returnValues.add(nli.id + "," + nli.label);
		}

		return returnValues;
	}


	/**
	 * This returns back the type values for a ? marked node in the GUI. The GUI ignores if there is only one row in the returned arraylist.
	 * @param edges
	 * @return
	 */
	public ArrayList<String> nodeTypeCandidate( String edges, int domain, String keyword) {
		ArrayList<String> returnValues = new ArrayList<String>();
		HashMap<Integer, Integer> typeDomainMap = LoadData.getTypeDomainMap();
		//boolean isType = LoadData.isTypeNode(nodeID);
		//if(!isType) {
		//	returnValues.add("0,Select Type...");
		//} else {
		HashMap<Integer, String> typeLabel = LoadData.getNodeLabelIndex();
		HashMap<Integer, HashSet<Integer>> sourceCandidate = LoadData.getCandidateSubject();
		HashMap<Integer, HashSet<Integer>> objectCandidate = LoadData.getCandidateObject();
		System.out.println("Size of sourceCandidate:"+sourceCandidate.size());
		System.out.println("Size of objectCandidate:"+objectCandidate.size());
		HashSet<Integer> allCandidate = new HashSet<Integer>();
		String[] eachEdge = edges.split("\\|");
		for(String edge : eachEdge) {
			String[] idflag = edge.split(",");
			int e = Integer.parseInt(idflag[0].trim());
			int t = Integer.parseInt(idflag[1].trim());
			System.out.println("edge id:"+e+" type:"+t);
			if(t==0){
				if(sourceCandidate.containsKey(e)){
					allCandidate.addAll(sourceCandidate.get(e));
				}


			}
			else if(t==1){
				if(objectCandidate.containsKey(e)){
					allCandidate.addAll(objectCandidate.get(e));
				}
			}

		}
		returnValues.add("0,SELECT TYPE...");
		ArrayList<NodeLabelID> labels = new ArrayList<NodeLabelID>();
		for(Integer i: allCandidate){
			if(typeLabel.containsKey(i)){
				NodeLabelID nl = new NodeLabelID();
				nl.id = i;
				nl.label = typeLabel.get(i);
				if(domain!=-1){
					if(typeDomainMap.containsKey(i) && typeDomainMap.get(i)==domain){
						labels.add(nl);
					}
				}
				else{
					labels.add(nl);
				}

			}
		}

		Collections.sort(labels, new Comparator<NodeLabelID>() {
			public int compare(NodeLabelID nl1, NodeLabelID nl2) {
				return nl1.label.compareToIgnoreCase(nl2.label);
			}
		});

		for(int i=0; i<labels.size() ; i++) {
			NodeLabelID nli = labels.get(i);
			if(keyword.length()>2){
				if(nli.label.toLowerCase().contains(keyword.toLowerCase())){
					returnValues.add(nli.id + "," + nli.label);

				}
			}
			else{
				returnValues.add(nli.id + "," + nli.label);
			}



		}

		return returnValues;
	}

	//subject to DTL, this method returns DL,TL,EL
	public ArrayList<ArrayList<NodeLabelID>> nodeEditValues(String typeValues, int entityId, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, int nodeToBeEdited) {

		long start = System.currentTimeMillis();
		ArrayList<ArrayList<NodeLabelID>> returnValues = new ArrayList<ArrayList<NodeLabelID>>();
		ArrayList<NodeLabelID> entities = new ArrayList<NodeLabelID>();
		ArrayList<NodeLabelID> types = new ArrayList<NodeLabelID>();
		ArrayList<NodeLabelID> domains = new ArrayList<NodeLabelID>();

		HashSet<Integer> typesOfThisNode = getListFromString(typeValues);
		HashSet<Integer> nodeValues = null;

		if(nodeToBeEdited == -1 || entityId != -1) { //either it is new node that hasn't been created yet, or an entity node
 			nodeValues = getEntitiesForMultipleTypesIntersection(typesOfThisNode);
		} else {
			nodeValues = evaluatedValues.get(nodeToBeEdited);
		}

		//System.out.println("entityId = "+entityId+", nodeValuesSize = "+nodeValues.size());


		//int[] entityWindowNumberForEntityNode = new int[]{-1}; //created length1 array instead of using integer because array can be passed by reference
		getInstancesIntersection(typesOfThisNode, entities, types, domains, nodeValues);
		// if(entityWindowNumberForEntityNode[0] != -1) {
		// 	entityWindowNumber += entityWindowNumberForEntityNode[0];
		// }

		System.out.println("entity size = "+entities.size());
		//sorting entities
		Collections.sort(entities, new Comparator<NodeLabelID>() {
			public int compare(NodeLabelID nl1, NodeLabelID nl2) {
				return nl1.label.compareToIgnoreCase(nl2.label);
			}
		});


		//sorting types
		Collections.sort(types, new Comparator<NodeLabelID>() {
			public int compare(NodeLabelID nl1, NodeLabelID nl2) {
				return nl1.label.compareToIgnoreCase(nl2.label);
			}
		});

		//sorting domains
		Collections.sort(domains, new Comparator<NodeLabelID>() {
			public int compare(NodeLabelID nl1, NodeLabelID nl2) {
				return nl1.label.compareToIgnoreCase(nl2.label);
			}
		});


		System.out.println("Size of entities = " + entities.size());

		returnValues.add(domains);
		returnValues.add(types);
		returnValues.add(entities);

		System.out.println("TOTAL time to calculate EL,TL,DL = "+(System.currentTimeMillis()-start)/1000.0);
		return returnValues;
	}

	//filter the full list based on various constrains
	public ArrayList<ArrayList<String>> filterValues(ArrayList<ArrayList<NodeLabelID>>allValues, int domain, int type, String typeKeyword, String entityKeyword, int typeWindowNumber, int entityWindowNumber, int entityId, String nodeTypeValues, int windowSize) {

		ArrayList<ArrayList<String>> returnValues = new ArrayList<ArrayList<String>>();

		//original DL,TL,EL
		ArrayList<NodeLabelID> domains = allValues.get(0);
		ArrayList<NodeLabelID> types = allValues.get(1);
		ArrayList<NodeLabelID> entities = allValues.get(2);

	  System.out.println("Filtering DL,TL,EL of size "+domains.size()+","+types.size()+","+entities.size());

		//initializting final return list
		ArrayList<String> domainReturnValues = new ArrayList<String>();
		ArrayList<String> typeReturnValues = new ArrayList<String>();
		ArrayList<String> entityReturnValues = new ArrayList<String>();
		domainReturnValues.add("0,Select Domain...");
		typeReturnValues.add("0,Select Type...");
		entityReturnValues.add("0,Select Entity...,nopreview");

		if(!entities.isEmpty() && entities.size() > (entityWindowNumber*windowSize)) {
			System.out.println("Found entities and populating return values now!");

			//type filters based on highlighted type/domain
			HashSet<Integer> typeConstrains = new HashSet<Integer>();
			List<String>nodeTypeValuesId = Arrays.asList(nodeTypeValues.split(","));
			if(type != -1) {
				if(!nodeTypeValuesId.contains(Integer.toString(type))) {
					typeConstrains.add(type);
				}
			} else if(domain != -1) {
				LoadData ldlm = new LoadData();
				for(NodeLabelID nli : types) {
					if(ldlm.getTypeDomainMap().containsKey(nli.id) && domain == ldlm.getTypeDomainMap().get(nli.id) && !nodeTypeValuesId.contains(nli.id)) {
						typeConstrains.add(nli.id);
					}
				}
			}

			LinkedHashSet<Integer> entityConstrains = new LinkedHashSet<Integer>();
			if(!typeConstrains.isEmpty()) {
				entityConstrains = getEntitiesForMultipleTypesIntersection(typeConstrains);
			}

			//storing final domain values
			for(NodeLabelID nli : domains) {
				domainReturnValues.add(nli.id + "," + nli.label.replace("_", " "));
			}

			//storing final type values
			int cnt = 0;
			for (NodeLabelID nli : types) {
				if(domain == -1 || typeConstrains.contains(nli.id)) {
					if(typeKeyword.equals("") || nli.label.toLowerCase().contains(typeKeyword.toLowerCase())) {
						if(cnt == (typeWindowNumber+1)*windowSize) {
							break;
						}
						if(cnt >= typeWindowNumber*windowSize) {
							typeReturnValues.add(nli.id + "," + nli.label);
						}
						cnt++;
					}
				}
			}

			//finding initial entity windownumber if node was an entity, we want to keep that entity highlighted
			
			int entityWindowForSelectedEntity = 0;
			// cnt = 0;
			// if(entityId != -1 && entityKeyword.equals("")) {
			// 	for (NodeLabelID nli : entities) {
			// 		if(entityConstrains.isEmpty() || entityConstrains.contains(nli.id)) {
			// 			if(nli.id == entityId) {
			// 				entityWindowForSelectedEntity = cnt/windowSize;
			// 				break;
			// 			}
			// 			cnt++;
			// 		}
			// 	}
			// }

			//storing final entity values
			cnt = 0;
			entityWindowNumber +=  entityWindowForSelectedEntity;
			for (NodeLabelID nli : entities) {
				if(entityConstrains.isEmpty() || entityConstrains.contains(nli.id)) {
					if(entityKeyword.equals("") || nli.label.toLowerCase().contains(entityKeyword.toLowerCase())) {
						if(cnt == (entityWindowNumber+1)*windowSize) {
							break;
						}
						if(cnt >= entityWindowNumber*windowSize) {
							if(getWikiSummary(nli.id)=="") {
								entityReturnValues.add(nli.id + "," + nli.label + ",nopreview");
							} else {
								entityReturnValues.add(nli.id + "," + nli.label + ",preview");
							}
						}
						cnt++;
					}
				}
			}
		}

		returnValues.add(domainReturnValues);
		returnValues.add(typeReturnValues);
		returnValues.add(entityReturnValues);

		return returnValues;
	}


	public String getEndTypesForEdge(int edge) {
		HashMap<Integer, EdgeTypeInfo> edgeTypeId = LoadData.getEdgeType();
		HashMap<Integer, String> typeIdToLabel = LoadData.getNodeLabelIndex();

		int sourceId = edgeTypeId.get(edge).source_type;
		int objectId = edgeTypeId.get(edge).object_type;

		System.out.println(edge+","+sourceId+","+objectId);

		return sourceId+","+typeIdToLabel.get(sourceId)+"|"+objectId+","+typeIdToLabel.get(objectId);
	}


	public ArrayList<String> entityEditValues(int typeId, int entityId, String entityName, String edges, String keyword, int windowNumber, int windowSize, int domain) {
		ArrayList<String> returnValues = new ArrayList<String>();

		HashMap<Integer, EdgeTypeInfo> edgeTypes = LoadData.getEdgeType();
		HashSet<Integer> typesOfThisNode = new HashSet<Integer>();
		HashMap<Integer, Integer> typeDomainMap = LoadData.getTypeDomainMap();
		//System.out.println("FUll edge = " + edges);

		String[] eachEdge = edges.split("\\|");
		for(String edge : eachEdge) {
			System.out.println("EDGE = " + edge);
			String[] idflag = edge.split(",");
			int e = Integer.parseInt(idflag[0].trim());
			if(edgeTypes.containsKey(e)) {
				// the property is sent in the following format:
				// 28113,0|28114,1 etc.
				// The first id is the edge ID, 0 or 1 indicates if this node is the source of object of this edge respectively.
				int type = idflag[1].trim().equals("0") ? edgeTypes.get(e).source_type : edgeTypes.get(e).object_type;
				typesOfThisNode.add(type);
			}
			else{
				System.out.println("Could not find edge:"+e);
			}
		}

		ArrayList<NodeLabelID> labels = new ArrayList<NodeLabelID>();
		System.out.println("Types size:"+typesOfThisNode.size());
			// assuming that this is a type node, get instances associated with the intersection of a bunch of types!
			if(typesOfThisNode.size() == 1) {
				// if there is only one type, then we can use existing method.
				int tid = typesOfThisNode.iterator().next();
				System.out.println("keyword:"+keyword+" size:"+keyword.length());
				if(windowNumber==-1){
					if(keyword.length()==0){

						long pos = getEntityPositionWithType(tid, entityName, entityId);
						System.out.println(pos);
						windowNumber = (int)(pos / windowSize);
					}
					else{
						windowNumber = 0;
					}
				}
				getEntities(-1, tid, keyword, windowNumber, windowSize, returnValues);
				returnValues.add("windowNum,"+windowNumber);
				System.out.println("Return 1");
				return returnValues;
			}
			Date d1 = new Date();
			HashSet<Integer> typeOfEntity = new HashSet<Integer>();
			ArrayList<String> typeList = new ArrayList<String>();
			getTypesForEntity(entityId, typeList);
			for(int i=1;i<typeList.size();i++){
				String tids = typeList.get(i).split(",")[0];
				try{

					typeOfEntity.add(Integer.parseInt(tids));
				}
				catch(Exception e){
					System.out.println("Excpetion in parsing:"+tids);
					e.printStackTrace();
				}
			}

			System.out.println("Size of types of entity:"+typeOfEntity.size());

			typesOfThisNode.retainAll(typeOfEntity);
			System.out.println("Size of types afte intersection:"+typesOfThisNode.size());
			if(typesOfThisNode.size()>0){
				HashMap<Integer, Integer> typesInstancesCount = LoadData.getTypeInstancesCount();
				int highestType = 0;
				int highestCnt = Integer.MIN_VALUE;
				Iterator<Integer> it = typesOfThisNode.iterator();
				while(it.hasNext()) {
					int type = it.next();
					if(typesInstancesCount.containsKey(type) && typesInstancesCount.get(type) > highestCnt) {
						highestCnt = typesInstancesCount.get(type);
						highestType = type;
					}
				}
				if(windowNumber ==-1){
					if(keyword.length()==0){

						long pos = getEntityPositionWithType(highestType, entityName, entityId);
						System.out.println(pos);
						windowNumber = (int)(pos / windowSize);
					}
					else{
						windowNumber = 0;
					}
				}
				getEntities(-1, highestType,keyword, windowNumber, windowSize, returnValues );
				return returnValues;

			}else{
				System.out.println("Zero types");
			}

			Date d2 = new Date();
			System.out.println("TIme to get entities:"+(d2.getTime()-d1.getTime()));
		System.out.println("now sort and return " + keyword);
		System.out.println("Size of type list = " + typesOfThisNode.size());
		// if(!labels.isEmpty() && labels.size() > (windowNumber*windowSize)) {
		// 	System.out.println("Found labels and populating return values now!");
		// 	//		System.out.println("keyword is empty!!!!");
		// 	// no keyword, so return the corresponding window.
		// 	Collections.sort(labels, new Comparator<NodeLabelID>() {
		// 		public int compare(NodeLabelID nl1, NodeLabelID nl2) {
		// 			return nl1.label.compareToIgnoreCase(nl2.label);
		// 		}
		// 	});
		// 	Date d3 = new Date();
		// 	System.out.println("TIme to sort entities:"+(d3.getTime()-d2.getTime()));
		// 	if(windowNumber == -1){
		//
		// 		int low = 0;
		// 		int high = labels.size()-1;
		// 		int mid = high/2;
		// 		System.out.println("Size of entity list:"+labels.size());
		// 		while(low <= high){
		// 			mid = (low+high) >>> 1;
		// 			if(labels.get(mid).label.equalsIgnoreCase(entityName)){
		// 				if(labels.get(mid).id == entityId){
		// 					windowNumber = (int)(mid/windowSize);
		// 					break;
		// 				}
		// 				else if(labels.get(mid).id > entityId){
		// 					high = mid-1;
		// 				}
		// 				else{
		// 					low = mid+1;
		// 				}
		// 			}
		// 			else if(labels.get(mid).label.compareToIgnoreCase(entityName) < 0){
		// 				low = mid+1;
		// 			}
		// 			else{
		// 				high = mid-1;
		// 			}
		// 		}
		// 	}
		//
		// 	// for (NodeLabelID node: labels){
		// 	// 	if (node.id == entityId){
		// 	// 		break;
		// 	// 	}
		// 	// 	low++;
		// 	// }
		// 	// windowNumber = (int)(low/windowSize);
		// 	System.out.println("After finding page number");
		// 	if(windowNumber <0){
		// 		windowNumber = 0;
		// 	}
		// 	for(int i=(windowNumber*windowSize); i<labels.size() && windowSize-- > 0; i++) {
		// 		NodeLabelID nli = labels.get(i);
		// 		returnValues.add(nli.id + "," + nli.label);
		// 	}
		// 	returnValues.add("windowNum,"+windowNumber);
		// }
		return returnValues;
	}

	/*private void populateBasedOnKeyword(String keyword, ArrayList<NodeLabelID> labels, int windowNumber,
			int windowSize, ArrayList<String> returnValues) {
		int start = leftBinarySearch(labels, keyword);
		//System.out.println("\nleft binary search = " + start + "\n\n");
		if(start != -1) {
			int end = rightBinarySearch(labels, keyword);
			for(int i=start+(windowNumber*windowSize); i<labels.size() && windowSize-- > 0 && i<=end; i++) {
				NodeLabelID nli = labels.get(i);
				returnValues.add(nli.id + "," + nli.label);
			}
		}
	}

	private int leftBinarySearch(ArrayList<NodeLabelID> labels, String key) {
		int len = key.length();
		int index = -1;
		int start = 0;
		int end = labels.size()-1;
		int mid = (start+end) >>> 1;
		while(start <= end) {
			mid = (start+end) >>> 1;
			//System.out.print(mid + " " + labels.get(mid).label);
			if(labels.get(mid).label.length() < len)
				continue;
			String substr = labels.get(mid).label.substring(0, len);
			if(substr.equalsIgnoreCase(key)) {
				//System.out.print(" = equal ");
				end = mid-1;
				index = mid;
			} else if(substr.compareToIgnoreCase(key) < 0) {
				//System.out.print(" => going right ");
				// key is bigger than current
				start = mid+1;
			} else {
				//System.out.print(" <= going left ");
				end = mid-1;
			}
			//System.out.println();
		}
		return index;
	}
	private int rightBinarySearch(ArrayList<NodeLabelID> labels, String key) {
		int len = key.length();
		int index = -1;
		int start = 0;
		int end = labels.size()-1;
		int mid = (start+end) >>> 1;
			while(start <= end) {
				//System.out.print(mid + " " + labels.get(mid).label);
				mid = (start+end) >>> 1;
				if(labels.get(mid).label.length() < len)
					continue;
				String substr = labels.get(mid).label.substring(0, len);
				if(substr.equalsIgnoreCase(key)) {
					//	System.out.print(" = equal ");
					start = mid+1;
					index = mid;
				} else if(substr.compareToIgnoreCase(key) < 0) {
					//	System.out.print(" => going right ");
					// key is bigger than current
					start = mid+1;
				} else {
					//	System.out.print(" <= going left ");
					end = mid-1;
				}
				//System.out.println();
			}
			return index;
	}*/


	public LinkedHashSet<Integer> getEntitiesForMultipleTypesIntersection(HashSet<Integer>typeValues) {
		long start = System.currentTimeMillis();
		LoadData ldlm = new LoadData();
		Iterator<Integer> it = typeValues.iterator();
		HashMap<Integer, Integer> typesInstancesCount = ldlm.getTypeInstancesCount();
		int smallestType = 0;
		int smallestCnt = Integer.MAX_VALUE;
		while(it.hasNext()) {
			int type = it.next();
			if(typesInstancesCount.containsKey(type) && typesInstancesCount.get(type) < smallestCnt) {
				smallestCnt = typesInstancesCount.get(type);
				smallestType = type;
			}
		}
		HashMap<Integer, LinkedHashSet<Integer>> entitiesForType = ldlm.getEntitiesForType();
		if(smallestType == 0) {
			LinkedHashSet<Integer> emptySet = new LinkedHashSet<Integer>();
			return emptySet;
		}
		LinkedHashSet<Integer> entitiesOfOtherTypesAlreadyFound = new LinkedHashSet<Integer>(entitiesForType.get(smallestType));
		if(!entitiesForType.containsKey(smallestType)) {
			System.out.println(smallestType+" key not found!!");
		}
		//System.out.println("Entities found for type " + smallestType + " = " + entitiesOfOtherTypesAlreadyFound.size());

		it = typeValues.iterator();
		while(it.hasNext() && !entitiesOfOtherTypesAlreadyFound.isEmpty()) {
			int type = it.next();
			if(type == smallestType)
				continue;
			LinkedHashSet<Integer> temp = new LinkedHashSet<Integer>(entitiesForType.get(type));
			entitiesOfOtherTypesAlreadyFound.retainAll(temp);
			//System.out.println("Entities found for type " + type + " = " + entitiesOfOtherTypesAlreadyFound.size());
		}
		//System.out.println("Node values calculation took "+(System.currentTimeMillis()-start)/1000.0+" seconds");
		return entitiesOfOtherTypesAlreadyFound;
	}

	private LinkedHashSet<Integer> getNodeValues(int graphNode, String typeValuesStr, String entity, int originalType, ArrayList<GuiEdgeInfo> queryGraph) {
			LoadData ldlm = new LoadData();
			LinkedHashSet<Integer> vals = new LinkedHashSet<Integer>();
			if(!entity.equals("-1")) { //this is entity node
				vals.add(Integer.parseInt(entity));
			} else { //this is type node
				HashSet<Integer> edgeEndTypes = new HashSet<Integer>();

				//finding type constraints for this node that comes from its adjacent edges, thus can be avoided
				for(GuiEdgeInfo edge: queryGraph) {
					if(edge.edge == -1) continue;
					if(edge.graphSource == graphNode) {
						edgeEndTypes.add(ldlm.getEdgeType().get(edge.edge).source_type);
					} else if (edge.graphObject == graphNode) {
						edgeEndTypes.add(ldlm.getEdgeType().get(edge.edge).object_type);
					}
				}

				HashSet<Integer> typesOfNode = new HashSet<Integer>();
				String[] typeValues = typeValuesStr.split(",");
				for(String t : typeValues) {
					if(t.equals("")) continue;
					int typeId = Integer.parseInt(t);
					if(!edgeEndTypes.contains(typeId)) {
						typesOfNode.add(typeId);
					}
				}
				if(typesOfNode.isEmpty() && originalType != -1 && !edgeEndTypes.contains(originalType)) {
					typesOfNode.add(originalType);
				}
				//System.out.println("typesOfNode = "+typesOfNode);
				if(!typesOfNode.isEmpty()) {
					vals = getEntitiesForMultipleTypesIntersection(typesOfNode);
				}
				// for(int t: typesOfNode) {
				// 	System.out.print(t+" ");
				// }System.out.println("");
			}
			//System.out.println(graphNode+" ---> <"+typeValuesStr+">,"+entity+","+originalType+","+vals.size());
			return vals;
	}

	public QueryResult evaluateQueryGraph(ArrayList<GuiEdgeInfo> queryGraph) {
		long start = System.currentTimeMillis();

		LoadData ldlm = new LoadData();

		ArrayList<ArrayList<Integer>> answerTuples = new ArrayList<ArrayList<Integer>>();
		HashSet<Integer> seenEdges = new HashSet<Integer>();
		LinkedHashMap<Integer, Integer> nodeToTupleCol = new LinkedHashMap<Integer, Integer>();
		HashMap<Integer, LinkedHashSet<Integer>> nodeValues = new HashMap<Integer, LinkedHashSet<Integer>>();
		LinkedHashMap<Integer, HashSet<Integer>> evaluatedNodeValues = new LinkedHashMap<Integer, HashSet<Integer>>();
		HashMap<Integer, Integer> degrees = new HashMap<Integer, Integer>();

		//storing initial entity values of individual nodes
		for(GuiEdgeInfo edge: queryGraph) {
			//System.out.println("Querygraph : "+edge.source+" ; "+edge.sourceTypeValues+" ; "+edge.object+" ; "+edge.objectTypeValues);
			if(edge.graphSource != -1 && !nodeValues.containsKey(edge.graphSource)) {
				nodeValues.put(edge.graphSource, getNodeValues(edge.graphSource, edge.sourceTypeValues, edge.sourceEntity, edge.source, queryGraph));
			}
			if(edge.graphObject != -1 && !nodeValues.containsKey(edge.graphObject)) {
				nodeValues.put(edge.graphObject, getNodeValues(edge.graphObject, edge.objectTypeValues, edge.objectEntity, edge.object, queryGraph));
			}
			if(edge.graphSource != -1) degrees.put(edge.graphSource, degrees.containsKey(edge.graphSource) ? degrees.get(edge.graphSource) + 1 : 1);
			if(edge.graphObject != -1) degrees.put(edge.graphObject, degrees.containsKey(edge.graphObject) ? degrees.get(edge.graphObject) + 1 : 1);
		}

		//System.out.println("degrees of nodes "+degrees.keySet().toString() +" are "+degrees.values().toString());

		//System.out.println("Initial nodevalue calculation took "+(System.currentTimeMillis()-start)/1000.0+" seconds.");

		boolean newComponentFound = false;
		int curSortedCol = -1;

		while(true) {
			double minEstimatedSize = Double.POSITIVE_INFINITY;
			int maxDegrees = -Integer.MAX_VALUE;
			boolean joinOnSource = false;
			int selectedEdgeIndex = -1;

			for(int i = 0; i < queryGraph.size(); i++) {
				GuiEdgeInfo edge = queryGraph.get(i);
				double estimatedSize = -1.0;
				double selectivity = -1.0;
				boolean sourceSelected = false;
				int nodeDegree = -1;

				// int expectedSourceType = (edge.edge != -1) ? ldlm.getEdgeType().get(edge.edge).source_type : -1;
				// int expectedTargetType = (edge.edge != -1) ? ldlm.getEdgeType().get(edge.edge).object_type : -1;
				// int estimatedSourceSize = (edge.edge != -1) ? ldlm.getTypeInstancesCount().get(expectedSourceType) : -1;
				// int estimatedTargetSize = (edge.edge != -1) ? ldlm.getTypeInstancesCount().get(expectedTargetType) : -1;

				if(!seenEdges.contains(i)) {
					if(!newComponentFound) { //choosing first node in a new connected component
						if(edge.edge != -1) {
							// sourceSelected = (estimatedSourceSize > estimatedTargetSize) ? false : true;
							// selectivity = ((double)ldlm.getEdgeInstancesCount().get(edge.edge) / estimatedSourceSize)/estimatedTargetSize;
							// estimatedSize = selectivity * (edge.sourceEntity.equals("-1") ? estimatedSourceSize : 1) * (edge.objectEntity.equals("-1") ? estimatedTargetSize : 1);

							sourceSelected = (degrees.get(edge.graphSource) > degrees.get(edge.graphObject));
							nodeDegree = Math.max(degrees.get(edge.graphSource), degrees.get(edge.graphObject));
							if(!edge.sourceEntity.equals("-1")  || !edge.objectEntity.equals("-1")) {
								selectedEdgeIndex = i;
								joinOnSource = edge.sourceEntity.equals("-1") ? false : true;
								break;
							}
						} else {
							sourceSelected = true;
							selectedEdgeIndex = i;
							break; //found a new component, which is a single node. thus no further edge comparison needed
						}
					} else {
						if(nodeToTupleCol.containsKey(edge.graphSource)) { //condition for keeping the graph connected
							// selectivity = ((double)ldlm.getEdgeInstancesCount().get(edge.edge) / estimatedSourceSize)/estimatedTargetSize;
							// estimatedSize = selectivity * (edge.objectEntity.equals("-1") ? estimatedTargetSize : 1) * answerTuples.size();

							sourceSelected = true;
							nodeDegree = degrees.get(edge.graphSource);

							if(!edge.sourceEntity.equals("-1") || nodeToTupleCol.get(edge.graphSource) == curSortedCol) {
								selectedEdgeIndex = i;
								joinOnSource = true;
								break;
							}
						}
						if(nodeToTupleCol.containsKey(edge.graphObject)) {
							// selectivity = ((double)ldlm.getEdgeInstancesCount().get(edge.edge) / estimatedSourceSize)/estimatedTargetSize;
							// estimatedSize = selectivity * (edge.sourceEntity.equals("-1") ? estimatedSourceSize : 1) * answerTuples.size();

							if(degrees.get(edge.graphObject) > nodeDegree) {
								sourceSelected = false;
								nodeDegree = degrees.get(edge.graphObject);
							}

							if(!edge.objectEntity.equals("-1") || nodeToTupleCol.get(edge.graphObject) == curSortedCol) {
								selectedEdgeIndex = i;
								joinOnSource = false;
								break;
							}
						}
					}
				}

				if(nodeDegree != -1 && nodeDegree > maxDegrees) {
					maxDegrees = nodeDegree;
					selectedEdgeIndex = i;
					joinOnSource = sourceSelected ? true : false;
				}

				// if(estimatedSize != -1.0 && estimatedSize < minEstimatedSize) {
				// 	minEstimatedSize = estimatedSize;
				// 	selectedEdgeIndex = i;
				// 	joinOnSource = sourceSelected ? true : false;
				// }
			}

			if(selectedEdgeIndex == -1) { //no new edge found, implies the end of current connected component. storing the evaluated values
				long start1 = System.currentTimeMillis();
				//System.out.println("nodeToTupleCol  = "+nodeToTupleCol);
				//if(!answerTuples.isEmpty()) System.out.println("number of answertuple cols  = "+answerTuples.get(0).size());
				for (Map.Entry<Integer, Integer> entry : nodeToTupleCol.entrySet()) {
					HashSet<Integer> values = new HashSet<Integer>();
					int graphNode = entry.getKey();
					int col = entry.getValue();
					if(!answerTuples.isEmpty()) {
						for(int i = 0; i < answerTuples.size(); i++) {
							values.add(answerTuples.get(i).get(col));
						}
					}
					evaluatedNodeValues.put(graphNode, values);
					//System.out.println(values.size()+" values found for node "+graphNode);
				}
				//System.out.println("Storing the final result in evaluatedNodeValues took "+(System.currentTimeMillis()-start1)/1000.0+" seconds");
				//terminate if all edges are taken
				if(seenEdges.size() == queryGraph.size()) {
					break;
				}
				//reset
				newComponentFound = false;
				minEstimatedSize = Double.POSITIVE_INFINITY;
				nodeToTupleCol.clear();
				answerTuples.clear();
			} else {
				seenEdges.add(selectedEdgeIndex);
				newComponentFound = true;

				GuiEdgeInfo selectedEdge = queryGraph.get(selectedEdgeIndex);
				int joinNode = (selectedEdge.edge == -1 || joinOnSource) ? selectedEdge.graphSource : selectedEdge.graphObject;
				if(answerTuples.isEmpty() && nodeToTupleCol.isEmpty()) {
					long start2 = System.currentTimeMillis();
					//System.out.println(joinNode+" , "+nodeValues.get(joinNode).size());
					for(Integer val : nodeValues.get(joinNode)) {
						ArrayList<Integer> tuple = new ArrayList<Integer>();
						tuple.add(val);
						answerTuples.add(tuple);
					}
					nodeToTupleCol.put(joinNode, 0);
					//System.out.println("Populating answerTuples of size "+ answerTuples.size() +" for a component's first node "+joinNode+ " took "+(System.currentTimeMillis()-start2)/1000.0+" seconds");
				}
				if(selectedEdge.edge != -1) {
					int otherNode = joinOnSource ? selectedEdge.graphObject : selectedEdge.graphSource;
					answerTuples = extendTuples(joinOnSource, nodeToTupleCol, selectedEdge, answerTuples, nodeValues, curSortedCol);

					// if(answerTuples.size() < 50) {
					// 	System.out.println("Print intermediate answertuple");
					// 	for(int j = 0; j < answerTuples.size(); j++) {
					// 		for(int k = 0; k < answerTuples.get(j).size(); k++) {
					// 			System.out.print(answerTuples.get(j).get(k)+", ");
					// 		}
					// 		System.out.println("");
					// 	}
					// }

					curSortedCol = nodeToTupleCol.get(joinNode);
					if(!nodeToTupleCol.containsKey(otherNode)) { //check if not cyclic
						nodeToTupleCol.put(otherNode, nodeToTupleCol.size());
					}
				}
			}
		}

		//System.out.println("Query graph evaluation took "+(System.currentTimeMillis()-start)/1000.0+" seconds");


		// if(answerTuples.size() < 50) {
		// 	System.out.println("Print final answertuple");
		// 	for(int i = 0; i < answerTuples.size(); i++) {
		// 		for(int j = 0; j < answerTuples.get(i).size(); j++) {
		// 			System.out.print(answerTuples.get(i).get(j)+", ");
		// 		}
		// 		System.out.println("");
		// 	}
		// }

		QueryResult qr = new QueryResult();
		qr.values = evaluatedNodeValues;
		qr.tuples = answerTuples;
		// qr.nodeToTupleCol = nodeToTupleCol;
		// qr.nodeValues = nodeValues;
		// qr.curSortedCol = curSortedCol;

		// System.out.println("evaluatedNodeValues size = "+evaluatedNodeValues.size());
		//System.out.println("answerTuples columns = "+answerTuples.get(0).size());
		//System.out.println("answerTuples rows = "+answerTuples.size());
		return qr;
	}

	public ArrayList<ArrayList<Integer>> extendTuples(boolean joinOnSource, LinkedHashMap<Integer, Integer> nodeToTupleCol, GuiEdgeInfo triple, ArrayList<ArrayList<Integer>> answerTuples, HashMap<Integer, LinkedHashSet<Integer>> nodeValues, int curSortedCol) {
		//System.out.println("Extending answer tuple");
		ArrayList<ArrayList<Integer>> tempAnswerTuples = new ArrayList<ArrayList<Integer>>();
		try {
			System.out.println(joinOnSource+", "+ nodeToTupleCol.size()+", "+triple.graphObject+", "+ triple.graphSource+", "+ triple.edge+", "+answerTuples.size()+", "+nodeValues.size()+", "+curSortedCol);

			int curNode = joinOnSource ? triple.graphObject : triple.graphSource;
			int parentNode = joinOnSource ? triple.graphSource : triple.graphObject;
			int parentNodeIdx = nodeToTupleCol.get(parentNode);
			LinkedHashSet<Integer> curNodeConstraints = nodeValues.get(curNode);

			long start = System.currentTimeMillis();
			FileReader fr = null;
			if(curNode == triple.graphSource) { //join key was the target end
				fr = new FileReader(conf.getInputFilePath(PropertyKeys.targetAlignedPropertyTable)+Integer.toString(triple.edge));
			} else { //join key was the source end
				fr = new FileReader(conf.getInputFilePath(PropertyKeys.sourceAlignedPropertyTable)+Integer.toString(triple.edge));
			}

			//FIX
			// if(triple.edge == 47183530) {
			// 	ArrayList<Integer>tempAnswerTuple = new ArrayList<Integer>();
			// 	tempAnswerTuple.add(44200534);
			// 	tempAnswerTuple.add(35434359);
			// 	answerTuples.add(tempAnswerTuple);
			// } else if (triple.edge == 47181826 && answerTuples.size() > 0 && answerTuples.get(0).size() == 3) {
			// 	ArrayList<Integer>tempAnswerTuple = new ArrayList<Integer>();
			// 	tempAnswerTuple.add(44200534);
			// 	tempAnswerTuple.add(35434359);
			// 	tempAnswerTuple.add(17528699);
			// 	answerTuples.add(tempAnswerTuple);
			// }

		
			// //if(triple.edge == 47181826 || triple.edge == 47183530 ) {
			// if(answerTuples.size() < 50) {
			// 	System.out.println("Before sorting:");
			// 	for(int i = 0; i < answerTuples.size(); i++) {
			// 		for(int j = 0; j < answerTuples.get(i).size(); j++) {
			// 			System.out.print(answerTuples.get(i).get(j)+", ");
			// 		}
			// 		System.out.println("");
			// 	}
			// }

			if(curSortedCol != parentNodeIdx) { //check if tuples are already sorted on this column
				final int colToSortTuples = parentNodeIdx;
				//sorting answers on specific column
				long start1 = System.currentTimeMillis();
				Collections.sort(answerTuples, new Comparator<ArrayList<Integer>> () {
			    @Override
			    public int compare(ArrayList<Integer> a, ArrayList<Integer> b) {
			        return a.get(colToSortTuples).compareTo(b.get(colToSortTuples));
			    }
				});
				//System.out.println("Sorting answerTuples took "+(System.currentTimeMillis()-start1)/1000.0+" seconds for size "+answerTuples.size());
			} else {
				//System.out.println("answer tuples already sorted on this column!");
			}

			//System.out.println("sort on column = "+col);
			//System.out.println("first three tuples adter sorting on col = "+answerTuples.get(0).toString()+answerTuples.get(1).toString()+answerTuples.get(2).toString());

			int curToupleId = 0;
			int curNodeValue = -1, parentNodeValue = -1;
			int lastTupleOfNewComp = 0; //last tuple id where a new entity was encountered in the column

			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			String[] tokens = line.trim().split(",");
			parentNodeValue = Integer.parseInt(tokens[0]);
			curNodeValue =  Integer.parseInt(tokens[1]);

			long start2 = System.currentTimeMillis();
			//System.out.println("curNodeValue = "+curNodeValue+", parentNodeValue = "+parentNodeValue+", edge = "+triple.edge+", curNodeConstraintsSize = "+curNodeConstraints.size());
			//System.out.println("curNode constrains size = "+ curNodeConstraints.size());
			//System.out.println("We will join with table size = "+LoadData.getEdgeInstancesCount().get(triple.edge));
			//int k = 0;
			//ArrayList<Integer> tempTuple = new ArrayList<Integer>();

			long start4 = 0;
			long lineCount = 0;
			long maxTime = -1;


			while(line != null) {
			// while(br.ready()) {
				// if(triple.edge == 47183530) {
				// 	System.out.println(line);
				// }
				if(answerTuples.isEmpty()) {
					//System.out.println("nodeToTupleCol size = "+nodeToTupleCol.size());
					if(nodeToTupleCol.size()==1) { //when intermediate result is empty, because it is a first node in the component having no type constraint apart from edge constraint
						
						if(curNodeConstraints.isEmpty() || curNodeConstraints.contains(curNodeValue)) { //when the join node does not have any constrains, thus the parent node (join key) values are same as the propery table column		
								ArrayList<Integer> tempTuple = new ArrayList<Integer>();
								tempTuple.add(parentNodeValue);
								tempTuple.add(curNodeValue);
								tempAnswerTuples.add(tempTuple);
						}

						long start3 = System.currentTimeMillis();
						line = br.readLine();
						if(line == null) break;
						lineCount++;

						tokens = line.trim().split(",");
						parentNodeValue = Integer.parseInt(tokens[0]);
						curNodeValue =  Integer.parseInt(tokens[1]);
						long t = (System.currentTimeMillis()-start3);
						start4 += (System.currentTimeMillis()-start3);
						maxTime = Math.max(maxTime, t);
					} else { //when intermediate result is empty because of empty join result, abort query evaluation
						break;
					}
				}
				else if(answerTuples.get(curToupleId).get(parentNodeIdx) < parentNodeValue) {
					curToupleId++;
					if(curToupleId >= answerTuples.size()) {
						break;
					}
					else if(answerTuples.get(curToupleId).get(parentNodeIdx) > answerTuples.get(lastTupleOfNewComp).get(parentNodeIdx)) {
						lastTupleOfNewComp = curToupleId;
					}
				} else if (answerTuples.get(curToupleId).get(parentNodeIdx) > parentNodeValue) {
					line = br.readLine();
					if(line == null) break;
					tokens = line.trim().split(",");
					parentNodeValue = Integer.parseInt(tokens[0]);
					curNodeValue =  Integer.parseInt(tokens[1]);

					if(curToupleId > 0 && answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
						curToupleId = lastTupleOfNewComp;
					} else if(answerTuples.get(curToupleId).get(parentNodeIdx) > answerTuples.get(lastTupleOfNewComp).get(parentNodeIdx)) {
							lastTupleOfNewComp = curToupleId;
					}
				} else { //found a common element to join
					if(nodeToTupleCol.containsKey(curNode)) {
						//cyclic case, curNode was already visited
						int curNodeIdx = nodeToTupleCol.get(curNode);
						//System.out.println("circular case found! trying to match "+answerTuples.get(curToupleId).get(curNodeIdx)+" and "+curNodeValue);
						if(answerTuples.get(curToupleId).get(curNodeIdx) == curNodeValue) {
							ArrayList<Integer> tempTuple = new ArrayList<Integer>(answerTuples.get(curToupleId));
							//tempTuple.addAll(answerTuples.get(curToupleId));
							tempAnswerTuples.add(tempTuple);
						}
						curToupleId++;
						if(curToupleId >= answerTuples.size()) {
							line = br.readLine();
							if(line == null) break;
							tokens = line.trim().split(",");
							parentNodeValue = Integer.parseInt(tokens[0]);
							curNodeValue =  Integer.parseInt(tokens[1]);

							if(answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
								curToupleId = lastTupleOfNewComp;
							} else {
								break;
							}
						}
					}
					else {
						if(curNodeConstraints.isEmpty() || curNodeConstraints.contains(curNodeValue)) {
							ArrayList<Integer> tempTuple = new ArrayList<Integer>(answerTuples.get(curToupleId));
							//tempTuple.addAll(answerTuples.get(curToupleId));
							tempTuple.add(curNodeValue); //extending tuple by one column
							tempAnswerTuples.add(tempTuple);
							curToupleId++;
							if(curToupleId >= answerTuples.size()) {
								line = br.readLine();
								if(line == null) break;
								tokens = line.trim().split(",");
								parentNodeValue = Integer.parseInt(tokens[0]);
								curNodeValue =  Integer.parseInt(tokens[1]);

								if(answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
									curToupleId = lastTupleOfNewComp;
								} else {
									break;
								}
							}
						} else {
							line = br.readLine();
							if(line == null) break;
							tokens = line.trim().split(",");
							parentNodeValue = Integer.parseInt(tokens[0]);
							curNodeValue =  Integer.parseInt(tokens[1]);
						}
					}
				}

				//tempTuple.clear();

				//break if for large table join it is taking more than 5 seconds
				if(System.currentTimeMillis()-start2 > 5000.0)
					break;
			}

			br.close();

			System.out.println("max "+ maxTime/1000.0+" seconds");
			System.out.println("average "+ (1.0*start4)/(lineCount*1000.0) +" seconds");
			System.out.println("read took  "+ start4/1000.0+" seconds");
			System.out.println("Merge join took "+(System.currentTimeMillis()-start2)/1000.0+" seconds");
			System.out.println("Joined property table "+triple.edge+" of size "+LoadData.getEdgeInstancesCount().get(triple.edge)+" with answerTuples of size "+answerTuples.size()+" and got new answerTuples size "+tempAnswerTuples.size());
			//System.out.println("Extending on graphnode "+curNode+" took "+(System.currentTimeMillis()-start)/1000.0+" seconds");
		} catch(IOException ioe) {
			System.out.println(ioe);
			ioe.printStackTrace();
		}

		// for(int i = 0; i < tempAnswerTuples.size(); i++) {
		// 	if(tempAnswerTuples.get(i).get(0)==32755195) {
		// 		System.out.println(tempAnswerTuples.get(i).toString());
		// 	}
		// }
		return tempAnswerTuples;
	}

	private ArrayList<ArrayList<Integer>> extendTuplesTEST(boolean joinOnSource, HashMap<Integer, Integer> nodeToTupleCol, GuiEdgeInfo triple, ArrayList<ArrayList<Integer>> answerTuples, HashMap<Integer, LinkedHashSet<Integer>> nodeValues, int curSortedCol) {
		//System.out.println("Extending answer tuple");
		ArrayList<ArrayList<Integer>> tempAnswerTuples = new ArrayList<ArrayList<Integer>>();

		int curNode = joinOnSource ? triple.graphObject : triple.graphSource;
		int parentNode = joinOnSource ? triple.graphSource : triple.graphObject;
		int parentNodeIdx = nodeToTupleCol.get(parentNode);
		LinkedHashSet<Integer> curNodeConstraints = nodeValues.get(curNode);

		long start = System.currentTimeMillis();
		LinkedHashSet<Pair> propTable = null;
		LoadData ldlm = new LoadData();
		// FileReader fr = null;
		if(curNode == triple.graphSource) { //join key was the target end
			propTable = ldlm.getObjPropTable().get(triple.edge);
		} else { //join key was the source end
			propTable = ldlm.getSrcPropTable().get(triple.edge);
		}

		if(curSortedCol != parentNodeIdx) { //check if tuples are already sorted on this column
			final int colToSortTuples = parentNodeIdx;
			//sorting answers on specific column
			long start1 = System.currentTimeMillis();
			Collections.sort(answerTuples, new Comparator<ArrayList<Integer>> () {
				@Override
				public int compare(ArrayList<Integer> a, ArrayList<Integer> b) {
						return a.get(colToSortTuples).compareTo(b.get(colToSortTuples));
				}
			});
			System.out.println("Sorting answerTuples took "+(System.currentTimeMillis()-start1)/1000.0+" seconds for size "+answerTuples.size());
		} else {
			System.out.println("answer tuples already sorted on this column!");
		}

		//System.out.println("sort on column = "+col);
		//System.out.println("first three tuples adter sorting on col = "+answerTuples.get(0).toString()+answerTuples.get(1).toString()+answerTuples.get(2).toString());

		int curToupleId = 0;
		int curNodeValue = -1, parentNodeValue = -1;
		int lastTupleOfNewComp = 0; //last tuple id where a new entity was encountered in the column

		//BufferedReader br = new BufferedReader(fr);
		Iterator<Pair> itr = propTable.iterator();
		Pair line = itr.next();
		parentNodeValue = line.x;
		curNodeValue =  line.y;

		long start2 = System.currentTimeMillis();
		//System.out.println("curNodeValue = "+curNodeValue+", parentNodeValue = "+parentNodeValue+", edge = "+triple.edge+", curNodeConstraintsSize = "+curNodeConstraints.size());
		System.out.println("curNode constrains size = "+ curNodeConstraints.size());
		System.out.println("We will join with table size = "+LoadData.getEdgeInstancesCount().get(triple.edge));
		//int k = 0;

		while(line != null) {
			if(answerTuples.isEmpty()) { //when the join node does not have any constrains, thus the parent node (join key) values are same as the propery table column
				if(curNodeConstraints.isEmpty() || curNodeConstraints.contains(curNodeValue)) {
						ArrayList<Integer> tempTuple = new ArrayList<Integer>();
						tempTuple.add(parentNodeValue);
						tempTuple.add(curNodeValue);
						tempAnswerTuples.add(tempTuple);
				}
				// line = br.readLine();
				// if(line == null) break;
				if(!itr.hasNext()) break;
				line = itr.next();
				parentNodeValue = line.x;
				curNodeValue =  line.y;
			}
			else if(answerTuples.get(curToupleId).get(parentNodeIdx) < parentNodeValue) {
				curToupleId++;
				if(curToupleId >= answerTuples.size()) {
					break;
				}
				else if(answerTuples.get(curToupleId).get(parentNodeIdx) > answerTuples.get(lastTupleOfNewComp).get(parentNodeIdx)) {
					lastTupleOfNewComp = curToupleId;
				}
			} else if (answerTuples.get(curToupleId).get(parentNodeIdx) > parentNodeValue) {
				// line = br.readLine();
				// if(line == null) break;
				if(!itr.hasNext()) break;
				line = itr.next();
				parentNodeValue = line.x;
				curNodeValue =  line.y;

				if(curToupleId > 0 && answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
					curToupleId = lastTupleOfNewComp;
				} else if(answerTuples.get(curToupleId).get(parentNodeIdx) > answerTuples.get(lastTupleOfNewComp).get(parentNodeIdx)) {
						lastTupleOfNewComp = curToupleId;
				}
			} else { //found a common element to join
				if(nodeToTupleCol.containsKey(curNode)) {
					//cyclic case, curNode was already visited
					int curNodeIdx = nodeToTupleCol.get(curNode);
					//System.out.println("circular case found! trying to match "+answerTuples.get(curToupleId).get(curNodeIdx)+" and "+curNodeValue);
					if(answerTuples.get(curToupleId).get(curNodeIdx) == curNodeValue) {
						ArrayList<Integer> tempTuple = new ArrayList<Integer>(answerTuples.get(curToupleId));
						tempAnswerTuples.add(tempTuple);
					}
					curToupleId++;
					if(curToupleId >= answerTuples.size()) {
						if(!itr.hasNext()) break;
						line = itr.next();
						parentNodeValue = line.x;
						curNodeValue =  line.y;

						if(answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
							curToupleId = lastTupleOfNewComp;
						} else {
							break;
						}
					}
				}
				else {
					if(curNodeConstraints.isEmpty() || curNodeConstraints.contains(curNodeValue)) {
						ArrayList<Integer> tempTuple = new ArrayList<Integer>(answerTuples.get(curToupleId));
						tempTuple.add(curNodeValue); //extending tuple by one column
						tempAnswerTuples.add(tempTuple);
						curToupleId++;
						if(curToupleId >= answerTuples.size()) {
							if(!itr.hasNext()) break;
							line = itr.next();
							parentNodeValue = line.x;
							curNodeValue =  line.y;

							if(answerTuples.get(curToupleId-1).get(parentNodeIdx) == parentNodeValue) {
								curToupleId = lastTupleOfNewComp;
							} else {
								break;
							}
						}
					} else {
						if(!itr.hasNext()) break;
						line = itr.next();
						parentNodeValue = line.x;
						curNodeValue =  line.y;
					}
				}
			}

			//break if for large table join it is taking more than 5 seconds
			if(System.currentTimeMillis()-start2 > 5000.0)
				break;
		}
		// System.out.println("Merge join took "+(System.currentTimeMillis()-start2)/1000.0+" seconds");
		// System.out.println("Joined property table "+triple.edge+" of size "+LoadData.getEdgeInstancesCount().get(triple.edge)+" with answerTuples of size "+answerTuples.size()+" and got new answerTuples size "+tempAnswerTuples.size());
		// System.out.println("Extending on graphnode "+curNode+" took "+(System.currentTimeMillis()-start)/1000.0+" seconds");


		// for(int i = 0; i < tempAnswerTuples.size(); i++) {
		// 	if(tempAnswerTuples.get(i).get(0)==32755195) {
		// 		System.out.println(tempAnswerTuples.get(i).toString());
		// 	}
		// }
		return tempAnswerTuples;
	}

	//check if two partial graphs are equal
	public boolean equalGraphs(ArrayList<GuiEdgeInfo> g1, ArrayList<GuiEdgeInfo> g2) {
		if(g1.size() != g2.size()) {
			System.out.println("partial graph size doesn't match with previous version!!!");
			return false;
		}
		for(int i = 0; i < g1.size(); i++) {
			GuiEdgeInfo e1 = g1.get(i);
			GuiEdgeInfo e2 = g2.get(i);
			//removed "e1.source==e2.source && e1.object==e2.object" from condition
			if(!(e1.edge==e2.edge && equalTypeValues(e1.sourceTypeValues, e2.sourceTypeValues) && equalTypeValues(e1.objectTypeValues, e2.objectTypeValues) && e1.sourceEntity.equals(e2.sourceEntity) && e1.objectEntity.equals(e2.objectEntity))) {
				return false;
			}
		}
		return true;
	}

	//check if two type lists are equal
	public boolean equalTypeValues(String t1, String t2) {
		HashSet<String> typesSet1 = new HashSet<String>();
		HashSet<String> typesSet2 = new HashSet<String>();
		for(String s : t1.split(",")) {
			if(!s.equals("")) typesSet1.add(s);
		}
		for(String s : t2.split(",")) {
			if(!s.equals("")) typesSet2.add(s);
		}
		return typesSet1.equals(typesSet2);
	}


	private void getInstancesIntersection(HashSet<Integer> typesOfThisNode, ArrayList<NodeLabelID> entities, ArrayList<NodeLabelID> types, ArrayList<NodeLabelID> domains, HashSet<Integer> nodeValues) {
		try {

			String filePath = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);

			// LinkedHashSet<Integer> entitiesOfOtherTypesAlreadyFound = getEntitiesForMultipleTypesIntersection(typesOfThisNode);
			//
			// if(!neighborEntities.equals("")) {
			// 	String[] neighbors = neighborEntities.split("\\|");
			// 	GenerateCandidatesNew gc = new GenerateCandidatesNew(conf);
			// 	entitiesOfOtherTypesAlreadyFound = gc.filterByEdgeConstraint(entitiesOfOtherTypesAlreadyFound, new HashSet<String>(Arrays.asList(neighbors)));
			// }

			//System.out.println("Elapsed time 2 = "+(System.currentTimeMillis()-start));

			//start = System.currentTimeMillis();

			HashSet<String> typesOfAllEntities = new HashSet<String>();




			Iterator<Integer> it = typesOfThisNode.iterator();
			if(typesOfThisNode.size() == 1) {
				typesOfAllEntities = LoadData.getTypesClique().get(Integer.toString(it.next()));
			} else if(typesOfThisNode.size() == 2) {
				int first = it.next();
				int second = it.next();
				if(first < second) {
					typesOfAllEntities = LoadData.getTypesClique().get(Integer.toString(first)+","+Integer.toString(second));
				} else {
					typesOfAllEntities = LoadData.getTypesClique().get(Integer.toString(second)+","+Integer.toString(first));
				}
			}
			//System.out.println("Elapsed time 2.1 = "+(System.currentTimeMillis()-start));
			//storing entity results in array
			HashMap<Integer, String> nodeLabelIndex = LoadData.getNodeLabelIndex();
			Iterator<Integer> iter = nodeValues.iterator();
			int entityCnt = 0;
			//Iterator iter =  entitiesOfOtherTypesAlreadyFound.entrySet().iterator();
			while(iter.hasNext()) {
				NodeLabelID nli = new NodeLabelID();
				Integer entity = iter.next();
				nli.id = entity;
				nli.label = nodeLabelIndex.get(entity);

				if(nli.label == null) continue;
				entities.add(nli);

				if(typesOfThisNode.size() > 2) {
					ArrayList<String> typesForEntity = new ArrayList<String>();
					getTypesForEntity(nli.id, typesForEntity);
					for(String t : typesForEntity) {
						if(!t.equals("0,Select Type...")) {
							typesOfAllEntities.add(t.split(",")[0]);
						}
					}
				}
			}
			//System.out.println("Elapsed time 2.2 = "+(System.currentTimeMillis()-start));
			//storing type results in array
			Iterator<String> setIter = typesOfAllEntities.iterator();
			HashSet<String> domainOfAllTypes = new HashSet<String>();
			while(setIter.hasNext()) {
				NodeLabelID nli = new NodeLabelID();
				String type = setIter.next();
				nli.id = Integer.parseInt(type);
				nli.label = nodeLabelIndex.get(nli.id).replace("_", " ");
				//if(typeKeyword == "" || nli.label.toLowerCase().startsWith(typeKeyword.toLowerCase())) {
				types.add(nli);
			  //}
				ArrayList<String> arr = domainForTypes(type);
				if(arr.size() != 1) {
					domainOfAllTypes.add(arr.get(1));
				}
			}
			//System.out.println("Elapsed time 2.3 = "+(System.currentTimeMillis()-start));
			//storing domain results in array
			setIter = domainOfAllTypes.iterator();
			while(setIter.hasNext()) {
				NodeLabelID nli = new NodeLabelID();
				String domain = setIter.next();
				nli.id = Integer.parseInt(domain.split(",")[0]);
				nli.label = domain.split(",")[1];
				domains.add(nli);
			}
			//System.out.println("Elapsed time 3 = "+(System.currentTimeMillis()-start));


			baf.close();
		} catch(IOException ioe) {
			System.out.println(ioe);
			ioe.printStackTrace();
		}
	}


	public ArrayList<String> getInstancesUnion(HashSet<Integer> typesOfThisNode,  int entityWindowSize, int entityWindowNumber){
		HashMap<Integer, String> entities = new HashMap<Integer, String>();

		ArrayList<String> returnValues = new ArrayList<String>();
		try{
			Iterator<Integer> it = typesOfThisNode.iterator();
			String filePath = conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.typesSortedToInstancesIndexNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);
			while(it.hasNext()){
				int type = it.next();
				baf.getNodeIndexValues(type, entities);
			}
			ArrayList<NodeLabelID> labels = new ArrayList<NodeLabelID>();
			Iterator<Integer> iter = entities.keySet().iterator();
			while(iter.hasNext()) {
				NodeLabelID nli = new NodeLabelID();
				nli.id = iter.next();
				nli.label = entities.get(nli.id);
				labels.add(nli);
			}
			Collections.sort(labels, new Comparator<NodeLabelID>() {
				public int compare(NodeLabelID nl1, NodeLabelID nl2) {
					return nl1.label.compareToIgnoreCase(nl2.label);
				}
			});


			returnValues.add("0,Select Entity...,nopreview");
			for(int i=(entityWindowNumber*entityWindowSize); i<labels.size() && entityWindowSize-- > 0; i++) {
				NodeLabelID nli = labels.get(i);
				if(getWikiSummary(nli.id)=="") {
					returnValues.add(nli.id + "," + nli.label + ",nopreview");
				} else {
					returnValues.add(nli.id + "," + nli.label+ ",preview");
				}
			}

		 	baf.close();
		} catch(IOException ioe) {
			System.out.println(ioe);
			ioe.printStackTrace();
		}
		return returnValues;

	}

	public HashSet<Integer> getListFromString(String str) {
		HashSet<Integer> res = new HashSet<Integer>();
		for(String elem : str.split(",")) {
			elem = elem.trim();
			if(!elem.equals(""))
				res.add(Integer.parseInt(elem));
		}
		return res;
	}
}
