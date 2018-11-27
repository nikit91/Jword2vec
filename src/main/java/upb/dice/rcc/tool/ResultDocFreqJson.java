package upb.dice.rcc.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.*;
import java.util.*;

public class ResultDocFreqJson {
    private HashMap<String, HashMap<String, Double>> resultAttrMap = new HashMap<>();
    private int totalDocuments = 0;

    protected void readResultAttrJson(File resultAttrJsonFile) {
        try {
            FileInputStream inputStream = new FileInputStream(resultAttrJsonFile);
            ObjectMapper objM = new ObjectMapper();
            ObjectReader objR = objM.reader();

            ArrayNode fileNodes = (ArrayNode) objR.readTree(inputStream);

            for (JsonNode node : fileNodes) {
                totalDocuments++;

                ArrayNode resultNodes = (ArrayNode) node.get("result");
                setResultAttrMap(resultNodes);
            }
            setTfIdf();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected HashMap<String, HashMap<String, Double>> getFileResultMap() {
        return resultAttrMap;
    }

    protected void writeResultPropsToFile(String outputPath) {
        try {
            PrintWriter writer = new PrintWriter(outputPath, "UTF-8");
            for (String resultId : resultAttrMap.keySet()) {
                HashMap<String, Double> attrMap = resultAttrMap.get(resultId);

                String resultLine = String.format("%s,%s,%s,%s,%s",
                        resultId,
                        attrMap.get("docFreq"),
                        attrMap.get("scoreSum"),
                        attrMap.get("idf"),
                        attrMap.get("tfidf"));
                writer.println(resultLine);
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void setResultAttrMap(ArrayNode results) {
        Iterator<JsonNode> resultIt = results.iterator();
        while (resultIt.hasNext()) {
            JsonNode node = resultIt.next();

            String methodId = node.get("methodId").asText().trim();
            double score = node.get("score").asDouble();

            HashMap<String, Double> attrMap;
            if (resultAttrMap.containsKey(methodId)) {
                attrMap = resultAttrMap.get(methodId);

                double docFreq = attrMap.get("docFreq") + 1.0;
                double scoreSum = attrMap.get("scoreSum") + score;

                attrMap.put("docFreq", docFreq);
                attrMap.put("scoreSum", scoreSum);

                resultAttrMap.put(methodId, attrMap);
            }
            else {
                attrMap = new HashMap<>();
                attrMap.put("docFreq", 1.0);
                attrMap.put("scoreSum", score);

                resultAttrMap.put(methodId, attrMap);
            }
        }
    }

    private void setTfIdf() {
        for (String resultId : resultAttrMap.keySet()) {
            HashMap<String, Double> attrMap = resultAttrMap.get(resultId);

            double idf = Math.log((double) totalDocuments / attrMap.get("docFreq"));
            double scoreAvg = attrMap.get("scoreSum") / attrMap.get("docFreq");
            double tfidf = scoreAvg * idf;
            attrMap.put("idf", idf);
            attrMap.put("tfidf" , tfidf);


            resultAttrMap.put(resultId, attrMap);
        }
    }

    public static void main(String[] args) {
        ResultDocFreqJson resultDocFreqJson = new ResultDocFreqJson();
        resultDocFreqJson.readResultAttrJson(new File("data/rcc/res_out/research_methods_mult_results.json"));
        resultDocFreqJson.writeResultPropsToFile("data/rcc/idfRsrchMethodMap.csv");
    }
}
