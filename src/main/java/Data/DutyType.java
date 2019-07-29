package Data;

import lombok.Getter;

@Getter
public class DutyType 
{
	private String dutyTypeDescription; 
	private double fixedCost; 
	private double costPerHour; 
	private int maxDuration; 
	private int minimumPaidTime; 
	private int minimumBreakDuration; 
	private int maximumDurationWithoutBreak; 
	private int maximumNumberOfBlockChanges; 
	
	public DutyType(String dutyTypeDescription, double fixedCost, double costPerHour, int maxDuration, int minimumPaidTime, int minimumBreakDuration, int maximumDurationWithoutBreak, int maximumNumberOfBlockChanges)
	{
		this.dutyTypeDescription = dutyTypeDescription; 
		this.fixedCost = fixedCost; 
		this.costPerHour = costPerHour; 
		this.maxDuration = maxDuration; 
		this.minimumPaidTime = minimumPaidTime; 
		this.minimumBreakDuration = minimumBreakDuration; 
		this.maximumDurationWithoutBreak = maximumDurationWithoutBreak; 
		this.maximumNumberOfBlockChanges = maximumNumberOfBlockChanges; 
	}

}
