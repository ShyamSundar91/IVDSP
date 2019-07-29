package Networks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Subproblems.DriverRCSPP;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

public class DriverGraphGeneration 
{
	private Set<DutyTypeDepot> dutyTypeDepots; 
	private List<DriverTravel> allDriverTravels; 
	private Set<Node> allNodes; 
	private List<Trip> allTrips; 
	private Set<Deadrun> allDeadRuns; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	@Getter
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs; 
	@Getter
	private List<Duty> dutiesGenerated; 
	
	public DriverGraphGeneration(Set<DutyTypeDepot> dutyTypeDepots, List<DriverTravel> allDriverTravels, Set<Node> allNodes, List<Trip> allTrips, Set<Deadrun> allDeadRuns, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs)
	{
		this.dutyTypeDepots = dutyTypeDepots; 
		this.allDriverTravels = allDriverTravels; 
		this.allNodes = allNodes; 
		this.allTrips = allTrips; 
		this.allDeadRuns = allDeadRuns; 
		this.vehicleGraphs = vehicleGraphs; 
		
		this.driverGraphs = new HashMap<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>>(); 
		generateGraphs(); 
	}
	
	private void generateGraphs()
	{
		for(DutyTypeDepot dutyTypeDepot : this.dutyTypeDepots)
		{
			DriverGraph graph = new DriverGraph(dutyTypeDepot, this.allDriverTravels, this.allNodes, this.vehicleGraphs, this.allTrips, this.allDeadRuns); 
			this.driverGraphs.put(dutyTypeDepot, graph.getDriverGraph()); 
			System.out.println("Driver Graph " + dutyTypeDepot.getDescription() + ", vertices " + graph.getDriverGraph().vertexSet().size() + ", edges " + graph.getDriverGraph().edgeSet().size());
			/*for(DriverArc arc : graph.getDriverGraph().edgeSet())
			{
				if(arc.getPredecessorVertex().getCurrentNode() == null)
				{
					System.out.println(-1 + ", " + arc.getPredecessorVertex().getCurrentTime() + " -----> " + arc.getSuccessorVertex().getCurrentNode().getNodeId() + ", " + arc.getSuccessorVertex().getCurrentTime() + "; " + arc.isAttendingBus());
				}
				else if(arc.getSuccessorVertex().getCurrentNode() == null)
				{
					System.out.println(arc.getPredecessorVertex().getCurrentNode().getNodeId() + ", " + arc.getPredecessorVertex().getCurrentTime() + " -----> " + Integer.MAX_VALUE + ", " + arc.getSuccessorVertex().getCurrentTime() + "; " + arc.isAttendingBus());
				}
				else
				{
					System.out.println(arc.getPredecessorVertex().getCurrentNode().getNodeId() + ", " + arc.getPredecessorVertex().getCurrentTime() + " -----> " + arc.getSuccessorVertex().getCurrentNode().getNodeId() + ", " + arc.getSuccessorVertex().getCurrentTime() + "; " + arc.isAttendingBus());
					if(arc.getPredecessorVertex().getDeadrun() != null)
					{
						System.out.print("Deadrun ----->");
					}
					else if(arc.getPredecessorVertex().getTrip() != null)
					{
						System.out.print("Trip ------>");
					}
					
					if(arc.getSuccessorVertex().getDeadrun() != null)
					{
						System.out.print(" Deadrun");
					}
					else if(arc.getSuccessorVertex().getTrip() != null)
					{
						System.out.print(" Trip");
					}
					
					System.out.println();
				}
				
				for(DutyActivity dutyActivity : arc.getDutyActivities())
				{
					System.out.println(dutyActivity.getDepartureNode().getNodeId() + "; " + dutyActivity.getArrivalNode().getNodeId() + "; " + dutyActivity.getDepartureTime() + "; " + dutyActivity.getArrivalTime() + "; " + dutyActivity.getTripOrDeadrunId() + "; " + dutyActivity.getActivity());
				}
				System.out.println("********************");
			}*/
		
			/*DriverRCSPP rcspp = new DriverRCSPP(dutyTypeDepot.getDutyType(), graph.getDriverGraph(), new HashMap<Trip, Double>(), new HashMap<Deadrun, Double>(), new HashMap<Deadrun, Double>(), new HashMap<IdleTime, Double>(), new ArrayList<Trip>(), new ArrayList<Deadrun>(), new ArrayList<IdleTime>(), true, false); 
			System.out.println("Number of duties generated = " + rcspp.getDutiesGenerated().size());
			dutiesGenerated = rcspp.getDutiesGenerated(); 
			/*for(Duty duty : dutiesGenerated)
			{
				if(!duty.getTripsInDuty().isEmpty() || !duty.getDeadrunsInDuty().isEmpty())
				{
					for(DutyActivity da : duty.getDutyActivities())
					{
						System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
					}
					
				}
				
				
			}*/
		}
	}

}
