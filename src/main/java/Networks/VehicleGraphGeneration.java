package Networks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import LinearProgramming.QuasiAssignmentFormulation;
import LinearProgramming.VehicleTest;
import Subproblems.VehicleRCSPP;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;


public class VehicleGraphGeneration 
{
	private Set<VehicleTypeDepot> vehicleTypeDepots; 
	private List<Trip> allTrips; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Set<Node> allNodes; 
	@Getter
	private Set<Deadrun> deadruns; 
	@Getter
	private Set<IdleTime> idleTimes; 
	@Getter
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	@Getter
	private List<Block> blocksGenerated; 
	 
	public VehicleGraphGeneration(Set<VehicleTypeDepot> vehicleTypeDepots, List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, Set<Node> allNodes) throws IloException 
	{
		this.vehicleTypeDepots = vehicleTypeDepots; 
		this.allTrips = allTrips; 
		this.allVehicleTravels = allVehicleTravels; 
		this.allNodes = allNodes; 
		this.vehicleGraphs = new HashMap<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>>();
		this.deadruns = new HashSet<Deadrun>(); 
		this.idleTimes = new HashSet<IdleTime>(); 
		
		generateGrpahs(); 
	}
	
	private void generateGrpahs() throws IloException 
	{
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleTypeDepots)
		{
			VehicleGraph graph = new VehicleGraph(vehicleTypeDepot, this.allTrips,  this.allVehicleTravels, this.allNodes, this.deadruns, this.idleTimes); 
			this.vehicleGraphs.put(vehicleTypeDepot, graph.getVehicleGraph()); 
			//System.out.println("Graph " + vehicleTypeDepot.getDescription() + ", vertices " + graph.getVehicleGraph().vertexSet().size() + ", edges " + graph.getVehicleGraph().edgeSet().size());
			/*for(VehicleArc arc : graph.getVehicleGraph().edgeSet())
			{
				System.out.println(arc.getPredecessorVertex().getVertexId() + " -----> " + arc.getSuccessorVertex().getVertexId());
				for(BlockActivity activity : arc.getBlockActivitiesOnEdge())
				{
					System.out.println(activity.getDepartureNode().getNodeId() + "; " + activity.getArrivalNode().getNodeId() + "; " + activity.getDepartureTime() + "; " + activity.getArrivalTime() + "; " + activity.getTripOrDeadrunId() + "; " + activity.getActivity()); 
				}
				System.out.println("********************");
			}*/
			
			/*VehicleRCSPP rcspp = new VehicleRCSPP(vehicleTypeDepot.getVehicleType(), graph.getVehicleGraph(), new HashMap<Trip, Double>()); 
			blocksGenerated = rcspp .getBlocksGenerated(); 
			for(Block block : blocksGenerated)
			{
				for(BlockActivity ba : block.getBlockActivities())
				{
					System.out.println(block.getBlockId() + "; " + block.getDistance() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
				}
			}
			System.out.println("Number of blocks generated = " + blocksGenerated.size());
			
			VehicleTest test = new VehicleTest(blocksGenerated, this.allTrips); */
			
			//QuasiAssignmentFormulation qaf = new QuasiAssignmentFormulation(this.allTrips, graph.getVehicleGraph()); 
		}
		
	}

}
