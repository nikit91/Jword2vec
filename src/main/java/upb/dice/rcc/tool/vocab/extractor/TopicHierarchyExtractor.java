package upb.dice.rcc.tool.vocab.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class TopicHierarchyExtractor {

	// Query headers
	public static final StringBuilder QUERY_PREFIX = new StringBuilder();
	// Parent node query
	public static final StringBuilder PARENT_NODE_QUERY = new StringBuilder();
	// Children nodes query
	public static final StringBuilder CHILDREN_NODE_QUERY = new StringBuilder();

	static {
		QUERY_PREFIX.append("PREFIX dbo: <http://dbpedia.org/ontology/> ");
		QUERY_PREFIX.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns/> ");
		QUERY_PREFIX.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		QUERY_PREFIX.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ");
		QUERY_PREFIX.append("PREFIX mads: <http://www.loc.gov/mads/rdf/v1#>");

		PARENT_NODE_QUERY.append(QUERY_PREFIX);
		PARENT_NODE_QUERY.append(" SELECT ?s (str(?lbl) as ?label) (str(?note) as ?desc)");
		PARENT_NODE_QUERY.append(" WHERE { ");
		PARENT_NODE_QUERY.append(" ?s mads:authoritativeLabel ?lbl . ");
		PARENT_NODE_QUERY.append(" OPTIONAL { ");
		PARENT_NODE_QUERY.append(" ?s mads:hasSource ?src . ");
		PARENT_NODE_QUERY.append(" ?src mads:citation-note ?note . ");
		PARENT_NODE_QUERY.append(" ?src mads:citation-source ?citsrc . ");
		PARENT_NODE_QUERY.append(" FILTER regex(?citsrc, \".*wikipedia.*\", \"i\") . ");
		PARENT_NODE_QUERY.append(" } ");
		PARENT_NODE_QUERY.append(" FILTER NOT EXISTS { ");
		PARENT_NODE_QUERY.append("  ?s mads:hasBroaderAuthority ?ba . ");
		PARENT_NODE_QUERY.append(" } ");
		PARENT_NODE_QUERY.append(" } ");

		CHILDREN_NODE_QUERY.append(QUERY_PREFIX);
		CHILDREN_NODE_QUERY.append(" SELECT ?s (str(?lbl) as ?label) (str(?note) as ?desc)");
		CHILDREN_NODE_QUERY.append(" WHERE { ");
		CHILDREN_NODE_QUERY.append(" ?s mads:authoritativeLabel ?lbl . ");
		CHILDREN_NODE_QUERY.append(" OPTIONAL { ");
		CHILDREN_NODE_QUERY.append(" ?s mads:hasSource ?src . ");
		CHILDREN_NODE_QUERY.append(" ?s mads:hasBroaderAuthority <%s> . ");
		CHILDREN_NODE_QUERY.append(" ?src mads:citation-note ?note . ");
		CHILDREN_NODE_QUERY.append(" ?src mads:citation-source ?citsrc . ");
		CHILDREN_NODE_QUERY.append(" FILTER regex(?citsrc, \".*wikipedia.*\", \"i\") . ");
		CHILDREN_NODE_QUERY.append(" } ");
		CHILDREN_NODE_QUERY.append(" } ");

	}

	// Method to find all parent nodes
	protected Set<TopicNode> fetchAllRootTopics() {
		Set<TopicNode> rootList = new HashSet<>();
		List<QuerySolution> querySolList = executeSparql(PARENT_NODE_QUERY.toString());
		for (QuerySolution sol : querySolList) {
			String resourceUri = sol.get("?s").toString();
			String label = sol.get("?label").toString();
			//String description = sol.get("?desc").toString();
			TopicNode curNode = new TopicNode(resourceUri, label, null, null);
			rootList.add(curNode);
		}
		return rootList;
	}

	// Method to find all children nodes
	protected void findLinkChildNodes(TopicNode parentNode) {
		String childrenQuery = String.format(CHILDREN_NODE_QUERY.toString(), parentNode.getResourceUri());
		List<QuerySolution> querySolList = executeSparql(childrenQuery);
		for (QuerySolution sol : querySolList) {
			String resourceUri = sol.get("?s").toString();
			String label = sol.get("?label").toString();
			//String description = sol.get("?desc").toString();
			TopicNode curNode = new TopicNode(resourceUri, label, null, parentNode);
			parentNode.addChildren(curNode);
		}
	}

	// Method to execute queries
	public static List<QuerySolution> executeSparql(String queryStr) {
		ResultSet res = null;
		List<QuerySolution> querySolutionList = new ArrayList<>();
		Query query = QueryFactory.create(queryStr.toString());
		// Remote execution.
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:3030/topic/sparql", query)) {
			// Set the DBpedia specific timeout.
			((QueryEngineHTTP) qexec).addParam("timeout", "10000");
			// Execute.
			res = qexec.execSelect();
			while (res.hasNext()) {
				querySolutionList.add(res.next());
			}
		} catch (Exception e) {
			System.out.println("Query Failed: " + queryStr);
			e.printStackTrace();
		}
		// sleep
		// sleep(1000);
		return querySolutionList;
	}

	// Method to write tree to a file
	protected void writeListToFile(Collection<TopicNode> rootCol, File outFile)
			throws JsonGenerationException, JsonMappingException, IOException {
		// ensure directory creation
		outFile.getParentFile().mkdirs();
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		writer.writeValue(outFile, rootCol);
	}

	protected void process(File outFile) {
		Set<TopicNode> rootSet = fetchAllRootTopics();
		LinkedList<TopicNode> linkedList = new LinkedList<>();
		linkedList.addAll(rootSet);
		while (linkedList.size() > 0) {
			TopicNode curNode = linkedList.poll();
			findLinkChildNodes(curNode);
			rootSet.addAll(curNode.getChildrenTopics());
		}
		try {
			writeListToFile(rootSet, outFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		File outputFile = new File(args[0]);
		TopicHierarchyExtractor extractor = new TopicHierarchyExtractor();
		extractor.process(outputFile);
	}

}
