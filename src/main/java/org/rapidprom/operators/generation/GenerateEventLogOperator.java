package org.rapidprom.operators.generation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.ptandloggenerator.models.NewickTree;
import org.processmining.ptandloggenerator.plugins.GenerateLog;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.NewickTreeIOObject;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.LogService;

public class GenerateEventLogOperator extends Operator {

	public static final String PARAMETER_1_KEY = "number of traces",
			PARAMETER_1_DESCR = "the number of traces that will be generated in the resulting "
					+ "event log. trace uniqueness  is not guaranteed.";

	private InputPort input = getInputPorts().createPort("newick tree", NewickTreeIOObject.class);

	private OutputPort output = getOutputPorts().createPort("event log");

	public GenerateEventLogOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(output, XLogIOObject.class));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: generating event log from newick tree");
		long time = System.currentTimeMillis();

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();

		NewickTree tree = input.getData(NewickTreeIOObject.class).getArtifact();

		GenerateLog generator = new GenerateLog();
		XLog result = generator.run(pluginContext, tree, getParameterAsInt(PARAMETER_1_KEY));

		output.deliver(new XLogIOObject(result, pluginContext));

		logger.log(Level.INFO,
				"End: generating event log from newick tree (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeInt parameter1 = new ParameterTypeInt(PARAMETER_1_KEY, PARAMETER_1_DESCR, 1, Integer.MAX_VALUE,
				100);
		parameterTypes.add(parameter1);

		return parameterTypes;
	}

}
