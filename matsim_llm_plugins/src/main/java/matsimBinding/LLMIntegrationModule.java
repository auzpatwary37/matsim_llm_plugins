package matsimBinding;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import chatcommons.ChatCompletionClientImpl;
import chatcommons.ChatManagerContainer;
import chatcommons.IChatCompletionClient;
import rag.IVectorDB;
import rag.VectorDBImplement;
import tools.DefaultToolManager;
import tools.IToolManager;

public class LLMIntegrationModule extends AbstractModule {

    private final DefaultToolManager toolManager = new DefaultToolManager();
    private final ChatManagerContainer container = new ChatManagerContainer();

    public DefaultToolManager getToolManager() {
        return toolManager;
    }

    public ChatManagerContainer getChatManagerContainer() {
        return container;
    }

    @Override
    public void install() {
        // These are pre-created so tools can be registered before controler.run()
        bind(IToolManager.class).toInstance(toolManager);
        bind(ChatManagerContainer.class).toInstance(container);

        // These are config-based and created at runtime by Guice
        bind(IVectorDB.class).toProvider(VectorDbProvider.class).in(Singleton.class);
        bind(IChatCompletionClient.class).toProvider(ChatClientProvider.class).in(Singleton.class);
    }
}


class VectorDbProvider implements Provider<IVectorDB> {

    @Inject
    private Config config;

    @Override
    public IVectorDB get() {
        VectorDBImplement vectorDb = new VectorDBImplement((LLMConfigGroup)config.getModules().get(LLMConfigGroup.GROUP_NAME));
        return vectorDb;
    }
}


class ChatClientProvider implements Provider<IChatCompletionClient> {

    @Inject
    private Config config;

    @Override
    public IChatCompletionClient get() {
        return new ChatCompletionClientImpl((LLMConfigGroup)config.getModules().get(LLMConfigGroup.GROUP_NAME));
    }
}

