package InitialSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Subproblems.DriverSubproblem;
import Variables.Block;
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
import lombok.Getter;

public class DriverScheduling 
{
	private List<Trip> tripsInLine; 
	private List<Block> blocksInSolution; 
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution; 
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripsConstraints; 
	private Map<Deadrun, IloRange> deadrunConstraints; 
	private Map<Duty, IloNumVar> dutyVariables; 
	private Map<Trip, IloNumVar> slackTripDuty;
	private Map<Deadrun, IloNumVar> slackDeadrunDuty; 
	@Getter
	private List<Duty> dutiesInSolution; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	public DriverScheduling(List<Trip> tripsInLine, List<Block> blocksInSolution, List<Deadrun> deadrunsInSolution, List<IdleTime> idleTimesInSolution,
			Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
	{
		this.tripsInLine = tripsInLine; 
		this.blocksInSolution = blocksInSolution; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		this.driverGraphs = driverGraphs; 
		this.dutiesInSolution = new ArrayList<Duty>(); 
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
		this.slackTripDuty = new HashMap<Trip, IloNumVar>(); 
		this.slackDeadrunDuty = new HashMap<Deadrun, IloNumVar>(); 
		this.tripsConstraints = addTripConstraints();
		this.deadrunConstraints = addDeadrunConstraints(); 
		
		columnGeneration(); 
		solveAsMIP();
	}
	
	private void columnGeneration() throws IloException
	{
		int status = 0; 
		int iteration = 0; 
		
		this.cplex.setOut(null);
		this.cplex.setParam(IloCplex.IntParam.ParallelMode, 1);
		
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iteration);
			System.out.println("Number of duties = " + this.dutyVariables.size());
			
			Map<Trip, Double> tripsDriverDual = new HashMap<Trip, Double>();
			Map<Deadrun, Double> deadrunsLowerLimitDual = new HashMap<Deadrun, Double>(); 
			Map<Deadrun, Double> deadrunsUpperLimitDual = new HashMap<Deadrun, Double>(); 
			
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				
				for(Trip trip : this.tripsInLine)
				{
					tripsDriverDual.put(trip, 0.0); 
					tripsDriverDual.replace(trip, this.cplex.getDual(this.tripsConstraints.get(trip))); 
				}
				
				for(Deadrun deadrun : this.deadrunsInSolution)
				{
					deadrunsLowerLimitDual.put(deadrun, 0.0); 
					deadrunsUpperLimitDual.put(deadrun, 0.0); 
					deadrunsLowerLimitDual.replace(deadrun, this.cplex.getDual(this.deadrunConstraints.get(deadrun))); 
				}
				
				
				List<Duty> dutiesGenerated = new ArrayList<Duty>(); 
				DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, new HashMap<IdleTime, Double>(), this.tripsInLine, this.deadrunsInSolution, this.idleTimesInSolution, false); 
				dutiesGenerated = driverSubproblem.getDutiesGenerated(); 
				
				if(!dutiesGenerated.isEmpty())
				{
					addDutyVariables(dutiesGenerated); 
				}
				else
				{
					status = 1;
				}
			}
		}
	}
	
	private void solveAsMIP() throws IloException
	{
		System.out.println("***************************************************");

		IloNumVar[] dutyColumns = this.dutyVariables.values().toArray(new IloNumVar[this.dutyVariables.size()]); 
		this.cplex.add(this.cplex.conversion(dutyColumns, IloNumVarType.Int)); 
		
		for(int i = 0; i < dutyColumns.length; i++)
		{
			dutyColumns[i].setUB(1);
		}
		
		IloNumVar[] slackDuty = this.slackTripDuty.values().toArray(new IloNumVar[this.slackTripDuty.size()]); 
		this.cplex.delete(slackDuty);
		
		IloNumVar[] slackDeadrun = this.slackDeadrunDuty.values().toArray(new IloNumVar[this.slackDeadrunDuty.size()]); 
		this.cplex.delete(slackDeadrun);
		
		this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
		this.cplex.setParam(IloCplex.Param.TimeLimit, 3600);
		this.cplex.setOut(System.out);
		
		if(this.cplex.solve())
		{
			for(Duty duty : this.dutyVariables.keySet())
			{
				double value = this.cplex.getValue(this.dutyVariables.get(duty)); 
				if(value > 0.99)
				{
					this.dutiesInSolution.add(duty); 
					for(DutyActivity da : duty.getDutyActivities())
					{
						System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
					} 
				}
			}
		}
	}
	
	private Map<Trip, IloRange> addTripConstraints() throws IloException
	{
		Map<Trip, IloRange> tripConstraints = new HashMap<Trip, IloRange>(); 
		
		for(Trip trip : this.tripsInLine)
		{
			tripConstraints.put(trip, this.cplex.addRange(1, Double.MAX_VALUE, "ctTrip_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripConstraints.get(trip), 1)); 
			this.slackTripDuty.put(trip, this.cplex.numVar(slack, 0, 1, "slackTrip_" + trip.getTripId())); 
		}
		
		return tripConstraints; 
	}
	
	private Map<Deadrun, IloRange> addDeadrunConstraints() throws IloException
	{
		Map<Deadrun, IloRange> deadrunConstraints = new HashMap<Deadrun, IloRange>(); 
		
		for(Deadrun deadrun : this.deadrunsInSolution)
		{
			deadrunConstraints.put(deadrun, this.cplex.addRange(1, Double.MAX_VALUE, "ctDeadrun_" + deadrun.getDeadrunId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(deadrunConstraints.get(deadrun), 1)); 
			this.slackDeadrunDuty.put(deadrun, this.cplex.numVar(slack, 0, 1, "slackDeadrun_" + deadrun.getDeadrunId())); 
		}
		
		return deadrunConstraints; 
	}
	
	
	private void addDutyVariables(List<Duty> duties) throws IloException
	{
		for(Duty duty : duties)
		{
			Assert.assertTrue(this.tripsInLine.containsAll(duty.getTripsInDuty()));
			Assert.assertTrue(this.deadrunsInSolution.containsAll(duty.getDeadrunsInDuty()));
			Assert.assertTrue(this.idleTimesInSolution.containsAll(duty.getIdleTimesInDuty()));
			
			IloColumn dutyVariable = this.cplex.column(this.cplex.getObjective(), duty.getTotalAmountPaid()); 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.tripsConstraints.get(trip), 1)); 				
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunConstraints.get(deadrun), 1));
			}
			
			this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 0, Double.MAX_VALUE, "duty_" + duty.getDutyId()));
		}
	}

}
