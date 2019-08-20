package IntegratedVehicleDriver.IVDSP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import ALNS.InitialSolutionController;
import ALNS.LocalSearch;
import ALNS.SequenceGeneration;
import BranchPriceDriver.BBNodeDriver;
import BranchPriceIntegratedVehicleDriver.BranchAndBoundIntegrated;
import BranchPriceVehicle.BranchAndBoundVehicle;
import Data.DriverTravel;
import Data.DutyType;
import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Data.VehicleType;
import InitialSolution.SingleDepotVehicleScheduling;
import Lagrangian.ColumnGeneration;
import LinearProgramming.IntegratedMasterProblem;
import LinearProgramming.Master;
import Networks.DriverArc;
import Networks.DriverGraphGeneration;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.NeighborhoodGraphGeneration;
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
    	String inputPath = args[0];     //"/Users/ShyamSundar/Desktop/IntegratedVehicleAndDriver/Data/BAASSydNordSmall1/";
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
    	System.out.println("***********************************************");

    	System.out.println("***********************************************");

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
    	Map<Block, Integer> blocksGenerated = new HashMap<Block, Integer>(); //grpahGen.getBlocksGenerated(); 
    	System.out.println("Number of deadheads = " + allDeadruns.size());
    	/*for(Deadrun deadrun : allDeadruns)
    	{
    		System.out.println(deadrun.getDeadrunId() + "; " + deadrun.getDepartureNode().getNodeId() + "; " + deadrun.getArrivalNode().getNodeId() + "; " + deadrun.getDepartureTime() + "; " + deadrun.getArrivalTime());
    	}*/
    	System.out.println("Number of idle time = " + allIdleTimes.size());
    	/*for(IdleTime idleTime : allIdleTimes)
    	{
    		System.out.println(idleTime.getNode().getNodeId() + "; " + idleTime.getDepartureTime() + "; " + idleTime.getArrivalTime());
    	}*/
    	System.out.println("***********************************************");
    	
    	DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(allDutyTypeDepots, allDriverTravels, allNodes, allTrips, allDeadruns, vehicleGraphs); 
    	Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs = driverGraphgen.getDriverGraphs(); 
    	Map<Duty, Integer> dutiesGenerated = new HashMap<Duty, Integer>(); // driverGraphgen.getDutiesGenerated(); 
    	System.out.println("***********************************************");
    	
    	long startIniLo = System.currentTimeMillis(); 
    	//InitialSolutionController initial = new InitialSolutionController(allTrips, allVehicleTravels, vehicleGraphs, driverGraphs); 
    	long endIniLo = System.currentTimeMillis(); 
    	System.out.println("Total time for initial and local search = " + (double)(endIniLo - startIniLo)/1000.00);
    	
    	long start = System.currentTimeMillis(); 
    	
    	NeighborhoodGraphGeneration neigh = new NeighborhoodGraphGeneration(allDutyTypeDepots,  allDriverTravels, allNodes, allTrips, vehicleGraphs, driverGraphs); 
    	Master master = new Master(allTrips, allVehicleTravels, neigh.getNeighborhoodGraphs()); 
    	//ColumnGeneration cg = new ColumnGeneration(allTrips, allDeadruns, allIdleTimes, vehicleGraphs, driverGraphs, /*initial.getBlocksInSolution(), initial.getDutiesInSolution(), initial.getInitialSolutionObj()*/ new ArrayList<Block>(), new ArrayList<Duty>(), Double.MAX_VALUE); 
    	//LocalSearch localSearch = new LocalSearch(allTrips, new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(),/* cg.getDeadrunMultipliers(), cg.getIdleTimeMulitpliers(),*/ vehicleGraphs, driverGraphs, initial.getBlocksInSolution(), initial.getDutiesInSolution(), initial.getInitialSolutionObj(), false, 1000); 
    	//blocksGenerated.addAll(initial.getBlocksInSolution()); 
    	//dutiesGenerated.addAll(initial.getDutiesInSolution()); 
    	Set<Deadrun> deadrunsInSolution = new HashSet<Deadrun>(); 
    	Set<IdleTime> idleTimesInSolution = new HashSet<IdleTime>(); 
    	/*for(Block block : blocksGenerated)
    	{
    		deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
    		idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
    	}*/
    	//IntegratedMasterProblem imp = new IntegratedMasterProblem(allTrips, deadrunsInSolution, idleTimesInSolution, /*allDeadruns, allIdleTimes,*/ vehicleGraphs, driverGraphs, blocksGenerated, dutiesGenerated, false); 
    	//BranchAndBoundIntegrated bb = new BranchAndBoundIntegrated(allTrips, blocksGenerated, dutiesGenerated, deadrunsInSolution, idleTimesInSolution, vehicleGraphs, driverGraphs, true); 
    	
    	
    	//SequentialApproach seq = new SequentialApproach(allTrips, vehicleGraphs, driverGraphs); 
    	long end = System.currentTimeMillis(); 
    	System.out.println("Total time = " + (double)(end-start)/1000.00);
    
    	
    	
    	
    }
}
