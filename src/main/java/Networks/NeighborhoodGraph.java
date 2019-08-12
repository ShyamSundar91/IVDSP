package Networks;

import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;

import lombok.Getter;

@Getter
public class NeighborhoodGraph 
{
	private int neighborhoodId; 
	private String neighborhoodType; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	public NeighborhoodGraph(int neighborhoodId, String neighborhoodType, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs)
	{
		this.neighborhoodId = neighborhoodId; 
		this.neighborhoodType = neighborhoodType; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
	}

}
