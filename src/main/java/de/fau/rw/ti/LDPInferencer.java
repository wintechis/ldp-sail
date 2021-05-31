package de.fau.rw.ti;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencer;

public class LDPInferencer extends AbstractForwardChainingInferencer {

	public LDPInferencer() {
		super();
	}

	public LDPInferencer(NotifyingSail baseSail) {
		super(baseSail);
	}

	@Override
	public LDPInferencerConnection getConnection() throws SailException {
		try {
			InferencerConnection con = (InferencerConnection) super.getConnection();
			return new LDPInferencerConnection(this, con);
		} catch (ClassCastException e) {
			throw new SailException(e.getMessage(), e);
		}
	}
    
}
