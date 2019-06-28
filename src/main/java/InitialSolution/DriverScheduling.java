package InitialSolution;

import java.util.List;

import Data.Trip;
import Variables.Block;
import Variables.Deadrun;
import Variables.IdleTime;

public class DriverScheduling 
{
	private List<Trip> tripsInLine; 
	private List<Block> blocksInSolution; 
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution; 
	
	public DriverScheduling(List<Trip> tripsInLine, List<Block> blocksInSolution, List<Deadrun> deadrunsInSolution, List<IdleTime> idleTimesInSolution)
	{
		this.tripsInLine = tripsInLine; 
		this.blocksInSolution = blocksInSolution; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
	}

}
