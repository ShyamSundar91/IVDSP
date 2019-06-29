package InitialSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Subproblems.DriverSubproblem;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class DriverScheduling 
{
	private List<Trip> tripsInLine; 
	private List<Block> blocksInSolution; 
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution; 
	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripsConstraints; 
	private Map<Duty, IloNumVar> dutyVariables; 
	private Map<Trip, IloNumVar> slackTripDuty;
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
		
		this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
		this.slackTripDuty = new HashMap<Trip, IloNumVar>(); 
		this.tripsConstraints = addTripConstraints(); 
		
		columnGeneration(); 
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
			
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				
				for(Trip trip : this.tripsInLine)
				{
					tripsDriverDual.put(trip, 0.0); 
					tripsDriverDual.replace(trip, this.cplex.getDual(this.tripsConstraints.get(trip))); 
				}
				
				List<Duty> dutiesGenerated = new ArrayList<Duty>(); 
				DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, tripsDriverDual, new HashMap<Deadrun, Double>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), this.tripsInLine, this.deadrunsInSolution, this.idleTimesInSolution, false); 
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
	
	private void addDutyVariables(List<Duty> duties) throws IloException
	{
		for(Duty duty : duties)
		{
			IloColumn dutyVariable = this.cplex.column(this.cplex.getObjective(), duty.getTotalAmountPaid()); 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.tripsConstraints.get(trip), 1)); 
			}
			
			this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 0, Double.MAX_VALUE, "duty_" + duty.getDutyId()));
		}
	}

}
