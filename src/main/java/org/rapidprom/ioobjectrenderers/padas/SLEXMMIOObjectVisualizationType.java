package org.rapidprom.ioobjectrenderers.padas;

public enum SLEXMMIOObjectVisualizationType {

//	EXAMPLE_SET("Example Set"),
//	DOTTED_CHART("Dotted Chart"),
//	DOTTED_CHART_L("Dotted Chart (Legacy)"),
	DEFAULT("Default");
	
	private final String name;

	private SLEXMMIOObjectVisualizationType(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
};
