package ALNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceIntegratedVehicleDriver.BranchAndBoundIntegrated;
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

public class RepairMethod 
{
	private List<Trip> allTrips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy;
	
	private List<Block> intermediateBlockSolution; 
	private List<Duty> intermediateDutySolution; 
	
	private List<Trip> uncoveredTripsOfVehicle; 
	private List<Trip> uncoveredTripsOfDriver; 
	private Set<Deadrun> deadrunInIntermediateSolution; 
	private Set<IdleTime> idleTimeInIntermediateSolution; 
	
	@Getter
	private List<Block> blocksInSoution; 
	@Getter
	private List<Duty> dutiesInSolution; 
	@Getter
	private double objective; 
	
	public RepairMethod(List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> intermediateBlockSolution, List<Duty> intermediateDutySolution, List<Trip> uncoveredTripsOfVehicle, List<Trip> uncoveredTripsOfDriver) throws IloException
	{
		this.allTrips = allTrips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		this.intermediateBlockSolution = intermediateBlockSolution; 
		this.intermediateDutySolution = intermediateDutySolution; 
		this.uncoveredTripsOfVehicle = uncoveredTripsOfVehicle; 
		this.uncoveredTripsOfDriver = uncoveredTripsOfDriver; 
		
		this.deadrunInIntermediateSolution = new HashSet<Deadrun>(); 
		this.idleTimeInIntermediateSolution = new HashSet<IdleTime>(); 
		
		this.blocksInSoution = new ArrayList<Block>(); 
		this.dutiesInSolution = new ArrayList<Duty>(); 
		this.objective = Double.MAX_VALUE; 
		
		createGraphsCopy(); 
		getDeadrunsAndIdleTimesInIntermediateSolution(); 
		repair(); 
	}
	
	private void repair() throws IloException
	{
		Map<Block, Integer> initialBlocks = new HashMap<Block, Integer>(); 
		Map<Duty, Integer> initialDuties = new HashMap<Duty, Integer>(); 
		for(Block block : this.intermediateBlockSolution)
		{
			initialBlocks.put(block, 1); 
		}
		
		for(Duty duty : this.intermediateDutySolution)
		{
			initialDuties.put(duty, 1); 
			
		}
		BranchAndBoundIntegrated bb = new BranchAndBoundIntegrated(this.allTrips, initialBlocks, initialDuties, this.deadrunInIntermediateSolution, this.idleTimeInIntermediateSolution, this.vehicleGraphsCopy, this.driverGraphsCopy, true); 
		this.blocksInSoution.addAll(bb.getBlocksInSolution()); 
		this.dutiesInSolution.addAll(bb.getDutiesInSolution()); 
		this.objective = bb.getObjective(); 
	}
	
	private void createGraphsCopy()
	{
		GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, this.uncoveredTripsOfVehicle, this.uncoveredTripsOfDriver);
		this.vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		this.driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
	}
	
	private void getDeadrunsAndIdleTimesInIntermediateSolution()
	{
		for(Block block : this.intermediateBlockSolution)
		{
			this.deadrunInIntermediateSolution.addAll(block.getDeadrunsInBlock()); 
			this.idleTimeInIntermediateSolution.addAll(block.getIdleTimesInBlock()); 
		}
		
		for(Duty duty : this.intermediateDutySolution)
		{
			this.deadrunInIntermediateSolution.addAll(duty.getDeadrunsInDuty()); 
			this.idleTimeInIntermediateSolution.addAll(duty.getIdleTimesInDuty()); 
		}
		
		
	}

}
