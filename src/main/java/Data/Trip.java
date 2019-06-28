package Data;

import lombok.Getter;

@Getter
public class Trip 
{
	private int lineNumber; 
	private int tripId; 
	private Node departureNode; 
	private Node arrivalNode; 
	private int departureTime; 
	private int arrivalTime; 
	private double distance; 
	
	public double multiplier; 
	public Trip(int lineNumber, int tripId, Node departureNode, Node arrivalNode, int departureTime, int arrivalTime, double distance)
	{
		this.lineNumber = lineNumber; 
		this.tripId = tripId; 
		this.departureNode = departureNode; 
		this.arrivalNode = arrivalNode; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime; 
		this.distance = distance; 
		
		this.multiplier = 0.0; 
	}

	public void setMultiplier(double value)
	{
		this.multiplier = value; 
	}
}
