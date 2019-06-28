package Data;

import lombok.Getter;

@Getter
public class DriverTravel 
{
	private Node departureNode; 
	private Node arrivalNode; 
	private int duration; 
	private String travelType; 

	public DriverTravel(Node departureNode, Node arrivalNode, int duration, String travelType)
	{
		this.departureNode = departureNode; 
		this.arrivalNode = arrivalNode; 
		this.duration = duration; 
		this.travelType = travelType; 
	}
}
