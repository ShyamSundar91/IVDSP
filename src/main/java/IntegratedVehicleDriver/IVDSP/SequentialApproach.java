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
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution; 
	private List<Duty> dutiesInSolution; 
	private double totalObjective; 
	
	public SequentialApproach(List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.allTrips = allTrips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
		long start = System.currentTimeMillis(); 
		vehicleSchedulingProblem(); 
		long end = System.currentTimeMillis(); 
    	System.out.println("Total time for vehicle scheduling problem= " + (double)(end-start)/1000.00);
    	long start1 = System.currentTimeMillis(); 
		driverSchedulingProblem(); 
		long end1 = System.currentTimeMillis(); 
    	System.out.println("Total time for driver scheduling problem= " + (double)(end1-start1)/1000.00);
		calculateObjective(); 
	}
	
	private void vehicleSchedulingProblem() throws IloException
	{
		BranchAndBoundVehicle bbVehicle = new BranchAndBoundVehicle(this.allTrips, this.vehicleGraphs, new HashMap<Block, Integer>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), false);
		this.blocksInSolution = bbVehicle.getBlocksInSolution();
		this.deadrunsInSolution = bbVehicle.getDeadrunsInSolution(); 
		this.idleTimesInSolution = bbVehicle.getIdleTimesInSolution(); 
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
		
		BranchAndBoundDriver dsp = new BranchAndBoundDriver(this.allTrips, this.blocksInSolution, this.deadrunsInSolution, this.idleTimesInSolution, this.driverGraphs, new HashMap<Duty, Integer>(), false);  
		this.dutiesInSolution = dsp.getDutiesInSolution(); 
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
		
		System.out.println("Total objective = " + this.totalObjective);
	}

}
