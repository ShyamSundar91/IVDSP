package ALNS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import lombok.Getter;

public class GraphCopy 
{
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private List<Trip> uncoveredTripsOfVehicle;
	private List<Trip> uncoveredTripsOfDriver; 
	
	@Getter
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphsCopy;
	@Getter
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy;
	
	public GraphCopy(Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Trip> uncoveredTripsOfVehicle, List<Trip> uncoveredTripsOfDriver)
	{
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.uncoveredTripsOfVehicle = uncoveredTripsOfVehicle; 
		this.uncoveredTripsOfDriver = uncoveredTripsOfDriver; 
		
		this.vehicleGraphsCopy = createCopyOfVehicleGraph(); 
		this.driverGraphsCopy = createCopyOfDriverGraph();
		removeVehicleArcsBasedOnTrips(this.vehicleGraphsCopy, this.uncoveredTripsOfVehicle);
		removeDriverArcsBasedOnTrips(this.driverGraphsCopy, this.uncoveredTripsOfDriver); 
		
		
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
	
	private void removeDriverArcsBasedOnTrips(Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphsCopy, List<Trip> trips)
	{
		
		for(DutyTypeDepot dutyTypeDepot : driverGraphsCopy.keySet())
		{
			DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph = driverGraphsCopy.get(dutyTypeDepot); 
			Set<DriverArc> tripArcs = driverGraph.edgeSet().stream().filter(a -> a.getTrip() != null).collect(Collectors.toSet()); 
			for(DriverArc tripArc : tripArcs)
			{
				if(!trips.contains(tripArc.getTrip()))
				{
					driverGraph.removeEdge(tripArc); 
				}
			}
		}
		
	}

}
