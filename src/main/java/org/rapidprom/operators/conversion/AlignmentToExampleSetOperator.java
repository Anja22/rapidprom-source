package org.rapidprom.operators.conversion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.rapidprom.ioobjects.PNRepResultIOObject;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

public class AlignmentToExampleSetOperator extends Operator {

	/** defining the ports */
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM PNRepResult)", PNRepResultIOObject.class);
	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set (Data Table)");
	
	private Attribute[] attributes = null;
	private ExampleSetMetaData metaData = null;
	private MemoryExampleTable table = null;
	private LinkedList<String> alignmentMoves;
	
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
		
		
		ExampleSet es = null;
		table = createStructureTable(xlog, repResult);
		alignmentMoves = getUniqueMoves(xlog,repResult);
		
		for (SyncReplayResult alignment : repResult) {
			
			// For each trace in the alignment
            for (Integer index : alignment.getTraceIndex()) {
				XTrace t=xlog.get(index);
				TreeMap<String, Integer> mMap = extractAlignmentDetails(t,alignment.getStepTypes(), alignment.getNodeInstance(),alignmentMoves);
				String id = t.getAttributes().values().toString().replaceAll("[\\[\\]]", "");
				es = fillTable(mMap, id);
//				XAttributeMap x = t.get(0).getAttributes();
//				XAttribute attr = x.get("x");
            	}
			}
		
		exampleSetOutput.deliver(es);
		
		logger.log(Level.INFO, "End: Alignment to Table conversion ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");
		
		}
	
	
		
	protected TreeMap<String, Integer> extractAlignmentDetails(XTrace xTrace,List<StepTypes> steps, List<Object> nodeInstanceList, LinkedList<String> alignmentMoves) {
		
		TreeMap<String, Integer> movesMap = new TreeMap<String, Integer>();
		
		for(String move : alignmentMoves) {
		    //Add all items to the Map
		    movesMap.put(move, 0);
		}
		
		Iterator<XEvent> eventIter=xTrace.iterator();
		Iterator<Object> transIter=nodeInstanceList.iterator();
		
		Transition transition = null;
		XEvent nextEvent = null;
		String label = null;
		Object object = null;
		
		for(StepTypes step : steps)
          { 			
                switch(step)
                {                
                      case LMGOOD:
                            nextEvent = eventIter.next();
                            object = transIter.next();
                            transition = object instanceof XEventClass ? null : (Transition) object;	
                            label = XConceptExtension.instance().extractName(nextEvent);
                            movesMap.put("Sync move " + label, movesMap.get("Sync move "+label)+1);  //numSynchronousMoves++
                            break;
                           
                      case L :
                            nextEvent = eventIter.next();
                            transIter.next();
                            transition = null;
                            label = XConceptExtension.instance().extractName(nextEvent);
                            movesMap.put("Move log " + label, movesMap.get("Move log "+label)+1);
                            break; 
                           
                      case MINVI :
                    	  transIter.next();
                    	  break; 
                      case MREAL :
                            nextEvent = null;
                            object = transIter.next();
                            if (!(object instanceof XEventClass)){                            	
                            	transition = (Transition) object;
                            	label = (String) transition.getAttributeMap().get("ProM_Vis_attr_label");  
                            	movesMap.put("Move model " + label, movesMap.get("Move model " + label)+1);
                            }
                            
                            /*XAttributeMap x = t.get(0).getAttributes();
            				XAttribute attr = x.get("x");*/
                }
          }
		
		return movesMap;
	}
	
	private LinkedList<String> getUniqueMoves (XLog xlog, PNRepResult repResult){
		
		LinkedList<String> eventList = new LinkedList<String>();
		
		for (XTrace t : xlog) {
			for (XEvent e : t) {
				String event = XConceptExtension.instance().extractName(e);
				if (!eventList.contains("Sync move "+ event)) {
					eventList.add("Sync move "+ event);
					eventList.add("Move model "+ event);
					eventList.add("Move log "+ event);
				}
			}
		}
		
		for (SyncReplayResult alignment : repResult) {
			
			// For each trace in the alignment
            for (Object transition : alignment.getNodeInstance()) {
            	if(!(transition instanceof XEventClass)) {
            		Transition transition2 = (Transition) transition;
            	if((Boolean)transition2.getAttributeMap().get("ProM_Vis_attr_showLabel")==true) {
                	String label = (String) transition2.getAttributeMap().get("ProM_Vis_attr_label");  
                	if (!eventList.contains("Sync move "+ label)) {
    					eventList.add("Sync move "+ label);
    					eventList.add("Move model "+ label);
    					eventList.add("Move log "+ label);
    				}
                }
            	}
			}
		}     
		
		return eventList;
	}
	
	private MemoryExampleTable createStructureTable (XLog xlog, PNRepResult repResult) {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		List<Attribute> attributes = new LinkedList<Attribute>();
		LinkedList<String> alignmentMoves = getUniqueMoves(xlog, repResult);
		
		attributes.add(AttributeFactory.createAttribute("Traceidentifier", Ontology.STRING));
		AttributeMetaData amd_0 = new AttributeMetaData("Traceidentifier", Ontology.STRING);
		amd_0.setRole(AttributeColumn.REGULAR);
		amd_0.setNumberOfMissingValues(new MDInteger(0));
		metaData.addAttribute(amd_0);
		
		for (int i=0; i < alignmentMoves.size(); i++) {
			
			String columnName = alignmentMoves.get(i);
			AttributeMetaData amd = null;
			
				attributes.add(AttributeFactory.createAttribute(columnName, Ontology.NOMINAL));
				amd = new AttributeMetaData(columnName, Ontology.NOMINAL);
				amd.setRole(AttributeColumn.REGULAR);
				amd.setNumberOfMissingValues(new MDInteger(0));
				metaData.addAttribute(amd);
		}	
		// convert the list to array
		Attribute[] attribArray = new Attribute[attributes.size()];
		for (int ii=0; ii<attributes.size(); ii++) {
			attribArray[ii] = attributes.get(ii);
		}
		metaData.setNumberOfExamples(xlog.size());
		this.metaData = metaData;
		this.attributes = attribArray;
		MemoryExampleTable memoryExampleTable = new MemoryExampleTable(attributes);
		return memoryExampleTable;
	}
	
	private ExampleSet fillTable (TreeMap<String, Integer> movesMap, String traceID) {
		DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_INT_ARRAY, '.');
//		// now add per row
			// fill strings			
			String[] strings = new String[movesMap.size()+1];
			strings[0]= traceID;
			for (int j=0; j<movesMap.size(); j++) {
				strings[j+1] = movesMap.get(alignmentMoves.get(j)).toString();
			}			
			DataRow dataRow = factory.create(strings, attributes);
			table.addDataRow(dataRow);
//		}
		ExampleSet createExampleSet = table.createExampleSet();
		return createExampleSet;
	}
	
}
