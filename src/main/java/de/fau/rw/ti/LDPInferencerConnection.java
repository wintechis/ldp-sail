package de.fau.rw.ti;

import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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

        CloseableIteration<? extends Statement, SailException> directContainers = getWrappedConnection().getStatements(null, RDF.TYPE, LDP.DIRECT_CONTAINER, false);
        while(directContainers.hasNext()) {
            IRI dc = (IRI) directContainers.next().getSubject();

            CloseableIteration<? extends Statement, SailException> membershipResources = getWrappedConnection().getStatements(dc, LDP.MEMBERSHIP_RESOURCE, null, false);
            if(!membershipResources.hasNext()) {
                continue;
            }
            Statement membershipResourceStatement = membershipResources.next();
            Resource membershipResource = (Resource) membershipResourceStatement.getObject();
            Resource membershipResourceContext = (Resource) membershipResourceStatement.getContext();
            membershipResources.close();

            CloseableIteration<? extends Statement, SailException> memberRelations = getWrappedConnection().getStatements(dc, LDP.HAS_MEMBER_RELATION, null, false);
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();
            memberRelations.close();

            CloseableIteration<? extends Statement, SailException> containedResources = getWrappedConnection().getStatements(dc, LDP.CONTAINS, null, false);
            while(containedResources.hasNext()) {
                boolean added = addInferredStatement(membershipResource, memberRelation, containedResources.next().getObject(), membershipResourceContext);

                if (added) {
                    nofInferred++;
                }
            }
            containedResources.close();
        }
        directContainers.close();

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

        CloseableIteration<? extends Statement, SailException> indirectContainers = getWrappedConnection().getStatements(null, RDF.TYPE, LDP.INDIRECT_CONTAINER, false);
        while(indirectContainers.hasNext()) {
            IRI dc = (IRI) indirectContainers.next().getSubject();

            CloseableIteration<? extends Statement, SailException> membershipResources = getWrappedConnection().getStatements(dc, LDP.MEMBERSHIP_RESOURCE, null, false);
            if(!membershipResources.hasNext()) {
                continue;
            }
            Statement membershipResourceStatement = membershipResources.next();
            Resource membershipResource = (Resource) membershipResourceStatement.getObject();
            Resource membershipResourceContext = (Resource) membershipResourceStatement.getContext();
            membershipResources.close();

            CloseableIteration<? extends Statement, SailException> memberRelations = getWrappedConnection().getStatements(dc, LDP.HAS_MEMBER_RELATION, null, false);
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();
            memberRelations.close();

            CloseableIteration<? extends Statement, SailException> insertedContentRelations = getWrappedConnection().getStatements(dc, LDP.INSERTED_CONTENT_RELATION, null, false);
            if(!insertedContentRelations.hasNext()) {
                continue;
            }
            IRI insertedContentRelation = (IRI) insertedContentRelations.next().getObject();
            insertedContentRelations.close();

            CloseableIteration<? extends Statement, SailException> containedResources = getWrappedConnection().getStatements(dc, LDP.CONTAINS, null, false);
            while(containedResources.hasNext()) {
                CloseableIteration<? extends Statement, SailException> values = getWrappedConnection().getStatements((Resource) containedResources.next().getObject(), insertedContentRelation, null, false);
                while(values.hasNext()) {
                    Value v = values.next().getObject();
                    boolean added = addInferredStatement(membershipResource, memberRelation, v, membershipResourceContext);

                    if (added) {
                        nofInferred++;
                    }
                }
                values.close();
            }
            containedResources.close();
        }
        indirectContainers.close();

		return nofInferred;
	}
    
}
