package Variables;

import java.util.List;

import Data.Trip;
import Data.VehicleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(of={"vehicleType","tripsInBlock", "deadrunsInBlock", "blockActivities", "idleTimesInBlock", "distance", "totalCostOfBlock"})
@Getter
public class Block implements Comparable<Block>
{
	private int blockId; 
	private VehicleType vehicleType; 
	private List<Trip> tripsInBlock;
	private List<Deadrun> deadrunsInBlock; 
	private List<IdleTime> idleTimesInBlock; 
	private List<BlockActivity> blockActivities;
	private double distance; 
	private static int counter = 1; 
	
	private double totalCostOfBlock; 
	private double deltaOfBlock; 
	
	private int numberOfIterationsInMP; 
	private int numberOfTimesChosen; 
	public Block(VehicleType vehicleType, List<Trip> tripsInBlock, List<Deadrun> deadrunsInBlock, List<IdleTime> idleTimesInBlock, List<BlockActivity> blockActivities)
	{
		this.blockId = counter++; 
		this.vehicleType = vehicleType; 
		this.tripsInBlock = tripsInBlock; 
		this.deadrunsInBlock = deadrunsInBlock; 
		this.idleTimesInBlock = idleTimesInBlock; 
		this.blockActivities = blockActivities; 
		this.distance = this.blockActivities.stream().mapToDouble(b -> b.getDistance()).sum(); 
		this.totalCostOfBlock = this.distance*this.vehicleType.getCostPerkm() + this.vehicleType.getFixedCost(); 
		
		double totalDrivingDistance = 0; 
		for(Trip trip : this.tripsInBlock)
		{
			totalDrivingDistance = totalDrivingDistance + trip.getDistance(); 
		}
		
		this.deltaOfBlock = (this.totalCostOfBlock)/(double)totalDrivingDistance; 
		
		this.numberOfIterationsInMP = 0; 
		this.numberOfTimesChosen = 0; 
	}

	public int compareTo(Block b) {
		
		if(this.deltaOfBlock < b.getDeltaOfBlock()) return -1; 
		if(this.deltaOfBlock > b.getDeltaOfBlock()) return 1; 
		return 0;
	}
	
	public void resetBlockInMP()
	{
		this.numberOfIterationsInMP = 0; 
		this.numberOfTimesChosen = 0; 
	}
	
	public void increaseNumberOfIterationsInMP()
	{
		this.numberOfIterationsInMP++; 
	}
	
	public void increaseNumberOfTimesChosen()
	{
		this.numberOfTimesChosen++; 
	}
}
