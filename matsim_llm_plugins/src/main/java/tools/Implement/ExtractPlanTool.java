package tools.Implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;

import com.google.gson.JsonObject;

import matsimdtobjects.PlanDTO;
import rag.IVectorDB;
import tools.DefaultToolResponse;
import tools.ErrorMessages;
import tools.ITool;
import tools.IToolResponse;
import tools.ToolArgument;
import tools.ToolArgumentDTO;
import tools.VerificationFailedException;

public class ExtractPlanTool implements ITool<Plan> {
	
	public static final String Name = "extract_plan";

    private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();
    private Map<String, Object> context = new HashMap<>();

    public ExtractPlanTool() {
        registerArgument(
            new ToolArgument<>(
                "plan",
                PlanDTO.class,
                PlanDTO.toDTOFromBaseObject(),
                PlanDTO.getJsonSchema()
            )
        );
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public Class<Plan> getOutputClass() {
        return Plan.class;
    }

    @Override
    public String getDescription() {
        return "Final dummy tool that extracts a complete MATSim plan from structured JSON. "
             + "Use this only when the full plan has been fully decided.";
    }

    @Override
    public boolean isDummy() {
        return true;
    }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return arguments;
    }

    @Override
    public IToolResponse<Plan> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB) {
        Plan plan = (Plan) arguments.get("plan");

        JsonObject response = new JsonObject();
        response.addProperty("status", "OK");
        response.addProperty("message", "Plan extracted successfully.");

        // last boolean = isDummyResponse
        // true -> not returned to LLM, only kept for MATSim/internal use
        return new DefaultToolResponse<>(
            id,
            getName(),
            response.toString(),
            plan,
            true
        );
    }

    @Override
    public void verifyArguments(Map<String, Object> arguments, Map<String, Object> context, ErrorMessages em)
            throws VerificationFailedException {

        List<String> errors = new ArrayList<>();

        if (arguments == null) {
            errors.add("Arguments map is null.");
        } else {
            Object planObj = arguments.get("plan");

            if (planObj == null) {
                errors.add("Missing required argument: plan.");
            } else if (!(planObj instanceof Plan)) {
                errors.add("Argument 'plan' is not a MATSim Plan.");
            } else {
                Plan plan = (Plan) planObj;

                if (plan.getPlanElements() == null || plan.getPlanElements().isEmpty()) {
                    errors.add("The extracted MATSim plan contains no plan elements.");
                }
            }
        }
        em.getErrorMessages().addAll(errors);
        if (!errors.isEmpty()) {
            throw new VerificationFailedException(errors);//should this be thrown or all errors should be collected first? 
        }
    }

    @Override
    public Map<String, Object> getContextObject() {
        return context;
    }

    @Override
    public void setContextObject(Map<String, Object> context) {
        this.context = context;
    }
}