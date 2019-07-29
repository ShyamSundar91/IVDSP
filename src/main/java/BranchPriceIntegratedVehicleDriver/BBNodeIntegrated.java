package BranchPriceIntegratedVehicleDriver;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import LinearProgramming.IntegratedMasterProblem;
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

@Getter
public class BBNodeIntegrated 
{
	private Map<Block, Integer> initialBlocksAndGenerated; 
	private Map<Duty, Integer> initialDutiesAndGenerated; 
	private List<Trip> trips; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private Map<Block, Double> fractionalValuesOfBlocks; 
	private Map<Duty, Double> fractionalValuesOfDuties; 
	private boolean solutionInteger;
	private double lpObjective; 
	private boolean earlyTermination; 
	
	public BBNodeIntegrated(Map<Block, Integer> initialBlocksAndGenerated, Map<Duty, Integer> initialDutiesAndGenerated, List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, boolean earlyTermination)
	{
		this.initialBlocksAndGenerated = initialBlocksAndGenerated; 
		this.initialDutiesAndGenerated = initialDutiesAndGenerated; 
		this.trips = trips; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.earlyTermination = earlyTermination; 
	}
	
	public void solve() throws IloException
	{
		IntegratedMasterProblem imp = new IntegratedMasterProblem(this.trips, this.deadruns, this.idleTimes, this.vehicleGraphs, this.driverGraphs, this.initialBlocksAndGenerated, this.initialDutiesAndGenerated, this.earlyTermination);
		this.fractionalValuesOfBlocks = imp.getFractionalValuesOfBlocks(); 
		this.fractionalValuesOfDuties = imp.getFractionalValuesOfDuties(); 
		this.solutionInteger = imp.isSolutionInteger(); 
		this.lpObjective = imp.getLpObjective(); 
	}

}
