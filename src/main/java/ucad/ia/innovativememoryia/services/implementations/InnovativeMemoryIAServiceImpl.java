package ucad.ia.innovativememoryia.services.implementations;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
public class InnovativeMemoryIAServiceImpl implements InnovativeMemoryIAService {
    private VectorStore vectorStore;
    private JdbcTemplate jdbcTemplate;

    private final OpenAiService openAiService;

    public InnovativeMemoryIAServiceImpl(VectorStore vectorStore, JdbcTemplate jdbcTemplate, String apiKey) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.openAiService = new OpenAiService(apiKey);
    }

    @Override
    public Boolean uploadFile(List<MultipartFile> files) {
        boolean extensionExists = jdbcTemplate.query(
                "SELECT 1 FROM pg_extension WHERE extname = 'vector'",
                (rs, rowNum) -> rs.getInt(1)
        ).size() > 0;

        if (!extensionExists) {
            jdbcTemplate.execute("CREATE EXTENSION vector");
        }

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS vector_store (id SERIAL PRIMARY KEY, content TEXT, embedding VECTOR(1536))");
        jdbcTemplate.update("DELETE FROM vector_store");
        try {
            PdfDocumentReaderConfig config=PdfDocumentReaderConfig.defaultConfig();
            List<Document> allDocuments = new ArrayList<>();
            for (MultipartFile file : files) {
                PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(file.getResource(), config);
                List<Document> documents = pagePdfDocumentReader.get();
                allDocuments.addAll(documents);
                System.out.println(documents.get(documents.indexOf(file)+1).getContent());
            }
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.split(allDocuments);
            vectorStore.accept(chunks);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String chat(String question) {
        List<Document> documents = vectorStore.similaritySearch(question);
        String systemMessageTemplate="""
                You are a virtual assistant to make conversation summaries
                in the number of lines desired by the requester or 5 to 10 by default. 
                For the language you will provide, the text according to the language 
                requested by the user or by default the language of the question. 
                If the question is not about the context, answer with I don't know
                CONTEXT: {CONTEXT}
                """;
        Message systemeMessage=new SystemPromptTemplate(systemMessageTemplate).createMessage(Map.of("CONTEXT",documents));
        UserMessage userMessage=new UserMessage(question);
        Prompt prompt=new Prompt(List.of(systemeMessage, userMessage));
        OpenAiApi openAiApi=new OpenAiApi("");
        OpenAiChatModel model=new OpenAiChatModel(openAiApi, OpenAiChatOptions.
                builder().
                withModel(OpenAiApi.ChatModel.GPT_4_O).
                withTemperature(0.5)
                .withStreamUsage(true).
                build());
        ChatResponse chatResponse = model.call(prompt);
        return chatResponse.getResult().getOutput().getContent();
    }

    @Override
    public String audio(MultipartFile audioFile)
    {
        // Convertir MultipartFile en File temporaire
        File convertedFile = null;
        try {
            convertedFile = convertMultipartFileToFile(audioFile);

            // Créer la requête de transcription
            CreateTranscriptionRequest request = new CreateTranscriptionRequest();
            request.setModel("whisper-1");

            // Appeler l'API OpenAI pour la transcription
            String response = openAiService.createTranscription(request, convertedFile).getText();

            // Retourner le texte transcrit
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la transcription de l'audio.";
        } finally {
            // Supprimer le fichier temporaire après utilisation
            if (convertedFile != null && convertedFile.exists()) {
                convertedFile.delete();
            }
        }
    }
    // Méthode utilitaire pour convertir MultipartFile en File
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("audio", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }


}
