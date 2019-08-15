package hwu.elixir.dataAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import virtuoso.rdf4j.driver.VirtuosoRepository;

public class TripleStoreWrapper {

	private RepositoryConnection con;
	private VirtuosoRepository repository;

	private static Logger logger = LoggerFactory.getLogger("hwu.elixir.dataAccess");

	/** 
	 * Read connection details from database.properties and opens a connection to a virtuoso repository
	 */
	public TripleStoreWrapper() {
		ClassLoader classLoader = getClass().getClassLoader();

		URL resource = classLoader.getResource("database.properties");
		if (resource == null) {
			logger.error("Cannot find database.properties file");
			throw new IllegalArgumentException("file is not found!");
		}
		FileInputStream in = null;
		try {
			in = new FileInputStream(new File(resource.getFile()));
		} catch (FileNotFoundException e) {
			logger.error("Cannot read database.properties file", e);
			System.exit(0);
		}
		Properties prop = new Properties();

		try {
			prop.load(in);
		} catch (IOException e) {
			logger.error("Cannot load database.properties.", e);
			System.exit(0);
		}

		String jndiname = prop.getProperty("jndiname");
		DataSource dataSource;
		try {
			dataSource = (DataSource) new InitialContext().lookup("java:comp/env/" + jndiname);
		} catch (NamingException e) {			
			throw new IllegalStateException(jndiname + " is missing in JNDI!", e);
		}

		repository = new VirtuosoRepository(dataSource, "https://bioschemas.org/crawl/v1/", false);
	}

	/** 
	 * Open the connection
	 */
	private void openConnection() {
		con = repository.getConnection();
	}

	/**
	 * Close the connection
	 */
	private void closeConnection() {
		con.close();		
		con = null;
	}

	/**
	 * @deprecated
	 * 
	 */
	public void addModel(Model model) {
		if (con == null)
			openConnection();
		con.add(model);
		closeConnection();
	}

	/** Helper method to simplify running a query and obtaining a result
	 * 
	 * @param sparql The query (not update) to execute
	 * @return The result of the query
	 * @see TupleQuery
	 */
	public TupleQueryResult query(String sparql) {
		if (con == null)
			openConnection();

		TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
		TupleQueryResult result = tupleQuery.evaluate();	

		return result;
	}
}
