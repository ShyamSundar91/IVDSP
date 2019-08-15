package LinearProgramming;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import Data.Trip;
import Variables.Block;

public class InterTripsFixing 
{
	private List<Trip> allTrips; 
	private Map<Trip, Integer> mapTripIds; 
	private Map<Block, Double> fractionalValuesOfBlocks;
	private double [][] fractionalTrips; 
	
	public InterTripsFixing(List<Trip> allTrips, Map<Block, Double> fractionalValuesOfBlocks)
	{
		this.fractionalValuesOfBlocks = fractionalValuesOfBlocks; 
		Assert.assertTrue(!this.fractionalValuesOfBlocks.isEmpty());
		this.allTrips = allTrips; 
		this.mapTripIds = new HashMap<Trip, Integer>(); 
		int id  = 0; 
		for(Trip trip : this.allTrips)
		{
			this.mapTripIds.put(trip, id); 
			id++; 
		}
		
		this.fractionalTrips = new double[this.allTrips.size()][this.allTrips.size()]; 
		for(double [] row : this.fractionalTrips)
		{
			Arrays.fill(row, 0);
		}
		
		calculateFractionalValuesOfTrips(); 
		
		for(Trip trip1 : this.allTrips)
		{
			for(Trip trip2 : this.allTrips)
			{
				if(!trip1.equals(trip2))
				{
					int trip1Index = this.mapTripIds.get(trip1); 
					int trip2Index = this.mapTripIds.get(trip2); 
					
					double value = this.fractionalTrips[trip1Index][trip2Index]; 
					if(value > 0.001)
					{
						System.out.println("Trip " + trip1.getTripId() + " and Trip " + trip2.getTripId() + ", value = " + value);
					}
				}
			}
		}
	}
	
	private void calculateFractionalValuesOfTrips()
	{
		for(Block block : this.fractionalValuesOfBlocks.keySet())
		{
			List<Trip> tripsInBlock = block.getTripsInBlock(); 
			Collections.reverse(tripsInBlock);
			
			if(tripsInBlock.size() > 1)
			{
				for(int t1 = 0; t1 < tripsInBlock.size()-1; t1++)
				{
					for(int t2 = t1+1; t2 < tripsInBlock.size(); t2++)
					{
						Trip firstTrip = tripsInBlock.get(t1); 
						Trip secondTrip = tripsInBlock.get(t2); 
						
						//System.out.println(firstTrip.getArrivalTime() + ", " + secondTrip.getDepartureTime());
						Assert.assertTrue(secondTrip.getDepartureTime() >= firstTrip.getArrivalTime());
						
						int firstTripIndex = this.mapTripIds.get(firstTrip); 
						int secondTripIndex = this.mapTripIds.get(secondTrip); 
						
						this.fractionalTrips[firstTripIndex][secondTripIndex] = this.fractionalTrips[firstTripIndex][secondTripIndex] + this.fractionalValuesOfBlocks.get(block); 
					}
				}
			}
		}
	}

}
