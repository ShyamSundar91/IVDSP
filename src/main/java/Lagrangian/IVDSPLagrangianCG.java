package Lagrangian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.DriverSubproblem;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class IVDSPLagrangianCG 
{

	private List<Trip> trips; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private Map<Trip, Double> tripVehicleMultipliers; 
	private Map<Trip, Double> tripDriverMultipliers; 
	private Map<Deadrun, Double> deadrunLowerLimitMultiplier; 
	private Map<Deadrun, Double> deadrunUpperLimitMultiplier; 
	private Map<IdleTime, Double> idleTimeMultiplier; 
	
	private List<Block> blocksGenerated; 
	private List<Duty> dutiesGenerated; 
	private List<List<Double>> lowerBounds; 
	public IVDSPLagrangianCG (List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs)
	{
		this.trips = trips; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		this.blocksGenerated = new ArrayList<Block>(); 
		this.dutiesGenerated = new ArrayList<Duty>(); 
		
		this.tripVehicleMultipliers = new HashMap<Trip, Double>(); 
		this.tripDriverMultipliers = new HashMap<Trip, Double>(); 
		this.deadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.deadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>(); 
		this.idleTimeMultiplier = new HashMap<IdleTime, Double>(); 
		
		this.trips.forEach(t -> {
			tripVehicleMultipliers.put(t, 0.0); 
			tripDriverMultipliers.put(t, 0.0); 
    	});
    	this.deadruns.forEach(d -> {
    		deadrunLowerLimitMultiplier.put(d, 0.0); 
    		deadrunUpperLimitMultiplier.put(d, 0.0); 
    	});
    	this.idleTimes.forEach(i -> {
    		idleTimeMultiplier.put(i, 0.0); 
    	});
    	
    	this.lowerBounds = new ArrayList<List<Double>>(); 
    	initalColumns(); 
    	
    	algorithm(); 
    	
    	for(List<Double> lb : this.lowerBounds)
    	{
    		System.out.println(lb.get(0) + "; " + lb.get(1) + "; " + lb.get(2));
    	}
	}
	
	private void initalColumns()
	{
		Map<Trip, Double> initialTripVehicleMultipliers = new HashMap<Trip, Double>(); 
		Map<Trip, Double> initialTripDriverMultipliers = new HashMap<Trip, Double>();
		this.trips.forEach(t -> {
			initialTripVehicleMultipliers.put(t, 1000.0); 
			initialTripDriverMultipliers.put(t, 500.0); 
		});
		
		VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(0, this.vehicleGraphs, initialTripVehicleMultipliers, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
		this.blocksGenerated.addAll(vehicleSubproblem.getBlocksGenerated());
		
		DriverSubproblem driverSubproblem = new DriverSubproblem(0, this.driverGraphs, initialTripDriverMultipliers, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
		this.dutiesGenerated.addAll(driverSubproblem.getDutiesGenerated()); 
		
	}
	
	private void algorithm()
	{
		int status = 0; 
		int iteration = 0; 
		
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iteration);
			System.out.println("Number of blocks = " + this.blocksGenerated.size());
			System.out.println("Number of duties = " + this.dutiesGenerated.size());
			
			LagrangianSolverIVDSP solver = new LagrangianSolverIVDSP(this.blocksGenerated, this.dutiesGenerated, this.trips, this.deadruns, this.idleTimes, this.tripVehicleMultipliers, this.tripDriverMultipliers, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier); 
			System.out.println("Subgradient lower bound = " + solver.getLowerBound());
			List<Double> iter = new ArrayList<Double>(); 
			iter.add((double)this.blocksGenerated.size()); 
			iter.add((double)this.dutiesGenerated.size()); 
			iter.add((double)solver.getLowerBound()); 
			this.lowerBounds.add(iter); 
			
			Map<Trip, Double> tempTripVehicleMultipliers = new HashMap<Trip, Double>(); 
			Map<Trip, Double> tempTripDriverMultipliers = new HashMap<Trip, Double>(); 
			Map<Deadrun, Double> tempDeadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
			Map<Deadrun, Double> tempDeadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>(); 
			Map<IdleTime, Double> tempIdleTimeMultiplier = new HashMap<IdleTime, Double>();
			
			for(Trip trip : solver.getOriginalTripVehicleMultipliers().keySet())
			{
				tempTripVehicleMultipliers.put(trip, solver.getOriginalTripVehicleMultipliers().get(trip)); 
			}
			
			for(Trip trip : solver.getOriginalTripDriverMultipliers().keySet())
			{
				tempTripDriverMultipliers.put(trip, solver.getOriginalTripDriverMultipliers().get(trip)); 
			}
			
			for(Deadrun deadrun : solver.getOriginalDeadrunLowerLimitMultiplier().keySet())
			{
				tempDeadrunLowerLimitMultiplier.put(deadrun, solver.getOriginalDeadrunLowerLimitMultiplier().get(deadrun)); 
			}
			
			for(Deadrun deadrun : solver.getOriginalDeadrunUpperLimitMultiplier().keySet())
			{
				tempDeadrunUpperLimitMultiplier.put(deadrun, solver.getOriginalDeadrunUpperLimitMultiplier().get(deadrun)); 
			}
			
			for(IdleTime idleTime : solver.getOriginalIdleTimeMultiplier().keySet())
			{
				tempIdleTimeMultiplier.put(idleTime, solver.getOriginalIdleTimeMultiplier().get(idleTime)); 
			}
			
			//Adapted multiplers
			
			for(Trip trip : solver.getTripVehicleMultipliers().keySet())
			{
				this.tripVehicleMultipliers.put(trip, solver.getTripVehicleMultipliers().get(trip)); 
			}
			
			for(Trip trip : solver.getTripDriverMultipliers().keySet())
			{
				this.tripDriverMultipliers.put(trip, solver.getTripDriverMultipliers().get(trip)); 
			}
			
			for(Deadrun deadrun : solver.getDeadrunLowerLimitMultiplier().keySet())
			{
				this.deadrunLowerLimitMultiplier.put(deadrun, solver.getDeadrunLowerLimitMultiplier().get(deadrun)); 
			}
			
			for(Deadrun deadrun : solver.getDeadrunUpperLimitMultiplier().keySet())
			{
				this.deadrunUpperLimitMultiplier.put(deadrun, solver.getDeadrunUpperLimitMultiplier().get(deadrun)); 
			}
			
			for(IdleTime idleTime : solver.getIdleTimeMultiplier().keySet())
			{
				this.idleTimeMultiplier.put(idleTime, solver.getIdleTimeMultiplier().get(idleTime)); 
			}
			
			VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(iteration, this.vehicleGraphs, this.tripVehicleMultipliers, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
			List<Block> blocks = vehicleSubproblem.getBlocksGenerated();
			
			DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, this.tripDriverMultipliers, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
			List<Duty> duties = driverSubproblem.getDutiesGenerated();
			
			if(!blocks.isEmpty())
			{
				this.blocksGenerated.addAll(blocks); 
			}
			
			if(!duties.isEmpty())
			{
				this.dutiesGenerated.addAll(duties); 
			}
			
			if(blocks.isEmpty() && duties.isEmpty())
			{
				status = 1; 
			}
			
			//Original multiplers
			
			for(Trip trip :  tempTripVehicleMultipliers.keySet())
			{
				this.tripVehicleMultipliers.put(trip, tempTripVehicleMultipliers.get(trip)); 
			}
			
			for(Trip trip : tempTripDriverMultipliers.keySet())
			{
				this.tripDriverMultipliers.put(trip, tempTripDriverMultipliers.get(trip)); 
			}
			
			for(Deadrun deadrun : tempDeadrunLowerLimitMultiplier.keySet())
			{
				this.deadrunLowerLimitMultiplier.put(deadrun, tempDeadrunLowerLimitMultiplier.get(deadrun)); 
			}
			
			for(Deadrun deadrun : tempDeadrunUpperLimitMultiplier.keySet())
			{
				this.deadrunUpperLimitMultiplier.put(deadrun, tempDeadrunUpperLimitMultiplier.get(deadrun)); 
			}
			
			for(IdleTime idleTime : tempIdleTimeMultiplier.keySet())
			{
				this.idleTimeMultiplier.put(idleTime, tempIdleTimeMultiplier.get(idleTime)); 
			}
			
			iteration++; 
		}
	}
}
