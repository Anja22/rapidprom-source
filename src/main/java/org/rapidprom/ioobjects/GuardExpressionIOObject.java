package org.rapidprom.ioobjects;

import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.semantics.petrinet.Marking;
import org.rapidprom.ioobjects.abstr.AbstractRapidProMIOObject;

public class GuardExpressionIOObject extends AbstractRapidProMIOObject<GuardExpression>{

	private static final long serialVersionUID = 6337911956021358914L;

	private boolean goodValue = true;
	
	public GuardExpressionIOObject(GuardExpression t, PluginContext context) {
		super(t, context);
	}

	public void setGoodValue(boolean value) {
		goodValue = value;
	}
	
	public boolean getGoodValue() {
		return goodValue;
	}
}
