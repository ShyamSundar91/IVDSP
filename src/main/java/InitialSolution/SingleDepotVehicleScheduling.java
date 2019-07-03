package InitialSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

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
	private Map<VehicleVertex, IloNumVar> accDistanceAtVertexVariables; 
	private Map<Trip, IloRange> successorTripConstraints;
	private Map<Trip, IloRange> predecessorTripConstaints;
	private Map<VehicleArc, IloRange> distanceCalculationConstraints; 
	private Map<VehicleArc,IloRange> maxDistanceConstraints; 
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
		this.accDistanceAtVertexVariables = new HashMap<VehicleVertex, IloNumVar>(); 
		this.predecessorTripConstaints = addPredecessorTripConstraints(); 
		this.successorTripConstraints = addSuccessorTripConstraints();
		this.distanceCalculationConstraints = addDistanceCalculationConstraints(); 
		this.maxDistanceConstraints = new HashMap<VehicleArc,IloRange>(); 
		
		addVehicleArcVariables(); 
		addDistanceAccumulationVertex(); 
		
		lagrangian(); 
	}
	
	private void lagrangian() throws IloException
	{
		int status = 0; 
		int iterationNumber = 0; 
		
		while(status != 1)
		{
			Map<VehicleArc, Double> accumulatedDistanceOnArcs = solve(); 
			
			boolean stop = checkTerimation(accumulatedDistanceOnArcs); 
			
			if(iterationNumber == 0)
			{
				this.cplex.setParam(IloCplex.Param.TimeLimit, 30);
				/*this.cplex.delete(this.cplex.getObjective());
				
				IloLinearNumExpr obj = this.cplex.linearNumExpr(); 
				for(VehicleArc arc : this.vehicleArcsWithLineTrips)
				{
					if(arc.getStartTimeOfReCharging() > -1)
					{
						obj.addTerm(1, this.vehicleArcVariables.get(arc));
					}
				}
				
				this.cplex.addMaximize(obj);*/ 
			}
			iterationNumber++; 
			
			if(stop)
			{
				status = 1; 
			}
		}
	}
	
	private Map<VehicleArc, Double> solve() throws IloException
	{
		this.blocksInSolution.clear();
		this.deadrunsInSolution.clear();
		this.idleTimesInSolution.clear();
		Map<VehicleArc, Double> accumulatedDistanceOnArcs = new HashMap<VehicleArc, Double>();
		
		if(this.cplex.solve())
		{
			System.out.println("Objective = " + this.cplex.getObjValue());
			
			List<VehicleArc> arcsInSolution = new ArrayList<VehicleArc>(); 
			
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
				double accDist = 0.0; 
				List<Trip> trips = new ArrayList<Trip>(); 
				List<BlockActivity> blockActivities = new ArrayList<BlockActivity>(); 
				List<Deadrun> deadruns = new ArrayList<Deadrun>(); 
				List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
				
				boolean stop = false; 
				VehicleArc current = start; 
				while(!stop)
				{
					if(current.getStartTimeOfReCharging() == -1 && current.getSuccessorVertex().getTrip() != null)
					{
						accDist = accDist + current.getTotalDistance() + current.getSuccessorVertex().getDistance(); 
					}
					else if(current.getStartTimeOfReCharging() > -1)
					{
						accDist = accDist + current.getDistancedCoveredBeforeReCharging(); 
						//System.out.println("Arc = " + current.getPredecessorVertex().getVertexId() + "_" + current.getSuccessorVertex().getVertexId() + ", dist = " + value);
						accumulatedDistanceOnArcs.put(current, accDist); 
						
						accDist = 0.0; 
						accDist = current.getDistanceCoveredAfterReCharging() + current.getSuccessorVertex().getDistance(); 
					}
					else if(current.getSuccessorVertex().getTrip() == null)
					{
						accDist = accDist + current.getTotalDistance(); 
						//System.out.println("Arc = " + current.getPredecessorVertex().getVertexId() + "_" + current.getSuccessorVertex().getVertexId() + ", dist = " + value);
						accumulatedDistanceOnArcs.put(current, accDist); 
					}
					
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
		}
		
		return accumulatedDistanceOnArcs; 
	
	}
	
	
	private boolean checkTerimation(Map<VehicleArc, Double> accumulatedDistanceOnArcs) throws IloException
	{
		boolean added = false; 
		for(VehicleArc arc : accumulatedDistanceOnArcs.keySet())
		{
			if(accumulatedDistanceOnArcs.get(arc) > this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging())
			{
				added = true; 
				addMaxDistanceConstraints(arc); 
				break; 
			}
		}
		
		if(!added)
		{
			return true; 
		}
	
		return false; 
	}
	
	private void addMaxDistanceConstraints(VehicleArc arc) throws IloException
	{
		IloLinearNumExpr constraint = this.cplex.linearNumExpr(); 
		
		constraint.addTerm(1, this.accDistanceAtVertexVariables.get(arc.getPredecessorVertex()));
		
		double coef = 0.0; 
		if(arc.getStartTimeOfReCharging() > -1)
		{
			coef = arc.getDistancedCoveredBeforeReCharging(); 
		}
		else
		{
			coef = arc.getTotalDistance(); 
		}
		
		constraint.addTerm(coef, this.vehicleArcVariables.get(arc));
		
		this.maxDistanceConstraints.put(arc, this.cplex.addRange(-Double.MAX_VALUE, constraint, this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(), "CtMaxDist_" + arc.getPredecessorVertex().getVertexId() +"_" + arc.getSuccessorVertex().getVertexId())); 
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
		double maxDist = this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(); 
		for(VehicleArc arcWithLineTrips : this.vehicleArcsWithLineTrips)
		{
			double coef = arcWithLineTrips.getTotalDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm(); 
			if(arcWithLineTrips.getPredecessorVertex().getTrip() == null)
			{
				coef = coef + (arcWithLineTrips.getSuccessorVertex().getDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm()); 
				coef = coef + this.vehicleTypeDepot.getVehicleType().getFixedCost(); 
				
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arcWithLineTrips.getSuccessorVertex().getTrip()), 1)); 
				
				double totalDistance = arcWithLineTrips.getTotalDistance() + arcWithLineTrips.getSuccessorVertex().getDistance() + maxDist;
				arcVariable = arcVariable.and(this.cplex.column(this.distanceCalculationConstraints.get(arcWithLineTrips), -totalDistance)); 
				
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
				
				if(arcWithLineTrips.getStartTimeOfReCharging() > -1)
				{
					double totalDistance = arcWithLineTrips.getDistanceCoveredAfterReCharging() + arcWithLineTrips.getSuccessorVertex().getDistance() + maxDist;
					arcVariable = arcVariable.and(this.cplex.column(this.distanceCalculationConstraints.get(arcWithLineTrips), -totalDistance)); 
				}
				else
				{
					double totalDistance = arcWithLineTrips.getTotalDistance() + arcWithLineTrips.getSuccessorVertex().getDistance() + maxDist;
					arcVariable = arcVariable.and(this.cplex.column(this.distanceCalculationConstraints.get(arcWithLineTrips), -totalDistance)); 
				}
				
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.intVar(arcVariable, 0, 1)); 
			}
		}
	}
	
	private void addDistanceAccumulationVertex() throws IloException
	{
		List<VehicleVertex> tripVertices = this.vehicleGraph.vertexSet().stream().filter(v -> v.getTrip()!= null && this.tripsInLine.contains(v.getTrip())).collect(Collectors.toList()); 
		
		for(VehicleVertex tripVertex : tripVertices)
		{
			IloColumn vertesDistVar  = this.cplex.column(this.cplex.getObjective(), 0); 
			
			Set<VehicleArc> outgoingEdges = this.vehicleGraph.outgoingEdgesOf(tripVertex).stream().filter(v -> this.vehicleArcsWithLineTrips.contains(v)).collect(Collectors.toSet()); 
			
			for(VehicleArc outgoingArc : outgoingEdges)
			{
				if(outgoingArc.getSuccessorVertex().getTrip() != null && outgoingArc.getStartTimeOfReCharging() == -1)
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalculationConstraints.get(outgoingArc), -1)); 
				}	
			}
			
			Set<VehicleArc> incomingEdges = this.vehicleGraph.incomingEdgesOf(tripVertex).stream().filter(v -> this.vehicleArcsWithLineTrips.contains(v)).collect(Collectors.toSet()); 
			
			for(VehicleArc incomingArc : incomingEdges)
			{
				vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalculationConstraints.get(incomingArc), 1)); 
			}
			
			this.accDistanceAtVertexVariables.put(tripVertex, this.cplex.numVar(vertesDistVar, 0, Double.MAX_VALUE, "dist_" + tripVertex.getVertexId())); 
		}
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
	
	private Map<VehicleArc, IloRange> addDistanceCalculationConstraints() throws IloException
	{
		Map<VehicleArc, IloRange> distanceCalConstraints = new HashMap<VehicleArc, IloRange>(); 
		double maxDist = this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(); 
		
		for(VehicleArc arc : this.vehicleArcsWithLineTrips)
		{
			if(arc.getPredecessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-maxDist, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}
			else if(arc.getPredecessorVertex().getTrip() == null)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-maxDist, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}
			else if(arc.getStartTimeOfReCharging() > -1)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-maxDist, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}	
		}
		
		return distanceCalConstraints; 
	}
}
