package Variables;

import Data.Node;
import lombok.Getter;

@Getter
public class DutyActivity implements Comparable<DutyActivity>
{
	private Node departureNode; 
	private Node arrivalNode; 
	private int departureTime; 
	private int arrivalTime; 
	private int tripOrDeadrunId; 
	private String activity; 
	private int duration; 
	
	public DutyActivity(Node departureNode, Node arrivalNode, int departureTime, int arrivalTime, int tripOrDeadrunId, String activity)
	{
		this.departureNode = departureNode; 
		this.arrivalNode = arrivalNode; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime; 
		this.tripOrDeadrunId = tripOrDeadrunId; 
		this.activity = activity; 
		this.duration = this.arrivalTime - this.departureTime; 
	}

	@Override
	public int compareTo(DutyActivity o) {
		// TODO Auto-generated method stub
		 int startTimeComparison = this.departureTime - o.getDepartureTime();

	        if (startTimeComparison != 0) {
	            return startTimeComparison;
	        } else {
	            return this.arrivalTime - o.getArrivalTime();
	        }
	}

}
