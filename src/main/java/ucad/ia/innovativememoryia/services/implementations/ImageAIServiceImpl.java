package ucad.ia.innovativememoryia.services.implementations;

import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ucad.ia.innovativememoryia.services.ImageAIService;

import java.io.IOException;
import java.util.Base64;
@Service
public class ImageAIServiceImpl implements ImageAIService {
    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;
    @Override
    public byte[] generateImageDALLE() throws IOException {
        OpenAiImageApi openAiApi = new OpenAiImageApi(openAiKey);
        OpenAiImageModel openAiImageModel = new OpenAiImageModel(openAiApi);
        ImageResponse response = openAiImageModel.call(
                new ImagePrompt("un chat developpeur avec un costume et machine dans une fête avec un café dans sa main ",
                OpenAiImageOptions.builder()
                        .withModel("dall-e-3")
                        .withQuality("hd")
                        .withN(1)
                        .withResponseFormat("b64_json")
                        .withHeight(1024)
                        .withWidth(1024)
                        .build())
        );

        String image = response.getResult().getOutput().getB64Json();
        byte[] decode = Base64.getDecoder().decode(image);
        return decode;
    }

}
