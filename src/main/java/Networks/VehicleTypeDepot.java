package Networks;


import Data.Node;
import Data.VehicleType;
import lombok.Getter;

@Getter
public class VehicleTypeDepot 
{
	private VehicleType vehicleType; 
	private Node depot; 
	private String description; 
		
	public VehicleTypeDepot(VehicleType vehicleType, Node depot)
	{
		this.vehicleType = vehicleType; 
		this.depot = depot; 
		this.description = this.depot.getNodeId() + "_" + this.vehicleType.getVehicleTypeDescription(); 
	}
	

}
