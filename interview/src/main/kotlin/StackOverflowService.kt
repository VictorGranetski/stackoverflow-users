import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StackOverflowService {

    @GET("users?order=desc&sort=reputation")
    fun listUsers(
            @Query("page") page: Int = 1,
            @Query("pagesize") pageSize: Int = 100,
            @Query("min") reputation: Int = 223,
            @Query("filter") filter: String = "!LnNkvq16GJbfkXKi.EM)4I"
    ): Call<Users>

    @GET("users/{ids}/tags?order=desc&sort=popular")
    fun listUserTags(
            @Path("ids") ids: Int,
            @Query("page") page: Int = 1,
            @Query("pagesize") pageSize: Int = 100,
            @Query("filter") filter: String = "!-.G.68h(ttpy"
    ): Call<Tags>
}