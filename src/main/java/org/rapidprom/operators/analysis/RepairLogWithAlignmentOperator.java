package org.rapidprom.operators.analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.ProMCheckComboBox;
import org.processmining.modelrepair.parameters.RepairConfiguration;
import org.processmining.modelrepair.plugins.Uma_RepairModel_Plugin;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.RepairLog.AlignmentBasedLogRepairParametersImpl;
import org.processmining.plugins.DataConformance.RepairLog.RepairLog;
import org.processmining.plugins.petrinet.behavioralanalysis.woflan.Woflan;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.DataPetriNetIOObject;
import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.ioobjects.ResultReplayIOObject;
import org.rapidprom.ioobjects.WoflanDiagnosisIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.util.RapidProMProgress;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.preprocessing.filter.attributes.BlockTypeAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.NoMissingValuesAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.NumericValueAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.SingleAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.SubsetAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.TransparentAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.ValueTypeAttributeFilter;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

public class RepairLogWithAlignmentOperator extends Operator {
	
	public static final String PARAMETER_ATTRIBUTES_LOGMOVE = "Log moves";
	public static final String PARAMETER_ATTRIBUTES_MODELMOVE = "Process moves";
	public static final String PARAMETER_ATTRIBUTES_SYNCNOGOODMOVE = "Synchronous moves with wrong write operations:";
	private static final String PARAMETER_ATTRIBUTE_MODELMOVE = null;
	private static final String PARAMETER_ATTRIBUTE_SYNCNOGOODMOVE = null;
	
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM ResultReplay)", ResultReplayIOObject.class);
	private InputPort modelInput = getInputPorts().createPort("model (DataPetriNet)", PetriNetIOObject.class);
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
		
		XLog log = RepairLog.plugin(context, alignments, net, getConfiguration());
		logOutput.deliver(new XLogIOObject(log, context));
		

		logger.log(Level.INFO,
				"End: repair log with respect to alignment (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		
//		ParameterType logMoves = new ParameterTypeAttributes(PARAMETER_ATTRIBUTES_LOGMOVE, "Log moves to consider when repairing logs:",
//				alignmentInput, Ontology.STRING);
//		logMoves.setExpert(false);
//		
////		ParameterType modelMoves = new ParameterTypeAttributes(PARAMETER_ATTRIBUTES_MODELMOVE, "Process moves to consider when repairing logs:",
//				alignmentInput, Ontology.STRING);
//		modelMoves.setExpert(false);
//		
////		ParameterType syncNoGoodMoves = new ParameterTypeAttributes(PARAMETER_ATTRIBUTES_SYNCNOGOODMOVE, "Synchronous moves with wrong write operations:",
//				alignmentInput, Ontology.STRING);
//		syncNoGoodMoves.setExpert(false);
//		
//		types.add(logMoves);
//		types.add(modelMoves);
//		types.add(syncNoGoodMoves);
		return types;
	}
	
	private AlignmentBasedLogRepairParametersImpl getConfiguration() {
		
		List<String[]> logMoves = null;
		List<String[]> modelMoves = null;
		List<String[]> syncMovesNoGood = null;
		
		try {
			logMoves = getParameterList(PARAMETER_ATTRIBUTES_LOGMOVE);
			modelMoves = getParameterList(PARAMETER_ATTRIBUTE_MODELMOVE);
			syncMovesNoGood = getParameterList(PARAMETER_ATTRIBUTE_SYNCNOGOODMOVE);

		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		}
		
		AlignmentBasedLogRepairParametersImpl params = new AlignmentBasedLogRepairParametersImpl();
		
		if (!logMoves.isEmpty()) {
			for (Object s : logMoves) {
				params.getLogMoves().add(s.toString());
			}
		}
			
		if (!modelMoves.isEmpty()) {
			for (Object s : modelMoves) {
				params.getModelMoves().add(s.toString());
			}
		}
		
		if (!syncMovesNoGood.isEmpty()) {
			for (Object s : syncMovesNoGood) {
				params.getSyncMoves().add(s.toString());
			}
		}
		
		return params;
	}
}
