package org.rapidprom.operators.generation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.ptandloggenerator.plugins.NewickTreeToProcessTreeConverter;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.NewickTreeIOObject;
import org.rapidprom.ioobjects.ProcessTreeIOObject;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.ExampleSetFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.tools.LogService;

public class ConvertNewickTreeToProcessTree extends Operator {

	private InputPort input = getInputPorts().createPort("newick tree", NewickTreeIOObject.class);

	private OutputPort output = getOutputPorts().createPort("process tree");
	private OutputPort outputOriginalString = getOutputPorts().createPort("original newick tree (String)");

	public ConvertNewickTreeToProcessTree(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new GenerateNewMDRule(output, ProcessTreeIOObject.class));

		getTransformer().addRule(new GenerateNewMDRule(outputOriginalString, ExampleSet.class));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: converting newick tree to process tree");
		long time = System.currentTimeMillis();

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();

		NewickTreeToProcessTreeConverter converter = new NewickTreeToProcessTreeConverter();

		output.deliver(new ProcessTreeIOObject(
				converter.run(pluginContext, input.getData(NewickTreeIOObject.class).getArtifact()), pluginContext));

		Object[][] outputString = new Object[1][1];
		outputString[0][0] = input.getData(NewickTreeIOObject.class).getArtifact().getTree();
		ExampleSet es = ExampleSetFactory.createExampleSet(outputString);

		outputOriginalString.deliver(es);

		logger.log(Level.INFO,
				"End: converting newick tree to process tree (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

}
