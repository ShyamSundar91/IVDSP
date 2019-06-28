package Variables;

import java.util.List;

import Data.DutyType;
import Data.Trip;
import lombok.Getter;

@Getter
public class Duty 
{
	private int dutyId; 
	private DutyType dutyType; 
	private List<Trip> tripsInDuty; 
	private List<Deadrun> deadrunsInDuty; 
	private List<DutyActivity> dutyActivities; 
	private List<IdleTime> idleTimesInDuty; 
	private int totalDuration; 
	private double totalAmountPaid; 
	
	private static int counter = 1; 
	
	public Duty(DutyType dutyType, List<Trip> tripsInDuty, List<Deadrun> deadrunsInDuty, List<DutyActivity> dutyActivities, List<IdleTime> idleTimesInDuty)
	{
		this.dutyId = counter++; 
		this.dutyType = dutyType; 
		this.tripsInDuty = tripsInDuty; 
		this.deadrunsInDuty = deadrunsInDuty; 
		this.dutyActivities = dutyActivities; 
		this.idleTimesInDuty = idleTimesInDuty; 
		this.totalDuration = this.dutyActivities.stream().mapToInt(d -> d.getDuration()).sum(); 
		this.totalAmountPaid = ((double)this.totalDuration/(double)60) * this.dutyType.getCostPerHour(); 
	}

}
