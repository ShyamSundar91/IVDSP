package BranchPriceVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class BranchAndBoundVehicle 
{
	private List<Trip> trips; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
	private Map<Block, Integer> initialBlocksAndGenerated; 
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers; 
	private boolean earlyTermination;
	private List<BBNodeVehicle> nodes; 
	@Getter
	private List<Block> blocksInSolution; 
	@Getter
	private List<Deadrun> deadrunsInSolution;
	@Getter
	private List<IdleTime> idleTimesInSolution;
	@Getter
	private List<Block> blocksGenerated; 
	private Map<Integer, Double> lpObjectivesAtEachNode; 
	@Getter
	private double objective; 
	public BranchAndBoundVehicle(List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph, Map<Block, Integer> initialBlocksAndGenerated, Map<Deadrun, Double> deadrunMultipliers, Map<IdleTime, Double> idleTimeMultipliers, boolean earlyTermination) throws IloException
	{
		this.trips = trips; 
		this.vehicleGraph = vehicleGraph; 
		this.initialBlocksAndGenerated = initialBlocksAndGenerated; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.earlyTermination = earlyTermination; 
		this.blocksGenerated = new ArrayList<Block>(); 
		
		this.nodes = new ArrayList<BBNodeVehicle>();
		this.lpObjectivesAtEachNode = new HashMap<Integer, Double>(); 
		this.objective = Double.MAX_VALUE;  
		createRootNode(); 
		algorithm(); 
	}
	
	private void createRootNode() throws IloException
	{
		BBNodeVehicle rootNode = new BBNodeVehicle(this.trips, this.vehicleGraph, this.initialBlocksAndGenerated, this.deadrunMultipliers, this.idleTimeMultipliers, this.earlyTermination); 
		this.nodes.add(rootNode); 
	}
	
	private void algorithm() throws IloException
	{
		int nodeNo = 0; 
		while(!this.nodes.isEmpty())
		{
			System.out.println("Node number = " + nodeNo);
			long start = System.currentTimeMillis(); 
			BBNodeVehicle currentNode = this.nodes.get(0); 
			currentNode.solveCG();
			this.lpObjectivesAtEachNode.put(nodeNo, currentNode.getLpObjective()); 
			long end = System.currentTimeMillis(); 
			if(nodeNo == 0)
			{
				System.out.println("Time taken at root node = " + (double)(end-start)/1000.00);
			}
			if(!currentNode.isSolutionInteger())
			{
				BranchingDecisionVehicle branchingDecision = new BranchingDecisionVehicle(currentNode);
				BBNodeVehicle childNode = branchingDecision.getChildNode(); 
				this.nodes.add(childNode); 
			}
			else
			{
				this.objective = currentNode.getLpObjective(); 
				this.blocksInSolution = currentNode.getBlocksInSolution(); 
				this.deadrunsInSolution = currentNode.getDeadrunsInSolution(); 
				this.idleTimesInSolution = currentNode.getIdleTimesInSolution(); 
			}
			
			this.blocksGenerated.clear();
			this.blocksGenerated.addAll(currentNode.getBlockVariables().keySet()); 
			this.nodes.remove(currentNode);
			nodeNo++; 
		}
		
		/*for(Block block : blocksInSolution)
		{
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
		}*/
		
		
		double nodeLp = 0; 
		for(Integer node : this.lpObjectivesAtEachNode.keySet())
		{
			System.out.println(node + "; " + this.lpObjectivesAtEachNode.get(node));
			double val1 = Math.round(this.lpObjectivesAtEachNode.get(node) * 100.0) / 100.0; 
			//Assert.assertTrue(val1 - nodeLp >= 0);
			nodeLp = val1; 
		}
	}
	
	

}
