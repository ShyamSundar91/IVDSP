package Greedy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.DriverTravel;
import Data.Node;
import Data.Trip;
import Networks.DutyTypeDepot;
import Variables.Deadrun;
import Variables.Duty;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyDriverExperimental {

    private List<Trip> allTrips; 
    private Set<Deadrun> deadrunsInSolution; 
    private Set<IdleTime> idleTimesInSolution;
    private Map<Duty, Integer> initialAndDutiesGenerated;
    @Getter
    private List<Duty> dutiesInSolution; 
    @Getter
    private double objective;
    
    private List<Trip> uncoverdTrips; 
    private Set<Deadrun> uncoveredDeadruns; 
    private Set<IdleTime> uncoveredIdleTimes; 
    
    private DefaultDirectedGraph<GreedyDriverVertex, GreedyDriverArc> driverGraph; 
    private DutyTypeDepot dutyTypeDepot;
    
    private List<DriverTravel> allDriverTravels; 
    private Set<Node> allNodes;
    
    public GreedyDriverExperimental(List<Trip> allTrips, Map<Duty, Integer> initialAndDutiesGenerated, Set<Deadrun> deadrunsInSolution, Set<IdleTime> idleTimesInSolution, DutyTypeDepot dutyTypeDepot, List<DriverTravel> allDriverTravels, Set<Node> allNodes) {
        this.allTrips = allTrips; 
        this.deadrunsInSolution = deadrunsInSolution; 
        this.idleTimesInSolution = idleTimesInSolution; 
        this.dutyTypeDepot = dutyTypeDepot; 
        this.initialAndDutiesGenerated = initialAndDutiesGenerated; 
        
        this.uncoverdTrips = new ArrayList<>(this.allTrips); 
        this.uncoveredDeadruns = new HashSet<>(this.deadrunsInSolution); 
        this.uncoveredIdleTimes = new HashSet<>(this.idleTimesInSolution); 
        
        this.dutiesInSolution = new ArrayList<Duty>(); 
        this.objective = 0; 
        
        this.allDriverTravels = allDriverTravels; 
        this.allNodes = allNodes; 
        
        getUncoveredTasks(); 
        
        GreedyDriverGraph graph = new GreedyDriverGraph(this.uncoverdTrips, this.uncoveredDeadruns, this.uncoveredIdleTimes, this.dutyTypeDepot, this.allDriverTravels, this.allNodes); 
        this.driverGraph = graph.getDriverGraph(); 
        
        greedy(); 
        
        System.out.println("Greedy number of duties = " + this.dutiesInSolution.size());
        System.out.println("Greedy driver objective = " + this.objective);
    }
    
    private void getUncoveredTasks()
    {
        for(Duty duty : this.initialAndDutiesGenerated.keySet())
        {
            if(this.initialAndDutiesGenerated.get(duty) == 1)
            {
                this.dutiesInSolution.add(duty); 
                this.objective = this.objective + duty.getTotalCostOfDuty(); 
                
                this.uncoverdTrips.removeAll(duty.getTripsInDuty()); 
                this.uncoveredDeadruns.removeAll(duty.getDeadrunsInDuty()); 
                this.uncoveredIdleTimes.removeAll(duty.getIdleTimesInDuty());
            }
        }
    }
    
    private void greedy()
    {
        while(!this.uncoverdTrips.isEmpty() || !this.uncoveredDeadruns.isEmpty() || !this.uncoveredIdleTimes.isEmpty())
        {
            GreedyDriverController controller = new GreedyDriverController(this.dutyTypeDepot.getDutyType(), this.driverGraph); 
            for(Duty duty : controller.getDutiesGenerated()) {
                this.dutiesInSolution.add(duty); 
                this.objective = this.objective + duty.getTotalCostOfDuty(); 
                
                this.uncoverdTrips.removeAll(duty.getTripsInDuty()); 
                this.uncoveredDeadruns.removeAll(duty.getDeadrunsInDuty()); 
                this.uncoveredIdleTimes.removeAll(duty.getIdleTimesInDuty()); 
            }
            
            removeCoveredTasks(); 
        }
    }
    
    private void removeCoveredTasks()
    {
        List<GreedyDriverVertex> verticesToRemove = new ArrayList<>(); 
        for(GreedyDriverVertex vertex : this.driverGraph.vertexSet())
        {
            if(vertex.getTrip() != null && !this.uncoverdTrips.contains(vertex.getTrip()))
            {
                verticesToRemove.add(vertex); 
            }
            
            if(vertex.getDeadrun() != null && !this.uncoveredDeadruns.contains(vertex.getDeadrun()))
            {
                verticesToRemove.add(vertex); 
            }
            
            if(vertex.getIdleTime() != null && !this.uncoveredIdleTimes.contains(vertex.getIdleTime()))
            {
                verticesToRemove.add(vertex);
            }
            
            vertex.getLabels().clear();
        }
        
        this.driverGraph.removeAllVertices(verticesToRemove); 
    }
    
    
    
}
