package Networks;


import java.util.ArrayList;
import java.util.List;

import Data.Trip;
import Subproblems.LabelVehicle;
import Variables.BlockActivity;
import lombok.Getter;

@Getter
public class VehicleVertex 
{
	private int vertexId; 
	private Trip trip; 
	private VehicleTypeDepot vehicleTypeDepot; 
	private BlockActivity blockActivity; 
	private double distance; 
	private double reducedCost; 
	private double totalCostOfVertex; 
	
	private List<LabelVehicle> labels; 
	
	public VehicleVertex(int vertexId, Trip trip, VehicleTypeDepot vehicleTypeDepot)
	{
		this.vertexId = vertexId; 
		this.trip = trip; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.reducedCost = 0.0; 
		this.distance = 0.0; 
		this.labels = new ArrayList<LabelVehicle>(); 
		
		if(this.trip != null)
		{
			this.blockActivity = new BlockActivity(this.trip.getDepartureNode(), this.trip.getArrivalNode(), this.trip.getDepartureTime(), this.trip.getArrivalTime(), this.trip.getDistance(), this.trip.getTripId(), "Trip"); 
			this.distance = this.trip.getDistance(); 
		}
		
		this.totalCostOfVertex = this.vehicleTypeDepot.getVehicleType().getCostPerkm()*this.distance; 
	}
	
	
	public void calculateReducedCostOfVertex(double dualOfTrip)
	{
		this.reducedCost = 0.0; 
		{
			this.reducedCost = this.totalCostOfVertex - dualOfTrip; 
		}
	}

}
