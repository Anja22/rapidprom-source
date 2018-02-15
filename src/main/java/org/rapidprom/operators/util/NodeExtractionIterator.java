package org.rapidprom.operators.util;

import java.util.LinkedList;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.DataPetriNetIOObject;
import org.rapidprom.ioobjects.GuardExpressionIOObject;
import org.rapidprom.ioobjects.ResultReplayIOObject;
import org.rapidprom.ioobjects.WekaTreeExtractionIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.logmanipulation.MergeTwoEventLogsOperator;

import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

public class NodeExtractionIterator extends OperatorChain{
	
//	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM ResultReplay)", ResultReplayIOObject.class);
	private InputPort modelInput = getInputPorts().createPort("model (DataPetriNet)", DataPetriNetIOObject.class);
	private InputPort extractionInput = getInputPorts().createPort("node extraction",WekaTreeExtractionIOObject.class);
	
	private final OutputPort logOutput = getOutputPorts().createPort("event log (ProM Event Log)");
	
//	private final OutputPort alignmentInnerSource = getSubprocess(0).getInnerSources().createPort("alignments (ProM ResultReplay)");
	private final OutputPort sublogInnerSource = getSubprocess(0).getInnerSources().createPort("event sublog (ProM Event Log)");
	private final OutputPort modelInnerSource = getSubprocess(0).getInnerSources().createPort("model (DataPetriNet)");
	private final OutputPort ruleInnerSource = getSubprocess(0).getInnerSources().createPort("guard expression (WekaTree)");
	
	private final InputPort logInnerSource = getSubprocess(0).getInnerSinks().createPort("event log (ProM Event Log)");
	
//	int counter;
	
	/**
	 * Indicates the current iteration respectively node extraction element.
	 */
	private int iteration;
	
	public NodeExtractionIterator(OperatorDescription description) {
		super(description, "Executed Process");
		
//		inputPortPairExtender.start();
//		outExtender.start();
	}
	
	@Override
	public void doWork() throws OperatorException {
//		outExtender.reset();
		WekaTreeExtractionIOObject extractIOObject = extractionInput.getData(WekaTreeExtractionIOObject.class);
		Pair<GuardExpression[], XLog[]> extract = extractIOObject.getArtifact();
		
//		ResultReplayIOObject alignmentIOObject = alignmentInput.getData(ResultReplayIOObject.class);
//		ResultReplay alignment = alignmentIOObject.getArtifact();
		
		PluginContext context = RapidProMGlobalContext.instance().getPluginContext();
		
		// init Operator progress
		getProgress().setTotal(extract.getFirst().length);

		// disable call to checkForStop as inApplyLoop will call it anyway
		getProgress().setCheckForStop(false);
		
		XLog mergedLog = null;
		
		for (iteration = 0; iteration < extract.getFirst().length; iteration++) {

			modelInnerSource.deliver(modelInput.getData(DataPetriNetIOObject.class));
			
			GuardExpression expression = extract.getFirst()[iteration];
			ruleInnerSource.deliver(new GuardExpressionIOObject(expression, context));
			
			XLog log = extract.getSecond()[iteration];
			System.out.println("XLog"+ iteration + " size: " + log.size());
			
			sublogInnerSource.deliver(new XLogIOObject(log,context));
			
//			LinkedList<String> traceIds = new LinkedList<String>();
//			for (XTrace t : log) {
//				traceIds.add(t.getAttributes().get("concept:name").toString());
//			}
//				
//			//TODO create new alignment
//			
//			for (Alignment alg : alignment.labelStepArray) {
//				
//				String trace = alg.getTraceName();
//				if(traceIds.contains(trace)) {
//					//TODO add to new alignment
//				}
//			}
						
			getSubprocess(0).execute();
			inApplyLoop();
			getProgress().step();

			XLogIOObject repairedLogIOObject = logInnerSource.getData(XLogIOObject.class);			
			XLog repairedLog = repairedLogIOObject.getArtifact();
			System.out.println("Repaired log"+ iteration + " size: " + repairedLog.size());
			
			if (mergedLog==null) {
				mergedLog = XFactoryRegistry.instance().currentDefault().createLog(repairedLog.getAttributes());
			} 
//				counter = 0;
				for (XTrace t : repairedLog) {
					copyIntoFirstLog(t, mergedLog, false);
				}
//				System.out.println("Counter: " + counter);
				
			}
			
//		}
		
		logOutput.deliver(new XLogIOObject(mergedLog, context));
		getProgress().complete();
	}

	private void copyIntoFirstLog(XTrace t, XLog result, boolean dontMergeDouble) {		
		// check if in result log
		String nameTrace = XConceptExtension.instance().extractName(t);
		XTrace simTrace = null;
		for (XTrace trace : result) {
			String name = XConceptExtension.instance().extractName(trace);
			if (name.equals(nameTrace)) {
				// found trace with same name
				simTrace = trace;
				break;
			}
		}
		if (simTrace != null && !dontMergeDouble) {
			// I found a trace with similar name
			// add the events
			for (XEvent e : t) {
				XEvent copyEvent = XFactoryRegistry.instance().currentDefault().createEvent(e.getAttributes());
				simTrace.add(copyEvent);
//				counter++;
			}
		} else {
			// trace is new
			XTrace copy = XFactoryRegistry.instance().currentDefault().createTrace(t.getAttributes());
			for (XEvent e : t) {
				XEvent copyEvent = XFactoryRegistry.instance().currentDefault().createEvent(e.getAttributes());
				copy.add(copyEvent);
//				counter++;
			}
			result.add(copy);
		}	
		
	}
	
}
