package Networks;

import java.util.ArrayList;
import java.util.List;

import Data.Node;
import Data.Trip;
import Subproblems.LabelDriver;
import Variables.Deadrun;
import lombok.Getter;

@Getter
public class DriverVertex implements Comparable<DriverVertex> 
{
	private Node currentNode; 
	private int currentTime; 
	private Deadrun deadrun; 
	private Trip trip; 
	private boolean departure; 
	private double reducedCostOfVertex; 
	private List<LabelDriver> labels; 
	
	private double cost; 
	public DriverVertex(Node currentNode, int currentTime, Trip trip, Deadrun deadrun, boolean departure)
	{
		this.currentNode = currentNode; 
		this.currentTime = currentTime; 
		this.trip = trip; 
		this.deadrun = deadrun; 
		this.departure = departure; 
		this.reducedCostOfVertex = 0.0; 
		
		this.labels = new ArrayList<LabelDriver>(); 
		
		this.cost = currentTime; 
		if((this.trip != null || this.deadrun != null)) {
		    this.cost = this.cost - 1000; 
		}
	}
	
	public int compareTo(DriverVertex dv) {
        
        if(this.cost < dv.getCost()) return -1; 
        if(this.cost > dv.getCost()) return 1; 
        
        return 0;
    }

}
