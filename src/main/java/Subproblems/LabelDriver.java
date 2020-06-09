package Subproblems;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import lombok.Getter;

@Getter
public class LabelDriver implements Comparable<LabelDriver> 
{
	private LabelDriver sourceLabel; 
	private DriverVertex sourceDriverVertex; 
	private DriverArc extendingDriverArc; 
	private DriverREF updatedResources; 
	
	private boolean labelDriverVisited; 
	
	public LabelDriver(LabelDriver sourceLabel, DriverVertex sourceDriverVertex, DriverArc extendingDriverArc, DriverREF updatedResources)
	{
		this.sourceLabel = sourceLabel; 
		this.sourceDriverVertex = sourceDriverVertex; 
		this.extendingDriverArc = extendingDriverArc; 
		this.updatedResources = updatedResources; 
		
		this.labelDriverVisited = false; 
	}
	
	public void labelDriverVisited()
	{
		this.labelDriverVisited = true; 
	}

	public int compareTo(LabelDriver la) {
		
		if(this.updatedResources.getUpdatedReducedCost() < la.getUpdatedResources().getUpdatedReducedCost()) return -1; 
		if(this.updatedResources.getUpdatedReducedCost() > la.getUpdatedResources().getUpdatedReducedCost()) return 1; 
		
		return 0;
	
	}
	
}
