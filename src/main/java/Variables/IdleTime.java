package Variables;

import Data.Node;
import Data.Trip;
import lombok.Getter;

@Getter
public class IdleTime 
{
	private Node node; 
	private int departureTime; 
	private int arrivalTime; 
	private Trip predecessotTrip; 
	private Trip successorTrip; 
	
	public IdleTime(Node node, int departureTime, int arrivalTime, Trip predecessotTrip, Trip successorTrip)
	{
		this.node = node; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime; 
		this.predecessotTrip = predecessotTrip; 
		this.successorTrip = successorTrip; 
	}

}
