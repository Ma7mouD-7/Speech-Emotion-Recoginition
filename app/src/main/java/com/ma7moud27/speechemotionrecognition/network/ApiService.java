package com.ma7moud27.speechemotionrecognition.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface ApiService {
    @Multipart
    @POST("/")
   Call<Response> uploadAudio(@Part MultipartBody.Part Audio);
}
