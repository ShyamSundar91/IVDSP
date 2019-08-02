package BranchPriceDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceVehicle.BBNodeVehicle;
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
import ilog.concert.IloException;
import lombok.Getter;

public class BranchingDecisionDriver 
{
	private List<Trip> trips; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private Map<Duty, Integer> initialAndDutiesGenerated;
	private List<Block> blocksInSolution; 
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	private boolean earlyTermination;
	
	@Getter
	private BBNodeDriver childNode;
	
	public BranchingDecisionDriver(BBNodeDriver parentNode) throws IloException
	{
		this.trips = parentNode.getTrips(); 
		this.driverGraphs = parentNode.getDriverGraphs(); 
		this.initialAndDutiesGenerated = parentNode.getInitialAndDutiesGenerated(); 
		this.blocksInSolution = parentNode.getBlocksInSolution(); 
		this.deadrunsInSolution = parentNode.getDeadrunsInSolution(); 
		this.idleTimesInSolution = parentNode.getIdleTimesInSolution(); 
		this.earlyTermination = parentNode.isEarlyTermination(); 
		
		List<Duty> dutiesToFix = variablesToFix(parentNode.getFractionalValuesOfDutyVariables()); 
		removeTripsDeadrunsIdleTimesOfFixedDuties(dutiesToFix); 
		createChildNode(); 
	}
	
	private List<Duty> variablesToFix(Map<Duty, Double> fractionalValuesOfDutyVariables)
	{
		List<Duty> dutiesToFixForChildNode = new ArrayList<Duty>(); 
		
		for(Duty duty : fractionalValuesOfDutyVariables.keySet())
		{
			if(this.initialAndDutiesGenerated.get(duty) != 1)
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
				if(this.initialAndDutiesGenerated.get(duty) != 1)
				{
					if(fractionalValuesOfDutyVariables.get(duty) > max)
					{
						closest = duty; 
						max = fractionalValuesOfDutyVariables.get(duty); 
					}
				}
				
			}
			
			dutiesToFixForChildNode.add(closest);
			System.out.println("Fix duty " + closest.getDutyId() + " with value " + max);
		}
		
		for(Duty duty : dutiesToFixForChildNode)
		{
			this.initialAndDutiesGenerated.replace(duty, 1); 
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
	
	private void createChildNode() throws IloException
	{
		this.childNode = new BBNodeDriver(this.trips, this.blocksInSolution, this.deadrunsInSolution, this.idleTimesInSolution, this.driverGraphs, this.initialAndDutiesGenerated, this.earlyTermination); 
	}

}
