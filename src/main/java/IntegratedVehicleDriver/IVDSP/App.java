package IntegratedVehicleDriver.IVDSP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.DutyType;
import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Data.VehicleType;
import InitialSolution.InitialSolutionController;
import Lagrangian.IVDSPLagrangianCG;
import Lagrangian.LagrangianSolverIVDSP;
import LinearProgramming.IntegratedMasterProblem;
import Networks.DriverArc;
import Networks.DriverGraphGeneration;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleGraphGeneration;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import ilog.concert.IloException;

public class App 
{
    public static void main( String[] args ) throws FileNotFoundException, IOException, IloException
    {
    	System.out.println("*************** Read Instance *****************");
    	String inputPath = "/Users/ShyamSundar/Desktop/BAASVest/";
    	ReadInstance rd = new ReadInstance(inputPath); 
    	System.out.println("***********************************************");
    	
    	createGraphs(rd.getAllNodes(), rd.getAllVehicleTypes(), rd.getAllTrips(), rd.getAllVehicleTravels(), rd.getAllDutyTypes(), rd.getAllDriverTravels()); 
    	
    	/*long startTime = System.currentTimeMillis(); 
    	System.out.println("Algorithm....");
    	Set<Block> initialBlocks = new HashSet<Block>(); 
    	//Experimental exp = new Experimental(rd.getAllTrips(), initialBlocks, vehicleGraphs); 
    	VehicleLagrangianCG cg = new VehicleLagrangianCG(rd.getAllTrips(), initialBlocks, vehicleGraphs); 
    	long endTime = System.currentTimeMillis(); 
    	System.out.println("Total Time = " + (double)(endTime- startTime)/(double)1000);
    	
    	System.out.println("***********************************************");*/
    }
    
    
    private static void createGraphs(Set<Node> allNodes, Set<VehicleType> allVehicleTypes, List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, List<DutyType> allDutyTypes, List<DriverTravel> allDriverTravels) throws IloException
    {
      	System.out.println("************** Graph Generation ***************");
    	Set<VehicleTypeDepot> allVehicleTypeDepots = new HashSet<VehicleTypeDepot>(); 
    	Set<Node> depots = allNodes.stream().filter(n -> n.isDepot()).collect(Collectors.toSet()); 
    	for(VehicleType vehicleType : allVehicleTypes)
    	{
    		for(Node depot : depots)
    		{
    			VehicleTypeDepot vtd = new VehicleTypeDepot(vehicleType, depot); 
    			allVehicleTypeDepots.add(vtd); 
    		}
    	}
    	
    	Set<DutyTypeDepot> allDutyTypeDepots = new HashSet<DutyTypeDepot>(); 
    	Set<Node> dutySignOnNodes = allNodes.stream().filter(n -> n.isDriverSignOnAllowed()).collect(Collectors.toSet()); 
    	for(DutyType dutuType : allDutyTypes)
    	{
    		for(Node dutySignOn : dutySignOnNodes)
    		{
    			DutyTypeDepot dtd = new DutyTypeDepot(dutuType, dutySignOn); 
    			allDutyTypeDepots.add(dtd); 
    		}
    	}
    	
    	VehicleGraphGeneration grpahGen = new VehicleGraphGeneration(allVehicleTypeDepots, allTrips, allVehicleTravels, allNodes); 
    	Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs = grpahGen.getVehicleGraphs(); 
    	Set<Deadrun> allDeadruns = grpahGen.getDeadruns();
    	Set<IdleTime> allIdleTimes = grpahGen.getIdleTimes(); 
    	List<Block> blocksGenerated = new ArrayList<Block>();  //grpahGen.getBlocksGenerated(); 
    	System.out.println("***********************************************");
    	
    	DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(allDutyTypeDepots, allDriverTravels, allTrips, allDeadruns, vehicleGraphs); 
    	Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs = driverGraphgen.getDriverGraphs(); 
    	List<Duty> dutiesGenerated = new ArrayList<Duty>();  //driverGraphgen.getDutiesGenerated(); 
    	System.out.println("***********************************************");
    	for(Trip trip : allTrips)
    	{
    		System.out.println(trip.getTripId() + "; " + trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime());
    	}
    	
    	System.out.println("***********************************************");
    	System.out.println("Number of deadruns = " + allDeadruns.size());
    	for(Deadrun deadrun : allDeadruns)
    	{
    		System.out.println(deadrun.getDeadrunId() + "; " + deadrun.getDepartureNode().getNodeId() + "; " + deadrun.getArrivalNode().getNodeId() + "; " + deadrun.getDepartureTime() + "; " + deadrun.getArrivalTime() + "; " + deadrun.getType());
    	}
    	
    	System.out.println("***********************************************");
    	System.out.println("Number of idle times = " + allIdleTimes.size());
    	for(IdleTime idleTime : allIdleTimes)
    	{
    		System.out.println(idleTime.getNode().getNodeId() + "; " + idleTime.getDepartureTime() + "; " + idleTime.getArrivalTime());
    	}
    	
    	System.out.println("***********************************************");
    	
    	InitialSolutionController inSol = new InitialSolutionController(allTrips, allVehicleTravels, vehicleGraphs); 
    	
    	/*Map<Trip, Double> tripVehicleMultiplier = new HashMap<Trip, Double>(); 
    	Map<Trip, Double> tripDriverMultiplier = new HashMap<Trip, Double>(); 
    	Map<Deadrun, Double> deadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
    	Map<Deadrun, Double> deadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>();
    	Map<IdleTime, Double> idleTimeMultiplier = new HashMap<IdleTime, Double>(); 
    	allTrips.forEach(t -> {
    		tripVehicleMultiplier.put(t, 0.0); 
    		tripDriverMultiplier.put(t, 0.0); 
    	});
    	allDeadruns.forEach(d -> {
    		deadrunLowerLimitMultiplier.put(d, 0.0); 
    		deadrunUpperLimitMultiplier.put(d, 0.0); 
    	});
    	allIdleTimes.forEach(i -> {
    		idleTimeMultiplier.put(i, 0.0); 
    	});
    	
    	
    	long start = System.currentTimeMillis(); 
    	/*IVDSPLagrangianCG  lag = new IVDSPLagrangianCG (allTrips, allDeadruns, allIdleTimes, vehicleGraphs, driverGraphs); 
    	List<Block> blocksGenerated = lag.getBlocksGenerated(); 
    	List<Duty> dutiesGenerated = lag.getDutiesGenerated(); 
    	
    	IntegratedMasterProblem mp = new IntegratedMasterProblem(allTrips, allDeadruns, allIdleTimes, vehicleGraphs, driverGraphs,  blocksGenerated, dutiesGenerated);
    	long end = System.currentTimeMillis(); 
    	System.out.println((end - start)/ 1000.00);*/
 
    }
}
