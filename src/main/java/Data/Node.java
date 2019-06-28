package Data;

import lombok.Getter;

@Getter
public class Node 
{
	private int nodeId; 
	private boolean isDepot; 
	private boolean isRechargingAvailable; 
	private int maxIdleTime; 
	
	private boolean driverSignOnAllowed; 
	private boolean driverChangeAllowed; 
	private boolean driverBreakAllowed; 
	
	public Node(int nodeId, boolean isDepot, int maxIdleTime, boolean driverSignOnAllowed, boolean driverChangeAllowed, boolean driverBreakAllowed)
	{
		this.nodeId = nodeId; 
		this.isDepot = isDepot;
		this.maxIdleTime = maxIdleTime; 
		
		this.driverSignOnAllowed = driverSignOnAllowed; 
		this.driverChangeAllowed = driverChangeAllowed; 
		this.driverBreakAllowed = driverBreakAllowed; 
	}

}
