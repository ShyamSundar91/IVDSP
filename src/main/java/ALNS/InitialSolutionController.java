package ALNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import BranchPriceDriver.BBNodeDriver;
import BranchPriceVehicle.BranchAndBoundVehicle;
import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import IntegratedVehicleDriver.IVDSP.SequentialApproach;
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
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class InitialSolutionController 
{
	private List<Trip> allTrips; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs; 
	private List<Node> depots; 
	private Set<Integer> lines; 
	
	@Getter
	private List<Duty> dutiesGenerated; 
	@Getter
	private List<Block> blocksGenerated; 
	@Getter
	private double initialSolutionObj; 
	@Getter
	private List<Block> blocksInSolution; 
	@Getter
	private List<Duty> dutiesInSolution; 
	public InitialSolutionController(List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.allTrips = allTrips; 
		this.allVehicleTravels = allVehicleTravels; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs;
		
		this.dutiesGenerated = new ArrayList<Duty>(); 
		this.blocksGenerated = new ArrayList<Block>(); 
		this.blocksInSolution = new ArrayList<Block>(); 
		this.dutiesInSolution = new ArrayList<Duty>(); 
		
		this.initialSolutionObj = 0.0; 
		
		this.depots = new ArrayList<Node>(); 
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleGraphs.keySet())
		{
			this.depots.add(vehicleTypeDepot.getDepot()); 
		}
		
		this.lines = new HashSet<Integer>(); 
		for(Trip trip : this.allTrips)
		{
			this.lines.add(trip.getLineNumber()); 
		}
		
		lineScheduling(); 
	}
	
	private void lineScheduling() throws IloException
	{
		System.out.println("Number of lines = " + this.lines.size());
		
		for(Integer lineNumber : this.lines)
		{
			long start = System.currentTimeMillis(); 
			
			List<Trip> tripsInLine = this.allTrips.stream().filter(t -> t.getLineNumber() == lineNumber.intValue()).collect(Collectors.toList()); 
			System.out.println("***************************************************");
			System.out.println("Line number = " + lineNumber + ", number of trips = " + tripsInLine.size());
			
			GraphCopy graphCopy = new GraphCopy(this.vehicleGraphs, this.driverGraphs, tripsInLine, tripsInLine, new HashSet<Deadrun>(), new HashSet<IdleTime>());  
			Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphCopy = graphCopy.getVehicleGraphsCopy(); 
			Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphCopy = graphCopy.getDriverGraphsCopy(); 
			
			SequentialApproach seqAp = new SequentialApproach(tripsInLine, vehicleGraphCopy, driverGraphCopy); 
			this.blocksInSolution.addAll(seqAp.getBlocksInSolution()); 
			this.dutiesInSolution.addAll(seqAp.getDutiesInSolution()); 
			this.initialSolutionObj = this.initialSolutionObj + seqAp.getTotalObjective(); 
			System.out.println("Line number = " + lineNumber + ", total cost = " + seqAp.getTotalObjective() + ", number of blocks = " + seqAp.getBlocksInSolution().size() + ", number of duties = " + seqAp.getDutiesInSolution().size());
			long end = System.currentTimeMillis(); 
			System.out.println("Total time = " + (double)(end-start)/(double)1000);
			System.out.println("Initial Solution = " + this.initialSolutionObj);
			
		}
	}
	
	
	
	
	
}
