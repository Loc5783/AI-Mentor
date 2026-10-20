import asyncio
import base64
import json
from pathlib import Path
import time
import uuid
from fastapi import FastAPI, Depends, Header, HTTPException, Request, UploadFile, File, Form, Query
from fastapi.responses import JSONResponse, StreamingResponse, FileResponse
from .config import Settings
from .database import Database, identifier, dump
from .auth import AuthService
from .resources import Resources
from .learning import Catalog, LearningService
from .learning_routes import learning_router
from .overview import LearningOverview
from .learning_stats import reward_summary, learning_day
from . import models
from .common import error, iso, check_revision, owned_resource, resource, ACTIVE_SUBJECTS, ALL_SUBJECTS


def create_app(settings=None):
    settings=settings or Settings.from_env()
    database=Database(settings.data_dir)
    database.initialize()
    auth=AuthService(database,settings)
    auth.seed_demo()
    resources=Resources(database)
    catalog=Catalog(settings.curriculum_dir or Path(__file__).resolve().parents[1]/"curriculum")
    learning=LearningService(database,catalog)
    overview=LearningOverview(database,catalog,lambda:learning.clock())
    app=FastAPI(title="AI Mentor API",version="0.2.0",
        description="Working account/chat/profile/event backend. Learning engine integration is in progress; six other subject modules remain framework-only.")
    app.state.db=database
    app.state.auth=auth
    app.state.learning=learning

    @app.exception_handler(HTTPException)
    async def api_error(request,exc):
        detail=exc.detail if isinstance(exc.detail,dict) else {"code":str(exc.detail)}
        return JSONResponse(status_code=exc.status_code,content={"code":detail.get("code","REQUEST_FAILED"),"requestId":str(uuid.uuid4())})

    @app.middleware("http")
    async def limits(request,call_next):
        length=request.headers.get("content-length")
        if length:
            try:too_large=int(length)>8*1024*1024
            except ValueError:too_large=True
            if too_large:return JSONResponse(status_code=413,content={"code":"BODY_TOO_LARGE"})
        response=await call_next(request)
        response.headers["X-Content-Type-Options"]="nosniff"
        response.headers["Cache-Control"]="no-store"
        return response

    def current(authorization: str|None=Header(default=None)):
        if not authorization or not authorization.startswith("Bearer "):
            error("AUTHENTICATION_REQUIRED",401)
        return auth.current(authorization[7:])

    app.include_router(learning_router(learning,current))

    @app.get("/me/dashboard",tags=["Learning"])
    def dashboard(grade:int=Query(...,ge=10,le=12),user=Depends(current)):
        return overview.load(user["id"],grade)

    def public(owner,at=None,connection=None):
        now=learning.clock() if at is None else at
        def read(db):
            row=db.execute("SELECT id,nickname,bio,avatar_id,revision,updated_at FROM users WHERE id=?",(owner,)).fetchone()
            if not row:error("PROFILE_NOT_FOUND",404)
            rewards=reward_summary(db.execute("SELECT xp,day FROM awards WHERE user_id=?",(owner,)).fetchall(),
                {r[0] for r in db.execute("SELECT day FROM learning_days WHERE user_id=?",(owner,))},now)
            return {"id":str(row["id"]),"nickname":row["nickname"],"bio":row["bio"],
                "avatarUrl":f"/profiles/{owner}/avatar" if row["avatar_id"] else None,
                "streakDays":rewards["streakDays"],"dailyXp":rewards["dailyXp"],"learningDay":rewards["learningDay"],"timezone":settings.learning_timezone,
                "revision":row["revision"],"updatedAt":iso(row["updated_at"])}
        if connection is not None:return read(connection)
        with database.read() as db:
            db.execute("BEGIN")
            return read(db)

    def page(value,limit,cursor):
        try:offset=int(base64.urlsafe_b64decode(cursor.encode()).decode()) if cursor else 0
        except (ValueError,UnicodeError):error("INVALID_CURSOR")
        if offset<0:error("INVALID_CURSOR")
        items=value["items"]
        return {**value,"items":items[offset:offset+limit],
            "nextCursor":base64.urlsafe_b64encode(str(offset+limit).encode()).decode() if offset+limit<len(items) else None,
            "asOf":iso(time.time())}

    @app.get("/health")
    def health():
        return {"status":"ok","version":"0.2.0","activeLessonSubjects":list(ACTIVE_SUBJECTS),
            "frameworkOnlySubjects":[x for x in ALL_SUBJECTS if x not in ACTIVE_SUBJECTS],
            "features":["auth","profile","chat_projects","chat_messages","attachments","friendships","rankings","events","learning_engine"],
            "learningEngineReady":True,
            "publishedCourses":[{"subjectId":s,"grade":g,"version":c["version"]} for (s,g),c in sorted(catalog.courses.items())],
            "realtimeTransport":"SSE"}

    @app.post("/auth/sign-up",status_code=201)
    def signup(body: models.SignUp,request: Request):
        auth.limit("signup:"+str(request.client.host),5,60)
        return auth.signup(body)

    @app.post("/auth/sign-in")
    def signin(body: models.SignIn,request: Request):
        auth.limit("login:"+str(request.client.host),20,60)
        return auth.signin(body)

    @app.post("/auth/refresh")
    def refresh(body: models.Refresh):
        return auth.refresh(body.refreshToken)

    @app.post("/auth/sign-out")
    def signout(body: models.Refresh,user=Depends(current)):
        return auth.signout(user["id"],body.refreshToken)

    @app.post("/auth/change-password")
    def password(body: models.ChangePassword,user=Depends(current)):
        return auth.change_password(user["id"],body)

    @app.get("/me/account")
    def own_account(user=Depends(current)):
        return {"id":str(user["id"]),"username":user["username"],"email":user["email"],"emailVerified":bool(user["verified"]),"profile":public(user["id"])}

    @app.get("/me/public-profile")
    def own_public(user=Depends(current)):
        return public(user["id"])

    @app.get("/profiles/{profile_id}")
    def other_public(profile_id:int,user=Depends(current)):
        return public(profile_id)

    @app.patch("/me/public-profile")
    def update_public(body:models.ProfilePatch,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        updates=body.model_dump(exclude_unset=True)
        if not updates:error("EMPTY_UPDATE")
        with database.transaction() as db:
            row=db.execute("SELECT * FROM users WHERE id=?",(user["id"],)).fetchone()
            check_revision(row,revision)
            nickname=updates.get("nickname",row["nickname"])
            bio=updates.get("bio",row["bio"])
            avatar=updates.get("avatarAttachmentId",row["avatar_id"])
            if nickname is None or bio is None or not nickname.strip():error("INVALID_PROFILE")
            if avatar:
                attachment=owned_resource(db,user["id"],"ATTACHMENT",avatar)
                if json.loads(attachment["body"]).get("purpose")!="AVATAR":error("INVALID_AVATAR")
            db.execute("UPDATE users SET nickname=?,bio=?,avatar_id=?,revision=revision+1,updated_at=? WHERE id=?",
                       (nickname.strip(),bio,avatar,time.time(),user["id"]))
            Database.event(db,None,"PROFILE",user["id"],row["revision"]+1)
        return public(user["id"])

    @app.get("/me/learning-profile")
    def learning_profile(user=Depends(current)):
        with database.read() as db:
            selected=[r[0] for r in db.execute("SELECT subject_id FROM enrollments WHERE user_id=? AND grade=?",(user["id"],user["grade"]))]
        return {"id":str(user["id"]),"grade":user["grade"],"displayName":user["nickname"],"subjectIds":selected,
            "timezone":settings.learning_timezone,"revision":user["revision"],"updatedAt":iso(user["updated_at"])}

    @app.patch("/me/learning-profile")
    def update_learning(body:models.LearningPatch,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        values=body.model_dump(exclude_unset=True)
        if not values or any(v is None for v in values.values()):error("EMPTY_UPDATE")
        with database.transaction() as db:
            row=db.execute("SELECT * FROM users WHERE id=?",(user["id"],)).fetchone();check_revision(row,revision)
            grade=values.get("grade",row["grade"]);name=values.get("displayName",row["nickname"]).strip()
            if len(name)<2:error("INVALID_NAME")
            db.execute("UPDATE users SET grade=?,nickname=?,revision=revision+1,updated_at=? WHERE id=?",(grade,name,time.time(),user["id"]))
            Database.event(db,user["id"],"PROFILE",user["id"],row["revision"]+1)
        with database.read() as db:updated=dict(db.execute("SELECT * FROM users WHERE id=?",(user["id"],)).fetchone())
        return learning_profile(updated)

    @app.get("/projects")
    def projects(user=Depends(current),limit:int=Query(25,ge=1,le=100),cursor:str|None=None):
        return page(resources.list(user["id"],"PROJECT"),limit,cursor)

    @app.post("/projects",status_code=201)
    def project_create(body:models.Named,user=Depends(current),key:str|None=Header(None,alias="Idempotency-Key")):
        return resources.create(user["id"],"PROJECT",body.model_dump(),key)

    @app.patch("/projects/{item}")
    def project_patch(item:str,body:models.Named,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        return resources.patch(user["id"],"PROJECT",item,body.model_dump(),revision)

    @app.delete("/projects/{item}",status_code=204)
    def project_delete(item:str,deleteConversations:bool,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        resources.delete(user["id"],"PROJECT",item,revision,deleteConversations)

    @app.get("/conversations")
    def conversations(user=Depends(current),limit:int=Query(25,ge=1,le=100),cursor:str|None=None,projectId:str|None=None,query:str=""):
        return page(resources.list(user["id"],"CONVERSATION",query,projectId),limit,cursor)

    @app.post("/conversations",status_code=201)
    def conversation_create(body:models.ConversationCreate,user=Depends(current),key:str|None=Header(None,alias="Idempotency-Key")):
        values=body.model_dump()
        if body.handoff is not None:
            if set(body.handoff)!={"id"} or not isinstance(body.handoff["id"],str):error("INVALID_HANDOFF")
            saved=learning.resume(user["id"],body.handoff["id"])
            if saved["handoff"]["grade"]!=body.grade:error("HANDOFF_GRADE_CONFLICT",409)
            values["handoff"]=saved["handoff"]
        return resources.create(user["id"],"CONVERSATION",{**values,"lastMessageAt":None},key)

    @app.get("/conversations/{item}")
    def conversation_get(item:str,user=Depends(current)):
        return resources.get(user["id"],"CONVERSATION",item)

    @app.patch("/conversations/{item}")
    def conversation_patch(item:str,body:models.ConversationPatch,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        values=body.model_dump(exclude_unset=True)
        if not values or values.get("title","ok") is None:error("EMPTY_UPDATE")
        return resources.patch(user["id"],"CONVERSATION",item,values,revision)

    @app.delete("/conversations/{item}",status_code=204)
    def conversation_delete(item:str,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        resources.delete(user["id"],"CONVERSATION",item,revision)

    @app.get("/conversations/{item}/messages")
    def messages(item:str,user=Depends(current),limit:int=Query(25,ge=1,le=100),cursor:str|None=None):
        resources.get(user["id"],"CONVERSATION",item)
        values=resources.list(user["id"],"MESSAGE")
        values["items"]=[m for m in reversed(values["items"]) if m["conversationId"]==item]
        return page(values,limit,cursor)

    @app.post("/conversations/{item}/messages",status_code=201)
    def message_create(item:str,body:models.MessageCreate,user=Depends(current),key:str|None=Header(None,alias="Idempotency-Key"),revision:str|None=Header(None,alias="If-Match")):
        return resources.append_message(user["id"],item,body.model_dump(),key,revision)

    def stored_file(owner,item):
        with database.read() as db:
            row=owned_resource(db,owner,"ATTACHMENT",item);body=json.loads(row["body"])
        path=settings.data_dir/"uploads"/str(owner)/(item+".bin")
        if not path.is_file():error("FILE_NOT_FOUND",404)
        return path,body

    @app.post("/attachments",status_code=201)
    async def upload(file:UploadFile=File(...),purpose:str=Form(...),user=Depends(current),key:str|None=Header(None,alias="Idempotency-Key")):
        if purpose not in ("CHAT","AVATAR"):error("INVALID_ATTACHMENT_PURPOSE")
        content=await file.read(4*1024*1024+1)
        if not content:error("EMPTY_ATTACHMENT")
        if len(content)>4*1024*1024:error("FILE_TOO_LARGE",413)
        if content.startswith(b"\xff\xd8\xff"):mime="image/jpeg"
        elif content.startswith(b"\x89PNG\r\n\x1a\n"):mime="image/png"
        elif content[:4]==b"RIFF" and content[8:12]==b"WEBP":mime="image/webp"
        elif content.startswith(b"%PDF-"):mime="application/pdf"
        else:
            try:
                text=content.decode("utf-8")
                if any(ord(c)<32 and c not in "\n\r\t" for c in text):raise ValueError()
                mime="text/plain"
            except (UnicodeError,ValueError):error("UNSUPPORTED_FILE_TYPE",415)
        if mime.startswith("image/"):
            from PIL import Image
            import io
            try:
                with Image.open(io.BytesIO(content)) as decoded:
                    if decoded.width*decoded.height>60000000:raise ValueError("Image dimensions exceed limit")
                    decoded.verify()
            except Exception:error("CORRUPT_IMAGE",415)
        if purpose=="AVATAR" and not mime.startswith("image/"):error("INVALID_AVATAR")
        name=Path(file.filename or "attachment").name
        name="".join(c for c in name if ord(c)>=32)[:120] or "attachment"
        # Payload digest is part of idempotency; a reused key with different bytes must conflict.
        import hashlib
        result=resources.create(user["id"],"ATTACHMENT",{"name":name,"mimeType":mime,"byteSize":len(content),"purpose":purpose,"sha256":hashlib.sha256(content).hexdigest()},key)
        path=settings.data_dir/"uploads"/str(user["id"])/(result["id"]+".bin")
        path.parent.mkdir(parents=True,exist_ok=True)
        temp=path.with_suffix(".tmp")
        temp.write_bytes(content);temp.replace(path)
        result.pop("sha256",None)
        return result

    @app.get("/attachments/{item}")
    def attachment_get(item:str,user=Depends(current)):
        value=resources.get(user["id"],"ATTACHMENT",item);value.pop("sha256",None)
        value["downloadUrl"]=f"/attachments/{item}/content"
        return value

    @app.get("/attachments/{item}/content")
    def attachment_content(item:str,user=Depends(current)):
        path,body=stored_file(user["id"],item)
        return FileResponse(path,media_type=body["mimeType"],filename=body["name"])

    @app.get("/profiles/{profile_id}/avatar")
    def avatar(profile_id:int,user=Depends(current)):
        with database.read() as db:
            profile=db.execute("SELECT avatar_id FROM users WHERE id=?",(profile_id,)).fetchone()
        if not profile or not profile["avatar_id"]:error("AVATAR_NOT_FOUND",404)
        path,body=stored_file(profile_id,profile["avatar_id"])
        return FileResponse(path,media_type=body["mimeType"],filename="avatar")

    @app.get("/rankings")
    def rankings(metric:str,scope:str,user=Depends(current)):
        if metric not in ("STREAK","DAILY_XP") or scope not in ("FRIENDS","GLOBAL"):error("INVALID_RANKING")
        now=learning.clock()
        with database.read() as db:
            db.execute("BEGIN")
            ids=[r[0] for r in db.execute("SELECT id FROM users")]
            friends={user["id"]}
            if scope=="FRIENDS":
                for row in db.execute("SELECT requester,recipient FROM friendships WHERE state='ACCEPTED' AND (requester=? OR recipient=?)",(user["id"],user["id"])):
                    friends.update(row)
                ids=[x for x in ids if x in friends]
            profiles=[public(x,now,db) for x in ids]
        field="streakDays" if metric=="STREAK" else "dailyXp"
        profiles.sort(key=lambda p:(-p[field],int(p["id"])))
        entries=[];rank=0;last=None
        for index,profile in enumerate(profiles):
            if profile[field]!=last:rank=index+1;last=profile[field]
            entries.append({"rank":rank,"profile":profile,"value":profile[field]})
        return {"metric":metric,"scope":scope,"learningDay":learning_day(now).isoformat(),"timezone":settings.learning_timezone,"asOf":iso(now),"entries":entries[:100],"nextCursor":None}

    def friendship(row):
        return {"id":row["id"],"requesterProfileId":str(row["requester"]),"recipientProfileId":str(row["recipient"]),"state":row["state"],"revision":row["revision"],"updatedAt":iso(row["updated_at"])}

    @app.get("/friendships")
    def friends(user=Depends(current)):
        with database.read() as db:
            rows=db.execute("SELECT * FROM friendships WHERE requester=? OR recipient=? ORDER BY updated_at DESC",(user["id"],user["id"])).fetchall()
        return {"items":[friendship(row) for row in rows],"nextCursor":None,"asOf":iso(time.time())}

    @app.post("/friendships",status_code=201)
    def friend_create(body:models.FriendCreate,user=Depends(current),key:str|None=Header(None,alias="Idempotency-Key")):
        try:recipient=int(body.recipientProfileId)
        except ValueError:error("INVALID_PROFILE")
        if recipient==user["id"]:error("CANNOT_FRIEND_SELF")
        with database.transaction() as db:
            from .common import idem
            def action():
                if not db.execute("SELECT 1 FROM users WHERE id=?",(recipient,)).fetchone():error("PROFILE_NOT_FOUND",404)
                if db.execute("SELECT 1 FROM friendships WHERE (requester=? AND recipient=?) OR (requester=? AND recipient=?)",(user["id"],recipient,recipient,user["id"])).fetchone():error("FRIENDSHIP_EXISTS",409)
                item=identifier();db.execute("INSERT INTO friendships(id,requester,recipient,state,updated_at) VALUES(?,?,?,'PENDING',?)",(item,user["id"],recipient,time.time()))
                for owner in (user["id"],recipient):Database.event(db,owner,"FRIENDSHIP",item)
                return friendship(db.execute("SELECT * FROM friendships WHERE id=?",(item,)).fetchone())
            return idem(db,user["id"],"friendship",key,body.model_dump(),action)

    @app.patch("/friendships/{item}")
    def friend_patch(item:str,body:models.FriendPatch,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        with database.transaction() as db:
            row=db.execute("SELECT * FROM friendships WHERE id=? AND recipient=?",(item,user["id"])).fetchone()
            if not row:error("FRIENDSHIP_NOT_FOUND",404)
            check_revision(row,revision)
            if row["state"]!="PENDING":error("FRIENDSHIP_ALREADY_DECIDED",409)
            state="ACCEPTED" if body.decision=="ACCEPT" else "DECLINED"
            db.execute("UPDATE friendships SET state=?,revision=revision+1,updated_at=? WHERE id=?",(state,time.time(),item))
            for owner in (row["requester"],row["recipient"]):Database.event(db,owner,"FRIENDSHIP",item,row["revision"]+1)
            return friendship(db.execute("SELECT * FROM friendships WHERE id=?",(item,)).fetchone())

    @app.delete("/friendships/{item}",status_code=204)
    def friend_delete(item:str,user=Depends(current),revision:str|None=Header(None,alias="If-Match")):
        with database.transaction() as db:
            row=db.execute("SELECT * FROM friendships WHERE id=? AND (requester=? OR recipient=?)",(item,user["id"],user["id"])).fetchone()
            if not row:error("FRIENDSHIP_NOT_FOUND",404)
            check_revision(row,revision)
            db.execute("DELETE FROM friendships WHERE id=?",(item,))
            for owner in (row["requester"],row["recipient"]):Database.event(db,owner,"FRIENDSHIP",item,row["revision"]+1,"DELETE")

    @app.get("/sync/cursor")
    def sync_cursor(user=Depends(current)):
        with database.read() as db:seq=db.execute("SELECT COALESCE(MAX(seq),0) FROM events").fetchone()[0]
        return {"cursor":str(seq)}

    @app.get("/events")
    async def events(request:Request,user=Depends(current),last:str|None=Header(None,alias="Last-Event-ID")):
        try:cursor=int(last or "0")
        except ValueError:error("INVALID_CURSOR")
        if cursor<0:error("INVALID_CURSOR")
        bearer=request.headers.get("authorization","")[7:]
        async def stream():
            nonlocal cursor
            while not await request.is_disconnected():
                try:auth.current(bearer)
                except HTTPException:return
                with database.read() as db:
                    rows=db.execute("SELECT * FROM events WHERE seq>? AND (owner=? OR owner IS NULL) ORDER BY seq LIMIT 100",(cursor,user["id"])).fetchall()
                for row in rows:
                    cursor=row["seq"]
                    event={"eventId":str(cursor),"resourceType":row["resource_type"],"resourceId":row["resource_id"],"revision":row["revision"],"operation":row["operation"],"serverTime":iso(row["created_at"])}
                    yield f"id: {cursor}\nevent: change\ndata: {dump(event)}\n\n"
                if not rows:yield ": heartbeat\n\n"
                await asyncio.sleep(1)
        return StreamingResponse(stream(),media_type="text/event-stream",headers={"X-Accel-Buffering":"no"})
    return app

app=create_app()

