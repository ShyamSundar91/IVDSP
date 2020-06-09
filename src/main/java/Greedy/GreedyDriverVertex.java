package Greedy;

import java.util.ArrayList;
import java.util.List;

import Data.DutyType;
import Data.Node;
import Data.Trip;
import Subproblems.LabelVehicle;
import Variables.Deadrun;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class GreedyDriverVertex implements Comparable<GreedyDriverVertex> {
     
    private int vertexID; 
    private Trip trip; 
    private Deadrun deadrun; 
    private IdleTime idleTime;
    
    private Node departureNode; 
    private Node arrivalNode; 
    private int departureTime; 
    private int arrivalTime;
    private int durationOfVertex;
    
    private DutyType dutyType; 
    
    private DutyActivity dutyActivity; 
    
    private double totalCostOfVertex; 
    private int startTimeOfBreak; 
    private int endTimeOfBreak; 
    
    private List<GreedyLabelDriver> labels;

    public GreedyDriverVertex(int vertexId, DutyType dutyType, Trip trip, Deadrun deadrun, IdleTime idleTime) 
    {
        this.vertexID = vertexId; 
        this.trip = trip; 
        this.deadrun = deadrun; 
        this.idleTime = idleTime;
        this.durationOfVertex = 0; 
        this.departureTime = Integer.MAX_VALUE; 
        this.arrivalTime = Integer.MAX_VALUE; 
        this.startTimeOfBreak = -1; 
        this.endTimeOfBreak = -1; 
        this.dutyActivity = null; 
        this.dutyType = dutyType; 
        
        this.labels = new ArrayList<>(); 
        
        
        calulateDuration(); 
        this.totalCostOfVertex = ((double)this.durationOfVertex/(double)60) * this.dutyType.getCostPerHour();
    }
    
    private void calulateDuration()
    {
        if(trip != null) {
            this.departureTime = this.trip.getDepartureTime(); 
            this.arrivalTime = this.trip.getArrivalTime(); 
            this.departureNode = this.trip.getDepartureNode(); 
            this.arrivalNode = this.trip.getArrivalNode(); 
            this.durationOfVertex = this.arrivalTime - this.departureTime;
            this.dutyActivity = new DutyActivity(this.trip.getDepartureNode(), this.trip.getArrivalNode(), this.departureTime, this.arrivalTime, this.trip.getTripId(), "Trip"); 
        }
        
        if(deadrun != null) {
            this.departureTime = this.deadrun.getDepartureTime(); 
            this.arrivalTime = this.deadrun.getArrivalTime(); 
            this.departureNode = this.deadrun.getDepartureNode(); 
            this.arrivalNode = this.deadrun.getArrivalNode(); 
            this.durationOfVertex = this.arrivalTime - this.departureTime;
            this.dutyActivity = new DutyActivity(this.deadrun.getDepartureNode(), this.deadrun.getArrivalNode(), this.departureTime, this.arrivalTime, this.deadrun.getDeadrunId(), this.deadrun.getType()); 
        }
        
        if(idleTime != null) {
            this.departureTime = this.idleTime.getDepartureTime(); 
            this.arrivalTime = this.idleTime.getArrivalTime(); 
            this.departureNode = this.idleTime.getNode(); 
            this.arrivalNode = this.idleTime.getNode(); 
            this.durationOfVertex = this.arrivalTime - this.departureTime;
            
            if(this.durationOfVertex >= this.dutyType.getMinimumBreakDuration())
            {
                this.dutyActivity = new DutyActivity(this.idleTime.getNode(), this.idleTime.getNode(), this.departureTime, this.arrivalTime, -1, "Break"); 
                this.startTimeOfBreak = this.departureTime; 
                this.endTimeOfBreak = this.arrivalTime; 
            }
            else
            {
                this.dutyActivity = new DutyActivity(this.idleTime.getNode(), this.idleTime.getNode(), this.departureTime, this.arrivalTime, -1, "Duty Regulation");
            }
        }
    }
    
    public int compareTo(GreedyDriverVertex vv) {
        
        if(this.departureTime < vv.getDepartureTime()) return -1; 
        if(this.departureTime > vv.getDepartureTime()) return 1; 
        
        return 0;
    }
}
