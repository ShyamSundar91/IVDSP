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
	
	private List<LabelVehicle> labels; 
	
	public VehicleVertex(int vertexId, Trip trip, VehicleTypeDepot vehicleTypeDepot)
	{
		this.vertexId = vertexId; 
		this.trip = trip; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.reducedCost = 0; 
		this.distance = 0; 
		this.labels = new ArrayList<LabelVehicle>(); 
		
		if(this.trip != null)
		{
			this.blockActivity = new BlockActivity(this.trip.getDepartureNode(), this.trip.getArrivalNode(), this.trip.getDepartureTime(), this.trip.getArrivalTime(), this.trip.getDistance(), this.trip.getTripId(), "Trip"); 
			this.distance = this.trip.getDistance(); 
		}
	}
	
	public void calculateReducedCostOfSinkVertex(boolean usedFarkas)
	{
		if(usedFarkas)
		{
			this.reducedCost = 0.0; 
		}
		else
		{
			this.reducedCost = this.vehicleTypeDepot.getVehicleType().getFixedCost(); 
		}
	}
	
	public void calculateReducedCostOfVertex(double dualOfTrip, boolean usedFarkas)
	{
		this.reducedCost = 0.0; 
		if(usedFarkas)
		{
			this.reducedCost = - dualOfTrip; 
		}
		else
		{
			this.reducedCost = (this.vehicleTypeDepot.getVehicleType().getCostPerkm()*this.distance) - dualOfTrip; 
		}
	}

}
