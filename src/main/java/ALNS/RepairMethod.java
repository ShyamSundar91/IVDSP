package ALNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceDriver.BranchAndBoundDriver;
import BranchPriceIntegratedVehicleDriver.BranchAndBoundIntegrated;
import BranchPriceVehicle.BranchAndBoundVehicle;
import Data.DriverTravel;
import Data.Node;
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

	private Map<Deadrun, Double> deadrunMultipliers;
	private Map<IdleTime, Double> idleTimeMultipliers; 
	private boolean initalSolutionLocalSearch; 
	
	public RepairMethod(int chosenDestroyMethod, List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, List<Block> intermediateBlockSolution, List<Duty> intermediateDutySolution, List<Block> blocksRemoved, List<Duty> dutiesRemoved, Set<Deadrun> deadrunInSolution,  Set<IdleTime> idleTimeInSolution, List<Trip> uncoveredTripsOfVehicle, List<Trip> uncoveredTripsOfDriver, boolean initalSolutionLocalSearch) throws IloException
	{
		this.chosenDestroyMethod = chosenDestroyMethod; 
		this.allTrips = allTrips;  
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		
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
		this.initalSolutionLocalSearch = initalSolutionLocalSearch; 
		
		
		if(this.initalSolutionLocalSearch)
		{
			if(this.chosenDestroyMethod == 0)
			{
				repairDriverSchedulingProblem(); 
			}
			else if(this.chosenDestroyMethod == 1)
			{
				repairVehicleAndDriverSequentially();
			}
		}
		else
		{
			if(this.chosenDestroyMethod == 0)
			{
				repairDriverSchedulingProblem(); 
			}
			else if(this.chosenDestroyMethod == 1)
			{
				repairVehicleAndDriverSequentially();
			}
			else if(this.chosenDestroyMethod == 2)
			{
				repairIntegrated();
			}
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
		
		BranchAndBoundDriver bb = new BranchAndBoundDriver(this.allTrips, this.intermediateBlockSolution, this.deadrunInSolution, this.idleTimeInSolution, this.driverGraphsCopy, initialDuties, true); 
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
		BranchAndBoundVehicle bbVehicle = new BranchAndBoundVehicle(this.allTrips, this.vehicleGraphsCopy, initialBlocks, this.deadrunMultipliers, this.idleTimeMultipliers, true);
		this.blocksInSoution.addAll(bbVehicle.getBlocksInSolution()); 
		this.deadrunInSolution.addAll(bbVehicle.getDeadrunsInSolution()); 
		this.idleTimeInSolution.addAll(bbVehicle.getIdleTimesInSolution()); 
		for(Block block : this.blocksInSoution)
		{
			this.objective = this.objective + block.getTotalCostOfBlock(); 
		} 
		
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
		
		BranchAndBoundDriver dsp = new BranchAndBoundDriver(this.allTrips, this.blocksInSoution, this.deadrunInSolution, this.idleTimeInSolution, this.driverGraphsCopy, initialDuties, true);  
		this.dutiesInSolution = dsp.getDutiesInSolution(); 
		this.objective = this.objective + dsp.getObjective(); 
	}
	
	private void repairIntegrated() throws IloException
	{
		this.deadrunInSolution = new HashSet<Deadrun>(); 
		this.idleTimeInSolution = new HashSet<IdleTime>(); 
		
		createGraphsCopy();
		
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			
			for(VehicleArc arc : vehicleGraph.edgeSet())
			{
				if(!arc.getDeadrunsOnEdge().isEmpty())
				{
					this.deadrunInSolution.addAll(arc.getDeadrunsOnEdge()); 
				}
				
				if(arc.getIdleTimeOnArc() != null)
				{
					this.idleTimeInSolution.add(arc.getIdleTimeOnArc()); 
				}
			}
		}
		
		Map<Block, Integer> initialBlocks = new HashMap<Block, Integer>(); 
		for(Block block : this.intermediateBlockSolution)
		{
			initialBlocks.put(block, 1); 
			this.deadrunInSolution.addAll(block.getDeadrunsInBlock()); 
			this.idleTimeInSolution.addAll(block.getIdleTimesInBlock()); 
		}
		
		for(Block block : this.blocksRemoved)
		{
			initialBlocks.put(block, 0);
			this.deadrunInSolution.addAll(block.getDeadrunsInBlock());
			this.idleTimeInSolution.addAll(block.getIdleTimesInBlock());
		}
		
		Map<Duty, Integer> initialDuties = new HashMap<Duty, Integer>();
		for(Duty duty : this.intermediateDutySolution)
		{
			initialDuties.put(duty, 0); 	
			this.deadrunInSolution.addAll(duty.getDeadrunsInDuty()); 
			this.idleTimeInSolution.addAll(duty.getIdleTimesInDuty()); 
		}
		
		for(Duty duty : this.dutiesRemoved)
		{
			initialDuties.put(duty, 0);
			this.deadrunInSolution.addAll(duty.getDeadrunsInDuty()); 
			this.idleTimeInSolution.addAll(duty.getIdleTimesInDuty()); 
		}
		
		BranchAndBoundIntegrated bb = new BranchAndBoundIntegrated(this.allTrips, initialBlocks, initialDuties, this.deadrunInSolution, this.idleTimeInSolution, this.vehicleGraphsCopy, this.driverGraphsCopy, true); 
		this.objective = bb.getObjective(); 
		this.blocksInSoution.addAll(bb.getBlocksInSolution()); 
		this.dutiesInSolution.addAll(bb.getDutiesInSolution()); 
	}
	
	private void createGraphsCopy()
	{
		if(this.uncoveredTripsOfDriver.isEmpty())
		{
			this.uncoveredTripsOfDriver.addAll(this.allTrips); 
		}
		GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, this.uncoveredTripsOfVehicle, this.uncoveredTripsOfDriver, this.deadrunInSolution, this.idleTimeInSolution);
		this.vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		this.driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
		
		/*if(this.chosenDestroyMethod > 1)
		{
			int before = driverGraphsCopy.get(driverGraphsCopy.keySet().iterator().next()).edgeSet().size(); 
			System.out.println("Before 1 = " + before);
			
			graphCopy.restrictGraphSize(vehicleGraphsCopy, driverGraphsCopy);
		}*/
	}
	

}
