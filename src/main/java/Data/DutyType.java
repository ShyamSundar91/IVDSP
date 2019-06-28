package Data;

import lombok.Getter;

@Getter
public class DutyType 
{
	private String dutyTypeDescription; 
	private double costPerHour; 
	private int maxDuration; 
	private int minimumPaidTime; 
	private int minimumBreakDuration; 
	private int maximumDurationWithoutBreak; 
	
	public DutyType(String dutyTypeDescription, double costPerHour, int maxDuration, int minimumPaidTime, int minimumBreakDuration, int maximumDurationWithoutBreak)
	{
		this.dutyTypeDescription = dutyTypeDescription; 
		this.costPerHour = costPerHour; 
		this.maxDuration = maxDuration; 
		this.minimumPaidTime = minimumPaidTime; 
		this.minimumBreakDuration = minimumBreakDuration; 
		this.maximumDurationWithoutBreak = maximumDurationWithoutBreak; 
	}

}
