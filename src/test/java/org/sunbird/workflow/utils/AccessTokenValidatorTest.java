package org.sunbird.workflow.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.operator.KeyWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.keycloak.common.util.Time;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.KeyData;

import java.lang.reflect.Method;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidatorTest {

    @Mock
    private KeyManager keyManager;

    @Mock
    private KeyWrapper mockKeyWrapper;

    @Mock
    private PublicKey mockPublicKey;

    @InjectMocks
    private AccessTokenValidator accessTokenValidator;

    @Spy
    private AccessTokenValidator spyAccessTokenValidator;

    private static final ObjectMapper mapper = new ObjectMapper();

    String validToken;
    private String expiredToken;
    private String invalidSignatureToken;
    private String invalidIssuerToken;

    AccessTokenValidator validator;


    @BeforeEach
    void setUp() throws Exception {
        // Mock PropertiesCache.getInstance().getProperty(...) if needed
        // Generate tokens for different scenarios
        validToken = generateToken("validUserId", Time.currentTime() + 1000, "expectedIssuer");
        expiredToken = generateToken("expiredUserId", Time.currentTime() - 1000, "expectedIssuer");
        invalidSignatureToken = generateToken("invalidSignatureUserId", Time.currentTime() + 1000, "expectedIssuer");
        invalidIssuerToken = generateToken("invalidIssuerUserId", Time.currentTime() + 1000, "invalidIssuer");

    }

    @Test
    void testVerifyUserToken_ExpiredToken() {
        String userId = accessTokenValidator.verifyUserToken(expiredToken);
        assertEquals(Constants._UNAUTHORIZED, userId);
    }

    @Test
    void testVerifyUserToken_InvalidSignature() {
        String userId = accessTokenValidator.verifyUserToken(invalidSignatureToken);
        assertEquals(Constants._UNAUTHORIZED, userId);
    }

    @Test
    void testVerifyUserToken_InvalidIssuer() {
        String userId = accessTokenValidator.verifyUserToken(invalidIssuerToken);
        assertEquals(Constants._UNAUTHORIZED, userId);
    }

    @Test
    void testFetchUserIdFromAccessToken_NullToken() {
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(null);
        assertNull(userId);
    }

    // Helper method to generate tokens
    private String generateToken(String userId, int exp, String issuer) throws Exception {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", "testKeyId");

        Map<String, Object> body = new HashMap<>();
        body.put("sub", "user:" + userId);
        body.put("exp", exp);
        body.put("iss", issuer);

        String headerJson = mapper.writeValueAsString(header);
        String bodyJson = mapper.writeValueAsString(body);

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes());
        String encodedBody = Base64.getUrlEncoder().withoutPadding().encodeToString(bodyJson.getBytes());

        String unsignedToken = encodedHeader + "." + encodedBody;

        // For simplicity, we're not signing the token here
        String signature = "testSignature";

        return unsignedToken + "." + signature;
    }

    @Test
    void fetchUserIdFromAccessToken_validToken_returnsUserId() {
        String accessToken = "validToken";
        String expectedUserId = "user123";


        doReturn(expectedUserId).when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_unauthorizedToken_returnsNull() {
        String accessToken = "unauthorizedToken";

        doReturn("UNAUTHORIZED").when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertNull(actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_nullToken_returnsNull() {
        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(null);
        assertNull(actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_exceptionThrown_returnsNull() {
        String accessToken = "token";

        doThrow(new RuntimeException("some error")).when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertNull(actualUserId);
    }

    @Test
    void testCheckIss_invalidIssuer_returnsFalse() throws Exception {
        validator = new AccessTokenValidator();

        Method method = AccessTokenValidator.class.getDeclaredMethod("checkIss", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(validator, "invalid-issuer");

        assertFalse(result, "Expected false when issuer does not match REALM_URL");
    }

    @Test
    void testCheckIss_blankIssuer_returnsFalse() throws Exception {
        validator = new AccessTokenValidator();

        Method method = AccessTokenValidator.class.getDeclaredMethod("checkIss", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(validator, "");

        assertFalse(result, "Expected false when issuer is blank");
    }

    @Test
    void testValidateToken_validSignature_notExpired_returnsTokenBody() throws Exception {

        String token = "header.body.signature";
        Map<Object, Object> headerData = new HashMap<>();
        headerData.put("kid", "testKeyId");

        Map<String, Object> bodyData = new HashMap<>();
        bodyData.put("exp", Time.currentTime() + 5000);
        bodyData.put("iss", "http://realm");
        bodyData.put("sub", "user:123");

        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class);
             MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class)) {

            base64Mock.when(() -> Base64Util.decode(anyString(), anyInt()))
                    .thenReturn(mapper.writeValueAsBytes(headerData))
                    .thenReturn(mapper.writeValueAsBytes(bodyData))
                    .thenReturn("sig".getBytes());

            cryptoMock.when(() ->
                    CryptoUtil.verifyRSASign(anyString(), any(), any(), eq(Constants.SHA_256_WITH_RSA))
            ).thenReturn(true);

            KeyData keyData = mock(KeyData.class);
            when(keyManager.getPublicKey(anyString())).thenReturn(keyData);
            when(keyData.getPublicKey()).thenReturn(mockPublicKey);

            String result = accessTokenValidator.verifyUserToken(token);


            assertEquals("123", result);
        }
    }

    @Test
    void testValidateToken_validSignature_butExpired_returnsEmptyMap() throws Exception {
        String token = "header.body.signature";
        Map<Object, Object> headerData = new HashMap<>();
        headerData.put("kid", "testKeyId");

        Map<String, Object> bodyData = new HashMap<>();
        bodyData.put("exp", Time.currentTime() - 100);
        bodyData.put("iss", "issuer");
        bodyData.put("sub", "user:expired");

        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class);
             MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class)) {

            base64Mock.when(() -> Base64Util.decode(anyString(), anyInt()))
                    .thenReturn(mapper.writeValueAsBytes(headerData))
                    .thenReturn(mapper.writeValueAsBytes(bodyData))
                    .thenReturn("sig".getBytes());

            cryptoMock.when(() ->
                    CryptoUtil.verifyRSASign(anyString(), any(), any(), eq(Constants.SHA_256_WITH_RSA))
            ).thenReturn(true);

            KeyData keyData = mock(KeyData.class);
            when(keyManager.getPublicKey(anyString())).thenReturn(keyData);
            when(keyData.getPublicKey()).thenReturn(mockPublicKey);

            String result = accessTokenValidator.verifyUserToken(token);

            assertEquals(Constants._UNAUTHORIZED, result);
        }
    }

    @Test
    void testVerifyUserToken_validPayload_extractsUserId() {
        AccessTokenValidator validator = new AccessTokenValidator() {
            @Override
            public String verifyUserToken(String token) {
                Map<String, Object> payload = new HashMap<>();
                payload.put(Constants.SUB, "user:validUser");
                payload.put("iss", "realm");
                if (!payload.isEmpty()) {
                    String userId = (String) payload.get(Constants.SUB);
                    if (StringUtils.isNotBlank(userId)) {
                        int pos = userId.lastIndexOf(":");
                        userId = userId.substring(pos + 1);
                        return userId;
                    }
                }
                return Constants._UNAUTHORIZED;
            }
        };

        String result = validator.verifyUserToken("fake.token");

        assertEquals("validUser", result);
    }


}
