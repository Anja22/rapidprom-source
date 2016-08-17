package org.rapidprom.operators.niek;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.tools.LogService;

public class GreedyEntropyBasedLogFilterOperator extends Operator {

	private InputPort inputXLog = getInputPorts()
			.createPort("event log (ProM Event Log)", XLogIOObject.class);
	private OutputPort outputEventLog = getOutputPorts()
			.createPort("event log collection (ProM Event Log)");

	public GreedyEntropyBasedLogFilterOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(outputEventLog,
				IOObjectCollection.class));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: Filter Log using Entropy (Greedy)");
		long time = System.currentTimeMillis();

		IOObjectCollection<XLogIOObject> result = new IOObjectCollection<XLogIOObject>();

		GreedyEntropyBasedLogFilter filterer = new GreedyEntropyBasedLogFilter();

		XLogIOObject logWrapper = inputXLog.getData(XLogIOObject.class);

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();

		for (XLog log : filterer.getProjections(pluginContext,
				logWrapper.getArtifact())) {
			result.add(new XLogIOObject(log, pluginContext));
		}
		
		outputEventLog.deliver(result);

		logger.log(Level.INFO, "End: Filter Log using Entropy (Greedy) ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

}