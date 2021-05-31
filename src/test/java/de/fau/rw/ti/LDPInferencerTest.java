package de.fau.rw.ti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class LDPInferencerTest {
    public ValueFactory factory = SimpleValueFactory.getInstance();

    @Test
    public void testRuleLdpDirect1() throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new LDPInferencer(new MemoryStore()));
        RepositoryConnection conn = repo.getConnection();
        conn.add(getFile("ldp_direct.trig"));

        assertTrue(conn.getStatements(
            factory.createIRI("http://example.org/directContainer#a"), 
            factory.createIRI("http://example.org/value"), 
            factory.createIRI("http://example.org/directContainer/resourceA"),
            true,
            factory.createIRI("http://example.org/directContainer#a")
        ).hasNext());
    }

    @Test
    @SuppressWarnings("unused")
    public void testRuleLdpDirect1NotMore() throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new LDPInferencer(new MemoryStore()));
        RepositoryConnection conn = repo.getConnection();
        conn.add(getFile("ldp_direct.trig"));

        int size = 0;
        Iterable<Statement> statements = conn.getStatements(null, null, null, true);
        for(Statement s : statements) {
            size++;
        }

        assertEquals(8, size);
    }

    @Test
    public void testRuleLdpIndirect1() throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new LDPInferencer(new MemoryStore()));
        RepositoryConnection conn = repo.getConnection();
        conn.add(getFile("ldp_indirect.trig"));

        assertTrue(conn.getStatements(
            factory.createIRI("http://example.org/indirectContainer#b"), 
            factory.createIRI("http://example.org/value"), 
            factory.createLiteral("4", XSD.INTEGER),
            true,
            factory.createIRI("http://example.org/indirectContainer#b")
        ).hasNext());
    }

    @Test
    @SuppressWarnings("unused")
    public void testRuleLdpIndirect1NotMore() throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new LDPInferencer(new MemoryStore()));
        RepositoryConnection conn = repo.getConnection();
        conn.add(getFile("ldp_indirect.trig"));

        int size = 0;
        Iterable<Statement> statements = conn.getStatements(null, null, null, true);
        for(Statement s : statements) {
            size++;
        }

        assertEquals(9, size);
    }

    @Test
    public void testLdpDirectAddedLater() throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new LDPInferencer(new MemoryStore()));
        RepositoryConnection conn = repo.getConnection();
        conn.add(getFile("ldp_direct.trig"));

        IRI newResource = factory.createIRI("htt://example.org/directContainer/resourceC");
        IRI directContainer = factory.createIRI("http://example.org/directContainer");
        conn.add(newResource, RDF.TYPE, LDP.RESOURCE, newResource);
        conn.add(directContainer, LDP.CONTAINS, newResource, directContainer);

        assertTrue(conn.getStatements(
            factory.createIRI("http://example.org/directContainer#a"), 
            factory.createIRI("http://example.org/value"), 
            newResource,
            true,
            factory.createIRI("http://example.org/directContainer#a")
        ).hasNext());
    }

    private File getFile(String name) {
        ClassLoader classLoader = LDPInferencerTest.class.getClassLoader();
        return new File(classLoader.getResource("resources/" + name).getFile());
    }
}
