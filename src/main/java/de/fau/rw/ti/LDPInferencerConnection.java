package de.fau.rw.ti;

import java.util.Iterator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencerConnection;

public class LDPInferencerConnection extends AbstractForwardChainingInferencerConnection {

    public LDPInferencerConnection(Sail sail, InferencerConnection con) {
        super(sail, con);
    }

    @Override
    protected Model createModel() {
		return new DynamicModelFactory().createEmptyModel();
    }

    @Override
    protected void addAxiomStatements() throws SailException {
    }

    @Override
    protected int applyRules(Model iteration) throws SailException {
        int nofInferred = 0;
        
        nofInferred += applyRuleLdpDirect1(iteration);
        nofInferred += applyRuleLdpIndirect1(iteration);

        return nofInferred;
    }

    // aaa rdf:type ldp:DirectContainer ;
    //     ldp:contains rrr ;
    //     ldp:membershipResource sss ;
    //     ldp:hasMemberRelation ppp .     
    // --> sss ppp rrr .
	private int applyRuleLdpDirect1(Model iteration) throws SailException {
		int nofInferred = 0;

        Iterable<Statement> directContainers = iteration.getStatements(null, RDF.TYPE, LDP.DIRECT_CONTAINER);
        for(Statement directContainer : directContainers) {
            IRI dc = (IRI) directContainer.getSubject();

            Iterator<Statement> membershipResources = iteration.getStatements(dc, LDP.MEMBERSHIP_RESOURCE, null).iterator();
            if(!membershipResources.hasNext()) {
                continue;
            }
            Statement membershipResourceStatement = membershipResources.next();
            Resource membershipResource = (Resource) membershipResourceStatement.getObject();
            Resource membershipResourceContext = (Resource) membershipResourceStatement.getContext();

            Iterator<Statement> memberRelations = iteration.getStatements(dc, LDP.HAS_MEMBER_RELATION, null).iterator();
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();

            Iterable<Statement> containedResources = iteration.getStatements(dc, LDP.CONTAINS, null);
            for(Statement containedResource : containedResources) {
                boolean added = addInferredStatement(membershipResource, memberRelation, containedResource.getObject(), membershipResourceContext);

                if (added) {
                    nofInferred++;
                }
            }
        }

		return nofInferred;
	}

    // aaa rdf:type ldp:IndirectContainer ;
    //     ldp:contains rrr ;
    //     ldp:membershipResource sss ;
    //     ldp:hasMemberRelation ppp ;
    //     ldp:insertedContentRelation iii .
    // rrr iii vvv
    // --> sss ppp vvv .
	private int applyRuleLdpIndirect1(Model iteration) throws SailException {
		int nofInferred = 0;

        Iterable<Statement> indirectContainers = iteration.getStatements(null, RDF.TYPE, LDP.INDIRECT_CONTAINER);
        for(Statement indirectContainer : indirectContainers) {
            IRI dc = (IRI) indirectContainer.getSubject();

            Iterator<Statement> membershipResources = iteration.getStatements(dc, LDP.MEMBERSHIP_RESOURCE, null).iterator();
            if(!membershipResources.hasNext()) {
                continue;
            }
            Statement membershipResourceStatement = membershipResources.next();
            Resource membershipResource = (Resource) membershipResourceStatement.getObject();
            Resource membershipResourceContext = (Resource) membershipResourceStatement.getContext();

            Iterator<Statement> memberRelations = iteration.getStatements(dc, LDP.HAS_MEMBER_RELATION, null).iterator();
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();

            Iterator<Statement> insertedContentRelations = iteration.getStatements(dc, LDP.INSERTED_CONTENT_RELATION, null).iterator();
            if(!insertedContentRelations.hasNext()) {
                continue;
            }
            IRI insertedContentRelation = (IRI) insertedContentRelations.next().getObject();

            Iterable<Statement> containedResources = iteration.getStatements(dc, LDP.CONTAINS, null);
            for(Statement containedResource : containedResources) {
                Iterable<Statement> values = iteration.getStatements((Resource) containedResource.getObject(), insertedContentRelation, null);
                for(Statement value : values) {
                    Value v = value.getObject();
                    boolean added = addInferredStatement(membershipResource, memberRelation, v, membershipResourceContext);

                    if (added) {
                        nofInferred++;
                    }
                }
            }
        }

		return nofInferred;
	}
    
}
