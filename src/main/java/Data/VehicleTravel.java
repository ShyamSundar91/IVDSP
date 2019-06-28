package Data;

import lombok.Getter;

@Getter
public class VehicleTravel 
{
	private Node departureNode; 
	private Node arrivalNode; 
	private int duration; 
	private double distance; 
	
	public VehicleTravel(Node departureNode, Node arrivalNode, int duration, double distance)
	{
		this.departureNode = departureNode; 
		this.arrivalNode = arrivalNode; 
		this.duration = duration; 
		this.distance = distance; 
	}

}
