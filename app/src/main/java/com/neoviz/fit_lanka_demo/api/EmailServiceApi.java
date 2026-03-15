package com.neoviz.fit_lanka_demo.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface EmailServiceApi {
    @POST("smtp/email")
    Call<Void> sendEmail(
            @Header("api-key") String apiKey,
            @Body Object emailData
    );
}
