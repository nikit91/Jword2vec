package upb.dice.rcc.tool.rfld;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;

public abstract class RsrchFieldPrinter{


	public static void printResults(Map<String, Set<String>> resIdMap, int idIndx, int resColIndx) throws IOException {
		// logic to read labels from the csv file
		String inputFilePath = "data/rcc/train_test/sage_research_fields.csv";

		File inputFile = new File(inputFilePath);
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new FileReader(inputFile));
			// Reading header
			csvReader.readNext();
			String[] lineArr;
			Set<String> idSet = resIdMap.keySet();
			// Read the csv file line by line
			while ((lineArr = csvReader.readNext()) != null) {
				String lineId = lineArr[idIndx];
				if (idSet.contains(lineId)) {
					Set<String> fileNameSet = resIdMap.get(lineId);
					for (String fileName : fileNameSet) {
						System.out.println(fileName + " : " + lineArr[resColIndx]);
					}
				}
			}

		} finally {
			csvReader.close();
		}
	}

}
