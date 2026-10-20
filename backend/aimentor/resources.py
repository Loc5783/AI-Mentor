import json
import time
from .common import error, owned_resource, resource, check_revision, normalized, idem
from .database import Database, identifier, dump

class Resources:
    def __init__(self,db):
        self.db=db

    def create(self,owner,kind,body,key,operation=None):
        with self.db.transaction() as db:
            def action():
                if kind=="CONVERSATION" and body.get("projectId"):
                    owned_resource(db,owner,"PROJECT",body["projectId"])
                item=identifier()
                db.execute("INSERT INTO resources(id,kind,user_id,body,updated_at) VALUES(?,?,?,?,?)",(item,kind,owner,dump(body),time.time()))
                Database.event(db,owner,kind,item)
                return resource(owned_resource(db,owner,kind,item))
            return idem(db,owner,operation or "create_"+kind,key,body,action)

    def get(self,owner,kind,item):
        with self.db.read() as db:return resource(owned_resource(db,owner,kind,item))

    def list(self,owner,kind,query="",project=None):
        with self.db.read() as db:
            rows=db.execute("SELECT * FROM resources WHERE user_id=? AND kind=? AND deleted_at IS NULL ORDER BY updated_at DESC,id",(owner,kind)).fetchall()
        result=[]
        for row in rows:
            value=resource(row)
            if project is not None and value.get("projectId")!=project:continue
            if query and normalized(query) not in normalized(value.get("title","")+" "+value.get("text","")):continue
            result.append(value)
        return {"items":result,"nextCursor":None,"asOf":time.time()}

    def patch(self,owner,kind,item,patch,revision):
        with self.db.transaction() as db:
            row=owned_resource(db,owner,kind,item);check_revision(row,revision)
            if kind=="CONVERSATION" and patch.get("projectId"):
                owned_resource(db,owner,"PROJECT",patch["projectId"])
            body=json.loads(row["body"]);body.update(patch)
            if "title" in body and not body["title"].strip():error("INVALID_TITLE")
            db.execute("UPDATE resources SET body=?,revision=revision+1,updated_at=? WHERE id=?",(dump(body),time.time(),item))
            Database.event(db,owner,kind,item,row["revision"]+1)
            return resource(owned_resource(db,owner,kind,item))

    def delete(self,owner,kind,item,revision,delete_chats=False):
        with self.db.transaction() as db:
            row=owned_resource(db,owner,kind,item);check_revision(row,revision)
            def remove(resource_id,resource_kind):
                current=owned_resource(db,owner,resource_kind,resource_id)
                db.execute("UPDATE resources SET deleted_at=?,revision=revision+1 WHERE id=?",(time.time(),resource_id))
                Database.event(db,owner,resource_kind,resource_id,current["revision"]+1,"DELETE")
            conversations=[]
            if kind=="PROJECT":
                for c in db.execute("SELECT * FROM resources WHERE user_id=? AND kind='CONVERSATION' AND deleted_at IS NULL",(owner,)).fetchall():
                    body=json.loads(c["body"])
                    if body.get("projectId")==item:
                        if delete_chats:conversations.append(c["id"])
                        else:
                            body["projectId"]=None
                            db.execute("UPDATE resources SET body=?,revision=revision+1,updated_at=? WHERE id=?",(dump(body),time.time(),c["id"]))
                            Database.event(db,owner,"CONVERSATION",c["id"],c["revision"]+1)
            elif kind=="CONVERSATION":conversations.append(item)
            for conversation in conversations:
                for child in db.execute("SELECT * FROM resources WHERE user_id=? AND kind='MESSAGE' AND deleted_at IS NULL",(owner,)).fetchall():
                    if json.loads(child["body"]).get("conversationId")==conversation:remove(child["id"],"MESSAGE")
                if conversation!=item:remove(conversation,"CONVERSATION")
            remove(item,kind)

    def append_message(self,owner,conversation,body,key,revision):
        with self.db.transaction() as db:
            chat=owned_resource(db,owner,"CONVERSATION",conversation)
            if not body["text"].strip() and not body["attachmentIds"]:error("EMPTY_MESSAGE")
            for attachment in body["attachmentIds"]:
                file=owned_resource(db,owner,"ATTACHMENT",attachment)
                if json.loads(file["body"]).get("purpose")!="CHAT":error("INVALID_ATTACHMENT")
            if body.get("replyToMessageId"):
                previous=owned_resource(db,owner,"MESSAGE",body["replyToMessageId"])
                old=json.loads(previous["body"])
                if old.get("conversationId")!=conversation or old.get("role")!="USER":error("INVALID_REPLY_LINK")
            if body["role"]=="ASSISTANT" and not body.get("replyToMessageId"):error("REPLY_LINK_REQUIRED")
            def action():
                check_revision(chat,revision)
                values={**body,"conversationId":conversation,"source":"USER" if body["role"]=="USER" else "DIRECT_GEMINI_CLIENT"}
                for row in db.execute("SELECT body FROM resources WHERE user_id=? AND kind='MESSAGE' AND deleted_at IS NULL",(owner,)):
                    if json.loads(row["body"]).get("clientMessageId")==body["clientMessageId"]:error("DUPLICATE_CLIENT_MESSAGE",409)
                item=identifier();now=time.time()
                db.execute("INSERT INTO resources(id,kind,user_id,body,updated_at) VALUES(?,'MESSAGE',?,?,?)",(item,owner,dump(values),now))
                chat_body=json.loads(chat["body"]);chat_body["lastMessageAt"]=now
                db.execute("UPDATE resources SET body=?,revision=revision+1,updated_at=? WHERE id=?",(dump(chat_body),now,conversation))
                Database.event(db,owner,"MESSAGE",item);Database.event(db,owner,"CONVERSATION",conversation,chat["revision"]+1)
                return resource(owned_resource(db,owner,"MESSAGE",item))
            return idem(db,owner,"message_"+conversation,key,body,action)

