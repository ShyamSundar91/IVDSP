package Greedy;

import Data.Trip;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class GreedyDriverVertex implements Comparable<GreedyDriverVertex> {
     
    private Trip trip; 
    private Deadrun deadrun; 
    private IdleTime idleTime;
    
    int departureTime; 
    int arrivalTime;
    int cost; 

    public GreedyDriverVertex(Trip trip, Deadrun deadrun, IdleTime idleTime) 
    {
        this.trip = trip; 
        this.deadrun = deadrun; 
        this.idleTime = idleTime;
        this.cost = 0; 
        if(trip != null) {
            this.departureTime = this.trip.getDepartureTime(); 
            this.arrivalTime = this.trip.getArrivalTime(); 
        }
        
        if(deadrun != null) {
            this.departureTime = this.deadrun.getDepartureTime(); 
            this.arrivalTime = this.deadrun.getArrivalTime(); 
        }
        
        if(idleTime != null) {
            this.departureTime = this.idleTime.getDepartureTime(); 
            this.arrivalTime = this.idleTime.getArrivalTime(); 
        }
        
        this.cost = this.arrivalTime - this.departureTime; 
    }
    
    public int compareTo(GreedyDriverVertex vv) {
        
        if(this.departureTime < vv.getDepartureTime()) return -1; 
        if(this.departureTime > vv.getDepartureTime()) return 1; 
        
        return 0;
    }
}
