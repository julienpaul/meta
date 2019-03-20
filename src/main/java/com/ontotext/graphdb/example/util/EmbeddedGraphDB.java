package com.ontotext.graphdb.example.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import com.ontotext.trree.config.OWLIMSailSchema;

/**
 * A useful class for creating a local (embedded) GraphDB database (no networking needed).
 */
public class EmbeddedGraphDB implements Closeable {

	private LocalRepositoryManager repositoryManager;

	/**
	 * Creates a new embedded instance of GraphDB in the provided directory.
	 *
	 * @param baseDir
	 *        a directory where to store repositories
	 * @throws RepositoryException
	 */
	public EmbeddedGraphDB(String baseDir)
		throws RepositoryException
	{
		repositoryManager = new LocalRepositoryManager(new File(baseDir));
		repositoryManager.initialize();
	}

	/**
	 * Creates a repository with the given ID.
	 *
	 * @param repositoryId
	 *        a new repository ID
	 * @throws RDFHandlerException
	 * @throws RepositoryConfigException
	 * @throws RDFParseException
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public void createRepository(String repositoryId)
		throws RDFHandlerException,
		RepositoryConfigException,
		RDFParseException,
		IOException,
		RepositoryException
	{
		createRepository(repositoryId, null, null);
	}

	/**
	 * Creates a repository with the given ID, label and optional override parameters.
	 *
	 * @param repositoryId
	 *        a new repository ID
	 * @param repositoryLabel
	 *        a repository label, or null if none should be set
	 * @param overrides
	 *        a map of repository creation parameters that override the defaults, or null if none should be
	 *        overridden
	 * @throws RDFParseException
	 * @throws IOException
	 * @throws RDFHandlerException
	 * @throws RepositoryConfigException
	 * @throws RepositoryException
	 */
	public void createRepository(String repositoryId, String repositoryLabel, Map<String, String> overrides)
		throws RDFParseException,
		IOException,
		RDFHandlerException,
		RepositoryConfigException,
		RepositoryException
	{
		if (repositoryManager.hasRepositoryConfig(repositoryId)) {
			throw new RuntimeException("Repository " + repositoryId + " already exists.");
		}

		TreeModel graph = new TreeModel();

		InputStream config = EmbeddedGraphDB.class.getResourceAsStream("/repo-defaults.ttl");
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(config, RepositoryConfigSchema.NAMESPACE);
		config.close();

		Resource repositoryNode = Models.subject(
				graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);

		graph.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID,
				SimpleValueFactory.getInstance().createLiteral(repositoryId));

		if (repositoryLabel != null) {
			graph.add(repositoryNode, RDFS.LABEL,
					SimpleValueFactory.getInstance().createLiteral(repositoryLabel));
		}

		if (overrides != null) {
			Resource configNode = (Resource)Models.object(
					graph.filter(null, SailRepositorySchema.SAILIMPL, null)).orElse(null);
			for (Map.Entry<String, String> e : overrides.entrySet()) {
				IRI key = SimpleValueFactory.getInstance().createIRI(OWLIMSailSchema.NAMESPACE + e.getKey());
				Literal value = SimpleValueFactory.getInstance().createLiteral(e.getValue());
				graph.remove(configNode, key, null);
				graph.add(configNode, key, value);
			}
		}

		RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);

		repositoryManager.addRepositoryConfig(repositoryConfig);
	}

	public Repository getRepository(String repositoryId)
		throws RepositoryException,
		RepositoryConfigException
	{
		return repositoryManager.getRepository(repositoryId);
	}

	public boolean hasRepository(String repositoryId) {
		return repositoryManager.hasRepositoryConfig(repositoryId);
	}

	@Override
	public void close()
		throws IOException
	{
		repositoryManager.shutDown();
	}

}
