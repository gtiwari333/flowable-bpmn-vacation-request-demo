package flowabledemo.config;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/process")
public interface DefinitionsApi {
    @HttpExchange(value = "/{processDefinitionId}/image", contentType = MediaType.IMAGE_PNG_VALUE, method = "GET")
    ResponseEntity<byte[]> getProcessImage(@PathVariable String processDefinitionId);

    @HttpExchange(value = "/{processDefinitionKey}/{businessKey}/image", contentType = MediaType.IMAGE_PNG_VALUE, method = "GET")
    ResponseEntity<byte[]> getProcessImageAtCurrentState(@PathVariable String processDefinitionKey, @PathVariable String businessKey);
}
