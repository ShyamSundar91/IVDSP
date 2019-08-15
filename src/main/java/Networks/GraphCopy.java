package Networks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class GraphCopy 
{
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private List<Trip> uncoveredTripsOfVehicle;
	private List<Trip> uncoveredTripsOfDriver; 
	
	//Deadruns and Idle times when the block solution is fixed, else empty
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	
	@Getter
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy;
	@Getter
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy;
	
	public GraphCopy(Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Trip> uncoveredTripsOfVehicle, List<Trip> uncoveredTripsOfDriver, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution)
	{
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.uncoveredTripsOfVehicle = uncoveredTripsOfVehicle; 
		this.uncoveredTripsOfDriver = uncoveredTripsOfDriver; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		
		this.vehicleGraphsCopy = createCopyOfVehicleGraph(); 
		this.driverGraphsCopy = createCopyOfDriverGraph();
		
		removeVehicleArcsBasedOnTrips(this.vehicleGraphsCopy, this.uncoveredTripsOfVehicle);
		removeDriverArcsBasedOnTrips(this.driverGraphsCopy, this.uncoveredTripsOfDriver, this.deadrunsInSolution, this.idleTimesInSolution); 
	}
	
	public void restrictGraphSize(Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy)
	{
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			Set<VehicleArc> betweenTripArcs = vehicleGraph.edgeSet().stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getSuccessorVertex().getTrip() != null).collect(Collectors.toSet()); 
			Set<VehicleArc> arcsToRemove = new HashSet<VehicleArc>(); 
			
			for(VehicleArc arc : betweenTripArcs)
			{
				if(arc.getIdleTimeOnArc() != null)
				{
					/*if(arc.getPredecessorVertex().getTrip().getArrivalNode().equals(arc.getSuccessorVertex().getTrip().getDepartureNode()))
					{
						if(arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime() > 36)
						{
							arcsToRemove.add(arc); 
						}
					}
					else*/ if(arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime() > 36)
					{
						arcsToRemove.add(arc);
					}
				}
			}
			
			vehicleGraph.removeAllEdges(arcsToRemove); 
		}
		
		for(DutyTypeDepot dutyTypeDepot : driverGraphsCopy.keySet())
		{
			DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph = driverGraphsCopy.get(dutyTypeDepot); 
			//Set<DriverArc> tripArcs = driverGraph.edgeSet().stream().filter(a -> a.getTrip() != null).collect(Collectors.toSet()); 
			Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
			for(DriverArc arc : driverGraph.edgeSet())
			{
				if(arc.getIdleTimeOnArc() != null)
				{
					if(arc.getIdleTimeOnArc().getArrivalTime() - arc.getIdleTimeOnArc().getDepartureTime() > 36)
					{
						arcsToRemove.add(arc); 
					}
				}
				
			}
			driverGraph.removeAllEdges(arcsToRemove); 
		}
		
		/*Set<Deadrun> deadrunsInRestrictedGraph = new HashSet<Deadrun>(); 
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
		}*/
		
		
		int before = driverGraphsCopy.get(driverGraphsCopy.keySet().iterator().next()).edgeSet().size(); 
		System.out.println("Before = " + before);
		
		/*DriverGraphGeneration driverGraphgen = new DriverGraphGeneration(dutyTypeDepots, allDriverTravels, allNodes, allTrips, deadrunsInRestrictedGraph, vehicleGraphsCopy); 
		driverGraphsCopy = driverGraphgen.getDriverGraphs(); 
		
		removeDriverArcsBasedOnTrips(driverGraphsCopy, this.uncoveredTripsOfDriver, this.deadrunsInSolution, this.idleTimesInSolution);
		
		int after = driverGraphsCopy.get(driverGraphsCopy.keySet().iterator().next()).edgeSet().size(); 
		System.out.println("After = " + after);
		Assert.assertTrue(after < before);*/
	}
	
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> createCopyOfVehicleGraph()
	{
		Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy = new HashMap<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>>(); 
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleGraphs.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = this.vehicleGraphs.get(vehicleTypeDepot); 
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraphCopy = new DefaultDirectedGraph<VehicleVertex, VehicleArc>(VehicleArc.class);
			
			Set<VehicleVertex> allVertices = vehicleGraph.vertexSet(); 
			Set<VehicleArc> allArcs = vehicleGraph.edgeSet(); 
			for(VehicleVertex vertex : allVertices)
			{
				vehicleGraphCopy.addVertex(vertex); 
			}
			
			for(VehicleArc arc : allArcs)
			{
				vehicleGraphCopy.addEdge(arc.getPredecessorVertex(), arc.getSuccessorVertex(), arc); 
			}
			vehicleGraphsCopy.put(vehicleTypeDepot, vehicleGraphCopy); 
		}
		
		return vehicleGraphsCopy; 
	}
	
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> createCopyOfDriverGraph()
	{
		Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy = new HashMap<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>>(); 
		for(DutyTypeDepot dutyTypeDepot : this.driverGraphs.keySet())
		{
			DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph = this.driverGraphs.get(dutyTypeDepot); 
			DefaultDirectedGraph<DriverVertex, DriverArc> driverGraphCopy = new DefaultDirectedGraph<DriverVertex, DriverArc>(DriverArc.class); 
			
			Set<DriverVertex> allVertices = driverGraph.vertexSet(); 
			Set<DriverArc> allArcs = driverGraph.edgeSet(); 
			for(DriverVertex vertex : allVertices)
			{
				driverGraphCopy.addVertex(vertex); 
			}
			
			for(DriverArc arc : allArcs)
			{
				driverGraphCopy.addEdge(arc.getPredecessorVertex(), arc.getSuccessorVertex(), arc); 
			}
			driverGraphsCopy.put(dutyTypeDepot, driverGraphCopy); 
		}
		
		return driverGraphsCopy; 
	}
	
	private void removeVehicleArcsBasedOnTrips(Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy, List<Trip> trips)
	{
		for(VehicleTypeDepot vehicleTypeDepot : vehicleGraphsCopy.keySet())
		{
			DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph = vehicleGraphsCopy.get(vehicleTypeDepot); 
			Set<VehicleVertex> tripVertices = vehicleGraph.vertexSet().stream().filter(v -> v.getTrip() != null).collect(Collectors.toSet()); 
			for(VehicleVertex tripVertex : tripVertices)
			{
				if(!trips.contains(tripVertex .getTrip()))
				{
					vehicleGraph.removeVertex(tripVertex); 
				}
			}
		}
	}
	
	private void removeDriverArcsBasedOnTrips(Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy, List<Trip> trips, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution)
	{
		
		for(DutyTypeDepot dutyTypeDepot : driverGraphsCopy.keySet())
		{
			DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph = driverGraphsCopy.get(dutyTypeDepot); 
			//Set<DriverArc> tripArcs = driverGraph.edgeSet().stream().filter(a -> a.getTrip() != null).collect(Collectors.toSet()); 
			Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
			for(DriverArc arc : driverGraph.edgeSet())
			{
				if(arc.getTrip() != null && !trips.contains(arc.getTrip()))
				{
					arcsToRemove.add(arc); 
				}
				
				if(!deadrunsInSolution.isEmpty())
				{
					if(arc.getDeadrun() != null && !deadrunsInSolution.contains(arc.getDeadrun()))
					{
						arcsToRemove.add(arc);  
					}
				}
				
				if(!idleTimesInSolution.isEmpty())
				{
					if(arc.getIdleTimeOnArc() != null && !idleTimesInSolution.contains(arc.getIdleTimeOnArc()))
					{
						arcsToRemove.add(arc); 
					}
				}
			}
			driverGraph.removeAllEdges(arcsToRemove); 
		}
		
	}

}
