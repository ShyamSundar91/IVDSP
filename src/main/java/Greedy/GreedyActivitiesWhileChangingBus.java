package Greedy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import Data.DriverTravel;
import Data.DutyType;
import Data.Node;
import Variables.DutyActivity;
import lombok.Getter;

public class GreedyActivitiesWhileChangingBus {
    
    private GreedyDriverVertex predecessorVertex; 
    private GreedyDriverVertex successorVertex; 
    private DutyType dutyType; 
    private List<DriverTravel> allDriverTravels;
    private Set<Node> allNodes; 
    
    @Getter
    private List<DutyActivity> dutyActivities; 
    
    public GreedyActivitiesWhileChangingBus(GreedyDriverVertex predecessorVertex, GreedyDriverVertex successorVertex, DutyType dutyType, List<DriverTravel> allDriverTravels, Set<Node> allNodes)
    {
        this.predecessorVertex = predecessorVertex; 
        this.successorVertex = successorVertex; 
        this.dutyType = dutyType; 
        this.allDriverTravels = allDriverTravels; 
        this.allNodes = allNodes; 
        
        this.dutyActivities = new ArrayList<DutyActivity>(); 
        createActivities(); 
    }
    
    private void createActivities()
    {
        if(predecessorVertex.getArrivalNode().equals(successorVertex.getDepartureNode()) && predecessorVertex.getArrivalNode().isDriverBreakAllowed())
        {
            if((successorVertex.getDepartureTime() - predecessorVertex.getArrivalTime()) >= this.dutyType.getMinimumBreakDuration())
            {
                DutyActivity breakActivity = new DutyActivity(predecessorVertex.getArrivalNode(), predecessorVertex.getArrivalNode(), predecessorVertex.getArrivalTime(), successorVertex.getDepartureTime(), -1, "Break"); 
                this.dutyActivities.add(breakActivity);
            }
            
        }
        else if(!predecessorVertex.getArrivalNode().equals(successorVertex.getDepartureNode()) && successorVertex.getDepartureNode().isDriverBreakAllowed())
        {
            Optional<DriverTravel> travelToSecondTrip = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(predecessorVertex.getArrivalNode()) && t.getArrivalNode().equals(successorVertex.getDepartureNode())).findAny(); 
            if(travelToSecondTrip.isPresent())
            {
                int endTimeOfTravelToSecondTrip = predecessorVertex.getArrivalTime() + travelToSecondTrip.get().getDuration(); 
                
                DutyActivity travelActivity = new DutyActivity(travelToSecondTrip.get().getDepartureNode(), travelToSecondTrip.get().getArrivalNode(), predecessorVertex.getArrivalTime(), endTimeOfTravelToSecondTrip, -1, travelToSecondTrip.get().getTravelType()); 
                this.dutyActivities.add(travelActivity); 
                
                if((successorVertex.getDepartureTime() - endTimeOfTravelToSecondTrip) >= this.dutyType.getMinimumBreakDuration())
                {
                    DutyActivity breakActivity = new DutyActivity(successorVertex.getDepartureNode(), successorVertex.getDepartureNode(), endTimeOfTravelToSecondTrip, successorVertex.getDepartureTime(), -1, "Break"); 
                    this.dutyActivities.add(breakActivity);
                }
                else
                {
                    this.dutyActivities.clear();
                }
            }
                    
        }
        else
        {
            Node bestBreakNode = null; 
            int minTotalDuration = Integer.MAX_VALUE; 
            Set<Node> breakNodes = this.allNodes.stream().filter(n -> n.isDriverBreakAllowed()).collect(Collectors.toSet()); 
            for(Node breakNode : breakNodes)
            {
                int durationToBreakNode = Integer.MAX_VALUE; 
                int durationFromBreakNode = Integer.MAX_VALUE; 
                if(predecessorVertex.getArrivalNode().equals(breakNode))
                {
                    durationToBreakNode = 0; 
                }
                else
                {
                    Optional<DriverTravel> travelToBreakNode = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(predecessorVertex.getArrivalNode()) && t.getArrivalNode().equals(breakNode)).findFirst(); 
                    if(travelToBreakNode.isPresent())
                    {
                        durationToBreakNode = travelToBreakNode.get().getDuration(); 
                    }
                }
                
                Optional<DriverTravel> travelFromBreakNode = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(breakNode) && t.getArrivalNode().equals(successorVertex.getDepartureNode())).findFirst(); 
                if(travelFromBreakNode.isPresent())
                {
                    durationFromBreakNode = travelFromBreakNode.get().getDuration(); 
                }
                
                if(durationToBreakNode != Integer.MAX_VALUE && durationFromBreakNode != Integer.MAX_VALUE)
                {
                    int totalDuration = durationToBreakNode + durationFromBreakNode; 
                    if(totalDuration < minTotalDuration)
                    {
                        minTotalDuration = totalDuration; 
                        bestBreakNode = breakNode; 
                    }
                }
            }
            
            if(bestBreakNode != null)
            {
                Node bestNode = bestBreakNode; 
                int endTimeOfTravelToBreakNode = predecessorVertex.getArrivalTime(); 
                if(!bestNode.equals(predecessorVertex.getArrivalNode()))
                {
                    DriverTravel travelToBreakNode = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(predecessorVertex.getArrivalNode()) && t.getArrivalNode().equals(bestNode)).findFirst().get();
                    endTimeOfTravelToBreakNode = endTimeOfTravelToBreakNode + travelToBreakNode.getDuration(); 
                    
                    DutyActivity travelActivityToBreakNode = new DutyActivity(travelToBreakNode.getDepartureNode(), travelToBreakNode.getArrivalNode(), predecessorVertex.getArrivalTime(), endTimeOfTravelToBreakNode, -1, travelToBreakNode.getTravelType()); 
                    this.dutyActivities.add(travelActivityToBreakNode); 
                }
                
                int startTimeOfTravelFromBreakNode = successorVertex.getDepartureTime(); 
                DriverTravel travelFromBreakNode = this.allDriverTravels.stream().filter(t -> t.getDepartureNode().equals(bestNode) && t.getArrivalNode().equals(successorVertex.getDepartureNode())).findFirst().get(); 
                startTimeOfTravelFromBreakNode = startTimeOfTravelFromBreakNode - travelFromBreakNode.getDuration(); 
                
                DutyActivity travelActivityFromBreakNode = new DutyActivity(travelFromBreakNode.getDepartureNode(), travelFromBreakNode.getArrivalNode(), startTimeOfTravelFromBreakNode, successorVertex.getDepartureTime(), -1, travelFromBreakNode.getTravelType()); 
                this.dutyActivities.add(travelActivityFromBreakNode); 
                
                if((startTimeOfTravelFromBreakNode - endTimeOfTravelToBreakNode) >= this.dutyType.getMinimumBreakDuration())
                {
                    DutyActivity breakActivity = new DutyActivity(bestNode, bestNode, endTimeOfTravelToBreakNode, startTimeOfTravelFromBreakNode, -1, "Break"); 
                    this.dutyActivities.add(breakActivity); 
                }
                else
                {
                    this.dutyActivities.clear();
                }
            }
        }
        
        Collections.sort(this.dutyActivities);
    }

}
