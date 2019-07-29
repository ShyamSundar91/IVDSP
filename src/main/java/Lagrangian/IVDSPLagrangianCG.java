//package Lagrangian;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.jgrapht.graph.DefaultDirectedGraph;
//import org.junit.Assert;
//
//import Data.Trip;
//import Networks.DriverArc;
//import Networks.DriverVertex;
//import Networks.DutyTypeDepot;
//import Networks.VehicleArc;
//import Networks.VehicleTypeDepot;
//import Networks.VehicleVertex;
//import Subproblems.DriverSubproblem;
//import Subproblems.VehicleSubproblem;
//import Variables.Block;
//import Variables.Deadrun;
//import Variables.Duty;
//import Variables.DutyActivity;
//import Variables.IdleTime;
//import ilog.concert.IloColumn;
//import ilog.concert.IloException;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.concert.IloNumVarType;
//import ilog.concert.IloRange;
//import ilog.cplex.IloCplex;
//import lombok.Getter;
//
//@Getter
//public class IVDSPLagrangianCG 
//{
//	private double initialUpperBound; 
//	private List<Trip> trips; 
//	private Set<Deadrun> deadruns; 
//	private Set<IdleTime> idleTimes; 
//	private Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs; 
//	private Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs;
//	
//	private Map<Trip, Double> tripVehicleduals; 
//	private Map<Trip, Double> tripDriverduals; 
//	private Map<Deadrun, Double> deadrunLowerLimitMultiplier; 
//	private Map<Deadrun, Double> deadrunUpperLimitMultiplier; 
//	private Map<IdleTime, Double> idleTimeMultiplier; 
//	
//	private List<Block> blocksGenerated; 
//	private List<Duty> dutiesGenerated; 
//	private List<List<Double>> lowerBounds; 
//	
//	private IloCplex vehicleCplex; 
//	private IloCplex driverCplex; 
//	private Map<Trip, IloRange> tripVehicleConstraints; 
//	private Map<Trip, IloRange> tripDriverConstraints; 
//	private Map<Block, IloNumVar> blockVariables; 
//	private Map<Duty, IloNumVar> dutyVariables;
//	
//	@Getter
//	private List<Block> blocksInSolution; 
//	@Getter
//	private List<Deadrun> deadrunsInSolution; 
//	@Getter
//	private List<IdleTime> idleTimesInSolution;
// 
//	public IVDSPLagrangianCG(double initialUpperBound, List<Trip> trips, Set<Deadrun> deadruns, Set<IdleTime> idleTimes, List<Block> initialBlocks, List<Duty> initialDuties, Map<VehicleTypeDepot, DefaultDirectedGraph<VehicleVertex, VehicleArc>> vehicleGraphs, Map<DutyTypeDepot, DefaultDirectedGraph<DriverVertex, DriverArc>> driverGraphs) throws IloException
//	{
//		this.initialUpperBound = initialUpperBound; 
//		System.out.println("Initial = " + this.initialUpperBound);
//		this.trips = trips; 
//		this.deadruns = deadruns; 
//		this.idleTimes = idleTimes; 
//		this.vehicleGraphs = vehicleGraphs; 
//		this.driverGraphs = driverGraphs; 
//		
//		this.blocksGenerated = new ArrayList<Block>(); 
//		this.dutiesGenerated = new ArrayList<Duty>(); 
//		this.blocksGenerated.addAll(initialBlocks); 
//		this.dutiesGenerated.addAll(initialDuties); 
//		
//		this.tripVehicleduals = new HashMap<Trip, Double>(); 
//		this.tripDriverduals = new HashMap<Trip, Double>(); 
//		this.deadrunLowerLimitMultiplier = new HashMap<Deadrun, Double>(); 
//		this.deadrunUpperLimitMultiplier = new HashMap<Deadrun, Double>(); 
//		this.idleTimeMultiplier = new HashMap<IdleTime, Double>(); 
//		initializeDualsAndMultipliers(); 
//	
//		this.vehicleCplex = new IloCplex(); 
//		this.vehicleCplex.addMinimize(); 
//		this.driverCplex = new IloCplex();
//		this.driverCplex.addMinimize(); 
//		this.tripVehicleConstraints = addTripVehicleConstraints(); 
//		this.tripDriverConstraints = addTripDriverConstraints();
//		this.blockVariables = new HashMap<Block, IloNumVar>(); 
//		this.dutyVariables = new HashMap<Duty, IloNumVar>(); 
//		addBlockVariables(this.blocksGenerated); 
//		addDutyVariables(this.dutiesGenerated);
//		
//		algorithm(); 
//		
//		this.blocksInSolution = new ArrayList<Block>(); 
//		this.deadrunsInSolution = new ArrayList<Deadrun>(); 
//		this.idleTimesInSolution = new ArrayList<IdleTime>(); 
//		
//		//solveVehicleAsMIP(); 
//	}
//	
//	private void algorithm() throws IloException
//	{
//		int status = 0; 
//		this.vehicleCplex.setOut(null);
//		this.vehicleCplex.setParam(IloCplex.IntParam.ParallelMode, 1);
//		this.driverCplex.setOut(null);
//		this.driverCplex.setParam(IloCplex.IntParam.ParallelMode, 1);
//		/*this.driverCplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
//		this.driverCplex.setParam(IloCplex.Param.Simplex.Limits.Iterations, 500);*/
//		double previousLagBound = Double.MAX_VALUE;
//		int minimalChange = 0; 
//		while(status != 1)
//		{
//			//vehicleColumnGeneration(); 
//
//			//driverColumnGeneration(); 
//			
//			LagrangianSolverIVDSP lagSolver = new LagrangianSolverIVDSP(this.vehicleCplex, this.driverCplex, this.initialUpperBound, this.blockVariables, this.dutyVariables, this.deadrunLowerLimitMultiplier, this.idleTimeMultiplier, this.deadruns, this.idleTimes); 
//			System.out.println("Lagrangian bound = " + lagSolver.getLagrangianLowerBound());
//			double current = lagSolver.getLagrangianLowerBound();
//		
//			/*double change = Math.abs(previousLagBound - current)/previousLagBound * 100.00; 
//			if(change < 0.01)
//			{
//				minimalChange++;  
//			}
//			else
//			{
//				minimalChange = 0; 
//			}
//			
//			if(minimalChange >= 5)
//			{
//				status = 1; 
//			}
//			*/
//			if(status != 1)
//			{
//				status = columnGeneration();
//				
//				this.vehicleCplex.solve(); 
//				this.driverCplex.solve(); 
//				double lb = this.vehicleCplex.getObjValue() + this.driverCplex.getObjValue();
//				double change = Math.abs(current - lb)/current * 100.00;
//				System.out.println("Change = " + change);
//				if(change < 0.1)
//				{
//					status = 1; 
//				}
//			}
//			
//			previousLagBound = current; 
//		}
//	}
//	
//	private void vehicleColumnGeneration() throws IloException
//	{
//		int status = 0; 
//		int iteration = 0; 
//		double previousLPObjective = Double.MAX_VALUE; 
//		
//		//while(status != 1)
//		{
//			System.out.println("***************************************************");
//			System.out.println("Iteration number = " + iteration);
//			System.out.println("Number of blocks = " + this.blockVariables.size());
//			
//			//if(this.vehicleCplex.solve())
//			//{
//			//	double currentLPObjective = this.vehicleCplex.getObjValue(); 
//			//	System.out.println("LP Objective = " + currentLPObjective);
//				
//				for(Trip trip : this.trips)
//				{
//					this.tripVehicleduals.replace(trip, this.vehicleCplex.getDual(this.tripVehicleConstraints.get(trip))); 
//				}
//				
//			/*	double change = ((previousLPObjective - currentLPObjective)/(previousLPObjective)) * 100.00 ; 
//				
//				if(change < 0.1)
//				{
//					status = 1; 
//				}
//				else
//				{
//					previousLPObjective = currentLPObjective; */
//					List<Block> blocksGenerated = new ArrayList<Block>(); 
//					
//					VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(iteration, this.trips, this.vehicleGraphs, this.tripVehicleduals, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
//					blocksGenerated = vehicleSubproblem.getBlocksGenerated(); 
//					
//					if(!blocksGenerated.isEmpty())
//					{
//						addBlockVariables(blocksGenerated); 
//						this.blocksGenerated.addAll(blocksGenerated);
//					}
//					else
//					{
//						status = 1;
//					}
//				}
//			//}
//			
//			//iteration++; 
//		//}
//	}
//	
//	private void driverColumnGeneration() throws IloException
//	{
//		int status = 0; 
//		int iteration = 0;
//		double previousLPObjective = Double.MAX_VALUE; 
//		
//		List<Deadrun> deadrunsGenerated = new ArrayList<Deadrun>(); 
//		List<IdleTime> idleTimesGenerated = new ArrayList<IdleTime>(); 
//		
//		for(Block block : this.blocksGenerated)
//		{
//			deadrunsGenerated.addAll(block.getDeadrunsInBlock()); 
//			idleTimesGenerated.addAll(block.getIdleTimesInBlock()); 
//		}
//		
//		deadrunsGenerated = deadrunsGenerated.stream().distinct().collect(Collectors.toList()); 
//		idleTimesGenerated = idleTimesGenerated.stream().distinct().collect(Collectors.toList()); 
//		
//		//while(status != 1)
//		{
//			System.out.println("***************************************************");
//			System.out.println("Iteration number = " + iteration);
//			System.out.println("Number of duties = " + this.dutyVariables.size());
//			
//		/*	if(this.driverCplex.solve())
//			{
//				double currentLPObjective = this.driverCplex.getObjValue(); 
//				System.out.println("LP Objective = " + currentLPObjective);
//		*/		
//				for(Trip trip : this.trips)
//				{ 
//					this.tripDriverduals.replace(trip, this.driverCplex.getDual(this.tripDriverConstraints.get(trip))); 
//				}
//				
//			//	double change = ((previousLPObjective - currentLPObjective)/(previousLPObjective)) * 100.00 ; 
//				
//			/*	if(change < 0.1)
//				{
//					status = 1; 
//				}
//				else
//				{
//					previousLPObjective = currentLPObjective;*/ 
//					
//					List<Duty> dutiesGenerated = new ArrayList<Duty>(); 
//					
//					DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, this.tripDriverduals, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, this.trips, deadrunsGenerated, idleTimesGenerated, false, false); 
//					dutiesGenerated = driverSubproblem.getDutiesGenerated(); 
//					
//					List<Duty> dutiesToRemove = new ArrayList<Duty>(); 
//					for(Duty duty : dutiesGenerated)
//					{
//						if(this.dutiesGenerated.contains(duty))
//						{
//							for(DutyActivity da : duty.getDutyActivities())
//							{
//								System.out.println(duty.getDutyId() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + da.getTripOrDeadrunId()); 
//							}
//							
//							Duty exist = this.dutyVariables.keySet().stream().filter(d -> d.equals(duty)).findFirst().get(); 
//							
//							for(DutyActivity da : exist.getDutyActivities())
//							{
//								System.out.println(exist.getDutyId() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + da.getTripOrDeadrunId()); 
//							}
//							
//							dutiesToRemove.add(duty); 
//						}
//					}
//					
//					if(!dutiesGenerated.isEmpty())
//					{
//						addDutyVariables(dutiesGenerated); 
//						this.dutiesGenerated.addAll(dutiesGenerated); 
//					}
//					else
//					{
//						status = 1; 
//					}
//				}
//			//}
//			
//			
//		//}
//	}
//	
//	private int columnGeneration() throws IloException
//	{
//		int status = 0; 
//		int iteration = 0;  
//		
//		//while(status != 1)
//		{
//			System.out.println("***************************************************");
//			System.out.println("Iteration number = " + iteration);
//			System.out.println("Number of blocks = " + this.blockVariables.size());
//			System.out.println("Number of duties = " + this.dutyVariables.size());
//			
//			/*if(this.cplex.solve())
//			{
//				double currentLPObjective = this.cplex.getObjValue(); 
//				System.out.println("LP Objective = " + currentLPObjective);
//				
//				/*for(Duty duty : this.dutyVariables.keySet())
//				{
//					if(duty.getDutyId() == 39)
//					{
//						System.out.println("Reduced cost = " + this.cplex.getReducedCost(this.dutyVariables.get(duty)) + ", value = " + this.cplex.getValue(this.dutyVariables.get(duty)));
//						
//						double value = duty.getTotalAmountPaid(); 
//						for(Deadrun deadrun : duty.getDeadrunsInDuty())
//						{
//							value = value - this.deadrunLowerLimitMultiplier.get(deadrun);  
//						}
//						
//						for(IdleTime idleTime : duty.getIdleTimesInDuty())
//						{
//							value = value - this.idleTimeMultiplier.get(idleTime); 
//						}
//						
//						System.out.println(value);
//						
//						for(Trip trip : duty.getTripsInDuty())
//						{
//							value = value- this.tripDriverduals.get(trip); 
//						}
//						
//						System.out.println(value);
//					}
//					
//
//				}
//				double change = ((this.previousLPObjective - currentLPObjective)/(this.previousLPObjective)) * 100.00 ; 
//				if(change < 0.1)
//				{
//					status = 1; 
//				}
//				else
//				{
//					this.previousLPObjective = currentLPObjective; */
//					for(Trip trip : this.trips)
//					{
//						this.tripVehicleduals.replace(trip, this.vehicleCplex.getDual(this.tripVehicleConstraints.get(trip))); 
//						this.tripDriverduals.replace(trip, this.driverCplex.getDual(this.tripDriverConstraints.get(trip))); 
//					}
//					
//					status =  solveSubproblem(iteration); 
//				//}
//			//}
//			
//			iteration++; 
//		}
//		
//		return status; 
//	}
//	
//	private int solveSubproblem(int iteration) throws IloException
//	{
//		int status = 0; 
//		List<Block> blocksGenerated = new ArrayList<Block>(); 
//		
//		VehicleSubproblem vehicleSubproblem = new VehicleSubproblem(iteration, this.trips, this.vehicleGraphs, this.tripVehicleduals, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, false); 
//		blocksGenerated = vehicleSubproblem.getBlocksGenerated(); 
//		
//		List<Deadrun> deadrunsGenerated = new ArrayList<Deadrun>(); 
//		List<IdleTime> idleTimesGenerated = new ArrayList<IdleTime>(); 
//		
//		for(Block block : this.blocksGenerated)
//		{
//			deadrunsGenerated.addAll(block.getDeadrunsInBlock()); 
//			idleTimesGenerated.addAll(block.getIdleTimesInBlock()); 
//		}
//		
//		deadrunsGenerated = deadrunsGenerated.stream().distinct().collect(Collectors.toList()); 
//		idleTimesGenerated = idleTimesGenerated.stream().distinct().collect(Collectors.toList()); 
//		
//		List<Duty> dutiesGenerated = new ArrayList<Duty>(); 
//		
//		DriverSubproblem driverSubproblem = new DriverSubproblem(iteration, this.driverGraphs, this.tripDriverduals, this.deadrunLowerLimitMultiplier, this.deadrunUpperLimitMultiplier, this.idleTimeMultiplier, this.trips, deadrunsGenerated, idleTimesGenerated, true, false); 
//		dutiesGenerated = driverSubproblem.getDutiesGenerated(); 
//		
//		List<Duty> dutiesToRemove = new ArrayList<Duty>(); 
//		for(Duty duty : dutiesGenerated)
//		{
//			if(this.dutiesGenerated.contains(duty))
//			{
//				for(DutyActivity da : duty.getDutyActivities())
//				{
//					System.out.println(duty.getDutyId() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + da.getTripOrDeadrunId()); 
//				}
//				
//				Duty exist = this.dutyVariables.keySet().stream().filter(d -> d.equals(duty)).findFirst().get(); 
//				
//				for(DutyActivity da : exist.getDutyActivities())
//				{
//					System.out.println(exist.getDutyId() + "; " + da.getDepartureNode().getNodeId() + "; " + da.getArrivalNode().getNodeId() + "; " + da.getDepartureTime() + "; " + da.getArrivalTime() + "; " + da.getActivity() + da.getTripOrDeadrunId()); 
//				}
//				
//				dutiesToRemove.add(duty); 
//			}
//		}
//		
//		if(!dutiesToRemove.isEmpty())
//		{
//			dutiesGenerated.removeAll(dutiesToRemove); 
//		}
//		
//		if(!blocksGenerated.isEmpty())
//		{
//			addBlockVariables(blocksGenerated); 
//			this.blocksGenerated.addAll(blocksGenerated); 
//		}
//		
//		if(!dutiesGenerated.isEmpty())
//		{
//			addDutyVariables(dutiesGenerated); 
//			this.dutiesGenerated.addAll(dutiesGenerated); 
//		}
//		
//		if(blocksGenerated.isEmpty() && dutiesGenerated.isEmpty())
//		{
//			status = 1; 
//		}
//		
//		return status; 
//	}
//	
//	private void initializeDualsAndMultipliers()
//	{
//		for(Trip trip : this.trips)
//		{
//			this.tripVehicleduals.put(trip, 0.0); 
//			this.tripDriverduals.put(trip, 0.0); 
//		}
//		
//		for(Deadrun deadrun : this.deadruns)
//		{
//			this.deadrunLowerLimitMultiplier.put(deadrun, 0.0); 
//			this.deadrunUpperLimitMultiplier.put(deadrun, 0.0); 
//		}
//		
//		for(IdleTime idleTime : this.idleTimes)
//		{
//			this.idleTimeMultiplier.put(idleTime, 0.0); 
//		}
//	}
//	
//	private Map<Trip, IloRange> addTripVehicleConstraints() throws IloException
//	{
//		Map<Trip, IloRange> tripVehicleConstraints = new HashMap<Trip, IloRange>(); 
//		
//		for(Trip trip : this.trips)
//		{
//			tripVehicleConstraints.put(trip, this.vehicleCplex.addRange(1, Double.MAX_VALUE, "ctTripVehicle_" +  trip.getTripId())); 
//			 
//		}
//		
//		return tripVehicleConstraints; 
//	}
//	
//	private Map<Trip, IloRange> addTripDriverConstraints() throws IloException
//	{
//		Map<Trip, IloRange> tripDriverConstraints = new HashMap<Trip, IloRange>(); 
//		
//		for(Trip trip : this.trips)
//		{
//			tripDriverConstraints.put(trip, this.driverCplex.addRange(1, Double.MAX_VALUE, "ctTripDriver_" +  trip.getTripId())); 
//		}
//		
//		return tripDriverConstraints ; 
//	}
//	
//	private void addBlockVariables(List<Block> blocks) throws IloException
//	{
//		for(Block block : blocks)
//		{
//			double coef = block.getTotalCostOfBlock(); 
//			for(Deadrun deadrun : block.getDeadrunsInBlock())
//			{
//				coef = coef + this.deadrunLowerLimitMultiplier.get(deadrun); 
//			}
//			
//			for(IdleTime idleTime : block.getIdleTimesInBlock())
//			{
//				coef = coef + this.idleTimeMultiplier.get(idleTime); 
//			}
//			
//			IloColumn blockVariable = this.vehicleCplex.column(this.vehicleCplex.getObjective(), coef); 
//			
//			for(Trip trip : block.getTripsInBlock())
//			{
//				blockVariable = blockVariable.and(this.vehicleCplex.column(this.tripVehicleConstraints.get(trip), 1)); 
//			}
//			
//			this.blockVariables.put(block, this.vehicleCplex.numVar(blockVariable, 0, Double.MAX_VALUE, "blockVar_" + block.getBlockId())); 
//		}
//	}
//	
//	private void addDutyVariables(List<Duty> duties) throws IloException
//	{
//		for(Duty duty : duties)
//		{
//			double coef = duty.getTotalCostOfDuty(); 
//			for(Deadrun deadrun : duty.getDeadrunsInDuty())
//			{
//				coef = coef - this.deadrunLowerLimitMultiplier.get(deadrun); 
//			}
//			
//			for(IdleTime idleTime : duty.getIdleTimesInDuty())
//			{
//				coef = coef - this.idleTimeMultiplier.get(idleTime); 
//			}
//			
//			IloColumn dutyVariable = this.driverCplex.column(this.driverCplex.getObjective(), coef); 
//			
//			for(Trip trip : duty.getTripsInDuty())
//			{
//				dutyVariable = dutyVariable.and(this.driverCplex.column(this.tripDriverConstraints.get(trip), 1)); 
//			}
//			
//			this.dutyVariables.put(duty, this.driverCplex.numVar(dutyVariable, 0, 1, "dutyVar_" + duty.getDutyId())); 
//		}
//		
//	}
//	
//	private void solveVehicleAsMIP() throws IloException
//	{
//		System.out.println("***************************************************");
//
//		IloNumVar[] blockColumns = this.blockVariables.values().toArray(new IloNumVar[this.blockVariables.size()]); 
//		this.vehicleCplex.add(this.vehicleCplex.conversion(blockColumns, IloNumVarType.Int)); 
//		
//		for(int i = 0; i < blockColumns.length; i++)
//		{
//			blockColumns[i].setUB(1);
//		}
//				
//		this.vehicleCplex.setParam(IloCplex.IntParam.AdvInd, 0);
//		this.vehicleCplex.setParam(IloCplex.Param.TimeLimit, 900);
//		this.vehicleCplex.setOut(System.out);
//		
//		if(this.vehicleCplex.solve())
//		{
//			System.out.println("IP Objective = " + this.vehicleCplex.getObjValue());
//			for(Block block : this.blockVariables.keySet())
//			{
//				double value = this.vehicleCplex.getValue(this.blockVariables.get(block)); 
//				if(value > 0.99)
//				{
//					this.blocksInSolution.add(block); 
//					
//					for(Deadrun deadrun : block.getDeadrunsInBlock())
//					{
//						Assert.assertTrue(!this.deadrunsInSolution.contains(deadrun));
//						this.deadrunsInSolution.add(deadrun); 
//					}
//					
//					for(IdleTime idleTime : block.getIdleTimesInBlock())
//					{
//						Assert.assertTrue(!this.idleTimesInSolution.contains(idleTime));
//						this.idleTimesInSolution.add(idleTime); 
//					}
//				}
//			}
//			
//		}
//		else
//		{
//			System.out.println("Problem infeasible");
//		}
//	}
//	
//	
//
//	
//}
