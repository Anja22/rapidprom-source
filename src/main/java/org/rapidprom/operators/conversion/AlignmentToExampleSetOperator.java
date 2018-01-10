package org.rapidprom.operators.conversion;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.rapidprom.ioobjects.PNRepResultIOObject;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.LogService;

public class AlignmentToExampleSetOperator extends Operator {

	/** defining the ports */
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM PNRepResult)", PNRepResultIOObject.class);
	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set (Data Table)");
	
	public AlignmentToExampleSetOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: Alignment to Table conversion");
		long time = System.currentTimeMillis();
		
		PNRepResultIOObject input= alignmentInput.getData(PNRepResultIOObject.class);
		XLogIOObject log = inputXLog.getData(XLogIOObject.class);

		PNRepResult repResult = input.getArtifact();
		XLog xlog = log.getArtifact();
		List<StepTypes> steps;
		
		for (SyncReplayResult alignment : repResult) {

			// For each trace in the alignment
            for (Integer index : alignment.getTraceIndex()) {
				XTrace t=xlog.get(index);
				steps = alignment.getStepTypes();
				System.out.println(Arrays.toString(steps.toArray()));
//				processAlignment(t,alignment.getStepTypes(), alignment.getNodeInstance());
            	}
			}
		
//		exampleSetOutput.deliver(...);
		
		logger.log(Level.INFO, "End: Alignment to Table conversion ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");
		
		}
				 
//	protected void processAlignment(XTrace xTrace,List<StepTypes> steps, List<Object> nodeInstanceList) {
//			     
//            Iterator<XEvent> eventIter=xTrace.iterator();
//            Transition transition = null;
//            XEvent nextEvent = null;
//            int numSynchronousMoves,numLogMoves,numModelMoves;
//           
//           
//            for(StepTypes step : steps)
//            {
//                  switch(step)
//                  {
//                        case LMGOOD:
//                              nextEvent = eventIter.next();
//                              transition = (Transition) transIter.next();
//                              numSynchronousMoves++;				                              
//                              break;
//                             
//                        case L :
//                              nextEvent = eventIter.next();
//                              transIter.next();
//                              transition = null;
//                              numLogMoves++;
//                             
//                             
//                        case MINVI :
//                        case MREAL :
//                              nextEvent = null;
//                              transition = (Transition) transIter.next();
//                              numModelModes++;
//                  }
//
//	}
	
	
}
