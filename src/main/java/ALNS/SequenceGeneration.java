package ALNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Data.Trip;
import Variables.Block;
import Variables.Duty;
import lombok.Getter;

public class SequenceGeneration 
{
	private List<Trip> allTrips; 
	private List<Block> blocksInSolution; 
	private List<Duty> dutiesInSolution; 
	
	private Map<Trip, Block> tripCoveredByBlock; 
	private Map<Trip, Duty> tripCoveredByDuty; 
	@Getter
	private List<Sequence> sequences; 
	public SequenceGeneration(List<Trip> allTrips, List<Block> blocksInSolution, List<Duty> dutiesInSolution)
	{
		this.allTrips = allTrips; 
		this.blocksInSolution = blocksInSolution; 
		this.dutiesInSolution = dutiesInSolution;
		
		this.tripCoveredByBlock = new HashMap<Trip, Block>(); 
		this.tripCoveredByDuty = new HashMap<Trip, Duty>(); 
		this.sequences = new ArrayList<Sequence>(); 
		
		getCoveredTripsByBlockAndDuty(); 
		generate(); 
	}
	
	private void getCoveredTripsByBlockAndDuty()
	{
		for(Block block : this.blocksInSolution)
		{
			for(Trip trip : block.getTripsInBlock())
			{
				this.tripCoveredByBlock.put(trip, block); 
			}
		}
		
		for(Duty duty : this.dutiesInSolution)
		{
			for(Trip trip : duty.getTripsInDuty())
			{
				this.tripCoveredByDuty.put(trip, duty); 
			}
		}
	}
	
	private void generate()
	{
		List<Trip> tripsCovered = new ArrayList<Trip>();
		for(int t1 = 0; t1 < this.allTrips.size(); t1++)
		{
			if(!tripsCovered.contains(this.allTrips.get(t1)))
			{
				List<Trip> tripsInSequence = new ArrayList<Trip>();
				tripsInSequence.add(this.allTrips.get(t1));
				tripsCovered.add(this.allTrips.get(t1)); 
				Block blockCoveringTrip = this.tripCoveredByBlock.get(this.allTrips.get(t1)); 
				Duty dutyCoveringTrip = this.tripCoveredByDuty.get(this.allTrips.get(t1)); 
				
				for(int t2= 0; t2 < this.allTrips.size(); t2++)
				{
					if(t1 != t2 && !tripsCovered.contains(this.allTrips.get(t2)))
					{
						Block blockCoveringTrip2 = this.tripCoveredByBlock.get(this.allTrips.get(t2)); 
						Duty dutyCoveringTrip2 = this.tripCoveredByDuty.get(this.allTrips.get(t2));
						
						if(blockCoveringTrip.equals(blockCoveringTrip2) && dutyCoveringTrip.equals(dutyCoveringTrip2))
						{
							tripsInSequence.add(this.allTrips.get(t2)); 
							tripsCovered.add(this.allTrips.get(t2));
						}
					}
				}
				
				Sequence seq = new Sequence(tripsInSequence, blockCoveringTrip, dutyCoveringTrip); 
				this.sequences.add(seq); 
			}
		}
		/*int s = 1;
		
		for(Sequence seq : this.sequences)
		{
			 
			System.out.println("Sequence number = " + s);
			seq.getTripsInSequence().forEach(t ->{
				System.out.print(t.getTripId() + ", ");
			});
			System.out.print("Block = " + seq.getBlockCoveringSequence().getBlockId() + ", ");
			System.out.print("Duty = " + seq.getDutyCoveringSequence().getDutyId() + ", ");
			System.out.print("Cost = " + seq.getCostOfSequence());
			System.out.println();
			s++; 
		}*/
	}

}
