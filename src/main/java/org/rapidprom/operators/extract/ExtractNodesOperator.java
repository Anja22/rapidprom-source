package org.rapidprom.operators.extract;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.weka.WekaClassifier;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WekaTools;

import weka.classifiers.trees.J48;

import org.processmining.datadiscovery.estimators.weka.WekaTreeClassificationAdapter;
import org.processmining.datadiscovery.estimators.weka.WekaTreeClassificationAdapter.WekaLeafNode;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.util.Pair;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.parameter.ParameterTypeLabelValues;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.datadiscovery.estimators.Type;



public class ExtractNodesOperator extends Operator{
	
	/** defining the ports */
	private InputPort modelInput = getInputPorts().createPort("model (W-J48 Tree)");
	private InputPort exampleSetInput = getInputPorts().createPort("example set (Training set for the model)", ExampleSet.class);
	private InputPort eventlogInput = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

	private OutputPort passthroughTreeModel = getOutputPorts().createPort("model (W-J48 Tree)");
	
	private static final String REMAINING = "Remaining Instances";
	private static final String EXCLUDE_WRONG = "Exclude wrongly classified instances";
	private static final String PARAMETER_ATTRIBUTES = "attributes";
	
	public ExtractNodesOperator(OperatorDescription description) {
		super(description);
//		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput,));		
		getTransformer().addRule(new GenerateNewMDRule(passthroughTreeModel, WekaClassifier.class));
	}

	@Override
	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: Extract Nodes");
		long time = System.currentTimeMillis();
		
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		XLogIOObject xlogIO = eventlogInput.getData(XLogIOObject.class);
		XLog log = xlogIO.getArtifact();
		boolean onlyCorrectlyClassified = getParameterAsBoolean(EXCLUDE_WRONG);
		
		IOObject object = modelInput.getAnyDataOrNull();
		WekaClassifier wekaObject =  (WekaClassifier) object;
		J48 tree = (J48) wekaObject.getClassifier();
		
//		List<Pair<String,GuardExpression>> listExpressions = getExpressionsAtLeaves(tree, exampleSet);
		try {
			Pair<String[], XLog[]> extract = clusterLog(onlyCorrectlyClassified,tree,exampleSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		passthroughTreeModel.deliver(wekaObject);
		
		logger.log(Level.INFO, "End: Extract Nodes ("+ (System.currentTimeMillis() - time) / 1000 + " sec)");
	}
	
	 
	 
	 private Pair<String[], XLog[]> clusterLog(boolean onlyCorrectlyClassified, J48 tree, ExampleSet exampleSet ) throws Exception
		{
//			if (maxDeviation>1 && maxDeviation<=100)
//				maxDeviation/=100.0;
//			else if (maxDeviation>1 || maxDeviation<0)
//				return null;	
		
		 List<Pair<String,GuardExpression>> listExpressions = getExpressionsAtLeaves(tree, exampleSet);
		 
			if (listExpressions==null)
				return null;
			
			int size=listExpressions.size();
			if (onlyCorrectlyClassified)
				size++;
			
			String[] objectArray=new String[size];
			GuardExpression[] exprArray=new GuardExpression[size];
			XLog retValue[]=new XLog[size];
			
			int j=0;
			for(Pair<String, GuardExpression> entry : listExpressions)
			{
				objectArray[j]=entry.getFirst();
				exprArray[j]=entry.getSecond();
				retValue[j++]=XFactoryRegistry.instance().currentDefault().createLog();
			}
			
			if (onlyCorrectlyClassified)
			{
				objectArray[j]=REMAINING;
				exprArray[j]=null;
				retValue[j]=XFactoryRegistry.instance().currentDefault().createLog();
			
			}
			
			NominalMapping idMapping = exampleSet.getAttributes().getId().getMapping();
			
			for(Example example : exampleSet){
				
				final Hashtable<String,Object> variableValues=new Hashtable<String, Object>();
							
				String traceId = idMapping.mapIndex((int)example.getId());
				
				for(Attribute attr: example.getAttributes()) {
					String name = attr.getName();	
//					System.out.println(name);
					variableValues.put(name, (Object) example.getNominalValue(attr));
				}
				
				//TO-DO get t:concept:name
				
//				Object[] instance = instanceOfATrace.get(entry);
//				
//				for(int i=0;i<instance.length-1;i++)
//					if (instance[i]!=null)
//						variableValues.put(augementationArray[i].getAttributeName(), instance[i]);
				
				for(j=0;j<exprArray.length;j++)
				{
					if (exprArray[j]==null || exprArray[j].isTrue(variableValues))
					{						
						boolean isOK=false;
						double valAsNumber=0;
						double secVal=-1;
						boolean isANumber=false;
						
						//check whether category is a number or a string
						try
						{
							valAsNumber=Double.parseDouble(objectArray[j]);
							isANumber=true;
						}
						catch(NumberFormatException nfe) {}
						if (!isANumber)
						{
							try
							{
								String value[]=objectArray[j].replace('[', ' ').replace(']', ' ').replace(',', ' ').trim().split(" ");
										
								if (value.length==2)
								{
									valAsNumber=Double.parseDouble(value[0]);
									secVal=Double.parseDouble(value[1]);
								}
							}
							catch(NumberFormatException nfe) {}
						}
//						if (onlyCorrectlyClassified && secVal>=valAsNumber && 
//								variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
//						{
//							double value=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
//							if (objectArray[j].indexOf(']')<0)
//								isOK= (value>=valAsNumber && value< secVal);
//							else
//								isOK= (value>=valAsNumber && value <= secVal);
//								
//						}
//						else if (onlyCorrectlyClassified && isANumber && 
//								variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
//						{
//							double actVal=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
//							if (Math.abs((actVal-valAsNumber)/actVal)<maxDeviation)
//								isOK=true;
//							else
//								isOK=false;
//									
//						}
//						
//						if (!onlyCorrectlyClassified || objectArray[j].equals(REMAINING) || isOK || 
//								objectArray[j].equals(variableValues.get(outputAttribute.getAttributeName())))
//						{
//							
//							XTrace aNewTrace=XFactoryRegistry.instance().currentDefault().createTrace(entry.getAttributes());
//							
//							for(XEvent event : entry)
//								if (!CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event)))
//									aNewTrace.add(XFactoryRegistry.instance().currentDefault().createEvent(event.getAttributes()));
//							retValue[j].add(aNewTrace);
//							break;
//						}
					}
				}
			}
			String[] description=new String[exprArray.length];
//			for(int i=0;i<exprArray.length;i++)
//			{
//				if (objectArray[i]!=REMAINING)
//					description[i]=objectArray[i].toString()+". Expression: "+exprArray[i];
//				else
//					description[i]=null;
//			}
			return new Pair<String[],XLog[]>(description,retValue);
				
		}
	 
	 private List<Pair<String, GuardExpression >> getExpressionsAtLeaves(J48 tree, ExampleSet exampleSet) throws OperatorException
     {
		weka.core.Instances instances = WekaTools.toWekaInstances(exampleSet, "name", exampleSet.getAttributes().size());
		Map<String, Type> variableType = new HashMap<String,Type>();
		
		for(Attribute attribute :exampleSet.getAttributes()) {
			Type type = null;
			switch(attribute.getValueType())
			{               
                  case 1:
						type = Type.LITERAL;
						break;
                  case 2:
                	    type = Type.DISCRETE;
                	    break;
                  case 3:
              	    	type = Type.DISCRETE;
              	    	break;
                  case 4:
            	    	type = Type.CONTINUOS;
            	    	break;
                  case 5:
                	  	type = Type.LITERAL;
                	  	break;
                  case 6:
                	  	type = Type.CONTINUOS;
                	  	break;
                  case 7:
                	  	type = Type.LITERAL;
                	  	break;
                  case 8:
                	  	type = Type.LITERAL;
                	  	break;
                  default: 
                	  	type = Type.TIMESTAMP;
                	  	break;
			} 	  	
			variableType.put(attribute.getName(),type);
			
			
		}
		
        LinkedList<Pair<String, GuardExpression >> retValue=new LinkedList<Pair<String, GuardExpression >>();
        try {
             WekaTreeClassificationAdapter wekaJ48Adapter = new WekaTreeClassificationAdapter(tree, instances,
                         variableType);
             for( WekaLeafNode leaf : wekaJ48Adapter.traverseLeafNodes())
             {
                   GuardExpression guard=leaf.getExpression();
                   String s=leaf.getClassName();
                   retValue.add(new Pair<String,GuardExpression>(s,guard));
             }
             return retValue;
       } catch (Exception e) {
             e.printStackTrace();
             return null;
       }
       
     }
	 
	 	@Override
		public List<ParameterType> getParameterTypes() {
			List<ParameterType> params = super.getParameterTypes();
			
			ParameterType type = new ParameterTypeLabelValues(PARAMETER_ATTRIBUTES, "The attribute which should be chosen.");
			type.setExpert(false);
			params.add(type);
			
			params.add(new ParameterTypeBoolean(EXCLUDE_WRONG,"Exclude wrongly classified instances to get strictly reliable results", true, false));
			
			return params;
		}
	
}
