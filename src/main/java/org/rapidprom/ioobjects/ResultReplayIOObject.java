package org.rapidprom.ioobjects;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.rapidprom.ioobjects.abstr.AbstractRapidProMIOObject;

public class ResultReplayIOObject extends AbstractRapidProMIOObject<ResultReplay> {

	private static final long serialVersionUID = 1L;

	public ResultReplayIOObject(ResultReplay t, PluginContext context) {
		super(t, context);
	}

}
