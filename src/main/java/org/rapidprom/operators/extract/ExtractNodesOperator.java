package org.rapidprom.operators.extract;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.weka.WekaClassifier;
import com.rapidminer.operator.ports.InputPort;
import weka.classifiers.trees.J48;
import weka8.classifiers.Classifier;

public class ExtractNodesOperator extends Operator{
	
	/** defining the ports */
	private InputPort modelInput = getInputPorts().createPort("Tree model");

	public ExtractNodesOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doWork() throws OperatorException {
		
		IOObject object = modelInput.getAnyDataOrNull();
		WekaClassifier wekaObject =  (WekaClassifier) object;
		
		
	}
	
}
