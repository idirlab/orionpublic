package viiq.otherClassifiers.randomForest;

public class RandomForestHelper {
	private String randomForestTreesOutputFolder;
	private String randomForestTempFolder;
	
	public RandomForestHelper(String outputFolderPath, String outputTempFolderPath) {
		randomForestTreesOutputFolder = outputFolderPath;
		randomForestTempFolder = outputTempFolderPath;
	}
	
	public String getTempOutputFileName(String tempFile) {
		return randomForestTempFolder + tempFile;
	}
	
	public String getTreeOutputFileName(int treeNumber) {
		return randomForestTreesOutputFolder + "rf" + treeNumber + ".model";
	}
}
