package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.DutyType;
import Data.Trip;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyDriverController {
    
    private DutyType dutyType; 
    private DefaultDirectedGraph<GreedyDriverVertex, GreedyDriverArc> graph; 
    @Getter
    private List<Duty> dutiesGenerated; 
    
    private GreedyDriverVertex sourceVertex; 
    private GreedyDriverVertex sinkVertex; 
    
    private List<GreedyDriverVertex> queue;
    
    public GreedyDriverController(DutyType dutyType, DefaultDirectedGraph<GreedyDriverVertex, GreedyDriverArc> graph) {
        this.dutyType = dutyType; 
        this.graph = graph; 
        this.dutiesGenerated = new ArrayList<>(); 
        
        this.queue = new ArrayList<>(); 
        this.sourceVertex = this.graph.vertexSet().stream().filter(v -> v.getVertexID() == -1).collect(Collectors.toList()).get(0); 
        this.sinkVertex = this.graph.vertexSet().stream().filter(v -> v.getVertexID() == Integer.MAX_VALUE).collect(Collectors.toList()).get(0); 
        
        initialization();
       
        algorithm(); 
    }
    
    private void initialization()
    {
        GreedyDriverREF initialREF = new GreedyDriverREF(this.dutyType, null, null, null); 
        GreedyLabelDriver initialLabel = new GreedyLabelDriver(null, null, null, initialREF); 
        this.sourceVertex.getLabels().add(initialLabel); 
        this.queue.add(sourceVertex); 
    }
    
    private void algorithm()
    {
        List<GreedyDriverVertex> currentBestVertices = new ArrayList<GreedyDriverVertex>();  
        
        while(!this.queue.isEmpty())
        {
            //Collections.sort(this.queue);
            GreedyDriverVertex selectedVertex = selectCandidate(this.queue); 
            
            currentBestVertices = new ArrayList<>();
            
            Set<GreedyDriverArc> outgoingArcs = this.graph.outgoingEdgesOf(selectedVertex).stream().collect(Collectors.toSet()); 
            
            for(GreedyLabelDriver label : selectedVertex.getLabels())
            {
                if(!label.isLabelDriverVisited())
                {
                    for(GreedyDriverArc outgoingArc : outgoingArcs)
                    {
                        
                        GreedyDriverVertex successorVertex = outgoingArc.getSuccessor(); 
                         
                        GreedyDriverREF newREF = new GreedyDriverREF(this.dutyType, label.getUpdatedResources(), outgoingArc, successorVertex);  
                        if(newREF.isValid())
                        {
                            if(successorVertex.getVertexID() == Integer.MAX_VALUE)
                            {
                                GreedyLabelDriver newLabel = new GreedyLabelDriver(label, selectedVertex, outgoingArc, newREF);
                                successorVertex.getLabels().add(newLabel); 
                                       
                            }
                            else if(successorVertex.getVertexID() != Integer.MAX_VALUE)
                            {
                                GreedyLabelDriver newLabel = new GreedyLabelDriver(label, selectedVertex, outgoingArc, newREF);
                                successorVertex.getLabels().add(newLabel); 
                                    
                                if(!currentBestVertices.contains(successorVertex))
                                {
                                    currentBestVertices.add(successorVertex); 
                                }
                            }
                        }
                    }
                }
                    
                label.labelDriverVisited();
            }
        
            this.queue.remove(selectedVertex); 
            
            if(!currentBestVertices.isEmpty()) {
                
                for(GreedyDriverVertex vertex : this.queue)
                {
                    List<GreedyLabelDriver> labelsToRemove = new ArrayList<>(); 
                    for(GreedyLabelDriver label : vertex.getLabels()) {
                        
                       if(!label.getSourceDriverVertex().equals(selectedVertex)) {
                           
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
        List<GreedyLabelDriver> labelsAtSink = new ArrayList<>(this.sinkVertex.getLabels()); 
        labelsAtSink.forEach(l -> l.calculateDelta());
        Collections.sort(labelsAtSink, Comparator.comparingDouble(GreedyLabelDriver::getDelta));
        Collections.reverse(labelsAtSink);
        labelsAtSink = labelsAtSink.subList(0, 1); 
       
        List<GreedyDriverVertex> driverVertices = new ArrayList<>();
        List<GreedyDriverArc> driverArcs = new ArrayList<>(); 
        
        for(GreedyLabelDriver finalLabel : labelsAtSink)
        {
            boolean stop = false; 
            double totalCost = 0.0; 
            GreedyLabelDriver currentLabel = finalLabel;
            driverVertices = new ArrayList<>(); 
            driverArcs = new ArrayList<>();
            GreedyDriverVertex currentVertex = sinkVertex; 
            while(!stop)
            {
                if(currentVertex.getTrip() != null)
                {
                    driverVertices.add(currentVertex); 
                    totalCost = totalCost + currentVertex.getTotalCostOfVertex(); 
                }
                
                if(currentVertex.getDeadrun() != null)
                {
                    driverVertices.add(currentVertex); 
                    totalCost = totalCost + currentVertex.getTotalCostOfVertex(); 
                }
                
                if(currentVertex.getIdleTime() != null)
                {
                    driverVertices.add(currentVertex); 
                    totalCost = totalCost + currentVertex.getTotalCostOfVertex();
                }
                
                GreedyLabelDriver previousLabel = currentLabel.getSourceLabel(); 
                GreedyDriverVertex previousVertex = currentLabel.getSourceDriverVertex(); 
                
                if(previousLabel != null && previousVertex != null)
                {
                    GreedyDriverArc currentArc = currentLabel.getExtendingDriverArc(); 
                    driverArcs.add(currentArc); 
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
            List<DutyActivity> dutyActivities = new ArrayList<>(); 
            List<Deadrun> deadruns = new ArrayList<Deadrun>();
            List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
            driverVertices.forEach(v -> {
                  if(v.getTrip() != null)
                  {
                      trips.add(v.getTrip()); 
                      dutyActivities.add(v.getDutyActivity()); 
                  }
                  
                  if(v.getDeadrun() != null)
                  {
                      deadruns.add(v.getDeadrun()); 
                      dutyActivities.add(v.getDutyActivity());
                  }
                  
                  if(v.getIdleTime() != null)
                  {
                      idleTimes.add(v.getIdleTime()); 
                      dutyActivities.add(v.getDutyActivity()); 
                  }
            });
            
            Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedTrips().containsAll(trips));
            Assert.assertTrue(trips.containsAll(finalLabel.getUpdatedResources().getUpdatedTrips()));
            
             
            driverArcs.forEach(a -> {
                if(!a.getDutyActivities().isEmpty()) {
                    dutyActivities.addAll(a.getDutyActivities()); 
                }
            });
            
            Collections.sort(dutyActivities);
            
            Duty duty = new Duty(this.dutyType, trips, deadruns, dutyActivities, idleTimes); 
            this.dutiesGenerated.add(duty); 
            /*for(DutyActivity da : duty.getDutyActivities())
            {
                System.out.println(da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() +"; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity());
            }*/
            //Assert.assertTrue((trips.isEmpty() && !deadruns.isEmpty()) || (!trips.isEmpty() && deadruns.isEmpty()) || (!trips.isEmpty() && !deadruns.isEmpty()));
            Assert.assertTrue(duty.getTotalDuration() == finalLabel.getUpdatedResources().getUpdatedTotalDuration());
            Assert.assertTrue(Math.abs(duty.getTotalCostOfDuty()-totalCost) <= 1e-6);
            //Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedTrips().containsAll(duty.getTripsInDuty()) && duty.getTripsInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedTrips()));
            //Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedDeadruns().containsAll(duty.getDeadrunsInDuty()) && duty.getDeadrunsInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedDeadruns()));
            //Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedIdleTimes().containsAll(duty.getIdleTimesInDuty()) && duty.getIdleTimesInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedIdleTimes()));
            validateDuty(duty);
            
        }
    }
    
    private GreedyDriverVertex selectCandidate(List<GreedyDriverVertex> vertices)
    {
        Collections.sort(vertices);
        GreedyDriverVertex selectedVertex = vertices.get(0); 
     
        for(GreedyDriverVertex vertex : vertices)
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
    
    private boolean validateDuty(Duty intDuty)
    {
        int durationToBreak = 0; 
        for(DutyActivity dutyActivity : intDuty.getDutyActivities())
        {
            if(dutyActivity.getActivity().equals("Break"))
            {
                durationToBreak = 0; 
            }
            else
            {
                durationToBreak = durationToBreak + dutyActivity.getDuration(); 
            }
            
            if(this.dutyType.getMaximumDurationWithoutBreak() > 0 && durationToBreak > this.dutyType.getMaximumDurationWithoutBreak())
            {
                System.out.println("Duty invlaid because of maximum duration without break");
                throw new IllegalArgumentException();
                
            }
        }
        
        if(intDuty.getTotalDuration() > this.dutyType.getMaxDuration())
        {
            System.out.println("Duty invlaid because of maximum duration");
            throw new IllegalArgumentException();
        }
        
        if(intDuty.getDutyActivities().size() > 1)
        {
            for(int i = 0; i < intDuty.getDutyActivities().size()-1 ; i++)
            {
                if(intDuty.getDutyActivities().get(i).getArrivalTime() != intDuty.getDutyActivities().get(i+1).getDepartureTime())
                {
                    System.out.println("The duty activities are not continuous with respect to time.");
                    intDuty.getDutyActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
                    throw new IllegalArgumentException();
                }
                 
                if(!intDuty.getDutyActivities().get(i).getArrivalNode().equals(intDuty.getDutyActivities().get(i+1).getDepartureNode()))
                {
                    System.out.println("The block activities are not continuous with respect to space");
                    intDuty.getDutyActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
                    throw new IllegalArgumentException();
                }
            }
        }
        
        
        return true; 
    }

}
