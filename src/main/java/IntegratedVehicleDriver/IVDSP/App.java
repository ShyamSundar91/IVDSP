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
import Networks.GraphCopy;
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
	
    public static void main( String[] args) throws FileNotFoundException, IOException, IloException
    {
    	System.out.println("*************** Read Instance *****************");
    	String inputPath = args[0]; // "/Users/ShyamSundar/Desktop/IntegratedVehicleAndDriver/Data/BAASVestSmall/";
    	ReadInstance rd = new ReadInstance(inputPath); 
    	double d1 = 0.3;  
    	double d2 = 0.3; 
    	double d3 = 0.3;
    	int integratedIterationLimit = 100;  
    	int integratedTimeLimit = 180; 
    	int initialAndlocalSearchTimeLimit = 86400; 
    	
    	if(args.length > 1)
    	{
    		d1 = Double.parseDouble(args[1]);  //0.3; 
        	d2 = Double.parseDouble(args[2]); //0.3; 
        	d3 = Double.parseDouble(args[3]); //0.3; 
        	integratedIterationLimit = Integer.parseInt(args[4]);
        	integratedTimeLimit = Integer.parseInt(args[5]); 
        	initialAndlocalSearchTimeLimit = Integer.parseInt(args[6]);
        	System.out.println("Degree of duty destruction = " + d1);
        	System.out.println("Degree of sequential destruction = " + d2);
        	System.out.println("Degree of integrated destruction = " + d3);
        	System.out.println("Integrated node iteration limit = " + integratedIterationLimit);
        	System.out.println("Integrated node time limit = " + integratedTimeLimit); 
        	System.out.println("Initial Sequential and Local search time limit = " + initialAndlocalSearchTimeLimit);
    	}
    	
    	
    	System.out.println("***********************************************");
    	
    	createGraphs(rd.getAllNodes(), rd.getAllVehicleTypes(), rd.getAllTrips(), rd.getAllVehicleTravels(), rd.getAllDutyTypes(), rd.getAllDriverTravels(), d1, d2, d3, integratedIterationLimit, integratedTimeLimit, initialAndlocalSearchTimeLimit); 
    	
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
    
    
    private static void createGraphs(Set<Node> allNodes, Set<VehicleType> allVehicleTypes, List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, List<DutyType> allDutyTypes, List<DriverTravel> allDriverTravels, double degreeOfDutyDestruction, double degreeOfSequentialDestruction, double degreeOfIntegratedDestruction, int integratedIterationLimit, int integratedTimeLimit, int localSearchTimeLimit) throws IloException
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
    	
    	double avgDistTrip = 0.0; 
    	double avgDurTrip = 0.0; 
    	for(Trip trip : allTrips)
    	{
    		avgDistTrip = avgDistTrip + trip.getDistance(); 
    		avgDurTrip = avgDurTrip + (double)(trip.getArrivalTime() - trip.getDepartureTime()); 
    	}
    	avgDistTrip = avgDistTrip/(double)allTrips.size(); 
    	avgDurTrip = avgDurTrip/(double)allTrips.size();
    	System.out.println("Average distance of trips in km = " + avgDistTrip);
    	System.out.println("Average duration of trips in minutes = " + avgDurTrip);
    	
    	VehicleGraphGeneration grpahGen = new VehicleGraphGeneration(allVehicleTypeDepots, allTrips, allVehicleTravels, allNodes); 
    	Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs = grpahGen.getVehicleGraphs(); 
    	Set<Deadrun> allDeadruns = grpahGen.getDeadruns();
    	Set<IdleTime> allIdleTimes = grpahGen.getIdleTimes(); 
    	Map<Block, Integer> blocksGenerated = new HashMap<Block, Integer>(); //grpahGen.getBlocksGenerated(); 
    	System.out.println("Number of deadheads = " + allDeadruns.size());
    	double avgDistDeadhead = 0.0; 
    	double avgDurDeadhead = 0.0; 
    	for(Deadrun deadrun : allDeadruns)
    	{
    		avgDistDeadhead = avgDistDeadhead + deadrun.getDistance(); 
    		avgDurDeadhead = avgDurDeadhead + (double)(deadrun.getArrivalTime() - deadrun.getDepartureTime()); 
    	}
    	avgDistDeadhead = avgDistDeadhead/(double)allDeadruns.size(); 
    	avgDurDeadhead = avgDurDeadhead/(double)allDeadruns.size();
    	System.out.println("Number of idle time = " + allIdleTimes.size());
    	double avgDurIdleTime = 0.0;
    	double min = Double.MAX_VALUE; 
    	double max = 0; 
    	for(IdleTime idleTime : allIdleTimes)
    	{
    		double id = (double)(idleTime.getArrivalTime() - idleTime.getDepartureTime()); 
    		avgDurIdleTime = avgDurIdleTime + id; 
    		if(id < min)
    		{
    			min = id; 
    		}
    	}
    	avgDurIdleTime = avgDurIdleTime/(double)allIdleTimes.size(); 
    	System.out.println("Average distance of deadheads in km = " + avgDistDeadhead);
    	System.out.println("Average duration of deadheads in minutes = " + avgDurDeadhead);
    	System.out.println("Average duration of idle times in minutes = " + avgDurIdleTime);
    	System.out.println("Minimum idle time = " + min);
    	System.out.println("***********************************************");
    	
    	DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(allDutyTypeDepots, allDriverTravels, allNodes, allTrips, allDeadruns, vehicleGraphs); 
    	Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs = driverGraphgen.getDriverGraphs(); 
    	Map<Duty, Integer> dutiesGenerated = new HashMap<Duty, Integer>(); // driverGraphgen.getDutiesGenerated(); 
    	System.out.println("***********************************************");
    	
    	double startIniLo = System.currentTimeMillis(); 
    	//InitialSolutionController initial = new InitialSolutionController(allTrips, allVehicleTravels, vehicleGraphs, driverGraphs); 
    	GraphCopy graphCopy = new GraphCopy(vehicleGraphs, driverGraphs, allTrips, allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>());  
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphCopy = graphCopy.getVehicleGraphsCopy(); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphCopy = graphCopy.getDriverGraphsCopy();
    	SequentialApproach seq = new SequentialApproach(allTrips, vehicleGraphCopy, driverGraphCopy); 
    	double endIniLo = System.currentTimeMillis(); 
    	System.out.println("Total time for sequential solution = " + (double)(endIniLo - startIniLo)/1000.00);
    	
    	//double start = System.currentTimeMillis();  

    	LocalSearch localSearch = new LocalSearch(startIniLo, allTrips, new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), vehicleGraphs, driverGraphs, seq.getBlocksInSolution(), seq.getDutiesInSolution(), seq.getTotalObjective(), degreeOfDutyDestruction, degreeOfSequentialDestruction, degreeOfIntegratedDestruction, integratedIterationLimit, integratedTimeLimit, localSearchTimeLimit, allDriverTravels, allNodes); 
    	//blocksGenerated.addAll(initial.getBlocksInSolution()); 
    	//dutiesGenerated.addAll(initial.getDutiesInSolution()); 
    	//Set<Deadrun> deadrunsInSolution = new HashSet<Deadrun>(); 
    	//Set<IdleTime> idleTimesInSolution = new HashSet<IdleTime>(); 
    	/*for(Block block : blocksGenerated)
    	{
    		deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
    		idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
    	}*/
    	//IntegratedMasterProblem imp = new IntegratedMasterProblem(allTrips, deadrunsInSolution, idleTimesInSolution, /*allDeadruns, allIdleTimes,*/ vehicleGraphs, driverGraphs, blocksGenerated, dutiesGenerated, false); 
    	//BranchAndBoundIntegrated bb = new BranchAndBoundIntegrated(allTrips, blocksGenerated, dutiesGenerated, deadrunsInSolution, idleTimesInSolution, vehicleGraphs, driverGraphs, true); 
    	
    	
    	//SequentialApproach seq = new SequentialApproach(allTrips, vehicleGraphs, driverGraphs); 
    	//IndependentApproach ind = new IndependentApproach(allTrips, vehicleGraphs, driverGraphs); 
    	double end = System.currentTimeMillis(); 
    	System.out.println("Total time = " + (double)(end-startIniLo)/1000.00);
        	
    }
}
