package LinearProgramming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleVertex;
import Variables.BlockActivity;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class QuasiAssignmentFormulation 
{
	private IloCplex cplex;
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph; 
	private List<Trip> trips; 
	private Map<VehicleArc, IloIntVar> vehicleArcVariable;
	private Map<Trip, IloRange> successorTripConstraints;
	private Map<Trip, IloRange> predecessorTripConstaints; 
	private Map<VehicleVertex, IloNumVar> accDistanceAtVertex;  
	private Map<VehicleArc, IloRange> distanceCalConstraints; 
	private Map<VehicleArc, IloRange> maxDistWithoutRechargingConstraints; 
	public QuasiAssignmentFormulation(List<Trip> trips, DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
	{
		this.vehicleGraph = vehicleGraph; 
		this.trips = trips; 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.vehicleArcVariable = new HashMap<VehicleArc, IloIntVar>(); 
		this.accDistanceAtVertex = new HashMap<VehicleVertex, IloNumVar>(); 
		this.successorTripConstraints = addSuccessorTripConstraints(); 
		this.predecessorTripConstaints = addPredecessorTripConstraints();
		this.distanceCalConstraints = addDistanceCalculationConstraints(); 
		this.maxDistWithoutRechargingConstraints = addMaxDistanceWithoutRechargingConstraints(); 
		addVehicleArcVariables(); 
		addDistanceAccumulationVertex(); 
		solve(); 
	}
	
	private Map<VehicleArc, IloRange> addMaxDistanceWithoutRechargingConstraints() throws IloException
	{
		Map<VehicleArc, IloRange> maxDistanceWithoutRecharging = new HashMap<VehicleArc, IloRange>(); 
		
		double maxDist = 100; 
		List<VehicleArc> rechargingArcs = this.vehicleGraph.edgeSet().stream().filter(v -> v.getSuccessorVertex().getTrip() == null || v.getStartTimeOfReCharging() > -1).collect(Collectors.toList()); 
		for(VehicleArc arc : rechargingArcs)
		{
			maxDistanceWithoutRecharging.put(arc, this.cplex.addRange(0, maxDist, "ctMaxDistRecharge_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
		}
		return maxDistanceWithoutRecharging; 
	}
	
	private Map<VehicleArc, IloRange> addDistanceCalculationConstraints() throws IloException
	{
		Map<VehicleArc, IloRange> distanceCalConstraints = new HashMap<VehicleArc, IloRange>(); 
		
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			if(arc.getPredecessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-200.00, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}
			else if(arc.getPredecessorVertex().getTrip() == null)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-200.00, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}
			else if(arc.getStartTimeOfReCharging() > -1)
			{
				distanceCalConstraints.put(arc, this.cplex.addRange(-200.00, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
			}	
		}
		
		return distanceCalConstraints; 
	}
	
	private Map<Trip, IloRange> addPredecessorTripConstraints() throws IloException
	{
		Map<Trip, IloRange> successorTripConstraints = new HashMap<Trip, IloRange>();
		for(Trip trip : this.trips)
		{
			successorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctPredeccor_" + trip.getTripId())); 
		}
		
		return successorTripConstraints; 
	}
	
	private Map<Trip, IloRange> addSuccessorTripConstraints() throws IloException
	{
		Map<Trip, IloRange> successorTripConstraints = new HashMap<Trip, IloRange>();
		for(Trip trip : this.trips)
		{
			successorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctSuccessor_" + trip.getTripId())); 
		}
		
		return successorTripConstraints;
	}

	private void addVehicleArcVariables() throws IloException
	{
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			double coef = arc.getTotalDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm(); 
			if(arc.getPredecessorVertex().getTrip() == null)
			{
				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm()); 
				coef = coef + arc.getVehicleTypeDepot().getVehicleType().getFixedCost(); 
				
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
				
				double totalDistance = arc.getTotalDistance() + arc.getSuccessorVertex().getDistance() + 200.00;
				arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1)); 
			}
			else if(arc.getSuccessorVertex().getTrip() == null)
			{
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
				arcVariable = arcVariable.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(arc), arc.getTotalDistance())); 
				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1));
			}
			else
			{
				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm());
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
				if(arc.getStartTimeOfReCharging() > -1)
				{
					double totalDistance = arc.getDistanceCoveredAfterReCharging() + arc.getSuccessorVertex().getDistance() + 200.00;
					arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
					arcVariable = arcVariable.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(arc), arc.getDistancedCoveredBeforeReCharging())); 
				}
				else
				{
					double totalDistance = arc.getTotalDistance() + arc.getSuccessorVertex().getDistance() + 200.00;
					arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
				}
				
				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1)); 
			}
			
			
		}
	}
	
	private void addDistanceAccumulationVertex() throws IloException
	{
		List<VehicleVertex> tripVertices = this.vehicleGraph.vertexSet().stream().filter(v -> v.getTrip()!= null).collect(Collectors.toList()); 
		
		for(VehicleVertex tripVertex : tripVertices)
		{
			IloColumn vertesDistVar  = this.cplex.column(this.cplex.getObjective(), 0); 
			
			Set<VehicleArc> outgoingEdges = this.vehicleGraph.outgoingEdgesOf(tripVertex); 
			
			for(VehicleArc outgoingArc : outgoingEdges)
			{
				if(outgoingArc.getSuccessorVertex().getTrip() != null && outgoingArc.getStartTimeOfReCharging() == -1)
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalConstraints.get(outgoingArc), -1)); 
				}
				else
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(outgoingArc), 1));
				}
					
			}
			
			Set<VehicleArc> incomingEdges = this.vehicleGraph.incomingEdgesOf(tripVertex); 
			
			for(VehicleArc incomingArc : incomingEdges)
			{
				vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalConstraints.get(incomingArc), 1)); 
			}
			
			this.accDistanceAtVertex.put(tripVertex, this.cplex.numVar(vertesDistVar, 0, Double.MAX_VALUE, "dist_" + tripVertex.getVertexId())); 
		}
	}
	
	private void solve() throws IloException
	{
		//this.cplex.exportModel("qap.lp");
		List<VehicleArc> arcsInSolution = new ArrayList<VehicleArc>(); 
		if(this.cplex.solve())
		{
			System.out.println(this.cplex.getObjValue());
			for(VehicleArc arc : this.vehicleArcVariable.keySet())
			{
				double value = this.cplex.getValue(this.vehicleArcVariable.get(arc));
				
				if(value > 0.99)
				{
					/*System.out.println(arc.getPredecessorVertex().getVertexId() + " -------> " + arc.getSuccessorVertex().getVertexId());
					arc.getBlockActivitiesOnEdge().forEach(b -> {
						System.out.println(b.getActivity() + ";" + b.getDistance());
					});*/
					arcsInSolution.add(arc); 
				}
			}
			List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
			for(VehicleArc start : starts)
			{
				System.out.println("*************************");
				boolean stop = false; 
				VehicleArc current = start; 
				while(!stop)
				{
					for(BlockActivity ba : current.getBlockActivitiesOnEdge())
					{
						System.out.println(ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
					}
					
					if(current.getSuccessorVertex().getTrip() != null)
					{
						Trip trip = current.getSuccessorVertex().getTrip(); 
						System.out.println(trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime() + "; " + "Trip" + "; " + trip.getTripId() + ";" + trip.getDistance());
						current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
					}
					else
					{
						stop = true; 
					}
				}
				
				
			}
			
			System.out.println("*************************");
			
			/*for(VehicleVertex tripVertex : this.accDistanceAtVertex.keySet())
			{
				System.out.println(tripVertex.getTrip().getTripId() + ", Distance = " + this.cplex.getValue(this.accDistanceAtVertex.get(tripVertex)));
			}*/
		}
	}
}
