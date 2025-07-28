package org.sunbird.workflow.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SunbirdApiRespParamTest {

    @Test
    void testDefaultConstructorAndSettersAndGetters() {
        SunbirdApiRespParam response = new SunbirdApiRespParam();

        response.setResmsgid("res123");
        response.setMsgid("msg123");
        response.setErr("ERR_CODE");
        response.setStatus("FAILED");
        response.setErrmsg("Something went wrong");

        assertThat(response.getResmsgid()).isEqualTo("res123");
        assertThat(response.getMsgid()).isEqualTo("msg123");
        assertThat(response.getErr()).isEqualTo("ERR_CODE");
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrmsg()).isEqualTo("Something went wrong");
    }

    @Test
    void testParameterizedConstructor() {
        SunbirdApiRespParam response = new SunbirdApiRespParam("id456");

        assertThat(response.getResmsgid()).isEqualTo("id456");
        assertThat(response.getMsgid()).isEqualTo("id456");
    }
}
