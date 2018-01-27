package org.rapidprom.operators.analysis;

import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.ioobjects.ResultReplayIOObject;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.InputPort;

public class RepairLogOperator extends Operator {
	
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM ResultReplay)", ResultReplayIOObject.class);
	private InputPort modelInput = getInputPorts().createPort("model (DataPetriNet)", PetriNetIOObject.class);

	public RepairLogOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}
	
	

}
