package Networks;

import java.util.ArrayList;
import java.util.List;

import Data.Node;
import Data.Trip;
import Subproblems.LabelDriver;
import Variables.Deadrun;
import lombok.Getter;

@Getter
public class DriverVertex 
{
	private Node currentNode; 
	private int currentTime; 
	private Deadrun deadrun; 
	private Trip trip; 
	private boolean departure; 
	private double reducedCostOfVertex; 
	private List<LabelDriver> labels; 
	
	public DriverVertex(Node currentNode, int currentTime, Trip trip, Deadrun deadrun, boolean departure)
	{
		this.currentNode = currentNode; 
		this.currentTime = currentTime; 
		this.trip = trip; 
		this.deadrun = deadrun; 
		this.departure = departure; 
		this.reducedCostOfVertex = 0.0; 
		
		this.labels = new ArrayList<LabelDriver>(); 
	}

}
