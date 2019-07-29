package Variables;

import java.util.List;

import Data.DutyType;
import Data.Trip;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(of={"dutyType","tripsInDuty", "deadrunsInDuty", "dutyActivities", "idleTimesInDuty", "totalDuration", "totalCostOfDuty"})
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
	private double totalCostOfDuty; 
	
	private static int counter = 1; 
	
	private int numberOfIterationsInMP; 
	private int numberOfTimesChosen;
	public Duty(DutyType dutyType, List<Trip> tripsInDuty, List<Deadrun> deadrunsInDuty, List<DutyActivity> dutyActivities, List<IdleTime> idleTimesInDuty)
	{
		this.dutyId = counter++; 
		this.dutyType = dutyType; 
		this.tripsInDuty = tripsInDuty; 
		this.deadrunsInDuty = deadrunsInDuty; 
		this.dutyActivities = dutyActivities; 
		this.idleTimesInDuty = idleTimesInDuty; 
		this.totalDuration = this.dutyActivities.stream().mapToInt(d -> d.getDuration()).sum();
		//int minPaid = Math.max(this.totalDuration, this.dutyType.getMinimumPaidTime()); 
		this.totalCostOfDuty = (((double)this.totalDuration/(double)60) * this.dutyType.getCostPerHour()) + this.dutyType.getFixedCost(); 
		
		this.numberOfIterationsInMP = 0; 
		this.numberOfTimesChosen = 0; 
	}
	
	public void resetDutyInMP()
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
