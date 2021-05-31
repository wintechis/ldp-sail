package de.fau.rw.ti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
            factory.createIRI("http://example.org/directContainer")
        ).hasNext());
    }

    @Test
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
            factory.createIRI("http://example.org/indirectContainer")
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

    private File getFile(String name) {
        ClassLoader classLoader = LDPInferencerTest.class.getClassLoader();
        return new File(classLoader.getResource("resources/" + name).getFile());
    }
}
