package Variables;

import java.util.List;

import Data.DutyType;
import Data.Trip;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(of={"dutyType","tripsInDuty", "deadrunsInDuty", "dutyActivities", "idleTimesInDuty", "totalDuration", "totalCostOfDuty"})
@Getter
public class Duty implements Comparable<Duty> 
{
	private int dutyId; 
	private DutyType dutyType; 
	private List<Trip> tripsInDuty; 
	private List<Deadrun> deadrunsInDuty; 
	private List<DutyActivity> dutyActivities; 
	private List<IdleTime> idleTimesInDuty; 
	private int totalDuration; 
	private double totalCostOfDuty; 
	private double deltaOfDuty;
	private int startTime; 
	private int endTime; 
	
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
		this.totalCostOfDuty = (((double)this.totalDuration/(double)60) * this.dutyType.getCostPerHour()) + this.dutyType.getFixedCost(); 
		int totalDrivingDuration = 0; 
		for(Trip trip : this.tripsInDuty)
		{
			totalDrivingDuration = totalDrivingDuration + (trip.getArrivalTime() - trip.getDepartureTime()); 
		}
		
		/*for(Deadrun deadrun : this.deadrunsInDuty)
		{
			totalDrivingDuration = totalDrivingDuration + (deadrun.getArrivalTime() - deadrun.getDepartureTime()); 
		}*/
		
		this.deltaOfDuty = (this.totalCostOfDuty/(double)(totalDrivingDuration)) ; 
		this.startTime = this.dutyActivities.get(0).getDepartureTime(); 
		this.endTime = this.dutyActivities.get(this.dutyActivities.size()-1).getArrivalTime(); 
		
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

	public int compareTo(Duty d) {
		
		if(this.deltaOfDuty < d.getDeltaOfDuty()) return -1; 
		if(this.deltaOfDuty > d.getDeltaOfDuty()) return 1; 
		
		return 0;
	}

}
