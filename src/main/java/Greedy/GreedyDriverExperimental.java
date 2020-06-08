package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Variables.Deadrun;
import Variables.IdleTime;

public class GreedyDriverExperimental {

    private List<Trip> uncoverdTrips; 
    private Set<Deadrun> uncoveredDeadruns; 
    private Set<IdleTime> uncoveredIdleTimes; 
    
    private DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph; 
    private DutyTypeDepot dutyTypeDepot;
    
    private List<GreedyDriverVertex> allVertices; 
    public GreedyDriverExperimental(List<Trip> uncoverdTrips, Set<Deadrun> uncoveredDeadruns, Set<IdleTime> uncoveredIdleTimes, DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph, DutyTypeDepot dutyTypeDepot) {
        this.uncoverdTrips = uncoverdTrips; 
        this.uncoveredDeadruns = uncoveredDeadruns; 
        this.uncoveredIdleTimes = uncoveredIdleTimes; 
        this.driverGraph = driverGraph; 
        this.dutyTypeDepot = dutyTypeDepot; 
        
        this.allVertices = new ArrayList<>(); 
        
        createVertices(); 
    }
    
    private void createVertices() {
        for(Trip trip : this.uncoverdTrips) {
            GreedyDriverVertex vertex = new GreedyDriverVertex(trip, null, null); 
            this.allVertices.add(vertex); 
        }
        
        for(Deadrun deadrun : this.uncoveredDeadruns) {
            GreedyDriverVertex vertex = new GreedyDriverVertex(null, deadrun, null); 
            this.allVertices.add(vertex); 
        }
        
        for(IdleTime idleTime : this.uncoveredIdleTimes) {
            GreedyDriverVertex vertex = new GreedyDriverVertex(null, null, idleTime);
            this.allVertices.add(vertex);
        }
        
        Collections.sort(this.allVertices);
    }
    
    
}
