package Subproblems;

import Networks.VehicleArc;
import Networks.VehicleVertex;
import lombok.Getter;

@Getter
public class LabelVehicle implements Comparable<LabelVehicle> 
{
	private LabelVehicle sourceLabel; 
	private VehicleVertex sourceVehicleVertex; 
	private VehicleArc extendedVehicleArc; 
	private VehicleREF updatedResources;
	
	private boolean labelVehicleVisited; 
	
	private double delta; 
	public LabelVehicle(VehicleREF updatedResources, LabelVehicle sourceLabel, VehicleVertex sourceVehicleVertex, VehicleArc extendedVehicleArc)
	{
		this.updatedResources = updatedResources; 
		this.sourceLabel = sourceLabel; 
		this.sourceVehicleVertex = sourceVehicleVertex; 
		this.extendedVehicleArc = extendedVehicleArc; 
		this.labelVehicleVisited = false; 
		this.delta = Double.MAX_VALUE;
	}
	
	public void calculateDelta()
	{
	    this.delta = this.updatedResources.getUpdatedTotalDistance(); 
	}
	public void labelVehicleVisited()
	{
		this.labelVehicleVisited = true; 
	}
	
	public int compareTo(LabelVehicle la)
	{
		if(this.updatedResources.getUpdatedReducedCost() < la.getUpdatedResources().getUpdatedReducedCost()) return -1; 
		if(this.updatedResources.getUpdatedReducedCost() > la.getUpdatedResources().getUpdatedReducedCost()) return 1; 
		
		return 0;
	} 
}
