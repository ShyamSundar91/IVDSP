package Networks;

import Data.DutyType;
import Data.Node;
import lombok.Getter;

@Getter
public class DutyTypeDepot 
{
	private DutyType dutyType; 
	private Node dutySignOn; 
	private String description; 
		
	public DutyTypeDepot(DutyType dutyType, Node dutySignOn)
	{
		this.dutyType = dutyType; 
		this.dutySignOn = dutySignOn; 
		this.description = this.dutySignOn.getNodeId() + "_" + this.dutyType.getDutyTypeDescription(); 
	}
	
}
