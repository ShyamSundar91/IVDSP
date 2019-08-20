package LinearProgramming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultDirectedGraph;

import Data.Trip;
import Networks.DriverArc;
import Networks.DriverVertex;
import Networks.DutyTypeDepot;
import Networks.VehicleArc;
import Networks.VehicleTypeDepot;
import Networks.VehicleVertex;
import Subproblems.DriverSubproblem;
import Subproblems.VehicleSubproblem;
import Variables.Block;
import Variables.BlockActivity;
import Variables.Deadrun;
import Variables.Duty;
import Variables.DutyActivity;
import Variables.IdleTime;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import lombok.Getter;

public class IntegratedMasterProblem 
{
	@Getter
	private Map<Block, Integer> initialBlocksAndGenerated;
	@Getter
	private Map<Duty, Integer> initialDutiesAndGenerated; 
	@Getter
	private List<Trip> trips;
	@Getter
	private Set<Deadrun> deadruns;
	@Getter
	private Set<IdleTime> idleTimes; 
	@Getter
	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
	@Getter
	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;

	
	private IloCplex cplex; 
	private Map<Trip, IloRange> tripVehicleConstraints; 
	private Map<Trip, IloRange> tripDriverConstraints; 
	private Map<Deadrun, IloRange> deadrunLowerLimitLinkingConstraints; 
	private Map<Deadrun, IloRange> deadrunUpperLimitLinkingConstraints; 
	private Map<IdleTime, IloRange> continousBusAttendanceConstraints; 
	private Map<Block, IloNumVar> blockVariables; 
	private Map<Duty, IloNumVar> dutyVariables; 
	private Map<Trip, IloNumVar> slackTripBlock; 
	private Map<Trip, IloNumVar> slackTripDuty;
	private Map<Deadrun, IloNumVar> slackDeadrunBlock;
	private Map<Deadrun, IloNumVar> slackDeadrunDuty; 
	private Map<IdleTime, IloNumVar> slackIdleTimeBlock; 
	private Map<IdleTime, IloNumVar> slackIdleTimeDuty; 
	private List<List<Double>> lowerBound;
	
	@Getter
	private Map<Block, Double> fractionalValuesOfBlocks; 
	@Getter
	private Map<Duty, Double> fractionalValuesOfDuties; 
	@Getter
	private boolean solutionInteger; 
	@Getter
	private double lpObjective; 
	private boolean earlyTermination; 
	private double previousLpObjective; 
	private boolean allowLineChange; 
	private boolean allowBlockChange; 
	private boolean useSubNetwork; 
	private boolean useDeadrunsAndIdleTimesInMaster; 
	private int noImprovement;
	private int terminationCriteria; 
	
	@Getter
	private Map<Trip, Double> tripVehicleDuals; 
	@Getter
	private Map<Trip, Double> tripDriverDuals; 
	@Getter
	private Map<Deadrun, Double> deadrunDuals;
	@Getter
	private Map<IdleTime, Double> idleTimeDuals; 
	
	private double totalTimeOfMaster; 
	private double totalTimeOfVehicleSub; 
	private double totalTimeOfDriverSub; 
	public IntegratedMasterProblem(List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs, Map<Block, Integer> initialBlocksAndGenerated, Map<Duty, Integer> initialDutiesAndGenerated, boolean earlyTermination) throws IloException
	{
		this.trips = trips; 
		this.deadruns = deadruns; 
		this.idleTimes = idleTimes; 
		this.vehicleGraphs = vehicleGraphs; 
		this.driverGraphs = driverGraphs; 
		this.initialBlocksAndGenerated = initialBlocksAndGenerated; 
		this.initialDutiesAndGenerated = initialDutiesAndGenerated; 
	
		this.earlyTermination = earlyTermination; 
		this.previousLpObjective = Double.MAX_VALUE; 
		this.allowLineChange = false; 
		this.allowBlockChange = false; 
		this.useDeadrunsAndIdleTimesInMaster = false; 
		this.useSubNetwork = false;  
		this.noImprovement = 0; 
		this.terminationCriteria = 0; 
		
		this.cplex = new IloCplex();
		this.cplex.addMinimize(); 
		this.slackTripBlock = new HashMap<Trip, IloNumVar>(); 
		this.slackTripDuty = new HashMap<Trip, IloNumVar>();
		this.slackDeadrunBlock = new HashMap<Deadrun, IloNumVar>(); 
		this.slackDeadrunDuty = new HashMap<Deadrun, IloNumVar>(); 
		this.slackIdleTimeBlock = new HashMap<IdleTime, IloNumVar>(); 
		this.slackIdleTimeDuty = new HashMap<IdleTime, IloNumVar>(); 
		this.tripVehicleConstraints = addTripVehicleConstraints(this.trips); 
		this.tripDriverConstraints = addTripDriverVehicleConstraints(this.trips);
		this.deadrunLowerLimitLinkingConstraints = addDeadrunLowerLimitLinkingConstrains(this.deadruns); 
		//this.deadrunUpperLimitLinkingConstraints = addDeadrunUpperLimitLinkingConstraints(this.deadruns); 
		this.continousBusAttendanceConstraints = addContinuousBusAttendanceConstraints(this.idleTimes); 
		this.blockVariables = new HashMap<Block, IloNumVar>(); 
		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
		
		this.tripVehicleDuals = new HashMap<Trip, Double>(); 
		this.tripDriverDuals = new HashMap<Trip, Double>(); 
		this.deadrunDuals = new HashMap<Deadrun, Double>(); 
		this.idleTimeDuals = new HashMap<IdleTime, Double>(); 
		
		addBlockVariables(this.initialBlocksAndGenerated.keySet()); 
		addDutyVariables(this.initialDutiesAndGenerated.keySet()); 
		this.lowerBound = new ArrayList<List<Double>>(); 
		this.totalTimeOfMaster = 0.0; 
		this.totalTimeOfDriverSub = 0.0; 
		this.totalTimeOfVehicleSub = 0.0; 
		
		columnGeneration(); 
		for(List<Double> lb : this.lowerBound)
		{
			System.out.println(lb.get(0) + "; " + lb.get(1) + "; " + lb.get(2));
		}
		System.out.println("Total time spent on solving master problem = " + this.totalTimeOfMaster);
		System.out.println("Total time spent on solving vehicle subproblem = " + this.totalTimeOfVehicleSub);
		System.out.println("Total time spent on solving driver subproblem = " + this.totalTimeOfDriverSub);
		
		getFractionalValues();  
		
		shutCPLEX(); 
	}
	
	private void columnGeneration() throws IloException
	{
		int iterationNumber = 0; 
		int status = 0; 
		 
		long start = System.currentTimeMillis(); 
		this.cplex.setOut(null);
		//this.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
		this.cplex.setParam(IloCplex.IntParam.Parallel, 1);
		this.cplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Barrier);
		this.cplex.setParam(IloCplex.IntParam.AdvInd, 2);
		while(status != 1)
		{
			System.out.println("***************************************************");
			System.out.println("Iteration number = " + iterationNumber);
			System.out.println("Number of blocks = " + this.blockVariables.size());
			System.out.println("Number of duties = " + this.dutyVariables.size());
			System.out.println("Number of deadruns = " + this.deadrunLowerLimitLinkingConstraints.size());
			System.out.println("Number of idleTimes = " + this.continousBusAttendanceConstraints.size());
			
			Map<Trip, Double> tripsVehicleDual = new HashMap<Trip, Double>(); 
			Map<Trip, Double> tripsDriverDual = new HashMap<Trip, Double>(); 
			Map<Deadrun, Double> deadrunsLowerLimitDual = new HashMap<Deadrun, Double>(); 
			Map<Deadrun, Double> deadrunsUpperLimitDual = new HashMap<Deadrun, Double>(); 
			Map<IdleTime, Double> idleTimesDual = new HashMap<IdleTime, Double>();
			
			List<Double> iter = new ArrayList<Double>(); 
			iter.add((double)this.blockVariables.size());
			iter.add((double)this.dutyVariables.size()); 
			double startMP = System.currentTimeMillis(); 
			if(this.cplex.solve())
			{
				System.out.println("LP Objective = " + this.cplex.getObjValue());
				double endtMP = System.currentTimeMillis(); 
				this.totalTimeOfMaster = this.totalTimeOfMaster + ((endtMP-startMP)/(double)1000) ;
				this.lpObjective = this.cplex.getObjValue(); 
				iter.add(this.cplex.getObjValue()); 
				this.lowerBound.add(iter);  
				
				for(Trip trip : this.trips)
				{
					tripsVehicleDual.put(trip, 0.0); 
					tripsDriverDual.put(trip, 0.0); 
					tripsVehicleDual.replace(trip, this.cplex.getDual(this.tripVehicleConstraints.get(trip))); 
					tripsDriverDual.replace(trip, this.cplex.getDual(this.tripDriverConstraints.get(trip))); 
				}
				
				for(Deadrun deadrun : this.deadruns)
				{
					deadrunsLowerLimitDual.put(deadrun, 0.0); 
					deadrunsUpperLimitDual.put(deadrun, 0.0); 
					deadrunsLowerLimitDual.replace(deadrun, this.cplex.getDual(this.deadrunLowerLimitLinkingConstraints.get(deadrun))); 
					//deadrunsUpperLimitDual.replace(deadrun, this.cplex.getDual(this.deadrunUpperLimitLinkingConstraints.get(deadrun))); 
				}
				
				for(IdleTime idleTime : this.idleTimes)
				{
					idleTimesDual.put(idleTime, 0.0);
					idleTimesDual.replace(idleTime, this.cplex.getDual(this.continousBusAttendanceConstraints.get(idleTime))); 
				}
				
				//columnManagement(iterationNumber);
					
				//if(this.useSubNetwork)
				{
					autoTuneNetworkSize(this.lpObjective);
				}
				
				
				long end = System.currentTimeMillis(); 
				double totalTime = (end- start)/1000.00; 
				int timLim = 3; 
				if(this.trips.size() > 200)
				{
					timLim = 60;
				}
				if(totalTime > timLim && this.earlyTermination)
				{
					System.out.println("Time limit reached");
					status = 1; 
				}
				
				/*if(this.earlyTermination && this.useDeadrunsAndIdleTimesInMaster)
				{

					double change = ((this.previousLpObjective - this.lpObjective)/this.previousLpObjective) * 100.00;
					if(change < 0.01)
					{
						this.terminationCriteria++;  
					}
					else
					{
						this.terminationCriteria = 0; 
					}
					
					if(this.terminationCriteria >= 10)
					{
						status = 1; 
					}
					
					this.previousLpObjective = this.lpObjective; 
				}*/
				
			
				
				if(status != 1)
				{
					status = solveSubproblems(iterationNumber, tripsVehicleDual, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual); 
				}
				
				if(status == 1)
				{
					this.tripVehicleDuals.putAll(tripsVehicleDual);
					this.tripDriverDuals.putAll(tripsDriverDual);
					this.deadrunDuals.putAll(deadrunsLowerLimitDual);
					this.idleTimeDuals.putAll(idleTimesDual);
				}
			}
			else
			{
				//this.cplex.exportModel("cplex_infeasible.lp");
				throw new IllegalArgumentException();
			}
			iterationNumber++; 
		}
	}
	
	private void columnManagement(int iteration) throws UnknownObjectException, IloException
	{
		for(Block block : this.blockVariables.keySet())
		{
			block.increaseNumberOfIterationsInMP();
			double value = this.cplex.getValue(this.blockVariables.get(block)); 
			if(value >= 1e-3)
			{
				block.increaseNumberOfTimesChosen();
			}
		}
		
		for(Duty duty : this.dutyVariables.keySet())
		{
			duty.increaseNumberOfIterationsInMP(); 
			double value = this.cplex.getValue(this.dutyVariables.get(duty)); 
			if(value >= 1e-3)
			{
				duty.increaseNumberOfTimesChosen();
			}
		}
		
		if(iteration != 0 && iteration%100 == 0)
		{
			List<Block> blocksToRemove = new ArrayList<Block>(); 
			List<Duty> dutiesToRemove = new ArrayList<Duty>(); 
			for(Block block : this.blockVariables.keySet())
			{
				if(block.getNumberOfIterationsInMP() >= 99 && block.getNumberOfTimesChosen() == 0)
				{
					blocksToRemove.add(block); 
				}
			}
			
			for(Duty duty : this.dutyVariables.keySet())
			{
				if(duty.getNumberOfIterationsInMP() >= 99 && duty.getNumberOfTimesChosen() == 0)
				{
					dutiesToRemove.add(duty); 
				}
			}
			
			System.out.println("Number of block variables to remove = " + blocksToRemove.size());
			System.out.println("Number of duty variabels to remove = " + dutiesToRemove.size());
			
			IloNumVar[] blockVarColumns = new IloNumVar[blocksToRemove.size()];
			int i = 0; 
			for(Block block : blocksToRemove)
			{
				blockVarColumns[i] = this.blockVariables.get(block); 
				this.blockVariables.remove(block); 
				this.initialBlocksAndGenerated.remove(block); 
				i++; 
			}
			this.cplex.delete(blockVarColumns);
			
			IloNumVar[] dutyVarColumns = new IloNumVar[dutiesToRemove.size()]; 
			i= 0; 
			for(Duty duty : dutiesToRemove)
			{
				dutyVarColumns[i] = this.dutyVariables.get(duty); 
				this.dutyVariables.remove(duty); 
				this.initialDutiesAndGenerated.remove(duty); 
				i++; 
			}
			this.cplex.delete(dutyVarColumns);
			
			for(Block block : this.blockVariables.keySet())
			{
				block.resetBlockInMP();
			}
			
			for(Duty duty : this.dutyVariables.keySet())
			{
				duty.resetDutyInMP();
			}
			
			List<Deadrun> deadrunsToRemove = new ArrayList<Deadrun>(); 
			List<IdleTime> idleTimesToRemove = new ArrayList<IdleTime>(); 
			for(Deadrun deadrun : this.deadruns)
			{
				boolean deadrunExists = false; 
				for(Block block : this.blockVariables.keySet())
				{
					if(block.getDeadrunsInBlock().contains(deadrun))
					{
						deadrunExists = true;
						break; 
					}
				}
				
				if(!deadrunExists)
				{
					for(Duty duty : this.dutyVariables.keySet())
					{
						if(duty.getDeadrunsInDuty().contains(deadrun))
						{
							deadrunExists = true;
							break;
						}
					}
				}
				
				if(!deadrunExists)
				{
					deadrunsToRemove.add(deadrun); 
				}
			}
			
			for(IdleTime idleTime : this.idleTimes)
			{
				boolean idleTimeExists = false; 
				for(Block block : this.blockVariables.keySet())
				{
					if(block.getIdleTimesInBlock().contains(idleTime))
					{
						idleTimeExists = true;
						break; 
					}
				}
				
				if(!idleTimeExists)
				{
					for(Duty duty : this.dutyVariables.keySet())
					{
						if(duty.getIdleTimesInDuty().contains(idleTime))
						{
							idleTimeExists = true;
							break; 
						}
					}
				}
				
				if(!idleTimeExists)
				{
					idleTimesToRemove.add(idleTime); 
				}
			}
			
			System.out.println("Number of deadruns to remove = " + deadrunsToRemove.size());
			System.out.println("Number of idle times to remove = " + idleTimesToRemove.size());
			
			IloRange[] deadrunConstraints = new IloRange[deadrunsToRemove.size()]; 
			i = 0;
			for(Deadrun deadrun : deadrunsToRemove)
			{
				deadrunConstraints[i] = this.deadrunLowerLimitLinkingConstraints.get(deadrun); 
				this.deadrunLowerLimitLinkingConstraints.remove(deadrun); 
				this.deadruns.remove(deadrun); 
				i++; 
			}
			this.cplex.delete(deadrunConstraints);
			
			IloRange[] idleTimeConstraints = new IloRange[idleTimesToRemove.size()]; 
			i = 0; 
			for(IdleTime idleTime : idleTimesToRemove)
			{
				idleTimeConstraints[i] = this.continousBusAttendanceConstraints.get(idleTime); 
				this.continousBusAttendanceConstraints.remove(idleTime); 
				this.idleTimes.remove(idleTime); 
				i++; 
			}
			this.cplex.delete(idleTimeConstraints);
			
			
		}
	}
	
	private void autoTuneNetworkSize(double currentLpObjective)
	{
		double change = ((this.previousLpObjective - currentLpObjective)/this.previousLpObjective) * 100.00;
		if(change < 1)
		{
			noImprovement++; 
		}
		else
		{
			noImprovement = 0; 
		}
		
		if(noImprovement >= 10)
		{
			if(!this.allowBlockChange)
			{
				System.out.println("Allow block change");
				this.allowBlockChange = true; 
				noImprovement = 0;
			}
			else if(!this.allowLineChange)
			{
				System.out.println("Allow line change");
				this.allowLineChange = true; 
				noImprovement = 0; 
			}
			/*else if(!this.useDeadrunsAndIdleTimesInMaster)
			{
				System.out.println("Use Deadruns and idle times in master");
				this.useDeadrunsAndIdleTimesInMaster = true; 
				noImprovement = 0;
			}
			/*else if(this.useSubNetwork)
			{
				System.out.println("Use full subproblem network");
				this.useSubNetwork = false; 
				noImprovement = 0;
			}*/
		}
		this.previousLpObjective = currentLpObjective; 
	}
	
	private void getFractionalValues() throws UnknownObjectException, IloException
	{
		this.fractionalValuesOfBlocks = new HashMap<Block, Double>(); 
		this.fractionalValuesOfDuties = new HashMap<Duty, Double>(); 
		this.solutionInteger = true; 
		 
		for(Block block : this.blockVariables.keySet())
		{
			double value = this.cplex.getValue(this.blockVariables.get(block)); 
			if(value >= 1-1e-3)
			{
				this.fractionalValuesOfBlocks.put(block, value); 
			}
			else if(value > 1e-3)
			{
				this.solutionInteger = false; 
				this.fractionalValuesOfBlocks.put(block, value);
			}
		}
		
		
		for(Duty duty : this.dutyVariables.keySet())
		{
			double value = this.cplex.getValue(this.dutyVariables.get(duty)); 
			if(value >= 1-1e-3)
			{
				this.fractionalValuesOfDuties.put(duty, value);  
			}
			else if(value > 1e-3)
			{
				this.solutionInteger = false; 
				this.fractionalValuesOfDuties.put(duty, value); 
			}
		}
		
	}
	
	private int solveSubproblems(int iterationNumber, Map<Trip, Double> tripsVehicleDual, Map<Trip, Double> tripsDriverDual, Map<Deadrun, Double> deadrunsLowerLimitDual, Map<Deadrun, Double> deadrunsUpperLimitDual, Map<IdleTime, Double> idleTimesDual) throws IloException
	{
		int status = 0; 
		Set<Block> blocksGenerated = new HashSet<Block>(); 
		Set<Duty> dutiesGenerated = new HashSet<Duty>(); 
		
		Set<Deadrun> deadrunsGenerated = new HashSet<Deadrun>(); 
		Set<IdleTime> idleTimesGenerated = new HashSet<IdleTime>(); 
		
		double startVehicleSub = System.currentTimeMillis(); 
		VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(iterationNumber, this.trips, this.vehicleGraphs, tripsVehicleDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, this.allowLineChange, this.useSubNetwork); 
		blocksGenerated.addAll(vehicleSubproblem.getBlocksGenerated()); 
		double endVehicleSub = System.currentTimeMillis(); 
		this.totalTimeOfVehicleSub = this.totalTimeOfVehicleSub + (endVehicleSub - startVehicleSub)/(double)1000; 
		for(Block block : blocksGenerated)
		{
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				if(!this.deadruns.contains(deadrun) && !deadrunsGenerated.contains(deadrun))
				{
					deadrunsGenerated.add(deadrun); 
				}
			}
				
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				if(!this.idleTimes.contains(idleTime) && !idleTimesGenerated.contains(idleTime))
				{
					idleTimesGenerated.add(idleTime); 
				}
			}
		}
		System.out.println("Number of deadruns generated from vehicle subproblem = " + deadrunsGenerated.size());
		System.out.println("Number of idle times generated from vehicle subproblem = " + idleTimesGenerated.size());
		
		/*Set<Deadrun> heuristicDeadruns = new HashSet<Deadrun>(); 
		Set<IdleTime> heuristicIdleTimesGenerated = new HashSet<IdleTime>();
		List<Trip> heuristicTrips = new ArrayList<Trip>();
		if(this.useSubNetwork && !this.useDeadrunsAndIdleTimesInMaster)
		{
			for(Block block : blocksGenerated)
			{
				for(Deadrun deadrun : block.getDeadrunsInBlock())
				{
					heuristicDeadruns.add(deadrun); 
				}
					
				for(IdleTime idleTime : block.getIdleTimesInBlock())
				{
					heuristicIdleTimesGenerated.add(idleTime); 
				}
					
				for(Trip trip : block.getTripsInBlock())
				{
					heuristicTrips.add(trip); 
				}
			}
				
			heuristicTrips = heuristicTrips.stream().distinct().collect(Collectors.toList()); 
		}
		else if(this.useDeadrunsAndIdleTimesInMaster && this.useSubNetwork)
		{
			heuristicTrips.addAll(this.trips); 
			heuristicDeadruns.addAll(this.deadruns); 
			heuristicIdleTimesGenerated.addAll(this.idleTimes); 
		}*/
		
		int beforeDeadrunSize = deadrunsGenerated.size(); 
		int beforeIdleTimeSize = idleTimesGenerated.size(); 
		double startDriverSub = System.currentTimeMillis(); 
		DriverSubproblem driverSubproblem = new DriverSubproblem(iterationNumber, this.driverGraphs, tripsDriverDual, deadrunsLowerLimitDual, deadrunsUpperLimitDual, idleTimesDual, this.trips, this.deadruns, this.idleTimes, this.allowBlockChange, true); 
		dutiesGenerated.addAll(driverSubproblem.getDutiesGenerated()); 
		double endDriverSub = System.currentTimeMillis(); 
		this.totalTimeOfDriverSub = this.totalTimeOfDriverSub + (endDriverSub - startDriverSub)/(double)1000; 
		for(Duty duty : dutiesGenerated)
		{
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				if(!this.deadruns.contains(deadrun) && !deadrunsGenerated.contains(deadrun))
				{
					deadrunsGenerated.add(deadrun); 
				}
			}
				
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				if(!this.idleTimes.contains(idleTime) && !idleTimesGenerated.contains(idleTime))
				{
					idleTimesGenerated.add(idleTime); 
				}
			}
		}
		System.out.println("Number of deadruns generated from driver subproblem = " + (deadrunsGenerated.size() - beforeDeadrunSize));
		System.out.println("Number of idle times generated from driver subproblem = " + (idleTimesGenerated.size() - beforeIdleTimeSize));
		
		
		if(!deadrunsGenerated.isEmpty())
		{
			this.deadruns.addAll(deadrunsGenerated); 
			addDeadrunLowerLimitLinkingConstrains(deadrunsGenerated); 
			//addDeadrunUpperLimitLinkingConstraints(deadrunsGenerated); 
		}
		
		if(!idleTimesGenerated.isEmpty())
		{
			this.idleTimes.addAll(idleTimesGenerated); 
			addContinuousBusAttendanceConstraints(idleTimesGenerated); 
		}
		
		
		if(!blocksGenerated.isEmpty())
		{
			for(Block block : blocksGenerated)
			{
				this.initialBlocksAndGenerated.put(block, 0); 
			}
			addBlockVariables(blocksGenerated); 
		}
		
		if(!dutiesGenerated.isEmpty())
		{
			for(Duty duty : dutiesGenerated)
			{
				this.initialDutiesAndGenerated.put(duty, 0); 
			}
			addDutyVariables(dutiesGenerated); 
		}
		
		if(blocksGenerated.isEmpty() && dutiesGenerated.isEmpty())
		{
			if(!this.allowBlockChange)
			{
				System.out.println("Allow block change");
				this.noImprovement = 0; 
				this.allowBlockChange = true; 
			}
			else if(!this.allowLineChange)
			{
				System.out.println("Allow line change");
				this.noImprovement = 0; 
				this.allowLineChange = true; 
			}
			/*else if(this.useSubNetwork)
			{
				System.out.println("Use full subproblem network");
				this.useSubNetwork = false; 
			}*/
			else
			{
				status = 1; 
			}
			
		}
		
		return status; 
	}
	
	
	private void addBlockVariables(Set<Block> blocks) throws IloException
	{
		for(Block block : blocks)
		{
			IloColumn blockVariable = this.cplex.column(this.cplex.getObjective(), block.getTotalCostOfBlock()); 
			
			for(Trip trip : block.getTripsInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.tripVehicleConstraints.get(trip), 1)); 
			}
			
			for(Deadrun deadrun : block.getDeadrunsInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), -1)); 
				
				//blockVariable = blockVariable.and(this.cplex.column(this.deadrunUpperLimitLinkingConstraints.get(deadrun), -2)); 
			}
			
			for(IdleTime idleTime : block.getIdleTimesInBlock())
			{
				blockVariable = blockVariable.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), -1)); 
			}
			
			if(this.initialBlocksAndGenerated.get(block) == 1)
			{
				this.blockVariables.put(block, this.cplex.numVar(blockVariable, 1, 1, "block_" + block.getBlockId())); 
			}
			else
			{
				this.blockVariables.put(block, this.cplex.numVar(blockVariable, 0, Double.MAX_VALUE, "block_" + block.getBlockId()));
			}
			
			
		}
	}
	
	private void addDutyVariables(Set<Duty> duties) throws IloException
	{
		for(Duty duty : duties)
		{
			IloColumn dutyVariable = this.cplex.column(this.cplex.getObjective(), duty.getTotalCostOfDuty()); 
			
			for(Trip trip : duty.getTripsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.tripDriverConstraints.get(trip), 1)); 
			}
			
			for(Deadrun deadrun : duty.getDeadrunsInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), 1)); 
				
				//dutyVariable = dutyVariable.and(this.cplex.column(this.deadrunUpperLimitLinkingConstraints.get(deadrun), 1)); 
			}
			
			for(IdleTime idleTime : duty.getIdleTimesInDuty())
			{
				dutyVariable = dutyVariable.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), 1)); 
			}
			
			if(this.initialDutiesAndGenerated.get(duty) == 1)
			{
				this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 1, 1, "duty_" + duty.getDutyId())); 
			}
			else
			{
				this.dutyVariables.put(duty, this.cplex.numVar(dutyVariable, 0, Double.MAX_VALUE, "duty_" + duty.getDutyId())); 
			}
			
		}
	}
	
	private Map<Trip, IloRange> addTripVehicleConstraints(List<Trip> trips) throws IloException
	{
		Map<Trip, IloRange> tripVehicleConstraints = new HashMap<Trip, IloRange>(); 
		for(Trip trip : trips)
		{
			tripVehicleConstraints.put(trip, this.cplex.addRange(1, 1, "ctTripVehicle_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripVehicleConstraints.get(trip), 1)); 
			this.slackTripBlock.put(trip, this.cplex.numVar(slack, 0, 1, "slackBlock_" + trip.getTripId())); 
		}
		return tripVehicleConstraints; 
	}
	
	private Map<Trip, IloRange> addTripDriverVehicleConstraints(List<Trip> trips) throws IloException
	{
		Map<Trip, IloRange> tripDriverConstraints = new HashMap<Trip, IloRange>(); 
		for(Trip trip : trips)
		{
			tripDriverConstraints.put(trip, this.cplex.addRange(1, 1, "ctTripDriver_" + trip.getTripId())); 
			
			IloColumn slack = this.cplex.column(this.cplex.getObjective(), 10000); 
			slack = slack.and(this.cplex.column(tripDriverConstraints.get(trip), 1)); 
			this.slackTripDuty.put(trip, this.cplex.numVar(slack, 0, 1, "slackBlock_" + trip.getTripId())); 
		}
		
		return tripDriverConstraints; 
	}
	
	private Map<Deadrun, IloRange> addDeadrunLowerLimitLinkingConstrains(Set<Deadrun> deadruns) throws IloException
	{
		Map<Deadrun, IloRange> deadrunLowerLimitLinkingConstraints = new HashMap<Deadrun, IloRange>(); 
		
		for(Deadrun deadrun : deadruns)
		{
			if(this.deadrunLowerLimitLinkingConstraints != null)
			{
				this.deadrunLowerLimitLinkingConstraints.put(deadrun, this.cplex.addRange(0, 0, "ctDeadrunLowerLimit_" + deadrun.getDeadrunId())); 
				
				IloColumn slackBlock = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackBlock = slackBlock.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), -1)); 
				this.slackDeadrunBlock.put(deadrun, this.cplex.numVar(slackBlock, 0, 1, "slackBlockDeadrun_" + deadrun.getDeadrunId()));
				
				IloColumn slackDuty = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackDuty = slackDuty.and(this.cplex.column(this.deadrunLowerLimitLinkingConstraints.get(deadrun), 1)); 
				this.slackDeadrunDuty.put(deadrun, this.cplex.numVar(slackDuty, 0, 1, "slackDutyDeadrun_" + deadrun.getDeadrunId()));
			}
			else
			{
				deadrunLowerLimitLinkingConstraints.put(deadrun, this.cplex.addRange(0, 0, "ctDeadrunLowerLimit_" + deadrun.getDeadrunId()));
				
				IloColumn slackBlock = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackBlock = slackBlock.and(this.cplex.column(deadrunLowerLimitLinkingConstraints.get(deadrun), -1)); 
				this.slackDeadrunBlock.put(deadrun, this.cplex.numVar(slackBlock, 0, 1, "slackBlockDeadrun_" + deadrun.getDeadrunId()));
				
				IloColumn slackDuty = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackDuty = slackDuty.and(this.cplex.column(deadrunLowerLimitLinkingConstraints.get(deadrun), 1)); 
				this.slackDeadrunDuty.put(deadrun, this.cplex.numVar(slackDuty, 0, 1, "slackDutyDeadrun_" + deadrun.getDeadrunId()));
			}
			
		}
		
		return deadrunLowerLimitLinkingConstraints; 
	}
	
	private Map<Deadrun, IloRange> addDeadrunUpperLimitLinkingConstraints(Set<Deadrun> deadruns) throws IloException
	{
		Map<Deadrun, IloRange> deadrunUpperLimitLinkingConstraints = new HashMap<Deadrun, IloRange>(); 
		
		for(Deadrun deadrun : deadruns)
		{
			if(this.deadrunUpperLimitLinkingConstraints != null)
			{
				this.deadrunUpperLimitLinkingConstraints.put(deadrun, this.cplex.addRange(-Double.MAX_VALUE, 0, "ctDeadrunUpperLimit_" + deadrun.getDeadrunId())); 
			}
			else
			{
				deadrunUpperLimitLinkingConstraints.put(deadrun, this.cplex.addRange(-Double.MAX_VALUE, 0, "ctDeadrunUpperLimit_" + deadrun.getDeadrunId())); 
			}
		}
		
		return deadrunUpperLimitLinkingConstraints; 
	}
	
	private Map<IdleTime, IloRange> addContinuousBusAttendanceConstraints(Set<IdleTime> idleTimes) throws IloException
	{
		Map<IdleTime, IloRange> continuousBusAttendanceConstraints = new HashMap<IdleTime, IloRange>(); 
		
		for(IdleTime idleTime : idleTimes)
		{
			if(this.continousBusAttendanceConstraints != null)
			{
				this.continousBusAttendanceConstraints.put(idleTime, this.cplex.addRange(0, 0, "ctIdleTime_" + idleTime.getNode().getNodeId() + "_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime())); 
				
				IloColumn slackBlock = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackBlock = slackBlock.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), -1)); 
				this.slackIdleTimeBlock.put(idleTime, this.cplex.numVar(slackBlock, 0, 1, "slackBlockIdleTime_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime()));
				
				IloColumn slackDuty = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackDuty  = slackDuty.and(this.cplex.column(this.continousBusAttendanceConstraints.get(idleTime), 1)); 
				this.slackIdleTimeDuty.put(idleTime, this.cplex.numVar(slackDuty, 0, 1, "slackDutyIdleTime_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime()));
			}
			else
			{
				continuousBusAttendanceConstraints.put(idleTime, this.cplex.addRange(0, 0, "ctIdleTime_" + idleTime.getNode().getNodeId() + "_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime())); 
				
				IloColumn slackBlock = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackBlock = slackBlock.and(this.cplex.column(continuousBusAttendanceConstraints.get(idleTime), -1)); 
				this.slackIdleTimeBlock.put(idleTime, this.cplex.numVar(slackBlock, 0, 1, "slackBlockIdleTime_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime()));
				
				IloColumn slackDuty = this.cplex.column(this.cplex.getObjective(), 1000); 
				slackDuty  = slackDuty.and(this.cplex.column(continuousBusAttendanceConstraints.get(idleTime), 1)); 
				this.slackIdleTimeDuty.put(idleTime, this.cplex.numVar(slackDuty, 0, 1, "slackDutyIdleTime_" + idleTime.getDepartureTime() + "_" + idleTime.getArrivalTime()));
			}
		}
		
		return continuousBusAttendanceConstraints; 
	}
	
	private void shutCPLEX()
	{
		this.cplex.end();
	}
	
}
