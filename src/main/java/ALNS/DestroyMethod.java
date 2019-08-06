package ALNS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	
	private Map<Trip, Double> tripVehicleDuals; 
	private Map<Trip, Double> tripDriverDuals; 
	private Map<Deadrun, Double> deadrunDuals;
	private Map<IdleTime, Double> idleTimeDuals; 
	
	public DestroyMethod(int iteration, int chosenDestroyMethod, List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, 
			Map<Trip, Double> tripVehicleDuals, Map<Trip, Double> tripDriverDuals, Map<Deadrun, Double> deadrunDuals, Map<IdleTime, Double> idleTimeDuals, List<Block> blocksInSolution, List<Duty> dutiesInSolution)
	{
		this.trips = trips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.tripVehicleDuals = tripVehicleDuals; 
		this.tripDriverDuals = tripDriverDuals; 
		this.deadrunDuals = deadrunDuals; 
		this.idleTimeDuals = idleTimeDuals; 
		
		this.blocksInSolution = new ArrayList<Block>(blocksInSolution); 
		this.dutiesInSolution = new ArrayList<Duty>(dutiesInSolution); 
		this.deadrunsInSolution = new HashSet<Deadrun>(); 
		this.idleTimesInSolution = new HashSet<IdleTime>(); 
		this.uncoveredTripsOfVehicle = new ArrayList<Trip>(); 
		this.uncoveredTripsOfDriver = new ArrayList<Trip>(); 
		this.iteration = iteration; 
		this.rnd = new Random(this.iteration); 
		this.degreeOfDestruction = 0.2; 
		
		
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
		/*int earliestStart = Math.max(0, this.dutiesInSolution.get(0).getStartTime() - this.dutiesInSolution.get(0).getDutyType().getMaxDuration()); 
		int latestEnd = this.dutiesInSolution.get(0).getEndTime() + this.dutiesInSolution.get(0).getDutyType().getMaxDuration();
		for(int i = 0; i < this.dutiesInSolution.size(); i++)
		{
			Duty candidate = this.dutiesInSolution.get(i); 
			if(candidate.getStartTime() >= earliestStart && candidate.getEndTime() <= latestEnd)
			{
				this.dutiesToBeRemoved.add(candidate); 
			}
			
			if(this.dutiesToBeRemoved.size() >= numberOfDutiesToDestroy)
			{
				break; 
			}
		}*/
		
		//Assert.assertTrue(this.dutiesToBeRemoved.size() == numberOfDutiesToDestroy);		
		
		this.blocksToBeRemoved = new ArrayList<Block>(); 
	}
	
	private void randomRemovalOfBlocks()
	{
		this.blocksToBeRemoved = new ArrayList<Block>();
		
		int numberOfBlocksToRemove = Math.max(2, (int)(this.degreeOfDestruction*this.blocksInSolution.size())); 
		Collections.shuffle(this.blocksInSolution, this.rnd); 
		this.blocksToBeRemoved.addAll(this.blocksInSolution.subList(0, numberOfBlocksToRemove)); 
		
		//List<Sequence> candidate = sequences.subList(0, 10); 
		//Collections.shuffle(candidate, this.rnd); 
		/*Sequence selectedSequence = sequences.get(0); 
		this.blocksToBeRemoved.add(selectedSequence.getBlockCoveringSequence()); 
		List<Trip> tripsInSelectedSequence = selectedSequence.getTripsInSequence(); 
		VehicleTypeDepot vehicleTypeDepot = this.vehicleGraphs.keySet().iterator().next(); 
		Set<VehicleVertex> tripVertices = this.vehicleGraphs.get(vehicleTypeDepot).vertexSet().stream().filter(v -> v.getTrip() != null).collect(Collectors.toSet()); 
		sequences.remove(selectedSequence); 
		Map<Sequence, Double> scoreSequences = new HashMap<Sequence, Double>(); 
		for(Sequence sequence : sequences)
		{
			double score = 0; 
			if(!sequence.getBlockCoveringSequence().equals(selectedSequence.getBlockCoveringSequence()))
			{ 
				for(Trip tripInSelected : tripsInSelectedSequence)
				{
					VehicleVertex tripInSelectedVertex = tripVertices.stream().filter(v -> v.getTrip().equals(tripInSelected)).findAny().get(); 
					List<Trip> tripsInOtherSequence = sequence.getTripsInSequence();
					for(Trip tripInOther : tripsInOtherSequence)
					{
						VehicleVertex tripInOtherVertex = tripVertices.stream().filter(v -> v.getTrip().equals(tripInOther)).findAny().get(); 
						
						if(this.vehicleGraphs.get(vehicleTypeDepot).containsEdge(tripInSelectedVertex, tripInOtherVertex))
						{
							score = score + this.vehicleGraphs.get(vehicleTypeDepot).getEdge(tripInSelectedVertex, tripInOtherVertex).getTotalCostOfArc(); 
						}
						else if(this.vehicleGraphs.get(vehicleTypeDepot).containsEdge(tripInOtherVertex, tripInSelectedVertex))
						{
							score = score + this.vehicleGraphs.get(vehicleTypeDepot).getEdge(tripInOtherVertex, tripInSelectedVertex).getTotalCostOfArc(); 
						}
						else
						{
							score = score + 100000; 
						}
					}
				}
				scoreSequences.put(sequence, score); 
			}
		}
		
		boolean stop = false; 
		while(!stop)
		{
			double minCost = Double.MAX_VALUE; 
			Sequence bestSequence = null; 
			for(Sequence sequence : scoreSequences.keySet())
			{
				if(scoreSequences.get(sequence) < minCost)
				{
					minCost = scoreSequences.get(sequence); 
					bestSequence = sequence; 
				}
			}
			
			if(bestSequence != null)
			{
				if(!this.blocksToBeRemoved.contains(bestSequence.getBlockCoveringSequence()))
				{
					this.blocksToBeRemoved.add(bestSequence.getBlockCoveringSequence()); 
				}
				scoreSequences.remove(bestSequence); 
				
				if(this.blocksToBeRemoved.size() >= numberOfBlocksToRemove)
				{
					stop = true; 
				}
			}
			else
			{
				stop = true; 
			}
		}
		
		
		/*for(Sequence sequenceToDestroy : sequencesToDestroy)
		{
			System.out.println("Other sequence = " );
			sequenceToDestroy.getTripsInSequence().forEach(t ->{
				System.out.print(t.getTripId() + ", ");
			});
			System.out.print("Block = " + sequenceToDestroy.getBlockCoveringSequence().getBlockId() + ", ");
			System.out.print("Duty = " + sequenceToDestroy.getDutyCoveringSequence().getDutyId() + ", ");
			System.out.print("Cost = " + sequenceToDestroy.getCostOfSequence());
			System.out.println();
			
			if(!this.blocksToBeRemoved.contains(sequenceToDestroy.getBlockCoveringSequence()))
			{
				this.blocksToBeRemoved.add(sequenceToDestroy.getBlockCoveringSequence()); 
			}
		}*/
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
	}
	
	private List<Duty> removeDutiesBasedOnRemovedBlocks(List<Block> blocksToBeRemoved)
	{
		List<Duty> dutiesToBeRemoved = new ArrayList<Duty>(); 
		
		Set<Deadrun> deadrunsInRemovedBlocks = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRemovedBlocks = new HashSet<IdleTime>(); 
		for(Block block : blocksToBeRemoved)
		{
			deadrunsInRemovedBlocks.addAll(block.getDeadrunsInBlock()); 
			idleTimesInRemovedBlocks.addAll(block.getIdleTimesInBlock()); 
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			boolean added = false; 
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				if(deadrunsInRemovedBlocks.contains(deadrun))
				{
					dutiesToBeRemoved.add(duty);
					added = true; 
					break; 
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
