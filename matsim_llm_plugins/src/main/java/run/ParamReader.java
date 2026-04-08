package run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.collections.Tuple;


/**
 * This class reads the default parameter values,limit, sub-population and any other code specified for the parameters 
 * 
 * The file should be in .csv
 * It should contain a header.
 * 
 * The columns should be 
 * SubPopulation,ParameterName,id,LowerLimit,UpperLimit,CurretValue,Code,IncludeIninitialParam
 * 
 * The id will be generated in the constructor as SubPopulationName+space+ParameterName
 * 
 * The code will be used to identify the parameters. This code is provided to facilitate same parameters for different subPopulation
 * same code parameters will be treated as one parameter. 
 * 
 * Parameter Names should include all the parameter names mentioned in AnalyticalModel Interface for all subPopulations
 * subPopulation names containing GV may exclude the parameters related to PT 
 * 
 * If There is no sub-population, then sub-population field should contain All. 
 * 
 * @author Ashraf
 *
 */
public class ParamReader {
/**
 * This file reads params and create the ParameterLimits
 */
	private final File paramFile;
	private final String defaultFileLoc="src/main/resources/paramReaderTrial1.csv";
	private ArrayList<String> subPopulationName=new ArrayList<>();
	
	//In No Code Format
	private LinkedHashMap<String,Double>DefaultParam=new LinkedHashMap<>();
	
	private boolean allowUnkownParamaeterWhileScalingUp=false;
	
	//In No Code Format
	private LinkedHashMap<String,Tuple<Double,Double>>paramLimit=new LinkedHashMap<>();
	private ArrayList<String>paramName=new ArrayList<>();
	
	//String-NoCode pair
	private LinkedHashMap<String,String> ParamNoCode=new LinkedHashMap<>();
	
	//In No Code Format
	private LinkedHashMap<String,Double>initialParam=new LinkedHashMap<>();
	
	//In No Code Format (This include limit for only the parameters that are inculded in the initial param)
	private LinkedHashMap<String,Tuple<Double,Double>>initialParamLimit=new LinkedHashMap<>();
	
	private static final Logger logger=LogManager.getLogger(ParamReader.class);
	
	public ParamReader(String fileLoc) {
		File file=new File(fileLoc);
		if(file.exists()) {
			this.paramFile=file;
		}else {
			this.paramFile=new File(defaultFileLoc);
		}
		try {
			BufferedReader bf=new BufferedReader(new FileReader(this.paramFile));
			bf.readLine();//getReadof the header
			String line;
			while((line=bf.readLine())!=null) {
				String[] part=line.split(",");
				String subPopName=part[0];
				String paramName=part[1];
				Double paramValue=Double.parseDouble(part[5]);
				Double upperLimit=Double.parseDouble(part[4]);
				Double lowerLimit=Double.parseDouble(part[3]);
				String paramId=part[2];
				if(subPopName.equals("")) {
					paramId=part[1];
				}else {
					paramId=part[0]+" "+part[1];
				}
				this.DefaultParam.put(part[6], paramValue);
				this.paramLimit.put(part[6], new Tuple<Double,Double>(lowerLimit,upperLimit));
				if(!this.subPopulationName.contains(subPopName) && !subPopName.equals("All") && !subPopName.equals("")) {
					this.subPopulationName.add(subPopName);
				}
				this.paramName.add(paramName);
				if(part[6]!=null) {
					this.ParamNoCode.put(paramId, part[6]);
				}
				if(Boolean.parseBoolean(part[7])==true) {
					this.initialParam.put(part[6], paramValue);
					this.initialParamLimit.put(part[6], this.paramLimit.get(part[6]));
				}
			}
			bf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public LinkedHashMap<String, Double> getInitialParam() {
		return initialParam;
	}


	public LinkedHashMap<String, Tuple<Double, Double>> getInitialParamLimit() {
		return initialParamLimit;
	}
	
	

	public void setInitialParam(LinkedHashMap<String, Double> initialParam) {
		this.initialParam = initialParam;
	}


	public void setInitialParamLimit(LinkedHashMap<String, Tuple<Double, Double>> initialParamLimit) {
		this.initialParamLimit = initialParamLimit;
	}



	public String getDefaultFileLoc() {
		return defaultFileLoc;
	}


	public ArrayList<String> getSubPopulationName() {
		return subPopulationName;
	}


	public LinkedHashMap<String, Double> getDefaultParam() {
		return DefaultParam;
	}


	public LinkedHashMap<String, Tuple<Double, Double>> getParamLimit() {
		return paramLimit;
	}


	public ArrayList<String> getParamName() {
		return paramName;
	}
	
	public static HashMap<String, Tuple<Double,Double>> getDefaultTimeBean() {
		HashMap<String, Tuple<Double,Double>> timeBean=new HashMap<>();
		timeBean.put("BeforeMorningPeak", new Tuple<Double,Double>(0.0,25200.));
		timeBean.put("MorningPeak", new Tuple<Double,Double>(25200.,36000.));
		timeBean.put("AfterMorningPeak", new Tuple<Double,Double>(36000.,57600.));
		timeBean.put("EveningPeak", new Tuple<Double,Double>(57600.,72000.));
		timeBean.put("AfterEveningPeak", new Tuple<Double,Double>(72000.,86400.));
		return timeBean;
	}
	
	
	/**
	 * This will scale up the parameter from no format to Parameter name format
	 * The sent parameter is not changed. A new parameter is generated and returned
	 * If the parameter sent is already in String format, than nothing will be changed.
	 * @param trialParam
	 * @return
	 */
	public LinkedHashMap<String,Double> ScaleUp(Map<String, Double> trialParam) throws IllegalArgumentException{
		if((this.ParamNoCode.values()).containsAll(trialParam.keySet())) {
			
		}else if((this.ParamNoCode.keySet()).containsAll(trialParam.keySet())) {
			logger.warn("Parameter is already scaled up, i.e. in ParamName-Value format. Method will exit.");
			return new LinkedHashMap<String,Double>(trialParam);
		}else {
			if(this.allowUnkownParamaeterWhileScalingUp==false) {
				logger.error("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
				throw new IllegalArgumentException("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
			}
		}
		LinkedHashMap<String,Double> scaledParam=new LinkedHashMap<String,Double>();
		for(Entry<String, String> e:this.ParamNoCode.entrySet()) {
			if(trialParam.get(e.getValue())==null) {
				//scaledParam.put(e.getKey(),this.DefaultParam.get(e.getValue()));
			}else {
				scaledParam.put(e.getKey(), trialParam.get(e.getValue()));
			}
		}
		
		if(this.allowUnkownParamaeterWhileScalingUp) {
			for(Entry<String,Double> parameter:trialParam.entrySet()) {
				if(!this.ParamNoCode.containsValue(parameter.getKey())){
					scaledParam.put(parameter.getKey(), parameter.getValue());
				}
			}
		}
		//System.out.println();
		return scaledParam;
	}
	
	
	public boolean isAllowUnkownParamaeterWhileScalingUp() {
		return allowUnkownParamaeterWhileScalingUp;
	}


	public void setAllowUnkownParamaeterWhileScalingUp(boolean allowUnkownParamaeterWhileScalingUp) {
		this.allowUnkownParamaeterWhileScalingUp = allowUnkownParamaeterWhileScalingUp;
	}


	public LinkedHashMap<String, Tuple<Double, Double>> ScaleUpLimit(LinkedHashMap<String,Tuple<Double,Double>>param) throws IllegalArgumentException{
		if((this.ParamNoCode.values()).containsAll(param.keySet())) {
			
		}else if((this.ParamNoCode.keySet()).containsAll(param.keySet())) {
			logger.warn("Parameter is already scaled up, i.e. in ParamName-Value format. Method will exit.");
			return new LinkedHashMap<String,Tuple<Double,Double>>(param);
		}else {
			logger.error("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
			throw new IllegalArgumentException("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
		}
		LinkedHashMap<String,Tuple<Double,Double>> scaledParam=new LinkedHashMap<>();
		for(Entry<String, String> e:this.ParamNoCode.entrySet()) {
			if(param.get(e.getValue())==null) {
				//scaledParam.put(e.getKey(),this.DefaultParam.get(e.getValue()));
			}else {
				scaledParam.put(e.getKey(), param.get(e.getValue()));
			}
		}
		return scaledParam;
	}
	
	/**
	 * This method will convert the ParameterName-Value format parameters to ParameterNo-Value Format
	 * @param param
	 * @return
	 */
	public LinkedHashMap<String,Double> ScaleDown(LinkedHashMap<String,Double>param) throws IllegalArgumentException{
		Set<String> keys = new HashSet<>(param.keySet());
		keys.retainAll(this.ParamNoCode.keySet());
		if(keys.size()==0) {
			logger.warn("nothing to scale down. Method will exit.");
			return new LinkedHashMap<String,Double>(param);
		}
		//else {
		//logger.error("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
		//throw new IllegalArgumentException("Invalid input. Params can be either in ParamName-Value format or ParamCode-Value Format");
	//}
		
		LinkedHashMap<String,Double> scaledDownParam=new LinkedHashMap<String,Double>();
		for(String s:param.keySet()) {
			scaledDownParam.put(this.ParamNoCode.get(s), param.get(s));
		}
		return scaledDownParam;
	}
	
	public LinkedHashMap<String,Double>generateSubPopSpecificParam(LinkedHashMap<String,Double>originalparams,String subPopName){
		LinkedHashMap<String,Double> specificParam=new LinkedHashMap<>();
		for(String s:originalparams.keySet()) {
			if(s.contains(subPopName)||s.contains("All")) {
				specificParam.put(s.split(" ")[1],originalparams.get(s));
			}
		}
		return specificParam;
	}
	
	
	/**
	 * This param Should be in No code-value format. But if not it will  be converted automatically. 
	 * @param config
	 * @param Nparams
	 * @return
	 */
	public Config SetParamToConfig(Config config, LinkedHashMap<String, Double> noparams) {
		System.out.println(config.isLocked());
		LinkedHashMap<String,Double> Nparams=this.ScaleDown(noparams);
		LinkedHashMap<String,Double>Noparams=new LinkedHashMap<>(Nparams);
		new ConfigWriter(config).write("config_Intermediate.xml");
		Config configOut=ConfigUtils.loadConfig("config_Intermediate.xml");
		for(String s:this.DefaultParam.keySet()) {
			if(Noparams.get(s)==null) {
				Noparams.put(s,this.DefaultParam.get(s));
			}
		}
		LinkedHashMap<String,Double> params=this.ScaleUp(Noparams);
		
		if(this.subPopulationName.size()!=0) {
		for(String subPop:this.subPopulationName) {
			if(!subPop.contains("GV")) {
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelCarName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofDistanceCarName));
			configOut.scoring().getOrCreateScoringParameters(subPop).setMarginalUtilityOfMoney(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofMoneyName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostCarName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelptName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfDistancePtName));
			configOut.scoring().getOrCreateScoringParameters(subPop).setMarginalUtlOfWaitingPt_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofWaitingName));
			configOut.scoring().getOrCreateScoringParameters(subPop).setUtilityOfLineSwitch(params.get(subPop+" "+AnalyticalModel.UtilityOfLineSwitchName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfWalkingName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostWalkName));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setConstant(params.get(subPop+" "+AnalyticalModel.ModeConstantPtname));
			configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setConstant(params.get(subPop+" "+AnalyticalModel.ModeConstantCarName));
			configOut.scoring().getOrCreateScoringParameters(subPop).setPerforming_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofPerformName));
			
			}else {
				configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelCarName));
				configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofDistanceCarName));
				configOut.scoring().getOrCreateScoringParameters(subPop).setMarginalUtilityOfMoney(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofMoneyName));
				configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostCarName));
				configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfWalkingName));
				configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostWalkName));
				configOut.scoring().getOrCreateScoringParameters(subPop).setPerforming_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofPerformName));
			}
		}
		}else {
			configOut.scoring().getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelCarName));
			configOut.scoring().getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(AnalyticalModel.MarginalUtilityofDistanceCarName));
			configOut.scoring().setMarginalUtilityOfMoney(params.get(AnalyticalModel.MarginalUtilityofMoneyName));
			configOut.scoring().getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostCarName));
			configOut.scoring().getOrCreateModeParams("pt").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelptName));
			configOut.scoring().getOrCreateModeParams("pt").setMonetaryDistanceRate(params.get(AnalyticalModel.MarginalUtilityOfDistancePtName));
			configOut.scoring().setMarginalUtlOfWaitingPt_utils_hr(params.get(AnalyticalModel.MarginalUtilityofWaitingName));
			configOut.scoring().setUtilityOfLineSwitch(params.get(AnalyticalModel.UtilityOfLineSwitchName));
			configOut.scoring().getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityOfWalkingName));
			configOut.scoring().getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostWalkName));
			configOut.scoring().getOrCreateModeParams("pt").setConstant(params.get(AnalyticalModel.ModeConstantPtname));
			configOut.scoring().getOrCreateModeParams("car").setConstant(params.get(AnalyticalModel.ModeConstantCarName));
			configOut.scoring().setPerforming_utils_hr(params.get(AnalyticalModel.MarginalUtilityofPerformName));
		}
		if(params.containsKey(AnalyticalModel.CapacityMultiplierName)) {
			configOut.qsim().setFlowCapFactor(params.get(AnalyticalModel.CapacityMultiplierName));
		}else {
			Double factor=params.get("All "+AnalyticalModel.CapacityMultiplierName);
			if(factor!=null)configOut.qsim().setFlowCapFactor(factor);
		}
		return configOut;
	}
	
	
	public void setDefaultParams(Config configOut, String subPop) {
		LinkedHashMap<String,Double> params=this.ScaleUp(this.DefaultParam);
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelCarName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(AnalyticalModel.MarginalUtilityofDistanceCarName));
		configOut.scoring().getOrCreateScoringParameters(subPop).setMarginalUtilityOfMoney(params.get(AnalyticalModel.MarginalUtilityofMoneyName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostCarName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelptName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMonetaryDistanceRate(params.get(AnalyticalModel.MarginalUtilityOfDistancePtName));
		configOut.scoring().getOrCreateScoringParameters(subPop).setMarginalUtlOfWaitingPt_utils_hr(params.get(AnalyticalModel.MarginalUtilityofWaitingName));
		configOut.scoring().getOrCreateScoringParameters(subPop).setUtilityOfLineSwitch(params.get(AnalyticalModel.UtilityOfLineSwitchName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityOfWalkingName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostWalkName));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setConstant(params.get(AnalyticalModel.ModeConstantPtname));
		configOut.scoring().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setConstant(params.get(AnalyticalModel.ModeConstantCarName));
		configOut.scoring().getOrCreateScoringParameters(subPop).setPerforming_utils_hr(params.get(AnalyticalModel.MarginalUtilityofPerformName));
	}
	
	
}
