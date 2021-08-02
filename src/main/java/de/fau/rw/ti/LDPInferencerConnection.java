package de.fau.rw.ti;

import java.math.BigDecimal;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencerConnection;

public class LDPInferencerConnection extends AbstractForwardChainingInferencerConnection {

    public static final IRI LDP_ORDERED_CONTAINER = SimpleValueFactory.getInstance().createIRI("https://solid.ti.rw.fau.de/public/ns/linked-data-structures#", "OrderedContainer");
    public static final IRI LDP_ORDER_CONTENT_RELATION = SimpleValueFactory.getInstance().createIRI("https://solid.ti.rw.fau.de/public/ns/linked-data-structures#", "orderContentRelation");

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
        nofInferred += applyRuleLdpOrdered1(iteration);

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
            membershipResources.close();

            CloseableIteration<? extends Statement, SailException> memberRelations = getWrappedConnection().getStatements(dc, LDP.HAS_MEMBER_RELATION, null, false);
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();
            memberRelations.close();

            CloseableIteration<? extends Statement, SailException> containedResources = getWrappedConnection().getStatements(dc, LDP.CONTAINS, null, false);
            while(containedResources.hasNext()) {
                boolean added = addInferredStatement(membershipResource, memberRelation, containedResources.next().getObject(), membershipResource);

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
    // rrr iii vvv .
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
                    boolean added = addInferredStatement(membershipResource, memberRelation, v, membershipResource);

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

    // aaa rdf:type ldp:IndirectContainer ;
    //     ldp:contains rrr, qqq ;
    //     ldp:membershipResource sss ;
    //     ldp:hasMemberRelation ppp ;
    //     ldp:orderContentRelation iii .
    // rrr iii vvv .
    // qqq iii uuu .
    // --> sss ppp rrr . iff vvv <= uuu
    // --> sss ppp qqq . else
	private int applyRuleLdpOrdered1(Model iteration) throws SailException {
		int nofInferred = 0;

        CloseableIteration<? extends Statement, SailException> orderedContainers = getWrappedConnection().getStatements(null, RDF.TYPE, LDP_ORDERED_CONTAINER, false);
        while(orderedContainers.hasNext()) {
            BigDecimal lowestValue = null;
            Resource lowestResource = null;

            IRI dc = (IRI) orderedContainers.next().getSubject();

            CloseableIteration<? extends Statement, SailException> membershipResources = getWrappedConnection().getStatements(dc, LDP.MEMBERSHIP_RESOURCE, null, false);
            if(!membershipResources.hasNext()) {
                continue;
            }
            Statement membershipResourceStatement = membershipResources.next();
            Resource membershipResource = (Resource) membershipResourceStatement.getObject();
            membershipResources.close();

            CloseableIteration<? extends Statement, SailException> memberRelations = getWrappedConnection().getStatements(dc, LDP.HAS_MEMBER_RELATION, null, false);
            if(!memberRelations.hasNext()) {
                continue;
            }
            IRI memberRelation = (IRI) memberRelations.next().getObject();
            memberRelations.close();

            CloseableIteration<? extends Statement, SailException> orderContentRelations = getWrappedConnection().getStatements(dc, LDP_ORDER_CONTENT_RELATION, null, false);
            if(!orderContentRelations.hasNext()) {
                continue;
            }
            IRI orderContentRelation = (IRI) orderContentRelations.next().getObject();
            orderContentRelations.close();

            CloseableIteration<? extends Statement, SailException> containedResources = getWrappedConnection().getStatements(dc, LDP.CONTAINS, null, false);
            while(containedResources.hasNext()) {
                Resource containedResource = (Resource) containedResources.next().getObject();
                CloseableIteration<? extends Statement, SailException> values = getWrappedConnection().getStatements(containedResource, orderContentRelation, null, false);
                if(values.hasNext()) {
                    Value v = values.next().getObject();

                    if(v.isLiteral()) {
                        Literal vl = (Literal) v;
                        BigDecimal dvl = vl.decimalValue();
                        if(lowestValue == null || lowestValue.compareTo(dvl) > 0) {
                            lowestResource = containedResource;
                            lowestValue = dvl;
                        }
                    }
                }
                values.close();
            }
            containedResources.close();

            if(lowestResource != null) {
                boolean added = addInferredStatement(membershipResource, memberRelation, lowestResource, membershipResource);

                if (added) {
                    nofInferred++;
                }
            }
        }
        orderedContainers.close();

		return nofInferred;
	}
    
}
