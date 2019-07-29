package BranchPriceVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class BranchingDecisionVehicle 
{
	private List<Trip> trips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers;
	private Map<Block, Integer> initialBlocksAndGenerated; 
	private boolean earlyTermination; 
	@Getter
	private BBNodeVehicle childNode; 
	public BranchingDecisionVehicle(BBNodeVehicle parentNode) throws IloException
	{
		this.trips = parentNode.getTrips(); 
		this.vehicleGraph = parentNode.getVehicleGraph(); 
		this.deadrunMultipliers = parentNode.getDeadrunMultipliers(); 
		this.idleTimeMultipliers = parentNode.getIdleTimeMultipliers(); 
		this.initialBlocksAndGenerated = parentNode.getInitialBlocksAndGenerated(); 
		this.earlyTermination = parentNode.isEarlyTermination(); 
		
		List<Block> blocksForChildNode = variablesToFix(parentNode.getFractionalValuesOfBlockVariables()); 
		removeTripsOfFixedBlocks(blocksForChildNode); 
		createChildNode(); 
	}
	
	private List<Block> variablesToFix(Map<Block, Double> fractionalValuesOfBlockVariables)
	{
		List<Block> blocksForChildNode = new ArrayList<Block>(); 
		
		for(Block block : fractionalValuesOfBlockVariables.keySet())
		{
			if(this.initialBlocksAndGenerated.get(block) != 1)
			{
				if(fractionalValuesOfBlockVariables.get(block) >= 0.8)
				{
					blocksForChildNode.add(block); 
					System.out.println("Fix block " + block.getBlockId() + " with value " + fractionalValuesOfBlockVariables.get(block));
				}
			}
		}
		
		if(blocksForChildNode.isEmpty())
		{
			Block closest = null; 
			double max = 0; 
			for(Block block : fractionalValuesOfBlockVariables.keySet())
			{
				if(this.initialBlocksAndGenerated.get(block) != 1)
				{
					if(fractionalValuesOfBlockVariables.get(block) > max)
					{
						closest = block; 
						max = fractionalValuesOfBlockVariables.get(block); 
					}
				}
			}
			
			blocksForChildNode.add(closest);
			System.out.println("Fix block " + closest.getBlockId() + " with value " + max);
		}
		
		for(Block block : blocksForChildNode)
		{
			this.initialBlocksAndGenerated.replace(block, 1); 
		}
		
		return blocksForChildNode; 
	}
	
	private void removeTripsOfFixedBlocks(List<Block> blocksForChildNode)
	{
		List<Trip> tripsInFixedBlocks = new ArrayList<Trip>(); 
		for(Block block : blocksForChildNode)
		{
			tripsInFixedBlocks.addAll(block.getTripsInBlock()); 
		}
		
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleGraph.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> graph = this.vehicleGraph.get(vehicleTypeDepot); 
			Set<VehicleVertex> tripVertices = graph.vertexSet().stream().filter(v -> v.getTrip() != null).collect(Collectors.toSet()); 
			
			for(VehicleVertex tripVertex : tripVertices)
			{
				if(tripsInFixedBlocks.contains(tripVertex.getTrip()))
				{
					graph.removeVertex(tripVertex); 
				}
			}
		}
			
	}
	
	private void createChildNode() throws IloException
	{
		this.childNode = new BBNodeVehicle(this.trips, this.vehicleGraph, this.initialBlocksAndGenerated, this.deadrunMultipliers, this.idleTimeMultipliers, this.earlyTermination); 
	}

}
