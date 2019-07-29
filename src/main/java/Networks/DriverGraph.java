package Networks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Networks.DriverUtil.ActivitiesWhileChangingBus;
import Networks.DriverUtil.ActivtiesWhileAttendingBus;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class DriverGraph 
{
	private DutyTypeDepot dutyTypeDepot; 
	private List<DriverTravel> allDriverTravels; 
	private Set<Node> allNodes; 
	private List<Trip> allTrips; 
	private Set<Deadrun> allDeadRuns; 
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	private DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph; 
	private DriverVertex sourceVertex; 
	private DriverVertex sinkVertex; 
	private Set<DriverVertex> driverVertices; 
	private Set<DriverArc> driverArcs; 
	private Map<Trip, DriverArc> tripArcs; 
	private Map<Deadrun, DriverArc> deadrunArcs; 
	
	public DriverGraph(DutyTypeDepot dutyTypeDepot, List<DriverTravel> allDriverTravels, Set<Node> allNodes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, List<Trip> allTrips, Set<Deadrun> allDeadRuns)
	{
		this.dutyTypeDepot = dutyTypeDepot; 
		this.allDriverTravels = allDriverTravels; 
		this.allNodes = allNodes; 
		this.vehicleGraphs = vehicleGraphs;
		this.allTrips = allTrips; 
		this.allDeadRuns = allDeadRuns; 
		
		this.driverGraph = new DefaultDirectedGraph<DriverVertex, DriverArc>(DriverArc.class); 
		this.driverVertices = new HashSet<DriverVertex>(); 
		this.driverArcs = new HashSet<DriverArc>(); 
		this.tripArcs = new HashMap<Trip, DriverArc>(); 
		this.deadrunArcs = new HashMap<Deadrun, DriverArc>(); 
		
		addSourceSink(); 
		addTripAndDeadrunArcs(); 
		createGraph(); 
		addArcsFromSource(); 
		addArcsToSink(); 
		addArcsBetweenBusChange(); 
	}
	
	private void addSourceSink()
	{
		sourceVertex = new DriverVertex(null, -1, null, null, false); 
		this.driverGraph.addVertex(sourceVertex); 
		
		sinkVertex = new DriverVertex(null, Integer.MAX_VALUE, null, null, false); 
		this.driverGraph.addVertex(sinkVertex); 
	}
	
	private void addTripAndDeadrunArcs()
	{
		for(Trip trip : this.allTrips)
		{
			DriverVertex startOfTrip = new DriverVertex(trip.getDepartureNode(), trip.getDepartureTime(), trip, null, true); 
			DriverVertex endOfTrip = new DriverVertex(trip.getArrivalNode(), trip.getArrivalTime(), trip, null, false); 
			this.driverGraph.addVertex(startOfTrip); 
			this.driverGraph.addVertex(endOfTrip);
			this.driverVertices.add(startOfTrip); 
			this.driverVertices.add(endOfTrip); 
			
			List<DutyActivity> dutyActivites = new ArrayList<DutyActivity>(); 
			DutyActivity da = new DutyActivity(trip.getDepartureNode(), trip.getArrivalNode(), trip.getDepartureTime(), trip.getArrivalTime(), trip.getTripId(), "Trip"); 
			dutyActivites.add(da); 
			DriverArc tripArc = new DriverArc(this.dutyTypeDepot.getDutyType(), startOfTrip, endOfTrip, trip, null, dutyActivites, null, true, false); 
			this.driverGraph.addEdge(startOfTrip, endOfTrip, tripArc); 
			this.tripArcs.put(trip, tripArc); 
		}
		
		for(Deadrun deadrun : this.allDeadRuns)
		{
			DriverVertex startOfDeadrun = new DriverVertex(deadrun.getDepartureNode(), deadrun.getDepartureTime(), null, deadrun, true);
			DriverVertex endOfDeadrun = new DriverVertex(deadrun.getArrivalNode(), deadrun.getArrivalTime(), null, deadrun, false); 
			this.driverGraph.addVertex(startOfDeadrun); 
			this.driverGraph.addVertex(endOfDeadrun); 
			this.driverVertices.add(startOfDeadrun); 
			this.driverVertices.add(endOfDeadrun); 
			
			List<DutyActivity> dutyActivites = new ArrayList<DutyActivity>(); 
			DutyActivity da = new DutyActivity(deadrun.getDepartureNode(), deadrun.getArrivalNode(), deadrun.getDepartureTime(), deadrun.getArrivalTime(), deadrun.getDeadrunId(), deadrun.getType()); 
			dutyActivites.add(da);
			DriverArc deadrunArc = new DriverArc(this.dutyTypeDepot.getDutyType(), startOfDeadrun, endOfDeadrun, null,deadrun, dutyActivites, null, true, false); 
			this.driverGraph.addEdge(startOfDeadrun, endOfDeadrun, deadrunArc); 
			this.deadrunArcs.put(deadrun, deadrunArc); 
		}
	}
	
	private void createGraph()
	{
		for(VehicleTypeDepot vehicleTypeDepot : this.vehicleGraphs.keySet())
		{
			for(VehicleArc vehicleArc : this.vehicleGraphs.get(vehicleTypeDepot).edgeSet())
			{
				List<BlockActivity> blockActivitites = vehicleArc.getBlockActivitiesOnEdge(); 
				if(!blockActivitites.isEmpty())
				{
					List<DriverArc> driverArcsToConnect = new ArrayList<DriverArc>(); 
					if(vehicleArc.getPredecessorVertex().getTrip() != null)
					{
						driverArcsToConnect.add(this.tripArcs.get(vehicleArc.getPredecessorVertex().getTrip())); 
						//System.out.println("Trip = " + vehicleArc.getPredecessorVertex().getTrip().getTripId() + "; " + vehicleArc.getPredecessorVertex().getTrip().getDepartureNode().getNodeId() + "; " + vehicleArc.getPredecessorVertex().getTrip().getArrivalNode().getNodeId() + "; " + vehicleArc.getPredecessorVertex().getTrip().getDepartureTime() + "; " + vehicleArc.getPredecessorVertex().getTrip().getArrivalTime());
					}
					
					List<Deadrun> deadruns = vehicleArc.getDeadrunsOnEdge(); 
		
					if(!deadruns.isEmpty())
					{
						for(Deadrun deadrun : deadruns)
						{
							driverArcsToConnect.add(this.deadrunArcs.get(deadrun));
							//System.out.println("Deadrun = " + deadrun.getDepartureNode().getNodeId() + "; " + deadrun.getArrivalNode().getNodeId() + "; " + deadrun.getDepartureTime() + "; " + deadrun.getArrivalTime());
						}
					}
					
					if(vehicleArc.getSuccessorVertex().getTrip() != null)
					{
						driverArcsToConnect.add(this.tripArcs.get(vehicleArc.getSuccessorVertex().getTrip())); 
						//System.out.println("Trip = " + vehicleArc.getSuccessorVertex().getTrip().getTripId() + "; " + vehicleArc.getSuccessorVertex().getTrip().getDepartureNode().getNodeId() + "; " + vehicleArc.getSuccessorVertex().getTrip().getArrivalNode().getNodeId() + "; " + vehicleArc.getSuccessorVertex().getTrip().getDepartureTime() + "; " + vehicleArc.getSuccessorVertex().getTrip().getArrivalTime());
					}
					
					Collections.sort(driverArcsToConnect);
					
					for(int i = 0; i < driverArcsToConnect.size() -1; i++)
					{
						DriverVertex predecessorVertex = driverArcsToConnect.get(i).getSuccessorVertex(); 
						DriverVertex successorVeretx = driverArcsToConnect.get(i+1).getPredecessorVertex(); 
						//System.out.println(predecessorVertex.getCurrentNode().getNodeId() + ", " + successorVeretx .getCurrentNode().getNodeId());
						Assert.assertTrue(predecessorVertex.getCurrentNode().equals(successorVeretx.getCurrentNode()));
						ActivtiesWhileAttendingBus activities = new ActivtiesWhileAttendingBus(predecessorVertex.getCurrentNode(), predecessorVertex.getCurrentTime(), successorVeretx.getCurrentTime(), this.dutyTypeDepot.getDutyType(), blockActivitites); 
						if(activities.isAddArc())
						{
							List<DutyActivity> dutyActivites = activities.getDutyActivities(); 
							IdleTime idleTimeOnArc = null; 
							if(activities.getBlockActivity() != null && activities.getBlockActivity().getActivity().equals("Idle"))
							{
								idleTimeOnArc = vehicleArc.getIdleTimeOnArc(); 
								Assert.assertTrue(idleTimeOnArc.getNode().equals(activities.getBlockActivity().getDepartureNode()) && idleTimeOnArc.getNode().equals(activities.getBlockActivity().getArrivalNode()) && idleTimeOnArc.getDepartureTime() == activities.getBlockActivity().getDepartureTime() && idleTimeOnArc.getArrivalTime() == activities.getBlockActivity().getArrivalTime());
							}
							
							
							DriverArc driverArc = new DriverArc(this.dutyTypeDepot.getDutyType(), predecessorVertex, successorVeretx , null, null, dutyActivites, idleTimeOnArc, true, false); 
							if(!this.driverArcs.contains(driverArc))
							{
								this.driverArcs.add(driverArc); 
								this.driverGraph.addEdge(predecessorVertex, successorVeretx, driverArc); 
							}
							else
							{
								 throw new IllegalArgumentException();
							}
						}
					}
				}
			}
		}
	}
	
	private void addArcsFromSource()
	{
		//Add arcs to end of all trips
		for(Trip trip : this.tripArcs.keySet())
		{
			DriverArc tripArc = this.tripArcs.get(trip); 
			
			DriverVertex endOfTrip = tripArc.getSuccessorVertex(); 
			Assert.assertTrue(!endOfTrip.isDeparture());
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			if(endOfTrip.getCurrentNode().isDriverChangeAllowed() && endOfTrip.getCurrentNode().equals(this.dutyTypeDepot.getDutySignOn()))
			{
				DutyActivity dutySignOn = new DutyActivity(endOfTrip.getCurrentNode(), endOfTrip.getCurrentNode(), endOfTrip.getCurrentTime(), endOfTrip.getCurrentTime(), -1, "Duty sign-on"); 
				dutyActivities.add(dutySignOn); 
				
				DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, endOfTrip, null, null, dutyActivities, null, false, false); 
				if(!this.driverArcs.contains(dutySignOnArc))
				{
					this.driverArcs.add(dutySignOnArc); 
					this.driverGraph.addEdge(this.sourceVertex, endOfTrip, dutySignOnArc); 
				}
				else
				{
					 throw new IllegalArgumentException();
				}
			}
			else if(endOfTrip.getCurrentNode().isDriverChangeAllowed())
			{
				Optional<DriverTravel> travelToTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(this.dutyTypeDepot.getDutySignOn()) && t.getArrivalNode().equals(endOfTrip.getCurrentNode())).findFirst(); 
				if(travelToTrip.isPresent())
				{
					int duration = travelToTrip.get().getDuration(); 
					int startTime = endOfTrip.getCurrentTime() - duration; 
					
					DutyActivity travelToTripActivity = new DutyActivity(travelToTrip.get().getDepartureNode(), travelToTrip.get().getArrivalNode(), startTime, endOfTrip.getCurrentTime(), -1, travelToTrip.get().getTravelType()); 
					dutyActivities.add(travelToTripActivity); 
					DutyActivity dutySignOn = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), startTime, startTime, -1, "Duty sign-on"); 
					dutyActivities.add(dutySignOn); 
					Collections.sort(dutyActivities);
					DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, endOfTrip, null, null, dutyActivities, null, false, false); 
					if(!this.driverArcs.contains(dutySignOnArc))
					{
						this.driverArcs.add(dutySignOnArc); 
						this.driverGraph.addEdge(this.sourceVertex, endOfTrip, dutySignOnArc); 
					}
					else
					{
						 throw new IllegalArgumentException();
					}
				}
			}
		}
		
		//Add arcs to start of pull-out deadruns or after parking or recharging. Assuming that a block always starts from the depot with a deadrun
		List<Deadrun> pullOutDeadruns = this.allDeadRuns.stream().filter(d -> d.isPullOut()).collect(Collectors.toList()); 
		for(Deadrun deadrun : pullOutDeadruns)
		{
			DriverArc deadrunArc = this.deadrunArcs.get(deadrun); 
			
			DriverVertex startOfDeadrun = deadrunArc.getPredecessorVertex(); 
			Assert.assertTrue(startOfDeadrun.isDeparture());
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			if(startOfDeadrun.getCurrentNode().isDriverChangeAllowed() && startOfDeadrun.getCurrentNode().equals(this.dutyTypeDepot.getDutySignOn()))
			{
				DutyActivity dutySignOn = new DutyActivity(startOfDeadrun.getCurrentNode(), startOfDeadrun.getCurrentNode(), startOfDeadrun.getCurrentTime(), startOfDeadrun.getCurrentTime(), -1, "Duty sign-on"); 
				dutyActivities.add(dutySignOn); 
				
				DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, startOfDeadrun, null, null, dutyActivities, null, false, false); 
				if(!this.driverArcs.contains(dutySignOnArc))
				{
					this.driverArcs.add(dutySignOnArc); 
					this.driverGraph.addEdge(this.sourceVertex, startOfDeadrun, dutySignOnArc); 
				}
				else
				{
					 throw new IllegalArgumentException();
				}
			}
			else if(startOfDeadrun.getCurrentNode().isDriverChangeAllowed())
			{
				Optional<DriverTravel> travelToDeadrun = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(this.dutyTypeDepot.getDutySignOn()) && t.getArrivalNode().equals(startOfDeadrun.getCurrentNode())).findFirst(); 
				if(travelToDeadrun.isPresent())
				{
					int duration = travelToDeadrun.get().getDuration(); 
					int startTime = startOfDeadrun.getCurrentTime() - duration; 
					
					DutyActivity travelToTripActivity = new DutyActivity(travelToDeadrun.get().getDepartureNode(), travelToDeadrun.get().getArrivalNode(), startTime, startOfDeadrun.getCurrentTime(), -1, travelToDeadrun.get().getTravelType()); 
					dutyActivities.add(travelToTripActivity); 
					DutyActivity dutySignOn = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), startTime, startTime, -1, "Duty sign-on"); 
					dutyActivities.add(dutySignOn); 
					Collections.sort(dutyActivities);
					DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, startOfDeadrun, null, null, dutyActivities, null, false, false); 
					if(!this.driverArcs.contains(dutySignOnArc))
					{
						this.driverArcs.add(dutySignOnArc); 
						this.driverGraph.addEdge(this.sourceVertex, startOfDeadrun, dutySignOnArc); 
					}
					else
					{
						 throw new IllegalArgumentException();
					}
				}
			}
			
		}
		
		//Add arcs to end of all deadruns except pull-in
		List<Deadrun> allDeadRunsExceptPullIn = this.allDeadRuns.stream().filter(d -> !d.isPullIn()).collect(Collectors.toList()); 
		for(Deadrun deadrun : allDeadRunsExceptPullIn)
		{
			DriverArc deadrunArc = this.deadrunArcs.get(deadrun); 
			
			DriverVertex endOfDeadrun = deadrunArc.getSuccessorVertex(); 
			Assert.assertTrue(!endOfDeadrun.isDeparture());
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			if(endOfDeadrun.getCurrentNode().isDriverChangeAllowed() && endOfDeadrun.getCurrentNode().equals(this.dutyTypeDepot.getDutySignOn()))
			{
				DutyActivity dutySignOn = new DutyActivity(endOfDeadrun.getCurrentNode(), endOfDeadrun.getCurrentNode(), endOfDeadrun.getCurrentTime(), endOfDeadrun.getCurrentTime(), -1, "Duty sign-on"); 
				dutyActivities.add(dutySignOn); 
				
				DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, endOfDeadrun, null, null, dutyActivities, null, false, false); 
				if(!this.driverArcs.contains(dutySignOnArc))
				{
					this.driverArcs.add(dutySignOnArc); 
					this.driverGraph.addEdge(this.sourceVertex, endOfDeadrun, dutySignOnArc); 
				}
				else
				{
					 throw new IllegalArgumentException();
				}
			}
			else if(endOfDeadrun.getCurrentNode().isDriverChangeAllowed())
			{
				Optional<DriverTravel> travelToDeadrun = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(this.dutyTypeDepot.getDutySignOn()) && t.getArrivalNode().equals(endOfDeadrun.getCurrentNode())).findFirst(); 
				if(travelToDeadrun.isPresent())
				{
					int duration = travelToDeadrun.get().getDuration(); 
					int startTime = endOfDeadrun.getCurrentTime() - duration; 
					
					DutyActivity travelToTripActivity = new DutyActivity(travelToDeadrun.get().getDepartureNode(), travelToDeadrun.get().getArrivalNode(), startTime, endOfDeadrun.getCurrentTime(), -1, travelToDeadrun.get().getTravelType()); 
					dutyActivities.add(travelToTripActivity); 
					DutyActivity dutySignOn = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), startTime, startTime, -1, "Duty sign-on"); 
					dutyActivities.add(dutySignOn); 
					Collections.sort(dutyActivities);
					DriverArc dutySignOnArc = new DriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, endOfDeadrun, null, null, dutyActivities, null, false, false); 
					if(!this.driverArcs.contains(dutySignOnArc))
					{
						this.driverArcs.add(dutySignOnArc); 
						this.driverGraph.addEdge(this.sourceVertex, endOfDeadrun, dutySignOnArc); 
					}
					else
					{
						 throw new IllegalArgumentException();
					}
				}
			}
			
		}
		
	}
	
	private void addArcsToSink()
	{
		//Add arcs from end of all trips
		for(Trip trip : this.tripArcs.keySet())
		{
			DriverArc tripArc = this.tripArcs.get(trip); 
			
			DriverVertex endOfTrip = tripArc.getSuccessorVertex(); 
			Assert.assertTrue(!endOfTrip.isDeparture());
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			if(endOfTrip.getCurrentNode().isDriverChangeAllowed() && endOfTrip.getCurrentNode().equals(this.dutyTypeDepot.getDutySignOn()))
			{
				DutyActivity dutySignOff = new DutyActivity(endOfTrip.getCurrentNode(), endOfTrip.getCurrentNode(), endOfTrip.getCurrentTime(), endOfTrip.getCurrentTime(), -1, "Duty sign-off"); 
				dutyActivities.add(dutySignOff); 
				
				DriverArc dutySignOffArc = new DriverArc(this.dutyTypeDepot.getDutyType(), endOfTrip, this.sinkVertex, null, null, dutyActivities, null, false, false); 
				if(!this.driverArcs.contains(dutySignOffArc))
				{
					this.driverArcs.add(dutySignOffArc); 
					this.driverGraph.addEdge(endOfTrip, this.sinkVertex, dutySignOffArc); 
				}
				else
				{
					 throw new IllegalArgumentException();
				}
			}
			else if(endOfTrip.getCurrentNode().isDriverChangeAllowed())
			{
				Optional<DriverTravel> travelToTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(endOfTrip.getCurrentNode()) && t.getArrivalNode().equals(this.dutyTypeDepot.getDutySignOn())).findFirst(); 
				if(travelToTrip.isPresent())
				{
					int duration = travelToTrip.get().getDuration(); 
					int endTime = endOfTrip.getCurrentTime() + duration; 
					
					DutyActivity travelToTripActivity = new DutyActivity(travelToTrip.get().getDepartureNode(), travelToTrip.get().getArrivalNode(), endOfTrip.getCurrentTime(), endTime, -1, travelToTrip.get().getTravelType()); 
					dutyActivities.add(travelToTripActivity); 
					DutyActivity dutySignOff = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), endTime, endTime, -1, "Duty sign-off"); 
					dutyActivities.add(dutySignOff); 
					Collections.sort(dutyActivities);
					DriverArc dutySignOffArc = new DriverArc(this.dutyTypeDepot.getDutyType(), endOfTrip, this.sinkVertex, null, null, dutyActivities, null, false, false); 
					if(!this.driverArcs.contains(dutySignOffArc))
					{
						this.driverArcs.add(dutySignOffArc); 
						this.driverGraph.addEdge(endOfTrip, this.sinkVertex, dutySignOffArc); 
					}
					else
					{
						 throw new IllegalArgumentException();
					}
				}
			}
			
		}
		
		//Add arcs from end of all deadruns
		for(Deadrun deadrun : this.deadrunArcs.keySet())
		{
			DriverArc deadrunArc = this.deadrunArcs.get(deadrun); 
			
			DriverVertex endOfDeadrun = deadrunArc.getSuccessorVertex(); 
			Assert.assertTrue(!endOfDeadrun.isDeparture());
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			if(endOfDeadrun.getCurrentNode().isDriverChangeAllowed() && endOfDeadrun.getCurrentNode().equals(this.dutyTypeDepot.getDutySignOn()))
			{
				DutyActivity dutySignOff = new DutyActivity(endOfDeadrun.getCurrentNode(), endOfDeadrun.getCurrentNode(), endOfDeadrun.getCurrentTime(), endOfDeadrun.getCurrentTime(), -1, "Duty sign-off"); 
				dutyActivities.add(dutySignOff); 
				
				DriverArc dutySignOffArc = new DriverArc(this.dutyTypeDepot.getDutyType(), endOfDeadrun, this.sinkVertex, null, null, dutyActivities, null, false, false); 
				if(!this.driverArcs.contains(dutySignOffArc))
				{
					this.driverArcs.add(dutySignOffArc); 
					this.driverGraph.addEdge(endOfDeadrun, this.sinkVertex, dutySignOffArc); 
				}
				else
				{
					 throw new IllegalArgumentException();
				}
			}
			else if(endOfDeadrun.getCurrentNode().isDriverChangeAllowed())
			{
				Optional<DriverTravel> travelToTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(endOfDeadrun.getCurrentNode()) && t.getArrivalNode().equals(this.dutyTypeDepot.getDutySignOn())).findFirst(); 
				if(travelToTrip.isPresent())
				{
					int duration = travelToTrip.get().getDuration(); 
					int endTime = endOfDeadrun.getCurrentTime() + duration; 
					
					DutyActivity travelToTripActivity = new DutyActivity(travelToTrip.get().getDepartureNode(), travelToTrip.get().getArrivalNode(), endOfDeadrun.getCurrentTime(), endTime, -1, travelToTrip.get().getTravelType()); 
					dutyActivities.add(travelToTripActivity); 
					DutyActivity dutySignOff = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), endTime, endTime, -1, "Duty sign-off"); 
					dutyActivities.add(dutySignOff); 
					Collections.sort(dutyActivities);
					DriverArc dutySignOffArc = new DriverArc(this.dutyTypeDepot.getDutyType(), endOfDeadrun, this.sinkVertex, null, null, dutyActivities, null, false, false); 
					if(!this.driverArcs.contains(dutySignOffArc))
					{
						this.driverArcs.add(dutySignOffArc); 
						this.driverGraph.addEdge(endOfDeadrun, this.sinkVertex, dutySignOffArc); 
					}
					else
					{
						 throw new IllegalArgumentException();
					}
				}
			}
			
		}
	
	}
	
	private void addArcsBetweenBusChange()
	{
		//Add arcs between all end of trips where driver change is allowed
		Set<DriverVertex> endOfAllTripsAndDeadruns = this.driverVertices.stream().filter(d -> !d.isDeparture() && ((d.getTrip() != null) || (d.getDeadrun() != null)) && d.getCurrentNode().isDriverChangeAllowed()).collect(Collectors.toSet());
		Set<DriverVertex> allSuccessorVertices = this.driverVertices.stream().filter(d -> ((!d.isDeparture() && d.getTrip() != null) || (d.getDeadrun() != null && d.isDeparture() && d.getDeadrun().isPullOut())) && d.getCurrentNode().isDriverChangeAllowed()).collect(Collectors.toSet());
		//Currently, while changing blocks, drivers have to take a break and the maximum time between block change is 60 minutes. 
		int maxTimeBetweenBlockChange = 60; 
		for(DriverVertex predecessorVertex : endOfAllTripsAndDeadruns)
		{
			Set<DriverVertex> possibleSuccessorVertices  = allSuccessorVertices.stream().filter(e -> e.getCurrentTime() >= (predecessorVertex.getCurrentTime() + this.dutyTypeDepot.getDutyType().getMinimumBreakDuration()) && (e.getCurrentTime() - predecessorVertex.getCurrentTime()) <= maxTimeBetweenBlockChange).collect(Collectors.toSet()); 
			for(DriverVertex possibleSuccessorVertex : possibleSuccessorVertices)
			{
				/*for(DriverArc driverArc : this.driverArcs)
				{
					if(driverArc.getPredecessorVertex().equals(predecessorVertex) && driverArc.getSuccessorVertex().equals(possibleSuccessorVertex))
					{
						throw new IllegalArgumentException();
					}
				}*/
				//if(!this.driverGraph.containsEdge(predecessorVertex, possibleSuccessorVertex))
				{
					ActivitiesWhileChangingBus activties = new ActivitiesWhileChangingBus(predecessorVertex, possibleSuccessorVertex, this.dutyTypeDepot.getDutyType(), this.allDriverTravels, this.allNodes); 
					if(!activties.getDutyActivities().isEmpty())
					{
						/*activties.getDutyActivities().forEach(d -> {
							System.out.println(d.getDepartureNode().getNodeId() + "; " + d.getArrivalNode().getNodeId() + "; " + d.getDepartureTime() + "; " + d.getArrivalTime() + "; " + d.getActivity());
						});
						System.out.println();*/
						DriverArc driverArc = new DriverArc(this.dutyTypeDepot.getDutyType(), predecessorVertex, possibleSuccessorVertex , null, null, activties.getDutyActivities(), null, false, true);
						if(!this.driverArcs.contains(driverArc))
						{
							this.driverArcs.add(driverArc); 
							this.driverGraph.addEdge(predecessorVertex, possibleSuccessorVertex, driverArc); 
						}
						else
						{
							throw new IllegalArgumentException();
						}
					}
				}
				/*else
				{
					throw new IllegalArgumentException();
				}*/
			}
		}
	}

}
