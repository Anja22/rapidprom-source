package org.rapidprom.operators.extract;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.weka.WekaClassifier;
import com.rapidminer.operator.ports.InputPort;

import weka.classifiers.Classifier;
//import weka.classifiers.trees.J48;

//import weka.classifiers.Classifier;

//import weka.classifiers.trees.J48;


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
		Classifier tree = wekaObject.getClassifier();
		boolean isCorrect =false;
//		if(wekaObject.getClassifier() instanceof J48)
//		{
//			isCorrect = true;
//		}
		Object clsfr = wekaObject.getClassifier();
		Object clas = clsfr.getClass();
		
//		wekaObject.
//		wekaObject.
//		J48 tree = (J48) wekaObject;
	}
	
}
