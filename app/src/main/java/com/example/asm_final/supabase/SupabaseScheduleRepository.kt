package com.example.asm_final.supabase

import android.content.Context
import com.example.mentor.schedule.domain.*
import com.example.mentor.data.ActiveUser
import org.json.JSONArray
import org.json.JSONObject

/** Online schedule repository. User identity always comes from the signed session, never request JSON. */
class SupabaseScheduleRepository(private val context: Context): ScheduleRepository, ScheduleImportRepository {
    private fun call(account:String,op:String,data:JSONObject=JSONObject()):JSONObject {
        val owner=account.toInt()
        check(owner>0 && ActiveUser.get(context)==owner)
        return JSONObject(SupabaseAccounts(context).requestFor(owner,"POST","/rest/v1/rpc/mentor_schedule_action",JSONObject().put("op",op).put("data",data)))
    }
    private fun nullable(row:JSONObject,key:String)=if(row.isNull(key)) null else row.getString(key)
    private fun millis(row:JSONObject,key:String):Long? = nullable(row,key)?.let {
        // PostgreSQL timestamps include offsets. ISO_OFFSET_DATE_TIME is desugared by Android API 26+.
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",java.util.Locale.ROOT).apply { timeZone=java.util.TimeZone.getTimeZone("UTC") }.parse(it.substringBefore('+').substringBefore('Z'))?.time
    }
    private fun entryData(e:ScheduleEntry)=JSONObject().put("id",e.id).put("day",e.dayOfWeek).put("period",e.period).put("title",e.title).put("teacher",e.teacher ?: JSONObject.NULL).put("notes",e.notes ?: JSONObject.NULL)
    private fun todoData(t:TodoTask)=JSONObject().put("id",t.id).put("day",t.dayOfWeek).put("title",t.title).put("priority",t.priority).put("linked_subject",t.linkedSubject ?: JSONObject.NULL).put("completed",t.isCompleted)
    override fun getEntries(accountId:String,dayOfWeek:Int):List<ScheduleEntry> {
        val items=call(accountId,"entries",JSONObject().put("day",dayOfWeek)).getJSONArray("items")
        return (0 until items.length()).map { val r=items.getJSONObject(it); ScheduleEntry(r.getLong("client_id"),accountId,r.getString("subject"),r.getInt("day"),r.getInt("period"),nullable(r,"teacher"),nullable(r,"notes")) }
    }
    override fun insertEntry(entry:ScheduleEntry)=call(entry.accountId,"insert_entry",entryData(entry)).getLong("id")
    override fun updateEntry(entry:ScheduleEntry) {call(entry.accountId,"update_entry",entryData(entry))}
    override fun deleteEntry(id:Long,accountId:String) {call(accountId,"delete_entry",JSONObject().put("id",id))}
    override fun getTodos(accountId:String,dayOfWeek:Int):List<TodoTask> {
        val rows=call(accountId,"todos",JSONObject().put("day",dayOfWeek)).getJSONArray("items")
        return (0 until rows.length()).map { val r=rows.getJSONObject(it); TodoTask(r.getLong("id"),accountId,r.getString("title"),r.getBoolean("completed"),r.getInt("day"),r.getInt("priority"),nullable(r,"linked_subject"),millis(r,"created_at") ?: 0,millis(r,"completed_at")) }
    }
    override fun insertTodo(task:TodoTask)=call(task.accountId,"insert_todo",todoData(task)).getLong("id")
    override fun updateTodo(task:TodoTask) {call(task.accountId,"update_todo",todoData(task))}
    override fun deleteTodo(id:Long,accountId:String) {call(accountId,"delete_todo",JSONObject().put("id",id))}
    override fun updateTodoStatus(id:Long,accountId:String,isCompleted:Boolean,completedAt:Long?) {call(accountId,"todo_status",JSONObject().put("id",id).put("completed",isCompleted))}
    override fun getLogs(accountId:String,limit:Int):List<ScheduleActivityLog> {
        val rows=call(accountId,"logs",JSONObject().put("limit",limit)).getJSONArray("items")
        return (0 until rows.length()).map { val r=rows.getJSONObject(it); ScheduleActivityLog(r.getLong("id"),accountId,r.getString("action"),r.getString("entity"),r.getString("description"),millis(r,"created_at") ?: 0) }
    }
    override fun insertLog(log:ScheduleActivityLog) {call(log.accountId,"insert_log",JSONObject().put("action",log.actionType).put("entity",log.entityType).put("description",log.description))}
    override fun clearLogs(accountId:String) {call(accountId,"clear_logs")}
    override fun importVacantEntries(accountId:String,entries:List<ScheduleEntry>):Int {
        require(entries.isNotEmpty() && entries.size<=70 && entries.all { it.accountId==accountId && it.dayOfWeek in 1..7 && it.period in 1..10 && it.title.isNotBlank() && it.title.length<=120 })
        val rows=JSONArray(); entries.forEach {rows.put(entryData(it))}
        return call(accountId,"import",JSONObject().put("entries",rows)).getInt("inserted")
    }
    override fun close() { }
}
