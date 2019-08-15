package ALNS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class LocalSearch 
{
	private List<Trip> allTrips;  
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers;
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	@Getter
	private List<Block> bestBlockSolution; 
	@Getter
	private List<Duty> bestDutySolution;
	@Getter
	private double bestObjective; 
	
	private int score1; 
	private int score2; 
	private double lambda; 
	
	private double[] destroyWeights; 
	private double[] destroyProbabilities; 
	private int[] destroyScoreInSegment; 
	private int[] destoryChosenInSegement; 
	
	private int maxIterations;
	private int segmentSize;
	
	private Map<Integer, List<Double>> weightAtEachIteration; 
	private Map<Integer, List<Integer>> scoreAtEachIteration; 
	private Map<Integer, List<Double>> objectiveAtEachIteration;
	private boolean initalSolutionLocalSearch; 
	public LocalSearch(List<Trip> allTrips, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> bestBlockSolution, List<Duty> bestDutySolution, double bestObjective, boolean initalSolutionLocalSearch, int maxiterations) throws IloException
	{
		this.allTrips = allTrips;  
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.bestBlockSolution = new ArrayList<Block>(bestBlockSolution); 
		this.bestDutySolution = new ArrayList<Duty>(bestDutySolution); 
		this.bestObjective = bestObjective; 
		
		this.weightAtEachIteration = new HashMap<Integer, List<Double>>(); 
		this.scoreAtEachIteration = new HashMap<Integer, List<Integer>>(); 
		this.objectiveAtEachIteration = new HashMap<Integer, List<Double>>(); 
		this.initalSolutionLocalSearch = initalSolutionLocalSearch; 
		
		initializeParameters(maxiterations); 
		
		algorithm(); 
		
		System.out.println("Final Solution...");
		for(Block block : this.bestBlockSolution)
		{
		
			System.out.println();
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
		}
		
		for(Duty duty : this.bestDutySolution)
		{
			System.out.println();
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			}
		}
		
		displayKPI(); 
	}
	
	private void initializeParameters(int maxIterations)
	{
		this.score1 = 25;
		this.score2 = 0; 
		this.lambda = 0.1; 
		
		this.maxIterations = maxIterations; 
		this.segmentSize = 25; 
		
		int numberOfDestroyMethods = 2; 
		double initialProbabilities = 1.0/(double)(numberOfDestroyMethods); 
		this.destroyWeights = new double[numberOfDestroyMethods]; 
		this.destroyProbabilities = new double[numberOfDestroyMethods]; 
		this.destroyScoreInSegment = new int[numberOfDestroyMethods]; 
		this.destoryChosenInSegement = new int[numberOfDestroyMethods]; 
		
		Arrays.fill(destroyWeights, 1.0);
		Arrays.fill(this.destroyProbabilities, initialProbabilities);
		Arrays.fill(this.destoryChosenInSegement, 0);
		Arrays.fill(this.destroyScoreInSegment, 0);
	}
	
	private void algorithm() throws IloException
	{
		 
		for(int i = 0; i < maxIterations; i++)
		{
			int selectedDestroyMethod = selectDestroyMethod(); 
			int scoreInIteration = 0; 
			System.out.println("*****************************************" + " Local search iteration number = " + i + " *****************************************");
			System.out.println("Selected destroy method = " + selectedDestroyMethod);
			
			DestroyMethod destroy = new DestroyMethod(i, selectedDestroyMethod, this.allTrips, this.vehicleGraphs, this.driverGraphs, this.deadrunMultipliers, this.idleTimeMultipliers, this.bestBlockSolution, this.bestDutySolution, this.initalSolutionLocalSearch); 
			List<Block> intermediateBlockSolution = destroy.getBlocksInSolution(); 
			List<Duty> intermediateDutySolution = destroy.getDutiesInSolution();
			List<Block> blocksRemoved = destroy.getBlocksToBeRemoved(); 
			List<Duty> dutiesRemoved = destroy.getDutiesToBeRemoved(); 
			Set<Deadrun> deadrunsInSolution = destroy.getDeadrunsInSolution(); 
			Set<IdleTime> idleTimesInSolution = destroy.getIdleTimesInSolution(); 
			List<Trip> uncoveredTripsOfVehicle = destroy.getUncoveredTripsOfVehicle();
			List<Trip> uncoveredTripsOfDriver = destroy.getUncoveredTripsOfDriver(); 
			
			RepairMethod repair = new RepairMethod(selectedDestroyMethod, this.allTrips, this.vehicleGraphs, this.driverGraphs, this.deadrunMultipliers, this.idleTimeMultipliers, intermediateBlockSolution, intermediateDutySolution, blocksRemoved, dutiesRemoved, deadrunsInSolution, idleTimesInSolution, uncoveredTripsOfVehicle, uncoveredTripsOfDriver, this.initalSolutionLocalSearch); 
			
			if(this.bestObjective - repair.getObjective() > 1e-3)
			{
				this.bestBlockSolution = repair.getBlocksInSoution(); 
				this.bestDutySolution = repair.getDutiesInSolution(); 
				this.bestObjective = repair.getObjective();
				System.out.println("Best objective = " + this.bestObjective);
				scoreInIteration = this.score1;
			}
			else
			{
				scoreInIteration = this.score2; 
			}
			
			this.destroyScoreInSegment[selectedDestroyMethod] = this.destroyScoreInSegment[selectedDestroyMethod] + scoreInIteration; 
			this.destoryChosenInSegement[selectedDestroyMethod] = this.destoryChosenInSegement[selectedDestroyMethod] + 1; 
			
			if(i != 0 && i%this.segmentSize == 0)
			{
				updateProbabilities(); 
				
				resetScores(); 
			}
			
			kpi(i); 
		}
	}
	
	private void kpi(int iterationNumber)
	{
		List<Double> weights = new ArrayList<Double>(); 
		for(int j = 0; j < this.destroyWeights.length; j++)
		{
			weights.add(this.destroyWeights[j]); 
		}
		this.weightAtEachIteration.put(iterationNumber, weights); 
		
		List<Integer> scores = new ArrayList<Integer>(); 
		for(int j = 0; j < this.destroyScoreInSegment.length; j++)
		{
			scores.add(this.destroyScoreInSegment[j]); 
		}
		this.scoreAtEachIteration.put(iterationNumber, scores);
		
		List<Double> objective = new ArrayList<Double>(); 
		objective.add(this.bestObjective); 
		objective.add((double)this.bestBlockSolution.size()); 
		objective.add((double)this.bestDutySolution.size()); 
		this.objectiveAtEachIteration.put(iterationNumber, objective); 
	}
	
	private void updateProbabilities()
	{
		double sumOfDestroyWeights = 0.0; 
		for(int i = 0; i < this.destroyWeights.length; i++)
		{
			if(this.destoryChosenInSegement[i] != 0)
			{
				this.destroyWeights[i] = (this.lambda*(this.destroyScoreInSegment[i]/this.destoryChosenInSegement[i])) + ((1-this.lambda)*this.destroyWeights[i]);
				System.out.println("Weight of destroy method " + i + " = " + this.destroyWeights[i]);
			}
			sumOfDestroyWeights = sumOfDestroyWeights + this.destroyWeights[i];  
		}
		
		for(int i = 0; i < this.destroyProbabilities.length; i++)
		{
			this.destroyProbabilities[i] = this.destroyWeights[i]/sumOfDestroyWeights; 
		}
	}
	
	private void resetScores()
	{
		Arrays.fill(this.destoryChosenInSegement, 0);
		Arrays.fill(this.destroyScoreInSegment, 0);
	}
	
	private int selectDestroyMethod()
	{
		int selectedDestroyMethod = 0; 
		double sumOfDestroyProbabilites = 0.0; 
		double rDestroy = Math.random();  
		for(int i = 0; i < this.destroyProbabilities.length; i++)
		{
			sumOfDestroyProbabilites = sumOfDestroyProbabilites + this.destroyProbabilities[i]; 
			if(rDestroy <= sumOfDestroyProbabilites)
			{
				selectedDestroyMethod = i;
				break; 
			}
		}
		return selectedDestroyMethod; 
	}
	
	private void displayKPI()
	{
		System.out.println("Weights...");
		for(Integer i : this.weightAtEachIteration.keySet())
		{
			this.weightAtEachIteration.get(i).forEach(w -> {
				System.out.print(w + "; ");
			});
			System.out.println();
		}
		
		System.out.println("Scores...");
		for(Integer i : this.scoreAtEachIteration.keySet())
		{
			this.scoreAtEachIteration.get(i).forEach(w -> {
				System.out.print(w + "; ");
			});
			System.out.println();
		}
		
		System.out.println("Objectives...");
		for(Integer i : this.objectiveAtEachIteration.keySet())
		{
			this.objectiveAtEachIteration.get(i).forEach(w -> {
				System.out.print(w + "; ");
			});
			System.out.println();
		}
	}

}
