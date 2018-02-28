package org.rapidprom.operators.conversion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.Alignment.AlignmentStep;
import org.rapidprom.ioobjects.ResultReplayIOObject;
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
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

public class AlignmentToExampleSetOperator extends Operator {

	/** defining the ports */
	private InputPort alignmentInput = getInputPorts().createPort("alignments (ProM PNRepResult)", ResultReplayIOObject.class);
	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set (Data Table)");
	
	private Attribute[] attributes = null;
	private ExampleSetMetaData metaData = null;
	private MemoryExampleTable table = null;
	private LinkedList<String> alignmentMoves;
	
	
	public AlignmentToExampleSetOperator(OperatorDescription description) {
		super(description);
		
		getTransformer().addRule(new GenerateNewMDRule(exampleSetOutput, ExampleSet.class));
	}

	@Override
	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: Alignment to Table conversion");
		long time = System.currentTimeMillis();
		
		ResultReplayIOObject input= alignmentInput.getData(ResultReplayIOObject.class);
		XLogIOObject log = inputXLog.getData(XLogIOObject.class);

		ResultReplay repResult = input.getArtifact();
		XLog xlog = log.getArtifact();
		
		
		ExampleSet es = null;
		table = createStructureTable(xlog, repResult);
		alignmentMoves = getUniqueMoves(repResult);
		
		for (Alignment alignment : repResult.labelStepArray) {
			
				String trace = alignment.getTraceName();
				TreeMap<String, Integer> mMap = extractAlignmentDetails(alignment,alignmentMoves);
				es = fillTable(mMap, trace);

			}
		
		exampleSetOutput.deliverMD(metaData);
		exampleSetOutput.deliver(es);
		
		logger.log(Level.INFO, "End: Alignment to Table conversion ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");
		
		}
	
	
		

	protected TreeMap<String, Integer> extractAlignmentDetails(Alignment alignment, LinkedList<String> alignmentMoves) {
		
		TreeMap<String, Integer> movesMap = new TreeMap<String, Integer>();
		
		for(String move : alignmentMoves) {
		    //Add all items to the Map
		    movesMap.put(move, 0);
		}
		
		String label = null;
		
		Iterator<AlignmentStep> alignIter = alignment.alignmentStepIterator();
		
		while(alignIter.hasNext())
			{
				AlignmentStep step = alignIter.next();
				
				switch(step.getType())
				{               
                      case LMGOOD:
                    	  	label = step.getProcessView().getActivity().replaceAll(" ", "_");
                            movesMap.put("Sync_move_" + label, movesMap.get("Sync_move_"+label)+1);  //numSynchronousMoves++
//                            System.out.println("Sync move "+ label);
                            break;
                           
                      case L :
                            label = step.getLogView().getActivity().replaceAll(" ", "_");
                            movesMap.put("Move_log_" + label, movesMap.get("Move_log_"+label)+1);
//                            System.out.println("Move log "+ label);
                            break; 
                           
                      case MINVI :
                    	  	break; 
                      case MREAL :
                          	label = step.getProcessView().getActivity().replaceAll(" ", "_");  
                          	movesMap.put("Move_model_" + label, movesMap.get("Move_model_" + label)+1);
//                          	System.out.println("Move model "+ label);
                          	break; 
                      case LMNOGOOD:
                    	  	label = step.getProcessView().getActivity().replaceAll(" ", "_");                	  	
                    	  	movesMap.put("Sync_move_" + label, movesMap.get("Sync_move_"+label)+1);  //numSynchronousMoves++
//                    	  	System.out.println("Sync move "+ label);
                    	  	break;
                            
                            /*XAttributeMap x = t.get(0).getAttributes();
            				XAttribute attr = x.get("x");*/
                }
          }
		
		return movesMap;
	}
	
	private LinkedList<String> getUniqueMoves (ResultReplay repResult){
		
		LinkedList<String> eventList = new LinkedList<String>();
		
		for (Alignment alignment : repResult.labelStepArray) {
		
		Iterator<AlignmentStep> alignIter = alignment.alignmentStepIterator();
		
		while(alignIter.hasNext())
			{
				AlignmentStep step = alignIter.next();
				String label = null;
				if(!step.getProcessView().isInvisible()) {
					switch (step.getType()) {
						case MREAL:
							label = step.getProcessView().getActivity().replaceAll(" ", "_");
							break;
						default:
							label = step.getLogView().getActivity().replaceAll(" ", "_");
					}
					
					if (!eventList.contains("Sync_move_"+ label)) {
//						System.out.println(label);
    					eventList.add("Sync_move_"+ label);
    					eventList.add("Move_model_"+ label);
    					eventList.add("Move_log_"+ label);
    				}
				}
			}
		}
//		System.out.println("Size: " + eventList.size());
		return eventList;
	}
	
	@SuppressWarnings("deprecation")
	private MemoryExampleTable createStructureTable (XLog xlog, ResultReplay repResult) {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		List<Attribute> attributes = new LinkedList<Attribute>();
		
		LinkedList<String> alignmentMoves = getUniqueMoves(repResult);
		
		attributes.add(AttributeFactory.createAttribute("Traceidentifier", Ontology.NOMINAL));
		AttributeMetaData amd_0 = new AttributeMetaData("Traceidentifier", Ontology.NOMINAL);
		amd_0.setRole(AttributeColumn.REGULAR);
		amd_0.setNumberOfMissingValues(new MDInteger(0));
		metaData.addAttribute(amd_0);
		
		for (int i=0; i < alignmentMoves.size(); i++) {
			
			String columnName = alignmentMoves.get(i);
			AttributeMetaData amd = null;
			
				attributes.add(AttributeFactory.createAttribute(columnName, Ontology.NUMERICAL));
				amd = new AttributeMetaData(columnName, Ontology.NUMERICAL);
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
