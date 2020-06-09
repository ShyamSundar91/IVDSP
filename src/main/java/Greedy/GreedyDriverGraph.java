package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Networks.DutyTypeDepot;
import Variables.Deadrun;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class GreedyDriverGraph {
    
    private List<Trip> uncoverdTrips; 
    private Set<Deadrun> uncoveredDeadruns; 
    private Set<IdleTime> uncoveredIdleTimes; 
    
    private DutyTypeDepot dutyTypeDepot;
    private List<DriverTravel> allDriverTravels; 
    private Set<Node> allNodes;
    private DefaultDirectedGraph<GreedyDriverVertex, GreedyDriverArc> driverGraph; 
    private GreedyDriverVertex sourceVertex; 
    private GreedyDriverVertex sinkVertex; 
    
    public GreedyDriverGraph(List<Trip> uncoverdTrips, Set<Deadrun> uncoveredDeadruns, Set<IdleTime> uncoveredIdleTimes, DutyTypeDepot dutyTypeDepot, List<DriverTravel> allDriverTravels, Set<Node> allNodes)
    {
        this.uncoverdTrips = uncoverdTrips; 
        this.uncoveredDeadruns = uncoveredDeadruns; 
        this.uncoveredIdleTimes = uncoveredIdleTimes; 

        this.dutyTypeDepot = dutyTypeDepot;
        this.allDriverTravels = allDriverTravels; 
        this.allNodes = allNodes; 
        
        this.driverGraph = new DefaultDirectedGraph<GreedyDriverVertex, GreedyDriverArc>(GreedyDriverArc.class); 
        
        addSourceSink(); 
        addTasks();
        addArcsFromSource(); 
        addArcsToSink();
        addArcsWithinBlocks(); 
        addArcsForBlockChange(); 
        
    }
    
    private void addSourceSink()
    {
        sourceVertex = new GreedyDriverVertex(-1, this.dutyTypeDepot.getDutyType(), null, null, null); 
        this.driverGraph.addVertex(sourceVertex); 
        
        sinkVertex = new GreedyDriverVertex(Integer.MAX_VALUE, this.dutyTypeDepot.getDutyType(), null, null, null); 
        this.driverGraph.addVertex(sinkVertex); 
    }
    
    private void addTasks()
    {
        for(Trip trip : this.uncoverdTrips)
        {
            GreedyDriverVertex vertex = new GreedyDriverVertex(trip.getTripId(), this.dutyTypeDepot.getDutyType(), trip, null, null); 
            this.driverGraph.addVertex(vertex); 
        }
        
        for(Deadrun deadrun : this.uncoveredDeadruns)
        {
            GreedyDriverVertex vertex = new GreedyDriverVertex(deadrun.getDeadrunId(), this.dutyTypeDepot.getDutyType(), null, deadrun, null); 
            this.driverGraph.addVertex(vertex); 
        }
        
        for(IdleTime idleTime : this.uncoveredIdleTimes)
        {
            GreedyDriverVertex vertex = new GreedyDriverVertex(3, this.dutyTypeDepot.getDutyType(), null, null, idleTime);
            this.driverGraph.addVertex(vertex); 
        }
    }
    
    private void addArcsFromSource()
    {
        List<GreedyDriverVertex> allTaskVertices = this.driverGraph.vertexSet().stream().filter(v -> v.getVertexID() > -1 && v.getVertexID() < Integer.MAX_VALUE).collect(Collectors.toList()); 
        
        for(GreedyDriverVertex vertex : allTaskVertices)
        {
            List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
            
            if(vertex.getDepartureNode().isDriverChangeAllowed() && vertex.getDepartureNode().equals(this.dutyTypeDepot.getDutySignOn()))
            {
                DutyActivity dutySignOn = new DutyActivity(vertex.getDepartureNode(), vertex.getDepartureNode(), vertex.getDepartureTime(), vertex.getDepartureTime(), -1, "Duty sign-on"); 
                dutyActivities.add(dutySignOn); 
                
                GreedyDriverArc dutySignOnArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, vertex, dutyActivities, false);
                this.driverGraph.addEdge(this.sourceVertex, vertex, dutySignOnArc); 
            }
            else if(vertex.getDepartureNode().isDriverChangeAllowed())
            {
                Optional<DriverTravel> travelToTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(this.dutyTypeDepot.getDutySignOn()) && t.getArrivalNode().equals(vertex.getDepartureNode())).findFirst(); 
                if(travelToTrip.isPresent())
                {
                    int duration = travelToTrip.get().getDuration(); 
                    int startTime = vertex.getDepartureTime() - duration; 
                    
                    DutyActivity travelToTripActivity = new DutyActivity(travelToTrip.get().getDepartureNode(), travelToTrip.get().getArrivalNode(), startTime, vertex.getDepartureTime(), -1, travelToTrip.get().getTravelType()); 
                    dutyActivities.add(travelToTripActivity); 
                    DutyActivity dutySignOn = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), startTime, startTime, -1, "Duty sign-on"); 
                    dutyActivities.add(dutySignOn); 
                    Collections.sort(dutyActivities);
                    GreedyDriverArc dutySignOnArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), this.sourceVertex, vertex, dutyActivities, false);
                    this.driverGraph.addEdge(this.sourceVertex, vertex, dutySignOnArc); 
                }
            }
        }
    }
    
    private void addArcsToSink()
    {
        List<GreedyDriverVertex> allTaskVertices = this.driverGraph.vertexSet().stream().filter(v -> v.getVertexID() > -1 && v.getVertexID() < Integer.MAX_VALUE).collect(Collectors.toList());
        
        for(GreedyDriverVertex vertex : allTaskVertices)
        {
            List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
            if(vertex.getArrivalNode().isDriverChangeAllowed() && vertex.getArrivalNode().equals(this.dutyTypeDepot.getDutySignOn()))
            {
                DutyActivity dutySignOff = new DutyActivity(vertex.getArrivalNode(), vertex.getArrivalNode(), vertex.getArrivalTime(), vertex.getArrivalTime(), -1, "Duty sign-off"); 
                dutyActivities.add(dutySignOff); 
                
                GreedyDriverArc dutySignOffArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), vertex, this.sinkVertex, dutyActivities, false); 
                this.driverGraph.addEdge(vertex, this.sinkVertex, dutySignOffArc); 
               
            }
            else if(vertex.getArrivalNode().isDriverChangeAllowed())
            {
                Optional<DriverTravel> travelToTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(vertex.getArrivalNode()) && t.getArrivalNode().equals(this.dutyTypeDepot.getDutySignOn())).findFirst(); 
                if(travelToTrip.isPresent())
                {
                    int duration = travelToTrip.get().getDuration(); 
                    int endTime = vertex.getArrivalTime() + duration; 
                    
                    DutyActivity travelToTripActivity = new DutyActivity(travelToTrip.get().getDepartureNode(), travelToTrip.get().getArrivalNode(), vertex.getArrivalTime(), endTime, -1, travelToTrip.get().getTravelType()); 
                    dutyActivities.add(travelToTripActivity); 
                    DutyActivity dutySignOff = new DutyActivity(this.dutyTypeDepot.getDutySignOn(), this.dutyTypeDepot.getDutySignOn(), endTime, endTime, -1, "Duty sign-off"); 
                    dutyActivities.add(dutySignOff); 
                    Collections.sort(dutyActivities);
                    GreedyDriverArc dutySignOffArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), vertex, this.sinkVertex, dutyActivities, false); 
                    this.driverGraph.addEdge(vertex, this.sinkVertex, dutySignOffArc); 
                }
            }
            
        }
    }
    
    private void addArcsWithinBlocks()
    {
        List<GreedyDriverVertex> allTaskVertices = this.driverGraph.vertexSet().stream().filter(v -> v.getVertexID() > -1 && v.getVertexID() < Integer.MAX_VALUE).collect(Collectors.toList());
        
        for(GreedyDriverVertex vertex : allTaskVertices)
        {
            List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
            Optional<GreedyDriverVertex> succeedingVertex = allTaskVertices.stream().filter(v -> vertex.getArrivalTime() == v.getDepartureTime() && vertex.getArrivalNode().equals(v.getDepartureNode())).findAny(); 
            if(succeedingVertex.isPresent())
            {
                GreedyDriverArc withinBlockArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), vertex, succeedingVertex.get(), dutyActivities, false);
                this.driverGraph.addEdge(vertex, succeedingVertex.get(), withinBlockArc); 
            }
        }
    }
    
    private void addArcsForBlockChange()
    {
        List<GreedyDriverVertex> allTaskVertices = this.driverGraph.vertexSet().stream().filter(v -> v.getVertexID() > -1 && v.getVertexID() < Integer.MAX_VALUE).collect(Collectors.toList());
        
        for(GreedyDriverVertex vertex : allTaskVertices)
        {
            List<GreedyDriverVertex> possibleSuccessors = allTaskVertices.stream().filter(v -> (v.getDepartureTime() >= (vertex.getArrivalTime() + this.dutyTypeDepot.getDutyType().getMinimumBreakDuration())) && ((v.getDepartureTime() - vertex.getArrivalTime()) <= 60)).collect(Collectors.toList()); 
            
            for(GreedyDriverVertex second : possibleSuccessors)
            {
                GreedyActivitiesWhileChangingBus activties = new GreedyActivitiesWhileChangingBus(vertex , second, this.dutyTypeDepot.getDutyType(), this.allDriverTravels, this.allNodes); 
                if(!activties.getDutyActivities().isEmpty())
                {
                    
                    GreedyDriverArc driverArc = new GreedyDriverArc(this.dutyTypeDepot.getDutyType(), vertex, second, activties.getDutyActivities(), true);
                    this.driverGraph.addEdge(vertex, second, driverArc); 
                }
            }
            
        }
    }

}
