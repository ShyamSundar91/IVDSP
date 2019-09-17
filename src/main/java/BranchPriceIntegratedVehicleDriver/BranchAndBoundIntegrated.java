package BranchPriceIntegratedVehicleDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import BranchPriceDriver.BBNodeDriver;
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

public class BranchAndBoundIntegrated 
{
	private Map<Block, Integer> initialBlocksAndGenerated; 
	private Map<Duty, Integer> initialDutiesAndGenerated; 
	private List<Trip> trips; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	
	private List<BBNodeIntegrated> nodes; 
	private Map<Integer, Double> lpObjectivesAtEachNode; 
	private boolean earlyTermination;
	private int iterationLimit; 
	
	@Getter
	private List<Block> blocksInSolution; 
	@Getter
	private List<Duty> dutiesInSolution; 
	@Getter
	private double objective;
	@Getter
	private Map<Trip, Double> tripVehicleDuals; 
	@Getter
	private Map<Trip, Double> tripDriverDuals; 
	@Getter
	private Map<Deadrun, Double> deadrunDuals;
	@Getter
	private Map<IdleTime, Double> idleTimeDuals; 
	public BranchAndBoundIntegrated(List<Trip> trips, Map<Block, Integer> initialBlocksAndGenerated,  Map<Duty, Integer> initialDutiesAndGenerated, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, boolean earlyTermination, int iterationLimit) throws IloException
	{
		this.trips = trips; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.earlyTermination = earlyTermination; 
		this.iterationLimit = iterationLimit; 
		
		this.initialBlocksAndGenerated = new HashMap<Block, Integer>(initialBlocksAndGenerated); 
		this.initialDutiesAndGenerated = new HashMap<Duty, Integer>(initialDutiesAndGenerated); 
		this.deadruns = new HashSet<Deadrun>(deadruns); 
		this.idleTimes = new HashSet<IdleTime>(idleTimes); 
		
		this.nodes = new ArrayList<BBNodeIntegrated>();
		this.lpObjectivesAtEachNode = new HashMap<Integer, Double>(); 
		this.blocksInSolution = new ArrayList<Block>(); 
		this.dutiesInSolution = new ArrayList<Duty>(); 
		this.objective = Double.MAX_VALUE; 
		createRootNode();
		algorithm(); 
	}
	
	private void createRootNode()
	{
		BBNodeIntegrated rootNode = new BBNodeIntegrated(this.initialBlocksAndGenerated, this.initialDutiesAndGenerated, this.trips, this.deadruns, this.idleTimes, this.vehicleGraphs, this.driverGraphs, this.earlyTermination, this.iterationLimit); 
		this.nodes.add(rootNode); 
	}
	
	private void algorithm() throws IloException
	{
		int nodeNo = 0; 
		while(!this.nodes.isEmpty())
		{
			System.out.println("Node number = " + nodeNo);
			BBNodeIntegrated currentNode = this.nodes.get(0); 
			currentNode.solve();
			this.lpObjectivesAtEachNode.put(nodeNo, currentNode.getLpObjective()); 
			if(!currentNode.isSolutionInteger())
			{
				BranchingDecisionIntegrated branchingDecision = new BranchingDecisionIntegrated(currentNode);
				BBNodeIntegrated childNode = branchingDecision.getChildNode(); 
				this.nodes.add(childNode); 
			}
			else
			{
				this.objective = currentNode.getLpObjective(); 
				getSolution(currentNode.getFractionalValuesOfBlocks(), currentNode.getFractionalValuesOfDuties()); 
			}
			
			this.nodes.remove(currentNode);
			nodeNo++; 
		}
	}
	
	private void getSolution(Map<Block, Double> blockSolution, Map<Duty, Double> dutySolution) throws IloException
	{
		List<Block> blocksInSolution = new ArrayList<Block>(); 
		List<Deadrun> deadrunsInSolution = new ArrayList<Deadrun>(); 
		List<IdleTime> idleTimesInSolution = new ArrayList<IdleTime>(); 
		List<Duty> dutiesInSolution = new ArrayList<Duty>(); 
		for(Block block : blockSolution.keySet())
		{
			Assert.assertTrue(blockSolution.get(block) >= 1- 1e-6);
			blocksInSolution.add(block); 
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				if(!deadrunsInSolution.contains(deadrun))
				{
					deadrunsInSolution.add(deadrun); 
				}
				else
				{
					for(Block block1 : blocksInSolution)
					{
						for(BlockActivity activity : block1.getBlockActivities())
						{
							System.out.println(activity.getDepartureNode().getNodeId() + "; " + activity.getArrivalNode().getNodeId() + "; " + activity.getDepartureTime() + "; " + activity.getArrivalTime() + "; " + activity.getActivity());
						}
						System.out.println();
					}
					
					System.out.println(deadrun.getDepartureNode().getNodeId() + "; " + deadrun.getArrivalNode().getNodeId() + "; " + deadrun.getDepartureTime() + "; " + deadrun.getArrivalTime());
					throw new IllegalArgumentException();
				}
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				if(!idleTimesInSolution.contains(idleTime))
				{
					idleTimesInSolution.add(idleTime); 
				}
				else
				{
					throw new IllegalArgumentException();
				}
			}
		}
		
		for(Duty duty : dutySolution.keySet())
		{
			Assert.assertTrue(dutySolution.get(duty) >= 1-1e-6);
			dutiesInSolution.add(duty); 
		}
		
		
		/*for(Block block : blocksInSolution)
		{
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
		}
		
		for(Duty duty : dutiesInSolution)
		{
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			} 
		}
		
		double totalCost = 0.0; 
		for(Block block : blocksInSolution)
		{
			totalCost = totalCost + block.getTotalCostOfBlock(); 
		}
		
		for(Duty duty : dutiesInSolution)
		{
			totalCost = totalCost + duty.getTotalCostOfDuty(); 
		}
		
		System.out.println("Total cost = " + totalCost);*/
		
		for(Integer node : this.lpObjectivesAtEachNode.keySet())
		{
			System.out.println(node + "; " + this.lpObjectivesAtEachNode.get(node));
		}
		
		this.blocksInSolution.addAll(blocksInSolution); 
		this.dutiesInSolution.addAll(dutiesInSolution); 
	}
	
	
	

}
