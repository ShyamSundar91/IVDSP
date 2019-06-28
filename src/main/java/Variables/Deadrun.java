package Variables;

import Data.Node;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(of={"departureNode","arrivalNode", "departureTime", "arrivalTime", "pullOut", "pullIn"})
@Getter
public class Deadrun 
{
	private int deadrunId; 
	private Node departureNode; 
	private Node arrivalNode; 
	private int departureTime; 
	private int arrivalTime; 
	private boolean pullOut; 
	private boolean pullIn; 
	//private boolean beforeParkRecharge; 
	//private boolean afterParkRecharge; 
	private static int counter = 1; 
	private String type; 
	public Deadrun(Node departureNode, Node arrivalNode, int departureTime, int arrivalTime, boolean pullOut, boolean pullIn)
	{
		this.deadrunId = counter++; 
		this.departureNode = departureNode; 
		this.arrivalNode = arrivalNode; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime;
		this.pullOut = pullOut;
		this.pullIn = pullIn; 
		//this.beforeParkRecharge = beforeParkRecharge; 
		//this.afterParkRecharge = afterParkRecharge; 
		
		if(this.pullIn || this.pullOut)
		{
			this.type = "Garage"; 
		}
		else
		{
			this.type = "Deadrun"; 
		}
	}

}
