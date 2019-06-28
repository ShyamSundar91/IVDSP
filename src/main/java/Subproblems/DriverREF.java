package Subproblems;

import Data.DutyType;
import Networks.DriverArc;
import Networks.DriverVertex;
import lombok.Getter;

@Getter
public class DriverREF 
{
	private DutyType dutyType; 
	private DriverREF previousREF; 
	private DriverArc extendingArc; 
	private DriverVertex succeedingVertex; 
	
	private boolean attendedBus; 
	private double updatedReducedCost; 
	private int updatedTotalDuration; 
	private int updatedDurationWithoutBreak; 
	
	public DriverREF(DutyType dutyType, DriverREF previousREF, DriverArc extendingArc)
	{
		this.dutyType = dutyType; 
		this.previousREF = previousREF; 
		this.extendingArc = extendingArc;
		
		if(this.previousREF == null)
		{
			initialize(); 
		}
		else
		{
			updateAttendedBus(); 
			updateReducedCost(); 
		}
	}
	
	private void initialize()
	{
		this.updatedReducedCost = 0.0; 
		this.updatedTotalDuration = 0; 
		this.updatedDurationWithoutBreak = 0;
		this.attendedBus = true;
	}
	
	private void updateAttendedBus()
	{
		this.attendedBus = this.extendingArc.isAttendingBus(); 
	}
	
	private void updateReducedCost()
	{
		this.updatedReducedCost = this.previousREF.getUpdatedReducedCost() + this.extendingArc.getReducedCostOfArc(); 
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
	
	

}
