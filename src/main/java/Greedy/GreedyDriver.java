package Greedy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Variables.Block;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyDriver {
    
    private List<Trip> allTrips; 
    private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
    private Map<Duty, Integer> initialAndDutiesGenerated;
    private List<Block> blocksInSolution; 
    private Set<Deadrun> deadrunsInSolution; 
    private Set<IdleTime> idleTimesInSolution; 
    
    private List<Trip> uncoveredTrips; 
    private Set<Deadrun> uncoveredDeadruns; 
    private Set<IdleTime> uncoveredIdleTimes;
    @Getter
    private List<Duty> dutiesInSolution; 
    @Getter
    private double objective; 
    
    public GreedyDriver(List<Trip> allTrips, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Duty, Integer> initialAndDutiesGenerated, List<Block> blocksInSolution, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution) {
        this.allTrips = allTrips; 
        this.driverGraphs = driverGraphs; 
        this.initialAndDutiesGenerated = initialAndDutiesGenerated; 
        this.blocksInSolution = blocksInSolution; 
        this.deadrunsInSolution = deadrunsInSolution; 
        this.idleTimesInSolution = idleTimesInSolution; 
        
        this.uncoveredTrips = new ArrayList<>(this.allTrips); 
        this.uncoveredDeadruns = new HashSet<>(this.deadrunsInSolution); 
        this.uncoveredIdleTimes = new HashSet<>(this.idleTimesInSolution); 
        
        this.dutiesInSolution = new ArrayList<Duty>(); 
        this.objective = 0; 
        
        getUncoveredTasks(); 
        
        
        greedy();
    }
    
    private void greedy() {
        
        while(!this.uncoveredTrips.isEmpty() || !this.uncoveredDeadruns.isEmpty() || !this.uncoveredIdleTimes.isEmpty()) {
          
            for(DutyTypeDepot dutyType : this.driverGraphs.keySet()) {
                keepOnlyRelevantArcs(this.driverGraphs.get(dutyType));
                GreedyDriverController controller = new GreedyDriverController(dutyType, this.driverGraphs.get(dutyType), this.uncoveredTrips, this.uncoveredDeadruns, this.uncoveredIdleTimes); 
                
                for(Duty duty : controller.getDutiesGenerated()) {
                    this.dutiesInSolution.add(duty); 
                    this.objective = this.objective + duty.getTotalCostOfDuty(); 
                    
                    this.uncoveredTrips.removeAll(duty.getTripsInDuty()); 
                    this.uncoveredDeadruns.removeAll(duty.getDeadrunsInDuty()); 
                    this.uncoveredIdleTimes.removeAll(duty.getIdleTimesInDuty()); 
                }
                System.out.println("Vertices set = " + this.driverGraphs.get(dutyType).vertexSet().size() + ", Edges set = " + this.driverGraphs.get(dutyType).edgeSet().size());
            }
            
            
            System.out.println("Number of uncovered trips = " + this.uncoveredTrips.size() + ", " + "Number of uncovered deadruns = " + this.uncoveredDeadruns.size() + ", " + "Number of uncovered idle times = " + this.uncoveredIdleTimes.size());
            /*for(Trip trip : this.uncoveredTrips) {
                System.out.println(trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime());
            }*/
            
            /*for(Deadrun trip : this.uncoveredDeadruns) {
            System.out.println(trip.getDepartureNode().getNodeId() + "; " + trip.getArrivalNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime());
            }*/
            
            /*for(IdleTime trip : this.uncoveredIdleTimes) {
                System.out.println(trip.getNode().getNodeId() + "; " + trip.getDepartureTime() + "; " + trip.getArrivalTime());
                }*/
        }
       
    }
    
    private void getUncoveredTasks() {
        for(Duty duty : this.initialAndDutiesGenerated.keySet()) {
            
            if(this.initialAndDutiesGenerated.get(duty) == 1) {
                this.dutiesInSolution.add(duty); 
                this.objective = this.objective + duty.getTotalCostOfDuty(); 
                
                this.uncoveredTrips.removeAll(duty.getTripsInDuty()); 
                this.uncoveredDeadruns.removeAll(duty.getDeadrunsInDuty()); 
                this.uncoveredIdleTimes.removeAll(duty.getIdleTimesInDuty()); 
            }
        }
        
    }
    
    private void keepOnlyRelevantArcs(DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph) {
        
            Set<DriverArc> arcsToRemove = new HashSet<DriverArc>(); 
            for(DriverArc arc : driverGraph.edgeSet())
            {
                if(arc.getTrip() != null && !this.uncoveredTrips.contains(arc.getTrip()))
                {
                    arcsToRemove.add(arc); 
                }
                
                if(!this.uncoveredDeadruns.isEmpty())
                {
                    if(arc.getDeadrun() != null && !this.uncoveredDeadruns.contains(arc.getDeadrun()))
                    {
                        arcsToRemove.add(arc);  
                    }
                }
                
                if(!this.uncoveredIdleTimes.isEmpty())
                {
                    if(arc.getIdleTimeOnArc() != null && !this.uncoveredIdleTimes.contains(arc.getIdleTimeOnArc()))
                    {
                        arcsToRemove.add(arc); 
                    }
                }
            }
            driverGraph.removeAllEdges(arcsToRemove); 
        
            Set<DriverVertex> verticesToRemove = new HashSet<DriverVertex>(); 
            for(DriverVertex vertex  : driverGraph.vertexSet())
            {
                if(driverGraph.incomingEdgesOf(vertex).isEmpty() && driverGraph.outgoingEdgesOf(vertex).isEmpty())
                {
                    verticesToRemove.add(vertex); 
                }
                
                vertex.getLabels().clear();
            }
            driverGraph.removeAllVertices(verticesToRemove); 
    }

}
