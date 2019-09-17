package BranchPriceDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import ilog.concert.IloException;
import lombok.Getter;

public class BranchAndBoundDriver 
{
	private List<Trip> trips; 
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
	private Map<Duty, Integer> initialAndDutiesGenerated;
	private List<Block> blocksInSolution; 
	private Set<Deadrun> deadrunsInSolution; 
	private Set<IdleTime> idleTimesInSolution; 
	private boolean earlyTermination;
	@Getter
	private List<Duty> dutiesGenerated; 
	@Getter
	private List<Duty> dutiesInSolution; 
	private List<BBNodeDriver> nodes; 
	@Getter
	private Map<Integer,List<Double>> lpObjectivesAtEachNode; 
	@Getter
	private double objective; 
	@Getter
	private double totalTimeSpentInMaster; 
	@Getter
	private double totalTimeSpentInSubproblem; 
	@Getter
	private int nodeNo; 
	public BranchAndBoundDriver(List<Trip> trips, List<Block> blocksInSolution, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Duty, Integer> initialAndDutiesGenerated, boolean earlyTermination) throws IloException
	{
		this.trips = trips; 
		this.driverGraphs = driverGraphs; 
		this.deadrunsInSolution = deadrunsInSolution; 
		this.idleTimesInSolution = idleTimesInSolution; 
		this.initialAndDutiesGenerated = initialAndDutiesGenerated; 
		this.earlyTermination = earlyTermination; 
		
		this.dutiesGenerated = new ArrayList<Duty>(); 
		this.nodes = new ArrayList<BBNodeDriver>();
		this.lpObjectivesAtEachNode = new HashMap<Integer, List<Double>>(); 
		this.objective = Double.MAX_VALUE; 
		this.totalTimeSpentInMaster = 0; 
		this.totalTimeSpentInSubproblem = 0; 
		this.nodeNo = 0;
		
		createRootNode(); 
		algorithm(); 
	}
	
	private void createRootNode() throws IloException
	{
		BBNodeDriver rootNode = new BBNodeDriver(this.trips, this.blocksInSolution, this.deadrunsInSolution, this.idleTimesInSolution, this.driverGraphs, this.initialAndDutiesGenerated, this.earlyTermination); 
		this.nodes.add(rootNode); 
	}
	
	private void algorithm() throws IloException
	{
		while(!this.nodes.isEmpty())
		{
			System.out.println("Node number = " + nodeNo);
			long start = System.currentTimeMillis(); 
			BBNodeDriver currentNode = this.nodes.get(0); 
			currentNode.solveCG();
			long end = System.currentTimeMillis(); 
			this.totalTimeSpentInMaster = this.totalTimeSpentInMaster + currentNode.getTotalTimeSpentInMaster(); 
			this.totalTimeSpentInSubproblem = this.totalTimeSpentInSubproblem + currentNode.getTotalTimeSpentInSub();
			List<Double> objTime = new ArrayList<Double>(); 
			objTime.add(currentNode.getLpObjective()); 
			objTime.add((double)(end-start)/1000.00); 
			this.lpObjectivesAtEachNode.put(nodeNo, objTime); 
			
			if(!currentNode.isSolutionInteger())
			{
				BranchingDecisionDriver branchingDecision = new BranchingDecisionDriver(currentNode);
				BBNodeDriver childNode = branchingDecision.getChildNode(); 
				this.nodes.add(childNode); 
			}
			else
			{
				this.dutiesInSolution = currentNode.getDutiesInSolution(); 
				this.objective = currentNode.getLpObjective(); 
			}
			
			this.dutiesGenerated.clear();
			this.dutiesGenerated.addAll(currentNode.getDutyVariables().keySet()); 
			this.nodes.remove(currentNode);
			nodeNo++; 
		}
		
		/*for(Duty duty : this.dutiesInSolution)
		{
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			} 
		}*/
		
		if(!this.earlyTermination)
		{
			double nodeLp = 0; 
			for(Integer node : this.lpObjectivesAtEachNode.keySet())
			{
				System.out.println(node + "; " + this.lpObjectivesAtEachNode.get(node));
				double val1 = this.lpObjectivesAtEachNode.get(node).get(0); 
				Assert.assertTrue(val1 - (nodeLp - 1e-2) >= 0);
				nodeLp = val1; 
			}
		}
		
		
	}

}
