package org.rapidprom.operators.extract;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.set.CustomFilter;
import com.rapidminer.example.set.ExpressionFilter;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.weka.WekaClassifier;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.preprocessing.filter.ExampleFilter;
import com.rapidminer.operator.tools.ExpressionEvaluationException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeFilter;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WekaTools;
import com.rapidminer.tools.expression.ExpressionException;

import weka.classifiers.trees.J48;

import org.processmining.datadiscovery.estimators.weka.WekaTreeClassificationAdapter;
import org.processmining.datadiscovery.estimators.weka.WekaTreeClassificationAdapter.WekaLeafNode;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.WekaTreeExtractionIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.util.RapidProMProgress;
import org.rapidprom.parameter.ParameterTypeLabelValues;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datadiscovery.estimators.Type;



public class ExtractNodesOperator extends Operator{
	
	/** defining the ports */
	private InputPort modelInput = getInputPorts().createPort("model (W-J48 Tree)");
	private InputPort exampleSetInput = getInputPorts().createPort("example set (Training set for the model)", ExampleSet.class);
	private InputPort eventlogInput = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

	private OutputPort passthroughTreeModel = getOutputPorts().createPort("model (W-J48 Tree)");
	private OutputPort extractOutput = getOutputPorts().createPort("node extraction");
	
	private static final String REMAINING = "Remaining Instances";
//	private static final String EXCLUDE_WRONG = "Exclude wrongly classified instances";
	private static final String PARAMETER_CONDITION_CLASS = "condition_class";
	public static final String PARAMETER_FILTER = "filters";
	
	/** The hidden parameter for &quot;The list of filters.&quot; */
	public static final String PARAMETER_FILTERS_LIST = "filters_list";
	
	/** The hidden parameter for &quot;Logic operator for filters.&quot; */
	public static final String PARAMETER_FILTERS_LOGIC_AND = "filters_logic_and";
	
	
	public ExtractNodesOperator(OperatorDescription description) {
		super(description);
		
		getTransformer().addRule(new GenerateNewMDRule(passthroughTreeModel, WekaClassifier.class));
		getTransformer().addRule(new GenerateNewMDRule(extractOutput, WekaTreeExtractionIOObject.class));
		
	}

	@Override
	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: Extract Nodes");
		long time = System.currentTimeMillis();
		
		PluginContext context = RapidProMGlobalContext.instance().getPluginContext();
		
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		XLogIOObject xlogIO = eventlogInput.getData(XLogIOObject.class);
		XLog log = xlogIO.getArtifact();
		
		IOObject object = modelInput.getAnyDataOrNull();
		WekaClassifier wekaObject =  (WekaClassifier) object;
		J48 tree = (J48) wekaObject.getClassifier();
		
		Pair<GuardExpression[], XLog[]> extract;
		
		try {
			extract = clusterLog(tree,exampleSet,log);
			extractOutput.deliver(new WekaTreeExtractionIOObject(extract,context));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		passthroughTreeModel.deliver(wekaObject);
		
		
		logger.log(Level.INFO, "End: Extract Nodes ("+ (System.currentTimeMillis() - time) / 1000 + " sec)");
	}
	
	 
	 
	 private Pair<GuardExpression[], XLog[]> clusterLog(J48 tree, ExampleSet exampleSet, XLog log ) throws Exception
		{
		  
		 	ExampleSet filteredExampleSet = filter(exampleSet);
		 	LinkedList<String> validNodeValues = new LinkedList<String>();
		 
		 	for (Example example : filteredExampleSet) {
			
				 String value = example.getNominalValue(example.getAttributes().getLabel());
				 if(!validNodeValues.contains(value))
					 validNodeValues.add(value);
		 	}
		 
		 	List<Pair<String,GuardExpression>> listExpressions = getExpressionsAtLeaves(tree, exampleSet);
		 	
		 	if (listExpressions==null)
				return null;
		 	
		 	List<Pair<String,GuardExpression>> toRemove = new LinkedList<Pair<String,GuardExpression>>();
		 	
		 	for(Pair<String, GuardExpression> entry : listExpressions)
			{	
				if(!validNodeValues.contains(entry.getFirst())) {
					toRemove.add(entry);
				}
			}
		 	listExpressions.removeAll(toRemove);
			
			int size=listExpressions.size();
			
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
			
			NominalMapping idMapping = filteredExampleSet.getAttributes().getId().getMapping();
			
			//for each example in the Exampleset
			for(Example example : filteredExampleSet){
				
				final Hashtable<String,Object> variableValues=new Hashtable<String, Object>();
				String traceId = idMapping.mapIndex((int)example.getId());
				
				for(Attribute attr: example.getAttributes()) {
					String name = attr.getName();
					if (attr.isNominal()) {
					variableValues.put(name, (Object) example.getNominalValue(attr));
					}else {
					variableValues.put(name, (Object) example.getNumericalValue(attr));
					}
					
				}
				
				for(j=0;j<exprArray.length;j++)
				{
					LinkedList<String> traceIds = new LinkedList<String>();
					
					if(exprArray[j].isTrue(variableValues))
					{	 
						
						for(XTrace t : log) {
	
							String id = t.getAttributes().get("concept:name").toString();							
							if (id.equals(traceId)){
								retValue[j].add(t);
								break;
							}
						}
					}
				}
			}

			return new Pair<GuardExpression[],XLog[]>(exprArray,retValue);
				
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
	 
	 private ExampleSet filter(ExampleSet inputSet) throws OperatorException {
		  
		 	String className = getParameterAsString(PARAMETER_CONDITION_CLASS);
			Condition condition = null;
			try {
					// special handling for custom_filters, as they cannot be instantiated via a simple
					// string parameter
					// this is necessary as operator.getParameterList() replaces '%{test}' by 'test'
					String rawParameterString = getParameters().getParameterAsSpecified(PARAMETER_FILTERS_LIST);
					if (rawParameterString == null) {
						throw new UndefinedParameterError(PARAMETER_FILTER, this);
					}
					List<String[]> operatorFilterList = ParameterTypeList.transformString2List(rawParameterString);
					condition = new CustomFilter(inputSet, operatorFilterList,
							getParameterAsBoolean(PARAMETER_FILTERS_LOGIC_AND), getProcess().getMacroHandler());
			
			} catch (AttributeTypeException e) {
				throw new UserError(this, e, "filter_wrong_type", e.getMessage());
			} catch (IllegalArgumentException e) {
				throw new UserError(this, e, 904, className, e.getMessage());
			}
			try {
				ExampleSet result = new ConditionedExampleSet(inputSet, condition,
						false, getProgress());
				return result;
			} catch (AttributeTypeException e) {
				throw new UserError(this, e, "filter_wrong_type", e.getMessage());
			} catch (ExpressionEvaluationException e) {
				throw new UserError(this, e, 904, className, e.getMessage());
			}
		 
	 }
	 
	 	@Override
		public List<ParameterType> getParameterTypes() {
			List<ParameterType> params = super.getParameterTypes();
			
			ParameterType type = new ParameterTypeFilter(PARAMETER_FILTER, "Define the values of the nodes that should be extracted",
					exampleSetInput, true);
			type.setExpert(false);
			params.add(type);
				
			// hidden parameter, only used to store the filters set via the ParameterTypeFilter dialog
			// above
			type = new ParameterTypeList(PARAMETER_FILTERS_LIST, "The list of filters.", new ParameterTypeString(
					"PARAMETER_FILTERS_ENTRY_KEY", "A key entry of the filters list."), new ParameterTypeString(
							"PARAMETER_FILTERS_ENTRY_VALUE", "A value entry of the filters list."), false);
			type.setHidden(true);
//			type.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_CONDITION_CLASS, true,ConditionedExampleSet.KNOWN_CONDITION_NAMES[8]));
			params.add(type);

			// hidden parameter, only used to store if the filters from the ParameterTypeFilter dialog
			// above should be ANDed or ORed
			type = new ParameterTypeBoolean(PARAMETER_FILTERS_LOGIC_AND, "Logic operator for filters.", true, false);
			type.setHidden(true);
//			type.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_CONDITION_CLASS, true,
//					ConditionedExampleSet.KNOWN_CONDITION_NAMES[8]));
			params.add(type);
//			params.add(new ParameterTypeBoolean(EXCLUDE_WRONG,"Exclude wrongly classified instances to get strictly reliable results", true, false));
			
			return params;
		}
	
}
