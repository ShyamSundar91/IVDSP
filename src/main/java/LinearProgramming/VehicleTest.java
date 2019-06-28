package LinearProgramming;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Data.Trip;
import Variables.Block;
import Variables.BlockActivity;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class VehicleTest 
{
	private List<Block> allBlocks; 
	private List<Trip> allTrips; 
	private IloCplex cplex; 
	
	private Map<Block, IloIntVar> blockVariables; 
	private Map<Trip, IloRange> tripConstraints; 
	public VehicleTest(List<Block> allBlocks, List<Trip> allTrips) throws IloException
	{
		this.allBlocks = allBlocks; 
		this.allTrips = allTrips; 
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.blockVariables = new HashMap<Block, IloIntVar>(); 
		this.tripConstraints = addTripConstraints(); 
		addVariables();
		solve(); 
	}
	
	private Map<Trip, IloRange> addTripConstraints() throws IloException
	{
		Map<Trip, IloRange> tripConstraints = new HashMap<Trip, IloRange>(); 
		
		for(Trip trip : this.allTrips)
		{
			tripConstraints.put(trip, this.cplex.addRange(1, 1)); 
		}
		
		return tripConstraints; 
	}
	
	private void addVariables() throws IloException
	{
		for(Block block : this.allBlocks)
		{
			IloColumn variable = this.cplex.column(this.cplex.getObjective(), block.getLhs()); 
			
			for(Trip trip : block.getTripsInBlock())
			{
				variable = variable.and(this.cplex.column(this.tripConstraints.get(trip), 1)); 
			}
			
			this.blockVariables.put(block, this.cplex.intVar(variable, 0, 1)); 
		}
	}
	
	private void solve() throws IloException
	{
		if(this.cplex.solve())
		{
			System.out.println(this.cplex.getObjValue());
			for(Block block : this.blockVariables.keySet())
			{
				double value = this.cplex.getValue(this.blockVariables.get(block)); 
				if(value > 0.99)
				{
					for(BlockActivity ba : block.getBlockActivities())
					{
						System.out.println(block.getBlockId() + "; " + block.getDistance() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
					}
				}
			}
		}
	}
}
