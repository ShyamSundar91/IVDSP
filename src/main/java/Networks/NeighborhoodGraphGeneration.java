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
	private int minimumBreakTime; 
	public NeighborhoodGraphGeneration(Set<DutyTypeDepot> dutyTypeDepots, List<DriverTravel> allDriverTravels, Set<Node> allNodes, List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> originalVehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> originalDriverGraphs)
	{
		this.dutyTypeDepots = dutyTypeDepots; 
		this.allDriverTravels = allDriverTravels; 
		this.allNodes = allNodes; 
		this.allTrips = allTrips; 
		this.minimumBreakTime = this.dutyTypeDepots.iterator().next().getDutyType().getMinimumBreakDuration(); 
		
		this.originalVehicleGraphs = originalVehicleGraphs; 
		this.originalDriverGraphs = originalDriverGraphs; 
		
		this.neighborhoodGraphs = new ArrayList<NeighborhoodGraph>(); 
		//restrictLineChangesAndIdleTime(); 
		restrictLineChanges(); 
		//restrictDeadrunsAndIdleTimes(); 
		restrictDeadruns(); 
		entireGraph(); 
	}
	
	private void restrictLineChangesAndIdleTime()
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
				if(!arc.getPredecessorVertex().getTrip().getArrivalNode().equals(arc.getSuccessorVertex().getTrip().getDepartureNode()))
				{
					arcsToRemove.add(arc); 
				}
				else if(arc.getIdleTimeOnArc() != null && (arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime()) > this.minimumBreakTime)
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
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(0, "Restricted Line Change and Idle time Graph", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
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
				if(!arc.getPredecessorVertex().getTrip().getArrivalNode().equals(arc.getSuccessorVertex().getTrip().getDepartureNode()))
				{
					arcsToRemove.add(arc); 
				}
				else if(arc.getIdleTimeOnArc() != null && (arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime()) > (2*this.minimumBreakTime))
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
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(1, "Restricted Line Change Graph", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
	}
	
	private void restrictDeadrunsAndIdleTimes()
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
				if(arc.getDeadrunsOnEdge().isEmpty())
				{
					if(arc.getIdleTimeOnArc() != null && (arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime()) > (2*this.minimumBreakTime))
					{
						arcsToRemove.add(arc);
					}
				}
				else if(arc.getIdleTimeOnArc() != null && (arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime()) > (this.minimumBreakTime/2))
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
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(2, "Restricted Deadruns and Idle Times", vehicleGraphsCopy, driverGraphsCopy); 
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
				if(arc.getIdleTimeOnArc() != null && (arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime()) > (2*this.minimumBreakTime))
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
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(3, "Restricted Deadruns", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
	}
	
	private void entireGraph()
	{
		GraphCopy graphCopy = new GraphCopy(this.originalVehicleGraphs, this.originalDriverGraphs, this.allTrips, this.allTrips, new HashSet<Deadrun>(), new HashSet<IdleTime>()); 
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy = graphCopy.getVehicleGraphsCopy(); 
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = graphCopy.getDriverGraphsCopy(); 
		
		NeighborhoodGraph neigh = new NeighborhoodGraph(4, "Entire Graph", vehicleGraphsCopy, driverGraphsCopy); 
		this.neighborhoodGraphs.add(neigh); 
	}
}
