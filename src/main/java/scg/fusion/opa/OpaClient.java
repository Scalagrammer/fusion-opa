package scg.fusion.opa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Data;
import okhttp3.*;
import scg.fusion.Environment;

import java.io.IOException;

import static java.lang.String.format;
import static okhttp3.RequestBody.create;
import static scg.fusion.opa.Utils.getPolicyPackage;

public final class OpaClient {

    public static final String OPA_SERVER_DATA_URL_PROPERTY_NAME = "fusion.opa.server.data.url";

    private static final MediaType json = MediaType.get("application/json; charset=utf-8");

    private final String serverUrl;

    private final OkHttpClient client = new OkHttpClient();

    private final ObjectMapper mapper = new ObjectMapper();

    public OpaClient(Environment environment) {
        this.serverUrl = environment.getProperty(OPA_SERVER_DATA_URL_PROPERTY_NAME);
    }

    public <R, T> R evalPolicy(T input, Class<R> resultType) throws IOException {

        String policyPackage = getPolicyPackage(input.getClass());

        try (Response response = client.newCall(preparePolicyRequest(policyPackage, input)).execute()) {
            if (200 == response.code()) {
                return extractPolicyResponse(resultType, response.body()).getResult();
            }
        }

        throw new RuntimeException(format("Policy [%s] eval failed", policyPackage));

    }

    private <T> PolicyResponse<T> extractPolicyResponse(Class<T> resultType, ResponseBody responseBody) throws IOException {
        return mapper.readValue(responseBody.bytes(), getResponseJavaType(resultType));
    }

    private <T> Request preparePolicyRequest(String policyPackage, T input) throws JsonProcessingException {

        PolicyRequest<T> policyRequest = new PolicyRequest<>();

        policyRequest.input = input;

        return new Request.Builder()
                .url(format("%s/%s", serverUrl, policyPackage.replace(".", "/")))
                .post(create(mapper.writeValueAsBytes(policyRequest), json))
                .build();
    }

    private static <T> JavaType getResponseJavaType(Class<T> resultType) {
        return TypeFactory.defaultInstance()
                .constructParametricType(PolicyResponse.class, resultType);
    }

    @Data
    private static class PolicyRequest<T> {
        private T input;
    }

    @Data
    private static class PolicyResponse<T> {
        private T result;
    }

}
