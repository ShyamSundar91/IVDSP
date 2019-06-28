package Networks.DriverUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import Data.DutyType;
import Data.Node;
import Variables.BlockActivity;
import Variables.DutyActivity;
import lombok.Getter;

@Getter
public class ActivtiesWhileAttendingBus 
{
	public Node node; 
	public int departureTime;
	public int arrivalTime; 
	public DutyType dutyType; 
	public List<DutyActivity> dutyActivities; 
	private BlockActivity blockActivity; 
	public ActivtiesWhileAttendingBus(Node node, int departureTime, int arrivalTime, DutyType dutyType, List<BlockActivity> blockActivites)
	{
		this.node = node; 
		this.departureTime = departureTime; 
		this.arrivalTime = arrivalTime; 
		this.dutyType = dutyType; 
		this.dutyActivities = new ArrayList<DutyActivity>();
		
		Optional<BlockActivity> bb = blockActivites.stream().filter(b -> b.getDepartureNode().equals(this.node) && b.getArrivalNode().equals(this.node) && b.getDepartureTime() == this.departureTime && 
				b.getArrivalTime() == this.arrivalTime).findFirst();
		if(bb.isPresent())
		{
			this.blockActivity = bb.get(); 
			fillActivities(); 
		}
		else if(this.departureTime != this.arrivalTime)
		{
			throw new IllegalArgumentException();
		}
			
		 
		
		
	}
	
	private void fillActivities()
	{
		if(this.blockActivity.getActivity().equals("Idle") || this.blockActivity.getActivity().equals("Parking") || this.blockActivity.getActivity().equals("Recharging"))
		{
			int duration = this.arrivalTime - this.departureTime; 
			if(node.isDriverBreakAllowed() &&  duration >= this.dutyType.getMinimumBreakDuration())
			{
				DutyActivity breakActivity = new DutyActivity(this.node, this.node, this.departureTime, this.arrivalTime, -1, "Break"); 
				this.dutyActivities.add(breakActivity); 
			}
			else
			{
				DutyActivity regulation = new DutyActivity(this.node, this.node, this.departureTime, this.arrivalTime, -1, "Duty regulation"); 
				this.dutyActivities.add(regulation); 
			}
		}
		else
		{
			 throw new IllegalArgumentException();
		}
	}

}
