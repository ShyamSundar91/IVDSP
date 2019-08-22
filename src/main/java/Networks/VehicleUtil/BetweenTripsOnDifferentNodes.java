package Networks.VehicleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Data.VehicleType;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.IdleTime;
import lombok.Getter;

public class BetweenTripsOnDifferentNodes 
{
	private Trip trip1; 
	private Trip trip2; 
	private VehicleType vehicleType; 
	private Set<Deadrun> deadruns; 
	private Set<IdleTime> idleTimes; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Set<Node> allNodes; 

	@Getter
	private List<BlockActivity> blockActivitiesOnArc; 
	@Getter
	private List<Deadrun> deadrunsOnArc; 
	@Getter
	private IdleTime idleTimeOnArc; 
	
	public BetweenTripsOnDifferentNodes(Trip trip1, Trip trip2, VehicleType vehicleType, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Set<VehicleTravel> allVehicleTravels, Set<Node> allNodes)
	{
		this.trip1 = trip1; 
		this.trip2 = trip2; 
		this.vehicleType = vehicleType; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		this.allVehicleTravels = allVehicleTravels; 
		this.allNodes = allNodes; 
		
		this.blockActivitiesOnArc = new ArrayList<BlockActivity>(); 
		this.deadrunsOnArc = new ArrayList<Deadrun>(); 
		this.idleTimeOnArc = null; 
	}
	
	public List<BlockActivity> getBlockActivities()
	{
		boolean advancedTravel = false; 
		Optional<VehicleTravel> travelToTrip2 = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(this.trip1.getArrivalNode()) && t.getArrivalNode().equals(this.trip2.getDepartureNode())).findFirst();
		if(travelToTrip2.isPresent())
		{
			int endTimeOfTravelToTrip2Node = this.trip1.getArrivalTime() + travelToTrip2.get().getDuration(); 
			Deadrun deadrun1 = new Deadrun(this.trip1.getArrivalNode(), this.trip2.getDepartureNode(), this.trip1.getArrivalTime(), endTimeOfTravelToTrip2Node, travelToTrip2.get().getDistance(), false, false, trip1.getTripId(), trip2.getTripId()); 
			//if(!this.deadruns.contains(deadrun1))
			{
				//this.deadruns.add(deadrun1); 
				this.deadrunsOnArc.add(deadrun1); 
			}
			/*else
			{
				Optional<Deadrun> existingDeadrun = this.deadruns.stream().filter(d -> d.equals(deadrun1)).findAny(); 
				this.deadrunsOnArc.add(existingDeadrun.get()); 
			}*/
			
			BlockActivity activityTravelToTrip2 = new BlockActivity(this.trip1.getArrivalNode(), this.trip2.getDepartureNode(), this.trip1.getArrivalTime(), endTimeOfTravelToTrip2Node, travelToTrip2.get().getDistance(), this.deadrunsOnArc.get(0).getDeadrunId(), "Deadrun"); 
			this.blockActivitiesOnArc.add(activityTravelToTrip2); 
			
			int maxIdleTime = this.trip2.getDepartureNode().getMaxIdleTime(); 
			int minIdleTime = this.trip2.getDepartureNode().getMinIdleTime(); 
			
			if(((this.trip2.getDepartureTime() - endTimeOfTravelToTrip2Node) <= maxIdleTime) && ((this.trip2.getDepartureTime() - endTimeOfTravelToTrip2Node) >= minIdleTime))
			{
				BlockActivity blockActivity = new BlockActivity(trip2.getDepartureNode(), trip2.getDepartureNode(), endTimeOfTravelToTrip2Node, trip2.getDepartureTime(), 0, -1 , "Idle"); 
				this.blockActivitiesOnArc.add(blockActivity); 	 
				IdleTime idle = new IdleTime(trip2.getDepartureNode(), endTimeOfTravelToTrip2Node, trip2.getDepartureTime(), trip1, trip2); 
				//if(!this.idleTimes.contains(idle))
				{
					this.idleTimes.add(idle); 
					this.idleTimeOnArc = idle; 
				}
				/*else
				{
					Optional<IdleTime> existingIdleTime = this.idleTimes.stream().filter(i -> i.equals(idle)).findFirst(); 
					this.idleTimeOnArc = existingIdleTime.get(); 
				}*/
				
				//if(!this.deadruns.contains(deadrun1))
				{
					this.deadruns.add(deadrun1); 
				}
			}
			else if(((this.trip2.getDepartureTime() - endTimeOfTravelToTrip2Node) > maxIdleTime))
			{
				this.blockActivitiesOnArc.clear();
				this.deadrunsOnArc.clear();
				this.idleTimeOnArc = null; 
				advancedTravel = true; 
			}
			else
			{
				this.blockActivitiesOnArc.clear(); 
				return this.blockActivitiesOnArc; 
			}
		}
		else
		{
			advancedTravel = true; 
		}
		
		if(advancedTravel)
		{
			List<Node> parkingOrRecgargingNodes = new ArrayList<Node>(); 
			boolean recharging = false; 
			if(this.vehicleType.getMaximumDistanceWithoutRecharging() > 0)
			{
				parkingOrRecgargingNodes = this.allNodes.stream().filter(n -> n.isDepot()).collect(Collectors.toList()); 
				recharging = true; 
			}
			else
			{
				parkingOrRecgargingNodes = this.allNodes.stream().filter(n -> n.isDepot()).collect(Collectors.toList()); 
			}
			
			Node bestNode = null; 
			double minDuration = Double.MAX_VALUE; 
			for(Node parkingOrRechargingNode : parkingOrRecgargingNodes)
			{
				Optional<VehicleTravel> travelToParking = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(trip1.getArrivalNode()) && t.getArrivalNode().equals(parkingOrRechargingNode)).findAny();
				Optional<VehicleTravel> travelFromParking = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(parkingOrRechargingNode) && t.getArrivalNode().equals(trip2.getDepartureNode())).findAny(); 
				if(travelToParking.isPresent() && travelFromParking.isPresent())
				{
					double totalDuration = travelToParking.get().getDuration() + travelFromParking.get().getDuration(); 
					if(totalDuration < minDuration)
					{
						minDuration = totalDuration; 
						bestNode = parkingOrRechargingNode; 
					}
				}
			}
			
			if(bestNode != null)
			{
				Node node = bestNode; 
				Optional<VehicleTravel> travelToParkingRefueling = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(trip1.getArrivalNode()) && t.getArrivalNode().equals(node)).findAny();  
				Optional<VehicleTravel> travelFromParkingRefueling = this.allVehicleTravels.stream().filter(t -> t.getDepartureNode().equals(node) && t.getArrivalNode().equals(trip2.getDepartureNode())).findAny(); 
				if(travelToParkingRefueling.isPresent() && travelFromParkingRefueling.isPresent())
				{
					int endTimeOfTravelToParkRefuel = trip1.getArrivalTime() + (travelToParkingRefueling.get().getDuration()); 
					Deadrun deadrun1 = new Deadrun(trip1.getArrivalNode(), bestNode, trip1.getArrivalTime(), endTimeOfTravelToParkRefuel, travelToParkingRefueling.get().getDistance(), false, true, trip1.getTripId(), Integer.MAX_VALUE); 
					//if(!this.deadruns.contains(deadrun1))
					{
						//this.deadruns.add(deadrun1); 
						this.deadrunsOnArc.add(deadrun1); 
					}
					/*else
					{
						Optional<Deadrun> existingDeadrun = this.deadruns.stream().filter(d -> d.equals(deadrun1)).findAny(); 
						this.deadrunsOnArc.add(existingDeadrun.get()); 
					}*/
					
					BlockActivity travelToParkRefuel = new BlockActivity(trip1.getArrivalNode(), bestNode, trip1.getArrivalTime(), endTimeOfTravelToParkRefuel, travelToParkingRefueling.get().getDistance(), this.deadrunsOnArc.get(0).getDeadrunId(), "Deadrun");
					this.blockActivitiesOnArc.add(travelToParkRefuel); 
						 
					
					int startTimeOfTravelFromParkRefuel = trip2.getDepartureTime() - (travelFromParkingRefueling.get().getDuration()); 
					Deadrun deadrun2 = new Deadrun(bestNode, trip2.getDepartureNode(), startTimeOfTravelFromParkRefuel, trip2.getDepartureTime(), travelFromParkingRefueling.get().getDistance(), true, false, -1, trip2.getTripId()); 
					//if(!this.deadruns.contains(deadrun2))
					{
						//this.deadruns.add(deadrun2); 
						this.deadrunsOnArc.add(deadrun2); 
					}
					/*else
					{
						Optional<Deadrun> existingDeadrun = this.deadruns.stream().filter(d -> d.equals(deadrun2)).findAny(); 
						this.deadrunsOnArc.add(existingDeadrun.get()); 
					}*/
					
					BlockActivity travelFromParkRefuel = new BlockActivity(bestNode, trip2.getDepartureNode(), startTimeOfTravelFromParkRefuel, trip2.getDepartureTime(), travelFromParkingRefueling.get().getDistance(), this.deadrunsOnArc.get(1).getDeadrunId(), "Deadrun"); 
					this.blockActivitiesOnArc.add(travelFromParkRefuel);  
					
					if(startTimeOfTravelFromParkRefuel - endTimeOfTravelToParkRefuel < 0)
					{
						this.blockActivitiesOnArc.clear();
						return this.blockActivitiesOnArc; 
					}
					
					if(recharging)
					{
						if((startTimeOfTravelFromParkRefuel - endTimeOfTravelToParkRefuel) >= this.vehicleType.getMinimumRechargingDuration())
						{
							BlockActivity rechargeAtNode = new BlockActivity(bestNode, bestNode, endTimeOfTravelToParkRefuel, startTimeOfTravelFromParkRefuel, 0, -1, "Recharging"); 
							this.blockActivitiesOnArc.add(rechargeAtNode);  
							
							//if(!this.deadruns.contains(deadrun1))
							{
								this.deadruns.add(deadrun1); 
							}
							
							//if(!this.deadruns.contains(deadrun2))
							{
								this.deadruns.add(deadrun2); 
							}
						}
						else
						{
							this.blockActivitiesOnArc.clear();
							return this.blockActivitiesOnArc; 
						}
					}
					else
					{
						BlockActivity parkAtNode = new BlockActivity(bestNode, bestNode, endTimeOfTravelToParkRefuel, startTimeOfTravelFromParkRefuel, 0, -1, "Parking"); 
						this.blockActivitiesOnArc.add(parkAtNode);
						
						//if(!this.deadruns.contains(deadrun1))
						{
							this.deadruns.add(deadrun1); 
						}
						
						//if(!this.deadruns.contains(deadrun2))
						{
							this.deadruns.add(deadrun2); 
						}
					}
				}
			}
		}		
		
		Collections.sort(this.blockActivitiesOnArc);
		return this.blockActivitiesOnArc; 
		
		
	}

}
