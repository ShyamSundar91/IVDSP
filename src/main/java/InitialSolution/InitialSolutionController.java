package InitialSolution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloException;

public class InitialSolutionController 
{
	private List<Trip> allTrips; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs; 
	private List<Node> depots; 
	private Set<Integer> lines; 
	
	public InitialSolutionController(List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.allTrips = allTrips; 
		this.allVehicleTravels = allVehicleTravels; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		
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
			List<Trip> tripsInLine = this.allTrips.stream().filter(t -> t.getLineNumber() == lineNumber.intValue()).collect(Collectors.toList()); 
			VehicleTypeDepot bestDepot = selectDepotForLine(tripsInLine); 
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraphForLine = this.vehicleGraphs.get(bestDepot); 
			
			SingleDepotVehicleScheduling sdvsp = new SingleDepotVehicleScheduling(lineNumber, tripsInLine, bestDepot, vehicleGraphForLine); 
			List<Block> blocksInSolution = sdvsp.getBlocksInSolution(); 
			List<Deadrun> deadrunsInSolution = sdvsp.getDeadrunsInSolution(); 
			List<IdleTime> idleTimesInSolution = sdvsp.getIdleTimesInSolution(); 
			for(Block block : sdvsp.getBlocksInSolution())
			{
				for(BlockActivity ba : block.getBlockActivities())
				{
					System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
				}
			}
			
			driverScheduling(tripsInLine, blocksInSolution, deadrunsInSolution, idleTimesInSolution); 
		}
	}
	
	private void driverScheduling(List<Trip> tripsInLine, List<Block> blocksInSolution, List<Deadrun> deadrunsInSolution, List<IdleTime> idleTimesInSolution) throws IloException
	{
		DriverScheduling dsp = new DriverScheduling(tripsInLine, blocksInSolution, deadrunsInSolution, idleTimesInSolution, this.driverGraphs); 
	}
	
	private VehicleTypeDepot selectDepotForLine(List<Trip> tripsInLine)
	{
		Set<Node> nodesInLine = new HashSet<Node>(); 
		VehicleTypeDepot vehichleTypeDepotForLine = null; 
		
		for(Trip trip : tripsInLine)
		{
			nodesInLine.add(trip.getDepartureNode()); 
			nodesInLine.add(trip.getArrivalNode()); 
		}
		
		double minimumDistance = Double.MAX_VALUE; 
		Node bestDepot = null; 
		
		for(Node depot : this.depots)
		{
			double distance = 0; 
			for(Node nodeInLine : nodesInLine)
			{
				if(!nodeInLine.equals(depot))
				{
					Optional<VehicleTravel> travelFromDepot = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(depot) && t.getArrivalNode().equals(nodeInLine)).findFirst(); 
					if(travelFromDepot.isPresent())
					{
						distance = distance + travelFromDepot.get().getDistance(); 
					}
					else
					{
						System.out.println("Error: Travel does not exist from depot, node = " + nodeInLine.getNodeId());
					}
				}
			}
			
			if(distance < minimumDistance)
			{
				minimumDistance = distance; 
				bestDepot = depot; 
			}
		}
		
		if(bestDepot != null)
		{
			final Node depot = bestDepot; 
			vehichleTypeDepotForLine = this.vehicleGraphs.entrySet().stream().filter(v -> v.getKey().getDepot().equals(depot)).findFirst().get().getKey(); 
		}
		
		return vehichleTypeDepotForLine; 
	}
	
	
}
