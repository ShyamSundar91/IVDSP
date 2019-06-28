package Lagrangian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import Data.Trip;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class LagrangianSolverIVDSP 
{
	private List<Block> blocks; 
	private List<Duty> duties; 
	private List<Trip> trips; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	
	private Map<Trip, Double> tripVehicleMultipliers; 
	private Map<Trip, Double> tripDriverMultipliers; 
	private Map<Deadrun, Double> deadrunLowerLimitMultiplier; 
	private Map<Deadrun, Double> deadrunUpperLimitMultiplier; 
	private Map<IdleTime, Double> idleTimeMultiplier; 

	private Map<Trip, Double> tripVehicleSubgradient; 
	private Map<Trip, Double> tripDriverSubgradient; 
	private Map<Deadrun, Double> deadrunLowerLimitSubgradient; 
	private Map<Deadrun, Double> deadrunUpperLimitSubgradient; 
	private Map<IdleTime, Double> idleTimeSubgradient;
	
	private double lagrangianLowerBound; 
	private double lowerBound; 
	private double decayRate; 
	private int lastImprovedSolution; 
	
	private Map<Block, Double> reducedCostOfBlocks; 
	private Map<Duty, Double> reducedCostOfDuties;
	
	private Map<Trip, Double> originalTripVehicleMultipliers; 
	private Map<Trip, Double> originalTripDriverMultipliers; 
	private Map<Deadrun, Double> originalDeadrunLowerLimitMultiplier; 
	private Map<Deadrun, Double> originalDeadrunUpperLimitMultiplier; 
	private Map<IdleTime, Double> originalIdleTimeMultiplier; 
	
	public LagrangianSolverIVDSP(List<Block> blocks, List<Duty> duties, List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<Trip, Double> tripVehicleMultipliers,  Map<Trip, Double> tripDriverMultipliers, Map<Deadrun, Double> deadrunLowerLimitMultiplier, Map<Deadrun, Double> deadrunUpperLimitMultiplier, 
			Map<IdleTime, Double> idleTimeMultiplier)
	{
		this.blocks = blocks; 
		this.duties = duties; 
		this.trips = trips; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		 
		initialize(tripVehicleMultipliers, tripDriverMultipliers, deadrunLowerLimitMultiplier, deadrunUpperLimitMultiplier, idleTimeMultiplier); 
		
		subgradientAlgorithm(); 
	}
	
	private void initialize( Map<Trip, Double> tripVehicleMultipliers,  Map<Trip, Double> tripDriverMultipliers, Map<Deadrun, Double> deadrunLowerLimitMultiplier, Map<Deadrun, Double> deadrunUpperLimitMultiplier, Map<IdleTime, Double> idleTimeMultiplier)
	{
		this.tripVehicleMultipliers = new HashMap<Trip, Double>(); 
		this.tripDriverMultipliers = new HashMap<Trip, Double>(); 
		this.deadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.deadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.idleTimeMultiplier = new HashMap<IdleTime, Double>();
		
		this.tripVehicleSubgradient = new HashMap<Trip, Double>(); 
		this.tripDriverSubgradient = new HashMap<Trip, Double>(); 
		this.deadrunLowerLimitSubgradient = new HashMap<Deadrun, Double>(); 
		this.deadrunUpperLimitSubgradient = new HashMap<Deadrun, Double>(); 
		this.idleTimeSubgradient = new HashMap<IdleTime, Double>(); 
		
		for(Trip trip : tripVehicleMultipliers.keySet())
		{
			this.tripVehicleMultipliers.put(trip, tripVehicleMultipliers.get(trip)); 
		}
		
		for(Trip trip : tripDriverMultipliers.keySet())
		{
			this.tripDriverMultipliers.put(trip, tripDriverMultipliers.get(trip)); 
		}
		
		for(Deadrun deadrun : deadrunLowerLimitMultiplier.keySet())
		{
			this.deadrunLowerLimitMultiplier.put(deadrun, deadrunLowerLimitMultiplier.get(deadrun)); 
		}
		
		for(Deadrun deadrun : deadrunUpperLimitMultiplier.keySet())
		{
			this.deadrunUpperLimitMultiplier.put(deadrun, deadrunUpperLimitMultiplier.get(deadrun)); 
		}
		
		for(IdleTime idleTime : idleTimeMultiplier.keySet())
		{
			this.idleTimeMultiplier.put(idleTime, idleTimeMultiplier.get(idleTime)); 
		}
		
		for(Trip trip : this.trips)
		{
			this.tripVehicleSubgradient.put(trip, 1.0); 
			this.tripDriverSubgradient.put(trip, 1.0); 
		}
		
		for(Deadrun deadrun : this.deadruns)
		{
			this.deadrunLowerLimitSubgradient.put(deadrun, 0.0); 
			this.deadrunUpperLimitSubgradient.put(deadrun, 0.0); 
		}
		
		for(IdleTime idleTime : this.idleTimes)
		{
			this.idleTimeSubgradient.put(idleTime, 0.0); 
		}
		
		this.reducedCostOfBlocks = new HashMap<Block, Double>(); 
		this.reducedCostOfDuties = new HashMap<Duty, Double>(); 
		for(Block block : this.blocks)
		{
			this.reducedCostOfBlocks.put(block, 0.0); 
		}
		for(Duty duty : this.duties)
		{
			this.reducedCostOfDuties.put(duty, 0.0); 
		}
		
		this.lagrangianLowerBound = 0.0; 
		this.lowerBound = 0.0; 
		this.decayRate = 2; 
		this.lastImprovedSolution = 0; 
	}
	
	private void subgradientAlgorithm()
	{
		boolean stop = false; 
		int iter = 0; 
		
		while(!stop)
		{
			solve(iter); 
			
			updateMultipliers(); 
			
			stop = checkTermination(iter); 
			
			iter++; 
		}
		
		adaptMultipliers(); 
	}
	
	private void solve(int iter)
	{	
		double lb = 0.0; 
		double ub = 0.0; 
		List<Block> blocksInSolution = new ArrayList<Block>(); 
		List<Duty> dutiesInSolution = new ArrayList<Duty>(); 
		
		for(Block block : this.blocks)
		{
			double vehicleRhs = 0.0; 
			for(Trip trip : block.getTripsInBlock())
			{
				vehicleRhs = vehicleRhs + this.tripVehicleMultipliers.get(trip); 
			}
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				vehicleRhs = vehicleRhs - this.deadrunLowerLimitMultiplier.get(deadrun); 
				vehicleRhs = vehicleRhs - (2*this.deadrunUpperLimitMultiplier.get(deadrun)); 
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				vehicleRhs = vehicleRhs - this.idleTimeMultiplier.get(idleTime); 
			}
			
			double redCostVehicle = block.getLhs() - vehicleRhs; 
			if(redCostVehicle < 0.0)
			{
				blocksInSolution.add(block); 
				lb = lb + redCostVehicle; 
				ub = ub + block.getLhs(); 
				this.reducedCostOfBlocks.replace(block, redCostVehicle); 
			}
		}
		
		for(Duty duty : this.duties)
		{
			double dutyRhs = 0.0; 
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyRhs = dutyRhs + this.tripDriverMultipliers.get(trip); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				dutyRhs = dutyRhs + this.deadrunLowerLimitMultiplier.get(deadrun); 
				dutyRhs = dutyRhs + this.deadrunUpperLimitMultiplier.get(deadrun); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				dutyRhs = dutyRhs + this.idleTimeMultiplier.get(idleTime); 
			}
			
			double redCostDriver = duty.getTotalAmountPaid() - dutyRhs; 
			if(redCostDriver < 0.0)
			{
				dutiesInSolution.add(duty); 
				lb = lb + redCostDriver; 
				ub = ub + duty.getTotalAmountPaid(); 
				this.reducedCostOfDuties.replace(duty, redCostDriver); 
			}
		}
		
		double sum1 = this.tripVehicleMultipliers.entrySet().stream().mapToDouble(t -> t.getValue()).sum();
		double sum2 = this.tripDriverMultipliers.entrySet().stream().mapToDouble(t -> t.getValue()).sum(); 
		
		lb = lb + sum1 + sum2; 
		
		this.lagrangianLowerBound = lb; 
		//System.out.println(lb);
		if(lb > this.lowerBound)
		{
			this.lowerBound = lb; 
			this.lastImprovedSolution = iter; 
		}
		
		calculateSubgradientVectors(blocksInSolution, dutiesInSolution); 
	}
	
	private void calculateSubgradientVectors(List<Block> blocksInSolution, List<Duty> dutiesInSolution)
	{
		for(Trip trip : this.tripVehicleSubgradient.keySet())
		{
			this.tripVehicleSubgradient.replace(trip, 1.0); 
		}
		
		for(Trip trip : this.tripDriverSubgradient.keySet())
		{
			this.tripDriverSubgradient.replace(trip, 1.0); 
		}
		
		for(Deadrun deadrun : this.deadrunLowerLimitSubgradient.keySet())
		{
			this.deadrunLowerLimitSubgradient.replace(deadrun, 0.0); 
		}
		
		for(Deadrun deadrun : this.deadrunUpperLimitSubgradient.keySet())
		{
			this.deadrunUpperLimitSubgradient.replace(deadrun, 0.0); 
		}
		
		for(IdleTime idleTime : this.idleTimeSubgradient.keySet())
		{
			this.idleTimeSubgradient.replace(idleTime, 0.0); 
		}
		
		for(Block block : blocksInSolution)
		{
			for(Trip trip : block.getTripsInBlock())
			{
				double currentValue = this.tripVehicleSubgradient.get(trip); 
				double newValue =  currentValue - 1; 
				this.tripVehicleSubgradient.replace(trip, newValue); 
			}
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				double currentLowerLimitValue = this.deadrunLowerLimitSubgradient.get(deadrun); 
				double newLowerLimitValue = currentLowerLimitValue + 1; 
				this.deadrunLowerLimitSubgradient.replace(deadrun, newLowerLimitValue); 
				
				double currentUpperLimitValue = this.deadrunUpperLimitSubgradient.get(deadrun); 
				double newUpperLimitValue = currentUpperLimitValue + 2; 
				this.deadrunUpperLimitSubgradient.replace(deadrun, newUpperLimitValue); 
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				double currentIdleTimeValue = this.idleTimeSubgradient.get(idleTime); 
				double newIdleTimeValue = currentIdleTimeValue + 1; 
				this.idleTimeSubgradient.replace(idleTime, newIdleTimeValue); 
			}
		}
		
		for(Duty duty : dutiesInSolution)
		{
			for(Trip trip : duty.getTripsInDuty())
			{
				double currentValue = this.tripDriverSubgradient.get(trip); 
				double newValue =  currentValue - 1; 
				this.tripDriverSubgradient.replace(trip, newValue); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				double currentLowerLimitValue = this.deadrunLowerLimitSubgradient.get(deadrun); 
				double newLowerLimitValue = currentLowerLimitValue - 1; 
				this.deadrunLowerLimitSubgradient.replace(deadrun, newLowerLimitValue); 
				
				double currentUpperLimitValue = this.deadrunUpperLimitSubgradient.get(deadrun); 
				double newUpperLimitValue = currentUpperLimitValue - 1; 
				this.deadrunUpperLimitSubgradient.replace(deadrun, newUpperLimitValue); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				double currentIdleTimeValue = this.idleTimeSubgradient.get(idleTime); 
				double newIdleTimeValue = currentIdleTimeValue - 1; 
				this.idleTimeSubgradient.replace(idleTime, newIdleTimeValue); 
			}
		}
		
	}
	
	private void updateMultipliers()
	{
		for(Trip trip : this.trips)
		{
			double newTripVehicleMultiplier = this.tripVehicleMultipliers.get(trip) + (this.decayRate * this.tripVehicleSubgradient.get(trip)); 
			this.tripVehicleMultipliers.replace(trip, newTripVehicleMultiplier); 
			
			double newTripDriverMultiplier = Math.max(0.0, this.tripDriverMultipliers.get(trip) + (this.decayRate * this.tripDriverSubgradient.get(trip))); 
			this.tripDriverMultipliers.replace(trip, newTripDriverMultiplier); 
		}
		
		for(Deadrun deadrun : this.deadruns)
		{
			double newLowerLimitMultiplier = Math.max(0.0, this.deadrunLowerLimitMultiplier.get(deadrun) + (this.decayRate * this.deadrunLowerLimitSubgradient.get(deadrun))); 
			this.deadrunLowerLimitMultiplier.replace(deadrun, newLowerLimitMultiplier); 
			
			double newUpperLimitMultiplier = Math.min(0.0, this.deadrunUpperLimitMultiplier.get(deadrun) + (this.decayRate * this.deadrunUpperLimitSubgradient.get(deadrun))); 
			this.deadrunUpperLimitMultiplier.replace(deadrun, newUpperLimitMultiplier); 
		}
		
		for(IdleTime idleTime : this.idleTimes)
		{
			double newIdleTimeMultiplier = this.idleTimeMultiplier.get(idleTime) + (this.decayRate * this.idleTimeSubgradient.get(idleTime)); 
			this.idleTimeMultiplier.replace(idleTime, newIdleTimeMultiplier); 
		}
	}
	
	private void adaptMultipliers()
	{
		this.originalTripVehicleMultipliers = new HashMap<Trip, Double>();  
		this.originalTripDriverMultipliers = new HashMap<Trip, Double>(); 
		this.originalDeadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.originalDeadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.originalIdleTimeMultiplier = new HashMap<IdleTime, Double>();
		
		this.tripVehicleMultipliers.entrySet().forEach(t -> {
			this.originalTripVehicleMultipliers.put(t.getKey(), t.getValue()); 
		});
		this.tripDriverMultipliers.entrySet().forEach(t -> {
			this.originalTripDriverMultipliers.put(t.getKey(), t.getValue()); 
		});
		this.deadrunLowerLimitMultiplier.entrySet().forEach(d -> {
			this.originalDeadrunLowerLimitMultiplier.put(d.getKey(), d.getValue()); 
		});
		this.deadrunUpperLimitMultiplier.entrySet().forEach(d -> {
			this.originalDeadrunUpperLimitMultiplier.put(d.getKey(), d.getValue()); 
		});
		this.idleTimeMultiplier.entrySet().forEach(i -> {
			this.originalIdleTimeMultiplier.put(i.getKey(), i.getValue()); 
		});
		
		for(Block block : this.blocks)
		{
			if(this.reducedCostOfBlocks.get(block) < 0.0)
			{
				double delta = (this.reducedCostOfBlocks.get(block))/(double)(block.getTripsInBlock().size() - (3*block.getDeadrunsInBlock().size()) - block.getIdleTimesInBlock().size());
				for(Trip trip : block.getTripsInBlock())
				{
					double currentValue = this.tripVehicleMultipliers.get(trip); 
					double newValue =  currentValue + delta;
					this.tripVehicleMultipliers.put(trip, newValue); 
				}
				
				for(Deadrun deadrun : block.getDeadrunsInBlock())
				{
					double currentLowerValue = this.deadrunLowerLimitMultiplier.get(deadrun); 
					double newLowerValue = currentLowerValue + delta;
					this.deadrunLowerLimitMultiplier.put(deadrun, newLowerValue); 
					
					double currentUpperValue = this.deadrunUpperLimitMultiplier.get(deadrun); 
					double newUpperValue = currentUpperValue + delta; 
					this.deadrunUpperLimitMultiplier.put(deadrun, newUpperValue); 
				}
				
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					double currentIdleValue = this.idleTimeMultiplier.get(idleTime); 
					double newIdleTime = currentIdleValue + delta;
					this.idleTimeMultiplier.put(idleTime, newIdleTime); 
				}
				
				
				for(Block block2 : this.blocks)
				{
					double vehicleRhs = 0.0; 
					for(Trip trip : block2.getTripsInBlock())
					{
						vehicleRhs = vehicleRhs + this.tripVehicleMultipliers.get(trip); 
					}
					
					for(Deadrun deadrun : block2.getDeadrunsInBlock())
					{
						vehicleRhs = vehicleRhs - this.deadrunLowerLimitMultiplier.get(deadrun); 
						vehicleRhs = vehicleRhs - (2*this.deadrunUpperLimitMultiplier.get(deadrun)); 
					}
					
					for(IdleTime idleTime : block2.getIdleTimesInBlock())
					{
						vehicleRhs = vehicleRhs - this.idleTimeMultiplier.get(idleTime); 
					}
					
					double redCostVehicle = block2.getLhs() - vehicleRhs; 
					this.reducedCostOfBlocks.put(block2, redCostVehicle); 
				}
				
				for(Duty duty : this.duties)
				{
					double dutyRhs = 0.0; 
					for(Trip trip : duty.getTripsInDuty())
					{
						dutyRhs = dutyRhs + this.tripDriverMultipliers.get(trip); 
					}
					
					for(Deadrun deadrun : duty.getDeadrunsInDuty())
					{
						dutyRhs = dutyRhs + this.deadrunLowerLimitMultiplier.get(deadrun); 
						dutyRhs = dutyRhs + this.deadrunUpperLimitMultiplier.get(deadrun); 
					}
					
					for(IdleTime idleTime : duty.getIdleTimesInDuty())
					{
						dutyRhs = dutyRhs + this.idleTimeMultiplier.get(idleTime); 
					}
					
					double redCostDriver = duty.getTotalAmountPaid() - dutyRhs;
					this.reducedCostOfDuties.put(duty, redCostDriver); 
				}
			}
			
		}
		
		for(Duty duty : this.duties)
		{
			if(this.reducedCostOfDuties.get(duty) < 0.0)
			{
				double delta = (this.reducedCostOfDuties.get(duty))/(double)(duty.getTripsInDuty().size() + (2*duty.getDeadrunsInDuty().size()) + duty.getIdleTimesInDuty().size());
				for(Trip trip : duty.getTripsInDuty())
				{
					double currentValue = this.tripDriverMultipliers.get(trip); 
					double newValue =  currentValue + delta;
					this.tripDriverMultipliers.put(trip, newValue); 
				}
				
				for(Deadrun deadrun : duty.getDeadrunsInDuty())
				{
					double currentLowerValue = this.deadrunLowerLimitMultiplier.get(deadrun); 
					double newLowerValue = currentLowerValue + delta;
					this.deadrunLowerLimitMultiplier.put(deadrun, newLowerValue); 
					
					double currentUpperValue = this.deadrunUpperLimitMultiplier.get(deadrun); 
					double newUpperValue =  currentUpperValue + delta;
					this.deadrunUpperLimitMultiplier.put(deadrun, newUpperValue); 
				}
				
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					double currentIdleValue = this.idleTimeMultiplier.get(idleTime); 
					double newIdleValue = currentIdleValue + delta; 
					this.idleTimeMultiplier.put(idleTime, newIdleValue); 
				}
				
				for(Duty duty2 : this.duties)
				{
					double dutyRhs = 0.0; 
					for(Trip trip : duty2.getTripsInDuty())
					{
						dutyRhs = dutyRhs + this.tripDriverMultipliers.get(trip); 
					}
					
					for(Deadrun deadrun : duty2.getDeadrunsInDuty())
					{
						dutyRhs = dutyRhs + this.deadrunLowerLimitMultiplier.get(deadrun); 
						dutyRhs = dutyRhs + this.deadrunUpperLimitMultiplier.get(deadrun); 
					}
					
					for(IdleTime idleTime : duty2.getIdleTimesInDuty())
					{
						dutyRhs = dutyRhs + this.idleTimeMultiplier.get(idleTime); 
					}
					
					double redCostDriver = duty2.getTotalAmountPaid() - dutyRhs;
					this.reducedCostOfDuties.put(duty, redCostDriver); 
				}
				
				for(Block block : this.blocks)
				{
					double vehicleRhs = 0.0; 
					for(Trip trip : block.getTripsInBlock())
					{
						vehicleRhs = vehicleRhs + this.tripVehicleMultipliers.get(trip); 
					}
					
					for(Deadrun deadrun : block.getDeadrunsInBlock())
					{
						vehicleRhs = vehicleRhs - this.deadrunLowerLimitMultiplier.get(deadrun); 
						vehicleRhs = vehicleRhs - (2*this.deadrunUpperLimitMultiplier.get(deadrun)); 
					}
					
					for(IdleTime idleTime : block.getIdleTimesInBlock())
					{
						vehicleRhs = vehicleRhs - this.idleTimeMultiplier.get(idleTime); 
					}
					
					double redCostVehicle = block.getLhs() - vehicleRhs; 
					this.reducedCostOfBlocks.put(block, redCostVehicle); 
				}
			}
		}
		
		
		 
		/*for(Duty duty : this.reducedCostOfDuties.keySet())
		{
			//System.out.println("Cost = " + this.reducedCostOfDuties.get(duty));
			Assert.assertTrue(this.reducedCostOfDuties.get(duty) >= -1e-6);
		}
		*/
		/*for(Block block : this.reducedCostOfBlocks.keySet())
		{
			System.out.println("Cost = " + this.reducedCostOfBlocks.get(block));
			Assert.assertTrue(Math.rint(this.reducedCostOfBlocks.get(block)) >= -1e-6);
		}*/
		
		
	}
	
	private boolean checkTermination(int iter)
	{
		if(iter - this.lastImprovedSolution > 200)
		{
			this.decayRate = this.decayRate/2.0; 
			
			if(this.decayRate < 1e-3)
			{
				return true; 
			}
		}
		
		if(iter > 1000)
		{
			return true; 
		}
		
		
		return false; 
		
	}
}
