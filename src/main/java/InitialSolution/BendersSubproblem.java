package InitialSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import lombok.Getter;

public class BendersSubproblem 
{
	private List<VehicleArc> vehicleArcsInSolution; 
	private VehicleTypeDepot vehicleTypeDepot; 
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph;
	private double maxDistanceWithoutRecharging; 
	
	private IloCplex cplex; 
	private Map<VehicleVertex, IloNumVar> accDistanceAtVertexVariables; 
	private Map<VehicleArc, IloRange> distanceCalculationConstraints; 
	private Map<VehicleArc,IloRange> maxDistanceConstraints; 
	
	@Getter
	private boolean feasible; 
	@Getter
	private Map<VehicleArc, Double> distCalVehicleArcDuals; 
	@Getter
	private Map<VehicleArc, Double> maxDistanceDuals;
	@Getter
	private List<List<VehicleArc>> violatedSequences; 
	public BendersSubproblem(DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph, List<VehicleArc> vehicleArcsInSolution, VehicleTypeDepot vehicleTypeDepot) throws IloException
	{
		this.vehicleGraph = vehicleGraph; 
		this.vehicleArcsInSolution = vehicleArcsInSolution; 
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.maxDistanceWithoutRecharging = this.vehicleTypeDepot.getVehicleType().getMaximumDistanceWithoutRecharging(); 
		
		this.violatedSequences = new ArrayList<List<VehicleArc>>(); 
		getListOfViolatedSequences();  
		/*this.cplex = new IloCplex(); 
		this.cplex.addMinimize(); 
		this.distanceCalculationConstraints = addDistanceCalculationConstraints(); 
		this.maxDistanceConstraints = addMaxDistanceConstraints();
		this.accDistanceAtVertexVariables = new HashMap<VehicleVertex, IloNumVar>(); 
		addDistanceAtVertexVariables();
		
		this.feasible = true;
		this.distCalVehicleArcDuals = new HashMap<VehicleArc, Double>(); 
		this.maxDistanceDuals = new HashMap<VehicleArc, Double>(); 
		
		solve(); */
	}
	
	private void getListOfViolatedSequences()
	{
		
		List<VehicleArc> starts = this.vehicleArcsInSolution.stream().filter(a -> a.getPredecessorVertex().getTrip() == null).collect(Collectors.toList());
		for(VehicleArc start : starts)
		{
			boolean stop = false; 
			double acc = 0; 
			List<VehicleArc> violatedSequence = new ArrayList<VehicleArc>();
			VehicleArc current = start; 
			while(!stop)
			{
				if(current.getSuccessorVertex().getTrip() != null && current.getStartTimeOfReCharging() == -1)
				{
					acc = acc + current.getTotalDistance() + current.getSuccessorVertex().getDistance(); 
					violatedSequence.add(current); 
					
					if(acc > this.maxDistanceWithoutRecharging)
					{
						Assert.assertTrue(!violatedSequence.isEmpty());
						this.violatedSequences.add(violatedSequence); 
						
						acc = 0; 
						violatedSequence= new ArrayList<VehicleArc>();
					}
					
				}
				else if(current.getSuccessorVertex().getTrip() == null)
				{
					acc = acc + current.getTotalDistance(); 
					
					if(acc > this.maxDistanceWithoutRecharging)
					{
						Assert.assertTrue(!violatedSequence.isEmpty());
						this.violatedSequences.add(violatedSequence); 
					} 
				}
				else if(current.getStartTimeOfReCharging() > -1)
				{
					acc = acc + current.getDistancedCoveredBeforeReCharging(); 
					
					if(acc > this.maxDistanceWithoutRecharging)
					{
						Assert.assertTrue(!violatedSequence.isEmpty());
						this.violatedSequences.add(violatedSequence); 
						System.out.println("Before recharging");
						violatedSequence.forEach(a -> {
							System.out.print(a.getPredecessorVertex().getVertexId() + "_" + a.getSuccessorVertex().getVertexId() + ", ");
						});
						System.out.println();
					}
					
					acc = 0; 
					violatedSequence= new ArrayList<VehicleArc>();
					acc = current.getDistanceCoveredAfterReCharging() + current.getSuccessorVertex().getDistance(); 
					//violatedSequence.add(current);
				}
				
				VehicleVertex currentVertex = current.getSuccessorVertex(); 
				if(currentVertex.getTrip() != null)
				{
					current = this.vehicleArcsInSolution.stream().filter(a -> a.getPredecessorVertex().equals(currentVertex)).findFirst().get(); 
				}
				else
				{
					stop = true;
				}
			}
		}
	}
	
	private void solve() throws IloException
	{
		this.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
		if(this.cplex.solve())
		{
			System.out.println("Solution feasible");
			
			for(VehicleVertex vertex : this.accDistanceAtVertexVariables.keySet())
			{
				System.out.println(vertex.getVertexId() + ", value = " + this.cplex.getValue(this.accDistanceAtVertexVariables.get(vertex)));
			}
		}
		else
		{
			this.feasible = false; 
			int size = this.distanceCalculationConstraints.size() + this.maxDistanceConstraints.size(); 
			IloRange[] farkasConstraints = new IloRange[size];
			double[] farkasValues = new double[size];
			
			this.cplex.dualFarkas(farkasConstraints, farkasValues); 
			
			for(int i = 0; i < size; i++)
			{
				for(VehicleArc arc : this.distanceCalculationConstraints.keySet())
				{
					if(this.distanceCalculationConstraints.get(arc).equals(farkasConstraints[i]))
					{
						this.distCalVehicleArcDuals.put(arc, farkasValues[i]); 
						break; 
					}
				}
				
				for(VehicleArc arc : this.maxDistanceConstraints.keySet())
				{
					if(this.maxDistanceConstraints.get(arc).equals(farkasConstraints[i]))
					{
						this.maxDistanceDuals.put(arc, farkasValues[i]); 
						break; 
					}
				}
			}
		}
		
	}
	
	private Map<VehicleArc, IloRange> addDistanceCalculationConstraints() throws IloException
	{
		Map<VehicleArc, IloRange> distanceCalConstraints = new HashMap<VehicleArc, IloRange>(); 
		
		
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			if(this.vehicleArcsInSolution.contains(arc))
			{
				double distance = 0; 
				if(arc.getPredecessorVertex().getTrip() != null && arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
				{
					distance  = distance + arc.getTotalDistance() + arc.getSuccessorVertex().getDistance(); 
					distanceCalConstraints.put(arc, this.cplex.addRange(distance, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
				}
				else if(arc.getPredecessorVertex().getTrip() == null)
				{
					distance = distance + arc.getTotalDistance() + arc.getSuccessorVertex().getDistance(); 
					distanceCalConstraints.put(arc, this.cplex.addRange(distance, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
				}
				else if(arc.getStartTimeOfReCharging() > -1)
				{
					distance  = distance + arc.getDistanceCoveredAfterReCharging() + arc.getSuccessorVertex().getDistance(); 
					distanceCalConstraints.put(arc, this.cplex.addRange(distance, Double.MAX_VALUE, "ctDistCal_" + arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
				}
			}
			else
			{
				if(arc.getSuccessorVertex().getTrip() != null && arc.getStartTimeOfReCharging() == -1)
				{
					distanceCalConstraints.put(arc, this.cplex.addRange(-maxDistanceWithoutRecharging, Double.MAX_VALUE, arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
				}
				else if(arc.getStartTimeOfReCharging() > -1)
				{
					distanceCalConstraints.put(arc, this.cplex.addRange(-maxDistanceWithoutRecharging, Double.MAX_VALUE, arc.getPredecessorVertex().getTrip().getTripId() + "_" + arc.getSuccessorVertex().getTrip().getTripId())); 
				}
			}
		}
		
		return distanceCalConstraints; 
	}
	
	private Map<VehicleArc, IloRange> addMaxDistanceConstraints() throws IloException
	{
		Map<VehicleArc, IloRange> maxDistanceConstraints = new HashMap<VehicleArc, IloRange>(); 
		
		for(VehicleArc arc : this.vehicleGraph.edgeSet())
		{
			double coef = this.maxDistanceWithoutRecharging; 
			
			if(this.vehicleArcsInSolution.contains(arc))
			{
				if(arc.getStartTimeOfReCharging() > -1)
				{
					coef = coef - arc.getDistancedCoveredBeforeReCharging(); 
					maxDistanceConstraints.put(arc, this.cplex.addRange(-Double.MAX_VALUE, coef, "ctMaxDist_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
				}
				else if(arc.getSuccessorVertex().getTrip() == null)
				{
					coef = coef - arc.getTotalDistance(); 
					maxDistanceConstraints.put(arc, this.cplex.addRange(-Double.MAX_VALUE, coef, "ctMaxDist_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
				}
			}
			else
			{
				if(arc.getStartTimeOfReCharging() > -1)
				{ 
					maxDistanceConstraints.put(arc, this.cplex.addRange(-Double.MAX_VALUE, coef, "ctMaxDist_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
				}
				else if(arc.getSuccessorVertex().getTrip() == null)
				{
					maxDistanceConstraints.put(arc, this.cplex.addRange(-Double.MAX_VALUE, coef, "ctMaxDist_" + arc.getPredecessorVertex().getVertexId() + "_" + arc.getSuccessorVertex().getVertexId())); 
				}
			}
			
		}
		
		return maxDistanceConstraints; 
	}
	
	private void addDistanceAtVertexVariables() throws IloException
	{
		Set<VehicleVertex> tripVertices = this.vehicleGraph.vertexSet().stream().filter(v -> v.getTrip() != null).collect(Collectors.toSet());  
		
		for(VehicleVertex tripVertex : tripVertices)
		{
			IloColumn vertesDistVar  = this.cplex.column(this.cplex.getObjective(), 0); 
			
			Set<VehicleArc> outgoingArcs = this.vehicleGraph.edgeSet().stream().filter(a -> a.getPredecessorVertex().equals(tripVertex)).collect(Collectors.toSet());
			
			for(VehicleArc outgoingArc : outgoingArcs)
			{
				if(outgoingArc.getSuccessorVertex().getTrip() != null && outgoingArc.getStartTimeOfReCharging() == -1)
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalculationConstraints.get(outgoingArc), -1)); 
				}
				else if(outgoingArc.getSuccessorVertex().getTrip() == null)
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.maxDistanceConstraints.get(outgoingArc), 1)); 
				}
				else if(outgoingArc.getStartTimeOfReCharging() > -1)
				{
					vertesDistVar = vertesDistVar.and(this.cplex.column(this.maxDistanceConstraints.get(outgoingArc), 1)); 
				}
			}
			
			
			Set<VehicleArc> incomingArcs = this.vehicleGraph.edgeSet().stream().filter(a -> a.getSuccessorVertex().equals(tripVertex)).collect(Collectors.toSet());
			
			for(VehicleArc incomingArc : incomingArcs)
			{
				vertesDistVar = vertesDistVar.and(this.cplex.column(this.distanceCalculationConstraints.get(incomingArc), 1)); 
			}
			
			
			this.accDistanceAtVertexVariables.put(tripVertex, this.cplex.numVar(vertesDistVar, 0, Double.MAX_VALUE, "dist_" + tripVertex.getVertexId())); 
		}
	}

}
