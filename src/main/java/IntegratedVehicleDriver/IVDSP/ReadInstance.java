package IntegratedVehicleDriver.IVDSP;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import Data.DriverTravel;
import Data.DutyType;
import Data.Node;
import Data.Trip;
import Data.VehicleTravel;
import Data.VehicleType;
import lombok.Getter;

@Getter
public class ReadInstance 
{
	private String path; 
	private Set<Node> allNodes; 
	private List<Trip> allTrips; 
	private Set<VehicleTravel> allVehicleTravels; 
	private Set<VehicleType> allVehicleTypes; 
	private List<DriverTravel> allDriverTravels; 
	private List<DutyType> allDutyTypes; 
	
	public ReadInstance(String path) throws FileNotFoundException, IOException
	{
		this.path = path; 
		
		this.allNodes = new HashSet<Node>(); 
		this.allTrips = new ArrayList<Trip>(); 
		this.allVehicleTravels = new HashSet<VehicleTravel>(); 
		this.allVehicleTypes = new HashSet<VehicleType>(); 
		this.allDriverTravels = new ArrayList<DriverTravel>(); 
		this.allDutyTypes = new ArrayList<DutyType>(); 
		
		readNodeData(); 
		readTripData(); 
		readVehicleTravelData(); 
		readVehicleTypeData(); 
		readDriverTravels(); 
		readDutyTypeTypeData(); 
	}
	
	private void readNodeData() throws FileNotFoundException, IOException
	{
		//Node input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "nodes.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
				    iteration++; 
				    continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
				
		for(List<String> record : records)
		{
			int nodeId = Integer.parseInt(record.get(0)); 
			boolean depot = Boolean.parseBoolean(record.get(1));
			int minIdleTime = Integer.parseInt(record.get(2)); 
			int maxIdleTime = Integer.parseInt(record.get(3)); 
			boolean dutySignOnAllowed = Boolean.parseBoolean(record.get(4)); 
			boolean driverChangeAllowed = Boolean.parseBoolean(record.get(5)); 
			boolean driverBreakAllowed = Boolean.parseBoolean(record.get(6)); 
			
			Node node = new Node(nodeId, depot, minIdleTime, maxIdleTime, dutySignOnAllowed, driverChangeAllowed, driverBreakAllowed); 
			this.allNodes.add(node); 
		}
		
		System.out.println("Number of nodes = " + this.allNodes.size());
	}
	
	private void readTripData() throws FileNotFoundException, IOException
	{
		//Trip input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "trips.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
					iteration++; 
					continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
		
		for(List<String> record : records)
		{
			int lineNumber = Integer.parseInt(record.get(0)); 
			int tripId = Integer.parseInt(record.get(1)); 
			int depNodeId = Integer.parseInt(record.get(2)); 
			Optional<Node> depNode = this.allNodes.stream().filter(n -> n.getNodeId() == depNodeId).findAny(); 
			int arrNodeId = Integer.parseInt(record.get(3)); 
			Optional<Node> arrNode = this.allNodes.stream().filter(n -> n.getNodeId() == arrNodeId).findAny(); 
			int depTime = Integer.parseInt(record.get(4)); 
			int arrTime = Integer.parseInt(record.get(5)); 
			double distance = Double.parseDouble(record.get(6)); 
			
			Trip trip = new Trip(lineNumber, tripId, depNode.get(), arrNode.get(), depTime, arrTime, distance); 
			this.allTrips.add(trip); 
		}
		
		System.out.println("Number of trips = " + this.allTrips.size());
	}
	
	private void readVehicleTravelData() throws FileNotFoundException, IOException
	{
		//Vehicle Travel input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "vehicleTravels.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
					iteration++; 
					continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
		
		for(List<String> record : records)
		{
			int depNodeId = Integer.parseInt(record.get(0)); 
			Optional<Node> depNode = this.allNodes.stream().filter(n -> n.getNodeId() == depNodeId).findAny(); 
			int arrNodeId = Integer.parseInt(record.get(1)); 
			Optional<Node> arrNode = this.allNodes.stream().filter(n -> n.getNodeId() == arrNodeId).findAny(); 
			int duration = Integer.parseInt(record.get(2)); 
			double distance = Double.parseDouble(record.get(3)); 
			
			VehicleTravel travel = new VehicleTravel(depNode .get(), arrNode.get(), duration, distance); 
			this.allVehicleTravels.add(travel); 
		}
		
		System.out.println("Number of vehicle travels = " + this.allVehicleTravels.size());
	}
	
	private void readVehicleTypeData() throws FileNotFoundException, IOException
	{
		//Vehicle Type input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "vehicleTypes.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
					iteration++; 
					continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
		
		for(List<String> record : records)
		{
			String vehicleTypename = record.get(0); 
			double fixedCost = Double.parseDouble(record.get(1)); 
			double costPerkm = Double.parseDouble(record.get(2)); 
			double maxDistWithRecharging = Double.parseDouble(record.get(3)); 
			int minRechargingDuration = Integer.parseInt(record.get(4)); 
			
			VehicleType vehicleType = new VehicleType(vehicleTypename, fixedCost, costPerkm, maxDistWithRecharging, minRechargingDuration);
			this.allVehicleTypes.add(vehicleType); 
		}
		
		System.out.println("Number of vehicle types = " + this.allVehicleTypes.size());
	}
	
	private void readDriverTravels() throws FileNotFoundException, IOException
	{
		//Driver Travel input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "driverTravels.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
					iteration++; 
					continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
		
		for(List<String> record : records)
		{
			int depNodeId = Integer.parseInt(record.get(0)); 
			Optional<Node> depNode = this.allNodes.stream().filter(n -> n.getNodeId() == depNodeId).findAny(); 
			int arrNodeId = Integer.parseInt(record.get(1)); 
			Optional<Node> arrNode = this.allNodes.stream().filter(n -> n.getNodeId() == arrNodeId).findAny(); 
			int duration = Integer.parseInt(record.get(2)); 
			String travelType = record.get(3);  
			
			DriverTravel travel = new DriverTravel(depNode .get(), arrNode.get(), duration, travelType); 
			this.allDriverTravels.add(travel);  
		}
		
		System.out.println("Number of driver travels = " + this.allDriverTravels.size());
	}
	
	private void readDutyTypeTypeData() throws FileNotFoundException, IOException
	{
		//Duty Type input
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(this.path + "dutyTypes.csv"))) {
			String line = null;
			int iteration = 0; 
			while ((line = br.readLine()) != null) {
				if(iteration == 0)
				{
					iteration++; 
					continue; 
				}
				String[] values = line.split(";");
				records.add(Arrays.asList(values));
			}
		}
		
		for(List<String> record : records)
		{
			String dutyTypeId = record.get(0); 
			double fixedCost = Double.parseDouble(record.get(1)); 
			double costPerHour = Double.parseDouble(record.get(2)); 
			int maxDuration = Integer.parseInt(record.get(3)); 
			int minPaidTime = Integer.parseInt(record.get(4)); 
			int minBreakTime = Integer.parseInt(record.get(5));
			int maxDurationWithoutBreak = Integer.parseInt(record.get(6));
			int maxNumberOfBlockChanges = Integer.parseInt(record.get(7));
			
			DutyType dutyType = new DutyType(dutyTypeId, fixedCost, costPerHour, maxDuration, minPaidTime, minBreakTime, maxDurationWithoutBreak, maxNumberOfBlockChanges);
			this.allDutyTypes.add(dutyType); 
			
		}
		
		System.out.println("Number of duty types = " + this.allDutyTypes.size());
	}

}
