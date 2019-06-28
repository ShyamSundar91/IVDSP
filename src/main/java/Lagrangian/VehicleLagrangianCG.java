//package Lagrangian;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.jgrapht.graph.DefaultDirectedGraph;
//import org.junit.Assert;
//
//import Data.Trip;
//import Networks.VehicleArc;
//import Networks.VehicleTypeDepot;
//import Networks.VehicleVertex;
//import Subproblems.VehicleSubproblem;
//import Variables.Block;
//import Variables.BlockActivity;
//import ilog.concert.IloException;
//
//public class VehicleLagrangianCG 
//{
//	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs;
//	
//	private List<Trip> trips; 
//	private List<Block> blocks; 
//	private double lowerBound; 
//	private double upperBound; 
//	private Set<Block> finalSolution;  
//	private List<List<Double>> kpi; 
//	private Map<Trip, Double> tripDuals; 
//	private List<Block> currentBlocks; 
//	public VehicleLagrangianCG(List<Trip> trips, Set<Block> blocks, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs) throws IloException
//	{
//		this.vehicleGraphs = vehicleGraphs; 
//		this.trips = trips;  
//		this.blocks = new ArrayList<>(blocks); 
//		this.lowerBound = -Double.MAX_VALUE; 
//		this.upperBound = (this.trips.size() * 100000); 
//		this.tripDuals = new HashMap<Trip, Double>(); 
//		this.currentBlocks = new ArrayList<Block>(); 
//		for(Trip trip : this.trips)
//		{
//			this.tripDuals.put(trip, 0.0); 
//		}
//
//		this.kpi = new ArrayList<List<Double>>(); 
//		
//		initialColumns();
//		
//		algorithm(); 
//		
//		System.out.println("Final lower bound = " + this.lowerBound);
//		
//		System.out.println("Solution..");
//		for(Block block : this.finalSolution)
//		{
//			System.out.println();
//			for(BlockActivity be : block.getBlockActivities())
//			{
//				System.out.println(block.getBlockId() + "; " + be.getDepartureNode().getNodeId() + "; " + be.getArrivalNode().getNodeId() + "; " + be.getDepartureNode().getNodeId() + "; " + be.getArrivalNode().getNodeId() + "; " +  be.getDepartureTime() + "; " + be.getArrivalTime() + "; " + be.getActivity() + "; " + be.getDistance());
//			} 
//			
//		}
//		
//		for(List<Double> iter : this.kpi)
//		{
//			System.out.println(iter.get(0) + "; " + iter.get(1) + "; " + iter.get(2) + "; " + iter.get(3));
//		}
//		
//	}
//	
//	private void initialColumns()
//	{
//		System.out.println("Initial columns");
//		Map<Trip, Double> initalMultipliers = new HashMap<Trip, Double>(); 
//		for(Trip trip : this.trips)
//		{
//			initalMultipliers.put(trip, 10000.00); 
//		}
//		
//		VehicleSubproblem sub = new VehicleSubproblem(0, this.vehicleGraphs, initalMultipliers);
//		List<Block> blocksGenerated = new ArrayList<Block>(sub.getBlocksGenerated()); 
//		if(!blocksGenerated.isEmpty())
//		{
//			this.blocks.addAll(blocksGenerated);
//			this.currentBlocks.clear();
//			this.currentBlocks.addAll(blocksGenerated); 
//		}
//	}
//
//	
//	private void algorithm() throws IloException
//	{
//		boolean stop = false; 
//		int colIter = 0; 
//		while(!stop)
//		{
//			System.out.println("VSP Column generation itertion number " + colIter);
//			System.out.println("VSP Number of blocks in master problem " + this.blocks.size());
//			long start = System.currentTimeMillis(); 
//			LagrangianSolver solver = new LagrangianSolver(this.tripDuals, this.blocks, this.trips, this.upperBound); 
//			this.lowerBound = solver.getLowerBound();
//			if(solver.getUpperBound() < this.upperBound)
//			{
//				this.upperBound = solver.getUpperBound(); 
//				this.finalSolution = solver.getFinalSolution(); 
//			}
//			
//			long end = System.currentTimeMillis();
//			System.out.println("Total time = " + (double)(end - start)/(double)1000);
//
//			System.out.println("Subgradient Lower bound " + this.lowerBound);
//			
//			estimateLowerBound(this.currentBlocks, this.blocks, solver.getReducedCostOfBlocks(), this.lowerBound); 
//			Map<Trip, Double> tempMultipliers = new HashMap<Trip, Double>(); 
//			double [] original = solver.getOriginalMultipliers(); 
//			for(Trip t : this.trips)
//			{
//				tempMultipliers.put(t, original[t.getTripId()]); 
//			}
//
//			double [] adapted = solver.getItemMultipliers(); 
//			for(Trip t : this.trips)
//			{
//				this.tripDuals.put(t, adapted[t.getTripId()]); 
//			}
//			
//			
//			List<Double> iter = new ArrayList<Double>(); 
//			iter.add((double) colIter); 
//			iter.add((double) this.blocks.size()); 
//			iter.add(this.lowerBound); 
//			iter.add(this.upperBound); 
//			this.kpi.add(iter); 
//			
//			VehicleSubproblem sub = new VehicleSubproblem(colIter, this.vehicleGraphs, this.tripDuals);
//			Set<Block> blocksGenerated = new HashSet<Block>(sub.getBlocksGenerated()); 
//			if(!blocksGenerated.isEmpty())
//			{
//				this.blocks.addAll(blocksGenerated); 
//				this.currentBlocks.clear();
//				this.currentBlocks.addAll(blocksGenerated); 
//			}
//			else
//			{
//				stop = true; 
//			}
//			
//			this.tripDuals.clear();
//			this.tripDuals.putAll(tempMultipliers);
//			
//			colIter++; 
//		}
//		
//	}
//	
//	private void estimateLowerBound(List<Block> currentBlocks, List<Block> allBlocks, double[] reducedCost, double lowerBound)
//	{
//		double estimatedLowerBound = lowerBound; 
//		/*System.out.println("Block added...");
//		for(Block block : currentBlocks)
//		{
//			System.out.println(block.getBlockId());
//		}*/
//		
//		//System.out.println("Block estimated...");
//		for(int i = 0; i < allBlocks.size(); i++)
//		{
//			if(currentBlocks.contains(allBlocks.get(i)))
//			{
//				estimatedLowerBound  = estimatedLowerBound  + reducedCost[i]; 
//				//System.out.println(allBlocks.get(i).getBlockId());
//			}
//		}
//		
//		System.out.println("Estimated lower bound = " + estimatedLowerBound);
//	}
//}
