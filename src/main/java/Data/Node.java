package Data;

import lombok.Getter;

@Getter
public class Node 
{
	private int nodeId; 
	private boolean isDepot; //Currently, recharging is done at the depot 
	//private boolean isRechargingAvailable; 
	private int minIdleTime; 
	private int maxIdleTime; 
	
	private boolean driverSignOnAllowed; 
	private boolean driverChangeAllowed; 
	private boolean driverBreakAllowed; 
	
	public Node(int nodeId, boolean isDepot, int minIdleTime, int maxIdleTime, boolean driverSignOnAllowed, boolean driverChangeAllowed, boolean driverBreakAllowed)
	{
		this.nodeId = nodeId; 
		this.isDepot = isDepot;
		this.minIdleTime = minIdleTime; 
		this.maxIdleTime = maxIdleTime; 
		
		this.driverSignOnAllowed = driverSignOnAllowed; 
		this.driverChangeAllowed = driverChangeAllowed; 
		this.driverBreakAllowed = driverBreakAllowed; 
	}

}
