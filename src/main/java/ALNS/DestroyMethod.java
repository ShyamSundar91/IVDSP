package ALNS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Data.VehicleType;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.DriverSubproblem;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class DestroyMethod 
{
	private int iteration; 
	private List<Trip> trips;
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers;
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private List<Block> blocksInSolution;
	private List<Duty> dutiesInSolution;
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	private List<Block> blocksToBeRemoved;
	private List<Duty> dutiesToBeRemoved; 
	private List<Trip> uncoveredTripsOfVehicle; 
	private List<Trip> uncoveredTripsOfDriver; 
	private Random rnd; 
	private double degreeOfDestruction; 
	private boolean initalSolutionLocalSearch; 
	
	public DestroyMethod(int iteration, int chosenDestroyMethod, List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, 
			Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, List<Block> blocksInSolution, List<Duty> dutiesInSolution, boolean initalSolutionLocalSearch)
	{
		this.trips = trips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 

		
		this.blocksInSolution = new ArrayList<Block>(blocksInSolution); 
		this.dutiesInSolution = new ArrayList<Duty>(dutiesInSolution); 
		this.deadrunsInSolution = new HashSet<Deadrun>(); 
		this.idleTimesInSolution = new HashSet<IdleTime>(); 
		this.uncoveredTripsOfVehicle = new ArrayList<Trip>(); 
		this.uncoveredTripsOfDriver = new ArrayList<Trip>(); 
		this.iteration = iteration; 
		this.rnd = new Random(this.iteration); 
		this.degreeOfDestruction = 0.1; 
		if(this.trips.size() < 250)
		{
			this.degreeOfDestruction = 0.3; 
		}
		else if(this.trips.size() < 500)
		{
			this.degreeOfDestruction = 0.2; 
		}
		System.out.println("Degree of destruction = " + this.degreeOfDestruction);
		
		this.initalSolutionLocalSearch = initalSolutionLocalSearch; 
		
		if(this.initalSolutionLocalSearch)
		{
			if(chosenDestroyMethod == 0)
			{
				randomRemovalOfDuties();     
			}
			else if(chosenDestroyMethod == 1)
			{
				randomRemovalOfBlocks(); 
			}
			else 
			{
				throw new IllegalArgumentException(); 
			}
		}
		else
		{
			if(chosenDestroyMethod == 0)
			{
				randomRemovalOfDuties();     
			}
			else if(chosenDestroyMethod == 1)
			{
				randomRemovalOfBlocks(); 
			}
			else if(chosenDestroyMethod == 2)
			{
				randomRemovalOfBlocks(); 
			}
		}
		
		
		
		System.out.println("Number of blocks removed = " + this.blocksToBeRemoved.size());
		System.out.println("Number of duties removed = " + this.dutiesToBeRemoved.size());
		
		getTripsUncovered(this.blocksToBeRemoved, this.dutiesToBeRemoved);
		this.blocksInSolution.removeAll(this.blocksToBeRemoved); 
		this.dutiesInSolution.removeAll(this.dutiesToBeRemoved); 
		
		getDeadrunsAndIdleTimes();
		
		/*for(Block block : blocksToBeRemoved)
		{
			System.out.println("Block to be removed**************");
			
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
		}
		
		for(Duty duty : dutiesToBeRemoved)
		{
			System.out.println("Duty to be removed*********");
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			}
		}*/
	}
	
	private void getDeadrunsAndIdleTimes()
	{
		for(Block block : this.blocksInSolution)
		{
			this.deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
			this.idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			this.deadrunsInSolution.addAll(duty.getDeadrunsInDuty()); 
			this.idleTimesInSolution.addAll(duty.getIdleTimesInDuty()); 
		}
	}
	
	
	private void randomRemovalOfDuties()
	{
		this.dutiesToBeRemoved = new ArrayList<Duty>(); 
		System.out.println("Number of duties in solution = " + this.dutiesInSolution.size());
		int numberOfDutiesToDestroy = Math.max(2, (int)(this.degreeOfDestruction*this.dutiesInSolution.size())); 
		Collections.shuffle(this.dutiesInSolution, this.rnd);
		this.dutiesToBeRemoved.addAll(this.dutiesInSolution.subList(0, numberOfDutiesToDestroy)); 	
		
		this.blocksToBeRemoved = new ArrayList<Block>(); 
	}
	
	private void randomRemovalOfBlocks()
	{
		this.blocksToBeRemoved = new ArrayList<Block>();
		
		int numberOfBlocksToRemove = Math.max(2, (int)(this.degreeOfDestruction*this.blocksInSolution.size())); 
		Collections.shuffle(this.blocksInSolution, this.rnd); 
		this.blocksToBeRemoved.addAll(this.blocksInSolution.subList(0, numberOfBlocksToRemove)); 
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
	}
	
	private void worstRemoval()
	{
		this.dutiesToBeRemoved = new ArrayList<Duty>();
		
		this.blocksToBeRemoved = new ArrayList<Block>(); 
	
		
		SequenceGeneration seqGen = new SequenceGeneration(this.trips, this.blocksInSolution, this.dutiesInSolution); 
		List<Sequence> allSequences = seqGen.getSequences(); 
		Collections.sort(allSequences);
		Collections.reverse(allSequences);
		
		int numberOfBlocksToDestroy = Math.max(2, (int)(this.degreeOfDestruction*this.blocksInSolution.size()));
		List<Sequence> candidates = allSequences.subList(0, numberOfBlocksToDestroy); 
		Collections.shuffle(candidates, this.rnd);
		Sequence selectedSequence = candidates.get(0); 
		allSequences.remove(selectedSequence); 
		
		this.blocksToBeRemoved.add(selectedSequence.getBlockCoveringSequence()); 
		int latestEndtTime = selectedSequence.getDutyCoveringSequence().getEndTime() + selectedSequence.getDutyCoveringSequence().getDutyType().getMaxDuration(); 
		int earliestStartTime = Math.max(0, selectedSequence.getDutyCoveringSequence().getStartTime() - selectedSequence.getDutyCoveringSequence().getDutyType().getMaxDuration()); 
		
		for(Sequence seq : allSequences)
		{
			if(!this.blocksToBeRemoved.contains(seq.getBlockCoveringSequence()))
			{
				 if(!seq.getDutyCoveringSequence().equals(selectedSequence.getDutyCoveringSequence()))
				 {
					 if(seq.getDutyCoveringSequence().getStartTime() >= earliestStartTime && seq.getDutyCoveringSequence().getEndTime() <= latestEndtTime)
					 {
						 this.blocksToBeRemoved.add(seq.getBlockCoveringSequence()); 
					 }
				 }
			}
			
			if(this.blocksToBeRemoved.size() >= numberOfBlocksToDestroy)
			{
				break; 
			}
		}
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
	}
	
	private List<Duty> removeDutiesBasedOnRemovedBlocks(List<Block> blocksToBeRemoved)
	{
		List<Duty> dutiesToBeRemoved = new ArrayList<Duty>(); 
		
		Set<Deadrun> deadrunsInRemovedBlocks = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRemovedBlocks = new HashSet<IdleTime>(); 
		List<Trip> tripsInRemovedBlocks = new ArrayList<Trip>(); 
		
		for(Block block : blocksToBeRemoved)
		{
			deadrunsInRemovedBlocks.addAll(block.getDeadrunsInBlock()); 
			idleTimesInRemovedBlocks.addAll(block.getIdleTimesInBlock()); 
			tripsInRemovedBlocks.addAll(block.getTripsInBlock()); 
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			boolean added = false; 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				if(tripsInRemovedBlocks.contains(trip))
				{
					dutiesToBeRemoved.add(duty);
					added = true; 
					break; 
				}
			}
			
			if(!added)
			{
				for(Deadrun deadrun : duty.getDeadrunsInDuty())
				{
					if(deadrunsInRemovedBlocks.contains(deadrun))
					{
						dutiesToBeRemoved.add(duty);
						added = true; 
						break; 
					}
				}
			}
			
			
			if(!added)
			{
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					if(idleTimesInRemovedBlocks.contains(idleTime))
					{
						dutiesToBeRemoved.add(duty);
						added = true; 
						break; 
					}
				}
			}
		}
		
		return dutiesToBeRemoved; 
	}
	
	private List<Block> removeBlocksBasedOnRemovedDuties(List<Duty> dutiesToBeRemoved)
	{
		List<Block> blocksToBeRemoved = new ArrayList<Block>(); 
		
		Set<Deadrun> deadrunsInRemovedBlocks = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRemovedBlocks = new HashSet<IdleTime>(); 
		for(Duty duty : dutiesToBeRemoved)
		{
			deadrunsInRemovedBlocks.addAll(duty.getDeadrunsInDuty()); 
			idleTimesInRemovedBlocks.addAll(duty.getIdleTimesInDuty()); 
		}
		
		for(Block block : this.blocksInSolution)
		{
			boolean added = false; 
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				if(deadrunsInRemovedBlocks.contains(deadrun))
				{
					blocksToBeRemoved.add(block);
					added = true; 
					break; 
				}
			}
			
			if(!added)
			{
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					if(idleTimesInRemovedBlocks.contains(idleTime))
					{
						blocksToBeRemoved.add(block);
						added = true; 
						break; 
					}
				}
			}
		}
		
		return blocksToBeRemoved; 
	}
	

	
	private void getTripsUncovered(List<Block> blocksToBeRemoved, List<Duty> dutiesToBeRemoved)
	{
		for(Block block : blocksToBeRemoved)
		{
			this.uncoveredTripsOfVehicle.addAll(block.getTripsInBlock()); 
		}

		for(Duty duty : dutiesToBeRemoved)
		{
			this.uncoveredTripsOfDriver.addAll(duty.getTripsInDuty()); 
		}
	}

}
