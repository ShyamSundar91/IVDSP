package Lagrangian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import Data.Trip;
import Variables.Block;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import lombok.Getter;

@Getter
public class LagrangianSolver 
{
	private double[] itemMultipliers; 
	private double[] subgradientVector;
	private double[] reducedCostOfBlocks; 
	private double[] costOfBlocks; 
	private double[][] tripBlockMatrix; 
	
	private List<Block> blocks; 
	private List<Trip> trips; 
	private int blockSize; 
	private int tripSize; 
	
	private double lagrangianLowerBound; 
	private double upperBound; 
	private double lowerBound; 
	private double stepSize; 
	private double decayRate; 
	private int lastImprovedSolution;
	private Set<Block> lagrangianSolution; 
	private Set<Block> finalSolution;
	
	private double[] originalMultipliers; 
	
	public LagrangianSolver(Map<Trip, Double> tripDual, List<Block> blocks, List<Trip> trips, double upperBound)
	{
		this.blocks = blocks; 
		this.trips = trips; 
		this.blockSize = blocks.size(); 
		this.tripSize = trips.size(); 
		Assert.assertTrue(this.trips.size() == tripDual.size());
		
		initialize(tripDual, upperBound); 
		
		subgradientAlgorithm(); 
	}
	
	private void initialize(Map<Trip, Double> tripDual, double ub)
	{
		this.itemMultipliers = new double[this.tripSize]; 
		this.subgradientVector = new double[this.tripSize]; 
		this.reducedCostOfBlocks = new double[this.blockSize]; 
		this.costOfBlocks = new double[this.blockSize]; 
		this.tripBlockMatrix = new double[this.blockSize][this.tripSize]; 
		
		//System.out.println("Before...");
		for(Trip trip : tripDual.keySet())
		{
			this.itemMultipliers[trip.getTripId()] = tripDual.get(trip); 
			//System.out.println("Trip " + trip.getTripId() + " = " + this.itemMultipliers[trip.getTripId()]);
			this.subgradientVector[trip.getTripId()] = 0.0; 
		}
		
		for(int b = 0; b < this.blockSize; b++)
		{
			this.reducedCostOfBlocks[b] = 0.0; 
			this.costOfBlocks[b] = this.blocks.get(b).getLhs(); 
			Arrays.fill(this.tripBlockMatrix[b], 0.0);
			for(int t : this.blocks.get(b).tripIds)
			{
				this.tripBlockMatrix[b][t] = 1.0; 
			}
		}
		
		this.lowerBound = -Double.MAX_VALUE; 
		this.upperBound = ub; 
		this.decayRate = 2; 
		this.finalSolution = new HashSet<Block>(); 
		this.lagrangianSolution = new HashSet<Block>(); 
	}
	
	private void subgradientAlgorithm()
	{
		List<Double> lowerBounds = new ArrayList<Double>(); 
		
		this.lowerBound = -Double.MAX_VALUE;
		this.lastImprovedSolution = 0; 
		this.decayRate = 2; 
		boolean stop = false; 
		int iter = 0; 
		
		while(!stop)
		{
			solve(iter, this.blockSize, this.tripSize, this.subgradientVector, this.itemMultipliers, this.costOfBlocks, 
					this.reducedCostOfBlocks, this.blocks/*, this.lowerBound, this.upperBound, this.lastImprovedSolution, this.lagrangianSolution, this.finalSolution, this.lagrangianLowerBound*/); 
			lowerBounds.add(this.lowerBound); 
			
			stop = checkTermination(iter); 
			
			if(!stop)
			{
				updateMultipliers(iter); 
			}
			
		
			
			
			iter++; 
		}
		
		adaptMultipliers(); 
		
		System.out.println("Number of subgradient iterations " + iter); 
	}
	
	private void solve(int iterationNumber, int blockSize, int tripSize, double[] subgradientVector, double[] itemMultipliers, double[] costOfBlocks, 
			double[] reducedCostOfBlocks, List<Block> blocks/*, double lowerBound, double upperBound, int lastImprovedSolution, Set<Block> lagrangianSolution, Set<Block> finalSolution, double lagrangianLowerBound*/)
	{ 
		lagrangianSolution.clear();
		Arrays.fill(subgradientVector, 1.0);
		double lb = 0.0;
		double ub = 0.0; 

		for(int b = 0; b < blockSize; b++)
		{
			double rhs = 0.0; 
			List<Integer> tripsIds = blocks.get(b).tripIds; 
			double cost = costOfBlocks[b]; 
			for(int t : tripsIds)
			{
				rhs = rhs + /*this.tripBlockMatrix[b][t]**/itemMultipliers[t];  
			}
			
			double redCost = cost - rhs; 
				
			reducedCostOfBlocks[b] = redCost;  
				
			if(redCost < 0.0)
			{
				lb = lb + redCost ; 
				ub = ub + cost ; 
				lagrangianSolution.add(blocks.get(b)); 
				
				for(int t : tripsIds)
				{
					//if(this.tripBlockMatrix[b][t] == 1.0)
					{
						subgradientVector[t] = subgradientVector[t] - 1.0; 
					}
				}
			}
		}	
			
		double x = Arrays.stream(itemMultipliers).sum(); 
	
		lb = lb + x; 
		
		for(int t = 0; t < tripSize; t++)
		{
			if(subgradientVector[t] == 1.0)
			{
				ub = ub + 100000; 
			}
		}
		
		
		if(ub < upperBound)
		{
			upperBound = ub;
			finalSolution.clear();
			finalSolution.addAll(lagrangianSolution); 		
		}
		
		//System.out.println(lb);
		lagrangianLowerBound = lb; 
		if(lb > lowerBound)
		{	
			lowerBound = lb; 
			lastImprovedSolution = iterationNumber; 
		}
	}
	
	private void updateMultipliers(int curretnIter)
	{
				
		double denominator = 0.0;
		
		for(int t = 0; t < this.tripSize; t++)
		{
			denominator = denominator + Math.pow(this.subgradientVector[t], 2); 
		}
		
		
		this.stepSize = (this.upperBound - this.lagrangianLowerBound)/denominator; 
		//System.out.println(this.stepSize);
		for(int t = 0; t < this.tripSize; t++)
		{
			double multiplier =   Math.max(0.0, this.itemMultipliers[t] + (this.decayRate *this.subgradientVector[t])); 
			this.itemMultipliers[t] = multiplier;  
			//System.out.println(multiplier);
		}
	}
	
	private boolean checkTermination(int currentIter)
	{
		boolean stop = false; 
		if(Math.abs(this.upperBound - this.lowerBound)/Math.abs(this.lowerBound) < 0.00001)
		{
			stop = true; 
		}
		
		if(currentIter - this.lastImprovedSolution > 200)
		{
			this.decayRate = this.decayRate/2;
			if(this.decayRate < 1e-3)
			{
				stop = true; 
			}
	
			this.lastImprovedSolution = currentIter; 
		}
		
		if(currentIter > 1000)
		{
			stop = true; 
		}
		
		return stop; 
	}
	
	private void adaptMultipliers()
	{
		this.originalMultipliers = new double[this.tripSize]; 
		//System.out.println("After...");
		//this.originalMultipliers = Arrays.copyOf(this.itemMultipliers, this.tripSize);
		for(int t = 0; t < this.tripSize; t++)
		{
			this.originalMultipliers[t] = this.itemMultipliers[t]; 
			//System.out.println("Trip " + t  + " = " + this.originalMultipliers[t]);
		}
		
		for(int b = 0; b < this.blockSize; b++)
		{
			if(this.reducedCostOfBlocks[b] < 0.0)
			{
				double delta = this.reducedCostOfBlocks[b]/(double)this.blocks.get(b).tripsInBlock.size(); 
				
				for(int t= 0; t < this.tripSize; t++)
				{
					if(this.tripBlockMatrix[b][t] == 1.0)
					{
						this.itemMultipliers[t] = this.itemMultipliers[t] + delta; 
					}
				}
			}
		}
		 
		for(int b = 0; b < this.blockSize; b++)
		{
			double rhs = 0.0; 
			for(int t= 0; t < this.tripSize; t++)
			{
				if(this.tripBlockMatrix[b][t] == 1.0)
				{
					rhs = rhs + this.itemMultipliers[t];
				}
				 
			}
			
			double redCost = this.costOfBlocks[b] - rhs; 
			
			//this.reducedCostOfBlocks[b] = redCost; 
			if(!Double.isNaN(redCost))
			{
				Assert.assertTrue("The value is " + redCost, redCost > -1e-3); 
			}
			
		}
	}
}
