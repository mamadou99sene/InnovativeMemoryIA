package ucad.ia.innovativememoryia.services.implementations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InnovativeMemoryIAServiceImpl implements InnovativeMemoryIAService {
    private VectorStore vectorStore;
    @Override
    public boolean uploadFile(List<Resource> files) {
        PdfDocumentReaderConfig config=PdfDocumentReaderConfig.defaultConfig();
        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : files) {
            PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(resource, config);
            List<Document> documents = pagePdfDocumentReader.get();
            allDocuments.addAll(documents);

        }
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(allDocuments);
        vectorStore.accept(chunks);
        return true;
    }

    @Override
    public String chat(String question) {
        List<Document> documents = vectorStore.similaritySearch(question);
        String systemMessageTemplate="""
                Answer the following question based only on the provided CONTEXT
                CONTEXT: {CONTEXT}
                """;
        Message systemeMessage=new SystemPromptTemplate(systemMessageTemplate).createMessage(Map.of("CONTEXT",documents));
        UserMessage userMessage=new UserMessage(question);
        Prompt prompt=new Prompt(List.of(systemeMessage, userMessage));
        OpenAiApi openAiApi=new OpenAiApi('OPEN_AI_KEY');
        OpenAiChatModel model=new OpenAiChatModel(openAiApi, OpenAiChatOptions.
                builder().
                withModel(OpenAiApi.ChatModel.GPT_4_O).
                withTemperature(0.2).
                build());
        ChatResponse chatResponse = model.call(prompt);
        return chatResponse.getResult().getOutput().getContent();
    }


}
