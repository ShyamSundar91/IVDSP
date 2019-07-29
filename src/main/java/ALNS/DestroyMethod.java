package ALNS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import Data.Trip;
import Data.VehicleType;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

public class DestroyMethod 
{
	private int iteration; 
	@Getter
	private List<Block> blocksInSolution;
	@Getter
	private List<Duty> dutiesInSolution;
	@Getter
	private List<Block> blocksToBeRemoved;
	@Getter
	private List<Duty> dutiesToBeRemoved; 
	@Getter
	private List<Trip> uncoveredTripsOfVehicle; 
	@Getter
	private List<Trip> uncoveredTripsOfDriver; 
	private Random rnd; 
	private double degreeOfDestruction; 
	public DestroyMethod(int iteration, int chosenDestroyMethod, List<Block> blocksInSolution, List<Duty> dutiesInSolution)
	{
		this.blocksInSolution = new ArrayList<Block>(blocksInSolution); 
		this.dutiesInSolution = new ArrayList<Duty>(dutiesInSolution); 
		this.uncoveredTripsOfVehicle = new ArrayList<Trip>(); 
		this.uncoveredTripsOfDriver = new ArrayList<Trip>(); 
		this.iteration = iteration; 
		this.rnd = new Random(this.iteration); 
		this.degreeOfDestruction = 0.25; 
		
		if(chosenDestroyMethod == 0)
		{
			randomRemovalOfBlocks(); 
		}
		else if(chosenDestroyMethod == 1)
		{
			worstRemovalOfBlocks(); 
		}
		System.out.println("Number of blocks removed = " + this.blocksToBeRemoved.size());
		
		getTripsUncovered(this.blocksToBeRemoved, this.dutiesToBeRemoved);
		this.blocksInSolution.removeAll(this.blocksToBeRemoved); 
		this.dutiesInSolution.removeAll(this.dutiesToBeRemoved); 
	}
	
	private void randomRemovalOfBlocks()
	{
		this.blocksToBeRemoved = new ArrayList<Block>(); 
		System.out.println("Number of blocks in solution = " + this.blocksInSolution.size());
		System.out.println("Calculated = "  + (int)(this.degreeOfDestruction*this.blocksInSolution.size()));
		int numberOfBlocksToDestroy = Math.max(1, (int)(this.degreeOfDestruction*this.blocksInSolution.size())); 

		Collections.shuffle(blocksInSolution, this.rnd);
		blocksToBeRemoved.addAll(this.blocksInSolution.subList(0, numberOfBlocksToDestroy)); 
		
		List<IdleTime> idleTimesInRemovedBlocks = new ArrayList<IdleTime>(); 
		List<Deadrun> deadrunsInRemovedBlocks = new ArrayList<Deadrun>(); 
		for(Block block : blocksToBeRemoved)
		{
			/*System.out.println("Block to be removed**************");
			
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
			System.out.println("Deadruns in removed block**************");
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				System.out.println(deadrun.getDeadrunId());
			}
			System.out.println("IdleTimes in removed block**************");
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				System.out.println(idleTime.getNode().getNodeId() + "; " + idleTime.getDepartureTime() + "; " + idleTime.getArrivalTime());
			}*/
			deadrunsInRemovedBlocks.addAll(block.getDeadrunsInBlock()); 
			idleTimesInRemovedBlocks.addAll(block.getIdleTimesInBlock()); 
		}
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedDeadrunsAndIdleTimes(deadrunsInRemovedBlocks, idleTimesInRemovedBlocks); 
	}
	
	private void worstRemovalOfBlocks()
	{
		this.blocksToBeRemoved = new ArrayList<Block>(); 
		int numberOfBlocksToDestroy = Math.max(1, (int)(this.degreeOfDestruction*this.blocksInSolution.size())); 
		
		Collections.sort(this.blocksInSolution);
		Collections.reverse(this.blocksInSolution);
		
		int candidateListSize = numberOfBlocksToDestroy* 2; 
		List<Block> candiateList = this.blocksInSolution.subList(0, candidateListSize);
		Collections.shuffle(candiateList, rnd);
		blocksToBeRemoved.addAll(candiateList.subList(0, numberOfBlocksToDestroy)); 
		
		List<IdleTime> idleTimesInRemovedBlocks = new ArrayList<IdleTime>(); 
		List<Deadrun> deadrunsInRemovedBlocks = new ArrayList<Deadrun>(); 
		for(Block block : blocksToBeRemoved)
		{
			/*System.out.println("Block to be removed**************");
			
			for(BlockActivity ba : block.getBlockActivities())
			{
				System.out.println(block.getBlockId() + "; " + ba.getDepartureNode().getNodeId() + "; " + ba.getArrivalNode().getNodeId() + "; " + ba.getDepartureTime() + "; " + ba.getArrivalTime() + "; " + ba.getActivity() + "; " + ba.getTripOrDeadrunId() + "; " + ba.getDistance());
			}
			System.out.println("Deadruns in removed block**************");
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				System.out.println(deadrun.getDeadrunId());
			}
			System.out.println("IdleTimes in removed block**************");
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				System.out.println(idleTime.getNode().getNodeId() + "; " + idleTime.getDepartureTime() + "; " + idleTime.getArrivalTime());
			}*/
			deadrunsInRemovedBlocks.addAll(block.getDeadrunsInBlock()); 
			idleTimesInRemovedBlocks.addAll(block.getIdleTimesInBlock()); 
		}
		
		this.dutiesToBeRemoved = removeDutiesBasedOnRemovedDeadrunsAndIdleTimes(deadrunsInRemovedBlocks, idleTimesInRemovedBlocks); 
	}
	
	private List<Duty> removeDutiesBasedOnRemovedDeadrunsAndIdleTimes(List<Deadrun> deadrunsInRemovedBlocks, List<IdleTime> idleTimesInRemovedBlocks)
	{
		List<Duty> dutiesToBeRemoved = new ArrayList<Duty>(); 
		for(Duty duty : this.dutiesInSolution)
		{
			boolean addedDuty = false; 
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				if(deadrunsInRemovedBlocks.contains(deadrun))
				{
					dutiesToBeRemoved.add(duty);
					addedDuty = true; 
					break; 
				}
			}
			
			if(!addedDuty)
			{
				for(IdleTime idleTime : duty.getIdleTimesInDuty())
				{
					if(idleTimesInRemovedBlocks.contains(idleTime))
					{
						dutiesToBeRemoved.add(duty);
						break; 
					}
				}
			}
		}
		
		/*for(Duty duty : dutiesToBeRemoved)
		{
			System.out.println("Duty to be removed*********");
			for(DutyActivity da : duty.getDutyActivities())
			{
				System.out.println(duty.getDutyId() + "; " + duty.getTotalDuration() + "; " + duty.getTotalCostOfDuty() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + "; " + da.getTripOrDeadrunId());
			}
			System.out.println("Deadruns in removed duty**************");
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				System.out.println(deadrun.getDeadrunId());
			}
			
			System.out.println("IdleTimes in removed duty**************");
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				System.out.println(idleTime.getNode().getNodeId() + "; " + idleTime.getDepartureTime() + "; " + idleTime.getArrivalTime());
			}
		}*/
		
		return dutiesToBeRemoved; 
	}
	
	private void getTripsUncovered(List<Block> blocksToBeRemoved, List<Duty> dutiesToBeRemoved)
	{
		for(Block block : blocksToBeRemoved)
		{
			this.uncoveredTripsOfVehicle.addAll(block.getTripsInBlock()); 
		}
		/*System.out.println("Uncovered trips of vehicle ");
		this.uncoveredTripsOfVehicle.forEach(t -> {
			System.out.print(t.getTripId() + "; ");
		});
		System.out.println();*/
		for(Duty duty : dutiesToBeRemoved)
		{
			this.uncoveredTripsOfDriver.addAll(duty.getTripsInDuty()); 
		}
		
		/*System.out.println("Uncovered trips of driver ");
		this.uncoveredTripsOfDriver.forEach(t -> {
			System.out.print(t.getTripId() + "; ");
		});
		System.out.println();*/
		
	}

}
