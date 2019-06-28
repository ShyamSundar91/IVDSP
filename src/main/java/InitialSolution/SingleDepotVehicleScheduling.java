package InitialSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	private IloCplex cplex;
	private Map<VehicleArc, IloNumVar> vehicleArcVariables;
	private Map<Trip, IloRange> successorTripConstraints;
	private Map<Trip, IloRange> predecessorTripConstaints;
	@Getter
	private List<Block> blocksInSolution; 
	public SingleDepotVehicleScheduling(int lineNumber, List<Trip> tripsInLine, VehicleTypeDepot vehicleTypeDepot,  DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
	{
		this.lineNumber = lineNumber; 
		this.tripsInLine = tripsInLine; 
		this.vehicleGraph = vehicleGraph; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.blocksInSolution = new ArrayList<Block>(); 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.vehicleArcVariables = new HashMap<VehicleArc, IloNumVar>();
		this.predecessorTripConstaints = addPredecessorTripConstraints(); 
		this.successorTripConstraints = addSuccessorTripConstraints(); 
		addVehicleArcVariables(); 
		
		solve(); 
	}
	
	private void solve() throws IloException
	{
		if(this.cplex.solve())
		{
			System.out.println("Objective = " + this.cplex.getObjValue());
			
			List<VehicleArc> arcsInSolution = new ArrayList<VehicleArc>(); 
			
			for(VehicleArc arc : this.vehicleArcVariables.keySet())
			{
				double value = this.cplex.getValue(this.vehicleArcVariables.get(arc)); 
				
				if(value >= (1.0 - 1e-6))
				{
					arcsInSolution.add(arc); 
				}
				else if(value > 1e-6)
				{
					System.out.println("Error: Solution fractional");
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
					deadruns.addAll(current.getDeadrunsOnEdge()); 
					idleTimes.add(current.getIdleTimeOnArc()); 
					
					
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
			}
		}
	}
	
	private void addVehicleArcVariables() throws IloException
	{
		List<VehicleArc> vehicleArcsWithLineTrips = new ArrayList<VehicleArc>(); 
		
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			if(arc.getPredecessorVertex().getTrip() == null)
			{
				if(this.tripsInLine.contains(arc.getSuccessorVertex().getTrip()))
				{
					vehicleArcsWithLineTrips.add(arc); 
				}
			}
			else if(arc.getSuccessorVertex().getTrip() == null)
			{
				if(this.tripsInLine.contains(arc.getPredecessorVertex().getTrip()))
				{
					vehicleArcsWithLineTrips.add(arc); 
				}
			}
			else
			{
				if(this.tripsInLine.contains(arc.getPredecessorVertex().getTrip()) && this.tripsInLine.contains(arc.getSuccessorVertex().getTrip()))
				{
					vehicleArcsWithLineTrips.add(arc); 
				}
			}
		}
		
		for(VehicleArc arcWithLineTrips : vehicleArcsWithLineTrips)
		{
			double coef = arcWithLineTrips.getTotalDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm(); 
			if(arcWithLineTrips.getPredecessorVertex().getTrip() == null)
			{
				coef = coef + (arcWithLineTrips.getSuccessorVertex().getDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm()); 
				coef = coef + this.vehicleTypeDepot.getVehicleType().getFixedCost(); 
				
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arcWithLineTrips.getSuccessorVertex().getTrip()), 1)); 
				
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.numVar(arcVariable, 0, 1)); 
			}
			else if(arcWithLineTrips.getSuccessorVertex().getTrip() == null)
			{
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arcWithLineTrips.getPredecessorVertex().getTrip()), 1));
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.numVar(arcVariable, 0, 1));
			}
			else
			{
				coef = coef + (arcWithLineTrips.getSuccessorVertex().getDistance() * this.vehicleTypeDepot.getVehicleType().getCostPerkm());
				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arcWithLineTrips.getSuccessorVertex().getTrip()), 1)); 
				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arcWithLineTrips.getPredecessorVertex().getTrip()), 1));
				
				this.vehicleArcVariables.put(arcWithLineTrips, this.cplex.numVar(arcVariable, 0, 1)); 
			}
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
}
