package Data;

import lombok.Getter;

@Getter
public class VehicleType 
{
	private String vehicleTypeDescription; 
	private double fixedCost; 
	private double costPerkm; 
	private double maximumDistanceWithoutRecharging; 
	private int minimumRechargingDuration; 
	
	public VehicleType(String vehicleTypeDescription, double fixedCost, double costPerkm, double maximumDistanceWithoutRecharging, int minimumRechargingDuration)
	{
		this.vehicleTypeDescription = vehicleTypeDescription; 
		this.fixedCost = fixedCost; 
		this.costPerkm = costPerkm; 
		this.maximumDistanceWithoutRecharging = maximumDistanceWithoutRecharging; 
		this.minimumRechargingDuration = minimumRechargingDuration; 
	}

}
