package Lagrangian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceVehicle.BranchAndBoundVehicle;
import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.GraphCopy;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class IntegratedSubgradientAlgorithm 
{
	private List<Trip> allTrips; 
	private Set<Deadrun> allDeadruns; 
	private Set<IdleTime> allIdleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy; 
	
	private List<Block> initialBlocksInSolution;
	@Getter
	private Set<Deadrun> deadrunsInSolution; 
	@Getter
	private Set<IdleTime> idleTimesInSolution;
	@Getter
	private List<VehicleArc> vehicleArcsInSolution; 
	private List<Duty> initialAndGeneratedDuties; 
	private double upperBound; 
	private double lowerBound;
	@Getter
	private double lagrangianLowerBound; 
	private double decayRate; 
	private int noImprovement; 
	private Map<Trip, Double> tripMultipliers; 
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMulitpliers;  
	
	private Map<Trip, Integer> tripSubgradient; 
	private Map<Deadrun, Integer> deadrunSubgradient;
	private Map<IdleTime, Integer> idleTimeSubgradinet; 
	
	public IntegratedSubgradientAlgorithm(List<Trip> allTrips, Set<Deadrun> allDeadruns, Set<IdleTime> allIdleTimes, Map<Trip, Double> tripMultipliers, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMulitpliers, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, List<Block> initialBlocksInSolution, List<Duty> initialAndGeneratedDuties, double upperBound) throws IloException
	{
		this.allTrips = allTrips; 
		this.allDeadruns = allDeadruns; 
		this.allIdleTimes = allIdleTimes; 
		this.tripMultipliers = tripMultipliers; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMulitpliers = idleTimeMulitpliers; 
		
		this.initialBlocksInSolution = initialBlocksInSolution; 
		this.initialAndGeneratedDuties = initialAndGeneratedDuties;  
		this.upperBound = upperBound; 
		this.vehicleGraphs = vehicleGraphs; 
		
		this.lowerBound = 0; 
		this.lagrangianLowerBound = -Double.MAX_VALUE;  
		this.decayRate = 2; 
		this.noImprovement = 0; 
		
		this.deadrunsInSolution = new HashSet<Deadrun>(); 
		this.idleTimesInSolution = new HashSet<IdleTime>();
		this.vehicleArcsInSolution = new ArrayList<VehicleArc>(); 
		
		this.tripSubgradient = new HashMap<Trip, Integer>(); 
		this.deadrunSubgradient = new HashMap<Deadrun, Integer>(); 
		this.idleTimeSubgradinet = new HashMap<IdleTime, Integer>(); 
		
		subgradient();
	}
	
	private void initializeSubgradients(Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution)
	{
		for(Trip trip : this.allTrips)
		{
			this.tripSubgradient.put(trip, 1); 
		}
		
		for(Deadrun deadrun : this.deadrunMultipliers.keySet())
		{
			if(deadrunsInSolution.contains(deadrun))
			{
				this.deadrunSubgradient.put(deadrun, 1); 
			}
			else
			{
				this.deadrunSubgradient.put(deadrun, 0); 
			}
		}
		
		for(IdleTime idleTime : this.idleTimeMulitpliers.keySet())
		{
			if(idleTimesInSolution.contains(idleTime))
			{
				this.idleTimeSubgradinet.put(idleTime, 1); 
			}
			else
			{
				this.idleTimeSubgradinet.put(idleTime, 0); 
			}
		}
	}
	
	
	private void subgradient() throws IloException
	{
		boolean stop = false; 
		int iterationNumber = 0; 
		 
		
		while(!stop)
		{
			
			vehicleSceduling(); 
			
			initializeSubgradients(this.deadrunsInSolution, this.idleTimesInSolution); 
		
			DriverLagrangianSolver dspLagSol = new DriverLagrangianSolver(this.allTrips, this.initialAndGeneratedDuties, this.deadrunsInSolution, this.idleTimesInSolution, this.tripMultipliers, this.deadrunMultipliers, this.idleTimeMulitpliers, this.tripSubgradient, this.deadrunSubgradient, this.idleTimeSubgradinet); 
			this.lowerBound = this.lowerBound + dspLagSol.getLowerBound(); 
			System.out.println("Lower bound = " + this.lowerBound);
			if(this.lowerBound > this.lagrangianLowerBound)
			{
				this.lagrangianLowerBound = this.lowerBound; 
				noImprovement = 0; 
			}
			else
			{
				noImprovement++; 
			}

			stop = terminationCriteria(iterationNumber); 
			
			updateMultipliers(dspLagSol.getTripSubgradient(), dspLagSol.getDeadrunSubgradient(), dspLagSol.getIdleTimeSubgradinet()); //Update multipliers
			
			iterationNumber++; 
		}
	}
	
	private void updateMultipliers(Map<Trip, Integer> tripSubgradient, Map<Deadrun, Integer> deadrunSubgradient, Map<IdleTime, Integer> idleTimeSubgradient)
	{
		double stepSizeOfTrips = 0.0; 
		double stepSizeOfDeadruns = 0.0; 
		double stepSizeOfIdleTimes = 0.0; 
		
		double denominatorOfTrips = 0.0; 
		double denominatorOfDeadruns = 0.0; 
		double denominatorOfIdleTimes = 0.0; 
		
		for(Trip trip : tripSubgradient.keySet())
		{
			denominatorOfTrips = denominatorOfTrips + Math.pow(tripSubgradient.get(trip), 2); 
		}
		
		for(Deadrun deadrun : deadrunSubgradient.keySet())
		{
			denominatorOfDeadruns = denominatorOfDeadruns + Math.pow(deadrunSubgradient.get(deadrun), 2); 
		}
		
		for(IdleTime idleTime : idleTimeSubgradient.keySet())
		{
			denominatorOfIdleTimes = denominatorOfIdleTimes + Math.pow(idleTimeSubgradient.get(idleTime), 2); 
		}
		
		if(denominatorOfTrips >= 1)
		{
			stepSizeOfTrips = (this.decayRate*(this.upperBound-this.lowerBound))/denominatorOfTrips; 
		}
		
		if(denominatorOfDeadruns >= 1)
		{
			stepSizeOfDeadruns = (this.decayRate*(this.upperBound-this.lowerBound))/denominatorOfDeadruns; 
		}
		
		if(denominatorOfIdleTimes >= 1)
		{
			stepSizeOfIdleTimes = (this.decayRate*(this.upperBound-this.lowerBound))/denominatorOfIdleTimes; 
		}
		
		for(Trip trip : this.tripMultipliers.keySet())
		{
			double newVal = Math.max(0.0, this.tripMultipliers.get(trip) + (stepSizeOfTrips*tripSubgradient.get(trip))); 
			this.tripMultipliers.replace(trip, newVal); 
		}
		
		for(Deadrun deadrun : this.deadrunMultipliers.keySet())
		{
			double newVal = Math.max(0.0, this.deadrunMultipliers.get(deadrun) + (stepSizeOfDeadruns*deadrunSubgradient.get(deadrun))); 
			this.deadrunMultipliers.replace(deadrun, newVal); 
		}
		
		for(IdleTime idleTime : this.idleTimeMulitpliers.keySet())
		{
			double newVal = Math.max(0.0, this.idleTimeMulitpliers.get(idleTime) + (stepSizeOfIdleTimes*idleTimeSubgradient.get(idleTime))); 
			this.idleTimeMulitpliers.replace(idleTime, newVal); 
		}
		
	}
	
	private boolean terminationCriteria(int iterationNumber)
	{
		if(iterationNumber >= 500)
		{
			return true; 
		}
		
		double change = ((this.upperBound - this.lagrangianLowerBound)/this.upperBound) * 100.00; 
		if(change <= 0.001)
		{
			return true; 
		}
		
		if(noImprovement >= 10)
		{
			this.decayRate = this.decayRate/2.0; 
			this.noImprovement = 0; 
		}
		
		if(this.decayRate < 0.01)
		{
			return true; 
		}
		
		return false; 
		
	}
	
	private void vehicleSceduling() throws IloException
	{
		this.lowerBound = 0; 
		this.deadrunsInSolution.clear();
		this.idleTimesInSolution.clear();
		this.vehicleArcsInSolution.clear();
		
		//Destroy vehicle schedule
		/*Map<Block, Double> costOfBlocks = new HashMap<Block, Double>(); 
		for(Block block : this.initialBlocksInSolution)
		{
			double cost = block.getTotalCostOfBlock(); 
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				cost = cost + this.deadrunMultipliers.get(deadrun); 
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				cost = cost + this.idleTimeMulitpliers.get(idleTime); 
			}
			costOfBlocks.put(block, cost); 
		}
		
		 // Create a list from elements of HashMap 
        List<Map.Entry<Block, Double> > list = 
               new LinkedList<Map.Entry<Block, Double> >(costOfBlocks.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<Block, Double> >() { 
            public int compare(Map.Entry<Block, Double> o1,  
                               Map.Entry<Block, Double> o2) 
            { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        Map<Block, Double> temp = new LinkedHashMap<Block, Double>(); 
        for (Map.Entry<Block, Double> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
    
        int numberofBlocksToDestory = Math.max(2, (int)(0.2*this.initialBlocksInSolution.size())); 
        List<Block> candidatesToDestroy = new ArrayList<Block>();
        for(Block block : temp.keySet())
        {
        	candidatesToDestroy.add(block); 
        	if(candidatesToDestroy.size() >= (2*numberofBlocksToDestory)) break; 
        }
        
        Collections.shuffle(candidatesToDestroy);
        candidatesToDestroy = candidatesToDestroy.subList(0, numberofBlocksToDestory); 
        System.out.println("Number of blocks destroyed = " + candidatesToDestroy.size());
        
        Map<Block, Integer> initialBlocks = new HashMap<Block, Integer>(); 
        for(Block block : this.initialBlocksInSolution)
        {
        	if(candidatesToDestroy.contains(block))
        	{
        		initialBlocks.put(block, 1); 
        	}
        	else
        	{
        		initialBlocks.put(block, 0); 
        	}
        }
        
       List<Trip> uncoveredVehicleTrips = new ArrayList<Trip>(); 
        for(Block block : candidatesToDestroy)
        {
        	uncoveredVehicleTrips.addAll(block.getTripsInBlock()); 
        }
		
		GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, new HashMap<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>>(), this.allTrips, this.allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>());
		this.vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		
		BranchAndBoundVehicle bbVehicle = new BranchAndBoundVehicle(this.allTrips, this.vehicleGraphsCopy, initialBlocks, this.deadrunMultipliers, this.idleTimeMulitpliers, false);
	
		this.initialBlocksInSolution = new ArrayList<Block>();
		this.initialBlocksInSolution.addAll(bbVehicle.getBlocksInSolution()); 
		for(Block block : initialBlocksInSolution)
		{
			this.deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
			this.idleTimesInSolution.addAll(block.getIdleTimesInBlock());  
		}
		this.lowerBound = bbVehicle.getObjective();*/
		
		 
		VehicleTypeDepot vehicleTypeDepot = this.vehicleGraphs.keySet().iterator().next(); 
		SingleDepotVehicleScheduling sdvsp = new SingleDepotVehicleScheduling(this.allTrips, this.deadrunMultipliers, this.idleTimeMulitpliers, this.vehicleGraphs.get(vehicleTypeDepot)); 
		this.lowerBound = this.lowerBound + sdvsp.getObjective(); 
		this.deadrunsInSolution.addAll(sdvsp.getDeadrunsInSolution()); 
		this.idleTimesInSolution.addAll(sdvsp.getIdleTimesInSolution()); 
		this.vehicleArcsInSolution.addAll(sdvsp.getArcsInSolution()); 
		
	}
}
