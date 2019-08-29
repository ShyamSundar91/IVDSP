package IntegratedVehicleDriver.IVDSP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceDriver.BBNodeDriver;
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

public class IndependentApproach 
{
	private List<Trip> trips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private double lpObjective; 
	private double totalMasterProblemTime; 
	private double totalVehicleSubproblemTime; 
	private double totalDriverSubproblemTime; 
	
	public IndependentApproach(List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.trips = trips; 
		this.vehicleGraph = vehicleGraph; 
		this.driverGraphs = driverGraphs; 
		
		this.totalMasterProblemTime = 0; 
		this.totalVehicleSubproblemTime = 0; 
		this.totalDriverSubproblemTime = 0; 
		System.out.println("*********************** Independent driver scheduling ****************************");
		driverScheduling(); 
		System.out.println("*********************** Independent vehicle scheduling ****************************");
		vehicleScheduling(); 
		System.out.println("***************************************************");
		System.out.println("Lower bound = " + this.lpObjective);
		System.out.println("Total time spent in mastet problem = " + this.totalMasterProblemTime);
		System.out.println("Total time spent in vehicle subproblem = " + this.totalVehicleSubproblemTime);
		System.out.println("Total time spent in driver subproblem = " + this.totalDriverSubproblemTime);
	}
	
	private void vehicleScheduling() throws IloException
	{
		BBNodeVehicle bbVehicle = new BBNodeVehicle(this.trips, this.vehicleGraph, new HashMap<Block, Integer>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), false); 
		bbVehicle.solveCG();
		
		this.lpObjective = this.lpObjective + bbVehicle.getLpObjective(); 
		this.totalMasterProblemTime = this.totalMasterProblemTime + bbVehicle.getTotalTimeSpentInMaster(); 
		this.totalVehicleSubproblemTime = bbVehicle.getTotalTimeSpentInSub(); 
		
	}
	
	private void driverScheduling() throws IloException
	{
		BBNodeDriver bbDriver = new BBNodeDriver(this.trips, new ArrayList<Block>(), new HashSet<Deadrun>(), new HashSet<IdleTime>(), this.driverGraphs, new HashMap<Duty, Integer>(), false); 
		bbDriver.solveCG();
		
		this.lpObjective = this.lpObjective + bbDriver.getLpObjective(); 
		this.totalMasterProblemTime = this.totalMasterProblemTime + bbDriver.getTotalTimeSpentInMaster(); 
		this.totalDriverSubproblemTime = bbDriver.getTotalTimeSpentInSub(); 
	}

}
