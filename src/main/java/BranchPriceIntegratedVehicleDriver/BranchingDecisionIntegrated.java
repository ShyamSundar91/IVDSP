package BranchPriceIntegratedVehicleDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

public class BranchingDecisionIntegrated 
{
	private List<Trip> trips;
	private Map<Block, Integer> blocksForChildNode; 
	private Map<Duty, Integer> dutiesForChildNode;   
	private Set<Deadrun> deadrunsForChildNode; 
	private Set<IdleTime> idleTimesForChildNode;
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private boolean earlyTermination; 
	private int iterationLimit; 
	
	@Getter
	private BBNodeIntegrated childNode; 
	
	public BranchingDecisionIntegrated(BBNodeIntegrated parentNode)
	{
		this.trips = parentNode.getTrips(); 
		this.vehicleGraphs = parentNode.getVehicleGraphs(); 
		this.driverGraphs = parentNode.getDriverGraphs(); 
		this.blocksForChildNode = parentNode.getInitialBlocksAndGenerated(); 
		this.dutiesForChildNode = parentNode.getInitialDutiesAndGenerated();  
		this.deadrunsForChildNode = parentNode.getDeadruns(); 
		this.idleTimesForChildNode = parentNode.getIdleTimes(); 
		this.earlyTermination = parentNode.isEarlyTermination(); 
		this.iterationLimit = parentNode.getIterationLimit(); 

		mixedVariableFixing(parentNode.getFractionalValuesOfBlocks(), parentNode.getFractionalValuesOfDuties()); 
		createChildNode(parentNode); 
		
	}
	
	private void mixedVariableFixing(Map<Block, Double> fractionalValuesOfBlocks, Map<Duty, Double> fractionalValuesOfDuties)
	{
		List<Block> blockVariables =  blockVariablesToFix(fractionalValuesOfBlocks); 
		if(!blockVariables.isEmpty())
		{
			removeTripsOfFixedBlocks(blockVariables); 
		}
		else
		{
			List<Duty> dutyVariables = dutyVariablesToFix(fractionalValuesOfDuties); 
			Assert.assertTrue(!dutyVariables.isEmpty());
			removeTripsDeadrunsIdleTimesOfFixedDuties(dutyVariables); 
		}
		
		/*List<Duty> dutyVariables = dutyVariablesToFix(fractionalValuesOfDuties);
		if(!dutyVariables.isEmpty())
		{
			removeTripsDeadrunsIdleTimesOfFixedDuties(dutyVariables); 
			
		}
		else
		{
			List<Block> blockVariables =  blockVariablesToFix(fractionalValuesOfBlocks); 
			removeTripsOfFixedBlocks(blockVariables); 
		}*/
	}
	
	private void createChildNode(BBNodeIntegrated parentNode)
	{
		for(Block block : this.blocksForChildNode.keySet())
		{
			block.resetBlockInMP();
		}
		
		for(Duty duty : this.dutiesForChildNode.keySet())
		{
			duty.resetDutyInMP();
		}
		
		this.childNode = new BBNodeIntegrated(this.blocksForChildNode, this.dutiesForChildNode, this.trips, this.deadrunsForChildNode, this.idleTimesForChildNode, this.vehicleGraphs, this.driverGraphs, this.earlyTermination, this.iterationLimit); 
	}
	
	private List<Block> blockVariablesToFix(Map<Block, Double> fractionalValuesOfBlockVariables)
	{
		List<Block> blocksForChildNode = new ArrayList<Block>(); 
		
		for(Block block : fractionalValuesOfBlockVariables.keySet())
		{
			if(this.blocksForChildNode.get(block) != 1)
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
				if(this.blocksForChildNode.get(block) != 1)
				{
					if(fractionalValuesOfBlockVariables.get(block) > max)
					{
						closest = block; 
						max = fractionalValuesOfBlockVariables.get(block); 
					}
				}
			}
			
			if(closest != null)
			{
				blocksForChildNode.add(closest);
				System.out.println("Fix block " + closest.getBlockId() + " with value " + max);
			}
		}
		
		for(Block block : blocksForChildNode)
		{
			this.blocksForChildNode.replace(block, 1); 
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
		
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleGraphs.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> graph = this.vehicleGraphs.get(vehicleTypeDepot); 
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
	
	private void interTripsFixingForBlock(Map<Block, Double> fractionalValuesOfBlockVariables)
	{
		double [][] fractionalTrips = new double[this.trips.size()][this.trips.size()];
		for(double [] row : fractionalTrips)
		{
			Arrays.fill(row, 0);
		}
		Map<Trip, Integer> mapTripIds =  new HashMap<Trip, Integer>(); 
		int id  = 0; 
		for(Trip trip : this.trips)
		{
			mapTripIds.put(trip, id); 
			id++; 
		}
		
		for(Block block : fractionalValuesOfBlockVariables.keySet())
		{
			List<Trip> tripsInBlock = block.getTripsInBlock(); 
			Collections.reverse(tripsInBlock);
			
			if(tripsInBlock.size() > 1)
			{
				for(int t1 = 0; t1 < tripsInBlock.size()-1; t1++)
				{
					for(int t2 = t1+1; t2 < tripsInBlock.size(); t2++)
					{
						Trip firstTrip = tripsInBlock.get(t1); 
						Trip secondTrip = tripsInBlock.get(t2); 
						
						Assert.assertTrue(secondTrip.getDepartureTime() >= firstTrip.getArrivalTime());
						
						int firstTripIndex = mapTripIds.get(firstTrip); 
						int secondTripIndex = mapTripIds.get(secondTrip); 
						
						fractionalTrips[firstTripIndex][secondTripIndex] = fractionalTrips[firstTripIndex][secondTripIndex] + fractionalValuesOfBlockVariables.get(block); 
					}
				}
			}
		}
		
		for(int t1 = 0; t1 < this.trips.size()-1 ; t1++)
		{
			for(int t2 = t1+1; t2 < this.trips.size(); t2++)
			{
			
			}
		}
		
	}
	
	private List<Duty> dutyVariablesToFix(Map<Duty, Double> fractionalValuesOfDutyVariables)
	{
		List<Duty> dutiesToFixForChildNode = new ArrayList<Duty>(); 
		
		for(Duty duty : fractionalValuesOfDutyVariables.keySet())
		{
			if(this.dutiesForChildNode.get(duty) != 1)
			{
				if(fractionalValuesOfDutyVariables.get(duty) >= 0.8)
				{
					dutiesToFixForChildNode.add(duty); 
					System.out.println("Fix duty " + duty.getDutyId() + " with value " + fractionalValuesOfDutyVariables.get(duty));
				}
			}
			
		}
		
		if(dutiesToFixForChildNode.isEmpty())
		{
			Duty closest = null; 
			double max = 0; 
			for(Duty duty : fractionalValuesOfDutyVariables.keySet())
			{
				if(this.dutiesForChildNode.get(duty) != 1)
				{
					if(fractionalValuesOfDutyVariables.get(duty) > max)
					{
						closest = duty; 
						max = fractionalValuesOfDutyVariables.get(duty); 
					}
				}
				
			}
			
			if(closest != null)
			{
				dutiesToFixForChildNode.add(closest);
				System.out.println("Fix duty " + closest.getDutyId() + " with value " + max);
			}
			
		}
		
		for(Duty duty : dutiesToFixForChildNode)
		{
			this.dutiesForChildNode.replace(duty, 1); 
		}
		
		return dutiesToFixForChildNode; 
	}
	
	private void removeTripsDeadrunsIdleTimesOfFixedDuties(List<Duty> dutiesToFix)
	{
		List<Trip> tripsInFixedDuties = new ArrayList<Trip>(); 
		List<Deadrun> deadrunsInFixedDuties = new ArrayList<Deadrun>(); 
		List<IdleTime> idleTimesInFixedDuties = new ArrayList<IdleTime>(); 
		
		for(Duty duty : dutiesToFix)
		{
			tripsInFixedDuties.addAll(duty.getTripsInDuty()); 
			deadrunsInFixedDuties.addAll(duty.getDeadrunsInDuty()); 
			idleTimesInFixedDuties.addAll(duty.getIdleTimesInDuty()); 
		}
		
		for(DutyTypeDepot dutyTypeDepot : this.driverGraphs.keySet())
		{
			DefaultDirectedGraph<DriverVertex, DriverArc> graph = this.driverGraphs.get(dutyTypeDepot); 
			Set<DriverArc> tripDeadIdleArcs = graph.edgeSet().stream().filter(a -> a.getTrip() != null || a.getDeadrun() != null || a.getIdleTimeOnArc() != null).collect(Collectors.toSet()); 
			
			for(DriverArc tripDeadIdleArc : tripDeadIdleArcs)
			{
				if(tripDeadIdleArc.getTrip() != null && tripsInFixedDuties.contains(tripDeadIdleArc.getTrip()))
				{
					graph.removeEdge(tripDeadIdleArc); 
				}
				else if(tripDeadIdleArc.getDeadrun() != null && deadrunsInFixedDuties.contains(tripDeadIdleArc.getDeadrun()))
				{
					graph.removeEdge(tripDeadIdleArc); 
				}
				else if(tripDeadIdleArc.getIdleTimeOnArc() != null && idleTimesInFixedDuties.contains(tripDeadIdleArc.getIdleTimeOnArc()))
				{
					graph.removeEdge(tripDeadIdleArc); 
				}
			}
		}		
	}

}
