package Networks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Data.VehicleType;
import Networks.VehicleUtil.BetweenTripsOnDifferentNodes;
import Networks.VehicleUtil.BetweenTripsOnSameNode;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class VehicleGraph 
{
	private VehicleTypeDepot vehicleTypeDepot;  
	private Node vehicleDepot; 
	private VehicleType vehicleType; 
	@Getter
	private DefaultDirectedGraph<VehicleVertex, VehicleArc> vehicleGraph; 
	
	private Map<Trip, VehicleVertex> tripVertices; 
	@Getter
	private VehicleVertex source; 
	@Getter
	private VehicleVertex sink;
	private List<Trip> allTrips; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Set<Node> allNodes; 
	@Getter
	private Set<Deadrun> deadruns; 
	@Getter
	private Set<IdleTime> idleTimes; 

	public VehicleGraph(VehicleTypeDepot vehicleTypeDepot, List<Trip> allTrips, Set<VehicleTravel> allVehicleTravels, Set<Node> allNodes, Set<Deadrun> deadruns, Set<IdleTime> idleTimes)
	{
		this.vehicleTypeDepot = vehicleTypeDepot; 
		this.vehicleDepot = this.vehicleTypeDepot.getDepot(); 
		this.vehicleType = this.vehicleTypeDepot.getVehicleType(); 
		this.allTrips = allTrips; 
		this.allVehicleTravels = allVehicleTravels; 
		this.allNodes = allNodes; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		
		this.vehicleGraph = new DefaultDirectedGraph<VehicleVertex, VehicleArc>(VehicleArc.class); 
		this.tripVertices = new HashMap<Trip, VehicleVertex>(); 
		
		addSourceSink();
		addTimeTableTrips();
		addEdgesFromToSourceSink();
		addEdgesBetweenTrips();
		check(); 
	}
	
	private void addSourceSink()
	{
		source = new VehicleVertex( -1, null, this.vehicleTypeDepot); 
		this.vehicleGraph.addVertex(source); 
		sink = new VehicleVertex(Integer.MAX_VALUE, null, this.vehicleTypeDepot); 
		this.vehicleGraph.addVertex(sink); 
	}
	
	private void addTimeTableTrips()
	{
		 
		for(Trip trip : this.allTrips)
		{
			VehicleVertex tripVertex = new VehicleVertex(trip.getTripId(), trip, this.vehicleTypeDepot); 
			this.vehicleGraph.addVertex(tripVertex); 
			this.tripVertices.put(trip, tripVertex); 
			
		}
	}
	
	private void addEdgesFromToSourceSink() 
	{
		for(VehicleVertex tripVertex : this.tripVertices.values())
		{
			if(!tripVertex.getTrip().getDepartureNode().equals(this.vehicleDepot))
			{
				List<BlockActivity> blockElements = new ArrayList<BlockActivity>(); 
				List<Deadrun> deadrunsOnArc = new ArrayList<Deadrun>(); 	
				Optional<VehicleTravel> travel = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(this.vehicleDepot) && t.getArrivalNode().equals(tripVertex.getTrip().getDepartureNode())).findAny();  
				if(travel.isPresent())
				{
					int endTime = tripVertex.getTrip().getDepartureTime(); 
				    int startTime = endTime - travel.get().getDuration(); 
				    Deadrun deadrun = new Deadrun(this.vehicleDepot, tripVertex.getTrip().getDepartureNode(), startTime, endTime, true, false, -1, tripVertex.getTrip().getTripId()); 
				    //if(!this.deadruns.contains(deadrun))
				    {
				    	this.deadruns.add(deadrun); 
				    	deadrunsOnArc.add(deadrun); 
				    }
				    /*else
				    {
				    	Optional<Deadrun> existingDeadrun = this.deadruns.stream().filter(d -> d.equals(deadrun)).findAny(); 
				    	deadrunsOnArc.add(existingDeadrun.get()); 
				    }*/
				    
					BlockActivity travelFromDepot = new BlockActivity(this.vehicleDepot, tripVertex.getTrip().getDepartureNode(), startTime, endTime, travel.get().getDistance(), deadrunsOnArc.get(0).getDeadrunId(), "Garage"); 
					blockElements.add(travelFromDepot); 
						
					VehicleArc fromSourceEdge = new VehicleArc(this.source, tripVertex, this.vehicleTypeDepot, deadrunsOnArc, null, blockElements); 
					this.vehicleGraph.addEdge(source, tripVertex, fromSourceEdge); 		
				}
			}
			else
			{
				VehicleArc fromSourceEdge = new VehicleArc(this.source, tripVertex, this.vehicleTypeDepot, new ArrayList<Deadrun>(), null, new ArrayList<BlockActivity>()); 
				this.vehicleGraph.addEdge(source, tripVertex, fromSourceEdge); 
			}
			
			if(!tripVertex.getTrip().getArrivalNode().equals(this.vehicleDepot))
			{
				List<BlockActivity> blockElements = new ArrayList<BlockActivity>(); 
				List<Deadrun> deadrunsOnArc = new ArrayList<Deadrun>();
				
				Optional<VehicleTravel> travel = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(tripVertex.getTrip().getArrivalNode()) && t.getArrivalNode().equals(this.vehicleDepot)).findAny();  
				if(travel.isPresent())
				{
					int startTime = tripVertex.getTrip().getArrivalTime();
					int endTime = startTime + travel.get().getDuration(); 
					
					Deadrun deadrun = new Deadrun(tripVertex.getTrip().getArrivalNode(), this.vehicleDepot, startTime, endTime, false, true, tripVertex.getTrip().getTripId(), Integer.MAX_VALUE); 
					//if(!this.deadruns.contains(deadrun))
					{
					    this.deadruns.add(deadrun); 
					    deadrunsOnArc.add(deadrun); 
					}
					/*else
					{
						Optional<Deadrun> existingDeadrun = this.deadruns.stream().filter(d -> d.equals(deadrun)).findAny(); 
					    deadrunsOnArc.add(existingDeadrun.get()); 
					}*/
					
					BlockActivity travelToDepot = new BlockActivity(tripVertex.getTrip().getArrivalNode(), this.vehicleDepot, startTime, endTime, travel.get().getDistance(), deadrunsOnArc.get(0).getDeadrunId(), "Garage"); 
					blockElements.add(travelToDepot); 
						
					VehicleArc toSinkEdge = new VehicleArc(tripVertex, this.sink, this.vehicleTypeDepot, deadrunsOnArc, null, blockElements); 
					this.vehicleGraph.addEdge(tripVertex, sink, toSinkEdge);
				}
					 
				
			}
			else
			{
				VehicleArc toSinkEdge = new VehicleArc(tripVertex, this.sink, this.vehicleTypeDepot, new ArrayList<Deadrun>(), null, new ArrayList<BlockActivity>()); 
				this.vehicleGraph.addEdge(tripVertex, sink, toSinkEdge);
			}
		}
	}
	
	private void addEdgesBetweenTrips()
	{
		for(VehicleVertex trip1 : this.tripVertices.values())
		{
			for(VehicleVertex trip2 : this.tripVertices.values())
			{
				if(!trip1.getTrip().equals(trip2.getTrip()) )
				{
					if((trip2.getTrip().getDepartureTime() - trip1.getTrip().getArrivalTime()) >= 0 && (trip2.getTrip().getDepartureTime() - trip1.getTrip().getArrivalTime()) <= 240) //Max time between two trips
					{
						
						if(trip1.getTrip().getArrivalNode().equals(trip2.getTrip().getDepartureNode()))
						{
							BetweenTripsOnSameNode activities = new BetweenTripsOnSameNode(trip1.getTrip(), trip2.getTrip(), this.vehicleType, this.deadruns, this.idleTimes, this.allVehicleTravels, this.allNodes); 
							List<BlockActivity> blockActivitiesBetweenTrips =  activities.getBlockActivities(); 
							if(!blockActivitiesBetweenTrips.isEmpty())
							{
								VehicleArc betweenTripEdge = new VehicleArc(trip1, trip2, this.vehicleTypeDepot, activities.getDeadrunsOnArc(), activities.getIdleTimeOnArc(), blockActivitiesBetweenTrips); 
								this.vehicleGraph.addEdge(trip1, trip2, betweenTripEdge);
							}									
						}
						else
						{
							BetweenTripsOnDifferentNodes activities = new  BetweenTripsOnDifferentNodes(trip1.getTrip(), trip2.getTrip(), this.vehicleType, this.deadruns, this.idleTimes, this.allVehicleTravels, this.allNodes); 
							List<BlockActivity> blockActivitiesBetweenTrips =  activities.getBlockActivities(); 
							if(!blockActivitiesBetweenTrips.isEmpty())
							{
								VehicleArc betweenTripEdge = new VehicleArc(trip1, trip2, this.vehicleTypeDepot, activities.getDeadrunsOnArc(), activities.getIdleTimeOnArc(), blockActivitiesBetweenTrips); 
								this.vehicleGraph.addEdge(trip1, trip2, betweenTripEdge);
							}
						}
				    }
				}	
			}
		}
	}
	

	private void check()
	{
		Set<VehicleArc> arcs = this.vehicleGraph.edgeSet(); 
		for(VehicleArc arc : arcs)
		{
			if(!arc.getBlockActivitiesOnEdge().isEmpty() && !arc.getDeadrunsOnEdge().isEmpty())
			{
				List<Deadrun> deadrunsInEdge = arc.getDeadrunsOnEdge(); 
				boolean foundDeadrun = false; 
				for(Deadrun deadrun : deadrunsInEdge)
				{
					for(BlockActivity activity : arc.getBlockActivitiesOnEdge())
					{
						if(deadrun.getDepartureNode().equals(activity.getDepartureNode()) && deadrun.getArrivalNode().equals(activity.getArrivalNode()) && deadrun.getDepartureTime() == activity.getDepartureTime() && deadrun.getArrivalTime() == activity.getArrivalTime())
						{
							foundDeadrun = true;
							break; 
						}
					}
					
					if(!foundDeadrun)
					{
						throw new IllegalArgumentException();
					}
				}
			}
		}
		
		Set<VehicleVertex> vertices = this.vehicleGraph.vertexSet().stream().filter(v -> v.getTrip()!= null).collect(Collectors.toSet());
		for(VehicleVertex vertex : vertices)
		{
			if(this.vehicleGraph.outDegreeOf(vertex) == 0 && this.vehicleGraph.inDegreeOf(vertex) == 0)
			{
				System.out.println("vertex with trip " + vertex.getTrip().getTripId() + " does not have outgoing and incoming edges" + ", " + vertex.getTrip().getDepartureNode().getNodeId() + ", " + vertex.getTrip().getArrivalNode().getNodeId());
			}
			
			
		}
	}

}
