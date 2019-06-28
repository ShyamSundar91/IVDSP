package Variables;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Data.Trip;
import Data.VehicleType;
import lombok.Getter;

@Getter
public class Block 
{
	private int blockId; 
	private VehicleType vehicleType; 
	public List<Trip> tripsInBlock;
	private List<Deadrun> deadrunsInBlock; 
	private List<IdleTime> idleTimesInBlock; 
	private List<BlockActivity> blockActivities;
	public List<Integer> tripIds; 
	private double distance; 
	private static int counter = 1; 
	
	private double lhs; 
	
	public Block(VehicleType vehicleType, List<Trip> tripsInBlock, List<Deadrun> deadrunsInBlock, List<IdleTime> idleTimesInBlock, List<BlockActivity> blockActivities)
	{
		this.blockId = counter++; 
		this.vehicleType = vehicleType; 
		this.tripsInBlock = tripsInBlock; 
		this.deadrunsInBlock = deadrunsInBlock; 
		this.idleTimesInBlock = idleTimesInBlock; 
		this.blockActivities = blockActivities; 
		this.distance = this.blockActivities.stream().mapToDouble(b -> b.getDistance()).sum(); 
		this.lhs = this.distance*this.vehicleType.getCostPerkm() + this.vehicleType.getFixedCost(); 
		this.tripIds = new ArrayList<Integer>(); 
		this.tripsInBlock.forEach(t -> {
			this.tripIds.add(t.getTripId()); 
		});
		
	}
}
