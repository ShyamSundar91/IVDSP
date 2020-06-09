package Greedy;

import Data.Trip;
import lombok.Getter;

@Getter
public class GreedyLabelDriver {
    
    private GreedyLabelDriver sourceLabel; 
    private GreedyDriverVertex sourceDriverVertex; 
    private GreedyDriverArc extendingDriverArc; 
    private GreedyDriverREF updatedResources; 
    
    private boolean labelDriverVisited; 
    
    private double delta; 
    
    public GreedyLabelDriver(GreedyLabelDriver sourceLabel, GreedyDriverVertex sourceDriverVertex, GreedyDriverArc extendingDriverArc, GreedyDriverREF updatedResources)
    {
        this.sourceLabel = sourceLabel; 
        this.sourceDriverVertex = sourceDriverVertex; 
        this.extendingDriverArc = extendingDriverArc; 
        this.updatedResources = updatedResources; 
        this.delta = Double.MAX_VALUE; 
        
        this.labelDriverVisited = false; 
    }
    
    public void calculateDelta()
    {
        this.delta = this.updatedResources.getUpdatedTotalDuration();  
    }
    
    public void labelDriverVisited()
    {
        this.labelDriverVisited = true; 
    }

}
