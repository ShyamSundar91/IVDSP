package ALNS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Duty;
import ilog.concert.IloException;
import lombok.Getter;

public class LocalSearch 
{
	private List<Trip> allTrips; 
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
	
	public LocalSearch(List<Trip> allTrips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, List<Block> bestBlockSolution, List<Duty> bestDutySolution, double bestObjective) throws IloException
	{
		this.allTrips = allTrips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.bestBlockSolution = new ArrayList<Block>(bestBlockSolution); 
		this.bestDutySolution = new ArrayList<Duty>(bestDutySolution); 
		this.bestObjective = bestObjective; 
		
		initializeParameters(); 
		
		algorithm(); 
	}
	
	private void initializeParameters()
	{
		this.score1 = 10;
		this.score2 = 0; 
		this.lambda = 0.1; 
		
		this.maxIterations = 500; 
		this.segmentSize = 25; 
		
		int numberOfDestroyMethods = 2; 
		this.destroyWeights = new double[numberOfDestroyMethods]; 
		this.destroyProbabilities = new double[numberOfDestroyMethods]; 
		this.destroyScoreInSegment = new int[numberOfDestroyMethods]; 
		this.destoryChosenInSegement = new int[numberOfDestroyMethods]; 
		
		Arrays.fill(destroyWeights, 1.0);
		Arrays.fill(this.destroyProbabilities, 0.5);
		Arrays.fill(this.destoryChosenInSegement, 0);
		Arrays.fill(this.destroyScoreInSegment, 0);
	}
	
	private void algorithm() throws IloException
	{
		 
		for(int i = 0; i < maxIterations; i++)
		{
			int selectedDestroyMethod = selectDestroyMethod(); 
			int scoreInIteration = 0; 
			System.out.println("Selected destroy method = " + selectedDestroyMethod);
			
			DestroyMethod destroy = new DestroyMethod(i, selectedDestroyMethod, this.bestBlockSolution, this.bestDutySolution); 
			List<Block> intermediateBlockSolution = destroy.getBlocksInSolution(); 
			List<Duty> intermediateDutySolution = destroy.getDutiesInSolution(); 
			List<Trip> uncoveredTripsOfVehicle = destroy.getUncoveredTripsOfVehicle();
			List<Trip> uncoveredTripsOfDriver = destroy.getUncoveredTripsOfDriver(); 
			
			RepairMethod repair = new RepairMethod(this.allTrips, this.vehicleGraphs, this.driverGraphs, intermediateBlockSolution, intermediateDutySolution, uncoveredTripsOfVehicle, uncoveredTripsOfDriver); 
			if(repair.getObjective() < this.bestObjective)
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
		}
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

}
