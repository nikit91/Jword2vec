package upb.dice.rcc.tool.rmthd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class RsrchMthdIdfUtil {

	public static Logger LOG = LogManager.getLogger(RsrchMthdIdfUtil.class);

	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();
	public static final JsonNodeFactory JSON_NODE_FACTORY = OBJ_MAPPER.getNodeFactory();

	public static JsonNode readJsonFile(File jsonFile) throws JsonProcessingException, IOException {
		FileInputStream fin = null;
		JsonNode resObj = null;
		try {
			fin = new FileInputStream(jsonFile);
			// Read file into a json
			resObj = OBJ_READER.readTree(fin);
		} finally {
			fin.close();
		}
		return resObj;
	}

	public static ArrayNode mergeJsonResults(ArrayNode oldArrNode, ArrayNode newArrNode) {
		ArrayNode resArrNode = JSON_NODE_FACTORY.arrayNode();
		Map<String, JsonNode> pubNodeMap = new HashMap<>();
		// generate a pubId based jsonnode map
		addJsonNodesToMap(oldArrNode, pubNodeMap);
		// add new entries and replace old entries if duplicate
		addJsonNodesToMap(newArrNode, pubNodeMap);
		for (JsonNode pubNode : pubNodeMap.values()) {
			resArrNode.add(pubNode);
		}
		return resArrNode;
	}

	private static void addJsonNodesToMap(ArrayNode arrayNode, Map<String, JsonNode> nodesMap) {
		Iterator<JsonNode> arrayNodeItr = arrayNode.iterator();
		while (arrayNodeItr.hasNext()) {
			JsonNode pubNode = arrayNodeItr.next();
			String pubId = pubNode.get("fileName").asText();
			nodesMap.put(pubId, pubNode);
		}
	}

	public static Map<String, Double> generateMethodIdfMap(ArrayNode resArray) {
		// fetch frequency map
		Map<String, Integer> methodFreqMap = getMethodDocFreqMap(resArray);
		LOG.info("Generated Frequency Map: " + methodFreqMap);
		// generate idf map
		Map<String, Double> idfValMap = makeIdfMap(resArray.size(), methodFreqMap);
		LOG.info("Generated IDF value Map: " + idfValMap);
		return idfValMap;
	}

	private static Map<String, Double> makeIdfMap(int totalDoc, Map<String, Integer> freqMap) {
		Map<String, Double> idfValMap = new HashMap<>();
		for (String key : freqMap.keySet()) {
			int freqVal = freqMap.get(key);
			double idfVal = Math.log(((double) totalDoc) / ((double) freqVal));
			idfValMap.put(key, idfVal);
		}
		return idfValMap;
	}

	private static Map<String, Integer> getMethodDocFreqMap(ArrayNode resArray) {
		Iterator<JsonNode> resItr = resArray.iterator();
		Map<String, Integer> methodFreqMap = new HashMap<>();
		// generate frequency map
		while (resItr.hasNext()) {
			JsonNode curNode = resItr.next();
			JsonNode subResNode = curNode.get("result");
			if (subResNode.isArray()) {
				// loop through all the results
				ArrayNode subResArr = (ArrayNode) subResNode;
				Iterator<JsonNode> subResItr = subResArr.iterator();
				while (subResItr.hasNext()) {
					JsonNode resNode = subResItr.next();
					addResultToMap(resNode.get("methodId").asText(), methodFreqMap);
				}
			} else {
				// fetch single result
				addResultToMap(subResNode.get("methodId").asText(), methodFreqMap);
			}
		}
		return methodFreqMap;
	}

	private static void addResultToMap(String methodId, Map<String, Integer> freqMap) {
		Integer curFreq = freqMap.get(methodId);
		if (curFreq == null) {
			curFreq = 0;
		}
		freqMap.put(methodId, curFreq + 1);
	}

}
