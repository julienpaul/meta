package se.lu.nateko.cp.meta.test;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.ontotext.graphdb.example.util.EmbeddedGraphDB;

public class GraphDbDeadlockTest {

	public static void main(String[] args)
		throws Exception
	{
		EmbeddedGraphDB db = new EmbeddedGraphDB("/home/oleg/workspace/meta/rdfStorage");
		String repoId = "icoscp";

		if (!db.hasRepository(repoId))
			db.createRepository(repoId);

		Repository repo = db.getRepository(repoId);
		try {
			ValueFactory f = repo.getValueFactory();
			Resource subj = f.createIRI("http://www.example.com/");
			Statement s = f.createStatement(subj, RDFS.LABEL, f.createLiteral("label"));

			RepositoryConnection conn = repo.getConnection();
			System.out.println("hasStatement #1:");
			conn.hasStatement(s, false, subj);
			conn.close();

			conn = repo.getConnection();
			System.out.println("hasStatement #2:");
			conn.hasStatement(s, false, subj);
			conn.close();

			conn = repo.getConnection();
			System.out.println("hasStatement #3:");
			conn.hasStatement(s, false, subj);
			conn.close();

			System.out.println("all done!");
		}
		finally {
			repo.shutDown();
			db.close();
		}
	}
}
