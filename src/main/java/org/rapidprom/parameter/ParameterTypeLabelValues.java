package org.rapidprom.parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;

public class ParameterTypeLabelValues extends ParameterType {

	private static final long serialVersionUID = -4491120971768503393L;

	public ParameterTypeLabelValues(String key, String description) {
		super(key, description);
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRange() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getDefaultValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isNumerical() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		// TODO Auto-generated method stub
		return null;
	}

}
