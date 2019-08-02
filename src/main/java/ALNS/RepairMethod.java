package ALNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceDriver.BranchAndBoundDriver;
import BranchPriceIntegratedVehicleDriver.BranchAndBoundIntegrated;
import BranchPriceVehicle.BranchAndBoundVehicle;
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
	private int chosenDestroyMethod; 
	private List<Trip> allTrips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy;
	
	private List<Block> intermediateBlockSolution; 
	private List<Duty> intermediateDutySolution; 
	private List<Block> blocksRemoved; 
	private List<Duty> dutiesRemoved; 
	private Set<Deadrun> deadrunInSolution; 
	private Set<IdleTime> idleTimeInSolution; 
	
	private List<Trip> uncoveredTripsOfVehicle; 
	private List<Trip> uncoveredTripsOfDriver; 
	
	
	@Getter
	private List<Block> blocksInSoution; 
	@Getter
	private List<Duty> dutiesInSolution; 
	@Getter
	private double objective; 
	
	@Getter
	private Map<Trip, Double> tripVehicleDuals; 
	@Getter
	private Map<Trip, Double> tripDriverDuals; 
	@Getter
	private Map<Deadrun, Double> deadrunDuals;
	@Getter
	private Map<IdleTime, Double> idleTimeDuals; 
	
	public RepairMethod(int chosenDestroyMethod, List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> intermediateBlockSolution, List<Duty> intermediateDutySolution, List<Block> blocksRemoved, List<Duty> dutiesRemoved, Set<Deadrun> deadrunInSolution,  Set<IdleTime> idleTimeInSolution, List<Trip> uncoveredTripsOfVehicle, List<Trip> uncoveredTripsOfDriver) throws IloException
	{
		this.chosenDestroyMethod = chosenDestroyMethod; 
		this.allTrips = allTrips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		this.intermediateBlockSolution = intermediateBlockSolution; 
		this.intermediateDutySolution = intermediateDutySolution; 
		this.blocksRemoved = blocksRemoved; 
		this.dutiesRemoved = dutiesRemoved; 
		this.deadrunInSolution = deadrunInSolution; 
		this.idleTimeInSolution = idleTimeInSolution; 
		this.uncoveredTripsOfVehicle = uncoveredTripsOfVehicle; 
		this.uncoveredTripsOfDriver = uncoveredTripsOfDriver; 
		
		
		this.blocksInSoution = new ArrayList<Block>(); 
		this.dutiesInSolution = new ArrayList<Duty>(); 
		this.objective = Double.MAX_VALUE; 
		
 
		if(this.chosenDestroyMethod == 0)
		{
			repairDriverSchedulingProblem(); 
		}
		if(this.chosenDestroyMethod == 1)
		{
			repairVehicleAndDriverSequentially(); 
		}
		  
	}
	
	private void repairDriverSchedulingProblem() throws IloException
	{
		createGraphsCopy(); 
		
		Map<Duty, Integer> initialDuties = new HashMap<Duty, Integer>();
		for(Duty duty : this.intermediateDutySolution)
		{
			initialDuties.put(duty, 1); 	
		}
		
		for(Duty duty : this.dutiesRemoved)
		{
			initialDuties.put(duty, 0);
		}
		
		BranchAndBoundDriver bb = new BranchAndBoundDriver(this.allTrips, this.intermediateBlockSolution, this.deadrunInSolution, this.idleTimeInSolution, this.driverGraphsCopy, initialDuties, false); 
		this.dutiesInSolution.addAll(bb.getDutiesInSolution()); 
		this.blocksInSoution.addAll(this.intermediateBlockSolution); 
		this.objective = 0.0;
		for(Block block : this.blocksInSoution)
		{
			this.objective = this.objective + block.getTotalCostOfBlock(); 
		}
		this.objective = this.objective + bb.getObjective(); 
	}
	
	private void repairVehicleAndDriverSequentially() throws IloException
	{
		this.deadrunInSolution = new HashSet<Deadrun>(); 
		this.idleTimeInSolution = new HashSet<IdleTime>(); 
		
		createGraphsCopy();
		
		Map<Block, Integer> initialBlocks = new HashMap<Block, Integer>(); 
		for(Block block : this.intermediateBlockSolution)
		{
			initialBlocks.put(block, 1); 
		}
		
		for(Block block : this.blocksRemoved)
		{
			initialBlocks.put(block, 0);
		}
		
		
		this.objective = 0; 
		BranchAndBoundVehicle bbVehicle = new BranchAndBoundVehicle(this.allTrips, this.vehicleGraphsCopy, initialBlocks, new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), false);
		this.blocksInSoution.addAll(bbVehicle.getBlocksInSolution()); 
		this.deadrunInSolution.addAll(bbVehicle.getDeadrunsInSolution()); 
		this.idleTimeInSolution.addAll(bbVehicle.getIdleTimesInSolution()); 
		this.objective = this.objective + bbVehicle.getObjective(); 
		
		for(DutyTypeDepot dutyTypeDepot : this.driverGraphsCopy.keySet())
		{
			Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
			DefaultDirectedGraph<DriverVertex, DriverArc> graph = this.driverGraphsCopy.get(dutyTypeDepot); 
			for(DriverArc arc : graph.edgeSet())
			{
				if(arc.getDeadrun() != null && !this.deadrunInSolution.contains(arc.getDeadrun()))
				{
					arcsToRemove.add(arc); 
				}
				
				if(arc.getIdleTimeOnArc() != null && !this.idleTimeInSolution.contains(arc.getIdleTimeOnArc()))
				{
					arcsToRemove.add(arc); 
				}
			}
			
			graph.removeAllEdges(arcsToRemove); 
		}
		
		Map<Duty, Integer> initialDuties = new HashMap<Duty, Integer>();
		for(Duty duty : this.intermediateDutySolution)
		{
			initialDuties.put(duty, 1); 	
		}
		
		BranchAndBoundDriver dsp = new BranchAndBoundDriver(this.allTrips, this.blocksInSoution, this.deadrunInSolution, this.idleTimeInSolution, this.driverGraphsCopy, initialDuties, false);  
		this.dutiesInSolution = dsp.getDutiesInSolution(); 
		this.objective = this.objective + dsp.getObjective(); 
		
		
	}
	
	private void createGraphsCopy()
	{
		GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, this.uncoveredTripsOfVehicle, this.uncoveredTripsOfDriver, this.deadrunInSolution, this.idleTimeInSolution);
		this.vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		this.driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
	}
	

}
