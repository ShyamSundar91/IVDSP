package Lagrangian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import BranchPriceDriver.BranchAndBoundDriver;
import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.GraphCopy;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.DriverSubproblem;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class ColumnGeneration 
{
	private List<Trip> allTrips; 
	private Set<Deadrun> allDeadruns; 
	private Set<IdleTime> allIdleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs;
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private List<VehicleArc> arcsInSolution; 
	private List<Block> initialBlocksInSolution; 
	private List<Duty> intitialAndGeneratedDuties; 
	private double initialUpperBound; 
	
	@Getter
	private Map<Trip, Double> tripMultipliers; 
	@Getter
	private Map<Deadrun, Double> deadrunMultipliers; 
	@Getter
	private Map<IdleTime, Double> idleTimeMulitpliers; 
	
	private List<Duty> currentDutiesAdded; 
	
	public ColumnGeneration(List<Trip> allTrips, Set<Deadrun> allDeadruns, Set<IdleTime> allIdleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> initialBlocksInSolution, List<Duty> intitialAndGeneratedDuties, double initialUpperBound) throws IloException
	{
		this.allTrips = allTrips; 
		this.allDeadruns = allDeadruns; 
		this.allIdleTimes = allIdleTimes; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		this.initialBlocksInSolution = new ArrayList<Block>(initialBlocksInSolution); 
		this.intitialAndGeneratedDuties = new ArrayList<Duty>(intitialAndGeneratedDuties); 
		this.initialUpperBound = initialUpperBound; 
		this.arcsInSolution = new ArrayList<VehicleArc>(); 
		
		this.currentDutiesAdded = new ArrayList<Duty>(); 
		
		sequential(new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>()); 
		
		initializeMultipliers(); 
		
		algorithm(); 
	}
	
	private void sequential(Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers) throws IloException
	{
		this.initialUpperBound = 0; 
		
		VehicleTypeDepot vehicleTypeDepot = this.vehicleGraphs.keySet().iterator().next(); 
		SingleDepotVehicleScheduling sdvsp = new SingleDepotVehicleScheduling(this.allTrips, deadrunMultipliers, idleTimeMultipliers, this.vehicleGraphs.get(vehicleTypeDepot)); 
		this.initialUpperBound = this.initialUpperBound + sdvsp.getObjective(); 
		/*this.arcsInSolution.addAll(sdvsp.getArcsInSolution()); 
		double totalCost = 0; 
		List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
		for(VehicleArc start : starts)
		{
			boolean stop = false; 
			VehicleArc current = start; 
			while(!stop)
			{
				totalCost = totalCost + current.getTotalCostOfArc(); 
				
				if(current.getSuccessorVertex().getTrip() != null)
				{
					Trip trip = current.getSuccessorVertex().getTrip(); 
					totalCost = totalCost + current.getSuccessorVertex().getTotalCostOfVertex(); 
					current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
				}
				else
				{
					stop = true; 
				}
			}
		}*/
		
	
		Set<Deadrun> deadrunsInSolution = sdvsp.getDeadrunsInSolution(); 
		Set<IdleTime> idleTimesInSolution = sdvsp.getIdleTimesInSolution(); 
		
		GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, this.allTrips, this.allTrips, deadrunsInSolution, idleTimesInSolution); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
		BranchAndBoundDriver bb = new BranchAndBoundDriver(this.allTrips, this.initialBlocksInSolution,  deadrunsInSolution, idleTimesInSolution, driverGraphsCopy, new HashMap<Duty, Integer>(), false); 
		this.initialUpperBound = this.initialUpperBound + bb.getObjective(); 
		//totalCost = totalCost + bb.getObjective(); 
		//System.out.println("Total objective = " + totalCost);
		this.intitialAndGeneratedDuties.clear();
		this.intitialAndGeneratedDuties.addAll(bb.getDutiesGenerated()); 
		System.out.println("Initial upper bound = " + this.initialUpperBound);
	}
	
	private void initializeMultipliers()
	{
		this.tripMultipliers = new HashMap<Trip, Double>(); 
		this.deadrunMultipliers = new HashMap<Deadrun, Double>(); 
		this.idleTimeMulitpliers = new HashMap<IdleTime, Double>(); 
		
		for(Trip trip : this.allTrips)
		{
			this.tripMultipliers.put(trip, 0.0); 
		}
		
		for(Deadrun deadrun : this.allDeadruns)
		{
			this.deadrunMultipliers.put(deadrun, 0.0); 
		}
		
		for(IdleTime idleTime : this.allIdleTimes)
		{
			this.idleTimeMulitpliers.put(idleTime, 0.0); 
		}
	}
	
	private void algorithm() throws IloException
	{
		int status = 0; 
		int iteration = 0; 
		while(status != 1)
		{
			System.out.println("************************ Driver column generation ************************");
			System.out.println("Iteration number = " + iteration);
			System.out.println("Number of duties = " + this.intitialAndGeneratedDuties.size());
			IntegratedSubgradientAlgorithm subgradient = new IntegratedSubgradientAlgorithm(this.allTrips, this.allDeadruns, this.allIdleTimes, this.tripMultipliers, this.deadrunMultipliers, this.idleTimeMulitpliers, this.vehicleGraphs, this.initialBlocksInSolution, this.intitialAndGeneratedDuties, this.initialUpperBound);
			System.out.println("Lagrangian lower bound = " + subgradient.getLagrangianLowerBound());
			double bound = subgradient.getLagrangianLowerBound(); 
			for(Duty duty : this.currentDutiesAdded)
			{
				bound = bound + duty.getTotalCostOfDuty(); 
				for(Trip trip : duty.getTripsInDuty())
				{
					bound = bound - this.tripMultipliers.get(trip); 
				}
				
				for(Deadrun deadrun : duty.getDeadrunsInDuty())
				{
					bound = bound - this.deadrunMultipliers.get(deadrun); 
				}
				
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					bound = bound - this.idleTimeMulitpliers.get(idleTime); 
				}
			}
			System.out.println("Estimated bound = " + bound);
			status = generateDriverColumns();
			iteration++; 
			
			if(status == 1)
			{
				this.arcsInSolution.clear();
				this.arcsInSolution.addAll(subgradient.getVehicleArcsInSolution()); 
				double totalCost = 0; 
				List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
				for(VehicleArc start : starts)
				{
					boolean stop = false; 
					VehicleArc current = start; 
					while(!stop)
					{
						totalCost = totalCost + current.getTotalCostOfArc(); 
						
						if(current.getSuccessorVertex().getTrip() != null)
						{
							Trip trip = current.getSuccessorVertex().getTrip(); 
							totalCost = totalCost + current.getSuccessorVertex().getTotalCostOfVertex(); 
							current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
						}
						else
						{
							stop = true; 
						}
					}
				}
				
				GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, this.allTrips, this.allTrips, subgradient.getDeadrunsInSolution(), subgradient.getIdleTimesInSolution()); 
				Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
				BranchAndBoundDriver bb = new BranchAndBoundDriver(this.allTrips, this.initialBlocksInSolution,  subgradient.getDeadrunsInSolution(), subgradient.getIdleTimesInSolution(), driverGraphsCopy, new HashMap<Duty, Integer>(), false); 
				totalCost = totalCost + bb.getObjective(); 
				System.out.println("Total objective = " + totalCost);
			}
		}
		

		
		
		/*Set<Deadrun> deadrunsInSolution = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInSolution = new HashSet<IdleTime>(); 
		double obj = 0; 
		for(Block block : this.initialBlocksInSolution)
		{
			deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
			idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
			obj = obj + block.getTotalCostOfBlock(); 
		}
		
		Map<Duty, Integer> duties = new HashMap<Duty, Integer>(); 
		/*for(Duty duty : this.intitialAndGeneratedDuties)
		{
			duties.put(duty, 0); 
		}
		
		for(DutyTypeDepot dutyTypeDepot : this.driverGraphs.keySet())
		{
			Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
			DefaultDirectedGraph<DriverVertex, DriverArc> graph = this.driverGraphs.get(dutyTypeDepot); 
			for(DriverArc arc : graph.edgeSet())
			{
				if(arc.getDeadrun() != null && !deadrunsInSolution.contains(arc.getDeadrun()))
				{
					arcsToRemove.add(arc); 
				}
				
				if(arc.getIdleTimeOnArc() != null && !idleTimesInSolution.contains(arc.getIdleTimeOnArc()))
				{
					arcsToRemove.add(arc); 
				}
			}
			
			graph.removeAllEdges(arcsToRemove); 
		}
		
		BranchAndBoundDriver bb = new BranchAndBoundDriver(this.allTrips, this.initialBlocksInSolution,  deadrunsInSolution, idleTimesInSolution, this.driverGraphs, duties, false); 
		obj = obj + bb.getObjective(); 
		System.out.println("Total objective = " + obj);*/
	}
	
	private int generateDriverColumns()
	{
		int status = 0; 
		
		Map<Trip, Double> tempTripMultipliers = new HashMap<Trip, Double>(); 
		Map<Deadrun, Double> tempDeadrunMultipliers = new HashMap<Deadrun, Double>(); 
		Map<IdleTime, Double> tempIdleTimeMultipliers = new HashMap<IdleTime, Double>(); 
		
		for(Trip trip : this.allTrips)
		{
			tempTripMultipliers.put(trip, this.tripMultipliers.get(trip)); 
		}
		
		for(Deadrun deadrun : this.allDeadruns)
		{
			tempDeadrunMultipliers.put(deadrun, this.deadrunMultipliers.get(deadrun));
		}
		
		for(IdleTime idleTime : this.allIdleTimes)
		{
			tempIdleTimeMultipliers.put(idleTime, this.idleTimeMulitpliers.get(idleTime));
		}
		
		for(Duty duty : this.intitialAndGeneratedDuties)
		{
			double redCost = duty.getTotalCostOfDuty(); 
			for(Trip trip : duty.getTripsInDuty())
			{
				redCost = redCost - tempTripMultipliers.get(trip); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				redCost = redCost - tempDeadrunMultipliers.get(deadrun); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				redCost = redCost - tempIdleTimeMultipliers.get(idleTime); 
			}
			
			if(redCost < -0.001)
			{
				double delta = redCost/(double)(duty.getTripsInDuty().size() + duty.getDeadrunsInDuty().size() + duty.getIdleTimesInDuty().size()); 
				for(Trip trip : duty.getTripsInDuty())
				{
					double newVal = tempTripMultipliers.get(trip) + delta;
					tempTripMultipliers.replace(trip, newVal); 
				}
				
				for(Deadrun deadrun : duty.getDeadrunsInDuty())
				{
					double newVal = tempDeadrunMultipliers.get(deadrun) + delta;
					tempDeadrunMultipliers.replace(deadrun, newVal); 
				}
				
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					double newVal = tempIdleTimeMultipliers.get(idleTime) + delta; 
					tempIdleTimeMultipliers.replace(idleTime, newVal); 
				}
			}
		}
		
		Map<Deadrun, Double> deadrunUpperLimitDuals = new HashMap<Deadrun, Double>(); 
		for(Deadrun deadrun : this.allDeadruns)
		{
			deadrunUpperLimitDuals.put(deadrun, 0.0); 
		}
		
		DriverSubproblem subProblem = new DriverSubproblem(0, this.driverGraphs, tempTripMultipliers, tempDeadrunMultipliers, deadrunUpperLimitDuals, tempIdleTimeMultipliers, new ArrayList<Trip>(), new HashSet<Deadrun>(), new HashSet<IdleTime>(), true, false); 
		List<Duty> dutiesGenerated = subProblem.getDutiesGenerated();
		if(!dutiesGenerated.isEmpty())
		{
			for(Duty duty : dutiesGenerated)
			{
				Assert.assertTrue(!this.intitialAndGeneratedDuties.contains(duty));
			}
			this.intitialAndGeneratedDuties.addAll(dutiesGenerated); 
			this.currentDutiesAdded.clear();
			this.currentDutiesAdded.addAll(dutiesGenerated); 
		}
		else
		{
			status = 1; 
		}
		
		return status; 
	}
	

}
