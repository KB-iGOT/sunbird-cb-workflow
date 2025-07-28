package org.sunbird.workflow.config;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
class ElasticsearchConfigTest {

    @Test
    void testElasticsearchClient() throws Exception {
        // Arrange
        ElasticsearchConfig config = new ElasticsearchConfig();
        setField(config, "elasticsearchHost", "localhost,127.0.0.1");
        setField(config, "elasticsearchPort", "9200,9201"); // port parsing exists but is ignored

        // Act
        RestHighLevelClient client = config.elasticsearchClient();

        // Assert
        assertNotNull(client);
        RestClient lowLevelClient = client.getLowLevelClient();
        assertNotNull(lowLevelClient);

        // Cleanup
        client.close();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
