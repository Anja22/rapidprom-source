package org.rapidprom.ioobjects;

import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.rapidprom.ioobjects.abstr.AbstractRapidProMIOObject;

public class WekaTreeExtractionIOObject extends AbstractRapidProMIOObject<Pair<GuardExpression[], XLog[]>> {

	private static final long serialVersionUID = 367815926675280672L;

	public WekaTreeExtractionIOObject(Pair<GuardExpression[], XLog[]> t, PluginContext context) {
		super(t, context);
		
	}
 
	
}
