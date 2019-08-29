package BranchPriceDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ilog.cplex.IloCplex.UnknownObjectException;
import lombok.Getter;

@Getter
public class BBNodeDriver 
{
	private List<Trip> trips; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private Map<Duty, Integer> initialAndDutiesGenerated;
	private List<Block> blocksInSolution; 
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripsConstraints; 
	private Map<Deadrun, IloRange> deadrunConstraints; 
	private Map<IdleTime, IloRange> idleTimeConstraints; 
	private Map<Duty, IloNumVar> dutyVariables; 
	private Map<Trip, IloNumVar> slackTripDuty;
	private Map<Deadrun, IloNumVar> slackDeadrunDuty; 
	private Map<IdleTime, IloNumVar> slackIdleTimeDuty; 
	
	private List<Duty> dutiesInSolution; 
	private Map<Duty, Double> fractionalValuesOfDutyVariables; 
	private boolean solutionInteger; 
	private boolean earlyTermination; 
	private boolean allowBlockChange; 
	private int noImprovement; 
	private boolean useSubNetwork; 
	private double lpObjective; 
	private double totalTimeSpentInMaster; 
	private double totalTimeSpentInSub; 
	 
	public BBNodeDriver(List<Trip> trips, List<Block> blocksInSolution, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution,
			Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Duty, Integer> initialAndDutiesGenerated, boolean earlyTermination) throws IloException
	{
		this.trips = trips; 
		this.blocksInSolution = blocksInSolution; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		this.driverGraphs = driverGraphs;  
		this.initialAndDutiesGenerated = initialAndDutiesGenerated; 
		this.earlyTermination = earlyTermination; 
		this.allowBlockChange = false; 
		this.noImprovement = 0; 
		this.useSubNetwork = false; 
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
		this.slackTripDuty = new HashMap<Trip, IloNumVar>(); 
		this.slackDeadrunDuty = new HashMap<Deadrun, IloNumVar>(); 
		this.slackIdleTimeDuty = new HashMap<IdleTime, IloNumVar>(); 
		this.tripsConstraints = addTripConstraints();
		this.deadrunConstraints = addDeadrunConstraints(); 
		this.idleTimeConstraints = addIdleTimeConstraints(); 
		addDutyVariables(this.initialAndDutiesGenerated.keySet()); 
		 
		this.totalTimeSpentInMaster = 0.0; 
		this.totalTimeSpentInSub = 0.0; 
		//solveAsMIP();
	}
	
	public void solveCG() throws IloException
	{
		int status = 0; 
		int iteration = 0; 
		 
		double previousObj = Double.MAX_VALUE; 
		
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
			Map<IdleTime, Double> idleTimeDual = new HashMap<IdleTime, Double>(); 
			
			double startMP = System.currentTimeMillis(); 
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				lpObjective = this.cplex.getObjValue(); 
				double endMP = System.currentTimeMillis(); 
				this.totalTimeSpentInMaster = this.totalTimeSpentInMaster + (endMP - startMP)/(double)1000; 
				
				for(Trip trip : this.trips)
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
				
				for(IdleTime idleTime : this.idleTimesInSolution)
				{
					idleTimeDual.put(idleTime, 0.0); 
					idleTimeDual.replace(idleTime, this.cplex.getDual(this.idleTimeConstraints.get(idleTime))); 
				}
				
				if(this.earlyTermination || !this.allowBlockChange)
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
						if(!this.allowBlockChange)
						{
							this.allowBlockChange = true; 
							noImprovement = 0;
							System.out.println("Allow Block Change");
						}
						else
						{
							status = 1; 
						}
					}
					previousObj = lpObjective;
				}
				
				if(status != 1)
				{
					Set<Duty> dutiesGenerated = new HashSet<Duty>(); 
					double startSub = System.currentTimeMillis(); 
					DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimeDual, this.trips, this.deadrunsInSolution, this.idleTimesInSolution, this.allowBlockChange, this.useSubNetwork); 
					dutiesGenerated.addAll(driverSubproblem.getDutiesGenerated()); 
					double endSub = System.currentTimeMillis(); 
					this.totalTimeSpentInSub = this.totalTimeSpentInSub + (endSub - startSub)/(double)1000; 
					
					for(Duty duty : dutiesGenerated)
					{
						Assert.assertTrue(!this.dutyVariables.keySet().contains(duty));
					}
					
					if(!dutiesGenerated.isEmpty())
					{
						for(Duty duty : dutiesGenerated)
						{
							this.initialAndDutiesGenerated.put(duty, 0); 
						}
						addDutyVariables(dutiesGenerated); 
					}
					else
					{
						if(!this.allowBlockChange)
						{
							this.allowBlockChange = true; 
							this.noImprovement = 0; 
							System.out.println("Allow Block Change");
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
		this.fractionalValuesOfDutyVariables = new HashMap<Duty, Double>(); 
		this.solutionInteger = true; 
		
		for(Duty duty : this.dutyVariables.keySet())
		{
			double value = this.cplex.getValue(this.dutyVariables.get(duty)); 
			if(value >= (1-1e-3))
			{
				this.fractionalValuesOfDutyVariables.put(duty, value); 
			}
			else if(value > 1e-3)
			{
				this.solutionInteger = false; 
				this.fractionalValuesOfDutyVariables.put(duty, value); 
			}
		}
		
		if(this.solutionInteger)
		{
			this.dutiesInSolution = new ArrayList<Duty>();
			this.dutiesInSolution.addAll(this.fractionalValuesOfDutyVariables.keySet()); 
		}
		
		for(Trip trip : this.trips)
		{
			double value = this.cplex.getValue(this.slackTripDuty.get(trip)); 
			Assert.assertTrue(value < 1e-3);
		}
		
		for(Deadrun deadrun : this.deadrunsInSolution)
		{
			double value = this.cplex.getValue(this.slackDeadrunDuty.get(deadrun));
			Assert.assertTrue(value < 1e-3);		
		}
		
		for(IdleTime idleTime : this.idleTimesInSolution)
		{
			double value = this.cplex.getValue(this.slackIdleTimeDuty.get(idleTime)); 
			//System.out.println(idleTime.getArrivalTime() - idleTime.getDepartureTime() + ", " + idleTime.getNode().getNodeId() + ", " + idleTime.getPredecessotTrip().getTripId() + ", " + idleTime.getSuccessorTrip().getTripId());
			Assert.assertTrue(value < 1e-3);
		}
	}
	
	
	private Map<Trip, IloRange> addTripConstraints() throws IloException
	{
		Map<Trip, IloRange> tripConstraints = new HashMap<Trip, IloRange>(); 
		
		for(Trip trip : this.trips)
		{
			tripConstraints.put(trip, this.cplex.addRange(1, 1, "ctTrip_" + trip.getTripId())); 
			
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
			deadrunConstraints.put(deadrun, this.cplex.addRange(1, 1, "ctDeadrun_" + deadrun.getDeadrunId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(deadrunConstraints.get(deadrun), 1)); 
			this.slackDeadrunDuty.put(deadrun, this.cplex.numVar(slack, 0, 1, "slackDeadrun_" + deadrun.getDeadrunId())); 
		}
		
		return deadrunConstraints; 
	}
	
	//Is redundant for some problems. Need to find a better way to remove it altogether.
	private Map<IdleTime, IloRange> addIdleTimeConstraints() throws IloException
	{
		Map<IdleTime, IloRange> idleTimeConstraints = new HashMap<IdleTime, IloRange>(); 
		
		for(IdleTime idleTime : this.idleTimesInSolution)
		{
			idleTimeConstraints.put(idleTime, this.cplex.addRange(1, 1, "ctIdleTime_" + idleTime.getPredecessotTrip().getTripId() + "_" + idleTime.getSuccessorTrip().getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(idleTimeConstraints.get(idleTime), 1)); 
			this.slackIdleTimeDuty.put(idleTime, this.cplex.numVar(slack, 0, 1)); 
		}
		
		return idleTimeConstraints; 
	}
	
	
	private void addDutyVariables(Set<Duty> duties) throws IloException
	{
		for(Duty duty : duties)
		{
			if(!this.deadrunsInSolution.isEmpty())
			{
				Assert.assertTrue(this.trips.containsAll(duty.getTripsInDuty()));
				Assert.assertTrue(this.deadrunsInSolution.containsAll(duty.getDeadrunsInDuty()));
				Assert.assertTrue(this.idleTimesInSolution.containsAll(duty.getIdleTimesInDuty()));
			}
			
			
			IloColumn dutyVariable = this.cplex.column(this.cplex.getObjective(), duty.getTotalCostOfDuty()); 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.tripsConstraints.get(trip), 1)); 				
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				if(this.deadrunsInSolution.contains(deadrun))
				{
					dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunConstraints.get(deadrun), 1));
				}
				
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				if(this.idleTimesInSolution.contains(idleTime))
				{
					dutyVariable = dutyVariable.and(this.cplex.column(this.idleTimeConstraints.get(idleTime), 1)); 
				}
				
			}
			
			if(this.initialAndDutiesGenerated.get(duty) == 1)
			{
				this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 1, 1, "duty_" + duty.getDutyId()));
			}
			else
			{
				this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 0, Double.MAX_VALUE, "duty_" + duty.getDutyId()));
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
		
		IloNumVar[] slackIdleTime = this.slackIdleTimeDuty.values().toArray(new IloNumVar[this.slackIdleTimeDuty.size()]); 
		this.cplex.delete(slackIdleTime);
		
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
	
	private void shutCPLEX()
	{
		this.cplex.end();
	}

}
