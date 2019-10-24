package ALNS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
	private double degreeOfDutyDestruction; 
	private double degreeOfSequentialDestruction; 
	private double degreeOfIntegratedDestruction; 
	private int integratedIterationLimit; 
	private int integratedTimeLimit; 
	private int localSearchTimeLimit; 
	@Getter
	private List<Block> bestBlockSolution; 
	@Getter
	private List<Duty> bestDutySolution;
	@Getter
	private double bestObjective; 
	private int numberOfRecharges; 
	private int noImprovement; 
	
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
	
	private int[] totalNumberOfTimesMethodChosen; 
	private double[] totalTimeTakenOfRepairMethods; 
	private double startTimeOfAlgorithm; 
	public LocalSearch(double startTimeOfAlgorithm, List<Trip> allTrips, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> bestBlockSolution, List<Duty> bestDutySolution, double bestObjective, boolean initalSolutionLocalSearch, int maxiterations, 
			double degreeOfDutyDestruction, double degreeOfSequentialDestruction, double degreeOfIntegratedDestruction, int integratedIterationLimit, int integratedTimeLimit, int localSearchTimeLimit) throws IloException
	{
		this.allTrips = allTrips;  
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.degreeOfDutyDestruction = degreeOfDutyDestruction; 
		this.degreeOfSequentialDestruction = degreeOfSequentialDestruction; 
		this.degreeOfIntegratedDestruction = degreeOfIntegratedDestruction; 
		this.integratedIterationLimit = integratedIterationLimit; 
		this.integratedTimeLimit = integratedTimeLimit; 
		this.localSearchTimeLimit = localSearchTimeLimit; 
		this.bestBlockSolution = new ArrayList<Block>(bestBlockSolution); 
		this.bestDutySolution = new ArrayList<Duty>(bestDutySolution); 
		this.bestObjective = bestObjective; 
		this.startTimeOfAlgorithm = startTimeOfAlgorithm; 
		this.numberOfRecharges = 0; 
		
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
			boolean count = false; 
			if(block.getDistance() > block.getVehicleType().getMaximumDistanceWithoutRecharging())
			{
				count = true; 
			}
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
				if(ba.getActivity().equals("Recharging") && count)
				{
					this.numberOfRecharges++; 
				}
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
		this.noImprovement = 0; 
		
		this.maxIterations = maxIterations; 
		this.segmentSize = 25; 
		
		int numberOfDestroyMethods = 2;
		int numberOfRepairMethods = 2; 
		if(!this.initalSolutionLocalSearch)
		{
			numberOfDestroyMethods = 3;
			numberOfRepairMethods = 3;
		}
		
		double initialProbabilities = 1.0/(double)(numberOfDestroyMethods); 
		this.destroyWeights = new double[numberOfDestroyMethods]; 
		this.destroyProbabilities = new double[numberOfDestroyMethods]; 
		this.destroyScoreInSegment = new int[numberOfDestroyMethods]; 
		this.destoryChosenInSegement = new int[numberOfDestroyMethods]; 
		
		this.totalNumberOfTimesMethodChosen = new int[numberOfRepairMethods]; 
		this.totalTimeTakenOfRepairMethods = new double[numberOfRepairMethods]; 
		
		Arrays.fill(destroyWeights, 1.0);
		Arrays.fill(this.destroyProbabilities, initialProbabilities);
		Arrays.fill(this.destoryChosenInSegement, 0);
		Arrays.fill(this.destroyScoreInSegment, 0);
		
		Arrays.fill(this.totalNumberOfTimesMethodChosen, 0);
		Arrays.fill(this.totalTimeTakenOfRepairMethods, 0.0);
	}
	
	private void algorithm() throws IloException
	{
		int status = 0; 
		int i = 1; 
		double startTime = System.currentTimeMillis(); 
		//for(int i = 0; i < maxIterations; i++)
		while(status != 1)
		{
			this.maxIterations = i;
			int selectedDestroyMethod = selectDestroyMethod(i); 
			int scoreInIteration = 0; 
			System.out.println("*****************************************" + " Local search iteration number = " + i + " *****************************************");
			System.out.println("Selected destroy method = " + selectedDestroyMethod);
			
			/*double dutyDestruction = this.degreeOfDutyDestruction; 
			double sequentialDestruction = this.degreeOfSequentialDestruction; 
			double integratedDestruction = this.degreeOfIntegratedDestruction; 
			if(this.allTrips.size() > 500)
			{
				if(i < 100)
				{
					dutyDestruction = Math.min(0.1, this.degreeOfDutyDestruction); 
					sequentialDestruction = Math.min(0.1,this.degreeOfSequentialDestruction); 
					integratedDestruction = Math.min(0.05, this.degreeOfIntegratedDestruction); 
				}
				else if( i < 200)
				{
					dutyDestruction = Math.min(0.2, this.degreeOfDutyDestruction); 
					sequentialDestruction = Math.min(0.15,this.degreeOfSequentialDestruction); 
					integratedDestruction = Math.min(0.075, this.degreeOfIntegratedDestruction); 
				}
				else
				{
					dutyDestruction = Math.min(0.3, this.degreeOfDutyDestruction); 
					sequentialDestruction = Math.min(0.2,this.degreeOfSequentialDestruction); 
					integratedDestruction = Math.min(0.1, this.degreeOfIntegratedDestruction);
				}
			}*/
			
			DestroyMethod destroy = new DestroyMethod(i, selectedDestroyMethod, this.allTrips, this.vehicleGraphs, this.driverGraphs, this.deadrunMultipliers, this.idleTimeMultipliers, this.bestBlockSolution, this.bestDutySolution, this.initalSolutionLocalSearch, this.degreeOfDutyDestruction, this.degreeOfSequentialDestruction, this.degreeOfIntegratedDestruction, this.noImprovement); 
			List<Block> intermediateBlockSolution = destroy.getBlocksInSolution(); 
			List<Duty> intermediateDutySolution = destroy.getDutiesInSolution();
			List<Block> blocksRemoved = destroy.getBlocksToBeRemoved(); 
			List<Duty> dutiesRemoved = destroy.getDutiesToBeRemoved(); 
			Set<Deadrun> deadrunsInSolution = destroy.getDeadrunsInSolution(); 
			Set<IdleTime> idleTimesInSolution = destroy.getIdleTimesInSolution(); 
			List<Trip> uncoveredTripsOfVehicle = destroy.getUncoveredTripsOfVehicle();
			List<Trip> uncoveredTripsOfDriver = destroy.getUncoveredTripsOfDriver(); 
			
			double start = System.currentTimeMillis(); 
			RepairMethod repair = new RepairMethod(selectedDestroyMethod, this.allTrips, this.vehicleGraphs, this.driverGraphs, this.deadrunMultipliers, this.idleTimeMultipliers, intermediateBlockSolution, intermediateDutySolution, blocksRemoved, dutiesRemoved, deadrunsInSolution, idleTimesInSolution, uncoveredTripsOfVehicle, uncoveredTripsOfDriver, this.initalSolutionLocalSearch, this.integratedIterationLimit, this.integratedTimeLimit); 
			double end = System.currentTimeMillis(); 
			this.totalNumberOfTimesMethodChosen[selectedDestroyMethod] = this.totalNumberOfTimesMethodChosen[selectedDestroyMethod] + 1; 
			this.totalTimeTakenOfRepairMethods[selectedDestroyMethod] = this.totalTimeTakenOfRepairMethods[selectedDestroyMethod] + (end-start)/(double)(1000); 
			
			if(this.bestObjective - repair.getObjective() > 1e-3)
			{
				this.bestBlockSolution = repair.getBlocksInSoution(); 
				this.bestDutySolution = repair.getDutiesInSolution(); 
				this.bestObjective = repair.getObjective();
				System.out.println("Best objective = " + this.bestObjective);
				scoreInIteration = this.score1;
				this.noImprovement = 0; 
			}
			else
			{
				scoreInIteration = this.score2; 
				this.noImprovement++; 
			}
			
			this.destroyScoreInSegment[selectedDestroyMethod] = this.destroyScoreInSegment[selectedDestroyMethod] + scoreInIteration; 
			this.destoryChosenInSegement[selectedDestroyMethod] = this.destoryChosenInSegement[selectedDestroyMethod] + 1; 
			 
			
			if(i != 0 && i%this.segmentSize == 0)
			{
				updateProbabilities(); 
				
				resetScores();
				
				boolean stop = true;
				if(!this.initalSolutionLocalSearch)
				{
					for(int j = 0; j < this.destroyWeights.length; j++)
					{
						if(this.destroyWeights[j] > 0.01)
						{
							stop = false; 
							break; 
						}
					}
				}
				
				if(stop)
				{
					status = 1; 
				}	
			}
			
			kpi(i); 
			
			if(this.initalSolutionLocalSearch && i >= 25)
			{
				status = 1; 
			}
			
			double endTime = System.currentTimeMillis(); 
			double totalTime = (endTime - this.startTimeOfAlgorithm)/1000.00; 
			System.out.println("Running time = " + totalTime);
			if(totalTime >= this.localSearchTimeLimit)
			{
				status = 1; 
			}
			
			i++; 
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
	
	private int selectDestroyMethod(int iter)
	{
		int selectedDestroyMethod = 0; 
		double sumOfDestroyProbabilites = 0.0; 
		Random rnd = new Random(/*iter*/); 
		double rDestroy = (double)rnd.nextInt(100)/(double)100;
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
		System.out.println("Number of recharges = " + this.numberOfRecharges);
		
		System.out.println("Time taken by repair methods...");
		System.out.println("Total number of iterations performed = " + this.maxIterations);
		System.out.println("Number of iterations; Number of times selected; Total time; Average time");
		for(int i = 0 ; i < this.totalNumberOfTimesMethodChosen.length; i++)
		{
			System.out.println(this.totalNumberOfTimesMethodChosen[i] + "; " + this.totalTimeTakenOfRepairMethods[i] + "; " + (this.totalTimeTakenOfRepairMethods[i]/(double)this.totalNumberOfTimesMethodChosen[i]));
		}
	}

}
