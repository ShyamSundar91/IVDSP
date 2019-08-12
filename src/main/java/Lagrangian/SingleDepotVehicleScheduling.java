package Lagrangian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import lombok.Getter;

public class SingleDepotVehicleScheduling 
{
	private IloCplex cplex;
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph; 
	private List<Trip> trips; 
	private Map<VehicleArc, IloNumVar> vehicleArcVariable;
	private Map<Trip, IloRange> successorTripConstraints;
	private Map<Trip, IloRange> predecessorTripConstaints; 
	
	private Map<Deadrun, Double> deadrunMultipliers; 
	private Map<IdleTime, Double> idleTimeMultipliers; 
	
	@Getter
	private Set<Deadrun> deadrunsInSolution;
	@Getter
	private Set<IdleTime> idleTimesInSolution; 
	@Getter
	private double objective; 
	@Getter
	private List<VehicleArc> arcsInSolution ; 
	
	public SingleDepotVehicleScheduling(List<Trip> trips,Map<Deadrun, Double> deadrunMultipliers,Map<IdleTime, Double> idleTimeMultipliers, DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
	{
		this.vehicleGraph = vehicleGraph; 
		this.deadrunMultipliers = deadrunMultipliers; 
		this.idleTimeMultipliers = idleTimeMultipliers; 
		this.trips = trips; 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.vehicleArcVariable = new HashMap<VehicleArc, IloNumVar>(); 
		this.successorTripConstraints = addSuccessorTripConstraints(); 
		this.predecessorTripConstaints = addPredecessorTripConstraints();

		this.deadrunsInSolution = new HashSet<Deadrun>(); 
		this.idleTimesInSolution = new HashSet<IdleTime>(); 
		this.arcsInSolution = new ArrayList<VehicleArc>(); 
		addVehicleArcVariables();  
		solve(); 
		
		this.cplex.end();
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
			if(!this.deadrunMultipliers.isEmpty())
			{
				for(Deadrun deadrun : arc.getDeadrunsOnEdge())
				{
					coef = coef + this.deadrunMultipliers.get(deadrun);  
				}
			}
			
			if(!this.idleTimeMultipliers.isEmpty())
			{
				if(arc.getIdleTimeOnArc() != null)
				{
					coef = coef + this.idleTimeMultipliers.get(arc.getIdleTimeOnArc()); 
				}
			}
			if(arc.getPredecessorVertex().getTrip() == null)
			{
				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm()); 
				coef = coef + arc.getVehicleTypeDepot().getVehicleType().getFixedCost(); 
				
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
				
				this.vehicleArcVariable.put(arc, this.cplex.numVar(arcVariable, 0, 1)); 
			}
			else if(arc.getSuccessorVertex().getTrip() == null)
			{
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
				this.vehicleArcVariable.put(arc, this.cplex.numVar(arcVariable, 0, 1));
			}
			else
			{
				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm());
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
				
				this.vehicleArcVariable.put(arc, this.cplex.numVar(arcVariable, 0, 1)); 
			}
			
			
		}
	}
	
	
	private void solve() throws IloException
	{
		//this.cplex.exportModel("qap.lp");
		this.cplex.setOut(null);
		
		if(this.cplex.solve())
		{
			//System.out.println(this.cplex.getObjValue());
			this.objective = this.cplex.getObjValue(); 
			for(VehicleArc arc : this.vehicleArcVariable.keySet())
			{
				double value = this.cplex.getValue(this.vehicleArcVariable.get(arc));
				
				if(value > 0.99)
				{
					arcsInSolution.add(arc); 
					if(!arc.getDeadrunsOnEdge().isEmpty())
					{
						this.deadrunsInSolution.addAll(arc.getDeadrunsOnEdge()); 
					}
					
					if(arc.getIdleTimeOnArc() != null)
					{
						this.idleTimesInSolution.add(arc.getIdleTimeOnArc()); 
					}
				}
			}
			/*List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
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
