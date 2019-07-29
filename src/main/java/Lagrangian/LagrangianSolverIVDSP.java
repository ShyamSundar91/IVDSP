//package Lagrangian;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.junit.Assert;
//
//import Data.Trip;
//import Variables.Block;
//import Variables.Deadrun;
//import Variables.Duty;
//import Variables.IdleTime;
//import ilog.concert.IloException;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.concert.IloRange;
//import ilog.cplex.IloCplex;
//import ilog.cplex.IloCplex.UnknownObjectException;
//import lombok.Getter;
//
//
//public class LagrangianSolverIVDSP 
//{
//	private IloCplex vehicleCplex;
//	private IloCplex driverCplex;
//	private double initialUpperBound; 
//	private Map<Block, IloNumVar> blockVariables;  
//	private Map<Duty, IloNumVar> dutyVariables; 
//	private Map<Deadrun, Double> deadrunLowerLimitMultiplier; 
//	private Map<IdleTime, Double> idleTimeMultiplier; 
//
//	private Set<Deadrun> deadruns; 
//	private Set<IdleTime> idleTimes; 
//	
//	private Map<Deadrun, Double> deadrunLowerLimitSubgradient; 
//	private Map<IdleTime, Double> idleTimeSubgradient; 
//	
//	@Getter
//	private double lagrangianLowerBound; 
//	private double lowerBound; 
//	private double decayRate; 
//	private int lastImprovedSolution; 
//	
//	public LagrangianSolverIVDSP(IloCplex vehicleCplex, IloCplex driverCplex, double initialUpperBound, Map<Block, IloNumVar> blockVariables, Map<Duty, IloNumVar> dutyVariables, Map<Deadrun, Double> deadrunLowerLimitMultiplier, Map<IdleTime, Double> idleTimeMultiplier,  Set<Deadrun> deadruns, Set<IdleTime> idleTimes) throws UnknownObjectException, IloException
//	{
//		this.vehicleCplex = vehicleCplex; 
//		this.driverCplex = driverCplex; 
//		this.initialUpperBound = initialUpperBound;  
//		this.blockVariables = blockVariables; 
//		this.dutyVariables = dutyVariables; 
//		this.deadrunLowerLimitMultiplier = deadrunLowerLimitMultiplier; 
//		this.idleTimeMultiplier = idleTimeMultiplier; 
//
//		this.deadruns = deadruns; 
//		this.idleTimes = idleTimes; 
//		 
//		this.deadrunLowerLimitSubgradient = new HashMap<Deadrun, Double>(); 
//		this.idleTimeSubgradient = new HashMap<IdleTime, Double>();
//		initializeSubgradients(); 
//		
//		this.lagrangianLowerBound = -Double.MAX_VALUE; 
//		this.lowerBound = 0.0; 
//		this.decayRate = 2; 
//		this.lastImprovedSolution = 0; 
//		
//		subgradientAlgorithm(); 
//	}
//	
//	private void initializeSubgradients()
//	{
//		for(Deadrun deadrun : this.deadruns)
//		{
//			this.deadrunLowerLimitSubgradient.put(deadrun, 0.0); 
//		}
//		
//		for(IdleTime idleTime : this.idleTimes)
//		{
//			this.idleTimeSubgradient.put(idleTime, 0.0); 
//		}
//		
//	}
//	
//	private void subgradientAlgorithm() throws UnknownObjectException, IloException
//	{
//		boolean stop = false; 
//		int iter = 0; 
//		
//		while(!stop)
//		{
//			adaptOjectiveFunction();
//			
//			solve(iter); 
//			
//			stop = checkTermination(iter); 
//			
//			if(!stop)
//			{
//				updateMultipliers(); 
//				
//				initializeSubgradients();
//			}
//			
//			
//			iter++; 
//		}
//		
//		for(Block block : this.blockVariables.keySet())
//		{
//			double value = this.vehicleCplex.getReducedCost(this.blockVariables.get(block)); 
//			
//			if(value < -1e-3)
//			{
//				System.out.println("Block variable " + block.getBlockId() + " less than 0 = " + value + ", " + this.vehicleCplex.getValue(this.blockVariables.get(block)));
//			}
//		}
//		
//		for(Duty duty : this.dutyVariables.keySet())
//		{
//			double value = this.driverCplex.getReducedCost(this.dutyVariables.get(duty)); 
//			
//			if(value < -1e-3)
//			{
//				System.out.println("Duty variable " + duty.getDutyId() + " less than 0 = " + value + ", " + this.driverCplex.getValue(this.dutyVariables.get(duty)));
//			}
//		}
//		
//	}
//	
//	private void solve(int iter) throws UnknownObjectException, IloException
//	{
//		this.lowerBound = 0.0; 
//		this.vehicleCplex.setParam(IloCplex.IntParam.AdvInd, 0);
//		this.driverCplex.setParam(IloCplex.IntParam.AdvInd, 0);
//		
//		if(this.vehicleCplex.solve())
//		{
//			this.lowerBound = this.vehicleCplex.getObjValue();
//			
//			for(Block block : this.blockVariables.keySet())
//			{
//				double value = this.vehicleCplex.getValue(this.blockVariables.get(block)); 
//				if(value > 1e-3)
//				{
//					//System.out.println("Block = " + block.getBlockId() + "; " + value);
//					for(Deadrun deadrun : block.getDeadrunsInBlock())
//					{
//						double coef = this.deadrunLowerLimitSubgradient.get(deadrun); 
//						coef = coef + value; 
//						this.deadrunLowerLimitSubgradient.replace(deadrun, coef); 
//					}
//					
//					for(IdleTime idleTime : block.getIdleTimesInBlock())
//					{
//						double coef = this.idleTimeSubgradient.get(idleTime); 
//						coef = coef + value;
//						this.idleTimeSubgradient.replace(idleTime, coef); 
//					}
//				}
//			}
//		}
//		else
//		{
//			System.out.println("Infeasible");
//		}
//		
//		if(this.driverCplex.solve())
//		{
//			this.lowerBound = this.lowerBound + this.driverCplex.getObjValue(); 
//			System.out.println(this.lowerBound);
//			
//			for(Duty duty : this.dutyVariables.keySet())
//			{
//				double value = this.driverCplex.getValue(this.dutyVariables.get(duty)); 
//				if(value > 1e-3)
//				{
//					for(Deadrun deadrun : duty.getDeadrunsInDuty())
//					{
//						double coef = this.deadrunLowerLimitSubgradient.get(deadrun); 
//						coef = coef - value; 
//						this.deadrunLowerLimitSubgradient.replace(deadrun, coef); 
//					}
//					
//					for(IdleTime idleTime : duty.getIdleTimesInDuty())
//					{
//						double coef = this.idleTimeSubgradient.get(idleTime); 
//						coef = coef - value; 
//						this.idleTimeSubgradient.replace(idleTime, coef); 
//					}
//				}
//			}
//		}
//		else
//		{
//			System.out.println("Infeasible");
//		}
//		
//		if(this.lowerBound > this.lagrangianLowerBound)
//		{
//			this.lagrangianLowerBound = this.lowerBound; 
//			this.lastImprovedSolution = 0; 
//		}
//		else
//		{
//			this.lastImprovedSolution++; 
//		}
//			
//	}
//	
//	
//	private void updateMultipliers()
//	{
//		double denominatorOfDeadrun = 0.0; 
//		for(Deadrun deadrun : this.deadrunLowerLimitSubgradient.keySet())
//		{
//			denominatorOfDeadrun = denominatorOfDeadrun + Math.pow(this.deadrunLowerLimitSubgradient.get(deadrun), 2); 
//		}
//		
//		double denominatorOfIdleTime = 0.0; 
//		for(IdleTime idleTime : this.idleTimeSubgradient.keySet())
//		{
//			denominatorOfIdleTime = denominatorOfIdleTime + Math.pow(this.idleTimeSubgradient.get(idleTime), 2); 
//		}
//		
//		double stepSizeOfDeadrun = 1.0; 
//		//System.out.println("Denominator = " + denominatorOfDeadrun);
//		//System.out.println("Value = " + Math.abs((this.initialUpperBound - this.lowerBound)));
//		if(denominatorOfDeadrun > 0.1)
//		{
//			stepSizeOfDeadrun =   Math.abs((this.initialUpperBound - this.lowerBound))/denominatorOfDeadrun;
//		}
//		
//		double stepSizeOfIdleTime = 1.0; 
//		if(denominatorOfIdleTime >= 0.1)
//		{
//			stepSizeOfIdleTime =   Math.abs((this.initialUpperBound - this.lowerBound))/denominatorOfIdleTime; 
//		}
//		
//		for(Deadrun deadrun : this.deadruns)
//		{
//			double newLowerLimitMultiplier =   Math.max(0.0, this.deadrunLowerLimitMultiplier.get(deadrun) + (this.decayRate * stepSizeOfDeadrun * this.deadrunLowerLimitSubgradient.get(deadrun)));
//			//newLowerLimitMultiplier = Math.min(newLowerLimitMultiplier, 10000); 
//			this.deadrunLowerLimitMultiplier.replace(deadrun, newLowerLimitMultiplier); 
//			//System.out.println("Deadrun = " + deadrun.getDeadrunId() + "; " + newLowerLimitMultiplier);
//		}
//		
//		for(IdleTime idleTime : this.idleTimes)
//		{
//			double newIdleTimeMultiplier =  Math.max(0.0, this.idleTimeMultiplier.get(idleTime) + (this.decayRate * stepSizeOfIdleTime * this.idleTimeSubgradient.get(idleTime))); 
//			//newIdleTimeMultiplier = Math.min(newIdleTimeMultiplier,10000); 
//			this.idleTimeMultiplier.replace(idleTime, newIdleTimeMultiplier); 
//		}
//	}
//	
//	private void adaptOjectiveFunction() throws IloException
//	{
//		
//		this.vehicleCplex.delete(this.vehicleCplex.getObjective());
//		
//		IloLinearNumExpr vehObj = this.vehicleCplex.linearNumExpr(); 
//		
//		for(Block block : this.blockVariables.keySet())
//		{
//			double coef = block.getTotalCostOfBlock(); 
//			
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
//			vehObj.addTerm(coef, this.blockVariables.get(block));
//		}
//		
//		this.vehicleCplex.addMinimize(vehObj); 
//		
//		
//		this.driverCplex.delete(this.driverCplex.getObjective());
//		
//		IloLinearNumExpr driObj = this.driverCplex.linearNumExpr(); 
//		for(Duty duty : this.dutyVariables.keySet())
//		{
//			double coef = duty.getTotalCostOfDuty(); 
//			
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
//			driObj.addTerm(coef, this.dutyVariables.get(duty));
//		}
//		
//		this.driverCplex.addMinimize(driObj); 
//		
//	 
//	}
//	
//	private boolean checkTermination(int iter)
//	{
//		if(this.lastImprovedSolution > 10)
//		{
//			this.decayRate = this.decayRate/2.0; 
//			this.lastImprovedSolution = 0; 
//			
//			if(this.decayRate < 1e-3)
//			{
//				return true; 
//			}
//		}
//		
//		double gap = (Math.abs(this.initialUpperBound - this.lagrangianLowerBound)/this.initialUpperBound) * 100.00; 
//		if(gap < 0.001)
//		{
//			return true; 
//		}
//		
//		if(iter > 100)
//		{
//			return true; 
//		}
//		
//		
//		return false; 
//		
//	}
//}
