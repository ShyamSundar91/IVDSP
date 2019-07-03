package Networks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;


import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class VehicleArc 
{
	private VehicleVertex predecessorVertex; 
	private VehicleVertex successorVertex; 
	
	private VehicleTypeDepot vehicleTypeDepot; 
	private List<BlockActivity> blockActivitiesOnEdge; 
	private List<Deadrun> deadrunsOnEdge; 
	private IdleTime idleTimeOnArc; 
	private double totalDistance; 
	private double reducedCostOfArc;
	
	private int startTimeOfReCharging;
	private int endTimeOfReCharging; 
	private double distancedCoveredBeforeReCharging; 
	private double distanceCoveredAfterReCharging;
	
	public VehicleArc(VehicleVertex predecessorVertex, VehicleVertex successorVertex, VehicleTypeDepot vehicleTypeDepot, List<Deadrun> deadrunsOnEdge, IdleTime idleTimeOnArc, List<BlockActivity> blockActivitiesOnEdge)
	{
		this.predecessorVertex = predecessorVertex; 
		this.successorVertex = successorVertex; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.deadrunsOnEdge = deadrunsOnEdge; 
		this.idleTimeOnArc = idleTimeOnArc; 
		this.blockActivitiesOnEdge = blockActivitiesOnEdge; 
		
		this.totalDistance = 0; 
		this.reducedCostOfArc = 0; 
		
		this.startTimeOfReCharging = -1; 
		this.endTimeOfReCharging = -1; 
		this.distancedCoveredBeforeReCharging = -1; 
		this.distanceCoveredAfterReCharging = -1; 
		
		this.reducedCostOfArc = 0; 
		
		calculateTotalDistance(); 
		//calculateReducedCostOfArc(); 
		checkRefueling(); 
	}
	
	private void calculateTotalDistance()
	{
		this.totalDistance = this.blockActivitiesOnEdge.stream().mapToDouble(b -> b.getDistance()).sum(); 
	}
	
	public void calculateReducedCostOfArc(Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit, Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit, Map<IdleTime, Double> dualValuesOfIdleTimes, boolean usedFarkas)
	{
		this.reducedCostOfArc = 0.0; 
		double rhs = 0.0; 
		if(!deadrunsOnEdge.isEmpty() && !dualValuesOfDeadrunsLowerLimit.isEmpty())
		{
			for(Deadrun deadrunOnEdge : this.deadrunsOnEdge)
			{
				rhs = rhs - dualValuesOfDeadrunsLowerLimit.get(deadrunOnEdge); 
				rhs = rhs -(2 * dualValuesOfDeadrunsUpperLimit.get(deadrunOnEdge)); 
			}
		}
		
		if(this.idleTimeOnArc != null && !dualValuesOfIdleTimes.isEmpty())
		{
			rhs = rhs - dualValuesOfIdleTimes.get(this.idleTimeOnArc); 
		}
		
		if(usedFarkas)
		{
			this.reducedCostOfArc = -rhs; 
		}
		else
		{
			this.reducedCostOfArc = (this.totalDistance * this.vehicleTypeDepot.getVehicleType().getCostPerkm()) - rhs; 
		}
		
	}
	
	private void checkRefueling()
	{
		List<BlockActivity> recharging = this.blockActivitiesOnEdge.stream().filter(b -> b.getActivity().equals("Recharging")).collect(Collectors.toList()); 
		if(!recharging.isEmpty())
		{
			Assert.assertTrue(recharging.size() == 1); // there should be just one refueling event
			this.startTimeOfReCharging = recharging.get(0).getDepartureTime(); 
			this.endTimeOfReCharging = recharging.get(0).getArrivalTime(); 
			
			boolean reachedRefueling = false; 
			this.distanceCoveredAfterReCharging = 0.0; 
			this.distancedCoveredBeforeReCharging = 0.0; 
			
			Collections.sort(this.blockActivitiesOnEdge);
			for(BlockActivity blockElement : this.blockActivitiesOnEdge)
			{
				if(blockElement.equals(recharging.get(0)))
				{
					reachedRefueling = true; 
				}
				else
				{
					if(!reachedRefueling)
					{
						this.distancedCoveredBeforeReCharging = this.distancedCoveredBeforeReCharging + blockElement.getDistance(); 
					}
					else
					{
						this.distanceCoveredAfterReCharging = this.distanceCoveredAfterReCharging + blockElement.getDistance(); 
					}
				}
			}
			
			Assert.assertTrue((this.distanceCoveredAfterReCharging + this.distancedCoveredBeforeReCharging) == this.totalDistance);
		}
	}
	

}
