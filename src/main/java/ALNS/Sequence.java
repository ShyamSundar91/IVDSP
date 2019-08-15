package ALNS;

import java.util.List;

import Data.Trip;
import Variables.Block;
import Variables.Duty;
import lombok.Getter;

@Getter
public class Sequence implements Comparable<Sequence> 
{
	private List<Trip> tripsInSequence; 
	private Block blockCoveringSequence; 
	private Duty dutyCoveringSequence;
	private double costOfSequence; 
	private int earliestStartTime; 
	private int latestEndTime; 
	public Sequence(List<Trip> tripsInSequence, Block blockCoveringSequence, Duty dutyCoveringSequence)
	{
		this.tripsInSequence = tripsInSequence; 
		this.blockCoveringSequence = blockCoveringSequence; 
		this.dutyCoveringSequence = dutyCoveringSequence; 
		double totalDurationOfTrips = this.tripsInSequence.stream().mapToDouble(t -> (t.getArrivalTime()-t.getDepartureTime())).sum(); 
		this.costOfSequence = (this.blockCoveringSequence.getTotalCostOfBlock() + this.dutyCoveringSequence.getTotalCostOfDuty())/totalDurationOfTrips; 
		
		this.earliestStartTime = this.tripsInSequence.stream().mapToInt(t -> t.getDepartureTime()).min().getAsInt();
		this.latestEndTime = this.tripsInSequence.stream().mapToInt(t -> t.getArrivalTime()).max().getAsInt(); 
	}

	public int compareTo(Sequence s) {
		
		if(this.costOfSequence < s.getCostOfSequence()) return -1; 
		if(this.costOfSequence > s.getCostOfSequence()) return 1; 
		
		return 0;
	}

}
