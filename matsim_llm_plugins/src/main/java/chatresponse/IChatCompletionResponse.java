package chatresponse;

import java.util.List;

public interface IChatCompletionResponse {
    List<? extends IChoice> getChoices();
    String getModel();
    IUsage getUsage(); // can return null
}

