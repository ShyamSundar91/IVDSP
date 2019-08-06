package Subproblems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Assert;

import Data.DutyType;
import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import lombok.Getter;

public class DriverRCSPP 
{
	private DutyType dutyType; 
	private DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph; 
	private DriverVertex sourceVertex; 
	private DriverVertex sinkVertex; 
	
	private Map<Trip, Double> dualValuesOfTripIDs; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit; 
	private Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit; 
	private Map<IdleTime, Double> dualValuesOfIdleTimes;
	private List<Trip> tripsInSolution; 
	private Set<Deadrun> deadrunsInSolutions; 
	private Set<IdleTime> idleTimesInSolution; 
	
	private List<DriverVertex> queue; 
	@Getter
	private List<Duty> dutiesGenerated; 
	private boolean generateAllVariables; 
	private boolean allowBlockChange;
	private boolean useSubNetwork; 
	private int allowedBlockChanges; 
	
	public DriverRCSPP(DutyType dutyType, DefaultDirectedGraph<DriverVertex, DriverArc> driverGraph,  Map<Trip, Double> dualValuesOfTripIDs, Map<Deadrun, Double> dualValuesOfDeadrunsLowerLimit, Map<Deadrun, Double> dualValuesOfDeadrunsUpperLimit, Map<IdleTime, Double> dualValuesOfIdleTimes, List<Trip> tripsInSolution, Set<Deadrun> deadrunsInSolutions, Set<IdleTime> idleTimesInSolution,  boolean allowBlockChange, boolean useSubNetwork)
	{
		this.dutyType = dutyType; 
		this.driverGraph = driverGraph; 
		this.dualValuesOfTripIDs = dualValuesOfTripIDs; 
		this.dualValuesOfDeadrunsLowerLimit = dualValuesOfDeadrunsLowerLimit; 
		this.dualValuesOfDeadrunsUpperLimit = dualValuesOfDeadrunsUpperLimit; 
		this.dualValuesOfIdleTimes = dualValuesOfIdleTimes; 
		this.tripsInSolution = tripsInSolution; 
		this.deadrunsInSolutions = deadrunsInSolutions; 
		this.idleTimesInSolution = idleTimesInSolution;
		
		this.sourceVertex = this.driverGraph.vertexSet().stream().filter(v -> v.getCurrentTime() == -1).findFirst().get(); 
		this.sinkVertex = this.driverGraph.vertexSet().stream().filter(v -> v.getCurrentTime() == Integer.MAX_VALUE).findFirst().get(); 
		for(DriverVertex vertex : this.driverGraph.vertexSet())
		{
			Assert.assertTrue(vertex.getLabels().isEmpty());
		}
		
		this.queue = new ArrayList<DriverVertex>();
		this.dutiesGenerated = new ArrayList<Duty>(); 
		this.generateAllVariables = false; 
		this.allowBlockChange = allowBlockChange; 
		this.allowedBlockChanges = 0; 
		if(this.allowBlockChange)
		{
			this.allowedBlockChanges = this.dutyType.getMaximumNumberOfBlockChanges(); 
		}
		this.useSubNetwork = useSubNetwork; 
		
		
		initialization(); 
		
		algorithm(); 
	}
	
	private void initialization()
	{
		DriverREF initialREF = new DriverREF(this.dutyType, null, null, this.allowedBlockChanges); 
		LabelDriver initialLabel = new LabelDriver(null, null, null, initialREF);
		this.sourceVertex.getLabels().add(initialLabel); 
		this.queue.add(this.sourceVertex); 
	}
	
	private void algorithm()
	{
		while(!this.queue.isEmpty())
		{
			DriverVertex selectedVertex = this.queue.get(0); 
			
			Set<DriverArc> outgoingArcs = this.driverGraph.outgoingEdgesOf(selectedVertex); 
			
			if(this.useSubNetwork)
			{
				outgoingArcs = selectArcs(outgoingArcs); 
			}
			
			for(LabelDriver selectedLabel : selectedVertex.getLabels())
			{
				if(!selectedLabel.isLabelDriverVisited())
				{
					//Cannot get this to work for an exact column generation. Have to look at it some other time. 
					/*if(!selectedLabel.getUpdatedResources().isAttendedBus())
					{
						Set<DriverArc> selected = new HashSet<DriverArc>(outgoingArcs.stream().filter(a -> a.isAttendingBus()).collect(Collectors.toSet()));
						outgoingArcs = selected; 
					}*/					
					
					for(DriverArc outgoingArc : outgoingArcs)
					{
						DriverVertex successorVertex = outgoingArc.getSuccessorVertex(); 
						
						DriverREF newREF = new DriverREF(this.dutyType, selectedLabel.getUpdatedResources(), outgoingArc, this.allowedBlockChanges); 
						if(newREF.isValid())
						{
							if(successorVertex.getCurrentTime() == Integer.MAX_VALUE && (newREF.getUpdatedReducedCost() <= -0.01  || this.generateAllVariables))
							{
								LabelDriver newLabel = new LabelDriver(selectedLabel, selectedVertex, outgoingArc, newREF); 
								//if(checkMinimumResources(newLabel))
								{
									successorVertex.getLabels().add(newLabel);
								}
							}
							else if(successorVertex.getCurrentTime() != Integer.MAX_VALUE)
							{
								LabelDriver newLabel = new LabelDriver(selectedLabel, selectedVertex, outgoingArc, newREF); 
								if(this.generateAllVariables)
								{
									successorVertex.getLabels().add(newLabel); 
								}
								else
								{
									checkDomination(successorVertex, newLabel); 
								}
								
								
								if(!this.queue.contains(successorVertex))
								{
									this.queue.add(successorVertex); 
								}
							}
						}
					}
				}
				selectedLabel.labelDriverVisited();
			}
			
			this.queue.remove(selectedVertex); 
		}
		
		retrievePaths(); 
	}
	
	/*private boolean checkMinimumResources(LabelDriver labelAtSink)
	{
		int duration = labelAtSink.getUpdatedResources().getUpdatedTotalDuration(); 
		
		if(!this.generateAllVariables)
		{
			if(duration < this.dutyType.getMinimumPaidTime())
			{
				double newRedCost = 0.0;
				for(Trip trip : labelAtSink.getUpdatedResources().getUpdatedTrips())
				{
					newRedCost = newRedCost + this.dualValuesOfTripIDs.get(trip); 
				}
				
				for(Deadrun deadrun : labelAtSink.getUpdatedResources().getUpdatedDeadruns())
				{
					if(!this.dualValuesOfDeadrunsLowerLimit.isEmpty() && this.dualValuesOfDeadrunsLowerLimit.containsKey(deadrun))
					{
						newRedCost = newRedCost  + this.dualValuesOfDeadrunsLowerLimit.get(deadrun) + this.dualValuesOfDeadrunsUpperLimit.get(deadrun); 
					}	
				}
				
				for(IdleTime idleTime : labelAtSink.getUpdatedResources().getUpdatedIdleTimes())
				{
					if(!this.dualValuesOfIdleTimes.isEmpty() && this.dualValuesOfIdleTimes.containsKey(idleTime))
					{
						newRedCost = newRedCost + this.dualValuesOfIdleTimes.get(idleTime);
					}
				}
				
				newRedCost = ((((double)this.dutyType.getMinimumPaidTime()/(double)60) * this.dutyType.getCostPerHour()) + this.dutyType.getFixedCost()) - newRedCost; 
				labelAtSink.getUpdatedResources().updatedReducedCostWithRespectToMinPaidTime(newRedCost);
				if(newRedCost > -1e-6)
				{
					return false; 
				}
			}
		}
		
		
		return true; 
	}*/
	
	private Set<DriverArc> selectArcs(Set<DriverArc> outgoingArcs)
	{
		Set<DriverArc> selectedArcs = new HashSet<DriverArc>(); 
		
		for(DriverArc outgoingArc : outgoingArcs)
		{
			if(outgoingArc.getTrip() != null && this.tripsInSolution.contains(outgoingArc.getTrip()))
			{
				selectedArcs.add(outgoingArc); 
			}
			else if(outgoingArc.getDeadrun() != null && this.deadrunsInSolutions.contains(outgoingArc.getDeadrun()))
			{
				selectedArcs.add(outgoingArc); 
			}
			else if(outgoingArc.getIdleTimeOnArc() != null && this.idleTimesInSolution.contains(outgoingArc.getIdleTimeOnArc()))
			{
				selectedArcs.add(outgoingArc); 
			}
			else
			{
				if(outgoingArc.getSuccessorVertex().getTrip() != null && this.tripsInSolution.contains(outgoingArc.getSuccessorVertex().getTrip()))
				{
					if(outgoingArc.getIdleTimeOnArc() == null)
					{
						selectedArcs.add(outgoingArc);
					}
					else if(this.idleTimesInSolution.contains(outgoingArc.getIdleTimeOnArc()))
					{
						selectedArcs.add(outgoingArc); 
					}
					
				}
				else if(outgoingArc.getSuccessorVertex().getDeadrun() != null && this.deadrunsInSolutions.contains(outgoingArc.getSuccessorVertex().getDeadrun()))
				{
					if(outgoingArc.getIdleTimeOnArc() == null)
					{
						selectedArcs.add(outgoingArc);
					}
					else if(this.idleTimesInSolution.contains(outgoingArc.getIdleTimeOnArc()))
					{
						selectedArcs.add(outgoingArc); 
					}
				}
				else if(outgoingArc.getSuccessorVertex().getCurrentNode() == null)
				{
					selectedArcs.add(outgoingArc); 
				}
			}
		}
		
		return selectedArcs; 
	}
	
	private void checkDomination(DriverVertex driverVertex, LabelDriver newLabel)
	{
		List<LabelDriver> existingLabels = driverVertex.getLabels(); 
		List<LabelDriver> existingLabelsToBeRemoved = new ArrayList<LabelDriver>(); 
		DriverREF newREF = newLabel.getUpdatedResources(); 
		
		if(!existingLabels.isEmpty())
		{
			boolean notToAddLabel = false;
			boolean pendingDecision = false;
			for(LabelDriver existingLabel : existingLabels)
			{
				DriverREF existingREF = existingLabel.getUpdatedResources(); 
				
				List<Boolean> dominatingDecisions = new ArrayList<Boolean>(); 
				
				/*
				 * Check if reduced cost of the new label is dominated
				 */
				boolean dominatedReducedCost = false; 
				if(newREF.getUpdatedReducedCost() >= existingREF.getUpdatedReducedCost())
				{
					dominatedReducedCost = true; 
				}
				dominatingDecisions.add(dominatedReducedCost);
			
				/*
				 * Check if max duration is dominated
				 */
				if(!this.useSubNetwork)
				{
					if(this.dutyType.getMaxDuration() > 0)
					{
						boolean dominatedMaxDuration = false; 
						if(newREF.getUpdatedTotalDuration() >= existingREF.getUpdatedTotalDuration())
						{
							dominatedMaxDuration = true;
						}
						dominatingDecisions.add(dominatedMaxDuration); 
					}
				}
				
				
				/*
				 * Check if max duration without break is dominated
				 */
				if(!this.useSubNetwork)
				{
					if(this.dutyType.getMaximumDurationWithoutBreak() > 0)
					{
						boolean dominatedMaxDurationWithoutBreak = false; 
						if(newREF.getUpdatedDurationWithoutBreak() >= existingREF.getUpdatedDurationWithoutBreak())
						{
							dominatedMaxDurationWithoutBreak = true;
						}
						dominatingDecisions.add(dominatedMaxDurationWithoutBreak); 
					}
				}
			
				
				/*
				 * Check if max number of block changes is dominated
				 */
				if(this.allowBlockChange)
				{
					if(this.dutyType.getMaximumNumberOfBlockChanges() > 0)
					{
						boolean dominatedMaxNumberOfBlockChanges = false; 
						if(newREF.getUpdatedNumberOfBlockChanges() >= existingREF.getUpdatedNumberOfBlockChanges())
						{
							dominatedMaxNumberOfBlockChanges = true;
						}
						dominatingDecisions.add(dominatedMaxNumberOfBlockChanges); 
					}
				}
				
				/*
				 * Check if attending bus is dominated
				 
				//Cannot get this to work for an exact column generation. Have to look at it some other time. 
				boolean dominatedAttendingBus = false; 
				int compare = Boolean.compare(newREF.isAttendedBus(), existingREF.isAttendedBus()); 
				if(compare <= 0)
				{
					dominatedAttendingBus = true; 
					dominatingDecisions.add(dominatedAttendingBus); 
				}
				else
				{
					dominatingDecisions.add(dominatedAttendingBus); 
				}
				
				
				/*
				 * Check if min duration is dominated
				 */
				/*if(this.dutyType.getMinimumPaidTime() > 0)
				{
					boolean dominatedMinDuration = false; 
					if(newREF.getUpdatedTotalDuration() <= existingREF.getUpdatedTotalDuration())
					{
						dominatedMinDuration = true;
					}
					dominatingDecisions.add(dominatedMinDuration); 
				}*/
				
				boolean dominated = true; 
				boolean removeExistingLabel = true; 
				 /*
				  * Check if the new label is dominated i.e., all its resources are dominated by resources of the existing label
				  */
				for(Boolean decision : dominatingDecisions)
				{
					if(decision.equals(false))
					{
						dominated = false;
						break; 
					}
				}
				
				/*
				 * Check if the existing label is dominated by the new label
				 */
				for(Boolean decision : dominatingDecisions)
				{
					if(decision.equals(true))
					{
						removeExistingLabel = false;
						break; 
					}
				}
				
				if(!pendingDecision)
				{
					if(dominated)
					{
						notToAddLabel = true; 
					}
					else if(removeExistingLabel)
					{
						existingLabelsToBeRemoved.add(existingLabel); 
					}
				}
				
			}
			
			if(!notToAddLabel)
			{
				driverVertex.getLabels().add(newLabel); 
			}
			
			for(LabelDriver removeLabel : existingLabelsToBeRemoved)
			{
				driverVertex.getLabels().remove(removeLabel); 
			}
		}
		else
		{
			driverVertex.getLabels().add(newLabel); 
		}
	}
	
	private void retrievePaths()
	{
		List<LabelDriver> labelsAtSink = new ArrayList<LabelDriver>(this.sinkVertex.getLabels()); 
		
		if(labelsAtSink.size() > 500 && !this.generateAllVariables)
		{
			/*if(labelsAtSink.size() > 1000 /*&& this.useSubNetwork)
			{
				//Collections.sort(labelsAtSink);
				//labelsAtSink = labelsAtSink.subList(0, 1000);
				labelsAtSink = selectComplementaryColumns(labelsAtSink);
			}
			else*/
			{
				Collections.sort(labelsAtSink);
				labelsAtSink = labelsAtSink.subList(0, 500);
			}
		}
		
		List<DriverArc> driverArcs = new ArrayList<DriverArc>();
		
		for(LabelDriver finalLabel : labelsAtSink)
		{
			boolean stop = false; 
			LabelDriver currentLabel = finalLabel;

			driverArcs = new ArrayList<DriverArc>();
			
			while(!stop)
			{
	
				LabelDriver previousLabel = currentLabel.getSourceLabel(); 
				DriverVertex previousVertex = currentLabel.getSourceDriverVertex();  
				
				if(previousLabel != null && previousVertex != null)
				{
					DriverArc curretnArc = currentLabel.getExtendingDriverArc(); 
					driverArcs.add(curretnArc); 
					
					currentLabel = previousLabel;  
				}
				else
				{
					stop = true; 
				}
				
			}
			
			List<DutyActivity> dutyActivities = new ArrayList<DutyActivity>(); 
			List<Trip> trips = new ArrayList<Trip>(); 
			List<Deadrun> deadruns = new ArrayList<Deadrun>(); 
			List<IdleTime> idleTimes = new ArrayList<IdleTime>(); 
			double totalCost = 0.0; 
			for(DriverArc arc : driverArcs)
			{
				totalCost = totalCost + arc.getTotalCostOfArc(); 
				if(!arc.getDutyActivities().isEmpty())
				{
					dutyActivities.addAll(arc.getDutyActivities()); 
				}
				
				if(arc.getTrip() != null)
				{
					trips.add(arc.getTrip()); 
				}
				
				if(arc.getDeadrun() != null)
				{
					deadruns.add(arc.getDeadrun()); 
				}
				
				if(arc.getIdleTimeOnArc() != null)
				{
					idleTimes.add(arc.getIdleTimeOnArc()); 
				}
			}
			
			/*if(finalLabel.getUpdatedResources().getUpdatedTotalDuration() < this.dutyType.getMinimumPaidTime())
			{
				totalCost = ((((double)this.dutyType.getMinimumPaidTime()/(double)60) * this.dutyType.getCostPerHour()) + this.dutyType.getFixedCost()); 
			}*/
			
			for(IdleTime idleTime : idleTimes)
			{
				Optional<DutyActivity> idleActivity = dutyActivities.stream().filter(b -> (b.getDepartureNode().equals(idleTime.getNode()) && b.getArrivalNode().equals(idleTime.getNode()) && b.getDepartureTime() == idleTime.getDepartureTime() && b.getArrivalTime() == idleTime.getArrivalTime()) && (b.getActivity().equals("Break") || b.getActivity().equals("Duty regulation"))).findFirst();
				Assert.assertTrue(idleActivity.isPresent());
			}
			
			
			
				Collections.sort(dutyActivities);
				Duty duty = new Duty(this.dutyType, trips, deadruns, dutyActivities, idleTimes); 
				this.dutiesGenerated.add(duty); 
				Assert.assertTrue((trips.isEmpty() && !deadruns.isEmpty()) || (!trips.isEmpty() && deadruns.isEmpty()) || (!trips.isEmpty() && !deadruns.isEmpty()));
				Assert.assertTrue(duty.getTotalDuration() == finalLabel.getUpdatedResources().getUpdatedTotalDuration());
				Assert.assertTrue(Math.abs(duty.getTotalCostOfDuty()-totalCost) <= 1e-6);
				Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedTrips().containsAll(duty.getTripsInDuty()) && duty.getTripsInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedTrips()));
				Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedDeadruns().containsAll(duty.getDeadrunsInDuty()) && duty.getDeadrunsInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedDeadruns()));
				Assert.assertTrue(finalLabel.getUpdatedResources().getUpdatedIdleTimes().containsAll(duty.getIdleTimesInDuty()) && duty.getIdleTimesInDuty().containsAll(finalLabel.getUpdatedResources().getUpdatedIdleTimes()));
				validateDuty(duty);
				
				if(!this.generateAllVariables)
				{
					double redCost = 0.0; 
					for(Trip trip : duty.getTripsInDuty())
					{
						redCost = redCost + this.dualValuesOfTripIDs.get(trip); 
					}
					for(Deadrun deadrun : duty.getDeadrunsInDuty())
					{
						if(!this.dualValuesOfDeadrunsLowerLimit.isEmpty() && this.dualValuesOfDeadrunsLowerLimit.containsKey(deadrun))
						{
							redCost = redCost + this.dualValuesOfDeadrunsLowerLimit.get(deadrun) + this.dualValuesOfDeadrunsUpperLimit.get(deadrun); 
						}
						
					}
					for(IdleTime idleTime : duty.getIdleTimesInDuty())
					{
						if(!this.dualValuesOfIdleTimes.isEmpty() && this.dualValuesOfIdleTimes.containsKey(idleTime))
						{
							redCost = redCost + this.dualValuesOfIdleTimes.get(idleTime);
						}
						
					}
					
					redCost = duty.getTotalCostOfDuty() - redCost; 
					
					
					Assert.assertTrue(Math.abs(finalLabel.getUpdatedResources().getUpdatedReducedCost() - redCost) <= 1e-1);
				}
				
			
			 
		}
	}
	
	private List<LabelDriver> selectComplementaryColumns(List<LabelDriver> labelsAtSink)
	{
		Set<Trip> tripsCovered = new HashSet<Trip>(); 
		List<LabelDriver> selectedLabels = new ArrayList<LabelDriver>();  
		Collections.sort(labelsAtSink);
		
		for(LabelDriver label : labelsAtSink)
		{
			if(tripsCovered.isEmpty())
			{
				selectedLabels.add(label); 
				tripsCovered.addAll(label.getUpdatedResources().getUpdatedTrips()); 
			}
			else
			{
				Set<Trip> tripsInLabel = new HashSet<Trip>(); 
				tripsInLabel.addAll(label.getUpdatedResources().getUpdatedTrips()); 
				//if(!tripsInLabel.isEmpty())
				{
					tripsInLabel.retainAll(tripsCovered); 
					if(tripsInLabel.size() <= 1)
					{
						selectedLabels.add(label); 
						tripsCovered.addAll(label.getUpdatedResources().getUpdatedTrips()); 
					}
				}
				
			}
		}
		
		return selectedLabels; 
		
	}
	
	private boolean validateDuty(Duty intDuty)
	{
		int durationToBreak = 0; 
		for(DutyActivity dutyActivity : intDuty.getDutyActivities())
		{
			if(dutyActivity.getActivity().equals("Break"))
			{
				durationToBreak = 0; 
			}
			else
			{
				durationToBreak = durationToBreak + dutyActivity.getDuration(); 
			}
			
			if(this.dutyType.getMaximumDurationWithoutBreak() > 0 && durationToBreak > this.dutyType.getMaximumDurationWithoutBreak())
			{
				System.out.println("Duty invlaid because of maximum duration without break");
				throw new IllegalArgumentException();
				
			}
		}
		
		if(intDuty.getTotalDuration() > this.dutyType.getMaxDuration())
		{
			System.out.println("Duty invlaid because of maximum duration");
			throw new IllegalArgumentException();
		}
		
		if(intDuty.getDutyActivities().size() > 1)
		{
			for(int i = 0; i < intDuty.getDutyActivities().size()-1 ; i++)
			{
				if(intDuty.getDutyActivities().get(i).getArrivalTime() != intDuty.getDutyActivities().get(i+1).getDepartureTime())
				{
					System.out.println("The duty activities are not continuous with respect to time.");
					intDuty.getDutyActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
	                throw new IllegalArgumentException();
				}
				 
				if(!intDuty.getDutyActivities().get(i).getArrivalNode().equals(intDuty.getDutyActivities().get(i+1).getDepartureNode()))
				{
					System.out.println("The block activities are not continuous with respect to space");
					intDuty.getDutyActivities().forEach(da -> System.out.println( da.getDepartureTime() + ", " + da.getArrivalTime() + ", " + da.getDepartureNode().getNodeId() + ", " +  da.getArrivalNode().getNodeId() + ", " + da.getActivity()));
	                throw new IllegalArgumentException();
				}
			}
		}
		
		
		return true; 
	}

}
