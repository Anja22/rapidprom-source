package org.rapidprom.operators.analysis;


import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.tools.LogService;

public class RepairLogWithAlignmentOperator extends Operator {
	
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM ResultReplay)", ResultReplayIOObject.class);
	private InputPort modelInput = getInputPorts().createPort("model (DataPetriNet)", PetriNetIOObject.class);
	private InputPort extractInput = getInputPorts().createPort("guard expression (WekaTree)", GuardExpressionIOObject.class);
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
		
		GuardExpressionIOObject expressionIOObject = extractInput.getData(GuardExpressionIOObject.class);
		GuardExpression expression = expressionIOObject.getArtifact();
		
		AlignmentBasedLogRepairParametersImpl config = getConfiguration(alignments,expression);
		
		XLog log = RepairLog.plugin(context, alignments, net, config);
		logOutput.deliver(new XLogIOObject(log, context));

		logger.log(Level.INFO,
				"End: repair log with respect to alignment (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	
	private AlignmentBasedLogRepairParametersImpl getConfiguration(ResultReplay alignments,GuardExpression expression) {
		
		String guard = expression.toString();
		String[] value = guard.replace("(", "").replace(")", "").replace("==","").replace("!=","").replaceAll("\"[0-9]\"","").replaceAll("\"[0-9].[0-9]\"","").trim().split("&&");
		List<String> valueAsList = Arrays.asList(value);
		
		for (String values : valueAsList) {
			System.out.println(values);
		}
				
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
//							System.out.println("Break move log " + alignLabel);
							if (valueAsList.contains(alignLabel)){
//								System.out.println("Break move log " + label);
								break;
							}else {
								params.getLogMoves().add(label);
								break;
							}
						case LMNOGOOD :
							label = align.getProcessView().getActivity();
							String sm = "Sync_move_";
							alignLabel = sm.concat(label.replaceAll(" ", "_"));
//							if (valueAsList.contains(alignLabel)){
								//System.out.println("Break no good move " + label);
//								break;
//							}else {
								params.getSyncMoves().add(label);
								break;
//							}
						case MREAL :
							label = align.getProcessView().getActivity();
							String mm = "Move_model_";
							alignLabel = mm.concat(label.replaceAll(" ", "_"));
//							System.out.println("Break move model " + alignLabel);
							if (valueAsList.contains(alignLabel)) {	
//								System.out.println("Break move model " + label);
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
}
