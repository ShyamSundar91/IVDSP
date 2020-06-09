package Greedy;

import java.util.ArrayList;
import java.util.List;

import Data.DutyType;
import Data.Trip;
import lombok.Getter;

@Getter
public class GreedyDriverREF {
    
    private DutyType dutyType; 
    private GreedyDriverREF previousREF; 
    private GreedyDriverArc extendingArc; 
    private GreedyDriverVertex sucessorVertex; 
    
    private double updatedTotalCost;  
    private int updatedTotalDuration; 
    private int updatedDurationWithoutBreak; 
    private int updatedNumberOfBlockChanges; 
    private List<Trip> updatedTrips; 
 
    
    public GreedyDriverREF(DutyType dutyType, GreedyDriverREF previousREF, GreedyDriverArc extendingArc, GreedyDriverVertex sucessorVertex)
    {
        this.dutyType = dutyType; 
        this.previousREF = previousREF; 
        this.extendingArc = extendingArc;
        this.sucessorVertex = sucessorVertex; 
        
        if(this.previousREF == null)
        {
            initialize(); 
        }
        else
        {
            updatedTotalCost(); 
            updateTrips();  
        }
    }
    
    private void initialize()
    {
        this.updatedTotalCost = 0;
        this.updatedTotalDuration = 0; 
        this.updatedDurationWithoutBreak = 0;
        this.updatedNumberOfBlockChanges = 0; 
        this.updatedTrips = new ArrayList<Trip>();  
    }
    
    private void updateTrips()
    {
        this.updatedTrips = new ArrayList<Trip>(); 
        if(previousREF != null)
        {
            this.updatedTrips.addAll(previousREF.getUpdatedTrips()); 
            if(this.sucessorVertex.getTrip() != null)
            {
                this.updatedTrips.add(this.sucessorVertex.getTrip()); 
            }
        }
    }
      
    private void updatedTotalCost()
    {
        this.updatedTotalCost = this.previousREF.getUpdatedTotalCost() + this.extendingArc.getTotalCostOfArc() + this.sucessorVertex.getTotalCostOfVertex(); 
    }
    
      
    public boolean isValid()
    {
        if(!checkMaxDuration())
        {
            return false; 
        }
        
        if(!checkMaxDurationWithoutBreak())
        {
            return false; 
        }
        
        if(!checkMaxNumberOfBlockChanges())
        {
            return false; 
        }
        
        return true; 
    }
    
    private boolean checkMaxDuration()
    {
        this.updatedTotalDuration =  this.previousREF.getUpdatedTotalDuration() + this.extendingArc.getDurationOfArc() + this.sucessorVertex.getDurationOfVertex(); 
        
        if(this.updatedTotalDuration > this.dutyType.getMaxDuration())
        {
            return false; 
        }
        
        return true; 
    }
    
    private boolean checkMaxDurationWithoutBreak()
    {
        if(this.extendingArc.getStartTimeOfBreak() > -1)
        {
            this.updatedDurationWithoutBreak = this.previousREF.getUpdatedDurationWithoutBreak() + this.extendingArc.getDurationBeforeBreak(); 
            if(this.updatedDurationWithoutBreak > this.dutyType.getMaximumDurationWithoutBreak())
            {
                return false; 
            }
            
            this.updatedDurationWithoutBreak = 0; 
            this.updatedDurationWithoutBreak = this.extendingArc.getDurationAfterBreak(); 
            if(this.updatedDurationWithoutBreak > this.dutyType.getMaximumDurationWithoutBreak())
            {
                return false; 
            }
        }
        else
        {
            this.updatedDurationWithoutBreak = this.previousREF.getUpdatedDurationWithoutBreak() + this.extendingArc.getDurationOfArc(); 
            if(this.updatedDurationWithoutBreak > this.dutyType.getMaximumDurationWithoutBreak())
            {
                return false; 
            }
        }
        
        if(this.sucessorVertex.getStartTimeOfBreak() > -1)
        {
            this.updatedDurationWithoutBreak = 0; 
        }
        else
        {
            this.updatedDurationWithoutBreak = this.updatedDurationWithoutBreak + this.sucessorVertex.getDurationOfVertex(); 
            if(this.updatedDurationWithoutBreak > this.dutyType.getMaximumDurationWithoutBreak())
            {
                return false; 
            }
        }
        
        return true; 
    }
    
    private boolean checkMaxNumberOfBlockChanges()
    {
        this.updatedNumberOfBlockChanges = this.previousREF.getUpdatedNumberOfBlockChanges(); 
        
        if(this.extendingArc.isBlockChange())
        {
            this.updatedNumberOfBlockChanges = this.updatedNumberOfBlockChanges + 1; 
        }
        
        if(this.updatedNumberOfBlockChanges > this.dutyType.getMaximumNumberOfBlockChanges())
        {
            return false; 
        }
        
        return true; 
    }

}
