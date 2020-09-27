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
	
	private int determinismParameter; 
	private List<Block> blocksInSolution;
	private List<Duty> dutiesInSolution;
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	private List<Block> blocksToBeRemoved;
	private List<Duty> dutiesToBeRemoved; 
	private List<Trip> uncoveredTripsOfVehicle; 
	private List<Trip> uncoveredTripsOfDriver; 
	private Random rnd; 
	private double degreeOfDutyDestruction;
	private double degreeOfSequentialDestruction; 
	private double degreeOfIntegratedDestruction; 
	private int noImprovement; 
	
	public DestroyMethod(int iteration, int chosenDestroyMethod, List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, 
			Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, List<Block> blocksInSolution, List<Duty> dutiesInSolution, double degreeOfDutyDestruction, double degreeOfSequentialDestruction, double degreeOfIntegratedDestruction, int noImprovement)
	{
		this.trips = trips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.degreeOfDutyDestruction = degreeOfDutyDestruction; 
		this.degreeOfSequentialDestruction = degreeOfSequentialDestruction; 
		this.degreeOfIntegratedDestruction = degreeOfIntegratedDestruction; 
		
		this.blocksInSolution = new ArrayList<Block>(blocksInSolution); 
		this.dutiesInSolution = new ArrayList<Duty>(dutiesInSolution); 
		this.deadrunsInSolution = new HashSet<Deadrun>(); 
		this.idleTimesInSolution = new HashSet<IdleTime>(); 
		this.uncoveredTripsOfVehicle = new ArrayList<Trip>(); 
		this.uncoveredTripsOfDriver = new ArrayList<Trip>(); 
		this.iteration = iteration; 
		this.noImprovement = noImprovement; 
		this.rnd = new Random(this.iteration); 
		this.determinismParameter = 4; 
		
		int maxIter = 500; 
		if(this.trips.size() > 200)
		{
			maxIter = 1000;
		}
		double additionalDegree = 0;
		
		
		if(chosenDestroyMethod == 0 || chosenDestroyMethod == 2)
		{
		    /*if(this.iteration <= maxIter)
            {
		        worstRemovalOfDuties(this.degreeOfDutyDestruction);  
            }
		    else*/
		    {
		        randomRemovalOfDuties(this.degreeOfDutyDestruction); 
		    }
		}
		else if(chosenDestroyMethod == 1 || chosenDestroyMethod == 3)
		{
		    /*if(this.iteration <= maxIter)
		    {
		        worstRemovalOfBlocks(this.degreeOfSequentialDestruction); 
		    }
		    else*/
		    {
		        randomRemovalOfBlocks(this.degreeOfSequentialDestruction); 
		    }
		    
		}
		else if(chosenDestroyMethod == 4)
		{
			if(this.iteration <= maxIter)
			{
			    worstRemovalOfBlocksAndDuties(); 
			}
			else
			{
				if(this.trips.size() < 500)
				{
					additionalDegree = 0.05; 
				}
					
				double destruction = this.degreeOfIntegratedDestruction + additionalDegree; 
				randomRemovalOfBlocks(destruction); 
			}	    
		}
		
		
		
		
		System.out.println("Number of blocks removed = " + this.blocksToBeRemoved.size());
		System.out.println("Number of duties removed = " + this.dutiesToBeRemoved.size());
		
		getTripsUncovered(this.blocksToBeRemoved, this.dutiesToBeRemoved);
		this.blocksInSolution.removeAll(this.blocksToBeRemoved); 
		this.dutiesInSolution.removeAll(this.dutiesToBeRemoved); 
		
		getDeadrunsAndIdleTimes();
		
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
	
	
	private void randomRemovalOfDuties(double dutyDestruction)
	{
		this.dutiesToBeRemoved = new ArrayList<Duty>(); 
		System.out.println("Number of duties in solution = " + this.dutiesInSolution.size());
		System.out.println("Degree of destruction = " + dutyDestruction);
		int numberOfDutiesToDestroy = Math.max(2, (int)(dutyDestruction*this.dutiesInSolution.size())); 
		Collections.shuffle(this.dutiesInSolution/*, this.rnd*/);
		this.dutiesToBeRemoved.addAll(this.dutiesInSolution.subList(0, numberOfDutiesToDestroy)); 	
		
		this.blocksToBeRemoved = new ArrayList<Block>(); 
	}
	
	private void worstRemovalOfDuties(double dutyDestruction)
	{
	    this.dutiesToBeRemoved = new ArrayList<Duty>(); 
        System.out.println("Number of duties in solution = " + this.dutiesInSolution.size());
        System.out.println("Degree of destruction = " + dutyDestruction);
        Collections.sort(this.dutiesInSolution);
        Collections.reverse(this.dutiesInSolution);
        
        int numberOfDutiesToDestroy = Math.max(2, (int)(this.degreeOfIntegratedDestruction*this.dutiesInSolution.size())); 
        
        int numberOfDutiesRemoved = 0; 
        List<Duty> tempDutiesInSolution = new ArrayList<Duty>(this.dutiesInSolution); 
        while(numberOfDutiesRemoved < numberOfDutiesToDestroy)
        {
            double y = Math.random(); 
            int positionOfDuty = (int)(Math.pow(y, this.determinismParameter)*tempDutiesInSolution.size());
            this.dutiesToBeRemoved.add(tempDutiesInSolution.get(positionOfDuty)); 
            tempDutiesInSolution.remove(positionOfDuty); 
            numberOfDutiesRemoved++; 
        }
        
        this.blocksToBeRemoved = new ArrayList<Block>();
	}
	
	private void randomRemovalOfBlocks(double sequentialDestruction)
	{
		this.blocksToBeRemoved = new ArrayList<Block>();
		
		int numberOfBlocksToRemove = Math.max(2, (int)(sequentialDestruction*this.blocksInSolution.size())); 
		System.out.println("Degree of destruction = " + sequentialDestruction);
		Collections.shuffle(this.blocksInSolution/*, this.rnd*/); 
		this.blocksToBeRemoved.addAll(this.blocksInSolution.subList(0, numberOfBlocksToRemove)); 
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
	}
	
	private void worstRemovalOfBlocks(double sequentialDestruction)
	{
	    this.blocksToBeRemoved = new ArrayList<Block>();
	    Collections.sort(this.blocksInSolution);
        Collections.reverse(this.blocksInSolution);
        int numberOfBlocksToRemove = Math.max(2, (int)(sequentialDestruction*this.blocksInSolution.size())); 
        System.out.println("Degree of destruction = " + sequentialDestruction);
        
        int numberOfBlocksRemoved = 0; 
        List<Block> tempBlocksInSolution = new ArrayList<Block>(this.blocksInSolution); 
        while(numberOfBlocksRemoved < numberOfBlocksToRemove)
        {
            double y = Math.random(); 
            int positionOfBlock = (int)(Math.pow(y, this.determinismParameter)*tempBlocksInSolution.size());
            this.blocksToBeRemoved.add(tempBlocksInSolution.get(positionOfBlock)); 
            tempBlocksInSolution.remove(positionOfBlock); 
            numberOfBlocksRemoved++; 
        }
        
        this.dutiesToBeRemoved = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
	}
	
	private void worstRemovalOfBlocksAndDuties()
	{
		this.dutiesToBeRemoved = new ArrayList<Duty>(); 
		System.out.println("Number of duties in solution = " + this.dutiesInSolution.size());
		
		Collections.sort(this.dutiesInSolution);
		Collections.reverse(this.dutiesInSolution);
		
		int numberOfDutiesToDestroy = Math.max(2, (int)(this.degreeOfIntegratedDestruction*this.dutiesInSolution.size())); 
		List<Duty> candidates = this.dutiesInSolution.subList(0, numberOfDutiesToDestroy); 
		Collections.shuffle(candidates/*, this.rnd*/);
		Duty selectedDuty = candidates.get(0); 
		
		this.dutiesToBeRemoved.add(selectedDuty); 
		
		this.blocksToBeRemoved = removeBlocksBasedOnRemovedDuties(this.dutiesToBeRemoved); 
		
		List<Block> remainingBlocks = new ArrayList<Block>(); 
		remainingBlocks.addAll(this.blocksInSolution); 
		remainingBlocks.removeAll(this.blocksToBeRemoved); 
		
		Collections.sort(remainingBlocks);
		Collections.reverse(remainingBlocks);
		if(!remainingBlocks.isEmpty())
		{
			int numberOfBlocksToRemove = (int)(this.degreeOfIntegratedDestruction * remainingBlocks.size()); 
			List<Block> candidatesBlocks = remainingBlocks.subList(0, numberOfBlocksToRemove); 
			Collections.shuffle(candidatesBlocks);
			Block selectedBlock = candidatesBlocks.get(0); 
			this.blocksToBeRemoved.add(selectedBlock); 
			Collections.shuffle(remainingBlocks);
			
			for(Block block : remainingBlocks)
			{
				if(this.blocksToBeRemoved.size() >= numberOfBlocksToRemove)
				{
					break; 
				}
				
				if(!this.blocksToBeRemoved.contains(block))
				{
					this.blocksToBeRemoved.add(block); 
				}
			}
			
			List<Duty> otherDuties = removeDutiesBasedOnRemovedBlocks(this.blocksToBeRemoved); 
			for(Duty duty : otherDuties)
			{
				if(!this.dutiesToBeRemoved.contains(duty))
				{
					this.dutiesToBeRemoved.add(duty); 
				}
			}
		}
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
		
		Set<Deadrun> deadrunsInRemovedDuties = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRemovedDuties = new HashSet<IdleTime>(); 
		List<Trip> tripsInRemovedDuties = new ArrayList<Trip>(); 
		
		for(Duty duty : dutiesToBeRemoved)
		{
			deadrunsInRemovedDuties.addAll(duty.getDeadrunsInDuty()); 
			idleTimesInRemovedDuties.addAll(duty.getIdleTimesInDuty()); 
			tripsInRemovedDuties.addAll(duty.getTripsInDuty());
		}
		
		for(Block block : this.blocksInSolution)
		{
			boolean added = false; 
			
			for(Trip trip : block.getTripsInBlock())
			{
				if(tripsInRemovedDuties.contains(trip))
				{
					blocksToBeRemoved.add(block);
					added = true; 
					break;
				}
			}
			
			if(!added)
			{
				for(Deadrun deadrun : block.getDeadrunsInBlock())
				{
					if(deadrunsInRemovedDuties.contains(deadrun))
					{
						blocksToBeRemoved.add(block);
						added = true; 
						break; 
					}
				}
			}
		
			
			if(!added)
			{
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					if(idleTimesInRemovedDuties.contains(idleTime))
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
