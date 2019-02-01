package upb.dice.rcc.tool.vocab.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class AdvSparqlJson {
	
	public static final int UNION_SIZE = 50;
	
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();
	public static final ObjectWriter OBJ_WRITER = OBJ_MAPPER.writer(new DefaultPrettyPrinter());
	public static final JsonNodeFactory JSON_NODE_FACTORY = OBJ_MAPPER.getNodeFactory();
	public static int tempId=1;
	public static final StringBuilder QUERY_PREFIX = new StringBuilder();
	public static final StringBuilder BROADER_QUERY_STR = new StringBuilder();
	public static final StringBuilder SUBJ_QUERY_STR = new StringBuilder();

	public static final StringBuilder CATEGORY_QUERY_PRT1 = new StringBuilder();
	public static final StringBuilder METHOD_QUERY_PRT1 = new StringBuilder();
	public static final StringBuilder METHOD_QUERY_PRT2 = new StringBuilder();

	public static final StringBuilder WHERE_PRT = new StringBuilder();
	public static final StringBuilder UNION_PRT = new StringBuilder();
	public static final StringBuilder END_PRT = new StringBuilder();

	public static final String BROADER_PARAM_STR = " { ?c skos:broader <%s> . } ";
	public static final String SUBJ_PARAM_STR = " { ?m dct:subject <%s> . } ";
	public static final String CATEG_FILTER_STR = " FILTER(?c=<%s>) . ";
	
	static {
		QUERY_PREFIX.append("PREFIX dbo: <http://dbpedia.org/ontology/> ");
		QUERY_PREFIX.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns/> ");
		QUERY_PREFIX.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		QUERY_PREFIX.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ");
		QUERY_PREFIX.append("PREFIX dct: <http://purl.org/dc/terms/> ");

		BROADER_QUERY_STR.append(QUERY_PREFIX);
		BROADER_QUERY_STR.append("SELECT distinct ?m ");
		BROADER_QUERY_STR.append(" WHERE { ");
		BROADER_QUERY_STR.append(" ?m skos:broader <%s> . ");
		BROADER_QUERY_STR.append(" } ");

		SUBJ_QUERY_STR.append(QUERY_PREFIX);
		SUBJ_QUERY_STR.append("SELECT DISTINCT ?s (str(?lbl) as ?label) (str(?about) as ?abs) ");
		SUBJ_QUERY_STR.append(" WHERE { ");
		SUBJ_QUERY_STR.append(" ?s dct:subject <%s> . ");
		SUBJ_QUERY_STR.append(" ?s rdfs:label ?lbl .");
		SUBJ_QUERY_STR.append(" ?s dbo:abstract ?about . ");
		SUBJ_QUERY_STR.append(" FILTER langMatches(lang(?about) , 'en') . ");
		SUBJ_QUERY_STR.append(" FILTER langMatches(lang(?lbl) , 'en') . ");
		SUBJ_QUERY_STR.append(" } ");
		
		WHERE_PRT.append(" WHERE { ");
		UNION_PRT.append(" UNION ");
		END_PRT.append(" } ");

		CATEGORY_QUERY_PRT1.append("SELECT distinct ?c ");

		METHOD_QUERY_PRT1.append("SELECT DISTINCT ?c ?m (str(?lbl) as ?label) (str(?about) as ?abs) ");

		METHOD_QUERY_PRT2.append(" ?m dct:subject ?c . ");
		METHOD_QUERY_PRT2.append(" ?m rdfs:label ?lbl .");
		METHOD_QUERY_PRT2.append(" ?m dbo:abstract ?about . ");
		METHOD_QUERY_PRT2.append(" FILTER langMatches(lang(?about) , 'en') . ");
		METHOD_QUERY_PRT2.append(" FILTER langMatches(lang(?lbl) , 'en') . ");
	}
	
	
	

	public static void main(String[] args) {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		try {
			process(inputFile, outputFile);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void process(File inputFile, File outputFile) throws JsonGenerationException, JsonMappingException, IOException {
		LinkedList<String> linkedList = new LinkedList<>();
		// Load all categories from file
		linkedList.addAll(Files.readAllLines(inputFile.toPath()));
		
		Map<String, List<Methodology>> resMap = new HashMap<String, List<Methodology>>();
		while (linkedList.size() > 0) {
			System.out.println("Current Queue size: "+linkedList.size());
			String category = linkedList.poll();
			System.out.println("Current Category: "+category);
			// generate query to fetch methods
			String methodQuery = genMethodsQuery(category);
			fetchAllMethods(executeSparql(methodQuery), resMap);
			List<Methodology> methodList = resMap.get(category);
			System.out.println("Current Category's resources size: "+(methodList==null?0:methodList.size()));
		}
		
		System.out.println("Final ResMap Category Size: "+resMap.size());
		System.out.println("Final ResMap Values Size: "+getValuesSize(resMap.values()));
		// get json node of map
		JsonNode jsonNode = generateJsonNode(resMap);
		writeJsonToFile(jsonNode, outputFile);
	}
	
	public static String genMethodsQuery(String category) {

		StringBuilder methodsQuery = new StringBuilder();

		methodsQuery.append(QUERY_PREFIX);
		methodsQuery.append(METHOD_QUERY_PRT1);
		methodsQuery.append(WHERE_PRT);
		methodsQuery.append(String.format(SUBJ_PARAM_STR, category));
		methodsQuery.append(METHOD_QUERY_PRT2);
		methodsQuery.append(String.format(CATEG_FILTER_STR, category));
		methodsQuery.append(END_PRT);

		return methodsQuery.toString();

	}
	
	public static List<QuerySolution> executeSparql(String queryStr) {
		ResultSet res = null;
		List<QuerySolution> querySolutionList = new ArrayList<>();
		Query query = QueryFactory.create(queryStr.toString(), Syntax.syntaxARQ);
		// Remote execution.
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
			// Set the DBpedia specific timeout.
			((QueryEngineHTTP) qexec).addParam("timeout", "10000");
			// Execute.
			res = qexec.execSelect();
			while(res.hasNext()) {
				querySolutionList.add(res.next());
			}
		} catch (Exception e) {
			System.out.println("Query Failed: "+query);
			e.printStackTrace();
		}
		// sleep
		sleep(1000);
		return querySolutionList;
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static JsonNode generateJsonNode(Map<String, List<Methodology>> resMap) {
		JsonNode jsonNode = OBJ_MAPPER.valueToTree(resMap);
		return jsonNode;
	}

	/**
	 * Method to write a json object to a file
	 * 
	 * @param node       - json object to be written
	 * @param outputFile - file to write json in
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void writeJsonToFile(JsonNode node, File outputFile)
			throws JsonGenerationException, JsonMappingException, IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		OBJ_WRITER.writeValue(outputFile, node);
	}
	
	public static List<Methodology> fetchAllMethods(List<QuerySolution> res) {
		List<Methodology> resList = new ArrayList<>();
		for (QuerySolution entry : res) {
			String uri = entry.get("s").toString();
			String label = entry.get("label").toString();
			String abs = entry.get("abs").toString();
			Methodology method = new Methodology(tempId++, uri, label, abs);
			resList.add(method);
		}
		return resList;
	}
	
	public static <T> int getValuesSize(Collection<List<T>> col) {
		int count = 0;
		for(Collection<T> inCol : col) {
			count+=inCol.size();
		}
		return count;
	}
	
	public static void fetchAllMethods(List<QuerySolution> res, Map<String, List<Methodology>> resMap) {

		for (QuerySolution entry : res) {
			String uri = entry.get("m").toString();
			String label = entry.get("label").toString();
			String abs = entry.get("abs").toString();
			String category = entry.get("c").toString();
			Methodology method = new Methodology(tempId++, uri, label, abs);
			List<Methodology> resList = resMap.get(category);
			if (resList == null) {
				resList = new ArrayList<>();
				resMap.put(category, resList);
			}
			resList.add(method);
		}
	}

}
