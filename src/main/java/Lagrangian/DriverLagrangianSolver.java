package Lagrangian;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Data.Trip;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

public class DriverLagrangianSolver 
{
	private List<Trip> allTrips; 
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	
	private Map<Trip, Double> tripMultipliers; 
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers; 
	
	private List<Duty> initialAndGeneratedDuties; 
	
	@Getter
	private Map<Trip, Integer> tripSubgradient; 
	@Getter
	private Map<Deadrun, Integer> deadrunSubgradient;
	@Getter
	private Map<IdleTime, Integer> idleTimeSubgradinet; 
	@Getter
	private double lowerBound; 
	public DriverLagrangianSolver(List<Trip> allTrips, List<Duty> initialAndGeneratedDuties, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution, Map<Trip, Double> tripMultipliers, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, 
			Map<Trip, Integer> tripSubgradient, Map<Deadrun, Integer> deadrunSubgradient, Map<IdleTime, Integer> idleTimeSubgradinet)
	{
		this.allTrips = allTrips; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		this.initialAndGeneratedDuties = initialAndGeneratedDuties; 
		
		this.tripMultipliers = tripMultipliers; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.tripSubgradient = tripSubgradient; 
		this.deadrunSubgradient = deadrunSubgradient; 
		this.idleTimeSubgradinet = idleTimeSubgradinet; 
		this.lowerBound = 0;
		
 
		calculateReducedCostOfDuties(); 
	}
	
	
	private void calculateReducedCostOfDuties()
	{
		for(Duty duty : this.initialAndGeneratedDuties)
		{
			double redCost = duty.getTotalCostOfDuty(); 
			for(Trip trip : duty.getTripsInDuty())
			{
				redCost = redCost - this.tripMultipliers.get(trip); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				redCost = redCost - this.deadrunMultipliers.get(deadrun); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				redCost = redCost - this.idleTimeMultipliers.get(idleTime); 
			}
			
			if(redCost < 0.00)
			{
				for(Trip trip : duty.getTripsInDuty())
				{
					int newVal = this.tripSubgradient.get(trip) - 1 ;
					this.tripSubgradient.replace(trip, newVal); 
				}
				
				for(Deadrun deadrun : duty.getDeadrunsInDuty())
				{
					int newVal = this.deadrunSubgradient.get(deadrun) - 1; 
					this.deadrunSubgradient.replace(deadrun, newVal); 
				}
				
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					int newVal = this.idleTimeSubgradinet.get(idleTime) - 1; 
					this.idleTimeSubgradinet.replace(idleTime, newVal); 
				}
				
				this.lowerBound =  this.lowerBound + redCost; 
			}
		}
		
		for(Trip trip : this.tripMultipliers.keySet())
		{
			this.lowerBound = this.lowerBound + this.tripMultipliers.get(trip); 
		}
	}
	
	

}
