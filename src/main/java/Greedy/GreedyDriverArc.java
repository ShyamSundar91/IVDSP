package Greedy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;

import Data.DutyType;
import Variables.DutyActivity;
import lombok.Getter;

@Getter
public class GreedyDriverArc {
    
    private GreedyDriverVertex predecessor; 
    private GreedyDriverVertex successor;
    
    private List<DutyActivity> dutyActivities; 
    
    private double totalCostOfArc;
    
    private int durationOfArc; 
    private DutyType dutyType; 
    
    private boolean blockChange; 
    private int startTimeOfBreak; 
    private int endTimeOfBreak; 
    private int durationBeforeBreak; 
    private int durationAfterBreak;
    
    public GreedyDriverArc(DutyType dutyType, GreedyDriverVertex predecessor, GreedyDriverVertex successor, List<DutyActivity> dutyActivities, boolean blockChange)
    {
        this.dutyType = dutyType; 
        this.predecessor = predecessor; 
        this.successor = successor; 
        
        this.startTimeOfBreak = -1; 
        this.endTimeOfBreak = -1; 
        this.durationBeforeBreak = 0; 
        this.durationAfterBreak = 0; 
        
        this.dutyActivities = new ArrayList<>();
        if(!dutyActivities.isEmpty())
        {
            this.dutyActivities.addAll(dutyActivities);
        }
        this.blockChange = blockChange; 
         
        getDuration(); 
        getBreakActivity(); 
        
        this.totalCostOfArc = ((double)this.durationOfArc/(double)60) * this.dutyType.getCostPerHour(); 
        if(this.getPredecessor().getVertexID() == -1)
        {
            this.totalCostOfArc = this.totalCostOfArc + this.dutyType.getFixedCost(); 
        }
    }
    
    private void getDuration()
    {
        if(!this.dutyActivities.isEmpty())
        {
            this.durationOfArc = this.dutyActivities.stream().mapToInt(da -> da.getDuration()).sum(); 
        }
    }
    
    private void getBreakActivity()
    {
        List<DutyActivity> breakActivity = this.dutyActivities.stream().filter(b -> b.getActivity().equals("Break")).collect(Collectors.toList()); 
        if(!breakActivity.isEmpty())
        {
            Assert.assertTrue(breakActivity.size() == 1);
            this.startTimeOfBreak = breakActivity.get(0).getDepartureTime(); 
            this.endTimeOfBreak = breakActivity.get(0).getArrivalTime(); 
            this.durationBeforeBreak = this.startTimeOfBreak - this.dutyActivities.get(0).getDepartureTime(); 
            this.durationAfterBreak = this.dutyActivities.get(this.dutyActivities.size()-1).getArrivalTime() - this.endTimeOfBreak; 
        }
        
    }

}
