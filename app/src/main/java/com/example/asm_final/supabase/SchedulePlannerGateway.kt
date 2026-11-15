package com.example.asm_final.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.chat.ScheduleProposal
import com.example.asm_final.chat.TodoProposal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** The planner talks to the authenticated cloud function directly; it never creates chat turns. */
class SchedulePlannerGateway(context: Context) {
    private val app = context.applicationContext

    suspend fun scanTimetable(owner: Int, image: Uri): String = withContext(Dispatchers.IO) {
        val bytes = imageJpeg(image)
        val files = JSONArray().put(JSONObject().put("mime", "image/jpeg")
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        val text = call(owner, "timetable_scan", "", files)
        if (ScheduleProposal.parse(text) == null) throw BackendClient.Failure("INVALID_AI_OUTPUT", 502)
        text
    }

    suspend fun planTodos(owner: Int, context: String, goal: String): String = withContext(Dispatchers.IO) {
        require(goal.isNotBlank() && goal.length <= 1000)
        val prompt = "Mục tiêu và việc người học muốn làm: ${goal.trim()}\n" +
            "Dữ liệu lịch hiện có cho ngày đã chọn (chỉ là bối cảnh): ${context.take(1600)}"
        val text = call(owner, "todo_plan", prompt, JSONArray())
        if (TodoProposal.parse(text) == null) throw BackendClient.Failure("INVALID_AI_OUTPUT", 502)
        text
    }

    private fun call(owner: Int, mode: String, text: String, files: JSONArray): String {
        val turns = JSONArray().put(JSONObject().put("role", "user")
            .put("text", text).put("files", files))
        val response = JSONObject(SupabaseAccounts(app).requestFor(owner, "POST",
            "/functions/v1/mentor-ai", JSONObject().put("mode", mode).put("turns", turns)))
        return response.getString("text")
    }

    private fun imageJpeg(uri: Uri): ByteArray {
        val resolver = app.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) > 20L * 1024 * 1024)
                throw BackendClient.Failure("IMAGE_TOO_LARGE", 413)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw BackendClient.Failure("INVALID_IMAGE", 400)
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 60_000_000)
            throw BackendClient.Failure("INVALID_IMAGE", 400)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw BackendClient.Failure("INVALID_IMAGE", 400)
        var rotated: Bitmap = decoded
        try {
            val orientation = runCatching {
                resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            }.getOrNull()
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) rotated = Bitmap.createBitmap(decoded, 0, 0,
                decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
            for (quality in intArrayOf(85, 70, 55)) {
                val output = ByteArrayOutputStream()
                if (rotated.compress(Bitmap.CompressFormat.JPEG, quality, output) && output.size() <= 4 * 1024 * 1024)
                    return output.toByteArray()
            }
            throw BackendClient.Failure("IMAGE_TOO_LARGE", 413)
        } finally {
            if (rotated !== decoded) rotated.recycle()
            decoded.recycle()
        }
    }
}
