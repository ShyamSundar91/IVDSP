package InitialSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.LabelVehicle;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import lombok.Getter;

public class VehicleSchedulingCG 
{
	private int lineNumber; 
	private List<Trip> tripsInLine; 
	private VehicleTypeDepot vehicleTypeDepot; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripConstraints; 
	private Map<Trip, IloNumVar> slackTripVariables;
	private Map<Block, IloNumVar> blockVariables; 
	@Getter
	private List<Block> blocksInSolution; 
	@Getter
	private List<Deadrun> deadrunsInSolution; 
	@Getter
	private List<IdleTime> idleTimesInSolution; 
	@Getter
	private List<Block> blocksGenerated; 
	
	public VehicleSchedulingCG(int lineNumber, List<Trip> tripsInLine, VehicleTypeDepot vehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
	{
		this.lineNumber = lineNumber; 
		this.tripsInLine = tripsInLine; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.vehicleGraph = new HashMap<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>>(); 
		this.vehicleGraph.put(this.vehicleTypeDepot, vehicleGraph);
		this.blocksInSolution = new ArrayList<Block>(); 
		this.deadrunsInSolution = new ArrayList<Deadrun>(); 
		this.idleTimesInSolution = new ArrayList<IdleTime>(); 
		this.blocksGenerated = new ArrayList<Block>(); 
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize();
		this.slackTripVariables = new HashMap<Trip, IloNumVar>(); 
		this.blockVariables = new HashMap<Block, IloNumVar>(); 
		this.tripConstraints = addTripConstraints(); 
		
		columnGeneration(); 
		solveAsMIP(); 
		this.blocksGenerated.addAll(this.blockVariables.keySet()); 
	}
	
	private void columnGeneration() throws IloException
	{
		int status = 0; 
		int iter = 0; 
		
		this.cplex.setOut(null);
		this.cplex.setParam(IloCplex.IntParam.ParallelMode, 1);
		
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iter);
			System.out.println("Number of blocks = " + this.blockVariables.size());
			
			Map<Trip, Double> tripsVehicleDual = new HashMap<Trip, Double>();
			
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				for(Trip trip : this.tripsInLine)
				{
					tripsVehicleDual.put(trip, 0.0); 
					tripsVehicleDual.replace(trip, this.cplex.getDual(this.tripConstraints.get(trip))); 
				}
				
				List<Block> blocksGenerated = new ArrayList<Block>(); 
				
				VehicleSubproblem sub = new VehicleSubproblem(iter,this.tripsInLine, this.vehicleGraph, tripsVehicleDual, new HashMap<Deadrun, Double>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), true, false); 
				blocksGenerated.addAll(sub.getBlocksGenerated()); 
				
				if(!blocksGenerated.isEmpty())
				{
					addBlockVariables(blocksGenerated); 
				}
				else
				{
					status = 1; 
				}
			}
			
			iter++; 
		}
	}
	
	private void solveAsMIP() throws IloException
	{
		System.out.println("***************************************************");

		IloNumVar[] blockColumns = this.blockVariables.values().toArray(new IloNumVar[this.blockVariables.size()]); 
		this.cplex.add(this.cplex.conversion(blockColumns, IloNumVarType.Int)); 
		
		for(int i = 0; i < blockColumns.length; i++)
		{
			blockColumns[i].setUB(1);
		}
		
		IloNumVar[] slackTrip = this.slackTripVariables.values().toArray(new IloNumVar[this.slackTripVariables.size()]); 
		this.cplex.delete(slackTrip);
				
		this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
		this.cplex.setParam(IloCplex.Param.TimeLimit, 60);
		this.cplex.setOut(System.out);
		
		if(this.cplex.solve())
		{
			System.out.println("IP Objective = " + this.cplex.getObjValue());
			for(Block block : this.blockVariables.keySet())
			{
				double value = this.cplex.getValue(this.blockVariables.get(block)); 
				if(value > 0.99)
				{
					this.blocksInSolution.add(block); 
					
					for(Deadrun deadrun : block.getDeadrunsInBlock())
					{
						Assert.assertTrue(!this.deadrunsInSolution.contains(deadrun));
						this.deadrunsInSolution.add(deadrun); 
					}
					
					for(IdleTime idleTime : block.getIdleTimesInBlock())
					{
						Assert.assertTrue(!this.idleTimesInSolution.contains(idleTime));
						this.idleTimesInSolution.add(idleTime); 
					}
				}
			}
			
		}
		else
		{
			System.out.println("Problem infeasible");
		}
	}
	
	private Map<Trip, IloRange> addTripConstraints() throws IloException
	{
		Map<Trip, IloRange> tripConstraints = new HashMap<Trip, IloRange>(); 
		for(Trip trip : tripsInLine)
		{
			tripConstraints.put(trip, this.cplex.addRange(1, 1, "ctTrip_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripConstraints.get(trip), 1)); 
			this.slackTripVariables.put(trip, this.cplex.numVar(slack, 0, 1)); 
		}
		
		return tripConstraints; 
	}
	
	private void addBlockVariables(List<Block> blocks) throws IloException
	{
		for(Block block : blocks)
		{
			IloColumn blockVariable = this.cplex.column(this.cplex.getObjective(), block.getTotalCostOfBlock()); 
			
			for(Trip trip : block.getTripsInBlock())
			{
				Assert.assertTrue(this.tripsInLine.contains(trip));
				blockVariable = blockVariable.and(this.cplex.column(this.tripConstraints.get(trip), 1)); 
			}
			
			this.blockVariables.put(block, this.cplex.numVar(blockVariable, 0, 1)); 
		}
	}

}
