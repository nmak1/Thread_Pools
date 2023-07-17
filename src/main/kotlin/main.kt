import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import dto.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

private val gson = Gson()

private const val BASE_URL = "http://127.0.0.1:9990"

suspend fun <T> makeCall(url: String, typeToken: TypeToken<T>): T =
    suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(client::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        continuation.resume(gson.fromJson(response.body?.string(), typeToken.type))
                    } catch (e: JsonParseException) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }


suspend fun getPosts(): List<Post> =
    makeCall("$BASE_URL/api/slow/posts", object : TypeToken<List<Post>>() {})

suspend fun getComments(postId: Long): List<Comment> =
    makeCall("$BASE_URL/api/slow/posts/$postId/comments", object : TypeToken<List<Comment>>() {})

suspend fun getAuthors(id: Long): List<Author> =
    makeCall("$BASE_URL/api/slow/authors/$id", object : TypeToken<List<Author>>() {})

fun main() {
    runBlocking {
        val posts = getPosts()

        val result = posts.map {
            async {
                PostWithComments(it,
                    getAuthors(it.authorId),
                    getComments(it.id).map { comment ->
                        CommentsWithAuthor(comment, getAuthors(comment.authorId))
                    })
            }
        }.awaitAll()

        println(result)

    }
}