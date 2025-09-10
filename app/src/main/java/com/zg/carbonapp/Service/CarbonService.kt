package com.zg.carbonapp.Service
import com.zg.carbonapp.Dao.Comment
import com.zg.carbonapp.Dao.Dynamic
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.Entity.CarbonFootprint
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface CarbonService {
    // ---------------------- 仅修改点赞/收藏返回类型 ----------------------
    // 1. 碳足迹查询相关
    @GET("api/carbon/query")
    suspend fun queryCarbonByBarcode(@Query("barcode") barcode: String): Response<CarbonFootprint>

    @GET("api/carbon/search")
    suspend fun searchCarbonByName(@Query("name") name: String): Response<CarbonFootprint>

    // 2. 用户注册登录相关
    @POST("regP")
    suspend fun register(
        @Query("userName") userName: String,
        @Query("email") email: String,
        @Query("userPassword") userPassword: String
    ): Response<ApiResponse<String>> // 返回空字符串

    @POST("loginByPaP")
    suspend fun login(@Body loginRequest: LoginRequest): Response<ApiResponse<String>> // 返回token字符串

    // 3. 动态相关
    @Multipart
    @POST("dynamic/publish")
    suspend fun publishDynamic(
        @Header("Authorization") token: String,
        @Part("content") content: String,
        @Part files: List<MultipartBody.Part>
    ): Response<ApiResponse<String>> // 返回"动态发布成功,动态id为XX"

    @GET("dynamic/Browse/{id}")
    suspend fun getDynamicDetail(@Path("id") id: String): Response<ApiResponse<DynamicDetailResponse>>

    @GET("dynamic/dynamicinfo/{id}")
    suspend fun getDynamicInfo(@Path("id") id: String): Response<ApiResponse<List<Int>>> // [点赞数, 收藏数, 评论数]

    // 4. 评论相关
    @POST("comment/publish/{feedId}")
    suspend fun publishComment(
        @Header("Authorization") token: String,
        @Path("feedId") feedId: String,
        @Query("content") content: String
    ): Response<ApiResponse<Boolean>> // 返回"评论成功"

    @GET("dynamic/commentList")
    suspend fun getCommentsByFeedId(@Query("feedId") feedId: String): Response<ApiResponse<List<Comment>>>

    @GET("users/commentList")
    suspend fun getCommentListByUser(@Header("Authorization") token: String): Response<ApiResponse<List<Comment>>>

    // ---------------------- 点赞接口返回布尔状态 ----------------------
    @POST("users/likes")
    suspend fun likeDynamic(
        @Header("Authorization") token: String,
        @Query("dynamicId") dynamicId: String
    ): Response<ApiResponse<Boolean>> // 返回isLike（当前点赞状态，true=已赞，false=未赞）

    @GET("users/likeList")
    suspend fun getLikeList(@Header("Authorization") token: String): Response<ApiResponse<List<Dynamic>>>

    // ---------------------- 收藏接口返回布尔状态 ----------------------
    @POST("users/collect")
    suspend fun collectDynamic(
        @Header("Authorization") token: String,
        @Query("feedId") feedId: String
    ): Response<ApiResponse<Boolean>> // 返回isCollect（当前收藏状态，true=已收藏，false=未收藏）

    @GET("users/collectList")
    suspend fun getCollectList(@Header("Authorization") token: String): Response<ApiResponse<List<Dynamic>>>

    @GET("dynamic/all")
    suspend fun getAllDynamics(): Response<ApiResponse<List<Dynamic>>> // 文档14修正：返回Dynamic列表

    // 5. 头像上传
    @Multipart
    @POST("users/updateAvatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part // 对应@RequestParam MultipartFile avatar
    ): Response<ApiResponse<String>> // 返回"头像上传成功"+avatarUrl

    // ---------------------- 查询个人详细信息接口 ----------------------
    @GET("users/userInfo")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Response<ApiResponse<User>> // 返回User对象（含email、carbonScore、userQQ等完整信息）

    // ---------------------- 访问用户主界面接口 ----------------------
    @GET("uses/{id}") // 按文档17：URL为/uses/{id}（注意非/users）
    suspend fun getUserProfile(
        @Path("id") userId: String // 用户ID（路径参数）
    ): Response<ApiResponse<User>> // 返回User对象（含carbonScore、userAvatar等信息）
}

// ---------------------- 更新数据类以匹配接口返回字段 ----------------------
// 1. 登录请求模型（无修改）
data class LoginRequest(val userTelephone: String, val userPassword: String)

// 2. 统一响应模型（无修改）
data class ApiResponse<T>(val code: Int, val message: String, val data: T)
