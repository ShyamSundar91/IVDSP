package InitialSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import lombok.Getter;

public class SingleDepotVehicleScheduling 
{
	private int lineNumber; 
	private List<Trip> tripsInLine; 
	private VehicleTypeDepot vehicleTypeDepot; 
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph;
	private List<VehicleArc> vehicleArcsWithLineTrips; 
	
	private IloCplex cplex;
	private Map<VehicleArc, IloIntVar> vehicleArcVariables;
	private Map<Trip, IloRange> successorTripConstraints;
	private Map<Trip, IloRange> predecessorTripConstaints; 
	private Map<Integer, IloRange> bendersCuts; 
	private IloRange minimumRechargingConstraint; 
	@Getter
	private List<Block> blocksInSolution;
	@Getter
	private List<Deadrun> deadrunsInSolution;
	@Getter
	private List<IdleTime> idleTimesInSolution; 
	 
	public SingleDepotVehicleScheduling(int lineNumber, List<Trip> tripsInLine, VehicleTypeDepot vehicleTypeDepot,  DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
	{
		
		this.lineNumber = lineNumber; 
		this.tripsInLine = tripsInLine; 
		this.vehicleGraph = vehicleGraph; 
		this.vehicleTypeDepot = vehicleTypeDepot;
		this.vehicleArcsWithLineTrips = new ArrayList<VehicleArc>(); 
		getArcsWithLineTrips(); 
		
		this.blocksInSolution = new ArrayList<Block>(); 
		this.deadrunsInSolution = new ArrayList<Deadrun>(); 
		this.idleTimesInSolution = new ArrayList<IdleTime>(); 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.vehicleArcVariables = new HashMap<VehicleArc, IloIntVar>();
		this.predecessorTripConstaints = addPredecessorTripConstraints(); 
		this.successorTripConstraints = addSuccessorTripConstraints();
		this.bendersCuts = new HashMap<Integer, IloRange>(); 
		
		addVehicleArcVariables();
		//this.minimumRechargingConstraint = addMinimumRechargingConstraint(); 
		
		benders(); 
	}
	
	private void benders() throws IloException
	{
		int status = 0; 
		int iterationNumber = 0; 
		this.cplex.setOut(null);
		//this.cplex.setParam(IloCplex.IntParam.TimeLimit, 300);
		
		while(status != 1)
		{
			System.out.println("************************************");
			System.out.println("Iteration number = " + iterationNumber);
			List<VehicleArc> arcsInSolution = solve(); 
			
			BendersSubproblem bendersSub = new BendersSubproblem(vehicleGraph, arcsInSolution, this.vehicleTypeDepot); 
			if(bendersSub.getViolatedSequences().isEmpty())
			{
				status = 1;
			}
			else
			{
				addCombinatorialCut(bendersSub.getViolatedSequences(), iterationNumber); 
			}
			/*if(bendersSub.isFeasible())
			{
				status = 1; 
			}
			else
			{
				addCut(vehicleGraph, arcsInSolution, bendersSub.getDistCalVehicleArcDuals(), bendersSub.getMaxDistanceDuals(), iterationNumber); 
			}*/
			
			
			iterationNumber++; 
			
		}
		
		for(Block block : blocksInSolution)
		{
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
		}
	}
	
	private void addCombinatorialCut(List<List<VehicleArc>> violatedSequences, int iteration) throws IloException
	{
		double i = 0; 
		for(List<VehicleArc> violatedSequence : violatedSequences)
		{
			//System.out.println("*****Cut******");
			IloLinearNumExpr cut = this.cplex.linearNumExpr();
			 
			double acc = 0; 
			for(VehicleArc arc : violatedSequence)
			{
				//System.out.println(arc.getPredecessorVertex().getVertexId() + ", " + arc.getSuccessorVertex().getVertexId());
				cut.addTerm(1, this.vehicleArcVariables.get(arc));
				
				/*acc = acc + arc.getSuccessorVertex().getDistance(); 
				List<VehicleArc> rechargingArcs = this.vehicleGraph.edgeSet().stream().filter(e -> e.getPredecessorVertex().equals(arc.getSuccessorVertex()) && e.getStartTimeOfReCharging() > -1).collect(Collectors.toList()); 
				for(VehicleArc rechargingArc : rechargingArcs)
				{
					double distanceToRecharging = acc + rechargingArc.getDistancedCoveredBeforeReCharging(); 
					if(distanceToRecharging <= this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging())
					{
						cut.addTerm(1, this.vehicleArcVariables.get(rechargingArc));
					}
				}*/
				
			}
			
			double rhs = violatedSequence.size() -1; 
			Assert.assertTrue(violatedSequence.size() >= 1);
			IloRange constraint = this.cplex.addRange(-Double.MAX_VALUE, cut, rhs, "ctBendersCuts_" + iteration + "_" + i);
			i++; 
			//this.bendersCuts.put(iteration, constraint);
		}
		
		/*double lb = this.minimumRechargingConstraint.getLB() + 1; 
		this.minimumRechargingConstraint.setLB(lb);*/
	}
	
	private void addCut(DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph, List<VehicleArc> arcsInSolution, Map<VehicleArc, Double> distCalVehicleArcDuals, Map<VehicleArc, Double> maxDistanceDuals, int iteration) throws IloException
	{
		double maxDistance = this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(); 
		
		IloLinearNumExpr cut = this.cplex.linearNumExpr(); 
		
		for(VehicleArc arc : vehicleGraph.edgeSet())
		{
			double coef = 0;
			if(distCalVehicleArcDuals.containsKey(arc))
			{
				if(arc.getPredecessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
				{
					coef = coef + arc.getTotalDistance() + arc.getSuccessorVertex().getDistance();
				}
				else if(arc.getPredecessorVertex().getTrip() == null)
				{
					coef = coef + arc.getTotalDistance() + arc.getSuccessorVertex().getDistance(); 
				}
				else if(arc.getStartTimeOfReCharging() > -1)
				{
					coef = coef + arc.getDistanceCoveredAfterReCharging() + arc.getSuccessorVertex().getDistance();
				}
				coef = coef + maxDistance; 
				coef = coef*distCalVehicleArcDuals.get(arc); 
			}
			
			
			double coef2 = 0; 
			if(maxDistanceDuals.containsKey(arc))
			{
				coef2 = 0; 
				if(arc.getStartTimeOfReCharging() > -1)
				{
					coef2 = coef2 - arc.getDistancedCoveredBeforeReCharging(); 
				}
				else if(arc.getSuccessorVertex().getTrip() == null)
				{
					coef2 = coef2 - arc.getTotalDistance();
				}
				
				coef2 = coef2*maxDistanceDuals.get(arc); 
			}
			
			double total = coef + coef2; 
			
			cut.addTerm(total, this.vehicleArcVariables.get(arc));
		}
		
		double rhs = maxDistanceDuals.entrySet().stream().mapToDouble(e -> e.getValue()).sum(); 
		rhs = - (rhs*maxDistance);
		
		double rhs2 = distCalVehicleArcDuals.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
		rhs2 = (rhs2*this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging()); 
		
		double totalRhs = rhs + rhs2; 
		IloRange constraint = this.cplex.addRange(-Double.MAX_VALUE, cut, totalRhs, "ctBendersCust_" + iteration);
		this.bendersCuts.put(iteration, constraint); 
		
	}
	
	private List<VehicleArc> solve() throws IloException
	{
		this.blocksInSolution.clear();
		this.deadrunsInSolution.clear();
		this.idleTimesInSolution.clear();
		
		List<VehicleArc> arcsInSolution = new ArrayList<VehicleArc>();
		
		this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
		if(this.cplex.solve())
		{
			System.out.println("Objective = " + this.cplex.getObjValue());
			
			for(VehicleArc arc : this.vehicleArcVariables.keySet())
			{
				double value = this.cplex.getValue(this.vehicleArcVariables.get(arc)); 
				
				if(value >= (1- 1e-6))
				{
					arcsInSolution.add(arc); 
				}
				else if(value > 1e-6)
				{
					System.out.println(value);
					throw new IllegalArgumentException(); 
				}
			}
			
			List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
			for(VehicleArc start : starts)
			{ 
				List<Trip> trips = new ArrayList<Trip>(); 
				List<BlockActivity> blockActivities = new ArrayList<BlockActivity>(); 
				List<Deadrun> deadruns = new ArrayList<Deadrun>(); 
				List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
				
				boolean stop = false; 
				VehicleArc current = start; 
				while(!stop)
				{
					
					blockActivities.addAll(current.getBlockActivitiesOnEdge()); 
					if(!current.getDeadrunsOnEdge().isEmpty())
					{
						deadruns.addAll(current.getDeadrunsOnEdge());
					}
					 
					if(current.getIdleTimeOnArc() != null)
					{
						idleTimes.add(current.getIdleTimeOnArc());
					}
					 
					
					
					if(current.getSuccessorVertex().getTrip() != null)
					{
						Trip trip = current.getSuccessorVertex().getTrip(); 
						trips.add(trip); 
						blockActivities.add(current.getSuccessorVertex().getBlockActivity()); 
						current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
					}
					else
					{
						stop = true; 
					}
				}
				
				Collections.sort(blockActivities);
				Block block = new Block(this.vehicleTypeDepot.getVehicleType(), trips, deadruns, idleTimes, blockActivities); 
				this.blocksInSolution.add(block); 
				for(Deadrun deadrun : block.getDeadrunsInBlock())
				{
					if(!this.deadrunsInSolution.contains(deadrun))
					{
						this.deadrunsInSolution.add(deadrun); 
					}
					else
					{
						throw new IllegalArgumentException(); 
					}
				}
				
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					if(!this.idleTimesInSolution.contains(idleTime))
					{
						this.idleTimesInSolution.add(idleTime); 
					}
					else
					{
						throw new IllegalArgumentException(); 
					}
				}
			}
			
			/*for(Block block : blocksInSolution)
			{
				for(BlockActivity ba : block.getBlockActivities())
				{
					System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
				}
			}*/
		}
		
		
		return arcsInSolution; 
	}
	
	
	private void getArcsWithLineTrips()
	{
		
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			if(arc.getPredecessorVertex().getTrip() == null)
			{
				if(this.tripsInLine.contains(arc.getSuccessorVertex().getTrip()))
				{
					this.vehicleArcsWithLineTrips.add(arc); 
				}
			}
			else if(arc.getSuccessorVertex().getTrip() == null)
			{
				if(this.tripsInLine.contains(arc.getPredecessorVertex().getTrip()))
				{
					this.vehicleArcsWithLineTrips.add(arc); 
				}
			}
			else
			{
				if(this.tripsInLine.contains(arc.getPredecessorVertex().getTrip()) && this.tripsInLine.contains(arc.getSuccessorVertex().getTrip()))
				{
					this.vehicleArcsWithLineTrips.add(arc); 
				}
			}
		}
	}
	
	private void addVehicleArcVariables() throws IloException
	{
		for(VehicleArc arcWithLineTrips : this.vehicleArcsWithLineTrips)
		{
			double coef = arcWithLineTrips.getTotalDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm(); 
			if(arcWithLineTrips.getPredecessorVertex().getTrip() == null)
			{
				coef = coef + (arcWithLineTrips.getSuccessorVertex().getDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm()); 
				coef = coef + this.vehicleTypeDepot.getVehicleType().getFixedCost(); 
				
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arcWithLineTrips.getSuccessorVertex().getTrip()), 1)); 
				
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.intVar(arcVariable, 0, 1)); 
			}
			else if(arcWithLineTrips.getSuccessorVertex().getTrip() == null)
			{
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arcWithLineTrips.getPredecessorVertex().getTrip()), 1));
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.intVar(arcVariable, 0, 1));
			}
			else
			{
				coef = coef + (arcWithLineTrips.getSuccessorVertex().getDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm());
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arcWithLineTrips.getSuccessorVertex().getTrip()), 1)); 
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arcWithLineTrips.getPredecessorVertex().getTrip()), 1));
				
				
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.intVar(arcVariable, 0, 1)); 
			}
		}
	}
	
	private IloRange addMinimumRechargingConstraint() throws IloException
	{
		List<VehicleArc> rechargingArcs = this.vehicleGraph.edgeSet().stream().filter(a -> a.getStartTimeOfReCharging() > -1).collect(Collectors.toList()); 
		
		IloLinearNumExpr recharge = this.cplex.linearNumExpr(); 
		for(VehicleArc rechargingArc : rechargingArcs)
		{
			recharge.addTerm(1, this.vehicleArcVariables.get(rechargingArc));
		}
		
		IloRange rechargingConstraint = this.cplex.addRange(0, recharge, Double.MAX_VALUE, "ctMinRecharge_"); 
		
		return rechargingConstraint; 
	}
	
	private Map<Trip, IloRange> addPredecessorTripConstraints() throws IloException
	{
		Map<Trip, IloRange> predecessorTripConstraints = new HashMap<Trip, IloRange>();
		for(Trip trip : this.tripsInLine)
		{
			predecessorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctPredecessor_" + trip.getTripId())); 
		}
		
		return predecessorTripConstraints; 
	}
	
	private Map<Trip, IloRange> addSuccessorTripConstraints() throws IloException
	{
		Map<Trip, IloRange> successorTripConstraints = new HashMap<Trip, IloRange>();
		for(Trip trip : this.tripsInLine)
		{
			successorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctSuccessor_" + trip.getTripId())); 
		}
		
		return successorTripConstraints; 
	}
}
