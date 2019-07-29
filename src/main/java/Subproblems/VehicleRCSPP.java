package Subproblems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Data.VehicleType;
import Networks.VehicleArc;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class VehicleRCSPP 
{
	private VehicleType vehicleType; 
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph;
	private VehicleVertex sourceVertex; 
	private VehicleVertex sinkVertex; 
	
	private List<VehicleVertex> queue;
	@Getter
	private List<Block> blocksGenerated; 
	private Map<Trip, Double> dualValuesOfTripIDs; 
	private List<Trip> trips; 
	
	private boolean generateAllVariables;
	private boolean allowedLineChange; 
	private boolean useSubNetwork; 
	public VehicleRCSPP(VehicleType vehicleType, List<Trip> trips, DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph, Map<Trip, Double> dualValuesOfTripIDs, boolean allowedLineChange, boolean useSubNetwork)
	{
		this.vehicleType = vehicleType; 
		this.vehicleGraph = vehicleGraph;  
		this.dualValuesOfTripIDs = dualValuesOfTripIDs; 
		this.trips = trips; 
		
		this.sourceVertex = this.vehicleGraph.vertexSet().stream().filter(v -> v.getVertexId() == -1).collect(Collectors.toList()).get(0); 
		this.sinkVertex = this.vehicleGraph.vertexSet().stream().filter(v -> v.getVertexId() == Integer.MAX_VALUE).collect(Collectors.toList()).get(0); 
		for(VehicleVertex vertex : this.vehicleGraph.vertexSet())
		{
			Assert.assertTrue(vertex.getLabels().isEmpty());
		}
		
		this.blocksGenerated = new ArrayList<Block>(); 
		this.queue = new ArrayList<VehicleVertex>(); 
		
		this.generateAllVariables = false; 
		this.allowedLineChange = allowedLineChange; 
		this.useSubNetwork = useSubNetwork; 
		
		initialization(); 
		
		algorithm(); 
	}
	
	private void initialization()
	{
		VehicleREF initialREF = new VehicleREF(this.vehicleType, null, null, null); 
		LabelVehicle initialLabel = new LabelVehicle(initialREF, null, null, null); 
		this.sourceVertex.getLabels().add(initialLabel); 
		this.queue.add(sourceVertex); 
	}
	
	private void algorithm()
	{
		while(!this.queue.isEmpty())
		{
			VehicleVertex selectedVertex = this.queue.get(0); 
			
			Set<VehicleArc> outgoingArcs = this.vehicleGraph.outgoingEdgesOf(selectedVertex).stream().collect(Collectors.toSet()); 
			
			if(!this.allowedLineChange && selectedVertex.getTrip() != null)
			{
				outgoingArcs = selectArc(selectedVertex, outgoingArcs); 
			}
			
			for(LabelVehicle label : selectedVertex.getLabels())
			{
				if(!label.isLabelVehicleVisited())
				{
					for(VehicleArc outgoingArc : outgoingArcs)
					{
						
						VehicleVertex successorVertex = outgoingArc.getSuccessorVertex(); 
							
						VehicleREF newREF = new VehicleREF(this.vehicleType, label.getUpdatedResources(), outgoingArc, successorVertex);  
						if(newREF.isValid())
						{
							if(successorVertex.getVertexId() == Integer.MAX_VALUE && (newREF.getUpdatedReducedCost() <= -0.01  || this.generateAllVariables))
							{
								LabelVehicle newLabel = new LabelVehicle(newREF, label, selectedVertex, outgoingArc);
								successorVertex.getLabels().add(newLabel); 
										
								if(!queue.contains(successorVertex))
								{
									this.queue.add(successorVertex); 
								}
							}
							else if(successorVertex.getVertexId() != Integer.MAX_VALUE)
							{
								LabelVehicle newLabel = new LabelVehicle(newREF, label, selectedVertex, outgoingArc); 
								if(this.generateAllVariables)
								{
									successorVertex.getLabels().add(newLabel); 
								}
								else
								{
									checkDomination(successorVertex, newLabel); 
								}
									
								if(!queue.contains(successorVertex))
								{
									this.queue.add(successorVertex); 
								}
							}
						}
					}
				}
					
				label.labelVehicleVisited();
			}
		
			this.queue.remove(selectedVertex); 
		}
		
		reterievePaths(); 
	}
	
	private Set<VehicleArc> selectArc(VehicleVertex selectedVertex, Set<VehicleArc> outgoingArcs)
	{
		Set<VehicleArc> selectedArc = new HashSet<VehicleArc>(); 
		
		for(VehicleArc arc : outgoingArcs)
		{
			if(arc.getSuccessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip().getLineNumber() == selectedVertex.getTrip().getLineNumber())
			{
				selectedArc.add(arc); 
			}
			else if(arc.getSuccessorVertex().getTrip() == null)
			{
				selectedArc.add(arc); 
			}
		}
		
		return selectedArc; 
	}
	
	private void checkDomination(VehicleVertex vehicleVertex, LabelVehicle newLabel)
	{
		List<LabelVehicle> existingLabels = vehicleVertex.getLabels(); 
		List<LabelVehicle> existingLabelsToBeRemoved = new ArrayList<LabelVehicle>(); 
		VehicleREF newREF = newLabel.getUpdatedResources(); 
		
		if(!existingLabels.isEmpty())
		{
			boolean notToAddLabel = false;
			for(LabelVehicle existingLabel : existingLabels)
			{
				VehicleREF existingREF = existingLabel.getUpdatedResources(); 
				
				List<Boolean> dominatingDecisions = new ArrayList<Boolean>(); 
				
				/*
				 * Check if reduced cost of the new label is dominated
				 */
				boolean dominatedReducedCost = false; 
				if(newREF.getUpdatedReducedCost() >= existingREF.getUpdatedReducedCost())
				{
					dominatedReducedCost = true; 
				}
				dominatingDecisions.add(dominatedReducedCost);
			
				/*
				 * Check if max distance without recharging is dominated
				 */
				if(!this.useSubNetwork)
				{
					if(this.vehicleType.getMaximumDistanceWithoutRecharging() > 0)
					{
						boolean dominatedMaxDistanceWithoutRefueling = false; 
						if(newREF.getUpdatedDistanceWithoutRecharging() >= existingREF.getUpdatedDistanceWithoutRecharging())
						{
							dominatedMaxDistanceWithoutRefueling = true;
						}
						dominatingDecisions.add(dominatedMaxDistanceWithoutRefueling); 
					}
				}
				
				
				
				boolean dominated = true; 
				boolean removeExistingLabel = true; 
				 /*
				  * Check if the new label is dominated i.e., all its resources are dominated by resources of the existing label
				  */
				for(Boolean decision : dominatingDecisions)
				{
					if(decision.equals(false))
					{
						dominated = false;
						break; 
					}
				}
				
				/*
				 * Check if the existing label is dominated by the new label
				 */
				for(Boolean decision : dominatingDecisions)
				{
					if(decision.equals(true))
					{
						removeExistingLabel = false;
						break; 
					}
				}
				
				if(dominated)
				{
					notToAddLabel = true; 
				}
				else if(removeExistingLabel)
				{
					existingLabelsToBeRemoved.add(existingLabel); 
				}
			}
			
			if(!notToAddLabel)
			{
				vehicleVertex.getLabels().add(newLabel); 
			}
			
			for(LabelVehicle removeLabel : existingLabelsToBeRemoved)
			{
				vehicleVertex.getLabels().remove(removeLabel); 
			}
		}
		else
		{
			vehicleVertex.getLabels().add(newLabel); 
		}
	}
	
	private void reterievePaths()
	{
		List<LabelVehicle> labelsAtSink = new ArrayList<LabelVehicle>(this.sinkVertex.getLabels()); 
		//System.out.println("Final labels = " + labelsAtSink.size());
		if(labelsAtSink.size() > 500 && !this.generateAllVariables)
		{
			if(labelsAtSink.size() > 1000 && this.useSubNetwork)
			{
				Collections.sort(labelsAtSink);
				//labelsAtSink = labelsAtSink.subList(0, 1000);
				labelsAtSink = selectComplementaryColumns(labelsAtSink);
			}
			else
			{
				Collections.sort(labelsAtSink);
				labelsAtSink = labelsAtSink.subList(0, 500);
			}
			
		}
		
		List<VehicleVertex> vehicleVertices = new ArrayList<VehicleVertex>();
		List<VehicleArc> vehicleArcs = new ArrayList<VehicleArc>(); 
		
		for(LabelVehicle finalLabel : labelsAtSink)
		{
			boolean stop = false; 
			double totalCost = 0.0; 
			LabelVehicle currentLabel = finalLabel;
			vehicleVertices = new ArrayList<VehicleVertex>(); 
			vehicleArcs = new ArrayList<VehicleArc>();
			VehicleVertex currentVertex = sinkVertex; 
			while(!stop)
			{
				if(currentVertex.getTrip() != null)
				{
					vehicleVertices.add(currentVertex); 
					totalCost = totalCost + currentVertex.getTotalCostOfVertex(); 
				}
				
				LabelVehicle previousLabel = currentLabel.getSourceLabel(); 
				VehicleVertex previousVertex = currentLabel.getSourceVehicleVertex(); 
				
				if(previousLabel != null && previousVertex != null)
				{
					VehicleArc currentArc = currentLabel.getExtendedVehicleArc(); 
					vehicleArcs.add(currentArc); 
					totalCost = totalCost + currentArc.getTotalCostOfArc(); 
					
					currentLabel = previousLabel; 
					currentVertex = previousVertex; 
				}
				else
				{
					stop = true; 
				}
				
			}
			
			
			List<Trip> trips = new ArrayList<Trip>(); 
			List<BlockActivity> blockElements = new ArrayList<BlockActivity>(); 
			List<Deadrun> deadruns = new ArrayList<Deadrun>();
			List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
			vehicleVertices.forEach(v -> {
				trips.add(v.getTrip());
				blockElements.add(v.getBlockActivity());  
			});
			
			Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedTrips().containsAll(trips));
			Assert.assertTrue(trips.containsAll(finalLabel.getUpdatedResources().getUpdatedTrips()));
			
			 
			vehicleArcs.forEach(a -> {
				if(!a.getDeadrunsOnEdge().isEmpty())
				{
					deadruns.addAll(a.getDeadrunsOnEdge());
				}
				
				if(!a.getBlockActivitiesOnEdge().isEmpty())
				{
					blockElements.addAll(a.getBlockActivitiesOnEdge());
				}
				
				if(a.getIdleTimeOnArc() != null)
				{
					idleTimes.add(a.getIdleTimeOnArc()); 
				}
			});
			Collections.sort(blockElements);
			
			for(IdleTime idleTime : idleTimes)
			{
				Optional<BlockActivity> idleActivity = blockElements.stream().filter(b -> b.getDepartureNode().equals(idleTime.getNode()) && b.getArrivalNode().equals(idleTime.getNode()) && b.getDepartureTime() == idleTime.getDepartureTime() && 
						b.getArrivalTime() == idleTime.getArrivalTime()).findFirst();
				Assert.assertTrue(idleActivity.isPresent());
			}
			
			Block intblock = new Block(this.vehicleType, trips, deadruns, idleTimes, blockElements); 
			Assert.assertTrue(Math.abs(intblock.getTotalCostOfBlock() - totalCost) <= 1e-6);
			
			if(validateBlock(intblock))
			{
				this.blocksGenerated.add(intblock);
			}
			
			
			/*if(!this.generateAllVariables)
			{
				double rhs = 0; 

				for(Trip trip : trips)
				{
					rhs = rhs + this.dualValuesOfTripIDs.get(trip); 
				}
				
				double reducedCost = (intblock.getDistance()*this.vehicleType.getCostPerkm())  + this.vehicleType.getFixedCost() - rhs; 
			
				Assert.assertTrue("Error in calculation of reduced cost", Math.abs(reducedCost-finalLabel.getUpdatedResources().getUpdatedReducedCost()) < 0.001);
			}*/
		}
	}
	
	private List<LabelVehicle> selectComplementaryColumns(List<LabelVehicle> labelsAtSink)
	{
		Set<Trip> tripsCovered = new HashSet<Trip>(); 
		List<LabelVehicle> selectedLabels = new ArrayList<LabelVehicle>();  
		Collections.sort(labelsAtSink);
		
		for(LabelVehicle label : labelsAtSink)
		{
			if(tripsCovered.isEmpty())
			{
				selectedLabels.add(label); 
				tripsCovered.addAll(label.getUpdatedResources().getUpdatedTrips()); 
			}
			else
			{
				Set<Trip> tripsInLabel = new HashSet<Trip>(); 
				tripsInLabel.addAll(label.getUpdatedResources().getUpdatedTrips()); 
				tripsInLabel.retainAll(tripsCovered); 
				
				if(tripsInLabel.size() <= 5)
				{
					selectedLabels.add(label); 
					tripsCovered.addAll(label.getUpdatedResources().getUpdatedTrips()); 
				}
			}
		}
		
		return selectedLabels; 
		
	}
	
	private boolean validateBlock(Block intblock)
	{
		double distanceToRefueling = 0; 
		for(BlockActivity blockElement : intblock.getBlockActivities())
		{
			if(blockElement.getActivity().equals("Recharging"))
			{
				distanceToRefueling = 0; 
			}
			else
			{
				distanceToRefueling = distanceToRefueling + blockElement.getDistance(); 
			}
			
			if(this.vehicleType.getMaximumDistanceWithoutRecharging() > 0 && distanceToRefueling > this.vehicleType.getMaximumDistanceWithoutRecharging())
			{
				System.out.println("Block invlaid because of maximum distance without refueling");
				throw new IllegalArgumentException();
				
			}
		}
		
		if(intblock.getBlockActivities().size() > 1)
		{
			for(int i = 0; i < intblock.getBlockActivities().size()-1 ; i++)
			{
				if(intblock.getBlockActivities().get(i).getArrivalTime() != intblock.getBlockActivities().get(i+1).getDepartureTime())
				{
					System.out.println("The block activities are not continuous with respect to time.");
					intblock.getBlockActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
	                throw new IllegalArgumentException();
				}
				 
				if(!intblock.getBlockActivities().get(i).getArrivalNode().equals(intblock.getBlockActivities().get(i+1).getDepartureNode()))
				{
					System.out.println("The block activities are not continuous with respect to space");
	                intblock.getBlockActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
	                throw new IllegalArgumentException();
				}
			}
		}
		
		
		return true; 
	}

}
