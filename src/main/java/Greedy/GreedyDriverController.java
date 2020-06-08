package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Subproblems.DriverREF;
import Subproblems.LabelDriver;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

public class GreedyDriverController {
    
    private DutyTypeDepot dutyTypeDepot; 
    private DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph; 
    
    private List<Trip> uncoveredTrips; 
    private Set<Deadrun> uncoveredDeadruns; 
    private Set<IdleTime> uncoveredIdleTimes;
    
    private DriverVertex sourceVertex; 
    private DriverVertex sinkVertex; 
    private List<DriverVertex> queue; 
    
    @Getter
    private List<Duty> dutiesGenerated; 
 
    public GreedyDriverController(DutyTypeDepot dutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph, List<Trip> uncoveredTrips, Set<Deadrun> uncoveredDeadruns, Set<IdleTime> uncoveredIdleTimes) {
        this.dutyTypeDepot = dutyTypeDepot; 
        this.driverGraph = driverGraph; 
        
        this.uncoveredDeadruns = uncoveredDeadruns; 
        this.uncoveredIdleTimes = uncoveredIdleTimes; 
        this.uncoveredTrips = uncoveredTrips; 
        
        this.sourceVertex = this.driverGraph.vertexSet().stream().filter(v -> v.getCurrentTime() == -1).findFirst().get(); 
        this.sinkVertex = this.driverGraph.vertexSet().stream().filter(v -> v.getCurrentTime() == Integer.MAX_VALUE).findFirst().get(); 
        this.queue = new ArrayList<DriverVertex>();
        this.dutiesGenerated = new ArrayList<Duty>(); 
        
        initialization();
        algorithm();
    }
    
    private void initialization()
    {
        DriverREF initialREF = new DriverREF(this.dutyTypeDepot.getDutyType(), null, null, this.dutyTypeDepot.getDutyType().getMaximumNumberOfBlockChanges()); 
        LabelDriver initialLabel = new LabelDriver(null, null, null, initialREF);
        this.sourceVertex.getLabels().add(initialLabel); 
        this.queue.add(this.sourceVertex); 
    }
    
    private void algorithm()
    {
        List<DriverVertex> currentBestVertices = new ArrayList<DriverVertex>(); 
        while(!this.queue.isEmpty())
        {
            DriverVertex selectedVertex = null; 
            
            Collections.sort(this.queue);
            selectedVertex = this.queue.get(0); 
                
            currentBestVertices = new ArrayList<DriverVertex>(); 
            
            Set<DriverArc> outgoingArcs = this.driverGraph.outgoingEdgesOf(selectedVertex); 
            
            List<LabelDriver> labels = selectedVertex.getLabels(); 
            for(LabelDriver selectedLabel : labels)
            {
                if(!selectedLabel.isLabelDriverVisited())
                {
                    
                    for(DriverArc outgoingArc : outgoingArcs)
                    {
                        boolean checkArc = true; 
                        if(!selectedLabel.getUpdatedResources().isAttendedBus() && !outgoingArc.isAttendingBus()) {
                            checkArc = false;
                        }
                        
                        if(checkArc) {
                            DriverVertex successorVertex = outgoingArc.getSuccessorVertex(); 
                            
                            DriverREF newREF = new DriverREF(this.dutyTypeDepot.getDutyType(), selectedLabel.getUpdatedResources(), outgoingArc, this.dutyTypeDepot.getDutyType().getMaximumNumberOfBlockChanges()); 
                            
                            if(newREF.isValid())
                            {
                                
                                if(successorVertex.getCurrentTime() == Integer.MAX_VALUE)
                                {
                                    LabelDriver newLabel = new LabelDriver(selectedLabel, selectedVertex, outgoingArc, newREF); 
                                    if(!newLabel.getUpdatedResources().getUpdatedTrips().isEmpty()) {
                                        successorVertex.getLabels().add(newLabel); 
                                        //foundDuty = true;
                                    }
                                    
                                }
                                else if(successorVertex.getCurrentTime() != Integer.MAX_VALUE)
                                {
                                    boolean addNewLabel = true; 
                                   /* if(successorVertex.getTrip()!= null && successorVertex.isDeparture() && !this.uncoveredTrips.contains(successorVertex.getTrip())) {
                                        addNewLabel = false;
                                    }
                                    else if(successorVertex.getDeadrun() != null && successorVertex.isDeparture() && !this.uncoveredDeadruns.contains(successorVertex.getDeadrun())) {
                                        addNewLabel = false; 
                                    }*/
                                    
                                    if(addNewLabel) {
                                        LabelDriver newLabel = new LabelDriver(selectedLabel, selectedVertex, outgoingArc, newREF); 
                                        successorVertex.getLabels().add(newLabel); 
                                        
                                        if(!currentBestVertices.contains(successorVertex))
                                        {
                                            currentBestVertices.add(successorVertex); 
                                        }
                                        
                                        
                                    }
                                   
                                 
                                }
                                
                            }
                        }
                        
                        
                    }
                    selectedLabel.labelDriverVisited();
                }
                    
            }
            this.queue.remove(selectedVertex); 
            
            if(!currentBestVertices.isEmpty()) {
                
                for(DriverVertex vertex : this.queue)
                {
                    List<LabelDriver> labelsToRemove = new ArrayList<>(); 
                    for(LabelDriver label : vertex.getLabels()) {
                        
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
        
        retrievePaths(); 
    }
    
    private void retrievePaths()
    {
        List<LabelDriver> labelsAtSink = new ArrayList<LabelDriver>(this.sinkVertex.getLabels()); 
        labelsAtSink.forEach(l -> l.calculateDelta());
        Collections.sort(labelsAtSink, Comparator.comparingDouble(LabelDriver::getDelta));
        labelsAtSink = labelsAtSink.subList(0, 1); 
        List<DriverArc> driverArcs = new ArrayList<DriverArc>();
        
        for(LabelDriver finalLabel : labelsAtSink)
        {
            boolean stop = false; 
            LabelDriver currentLabel = finalLabel;

            driverArcs = new ArrayList<DriverArc>();
            
            while(!stop)
            {
    
                LabelDriver previousLabel = currentLabel.getSourceLabel(); 
                DriverVertex previousVertex = currentLabel.getSourceDriverVertex();  
                
                if(previousLabel != null && previousVertex != null)
                {
                    DriverArc curretnArc = currentLabel.getExtendingDriverArc(); 
                    driverArcs.add(curretnArc); 
                    
                    currentLabel = previousLabel;  
                }
                else
                {
                    stop = true; 
                }
                
            }
            
            List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
            List<Trip> trips = new ArrayList<Trip>(); 
            List<Deadrun> deadruns = new ArrayList<Deadrun>(); 
            List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
            double totalCost = 0.0; 
            for(DriverArc arc : driverArcs)
            {
                totalCost = totalCost + arc.getTotalCostOfArc(); 
                if(!arc.getDutyActivities().isEmpty())
                {
                    dutyActivities.addAll(arc.getDutyActivities()); 
                }
                
                if(arc.getTrip() != null)
                {
                    trips.add(arc.getTrip()); 
                }
                
                if(arc.getDeadrun() != null)
                {
                    deadruns.add(arc.getDeadrun()); 
                }
                
                if(arc.getIdleTimeOnArc() != null)
                {
                    idleTimes.add(arc.getIdleTimeOnArc()); 
                }
            }
              
            for(IdleTime idleTime : idleTimes)
            {
                Optional<DutyActivity> idleActivity = dutyActivities.stream().filter(b -> (b.getDepartureNode().equals(idleTime.getNode()) && b.getArrivalNode().equals(idleTime.getNode()) && b.getDepartureTime() == idleTime.getDepartureTime() && b.getArrivalTime() == idleTime.getArrivalTime()) && (b.getActivity().equals("Break") || b.getActivity().equals("Duty regulation"))).findFirst();
                Assert.assertTrue(idleActivity.isPresent());
            }
            
            Collections.sort(dutyActivities);
            Duty duty = new Duty(this.dutyTypeDepot.getDutyType(), trips, deadruns, dutyActivities, idleTimes); 
            this.dutiesGenerated.add(duty); 
            Assert.assertTrue((trips.isEmpty() && !deadruns.isEmpty()) || (!trips.isEmpty() && deadruns.isEmpty()) || (!trips.isEmpty() && !deadruns.isEmpty()));
            Assert.assertTrue(duty.getTotalDuration() == finalLabel.getUpdatedResources().getUpdatedTotalDuration());
            Assert.assertTrue(Math.abs(duty.getTotalCostOfDuty()-totalCost) <= 1e-6);
            for(DutyActivity dutyActivity : duty.getDutyActivities())
            {
                System.out.println(dutyActivity.getDepartureNode().getNodeId() + "; " + dutyActivity.getArrivalNode().getNodeId() + "; " + dutyActivity.getDepartureTime() + "; " + dutyActivity.getArrivalTime() + "; " + dutyActivity.getActivity());
            }

            validateDuty(duty); 
        }
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
            
            if(this.dutyTypeDepot.getDutyType().getMaximumDurationWithoutBreak() > 0 && durationToBreak > this.dutyTypeDepot.getDutyType().getMaximumDurationWithoutBreak())
            {
                System.out.println("Duty invlaid because of maximum duration without break");
                throw new IllegalArgumentException();
                
            }
        }
        
        if(intDuty.getTotalDuration() > this.dutyTypeDepot.getDutyType().getMaxDuration())
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
