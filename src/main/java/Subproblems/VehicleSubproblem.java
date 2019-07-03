package Subproblems;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class VehicleSubproblem 
{
	private int globalIterationNumber;
	private List<Trip> tripsToGenerateVariables; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs;
	private Map<Trip, Double> dualValuesOfTripIDs; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit; 
	private Map<IdleTime, Double> dualValuesOfIdleTimes;
	@Getter
	private List<Block> blocksGenerated; 

	private boolean usedFarkas; 
	public VehicleSubproblem(int globalIterationNumber, List<Trip> tripsToGenerateVariables, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph, Map<Trip, Double> dualValuesOfTripIDs, Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit, Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit, Map<IdleTime, Double> dualValuesOfIdleTimes, 
			boolean usedFarkas)
	{
		this.globalIterationNumber = globalIterationNumber;  
		this.tripsToGenerateVariables = tripsToGenerateVariables; 
		this.vehicleGraphs = vehicleGraph; 
		this.dualValuesOfTripIDs = dualValuesOfTripIDs; 
		this.dualValuesOfDeadrunsLowerLimit = dualValuesOfDeadrunsLowerLimit; 
		this.dualValuesOfDeadrunsUpperLimit = dualValuesOfDeadrunsUpperLimit; 
		this.dualValuesOfIdleTimes = dualValuesOfIdleTimes; 
		this.usedFarkas = usedFarkas; 
		
		chooseSubproblem(); 
	}
	
	private void chooseSubproblem()
	{
		boolean allSubproblemsVisited = false; 
		List<VehicleTypeDepot> subproblems = this.vehicleGraphs.keySet().stream().collect(Collectors.toList()); 
		
		int numberOfSubProblemsVisited = 0; 
		
		while(!allSubproblemsVisited)
		{
			VehicleTypeDepot chosenSubproblem = subproblems.get((this.globalIterationNumber % subproblems.size()));
			System.out.println("Chosen subproblem " + chosenSubproblem.getDescription());
			solveSubProblem(chosenSubproblem); 
			if(!this.blocksGenerated.isEmpty())
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
	
	private void solveSubProblem(VehicleTypeDepot chosenSubproblem)
	{
		List<VehicleVertex> tripVertices = this.vehicleGraphs.get(chosenSubproblem).vertexSet().stream().filter(v -> v.getVertexId() != -1 && v.getVertexId() != Integer.MAX_VALUE).collect(Collectors.toList()); 
		for(VehicleVertex tripVertex : tripVertices)
		{
			if(this.dualValuesOfTripIDs.containsKey(tripVertex.getTrip()))
			{
				tripVertex.calculateReducedCostOfVertex(this.dualValuesOfTripIDs.get(tripVertex.getTrip()), this.usedFarkas);
			}
			
		}
		
		VehicleVertex sinkVertex = this.vehicleGraphs.get(chosenSubproblem).vertexSet().stream().filter(v -> v.getVertexId() == Integer.MAX_VALUE).findFirst().get(); 
		sinkVertex.calculateReducedCostOfSinkVertex(this.usedFarkas);
		
		for(VehicleArc vehicleArc : this.vehicleGraphs.get(chosenSubproblem).edgeSet())
		{
			vehicleArc.calculateReducedCostOfArc(this.dualValuesOfDeadrunsLowerLimit, this.dualValuesOfDeadrunsUpperLimit, this.dualValuesOfIdleTimes, this.usedFarkas);
		}
		
		for(VehicleVertex vertex : this.vehicleGraphs.get(chosenSubproblem).vertexSet())
		{
			vertex.getLabels().clear();
		}
		
		VehicleRCSPP rcspp = new VehicleRCSPP(chosenSubproblem.getVehicleType(), this.tripsToGenerateVariables, this.vehicleGraphs.get(chosenSubproblem), dualValuesOfTripIDs); 
		this.blocksGenerated = rcspp.getBlocksGenerated(); 
		System.out.println("Number of blocks generated " + this.blocksGenerated.size());
	}

}
