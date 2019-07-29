//package Lagrangian;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.jgrapht.graph.DefaultDirectedGraph;
//import org.junit.Assert;
//
//import Data.Trip;
//import Networks.VehicleArc;
//import Networks.VehicleTypeDepot;
//import Networks.VehicleVertex;
//import Variables.Block;
//import Variables.BlockActivity;
//import Variables.Deadrun;
//import Variables.IdleTime;
//import ilog.concert.IloColumn;
//import ilog.concert.IloException;
//import ilog.concert.IloIntVar;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.concert.IloNumVarType;
//import ilog.concert.IloRange;
//import ilog.cplex.IloCplex;
//import ilog.cplex.IloCplex.UnknownObjectException;
//import lombok.Getter;
//
//@Getter
//public class LagrangianSolver 
//{
//	private List<Trip> trips; 
//	private VehicleTypeDepot vehicleTypeDepot; 
//	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph; 
//	private double maxDistanceWithoutRecharging; 
//	
//	private IloCplex cplex;
//	private Map<VehicleArc, IloIntVar> vehicleArcVariable;
//	private Map<Trip, IloRange> successorTripConstraints;
//	private Map<Trip, IloRange> predecessorTripConstaints; 
//	private Map<VehicleVertex, IloNumVar> accDistanceAtVertex;  
//	private Map<VehicleArc, IloRange> distanceCalConstraints; 
//	//private Map<VehicleArc, IloRange> maxDistWithoutRechargingConstraints;  
//	
//	private Map<VehicleArc, Double> multiplier; 
//	private Map<VehicleArc, Double> subgradient; 
//	private double lagrangianLowerbound;
//	private double upperBound; 
//	private double lowerBound; 
//	private double decayRate; 
//	private int lastImproved; 
//	
//	public LagrangianSolver(List<Trip> trips, VehicleTypeDepot vehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph) throws IloException
//	{
//		this.trips = trips; 
//		this.vehicleTypeDepot = vehicleTypeDepot; 
//		this.vehicleGraph = vehicleGraph; 
//		this.maxDistanceWithoutRecharging = this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(); 
//		
//		this.cplex = new IloCplex();
//		this.cplex.addMinimize(); 
//		this.vehicleArcVariable = new HashMap<VehicleArc, IloIntVar>(); 
//		this.accDistanceAtVertex = new HashMap<VehicleVertex, IloNumVar>(); 
//		this.successorTripConstraints = addSuccessorTripConstraints(); 
//		this.predecessorTripConstaints = addPredecessorTripConstraints();
//		//this.distanceCalConstraints = addDistanceCalculationConstraints(); 
//		//this.maxDistWithoutRechargingConstraints = addMaxDistanceWithoutRechargingConstraints(); 
//		addVehicleArcVariables(); 
//		//addDistanceAccumulationVertex(); 
//		initializeLagrangian(); 
//		 
//		subgradientAlgorithm(); 
//		
//		this.cplex.exportModel("cplex.lp");
//	}
//	
//	private void subgradientAlgorithm() throws IloException
//	{
//		int status = 0; 
//		int iteration = 0; 
//		while(status != 1)
//		{
//			List<VehicleArc> arcsInSolution = solve();
//			
//			calculateSubgradient(arcsInSolution); 
//			
//			updateMultipliers(); 
//			
//			adaptObjectiveFunction(); 
//			
//			boolean stop = checkTermination(iteration);
//			
//			if(stop)
//			{
//				status = 1; 
//				List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
//				for(VehicleArc start : starts)
//				{
//					System.out.println("*************************");
//					boolean stop1 = false; 
//					VehicleArc current = start; 
//					while(!stop1)
//					{
//						for(BlockActivity ba : current.getBlockActivitiesOnEdge())
//						{
//							System.out.println(ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
//						}
//						
//						if(current.getSuccessorVertex().getTrip() != null)
//						{
//							Trip trip = current.getSuccessorVertex().getTrip(); 
//							System.out.println(trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime() + "; " + "Trip" + "; " + trip.getTripId() + ";" + trip.getDistance());
//							current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
//						}
//						else
//						{
//							stop1 = true; 
//						}
//					}
//					
//					
//				}
//			}
//		}
//	}
//	
//	private boolean checkTermination(int iteration)
//	{
//		boolean stop = false; 
//		
//		double gap = ((this.upperBound - this.lagrangianLowerbound)/this.upperBound) * 100.00; 
//		if(gap <= 0.00001)
//		{
//			stop = true; 
//		}
//		
//		if(this.lastImproved > 10)
//		{
//			this.decayRate = this.decayRate/2.0; 
//			this.lastImproved = 0; 
//		}
//		
//		if(this.decayRate < 0.001)
//		{
//			stop = true; 
//		}
//		
//		if(iteration > 500)
//		{
//			stop = true; 
//		}
//		
//		return stop; 
//	}
//	
//	private void calculateSubgradient(List<VehicleArc> arcsInSolution) throws UnknownObjectException, IloException
//	{
//		for(VehicleArc arc : this.subgradient.keySet())
//		{
//			this.subgradient.replace(arc, 0.0); 
//		}
//		
//		Map<VehicleVertex, Double> actualDistanceAccumulatedAtVertex = new HashMap<VehicleVertex, Double>(); 
//		List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
//		for(VehicleArc start : starts)
//		{
//			boolean stop1 = false; 
//			double acc = 0.0; 
//			VehicleArc current = start; 
//			while(!stop1)
//			{	
//				if(current.getSuccessorVertex().getTrip() != null && current.getStartTimeOfReCharging() == -1)
//				{
//					acc = acc + current.getTotalDistance() + current.getSuccessorVertex().getDistance(); 
//					actualDistanceAccumulatedAtVertex.put(current.getSuccessorVertex(), acc); 
//					
//				}
//				else if(current.getSuccessorVertex().getTrip() != null && current.getStartTimeOfReCharging() > -1)
//				{
//					acc = 0.0; 
//					acc = current.getDistanceCoveredAfterReCharging() + current.getSuccessorVertex().getDistance(); 
//					actualDistanceAccumulatedAtVertex.put(current.getSuccessorVertex(), acc);
//				}
//				else if(current.getSuccessorVertex().getTrip() == null)
//				{
//					stop1 = true; 
//				}
//				
//				if(!stop1)
//				{
//					Trip trip = current.getSuccessorVertex().getTrip(); 
//					current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
//				}
//			}	
//		}
//		
//		
//		List<VehicleArc> rechargingArcsInSolution = this.vehicleGraph.edgeSet().stream().filter(e -> e.getSuccessorVertex().getTrip() == null || e.getStartTimeOfReCharging() > -1).collect(Collectors.toList());
//		for(VehicleArc arc : rechargingArcsInSolution)
//		{
//			VehicleVertex predecessorVertex = arc.getPredecessorVertex(); 
//			//double lhs = this.cplex.getValue(this.accDistanceAtVertex.get(predecessorVertex)); 
//			double lhs = actualDistanceAccumulatedAtVertex.get(predecessorVertex); 
//			//System.out.println(predecessorVertex.getTrip().getTripId() + " = " + lhs);
//			if(arcsInSolution.contains(arc))
//			{
//				if(arc.getStartTimeOfReCharging() > -1)
//				{
//					lhs = lhs + arc.getDistancedCoveredBeforeReCharging(); 
//				}
//				else
//				{
//					lhs = lhs + arc.getTotalDistance(); 
//				}
//				//System.out.println(sub); 
//			}
//			double sub =  lhs - this.maxDistanceWithoutRecharging; 
//			this.subgradient.replace(arc, sub);
//		}
//	}
//	
//	private void updateMultipliers()
//	{
//		double denominator = 0.0; 
//		for(VehicleArc arc : this.subgradient.keySet())
//		{
//			denominator = denominator + Math.pow(this.subgradient.get(arc), 2); 
//		}
//		
//		double stepSize = 1; 
//		if(denominator > 0)
//		{
//			stepSize = ((this.upperBound - this.lowerBound)/denominator); 
//		}
//		//System.out.println("Step size = " + stepSize);
//		
//		for(VehicleArc arc : this.multiplier.keySet())
//		{
//			double mult = Math.max(0.0, this.multiplier.get(arc) + (this.decayRate * stepSize * this.subgradient.get(arc))); 
//			this.multiplier.replace(arc, mult); 
//			/*if( mult > 0)
//			{
//				System.out.println(arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId() + " = " + mult);
//			}*/
//		}
//	}
//	
//	private void adaptObjectiveFunction() throws IloException
//	{
//		this.cplex.delete(this.cplex.getObjective());
//		
//		IloLinearNumExpr obj = this.cplex.linearNumExpr(); 
//		
//		for(VehicleArc arc : this.vehicleGraph.edgeSet())
//		{
//			double coef = arc.getTotalDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm(); 
//			if(arc.getPredecessorVertex().getTrip() == null)
//			{
//				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm()); 
//				coef = coef + arc.getVehicleTypeDepot().getVehicleType().getFixedCost(); 
//				
//				obj.addTerm(coef, this.vehicleArcVariable.get(arc));
//				
//			}
//			else if(arc.getSuccessorVertex().getTrip() == null)
//			{
//				coef = coef + (arc.getTotalDistance()*this.multiplier.get(arc)); 
//				obj.addTerm(coef, this.vehicleArcVariable.get(arc));
//			}
//			else if(arc.getStartTimeOfReCharging() > -1)
//			{
//				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm());
//				coef = coef + (arc.getDistancedCoveredBeforeReCharging()*this.multiplier.get(arc)); 
//				obj.addTerm(coef, this.vehicleArcVariable.get(arc));
//			}
//			else
//			{
//				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm());
//				obj.addTerm(coef, this.vehicleArcVariable.get(arc));
//			}
//		}
//		
//		/*for(VehicleVertex tripVertex : this.accDistanceAtVertex.keySet())
//		{
//			double coef = 0.0;
//			List<VehicleArc> predecessorVertices = this.multiplier.keySet().stream().filter(e -> e.getPredecessorVertex().equals(tripVertex)).collect(Collectors.toList()); 
//			for(VehicleArc predecessorVertex : predecessorVertices)
//			{
//				coef = coef + this.multiplier.get(predecessorVertex); 
//			}
//			obj.addTerm(coef, this.accDistanceAtVertex.get(tripVertex));
//		}*/
//		
//		this.cplex.addMinimize(obj); 
//		
//		//this.cplex.exportModel("cplex.lp");
//	}
//	
//	private void initializeLagrangian()
//	{
//		this.upperBound = 13800;
//		this.lagrangianLowerbound = -Double.MAX_VALUE; 
//		this.lowerBound = -Double.MAX_VALUE; 
//		this.decayRate = 1.0;
//		this.lastImproved = 0; 
//		
//		this.multiplier = new HashMap<VehicleArc, Double>(); 
//		this.subgradient = new HashMap<VehicleArc, Double>(); 
//		List<VehicleArc> rechargingArcs = this.vehicleGraph.edgeSet().stream().filter(e -> e.getSuccessorVertex().getTrip() == null || e.getStartTimeOfReCharging() > -1).collect(Collectors.toList());
//		for(VehicleArc arc : rechargingArcs)
//		{
//			this.multiplier.put(arc, 0.0); 
//			this.subgradient.put(arc, 0.0); 
//		}	
//	}
//	
//	/*private Map<VehicleArc, IloRange> addMaxDistanceWithoutRechargingConstraints() throws IloException
//	{
//		Map<VehicleArc, IloRange> maxDistanceWithoutRecharging = new HashMap<VehicleArc, IloRange>(); 
//		
//		List<VehicleArc> rechargingArcs = this.vehicleGraph.edgeSet().stream().filter(v -> v.getSuccessorVertex().getTrip() == null || v.getStartTimeOfReCharging() > -1).collect(Collectors.toList()); 
//		for(VehicleArc arc : rechargingArcs)
//		{
//			maxDistanceWithoutRecharging.put(arc, this.cplex.addRange(-Double.MAX_VALUE, this.maxDistanceWithoutRecharging, "ctMaxDistRecharge_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
//		}
//		return maxDistanceWithoutRecharging; 
//	}*/
//	
//	private Map<VehicleArc, IloRange> addDistanceCalculationConstraints() throws IloException
//	{
//		Map<VehicleArc, IloRange> distanceCalConstraints = new HashMap<VehicleArc, IloRange>(); 
//		
//		for(VehicleArc arc : this.vehicleGraph.edgeSet())
//		{
//			if(arc.getPredecessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
//			{
//				distanceCalConstraints.put(arc, this.cplex.addRange(-this.maxDistanceWithoutRecharging, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
//			}
//			else if(arc.getPredecessorVertex().getTrip() == null)
//			{
//				distanceCalConstraints.put(arc, this.cplex.addRange(-this.maxDistanceWithoutRecharging, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
//			}
//			else if(arc.getStartTimeOfReCharging() > -1)
//			{
//				distanceCalConstraints.put(arc, this.cplex.addRange(-this.maxDistanceWithoutRecharging, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
//			}	
//		}
//		
//		return distanceCalConstraints; 
//	}
//	
//	private Map<Trip, IloRange> addPredecessorTripConstraints() throws IloException
//	{
//		Map<Trip, IloRange> successorTripConstraints = new HashMap<Trip, IloRange>();
//		for(Trip trip : this.trips)
//		{
//			successorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctPredeccor_" + trip.getTripId())); 
//		}
//		
//		return successorTripConstraints; 
//	}
//	
//	private Map<Trip, IloRange> addSuccessorTripConstraints() throws IloException
//	{
//		Map<Trip, IloRange> successorTripConstraints = new HashMap<Trip, IloRange>();
//		for(Trip trip : this.trips)
//		{
//			successorTripConstraints.put(trip, this.cplex.addRange(1, 1, "ctSuccessor_" + trip.getTripId())); 
//		}
//		
//		return successorTripConstraints;
//	}
//
//	private void addVehicleArcVariables() throws IloException
//	{
//		for(VehicleArc arc : this.vehicleGraph.edgeSet())
//		{
//			double coef = arc.getTotalDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm(); 
//			if(arc.getPredecessorVertex().getTrip() == null)
//			{
//				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm()); 
//				coef = coef + arc.getVehicleTypeDepot().getVehicleType().getFixedCost(); 
//				
//				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
//				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
//				
//				double totalDistance = arc.getTotalDistance() + arc.getSuccessorVertex().getDistance() + this.maxDistanceWithoutRecharging;
//				//arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
//				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1)); 
//			}
//			else if(arc.getSuccessorVertex().getTrip() == null)
//			{
//				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
//				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
//				//arcVariable = arcVariable.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(arc), arc.getTotalDistance())); 
//				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1));
//			}
//			else
//			{
//				coef = coef + (arc.getSuccessorVertex().getDistance() * arc.getVehicleTypeDepot().getVehicleType().getCostPerkm());
//				IloColumn arcVariable = this.cplex.column(this.cplex.getObjective(), coef);
//				arcVariable = arcVariable.and(this.cplex.column(this.predecessorTripConstaints.get(arc.getSuccessorVertex().getTrip()), 1)); 
//				arcVariable = arcVariable.and(this.cplex.column(this.successorTripConstraints.get(arc.getPredecessorVertex().getTrip()), 1));
//				if(arc.getStartTimeOfReCharging() > -1)
//				{
//					double totalDistance = arc.getDistanceCoveredAfterReCharging() + arc.getSuccessorVertex().getDistance() + this.maxDistanceWithoutRecharging;
//					//arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
//					//arcVariable = arcVariable.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(arc), arc.getDistancedCoveredBeforeReCharging())); 
//				}
//				else
//				{
//					double totalDistance = arc.getTotalDistance() + arc.getSuccessorVertex().getDistance() + this.maxDistanceWithoutRecharging;
//					//arcVariable = arcVariable.and(this.cplex.column(this.distanceCalConstraints.get(arc), -totalDistance)); 
//				}
//				
//				this.vehicleArcVariable.put(arc, this.cplex.intVar(arcVariable, 0, 1)); 
//			}
//			
//			
//		}
//	}
//	
//	private void addDistanceAccumulationVertex() throws IloException
//	{
//		List<VehicleVertex> tripVertices = this.vehicleGraph.vertexSet().stream().filter(v -> v.getTrip()!= null).collect(Collectors.toList()); 
//		
//		for(VehicleVertex tripVertex : tripVertices)
//		{
//			IloColumn vertesDistVar  = this.cplex.column(this.cplex.getObjective(), 0); 
//			
//			Set<VehicleArc> outgoingEdges = this.vehicleGraph.outgoingEdgesOf(tripVertex); 
//			
//			for(VehicleArc outgoingArc : outgoingEdges)
//			{
//				if(outgoingArc.getSuccessorVertex().getTrip() != null && outgoingArc.getStartTimeOfReCharging() == -1)
//				{
//					vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalConstraints.get(outgoingArc), -1)); 
//				}
//				/*else
//				{
//					vertesDistVar = vertesDistVar.and(this.cplex.column(this.maxDistWithoutRechargingConstraints.get(outgoingArc), 1));
//				}*/
//					
//			}
//			
//			Set<VehicleArc> incomingEdges = this.vehicleGraph.incomingEdgesOf(tripVertex); 
//			
//			for(VehicleArc incomingArc : incomingEdges)
//			{
//				vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalConstraints.get(incomingArc), 1)); 
//			}
//			
//			this.accDistanceAtVertex.put(tripVertex, this.cplex.numVar(vertesDistVar, 0, Double.MAX_VALUE, "dist_" + tripVertex.getVertexId())); 
//		}
//	}
//	
//	private List<VehicleArc> solve() throws IloException
//	{
//		//this.cplex.exportModel("qap.lp");
//		List<VehicleArc> arcsInSolution = new ArrayList<VehicleArc>(); 
//		this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
//		this.cplex.setOut(null);
//		if(this.cplex.solve())
//		{
//			//System.out.println(this.cplex.getObjValue());
//			this.lowerBound = this.cplex.getObjValue(); 
//			for(VehicleArc arc : this.multiplier.keySet())
//			{
//				this.lowerBound = this.lowerBound - (this.maxDistanceWithoutRecharging*this.multiplier.get(arc)); 
//			}
//			System.out.println("Lower bound = " + this.lowerBound);
//			if(this.lowerBound > this.lagrangianLowerbound)
//			{
//				this.lagrangianLowerbound = this.lowerBound; 
//				this.lastImproved =0; 
//			}
//			else
//			{
//				this.lastImproved++; 
//			}
//			
//			for(VehicleArc arc : this.vehicleArcVariable.keySet())
//			{
//				double value = this.cplex.getValue(this.vehicleArcVariable.get(arc));
//				
//				if(value >= 1-1e-3)
//				{
//					//System.out.println(arc.getPredecessorVertex().getVertexId() + " -------> " + arc.getSuccessorVertex().getVertexId() + " Value = " + value);
//					/*arc.getBlockActivitiesOnEdge().forEach(b -> {
//						System.out.println(b.getActivity() + ";" + b.getDistance());
//					});*/
//					arcsInSolution.add(arc); 
//				}
//			}
//			/*List<VehicleArc> starts = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList()); 
//			for(VehicleArc start : starts)
//			{
//				System.out.println("*************************");
//				boolean stop = false; 
//				VehicleArc current = start; 
//				while(!stop)
//				{
//					for(BlockActivity ba : current.getBlockActivitiesOnEdge())
//					{
//						System.out.println(ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
//					}
//					
//					if(current.getSuccessorVertex().getTrip() != null)
//					{
//						Trip trip = current.getSuccessorVertex().getTrip(); 
//						System.out.println(trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime() + "; " + "Trip" + "; " + trip.getTripId() + ";" + trip.getDistance());
//						current = arcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() != null && a.getPredecessorVertex().getTrip().equals(trip)).findFirst().get(); 
//					}
//					else
//					{
//						stop = true; 
//					}
//				}
//				
//				
//			}
//			
//			System.out.println("*************************");
//			
//			/*for(VehicleVertex tripVertex : this.accDistanceAtVertex.keySet())
//			{
//				System.out.println(tripVertex.getTrip().getTripId() + ", Distance = " + this.cplex.getValue(this.accDistanceAtVertex.get(tripVertex)));
//			}*/
//		}
//		
//		return arcsInSolution; 
//	}
//}
