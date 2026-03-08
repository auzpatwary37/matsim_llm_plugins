package chatcommons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import matsimdtobjects.PlanDTO;
import tools.DefaultToolManager;
import tools.Implement.ExtractPlanTool;

public class SchemaTest {
	public static void main(String[] args) {
		 Gson gson = new GsonBuilder()
	                .setPrettyPrinting()
	                .create();

	        String prettySchema = gson.toJson(PlanDTO.getJsonSchema());

	 
	        System.out.println(prettySchema);
	        
	        ExtractPlanTool tool1 = new ExtractPlanTool();
	        DefaultToolManager manager = new DefaultToolManager();
	        manager.registerTool(tool1);
	        
	        
	        
	        prettySchema = gson.toJson(manager.getAllToolSchemas());

	   	 
	        System.out.println(prettySchema);
	}

}
