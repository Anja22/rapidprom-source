package org.rapidprom.operators.analysis;


import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.Alignment.AlignmentStep;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.RepairLog.AlignmentBasedLogRepairParametersImpl;
import org.processmining.plugins.DataConformance.RepairLog.RepairLog;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.DataPetriNetIOObject;
import org.rapidprom.ioobjects.GuardExpressionIOObject;
import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.ioobjects.ResultReplayIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.util.RapidProMProgress;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.associations.FrequentItemSet;
import com.rapidminer.operator.learner.associations.FrequentItemSets;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.LogService;

public class RepairLogWithAlignmentOperator extends Operator {
	
	private static final String RULESOURCE = "Source";
	private static final String DECISIONTREE = "Decision Tree rules";
	private static final String FREQUENTITEMSET = "Frequent Itemset";
	
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM ResultReplay)", ResultReplayIOObject.class);
	private InputPort modelInput = getInputPorts().createPort("model (DataPetriNet)", PetriNetIOObject.class);
	private InputPort extractInput = getInputPorts().createPort("repair rules");
	private InputPort extractInput2 = getInputPorts().createPort("guard expression (WekaTree)", GuardExpressionIOObject.class);
	private OutputPort logOutput = getOutputPorts().createPort("event log (ProM Event Log)");
	
	
	public RepairLogWithAlignmentOperator(OperatorDescription description) {
		super(description);
		
		getTransformer().addRule(new GenerateNewMDRule(logOutput, XLogIOObject.class));
	}
	
	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start:Repair Log with Respect to Alignment");
		long time = System.currentTimeMillis();

		PluginContext context = RapidProMGlobalContext.instance().getProgressAwarePluginContext(new RapidProMProgress(getProgress()));
		
		ResultReplayIOObject resultRepIO = alignmentInput.getData(ResultReplayIOObject.class);
		ResultReplay alignments = resultRepIO.getArtifact();
		
		DataPetriNetIOObject dpnIOObject = modelInput.getData(DataPetriNetIOObject.class);
		DataPetriNet net = dpnIOObject.getArtifact();
		AlignmentBasedLogRepairParametersImpl config;
		
		if(getParameterAsString(RULESOURCE).equals(DECISIONTREE)) {
			GuardExpressionIOObject expressionIOObject = extractInput.getData(GuardExpressionIOObject.class);
			GuardExpression expression = expressionIOObject.getArtifact();
			config = getConfiguration(alignments,expression, expressionIOObject.getGoodValue());
		}else {
			FrequentItemSets fiSet = extractInput.getData(FrequentItemSets.class);
			GuardExpressionIOObject expressionIOObject = extractInput2.getData(GuardExpressionIOObject.class);
			config = getConfiguration(alignments,fiSet, expressionIOObject.getGoodValue());
		}
		
				
		XLog log = RepairLog.plugin(context, alignments, net, config);
		logOutput.deliver(new XLogIOObject(log, context));

		logger.log(Level.INFO,
				"End: repair log with respect to alignment (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	private AlignmentBasedLogRepairParametersImpl getConfiguration(ResultReplay alignments, List<String> valueAsList) {
		
		AlignmentBasedLogRepairParametersImpl params = new AlignmentBasedLogRepairParametersImpl();
		String label = null;
		String alignLabel = null;
		
		for (Alignment alignment : alignments.labelStepArray)
			{	
				Iterator<AlignmentStep> iterator = alignment.iterator();
				while(iterator.hasNext())
				{
					AlignmentStep align=iterator.next();
					switch(align.getType())
					{
						case L :
							label = align.getLogView().getActivity();					
							String ml = "Move_log_";
							alignLabel = ml.concat(label.replaceAll(" ", "_"));
							//System.out.println("Break move log " + alignLabel);
							if (!valueAsList.isEmpty() && valueAsList.contains(alignLabel)){
								//System.out.println("Break move log " + label);
								break;
							}else {
								params.getLogMoves().add(label);
								break;
							}
						case LMNOGOOD :
							label = align.getProcessView().getActivity();
							String sm = "Sync_move_";
							alignLabel = sm.concat(label.replaceAll(" ", "_"));
							if (!valueAsList.isEmpty() && valueAsList.contains(alignLabel)){
								//System.out.println("Break no good move " + label);
								break;
							}else{
								params.getSyncMoves().add(label);
								break;
							}
						case MREAL :
							label = align.getProcessView().getActivity();
							String mm = "Move_model_";
							alignLabel = mm.concat(label.replaceAll(" ", "_"));
							//System.out.println("Break move model " + alignLabel);
							if (!valueAsList.isEmpty() && valueAsList.contains(alignLabel)) {	
								//System.out.println("Break move model " + label);
								break;
							}else {
								params.getModelMoves().add(label);
								break;
							}							
						default :
							break;					
					}
				}
			}
		
		return params;
	}
	
	private AlignmentBasedLogRepairParametersImpl getConfiguration(ResultReplay alignments,GuardExpression expression,boolean relevantExpression) {
		
		List<String> valueAsList = new LinkedList<String>();
		
		if (relevantExpression == true) {
		String guard = expression.toString();
		String[] value = guard.replace("(", "").replace(")", "").replace("==","").replace("!=","").replace("<=","").replace(">","").replaceAll("\"[0-9]\"","").replaceAll("\"[0-9].[0-9]\"","").replaceAll("[0-9].[0-9]","").trim().split("&&");
		valueAsList = Arrays.asList(value);
		} 

		for (String values : valueAsList) {
			System.out.println(values);
		}
				
		return getConfiguration(alignments,valueAsList);
	}
	
	private AlignmentBasedLogRepairParametersImpl getConfiguration(ResultReplay alignments,FrequentItemSets frequentItemSets,boolean relevantExpression) {
		
		List<String> valueAsList = new LinkedList<String>();
		if (relevantExpression == true) {
			if (frequentItemSets.size() > 0) {
				
				int maxSize = frequentItemSets.getMaximumSetSize();
				
				for (FrequentItemSet itemSet : frequentItemSets) {
					
					//TODO get biggest frequentset
					if(itemSet.getNumberOfItems()==maxSize) {
						
						String[] value = itemSet.getItemsAsString().split(", ");
						valueAsList = Arrays.asList(value);
						
						for (String values : valueAsList) {
							System.out.println(values);
						}
						
		//					System.out.println("Size: "+ itemSet.getNumberOfItems() + " Items: " + itemSet.getItemsAsString());
		//					valueAsList.add(itemSet.getItemsAsString());
					}
					
				}
				
			}
		}	
		
//		String guard = expression.toString();
//		String[] value = guard.replace("(", "").replace(")", "").replace("==","").replace("!=","").replaceAll("\"[0-9]\"","").replaceAll("\"[0-9].[0-9]\"","").trim().split("&&");
		

				
		return getConfiguration(alignments,valueAsList);
	}
	
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> params = super.getParameterTypes();

		String[] options = {DECISIONTREE,FREQUENTITEMSET};
		
		params.add(new ParameterTypeCategory(RULESOURCE , "Source that defines which steps to repair.",	options , 1, false));
		return params;
	}
}
