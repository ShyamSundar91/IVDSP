package LinearProgramming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.DriverSubproblem;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class IntegratedMasterProblem 
{
	private List<Block> initialBlocks; 
	private List<Duty> initialDuties; 
	private List<Trip> trips; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripVehicleConstraints; 
	private Map<Trip, IloRange> tripDriverConstraints; 
	private Map<Deadrun, IloRange> deadrunLowerLimitLinkingConstraints; 
	private Map<Deadrun, IloRange> deadrunUpperLimitLinkingConstraints; 
	private Map<IdleTime, IloRange> continousBusAttendanceConstraints; 
	private Map<Block, IloNumVar> blockVariables; 
	private Map<Duty, IloNumVar> dutyVariables; 
	private Map<Trip, IloNumVar> slackTripBlock; 
	private Map<Trip, IloNumVar> slackTripDuty; 
	private List<List<Double>> lowerBound; 
	public IntegratedMasterProblem(List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> initialBlocks, List<Duty> initialDuties) throws IloException
	{
		this.trips = trips; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.initialBlocks = initialBlocks; 
		this.initialDuties = initialDuties; 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.tripVehicleConstraints = addTripVehicleConstraints(this.trips); 
		this.tripDriverConstraints = addTripDriverVehicleConstraints(this.trips);
		this.deadrunLowerLimitLinkingConstraints = addDeadrunLowerLimitLinkingConstrains(this.deadruns); 
		this.deadrunUpperLimitLinkingConstraints = addDeadrunUpperLimitLinkingConstraints(this.deadruns); 
		this.continousBusAttendanceConstraints = addContinuousBusAttendanceConstraints(this.idleTimes); 
		this.blockVariables = new HashMap<Block, IloNumVar>(); 
		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
		
		addBlockVariables(this.initialBlocks); 
		addDutyVariables(this.initialDuties); 
		this.lowerBound = new ArrayList<List<Double>>(); 
		
		columnGeneration(); 
		for(List<Double> lb : this.lowerBound)
		{
			System.out.println(lb.get(0) + "; " + lb.get(1) + "; " + lb.get(2));
		}
		//solveAsMIP(); 
	}
	
	private void columnGeneration() throws IloException
	{
		int iterationNumber = 0; 
		int status = 0; 
		
		this.cplex.setOut(null);
		//this.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
		this.cplex.setParam(IloCplex.IntParam.ParallelMode, 1);
		//this.cplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iterationNumber);
			System.out.println("Number of blocks = " + this.blockVariables.size());
			System.out.println("Number of duties = " + this.dutyVariables.size());
			
			Map<Trip, Double> tripsVehicleDual = new HashMap<Trip, Double>(); 
			Map<Trip, Double> tripsDriverDual = new HashMap<Trip, Double>(); 
			Map<Deadrun, Double> deadrunsLowerLimitDual = new HashMap<Deadrun, Double>(); 
			Map<Deadrun, Double> deadrunsUpperLimitDual = new HashMap<Deadrun, Double>(); 
			Map<IdleTime, Double> idleTimesDual = new HashMap<IdleTime, Double>();
			
			List<Double> iter = new ArrayList<Double>(); 
			iter.add((double)this.blockVariables.size());
			iter.add((double)this.dutyVariables.size()); 
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				iter.add(this.cplex.getObjValue()); 
				this.lowerBound.add(iter); 
				
				for(Trip trip : this.trips)
				{
					tripsVehicleDual.put(trip, 0.0); 
					tripsDriverDual.put(trip, 0.0); 
					tripsVehicleDual.replace(trip, this.cplex.getDual(this.tripVehicleConstraints.get(trip))); 
					tripsDriverDual.replace(trip, this.cplex.getDual(this.tripDriverConstraints.get(trip))); 
				}
				
				for(Deadrun deadrun : this.deadruns)
				{
					deadrunsLowerLimitDual.put(deadrun, 0.0); 
					deadrunsUpperLimitDual.put(deadrun, 0.0); 
					deadrunsLowerLimitDual.replace(deadrun, this.cplex.getDual(this.deadrunLowerLimitLinkingConstraints.get(deadrun))); 
					deadrunsUpperLimitDual.replace(deadrun, this.cplex.getDual(this.deadrunUpperLimitLinkingConstraints.get(deadrun))); 
				}
				
				for(IdleTime idleTime : this.idleTimes)
				{
					idleTimesDual.put(idleTime, 0.0);
					idleTimesDual.replace(idleTime, this.cplex.getDual(this.continousBusAttendanceConstraints.get(idleTime))); 
				}
				
				status = solveSubproblems(iterationNumber, tripsVehicleDual, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, false); 
				
			}
			else
			{
				//this.cplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Auto);
				System.out.println("Cplex status : " + this.cplex.getStatus());
				iter.add(Double.MAX_VALUE);
				this.lowerBound.add(iter); 
				
				int totalConstraints = this.tripVehicleConstraints.size() + this.tripDriverConstraints.size() + this.deadrunLowerLimitLinkingConstraints.size() + this.deadrunUpperLimitLinkingConstraints.size() + this.continousBusAttendanceConstraints.size(); 
				IloRange[] farkasConstraints = new IloRange[totalConstraints]; 
				double [] farkasValues = new double[totalConstraints]; 
				
				this.cplex.dualFarkas(farkasConstraints, farkasValues); 
				
				for(int i = 0; i < totalConstraints; i++)
				{
					boolean foundConstraint = false; 
					for(Trip trip : this.trips)
					{
						if(this.tripVehicleConstraints.get(trip).equals(farkasConstraints[i]))
						{
							tripsVehicleDual.put(trip, farkasValues[i]);
							foundConstraint = true; 
							break; 
						}
						else if(this.tripDriverConstraints.get(trip).equals(farkasConstraints[i]))
						{
							tripsDriverDual.put(trip, farkasValues[i]); 
							foundConstraint = true; 
							break; 
						}
					}
					
					if(!foundConstraint)
					{
						for(Deadrun deadrun : this.deadruns)
						{
							if(this.deadrunLowerLimitLinkingConstraints.get(deadrun).equals(farkasConstraints[i]))
							{
								deadrunsLowerLimitDual.put(deadrun, farkasValues[i]); 
								foundConstraint = true; 
								break; 
							}
							else if(this.deadrunUpperLimitLinkingConstraints.get(deadrun).equals(farkasConstraints[i]))
							{
								deadrunsUpperLimitDual.put(deadrun, farkasValues[i]); 
								foundConstraint = true; 
								break; 
							}
						}
					}
					
					if(!foundConstraint)
					{
						for(IdleTime idleTime : this.idleTimes)
						{
							if(this.continousBusAttendanceConstraints.get(idleTime).equals(farkasConstraints[i]))
							{
								idleTimesDual.put(idleTime, farkasValues[i]); 
								foundConstraint = true; 
								break; 
							}
						}
					}
				}
				
				status = solveSubproblems(iterationNumber, tripsVehicleDual, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, true); 
			}
			iterationNumber++; 
		}
	}
	
	private int solveSubproblems(int iterationNumber, Map<Trip, Double> tripsVehicleDual, Map<Trip, Double> tripsDriverDual, Map<Deadrun, Double> deadrunsLowerLimitDual, Map<Deadrun, Double> deadrunsUpperLimitDual, Map<IdleTime, Double> idleTimesDual, boolean usedFarkas) throws IloException
	{
		int status = 0; 
		List<Block> blocksGenerated = new ArrayList<Block>(); 
		//if(iterationNumber%2 == 0)
		{
			VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(iterationNumber, this.vehicleGraphs, tripsVehicleDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, usedFarkas); 
			blocksGenerated = vehicleSubproblem.getBlocksGenerated(); 
		}
		
		List<Duty> dutiesGenerated = new ArrayList<Duty>(); 
		//if(iterationNumber%2 != 0 || blocksGenerated.isEmpty())
		{
			DriverSubproblem driverSubproblem = new DriverSubproblem(iterationNumber, this.driverGraphs, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, usedFarkas); 
			dutiesGenerated = driverSubproblem.getDutiesGenerated(); 
		}
		
		
		if(!blocksGenerated.isEmpty())
		{
			addBlockVariables(blocksGenerated); 
		}
		
		if(!dutiesGenerated.isEmpty())
		{
			addDutyVariables(dutiesGenerated); 
		}
		
		if(blocksGenerated.isEmpty() && dutiesGenerated.isEmpty())
		{
			status = 1; 
		}
		
		return status; 
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
		
		IloNumVar[] dutyColumns = this.dutyVariables.values().toArray(new IloNumVar[this.dutyVariables.size()]); 
		this.cplex.add(this.cplex.conversion(dutyColumns, IloNumVarType.Int)); 
		
		for(int i = 0; i < dutyColumns.length; i++)
		{
			dutyColumns[i].setUB(1);
		}
		
		IloNumVar[] slackBlock = this.slackTripBlock.values().toArray(new IloNumVar[this.slackTripBlock.size()]); 
		this.cplex.delete(slackBlock);
		
		IloNumVar[] slackDuty = this.slackTripDuty.values().toArray(new IloNumVar[this.slackTripDuty.size()]); 
		this.cplex.delete(slackDuty);
		
		//this.cplex.exportModel("cplex.lp");
		this.cplex.setParam(IloCplex.BooleanParam.PreInd, true);
		this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
		this.cplex.setParam(IloCplex.Param.TimeLimit, 3600);
		this.cplex.setOut(System.out);
		if(this.cplex.solve())
		{
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
			
			for(Duty duty : this.dutyVariables.keySet())
			{
				double value = this.cplex.getValue(this.dutyVariables.get(duty)); 
				if(value > 0.99)
				{
					for(DutyActivity da : duty.getDutyActivities())
					{
						System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
					}
				}
			}
		}
	}
	
	private void addBlockVariables(List<Block> blocks) throws IloException
	{
		for(Block block : blocks)
		{
			IloColumn blockVariable = this.cplex.column(this.cplex.getObjective(), block.getLhs()); 
			
			for(Trip trip : block.getTripsInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.tripVehicleConstraints.get(trip), 1)); 
			}
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), -1)); 
				
				blockVariable = blockVariable.and(this.cplex.column(this.deadrunUpperLimitLinkingConstraints.get(deadrun), -2)); 
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), -1)); 
			}
			
			this.blockVariables.put(block, this.cplex.numVar(blockVariable, 0, Double.MAX_VALUE, "block_" + block.getBlockId())); 
		}
	}
	
	private void addDutyVariables(List<Duty> duties) throws IloException
	{
		for(Duty duty : duties)
		{
			IloColumn dutyVariable = this.cplex.column(this.cplex.getObjective(), duty.getTotalAmountPaid()); 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.tripDriverConstraints.get(trip), 1)); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), 1)); 
				
				dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunUpperLimitLinkingConstraints.get(deadrun), 1)); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), 1)); 
			}
			
			this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 0, Double.MAX_VALUE, "duty_" + duty.getDutyId())); 
		}
	}
	
	private Map<Trip, IloRange> addTripVehicleConstraints(List<Trip> trips) throws IloException
	{
		Map<Trip, IloRange> tripVehicleConstraints = new HashMap<Trip, IloRange>(); 
		this.slackTripBlock = new HashMap<Trip, IloNumVar>(); 
		for(Trip trip : trips)
		{
			tripVehicleConstraints.put(trip, this.cplex.addRange(1, 1, "ctTripVehicle_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripVehicleConstraints.get(trip), 1)); 
			this.slackTripBlock.put(trip, this.cplex.numVar(slack, 0, 1, "slackBlock_" + trip.getTripId())); 
		}
		return tripVehicleConstraints; 
	}
	
	private Map<Trip, IloRange> addTripDriverVehicleConstraints(List<Trip> trips) throws IloException
	{
		Map<Trip, IloRange> tripDriverConstraints = new HashMap<Trip, IloRange>(); 
		this.slackTripDuty = new HashMap<Trip, IloNumVar>();
		for(Trip trip : trips)
		{
			tripDriverConstraints.put(trip, this.cplex.addRange(1, Double.MAX_VALUE, "ctTripDriver_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripDriverConstraints.get(trip), 1)); 
			this.slackTripDuty.put(trip, this.cplex.numVar(slack, 0, 1, "slackBlock_" + trip.getTripId())); 
		}
		
		return tripDriverConstraints; 
	}
	
	private Map<Deadrun, IloRange> addDeadrunLowerLimitLinkingConstrains(Set<Deadrun> deadruns) throws IloException
	{
		Map<Deadrun, IloRange> deadrunLowerLimitLinkingConstraints = new HashMap<Deadrun, IloRange>(); 
		
		for(Deadrun deadrun : deadruns)
		{
			deadrunLowerLimitLinkingConstraints.put(deadrun, this.cplex.addRange(0, Double.MAX_VALUE, "ctDeadrunLowerLimit_" + deadrun.getDeadrunId())); 
		}
		
		return deadrunLowerLimitLinkingConstraints; 
	}
	
	private Map<Deadrun, IloRange> addDeadrunUpperLimitLinkingConstraints(Set<Deadrun> deadruns) throws IloException
	{
		Map<Deadrun, IloRange> deadrunUpperLimitLinkingConstraints = new HashMap<Deadrun, IloRange>(); 
		
		for(Deadrun deadrun : deadruns)
		{
			deadrunUpperLimitLinkingConstraints.put(deadrun, this.cplex.addRange(-Double.MAX_VALUE, 0, "ctDeadrunUpperLimit_" + deadrun.getDeadrunId())); 
		}
		
		return deadrunUpperLimitLinkingConstraints; 
	}
	
	private Map<IdleTime, IloRange> addContinuousBusAttendanceConstraints(Set<IdleTime> idleTimes) throws IloException
	{
		Map<IdleTime, IloRange> continuousBusAttendanceConstraints = new HashMap<IdleTime, IloRange>(); 
		
		for(IdleTime idleTime : idleTimes)
		{
			continuousBusAttendanceConstraints.put(idleTime, this.cplex.addRange(0, 0, "ctIdleTime_" + idleTime.getNode().getNodeId() + "_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime())); 
		}
		
		return continuousBusAttendanceConstraints; 
	}
	
}
