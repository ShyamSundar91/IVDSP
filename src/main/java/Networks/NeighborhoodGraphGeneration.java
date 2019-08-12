package Networks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class NeighborhoodGraphGeneration 
{
	private Set<DutyTypeDepot> dutyTypeDepots; 
	private List<DriverTravel> allDriverTravels; 
	private Set<Node> allNodes; 
	private List<Trip> allTrips; 
	
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> originalVehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> originalDriverGraphs;
	@Getter
	private List<NeighborhoodGraph> neighborhoodGraphs; 
	public NeighborhoodGraphGeneration(Set<DutyTypeDepot> dutyTypeDepots, List<DriverTravel> allDriverTravels, Set<Node> allNodes, List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> originalVehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> originalDriverGraphs)
	{
		this.dutyTypeDepots = dutyTypeDepots; 
		this.allDriverTravels = allDriverTravels; 
		this.allNodes = allNodes; 
		this.allTrips = allTrips; 
		
		this.originalVehicleGraphs = originalVehicleGraphs; 
		this.originalDriverGraphs = originalDriverGraphs; 
		
		this.neighborhoodGraphs = new ArrayList<NeighborhoodGraph>(); 
		restrictLineChanges(); 
		//restrictDeadruns(); 
		//entireGraph(); 
	}
	
	private void restrictLineChanges()
	{
		GraphCopy graphCopy = new GraphCopy(this.originalVehicleGraphs, this.originalDriverGraphs, this.allTrips, this.allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>()); 
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			Set<VehicleArc> arcsBetweenTrips = vehicleGraph.edgeSet().stream().filter(e -> e.getPredecessorVertex().getTrip() != null && e.getSuccessorVertex().getTrip() != null).collect(Collectors.toSet()); 
			Set<VehicleArc> arcsToRemove = new HashSet<VehicleArc>(); 
			
			for(VehicleArc arc : arcsBetweenTrips)
			{
				if(!arc.getPredecessorVertex().getTrip().getArrivalNode().equals(arc.getSuccessorVertex().getTrip().getDepartureNode()) || (arc.getSuccessorVertex().getTrip().getDepartureTime() - arc.getPredecessorVertex().getTrip().getArrivalTime()) >= 180)
				{
					arcsToRemove.add(arc); 
				}
			}
			
			vehicleGraph.removeAllEdges(arcsToRemove); 
		}
		
		Set<Deadrun> deadrunsInRestrictedGraph = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRestrictedGraph = new HashSet<IdleTime>(); 
		
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			
			for(VehicleArc arc : vehicleGraph.edgeSet())
			{
				if(!arc.getDeadrunsOnEdge().isEmpty())
				{
					deadrunsInRestrictedGraph.addAll(arc.getDeadrunsOnEdge()); 
				}
				
				if(arc.getIdleTimeOnArc() != null)
				{
					idleTimesInRestrictedGraph.add(arc.getIdleTimeOnArc()); 
				}
			}
		}
		
		DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(this.dutyTypeDepots, allDriverTravels, allNodes, allTrips, deadrunsInRestrictedGraph, vehicleGraphsCopy); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = driverGraphgen.getDriverGraphs(); 
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(0, "Restricted Line Change Graph", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
	}
	
	private void restrictDeadruns()
	{
		GraphCopy graphCopy = new GraphCopy(this.originalVehicleGraphs, this.originalDriverGraphs, this.allTrips, this.allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>()); 
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			Set<VehicleArc> arcsBetweenTrips = vehicleGraph.edgeSet().stream().filter(e -> e.getPredecessorVertex().getTrip() != null && e.getSuccessorVertex().getTrip() != null).collect(Collectors.toSet()); 
			Set<VehicleArc> arcsToRemove = new HashSet<VehicleArc>(); 
			
			for(VehicleArc arc : arcsBetweenTrips)
			{
				if(arc.getPredecessorVertex().getTrip().getArrivalNode().equals(arc.getSuccessorVertex().getTrip().getDepartureNode()) && (arc.getSuccessorVertex().getTrip().getDepartureTime() - arc.getPredecessorVertex().getTrip().getArrivalTime()) < 180)
				{
					continue; 
				}
				else if(!arc.getDeadrunsOnEdge().isEmpty() && (arc.getSuccessorVertex().getTrip().getDepartureTime() - arc.getPredecessorVertex().getTrip().getArrivalTime()) < 180)
				{
					List<Deadrun> dearun = arc.getDeadrunsOnEdge(); 
					for(Deadrun deadrun : dearun)
					{
						if(!deadrun.isPullIn() && !deadrun.isPullOut() && (deadrun.getArrivalTime() - deadrun.getDepartureTime()) > 5)
						{
							arcsToRemove.add(arc); 
							break; 
						}
					}
				}
				else if((arc.getSuccessorVertex().getTrip().getDepartureTime() - arc.getPredecessorVertex().getTrip().getArrivalTime()) >= 180)
				{
					arcsToRemove.add(arc); 
					break; 
				}
			}
			
			vehicleGraph.removeAllEdges(arcsToRemove); 
		}
		
		Set<Deadrun> deadrunsInRestrictedGraph = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesInRestrictedGraph = new HashSet<IdleTime>(); 
		
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			
			for(VehicleArc arc : vehicleGraph.edgeSet())
			{
				if(!arc.getDeadrunsOnEdge().isEmpty())
				{
					deadrunsInRestrictedGraph.addAll(arc.getDeadrunsOnEdge()); 
				}
				
				if(arc.getIdleTimeOnArc() != null)
				{
					idleTimesInRestrictedGraph.add(arc.getIdleTimeOnArc()); 
				}
			}
		}
		
		DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(this.dutyTypeDepots, allDriverTravels, allNodes, allTrips, deadrunsInRestrictedGraph, vehicleGraphsCopy); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = driverGraphgen.getDriverGraphs(); 
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(1, "Restricted Deadruns", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
		
		
	}
	
	private void entireGraph()
	{
		GraphCopy graphCopy = new GraphCopy(this.originalVehicleGraphs, this.originalDriverGraphs, this.allTrips, this.allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>()); 
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(2, "Entire Graph", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
	}
}
