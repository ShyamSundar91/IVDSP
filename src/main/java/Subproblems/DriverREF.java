package Subproblems;

import java.util.ArrayList;
import java.util.List;

import Data.DutyType;
import Data.Trip;
import Networks.DriverArc;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

@Getter
public class DriverREF 
{
	private DutyType dutyType; 
	private DriverREF previousREF; 
	private DriverArc extendingArc; 
	private int maxNumberOfBlockChanges; 
	
	private boolean attendedBus;
	private double updatedTotalCost; 
	private double updatedReducedCost; 
	private int updatedTotalDuration; 
	private int updatedDurationWithoutBreak; 
	private int updatedNumberOfBlockChanges; 
	private List<Trip> updatedTrips; 
	//private List<Deadrun> updatedDeadruns; 
	//private List<IdleTime> updatedIdleTimes; 
	
	public DriverREF(DutyType dutyType, DriverREF previousREF, DriverArc extendingArc, int maxNumberOfBlockChanges)
	{
		this.dutyType = dutyType; 
		this.previousREF = previousREF; 
		this.extendingArc = extendingArc;
		this.maxNumberOfBlockChanges = maxNumberOfBlockChanges; 
		
		if(this.previousREF == null)
		{
			initialize(); 
		}
		else
		{
		    updatedTotalCost(); 
			updateAttendedBus(); 
			updateReducedCost(); 
			updateTrips(); 
			//updateDeadruns(); 
			//updateIdleTimes(); 
		}
	}
	
	private void initialize()
	{
	    this.updatedTotalCost = 0; 
		this.updatedReducedCost = 0.0; 
		this.updatedTotalDuration = 0; 
		this.updatedDurationWithoutBreak = 0;
		this.updatedNumberOfBlockChanges = 0; 
		this.attendedBus = true;
		this.updatedTrips = new ArrayList<Trip>(); 
		//this.updatedDeadruns = new ArrayList<Deadrun>(); 
		//this.updatedIdleTimes = new ArrayList<IdleTime>(); 
	}
	
	private void updateTrips()
	{
		this.updatedTrips = new ArrayList<Trip>(); 
		if(previousREF != null)
		{
			this.updatedTrips.addAll(previousREF.getUpdatedTrips()); 
			if(this.extendingArc.getTrip() != null)
			{
				this.updatedTrips.add(this.extendingArc.getTrip()); 
			}
		}
	}
	
	/*private void updateDeadruns()
	{
		this.updatedDeadruns = new ArrayList<Deadrun>(); 
		if(this.previousREF != null)
		{
			this.updatedDeadruns.addAll(this.previousREF.getUpdatedDeadruns()); 
			if(this.extendingArc.getDeadrun() != null)
			{
				this.updatedDeadruns.add(this.extendingArc.getDeadrun()); 
			}
		}
	}
	
	private void updateIdleTimes()
	{
		this.updatedIdleTimes = new ArrayList<IdleTime>(); 
		if(this.previousREF != null)
		{
			this.updatedIdleTimes.addAll(this.previousREF.getUpdatedIdleTimes()); 
			if(this.extendingArc.getIdleTimeOnArc() != null)
			{
				this.updatedIdleTimes.add(this.extendingArc.getIdleTimeOnArc()); 
			}
		}
	}*/
	
	private void updateAttendedBus()
	{
		this.attendedBus = this.extendingArc.isAttendingBus(); 
	}
	
	private void updatedTotalCost()
	{
	    this.updatedTotalCost = this.previousREF.getUpdatedTotalCost() + this.extendingArc.getTotalCostOfArc(); 
	}
	
	private void updateReducedCost()
	{
		this.updatedReducedCost = this.previousREF.getUpdatedReducedCost() + this.extendingArc.getReducedCostOfArc(); 
	}
	
	public void updatedReducedCostWithRespectToMinPaidTime(double redCost)
	{
		this.updatedReducedCost = redCost; 
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
		this.updatedTotalDuration =  this.previousREF.getUpdatedTotalDuration() + this.extendingArc.getDurationOfArc(); 
		
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
		
		return true; 
	}
	
	private boolean checkMaxNumberOfBlockChanges()
	{
		this.updatedNumberOfBlockChanges = this.previousREF.getUpdatedNumberOfBlockChanges(); 
		
		if(this.extendingArc.isChangingBus())
		{
			this.updatedNumberOfBlockChanges = this.updatedNumberOfBlockChanges + 1; 
		}
		
		if(this.updatedNumberOfBlockChanges > this.maxNumberOfBlockChanges /*this.dutyType.getMaximumNumberOfBlockChanges()*/)
		{
			return false; 
		}
		
		return true; 
	}
}
