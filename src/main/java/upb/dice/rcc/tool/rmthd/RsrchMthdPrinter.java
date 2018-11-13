package upb.dice.rcc.tool.rmthd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import upb.dice.rcc.tool.rmthd.generator.RsrchMthdMdlGnrtr;

/**
 * Class to help with printing of results for Research Method mapping
 * 
 * @author nikitsrivastava
 *
 */
public abstract class RsrchMthdPrinter {

	public static final String LABEL_FLD = "skos:prefLabel";

	public static void printResults(Map<String, Set<String>> resIdMap) throws IOException {
		// logic to read labels from the csv file
		String inputFilePath = "data/rcc/train_test/sage_research_methods.json";

		File inputFile = new File(inputFilePath);
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) RsrchMthdMdlGnrtr.OBJ_READER.readTree(fin);
			ArrayNode methodArr = (ArrayNode) inpObj.get(RsrchMthdMdlGnrtr.CNTNT_ARR_FLD);
			Iterator<JsonNode> methodItr = methodArr.iterator();
			while (methodItr.hasNext()) {
				JsonNode methodEle = methodItr.next();
				// check the id of the method
				Set<String> fileNames = resIdMap.get(methodEle.get(RsrchMthdMdlGnrtr.ID_FLD).asText());
				if (fileNames != null) {
					String methodLabel = methodEle.get(LABEL_FLD).get(RsrchMthdMdlGnrtr.VALUE_FLD).asText();
					for (String fileName : fileNames) {
						System.out.println(fileName + " : " + methodLabel);
					}
				}
			}
		} finally {
			fin.close();
		}
	}

}
