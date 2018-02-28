package org.rapidprom.ioobjectrenderers;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.visualization.alignment.ColorTheme;
import org.processmining.plugins.DataConformance.visualization.alignment.XTraceResolver;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentMasterDetail;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentsSimpleImpl;
import org.processmining.plugins.balancedconformance.export.XAlignmentConverter;
import org.processmining.plugins.balancedconformance.result.AlignmentCollection;
import org.processmining.plugins.balancedconformance.result.AlignmentCollectionImpl;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentMasterView.GroupedAlignmentInput;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;
import org.rapidprom.ioobjectrenderers.abstr.AbstractRapidProMIOObjectRenderer;
import org.rapidprom.ioobjects.ResultReplayIOObject;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ResultReplayIOObjectRenderer extends AbstractRapidProMIOObjectRenderer<ResultReplayIOObject>{

	private final static class NoTraceResolver implements XTraceResolver {
		@Override
		public boolean hasOriginalTraces() {
			return false;
		}

		@Override
		public XTrace getOriginalTrace(String name) {
			return null;
		}
	}

	@Override
	public String getName() {
		return "Explore Alignments";
	}

	@Override
	protected JComponent runVisualization(ResultReplayIOObject ioObject) {
		
		XAlignmentConverter converter = new XAlignmentConverter();
		ResultReplay resultReplay = ioObject.getArtifact();
		XTraceResolver traceResolver = buildTraceMap(resultReplay);
		
		converter.setClassifier(resultReplay.getClassifier());
		converter.setVariableMapping(resultReplay.getVariableMapping());
		
		Iterable<XAlignment> xAlignments = convertToXAlignment((AlignmentCollection)resultReplay, converter, traceResolver);
		
		Map<String, Color> activityColorMap = ColorTheme.createColorMap(xAlignments);
		GroupedAlignmentInput<XAlignment> input = new GroupedAlignmentInput<>(
				new GroupedAlignmentsSimpleImpl(xAlignments, activityColorMap), new NoTraceResolver(), activityColorMap);
//				new GroupedAlignmentsSimpleImpl(activityColorMap), new NoTraceResolver(), activityColorMap);
		return new GroupedAlignmentMasterDetail(ioObject.getPluginContext(), input);
	}
	
	private XTraceResolver buildTraceMap(ResultReplay logReplayResult) {
		final Map<String, XTrace> traceMap = new HashMap<>();
		for (XTrace trace : logReplayResult.getAlignedLog()) {
			traceMap.put(XConceptExtension.instance().extractName(trace), trace);
		}
		return new XTraceResolver() {

			public boolean hasOriginalTraces() {
				return true;
			}

			public XTrace getOriginalTrace(String name) {
				return traceMap.get(name);
			}
		};
	}
	
	private Iterable<XAlignment> convertToXAlignment(AlignmentCollection alignments, final XAlignmentConverter converter,
			final XTraceResolver resolver) {
		return ImmutableList.copyOf(Iterables.transform(alignments.getAlignments(), new Function<Alignment, XAlignment>() {

			public XAlignment apply(Alignment a) {
				return converter.viewAsXAlignment(a, resolver.getOriginalTrace(a.getTraceName()));
			}
		}));
	}
}
