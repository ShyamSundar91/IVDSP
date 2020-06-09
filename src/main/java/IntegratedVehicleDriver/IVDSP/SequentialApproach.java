package IntegratedVehicleDriver.IVDSP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceDriver.BBNodeDriver;
import BranchPriceDriver.BranchAndBoundDriver;
import BranchPriceVehicle.BranchAndBoundVehicle;
import Data.Trip;
import Greedy.GreedyVehicle;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import lombok.Getter;

@Getter
public class SequentialApproach 
{
	private List<Trip> allTrips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs; 
	
	private List<Block> blocksInSolution; 
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	private List<Duty> dutiesInSolution; 
	private double totalObjective; 
	
	private int nodesVehicle; 
	private double masterVehicle; 
	private double subVehicle;
	private Map<Integer, List<Double>> vehicleLp; 
	
	private int nodesDriver; 
	private double masterDriver; 
	private double subDriver;
	private Map<Integer, List<Double>> driverLp; 
	public SequentialApproach(List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.allTrips = allTrips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		long start = System.currentTimeMillis(); 
		vehicleSchedulingProblem(); 
		long end = System.currentTimeMillis(); 
    	long start1 = System.currentTimeMillis(); 
		driverSchedulingProblem(); 
		long end1 = System.currentTimeMillis();
		System.out.println("Number of vehicle nodes processed = " + this.nodesVehicle);
		System.out.println("Node; LP Objective; Time");
		for(Integer lp : this.vehicleLp.keySet())
		{
			List<Double> obj = this.vehicleLp.get(lp); 
			System.out.println(lp + "; " + obj.get(0) + "; " + obj.get(1));
		}
		System.out.println("Total time spent in vehicle master problem = " + this.masterVehicle);
		System.out.println("Total time spent in vehicle subproblem = " + this.subVehicle);
		System.out.println("Total time for vehicle scheduling problem= " + (double)(end-start)/1000.00);
		System.out.println("Number of driver nodes processed = " + this.nodesDriver);
		System.out.println("Node; LP Objective; Time");
		for(Integer lp : this.driverLp.keySet())
		{
			List<Double> obj = this.driverLp.get(lp); 
			System.out.println(lp + "; " + obj.get(0) + "; " + obj.get(1));
		}
		System.out.println("Total time spent in driver master problem = " + this.masterDriver);
		System.out.println("Total time spent in driver subproblem = " + this.subDriver);
    	System.out.println("Total time for driver scheduling problem= " + (double)(end1-start1)/1000.00);
		calculateObjective(); 
	}
	
	private void vehicleSchedulingProblem() throws IloException
	{
	    /*GreedyVehicle greedy = new GreedyVehicle(this.allTrips, this.vehicleGraphs, new HashMap<Block, Integer>()); 
	    this.blocksInSolution = greedy.getBlocksInSolution(); 
        this.deadrunsInSolution = new HashSet<Deadrun>(greedy.getDeadrunsInSolution()); 
        this.idleTimesInSolution = new HashSet<IdleTime>(greedy.getIdleTimesInSolution()); */
        
		BranchAndBoundVehicle bbVehicle = new BranchAndBoundVehicle(this.allTrips, this.vehicleGraphs, new HashMap<Block, Integer>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), false);
		this.blocksInSolution = bbVehicle.getBlocksInSolution();
		this.deadrunsInSolution = new HashSet<Deadrun>(bbVehicle.getDeadrunsInSolution()); 
		this.idleTimesInSolution = new HashSet<IdleTime>(bbVehicle.getIdleTimesInSolution()); 
		this.nodesVehicle = bbVehicle.getNodeNo(); 
		this.masterVehicle = bbVehicle.getTotalTimeSpentInMaster(); 
		this.subVehicle = bbVehicle.getTotalTimeSpentInSubproblem();
		this.vehicleLp = bbVehicle.getLpObjectivesAtEachNode(); 
	}
	
	private void driverSchedulingProblem() throws IloException
	{
		for(DutyTypeDepot dutyTypeDepot : driverGraphs.keySet())
		{
			Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
			DefaultDirectedGraph<DriverVertex, DriverArc> graph = driverGraphs.get(dutyTypeDepot); 
			for(DriverArc arc : graph.edgeSet())
			{
				if(arc.getDeadrun() != null && !this.deadrunsInSolution.contains(arc.getDeadrun()))
				{
					arcsToRemove.add(arc); 
				}
				
				if(arc.getIdleTimeOnArc() != null && !this.idleTimesInSolution.contains(arc.getIdleTimeOnArc()))
				{
					arcsToRemove.add(arc); 
				}
			}
			
			graph.removeAllEdges(arcsToRemove); 
		}
		
		/*GreedyDriver greedy = new GreedyDriver(this.allTrips, this.driverGraphs, new HashMap<Duty, Integer>(), this.blocksInSolution, this.deadrunsInSolution, this.idleTimesInSolution); 
		this.dutiesInSolution = greedy.getDutiesInSolution(); */
		BranchAndBoundDriver dsp = new BranchAndBoundDriver(this.allTrips, this.blocksInSolution, this.deadrunsInSolution, this.idleTimesInSolution, this.driverGraphs, new HashMap<Duty, Integer>(), false);  
		this.dutiesInSolution = dsp.getDutiesInSolution(); 
		this.nodesDriver = dsp.getNodeNo(); 
		this.masterDriver = dsp.getTotalTimeSpentInMaster(); 
		this.subDriver = dsp.getTotalTimeSpentInSubproblem(); 
		this.driverLp = dsp.getLpObjectivesAtEachNode(); 
	}
	
	private void calculateObjective()
	{
		this.totalObjective = 0.0; 
		for(Block block : this.blocksInSolution)
		{
			this.totalObjective = this.totalObjective + block.getTotalCostOfBlock(); 
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			this.totalObjective = this.totalObjective + duty.getTotalCostOfDuty(); 
		}
		
		for(Block block : blocksInSolution)
		{
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
			
			System.out.println();
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			} 
			System.out.println();
		}
		
		System.out.println("Total sequential objective = " + this.totalObjective + ", Number of blocks = " + this.blocksInSolution.size() + ", Number of duties = " + this.dutiesInSolution.size());
	}

}
