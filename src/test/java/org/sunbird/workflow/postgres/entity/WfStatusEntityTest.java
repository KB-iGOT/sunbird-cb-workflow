package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WfStatusEntityTest {

    @Test
    void contentId_isNullByDefault() {
        assertNull(new WfStatusEntity().getContentId());
    }

    @Test
    void contentId_canBeSetAndRead() {
        WfStatusEntity entity = new WfStatusEntity();

        entity.setContentId("do_123");

        assertEquals("do_123", entity.getContentId());
    }

    @Test
    void contentId_isMappedToContentIdColumn() throws NoSuchFieldException {
        Column column = WfStatusEntity.class.getDeclaredField("contentId").getAnnotation(Column.class);

        assertEquals("content_id", column.name());
    }
}
