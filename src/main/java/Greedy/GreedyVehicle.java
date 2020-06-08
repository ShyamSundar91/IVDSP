package Greedy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Variables.Block;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyVehicle {
    
    private List<Trip> allTrips; 
    private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph;
    private Map<Block, Integer> initialBlocksAndGenerated; 
    @Getter
    private List<Block> blocksInSolution; 
    @Getter
    private List<Deadrun> deadrunsInSolution;
    @Getter
    private List<IdleTime> idleTimesInSolution;
    @Getter
    private double objective; 
    
    private List<Trip> uncoveredTrips; 
    
    public GreedyVehicle(List<Trip> trips, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraph, Map<Block, Integer> initialBlocksAndGenerated) {
        this.allTrips = trips; 
        this.vehicleGraph = vehicleGraph; 
        this.initialBlocksAndGenerated = initialBlocksAndGenerated; 
        
        this.uncoveredTrips = new ArrayList<>(this.allTrips); 
        this.blocksInSolution = new ArrayList<>(); 
        this.deadrunsInSolution = new ArrayList<>(); 
        this.idleTimesInSolution = new ArrayList<>(); 
        this.objective = 0; 
        
        getUncoveredTrips(); 
        greedy(); 
        System.out.println("Greedy number of blocks = " + this.blocksInSolution.size());
        System.out.println("Greedy vehicle objective = " + this.objective);
    }
    
    private void greedy() {
        
        while(!this.uncoveredTrips.isEmpty()) {
            for(VehicleTypeDepot depot : vehicleGraph.keySet()) {
                DefaultDirectedGraph<VehicleVertex, VehicleArc> graph = this.vehicleGraph.get(depot); 
                removeVertices(graph); 
               
                GreedyVehicleController controller = new GreedyVehicleController(depot.getVehicleType(), graph); 
                for(Block block : controller.getBlocksGenerated()) {
                    this.blocksInSolution.add(block); 
                    this.objective = this.objective + block.getTotalCostOfBlock(); 
                    
                    this.uncoveredTrips.removeAll(block.getTripsInBlock()); 
                    this.deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
                    this.idleTimesInSolution.addAll(block.getIdleTimesInBlock());
                }
            }
        }
    }
    
    private void removeVertices(DefaultDirectedGraph<VehicleVertex, VehicleArc> graph) {
        Set<VehicleVertex> tripVertices = graph.vertexSet().stream().filter(v -> v.getTrip() != null).collect(Collectors.toSet()); 
        for(VehicleVertex tripVertex : tripVertices)
        {
            if(!this.uncoveredTrips.contains(tripVertex .getTrip()))
            {
                graph.removeVertex(tripVertex); 
            }
        }
        
        for(VehicleVertex vertex : graph.vertexSet())
        {
            vertex.getLabels().clear();
        }
    }
    
    private void getUncoveredTrips() {
        for(Block block : this.initialBlocksAndGenerated.keySet()) {
            if(this.initialBlocksAndGenerated.get(block) == 1) {
                this.blocksInSolution.add(block); 
                this.objective = this.objective + block.getTotalCostOfBlock(); 
                
                this.uncoveredTrips.removeAll(block.getTripsInBlock()); 
                this.deadrunsInSolution.addAll(block.getDeadrunsInBlock()); 
                this.idleTimesInSolution.addAll(block.getIdleTimesInBlock()); 
            }
        }
    }
}
