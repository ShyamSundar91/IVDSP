package Subproblems;

import java.util.ArrayList;
import java.util.List;

import Data.Trip;
import Data.VehicleType;
import Networks.VehicleArc;
import Networks.VehicleVertex;
import lombok.Getter;

@Getter
public class VehicleREF 
{
	private VehicleType vehicleType; 
	private VehicleREF previousREF; 
	private VehicleArc extendingArc; 
	private VehicleVertex succeedingVertex;  
	
	private double updatedReducedCost; 
	private double updatedDistanceWithoutRecharging;
	private double updatedTotalDistance; 
	private List<Trip> updatedTrips; 
	
	public VehicleREF(VehicleType vehicleType, VehicleREF previousREF, VehicleArc extendingArc, VehicleVertex succeedingVertex)
	{
		this.vehicleType = vehicleType; 
		this.previousREF = previousREF; 
		this.extendingArc = extendingArc; 
		this.succeedingVertex = succeedingVertex;  
		
		if(this.previousREF == null)
		{
			initialize(); 
		}
		else
		{
			updateReducedCost(); 
			updateTrips(); 
			updatedTotalDistance(); 
		}
	}
	
	private void initialize()
	{
		this.updatedReducedCost = 0; 
		this.updatedTotalDistance = 0; 
		this.updatedDistanceWithoutRecharging = 0; 
		this.updatedTrips = new ArrayList<Trip>();

	}
	
	private void updateReducedCost()
	{
		this.updatedReducedCost = this.previousREF.getUpdatedReducedCost() + this.extendingArc.getReducedCostOfArc() + this.succeedingVertex.getReducedCost(); 
	}
	
	private void updateTrips()
	{
		this.updatedTrips = new ArrayList<Trip>(); 
		if(previousREF != null)
		{
			this.updatedTrips.addAll(previousREF.getUpdatedTrips()); 
			if(this.succeedingVertex.getTrip() != null)
			{
				this.updatedTrips.add(this.succeedingVertex.getTrip()); 
			}
		}
	}
	
	private void updatedTotalDistance()
	{
		this.updatedTotalDistance = this.previousREF.getUpdatedTotalDistance() + this.extendingArc.getTotalDistance() + this.succeedingVertex.getDistance(); 
	}
	
	public boolean isValid()
	{
		if(!checkValidMaxDistanceWithoutRefueling())
		{ 
			return false; 
		}

		
		return true; 
	}
	
	private boolean checkValidMaxDistanceWithoutRefueling()
	{
		if(this.extendingArc.getStartTimeOfReCharging() > -1)
		{
			this.updatedDistanceWithoutRecharging = this.previousREF.getUpdatedDistanceWithoutRecharging() + this.extendingArc.getDistancedCoveredBeforeReCharging(); 
			
			if(this.updatedDistanceWithoutRecharging > this.vehicleType.getMaximumDistanceWithoutRecharging())
			{
				return false; 
			}
			
			this.updatedDistanceWithoutRecharging = 0; 
			this.updatedDistanceWithoutRecharging = this.extendingArc.getDistanceCoveredAfterReCharging() + this.succeedingVertex.getDistance(); 
			if(this.updatedDistanceWithoutRecharging > this.vehicleType.getMaximumDistanceWithoutRecharging())
			{
				return false; 
			}
		}
		else
		{
			this.updatedDistanceWithoutRecharging = this.previousREF.getUpdatedDistanceWithoutRecharging() + this.extendingArc.getTotalDistance() + this.succeedingVertex.getDistance(); 
			if(this.updatedDistanceWithoutRecharging > this.vehicleType.getMaximumDistanceWithoutRecharging())
			{
				return false; 
			}
		}
		
		return true; 
	}
}
