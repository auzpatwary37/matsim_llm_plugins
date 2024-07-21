package langchaintrial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.output.Response;
import rest.Prompt;

public class LangchainChatbot {

	public static void main(String[] args) {
		String modelName = "Hermes";
		String baseUrl = "http://localhost:1234/v1/";//11434

		ChatLanguageModel model = MistralAiChatModel.builder()
				.baseUrl(baseUrl)
				.modelName(modelName)
				.maxTokens(4012)
				.logRequests(true)
				.logResponses(true)
				.build();

		List<ToolSpecification> tools = new ArrayList<>();
		List<ChatMessage> chats = new ArrayList<>();
		chats.add(new SystemMessage(Prompt.prompt2));
		chats.add(new UserMessage("Hi, give me an example daily plan."));

		
		ToolSpecification planGsonSpec = ToolSpecification.builder()
				.name("getPlanOut")
				.description("Method to get Matsim Plan from PlanGson class.")
				.addParameter("arg0", List.of(
						new JsonSchemaProperty("type", "object"),
						new JsonSchemaProperty("properties",Map.of(
								"activitiesAndLegs",Map.of(
										"type", "array",
										"description","The alternating list of activity and legs starting and ending with activity",
										"items", Map.of(
												"type", "object",
												"items", Map.of(
														"type", "object",
														"properties", Map.of(
																"activity", Map.of(
																		"type", "object",
																		"description","Activity object containing details of the activity performed by agent.",
																		"properties", Map.of(
																				"id", Map.of("type", "string","description","the unique identifier of an activity inside the plan. The id of an acitivity will be its type+___+the order of occurance of that specific activity type in the plan"),
																				"activityType", Map.of("type", "string","description","type of activity",
																						"enum",List.of("home","work","errands","shop","leisure","education","plugin","plugout")),
																				"endTime", Map.of("type", "number","description","the end time of a certain activity."),
																				"carLocation", Map.of("type", "string","description","current location of the vehicle owned by the agent. The vehicle location will be the facility id or the activity id whichever is available. The vehicle location will be same as the last activity if the leg before this activity is not car leg. If it is car leg. then the vehicle location will be updated to the current activity."),
																				"linkId", Map.of("type", "string","description","the id of the link of the activity"),
																				"facilityId", Map.of("type", "string","description","the id of the facility of the activity"),
																				"coord", Map.of(
																						"type", "object",
																						"description","the x and y coordinate of the current activity location",
																						"properties", Map.of(
																								"x", Map.of("type", "number"),
																								"y", Map.of("type", "number")
																								)
																						),
																				"typicalDuration", Map.of("type", "number","description","the typical duration of this kind of activity"),
																				"maximumDuration", Map.of("type", "number","description","The maximum duration this activity can be performed."),
																				"typicalSoc", Map.of("type", "number","description","Typical state of charage level of the EV when the agent performs this activity.")
																				),
																		"required", List.of("id", "activityType", "endTime", "carLocation")
																		),
																"leg", Map.of(
																		"type", "object",
																		"desciption","the trip leg that connects the activity before and after it",
																		"properties", Map.of(
																				"mode", Map.of("type", "string","description","mode of transport for this leg.","enum",List.of("car","bike","pt","walk","car_passenger")),
																				"distance", Map.of("type", "number","description","Eucleadian distance of this trip leg")
																				),
																		"required", List.of("mode")
																		)
																)

														)
												)
										)
								)
								)

						)
						)

				.build();



		tools.add(planGsonSpec);

		Response<AiMessage> response = model.generate(chats,tools);
		System.out.println(response.content().text());

	}



}
