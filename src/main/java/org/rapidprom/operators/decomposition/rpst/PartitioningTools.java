/*
 * From DecomposedReplayer 
 */
package org.rapidprom.operators.decomposition.rpst;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

/**
 * Tools to perform cuts and partitions over a RPST
 * 
 * @author Jorge Munoz-Gama (jmunoz)
 */
public class PartitioningTools {
	
	/**
	 * Return a set of RPST Nodes with the largest number of arcs below a given 
	 * threshold, such that the set represents a partition over the edges of the
	 * original Petri net (if the RPST has not been modified), i.e., all edges of
	 * the petri net belong to one node, and only one.
	 * 
	 * See: Jorge Munoz-Gama, Josep Carmona and Wil M.P. van der Aalst. 
	 * Conformance Checking in the Large: Partitioning and Topology
	 * @param rpst RPST 
	 * @param maxSize The nodes returned have a number of arcs assigned at most as large as the given threshold.
	 * @return Set of nodes of the RPST
	 */
	public static List<PetriNetRPSTNode> partitioning(PetriNetRPST rpst, int maxSize){
		
		Set<Transition> invisibles = computeInvisibles(rpst.getNet().getNet());
		Set<Set<Transition>> duplicates = computeDuplicates(rpst.getNet().getNet());
		
		List<PetriNetRPSTNode> part = new LinkedList<PetriNetRPSTNode>();
		
		Queue<PetriNetRPSTNode> toExplore = new LinkedList<PetriNetRPSTNode>();
		toExplore.add(rpst.getRoot());
		while(!toExplore.isEmpty()){
			PetriNetRPSTNode curr = toExplore.poll();
			
			if(curr.getArcs().size() <= maxSize){
				part.add(curr);
			}
			else if(!checkIfPossibleValidDecomposition(rpst.getNet().getNet(), rpst.getChildren(curr), 
					invisibles, duplicates)){
				part.add(curr);
			}
			else{
				toExplore.addAll(rpst.getChildren(curr));
			}
		}		
		return part;
	}
	
	private static Set<Transition> computeInvisibles(Petrinet net){
		Set<Transition> invisibles = new LinkedHashSet<Transition>();		
		for (Transition t: net.getTransitions()){
			if (t.isInvisible()) invisibles.add(t);
		}	
		return invisibles;
	}
	
	private static Set<Set<Transition>> computeDuplicates(Petrinet net){
		Map<String, Set<Transition>> mapDuplicates = new HashMap<String,Set<Transition>>();
		for (Transition t: net.getTransitions()){
			String label = t.getLabel();
			Set<Transition> transitions = mapDuplicates.get(label);
			if(transitions == null){
				transitions = new LinkedHashSet<Transition>();
				mapDuplicates.put(label, transitions);
			}
			transitions.add(t);
		}
		
		Set<Set<Transition>> duplicates = new LinkedHashSet<Set<Transition>>();
		for(Set<Transition> set: mapDuplicates.values()){
			if(set.size() > 1) duplicates.add(set);
		}
		
		return duplicates;
	}
	
	private static boolean checkIfPossibleValidDecomposition(Petrinet net, Collection<PetriNetRPSTNode> nodes, 
			Set<Transition> invisibles, Set<Set<Transition>> duplicates){
		
		//IF not invisible or duplicate presents, 
		if(invisibles.isEmpty() && duplicates.isEmpty()) return true;
		
		//Set of Duplicates of the overall net
		Set<Transition> duplicatesSet = new LinkedHashSet<Transition>();
		for(Set<Transition> set: duplicates){
			duplicatesSet.addAll(set);
		}
		
		//Set of shared transitions among the nodes 
		Set<Transition> sharedTransitions = new LinkedHashSet<Transition>();
		for(PetriNetRPSTNode node1: nodes){
			for(PetriNetRPSTNode node2: nodes){				
				if(!node1.equals(node2)){
					if(!Collections.disjoint(node1.getTrans(), node2.getTrans())){
						Set<Transition> intersection = new LinkedHashSet<Transition>(node1.getTrans());
						intersection.retainAll(node2.getTrans());
						sharedTransitions.addAll(intersection);
					}
				}
			}
		}
		
		//Set of shared transitions among the nodes 
		Set<Place> sharedPlaces = new LinkedHashSet<Place>();
		for(PetriNetRPSTNode node1: nodes){
			for(PetriNetRPSTNode node2: nodes){				
				if(!node1.equals(node2)){
					if(!Collections.disjoint(node1.getPlaces(), node2.getPlaces())){
						Set<Place> intersection = new LinkedHashSet<Place>(node1.getPlaces());
						intersection.retainAll(node2.getPlaces());
						sharedPlaces.addAll(intersection);
					}
				}
			}
		}
		
		
		//Valid Decomposition: Not sharing an invisible
		if(!Collections.disjoint(sharedTransitions, invisibles)) return false;
		
		//Valid Decomposition: Not sharing a duplicate
		if(!Collections.disjoint(sharedTransitions, duplicatesSet)) return false;
		
		//If a shared Place is connected with an invisible/duplicate, return false
		//Because the future 'bridging' of the place will produce shared them
		for(Place p: sharedPlaces){		
			Set<Transition> connectedTrans = new LinkedHashSet<Transition>();
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(p))
				connectedTrans.add((Transition)edge.getSource());
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(p))
				connectedTrans.add((Transition)edge.getTarget());
			
			if(	!Collections.disjoint(invisibles, connectedTrans) ||
				!Collections.disjoint(duplicatesSet, connectedTrans)) return false;
		}
		
		//Valid Decomposition: Duplicates with the same label must appear all in one subnet
		for(Set<Transition> dup: duplicates){
			for(PetriNetRPSTNode node: nodes){
				Set<Transition> intersection = new LinkedHashSet<Transition>(dup);
				intersection.retainAll(node.getTrans());
				if(intersection.size() > 0 && intersection.size() != dup.size()) return false;
			}
		}
		
		return true;
	}

}
