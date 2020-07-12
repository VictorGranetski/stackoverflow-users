import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit

object RetrofitFactory {
    private const val BASE_URL = "https://api.stackexchange.com/2.2/"

    fun makeRetrofitService(converterFactory: Converter.Factory): StackOverflowService {
        val httpClient = OkHttpClient.Builder().apply {
            addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalHttpUrl = originalRequest.url()
                val newUrl = originalHttpUrl.newBuilder()
                        .addQueryParameter("key", "uA5ggX4ruf*YR6hWG1Etnw((")
                        .addQueryParameter("site", "stackoverflow")
                        .build()

                val newRequest = originalRequest.newBuilder()
                        .url(newUrl)
                        .build()
                chain.proceed(newRequest)
            }
        }
        return Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl(BASE_URL)
                .addConverterFactory(converterFactory)
                .build().create(StackOverflowService::class.java)
    }
}