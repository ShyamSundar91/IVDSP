package Networks;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;

import Data.DutyType;
import Data.Trip;
import Variables.Deadrun;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class DriverArc implements Comparable<DriverArc>
{
	private DutyType dutyType; 
	private DriverVertex predecessorVertex; 
	private DriverVertex successorVertex; 

	private Trip trip; 
	private Deadrun deadrun;
	private List<DutyActivity> dutyActivities; 
	private int durationOfArc; 
	private int startTimeOfBreak; 
	private int endTimeOfBreak; 
	private int durationBeforeBreak; 
	private int durationAfterBreak;
	private IdleTime idleTimeOnArc; 
	private boolean attendingBus;
	private boolean changingBus; 
	private double reducedCostOfArc; 
	private double totalCostOfArc; 
	
	public DriverArc(DutyType dutyType, DriverVertex predecessorVertex, DriverVertex successorVertex, Trip trip, Deadrun deadrun, List<DutyActivity> dutyActivities, IdleTime idleTimeOnArc, boolean attendingBus, boolean changingBus) 
	{
		this.dutyType = dutyType; 
		this.predecessorVertex = predecessorVertex; 
		this.successorVertex = successorVertex; 
		
		this.trip = trip; 
		this.deadrun = deadrun; 
		this.dutyActivities = dutyActivities; 
		this.idleTimeOnArc = idleTimeOnArc; 
		this.attendingBus = attendingBus; 
		this.changingBus = changingBus; 
		
		this.startTimeOfBreak = -1; 
		this.endTimeOfBreak = -1; 
		this.durationBeforeBreak = 0; 
		this.durationAfterBreak = 0; 
		this.durationOfArc = 0; 
		this.reducedCostOfArc = 0.0; 
		this.totalCostOfArc = 0.0; 
		
		getDuration(); 
		getBreakActivity(); 
		
		this.totalCostOfArc = ((double)this.durationOfArc/(double)60) * this.dutyType.getCostPerHour(); 
		if(this.predecessorVertex.getCurrentTime() == -1)
		{
			this.totalCostOfArc = this.totalCostOfArc + this.dutyType.getFixedCost(); 
		}
	}
	
	public void calculateReducedCostOfArc(Map<Trip, Double> dualValuesOfTrips, Map<Deadrun, Double> dualValuesOfDeadrunLowerLimit, Map<Deadrun, Double> dualValuesOfDeadrunUpperLimit, Map<IdleTime, Double> dualValuesOfIdleTimes)
	{
		this.reducedCostOfArc = 0.0; 
		double rhs = 0.0; 
		if(this.trip != null && dualValuesOfTrips.containsKey(this.trip))
		{
			rhs = rhs + dualValuesOfTrips.get(this.trip); 
		}
		else if(this.deadrun != null && dualValuesOfDeadrunLowerLimit.containsKey(this.deadrun))
		{
			rhs = rhs + dualValuesOfDeadrunLowerLimit.get(this.deadrun) + dualValuesOfDeadrunUpperLimit.get(this.deadrun); 
		}
		else if(this.idleTimeOnArc != null && dualValuesOfIdleTimes.containsKey(this.idleTimeOnArc))
		{
			rhs = rhs + dualValuesOfIdleTimes.get(this.idleTimeOnArc); 
		}
		
		this.reducedCostOfArc = this.totalCostOfArc - rhs; 
		
	}
	
	private void getDuration()
	{
		if(!this.dutyActivities.isEmpty())
		{
			this.durationOfArc = this.dutyActivities.stream().mapToInt(da -> da.getDuration()).sum(); 
		}
	}
	
	private void getBreakActivity()
	{
		List<DutyActivity> breakActivity = this.dutyActivities.stream().filter(b -> b.getActivity().equals("Break")).collect(Collectors.toList()); 
		if(!breakActivity.isEmpty())
		{
			Assert.assertTrue(breakActivity.size() == 1);
			this.startTimeOfBreak = breakActivity.get(0).getDepartureTime(); 
			this.endTimeOfBreak = breakActivity.get(0).getArrivalTime(); 
			this.durationBeforeBreak = this.startTimeOfBreak - this.dutyActivities.get(0).getDepartureTime(); 
			this.durationAfterBreak = this.dutyActivities.get(this.dutyActivities.size()-1).getArrivalTime() - this.endTimeOfBreak; 
		}
		
	}

	@Override
	public int compareTo(DriverArc o) 
	{	
		if(this.successorVertex.getCurrentTime() < o.getPredecessorVertex().getCurrentTime()) return -1; 
		if(this.successorVertex.getCurrentTime() > o.getPredecessorVertex().getCurrentTime()) return 1; 
		
		if(this.predecessorVertex.getCurrentTime() < o.getPredecessorVertex().getCurrentTime()) return -1; 
		if(this.predecessorVertex.getCurrentTime() > o.getPredecessorVertex().getCurrentTime()) return 1; 
		
		
		
		return 0;
	}
}
