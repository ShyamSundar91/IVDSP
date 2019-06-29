package Subproblems;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

public class DriverSubproblem 
{
	private int globalIterationNumber; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private Map<Trip, Double> dualValuesOfTripIDs; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit; 
	private Map<IdleTime, Double> dualValuesOfIdleTimes;
	private List<Trip> tripsInSolution; 
	private List<Deadrun> deadrunsInSolution; 
	private List<IdleTime> idleTimesInSolution;
	private boolean usedFarkas; 
	@Getter
	private List<Duty> dutiesGenerated; 

	public DriverSubproblem(int globalIterationNumber, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Trip, Double> dualValuesOfTripIDs, Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit, Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit, Map<IdleTime, Double> dualValuesOfIdleTimes,
			List<Trip> tripsInSolution, List<Deadrun> deadrunsInSolution, List<IdleTime> idleTimesInSolution, boolean usedFarkas)
	{
		this.globalIterationNumber = globalIterationNumber;  
		this.driverGraphs = driverGraphs; 
		this.dualValuesOfTripIDs = dualValuesOfTripIDs; 
		this.dualValuesOfDeadrunsLowerLimit = dualValuesOfDeadrunsLowerLimit; 
		this.dualValuesOfDeadrunsUpperLimit = dualValuesOfDeadrunsUpperLimit; 
		this.dualValuesOfIdleTimes = dualValuesOfIdleTimes; 
		this.tripsInSolution = tripsInSolution; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		this.usedFarkas = usedFarkas; 
		
		chooseSubproblem(); 
	}
	
	private void chooseSubproblem()
	{
		boolean allSubproblemsVisited = false; 
		List<DutyTypeDepot> subproblems = this.driverGraphs.keySet().stream().collect(Collectors.toList()); 
		
		int numberOfSubProblemsVisited = 0; 
		
		while(!allSubproblemsVisited)
		{
			DutyTypeDepot chosenSubproblem = subproblems.get((this.globalIterationNumber % subproblems.size()));
			System.out.println("Chosen subproblem " + chosenSubproblem.getDescription());
			solveSubProblem(chosenSubproblem); 
			if(!this.dutiesGenerated.isEmpty())
			{
				break; 
			}
			else
			{
				this.globalIterationNumber++; 
				numberOfSubProblemsVisited++;
				if(numberOfSubProblemsVisited == subproblems.size())
				{
					allSubproblemsVisited = true; 
				}
			}
		}
	}
	
	private void solveSubProblem(DutyTypeDepot chosenSubproblem)
	{
		/*List<VehicleVertex> tripVertices = this.vehicleGraphs.get(chosenSubproblem).vertexSet().stream().filter(v -> v.getVertexId() != -1 && v.getVertexId() != Integer.MAX_VALUE).collect(Collectors.toList()); 
		for(VehicleVertex tripVertex : tripVertices)
		{
			tripVertex.calculateReducedCostOfVertex(this.dualValuesOfTripIDs.get(tripVertex.getTrip()));
		}*/
		
		for(DriverArc driverArc : this.driverGraphs.get(chosenSubproblem).edgeSet())
		{
			driverArc.calculateReducedCostOfArc(this.dualValuesOfTripIDs, this.dualValuesOfDeadrunsLowerLimit, this.dualValuesOfDeadrunsUpperLimit, this.dualValuesOfIdleTimes, this.usedFarkas);
		}
		
		for(DriverVertex vertex : this.driverGraphs.get(chosenSubproblem).vertexSet())
		{
			vertex.getLabels().clear();
		}
		
		DriverRCSPP rcspp = new DriverRCSPP(chosenSubproblem.getDutyType(), this.driverGraphs.get(chosenSubproblem), this.tripsInSolution, this.deadrunsInSolution, this.idleTimesInSolution); 
		this.dutiesGenerated = rcspp.getDutiesGenerated(); 
		System.out.println("Number of duties generated " + this.dutiesGenerated.size());
	}


}
