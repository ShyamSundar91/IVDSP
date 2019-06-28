package Variables;

import Data.Node;
import lombok.Getter;

@Getter
public class BlockActivity implements Comparable<BlockActivity> 
{
	private Node departureNode; 
	private Node arrivalNode; 
	private int departureTime; 
	private int arrivalTime; 
	private int tripOrDeadrunId; 
	private double distance; 
	private String activity; 
	
	public BlockActivity(Node departureNode, Node arrivalNode, int departureTime, int arrivalTime, double distance, int tripOrDeadrunId, String activity)
	{
		this.departureNode = departureNode;
		this.arrivalNode = arrivalNode; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime; 
		this.distance = distance; 
		this.tripOrDeadrunId = tripOrDeadrunId; 
		this.activity = activity; 
	}

	public int compareTo(BlockActivity o) {
		// TODO Auto-generated method stub
		 int startTimeComparison = this.departureTime - o.getDepartureTime();

	        if (startTimeComparison != 0) {
	            return startTimeComparison;
	        } else {
	            return this.arrivalTime - o.getArrivalTime();
	        }
	}

}
