package BranchPriceVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import lombok.Getter;

@Getter
public class BBNodeVehicle 
{
	private List<Trip> trips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
	private Map<Block, Integer> initialBlocksAndGenerated; 
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<Deadrun, Double> deadrunUpperLimit; 
	private Map<IdleTime, Double> idleTimeMultipliers; 
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripConstraints; 
	private Map<Block, IloNumVar> blockVariables; 
	private Map<Trip, IloNumVar> slackVariables; 
	
	private List<Block> blocksInSolution; 
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution; 
	private Map<Block, Double> fractionalValuesOfBlockVariables; 
	private boolean solutionInteger; 
	private boolean earlyTermination; 
	private int noImprovement; 
	private boolean allowLineChange; 
	private double lpObjective; 
	private double totalTimeSpentInMaster; 
	private double totalTimeSpentInSub; 
	public BBNodeVehicle(List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph, Map<Block, Integer> initialBlocksAndGenerated, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, boolean earlyTermination) throws IloException
	{
		this.trips = trips; 
		this.vehicleGraph = vehicleGraph; 
		this.initialBlocksAndGenerated = initialBlocksAndGenerated; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers;
		this.deadrunUpperLimit = new HashMap<Deadrun, Double>(); 
		for(Deadrun deadrun : this.deadrunMultipliers.keySet())
		{
			this.deadrunUpperLimit.put(deadrun, 0.0); 
		}
		this.earlyTermination = earlyTermination; 
		this.allowLineChange = false; 
		this.noImprovement = 0; 
		this.totalTimeSpentInMaster = 0;
		this.totalTimeSpentInSub = 0; 
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.blockVariables = new HashMap<Block, IloNumVar>(); 
		this.slackVariables = new HashMap<Trip, IloNumVar>(); 
		this.tripConstraints = addTripConstraints(); 
		addBlockVariables(this.initialBlocksAndGenerated.keySet()); 
	}
	
	public void solveCG() throws IloException
	{
		int status = 0; 
		int iteration = 0; 
		
		this.cplex.setOut(null);
		this.cplex.setParam(IloCplex.Param.Parallel, 1);
		double previousObj = Double.MAX_VALUE;
		 
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iteration);
			System.out.println("Number of blocks = " + this.blockVariables.size());
			
			Map<Trip, Double> tripsVehicleDual = new HashMap<Trip, Double>();
			
			double startMP = System.currentTimeMillis(); 
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				lpObjective = this.cplex.getObjValue(); 
				double endMP = System.currentTimeMillis(); 
				this.totalTimeSpentInMaster = this.totalTimeSpentInMaster + (endMP - startMP)/(double)1000; 
				for(Trip trip : this.trips)
				{
					tripsVehicleDual.put(trip, 0.0); 
					tripsVehicleDual.replace(trip, this.cplex.getDual(this.tripConstraints.get(trip))); 
				}
				
				
				if(this.earlyTermination || !this.allowLineChange)
				{
					double change = ((previousObj- lpObjective)/previousObj) * 100.00; 
					if(change < 0.001)
					{
						noImprovement++; 
					}
					else
					{
						noImprovement = 0; 
					}
					
					if(noImprovement >= 10)
					{
						if(!this.allowLineChange)
						{
							this.allowLineChange = true; 
							noImprovement = 0; 
							System.out.println("Allow Line Change");
						}
						else if(this.earlyTermination)
						{
							status = 1; 
						}
					}
					previousObj = lpObjective;
				}
				
				if(status != 1)
				{
					 
					Set<Block> blocksGenerated = new HashSet<Block>(); 
					
					double startSub = System.currentTimeMillis(); 
					VehicleSubproblem sub = new VehicleSubproblem(iteration,this.trips, this.vehicleGraph, tripsVehicleDual, this.deadrunMultipliers, this.deadrunUpperLimit, this.idleTimeMultipliers, this.allowLineChange); 
					blocksGenerated.addAll(sub.getBlocksGenerated()); 
					double endSub = System.currentTimeMillis(); 
					this.totalTimeSpentInSub = this.totalTimeSpentInSub + (endSub - startSub)/(double)1000; 
					if(!blocksGenerated.isEmpty())
					{
						for(Block block : blocksGenerated)
						{
							this.initialBlocksAndGenerated.put(block, 0); 
						}
						addBlockVariables(blocksGenerated); 
					}
					else
					{ 
						if(!this.allowLineChange)
						{
							this.allowLineChange = true; 
							this.noImprovement = 0; 
							System.out.println("Allow Line Change");
						}
						else
						{
							status = 1; 
						}
					}
				}
			}
			
			iteration++; 
		}
		
		getFractionalValues(); 
		
		shutCPLEX(); 
	}
	
	private void getFractionalValues() throws UnknownObjectException, IloException
	{
		this.fractionalValuesOfBlockVariables = new HashMap<Block, Double>(); 
		this.solutionInteger = true; 
		
		for(Block block : this.blockVariables.keySet())
		{
			double value = this.cplex.getValue(this.blockVariables.get(block)); 
			if(value >= (1-1e-3))
			{
				this.fractionalValuesOfBlockVariables.put(block, value);
			}
			else if(value > 1e-3)
			{
				this.solutionInteger = false; 
				this.fractionalValuesOfBlockVariables.put(block, value); 
			}
		}
		
		if(this.solutionInteger)
		{
			this.blocksInSolution = new ArrayList<Block>(); 
			this.deadrunsInSolution = new ArrayList<Deadrun>(); 
			this.idleTimesInSolution = new ArrayList<IdleTime>(); 
			
			this.blocksInSolution.addAll(this.fractionalValuesOfBlockVariables.keySet()); 
			for(Block block : this.blocksInSolution)
			{
				this.deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
				this.idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
			}
			
			for(Trip trip : this.trips)
			{
				double value = this.cplex.getValue(this.slackVariables.get(trip)); 
				Assert.assertTrue(value < 1e-3);
			}
		}
	}
	
	private void addBlockVariables(Set<Block> blocks) throws IloException
	{
		for(Block block : blocks)
		{
			double coef = block.getTotalCostOfBlock(); 
			
			if(!this.deadrunMultipliers.isEmpty())
			{
				for(Deadrun deadrun : block.getDeadrunsInBlock())
				{
					coef = coef + this.deadrunMultipliers.get(deadrun); 
				}
				
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					coef = coef + this.idleTimeMultipliers.get(idleTime); 
				}
			}
			
			
			IloColumn blockVar = this.cplex.column(this.cplex.getObjective(), coef); 
			
			for(Trip trip : block.getTripsInBlock())
			{
				blockVar = blockVar.and(this.cplex.column(this.tripConstraints.get(trip), 1)); 
			}
			
			if(this.initialBlocksAndGenerated.get(block) == 1)
			{
				this.blockVariables.put(block, this.cplex.numVar(blockVar, 1, 1, "BlockVar_" + block.getBlockId()));
			}
			else
			{
				this.blockVariables.put(block, this.cplex.numVar(blockVar, 0, Double.MAX_VALUE, "BlockVar_" + block.getBlockId())); 
			}
			
		}
	}
	
	private Map<Trip, IloRange> addTripConstraints() throws IloException
	{
		Map<Trip, IloRange> tripConstraints = new HashMap<Trip, IloRange>(); 
		for(Trip trip : this.trips)
		{
			double rhs = 1; 
			/*if(this.initialBlocksAndGenerated.isEmpty() && this.trips.size() > 400)
			{
				rhs = Double.MAX_VALUE; 
			}*/
			tripConstraints.put(trip, this.cplex.addRange(1, rhs, "ctTripVehicle_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripConstraints.get(trip), 1)); 
			this.slackVariables.put(trip, this.cplex.numVar(slack, 0, 1)); 
		}
		
		return tripConstraints; 
	}
	
	private void shutCPLEX()
	{
		this.cplex.end();
	}

}
