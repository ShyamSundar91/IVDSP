package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Data.VehicleType;
import Networks.VehicleArc;
import Networks.VehicleVertex;
import Subproblems.LabelVehicle;
import Subproblems.VehicleREF;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyVehicleController {
    
    private VehicleType vehicleType; 
    private DefaultDirectedGraph<VehicleVertex, VehicleArc> graph; 
    @Getter
    private List<Block> blocksGenerated; 
    
    private VehicleVertex sourceVertex; 
    private VehicleVertex sinkVertex; 
    
    private List<VehicleVertex> queue;
    
    public GreedyVehicleController(VehicleType vehicleType, DefaultDirectedGraph<VehicleVertex, VehicleArc> graph) {
        this.vehicleType = vehicleType; 
        this.graph = graph; 
        this.blocksGenerated = new ArrayList<>(); 
        
        this.queue = new ArrayList<>(); 
        this.sourceVertex = this.graph.vertexSet().stream().filter(v -> v.getVertexId() == -1).collect(Collectors.toList()).get(0); 
        this.sinkVertex = this.graph.vertexSet().stream().filter(v -> v.getVertexId() == Integer.MAX_VALUE).collect(Collectors.toList()).get(0); 
        
        initialization();
       
        algorithm(); 
    }
    
    private void initialization()
    {
        VehicleREF initialREF = new VehicleREF(this.vehicleType, null, null, null); 
        LabelVehicle initialLabel = new LabelVehicle(initialREF, null, null, null); 
        this.sourceVertex.getLabels().add(initialLabel); 
        this.queue.add(sourceVertex); 
    }
    
    private void algorithm()
    {
        List<VehicleVertex> currentBestVertices = new ArrayList<VehicleVertex>();  
        
        while(!this.queue.isEmpty())
        {
            //Collections.sort(this.queue);
            VehicleVertex selectedVertex = selectCandidate(this.queue); 
            
            currentBestVertices = new ArrayList<VehicleVertex>();
            
            Set<VehicleArc> outgoingArcs = this.graph.outgoingEdgesOf(selectedVertex).stream().collect(Collectors.toSet()); 
            
            for(LabelVehicle label : selectedVertex.getLabels())
            {
                if(!label.isLabelVehicleVisited())
                {
                    for(VehicleArc outgoingArc : outgoingArcs)
                    {
                        
                        VehicleVertex successorVertex = outgoingArc.getSuccessorVertex(); 
                            
                        VehicleREF newREF = new VehicleREF(this.vehicleType, label.getUpdatedResources(), outgoingArc, successorVertex);  
                        if(newREF.isValid())
                        {
                            if(successorVertex.getVertexId() == Integer.MAX_VALUE)
                            {
                                LabelVehicle newLabel = new LabelVehicle(newREF, label, selectedVertex, outgoingArc);
                                successorVertex.getLabels().add(newLabel); 
                                       
                            }
                            else if(successorVertex.getVertexId() != Integer.MAX_VALUE)
                            {
                                LabelVehicle newLabel = new LabelVehicle(newREF, label, selectedVertex, outgoingArc); 
                                successorVertex.getLabels().add(newLabel);
                                    
                                if(!currentBestVertices.contains(successorVertex))
                                {
                                    currentBestVertices.add(successorVertex); 
                                }
                            }
                        }
                    }
                }
                    
                label.labelVehicleVisited();
            }
        
            this.queue.remove(selectedVertex); 
            
            if(!currentBestVertices.isEmpty()) {
                
                for(VehicleVertex vertex : this.queue)
                {
                    List<LabelVehicle> labelsToRemove = new ArrayList<>(); 
                    for(LabelVehicle label : vertex.getLabels()) {
                        
                       if(!label.getSourceVehicleVertex().equals(selectedVertex)) {
                           
                           labelsToRemove.add(label); 
                       }
                   }
                   vertex.getLabels().removeAll(labelsToRemove);  
                }
                this.queue.clear();
                
                this.queue.addAll(currentBestVertices); 
            }
        }
        
        reterievePaths(); 
    }
    
    private void reterievePaths()
    {
        List<LabelVehicle> labelsAtSink = new ArrayList<LabelVehicle>(this.sinkVertex.getLabels()); 
        labelsAtSink.forEach(l -> l.calculateDelta());
        Collections.sort(labelsAtSink, Comparator.comparingDouble(LabelVehicle::getDelta));
        Collections.reverse(labelsAtSink);
        labelsAtSink = labelsAtSink.subList(0, 1); 
       
        List<VehicleVertex> vehicleVertices = new ArrayList<VehicleVertex>();
        List<VehicleArc> vehicleArcs = new ArrayList<VehicleArc>(); 
        
        for(LabelVehicle finalLabel : labelsAtSink)
        {
            boolean stop = false; 
            double totalCost = 0.0; 
            LabelVehicle currentLabel = finalLabel;
            vehicleVertices = new ArrayList<VehicleVertex>(); 
            vehicleArcs = new ArrayList<VehicleArc>();
            VehicleVertex currentVertex = sinkVertex; 
            while(!stop)
            {
                if(currentVertex.getTrip() != null)
                {
                    vehicleVertices.add(currentVertex); 
                    totalCost = totalCost + currentVertex.getTotalCostOfVertex(); 
                }
                
                LabelVehicle previousLabel = currentLabel.getSourceLabel(); 
                VehicleVertex previousVertex = currentLabel.getSourceVehicleVertex(); 
                
                if(previousLabel != null && previousVertex != null)
                {
                    VehicleArc currentArc = currentLabel.getExtendedVehicleArc(); 
                    vehicleArcs.add(currentArc); 
                    totalCost = totalCost + currentArc.getTotalCostOfArc(); 
                    
                    currentLabel = previousLabel; 
                    currentVertex = previousVertex; 
                }
                else
                {
                    stop = true; 
                }
                
            }
            
            
            List<Trip> trips = new ArrayList<Trip>(); 
            List<BlockActivity> blockElements = new ArrayList<BlockActivity>(); 
            List<Deadrun> deadruns = new ArrayList<Deadrun>();
            List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
            vehicleVertices.forEach(v -> {
                trips.add(v.getTrip());
                blockElements.add(v.getBlockActivity());  
            });
            
            Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedTrips().containsAll(trips));
            Assert.assertTrue(trips.containsAll(finalLabel.getUpdatedResources().getUpdatedTrips()));
            
             
            vehicleArcs.forEach(a -> {
                if(!a.getDeadrunsOnEdge().isEmpty())
                {
                    deadruns.addAll(a.getDeadrunsOnEdge());
                }
                
                if(!a.getBlockActivitiesOnEdge().isEmpty())
                {
                    blockElements.addAll(a.getBlockActivitiesOnEdge());
                }
                
                if(a.getIdleTimeOnArc() != null)
                {
                    idleTimes.add(a.getIdleTimeOnArc()); 
                }
            });
            Collections.sort(blockElements);
            
            for(IdleTime idleTime : idleTimes)
            {
                Optional<BlockActivity> idleActivity = blockElements.stream().filter(b -> b.getDepartureNode().equals(idleTime.getNode()) && b.getArrivalNode().equals(idleTime.getNode()) && b.getDepartureTime() == idleTime.getDepartureTime() && 
                        b.getArrivalTime() == idleTime.getArrivalTime()).findFirst();
                Assert.assertTrue(idleActivity.isPresent());
            }
            
            Block intblock = new Block(this.vehicleType, trips, deadruns, idleTimes, blockElements); 
            Assert.assertTrue(Math.abs(intblock.getTotalCostOfBlock() - totalCost) <= 1e-6);
            
            if(validateBlock(intblock))
            {
                this.blocksGenerated.add(intblock);
                
                /*for(BlockActivity ba : intblock.getBlockActivities()) {
                    System.out.println(ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() +"; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity());
                }*/
            }
            
        }
    }
    
    private VehicleVertex selectCandidate(List<VehicleVertex> vertices)
    {
        Collections.sort(vertices);
        VehicleVertex selectedVertex = vertices.get(0); 
        
        for(VehicleVertex vertex : vertices)
        {
           int incomingEdges = this.graph.inDegreeOf(vertex); 
           if(incomingEdges < 3)
           {
               selectedVertex = vertex; 
               break; 
           }
        }
        
        return selectedVertex; 
    }
    
    private boolean validateBlock(Block intblock)
    {
        double distanceToRefueling = 0; 
        for(BlockActivity blockElement : intblock.getBlockActivities())
        {
            if(blockElement.getActivity().equals("Recharging"))
            {
                distanceToRefueling = 0; 
            }
            else
            {
                distanceToRefueling = distanceToRefueling + blockElement.getDistance(); 
            }
            
            if(this.vehicleType.getMaximumDistanceWithoutRecharging() > 0 && distanceToRefueling > this.vehicleType.getMaximumDistanceWithoutRecharging())
            {
                System.out.println("Block invlaid because of maximum distance without refueling");
                throw new IllegalArgumentException();
                
            }
        }
        
        if(intblock.getBlockActivities().size() > 1)
        {
            for(int i = 0; i < intblock.getBlockActivities().size()-1 ; i++)
            {
                if(intblock.getBlockActivities().get(i).getArrivalTime() != intblock.getBlockActivities().get(i+1).getDepartureTime())
                {
                    System.out.println("The block activities are not continuous with respect to time.");
                    intblock.getBlockActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
                    throw new IllegalArgumentException();
                }
                 
                if(!intblock.getBlockActivities().get(i).getArrivalNode().equals(intblock.getBlockActivities().get(i+1).getDepartureNode()))
                {
                    System.out.println("The block activities are not continuous with respect to space");
                    intblock.getBlockActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
                    throw new IllegalArgumentException();
                }
            }
        }
        
        
        return true; 
    }

}
