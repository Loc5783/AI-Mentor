import io
import uuid
import pytest
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app

@pytest.fixture
def api(tmp_path):
    app=create_app(Settings(tmp_path))
    with TestClient(app) as client:
        yield client,app

def signup(client,name):
    response=client.post("/auth/sign-up",json={"displayName":name,"email":name.lower()+"@school.example","password":"Mentor01!2026","acceptedTermsVersion":"2026-10"})
    assert response.status_code==201,response.text
    return response.json()

def headers(session,**extra):
    return {"Authorization":"Bearer "+session["accessToken"],**extra}

def key():
    return str(uuid.uuid4())

def test_signup_login_refresh_revoke_and_no_password_leak(api):
    client,app=api
    session=signup(client,"StudentA")
    own=client.get("/me/account",headers=headers(session))
    assert own.status_code==200
    assert "password" not in own.text
    assert own.json()["email"]=="studenta@school.example"
    refreshed=client.post("/auth/refresh",json={"refreshToken":session["refreshToken"]})
    assert refreshed.status_code==200
    assert client.get("/me/account",headers=headers(session)).status_code==401
    assert client.post("/auth/refresh",json={"refreshToken":session["refreshToken"]}).status_code==401
    new=refreshed.json()
    assert client.post("/auth/sign-out",headers=headers(new),json={"refreshToken":new["refreshToken"]}).status_code==200
    assert client.get("/me/account",headers=headers(new)).status_code==401

def test_public_profile_hides_private_fields_and_revision_prevents_overwrite(api):
    client,_=api
    first,second=signup(client,"First"),signup(client,"Second")
    response=client.get("/profiles/"+first["userId"],headers=headers(second))
    assert response.status_code==200
    assert not {"email","username","password_hash","grade"} & response.json().keys()
    revision=str(response.json()["revision"])
    changed=client.patch("/me/public-profile",headers=headers(first,**{"If-Match":revision}),json={"nickname":"Biệt danh mới","bio":"Mình thích Sinh học"})
    assert changed.status_code==200
    assert changed.json()["bio"]=="Mình thích Sinh học"
    assert client.patch("/me/public-profile",headers=headers(first,**{"If-Match":revision}),json={"bio":"Ghi đè"}).status_code==412

def test_project_ownership_idempotency_and_folder_deletion_keeps_chat(api):
    client,_=api
    first,second=signup(client,"First"),signup(client,"Second")
    request_key=key()
    project=client.post("/projects",headers=headers(first,**{"Idempotency-Key":request_key}),json={"title":"Sinh học"}).json()
    repeated=client.post("/projects",headers=headers(first,**{"Idempotency-Key":request_key}),json={"title":"Sinh học"})
    assert repeated.json()["id"]==project["id"]
    assert client.post("/projects",headers=headers(first,**{"Idempotency-Key":request_key}),json={"title":"Đổi body"}).status_code==409
    foreign=client.post("/conversations",headers=headers(second,**{"Idempotency-Key":key()}),json={"title":"Không được","grade":11,"projectId":project["id"]})
    assert foreign.status_code==404
    chat=client.post("/conversations",headers=headers(first,**{"Idempotency-Key":key()}),json={"title":"Chat học","grade":11,"projectId":project["id"]}).json()
    assert client.delete("/projects/"+project["id"],params={"deleteConversations":"false"},headers=headers(first,**{"If-Match":str(project["revision"])})).status_code==204
    assert client.get("/conversations/"+chat["id"],headers=headers(first)).json()["projectId"] is None
    assert client.get("/conversations/"+chat["id"],headers=headers(second)).status_code==404

def test_message_retry_is_idempotent_even_with_original_revision(api):
    client,_=api
    session=signup(client,"Student")
    chat=client.post("/conversations",headers=headers(session,**{"Idempotency-Key":key()}),json={"title":"Vật lý","grade":10}).json()
    body={"clientMessageId":key(),"role":"USER","text":"Gia tốc là gì?","attachmentIds":[]}
    request_key=key()
    h=headers(session,**{"Idempotency-Key":request_key,"If-Match":"1"})
    first=client.post("/conversations/"+chat["id"]+"/messages",headers=h,json=body)
    assert first.status_code==201,first.text
    repeated=client.post("/conversations/"+chat["id"]+"/messages",headers=h,json=body)
    assert repeated.status_code==201,repeated.text
    assert first.json()["id"]==repeated.json()["id"]
    assert len(client.get("/conversations/"+chat["id"]+"/messages",headers=headers(session)).json()["items"])==1
    assert client.post("/conversations/"+chat["id"]+"/messages",headers=headers(session,**{"Idempotency-Key":key(),"If-Match":"1"}),json={**body,"clientMessageId":key()}).status_code==412

def test_upload_checks_content_and_owner(api):
    client,_=api
    first,second=signup(client,"First"),signup(client,"Second")
    uploaded=client.post("/attachments",headers=headers(first,**{"Idempotency-Key":key()}),data={"purpose":"CHAT"},files={"file":("hoc.txt","Nội dung bài tập".encode(),"text/plain")})
    assert uploaded.status_code==201,uploaded.text
    item=uploaded.json()["id"]
    assert client.get("/attachments/"+item+"/content",headers=headers(first)).content=="Nội dung bài tập".encode()
    assert client.get("/attachments/"+item+"/content",headers=headers(second)).status_code==404
    corrupt=client.post("/attachments",headers=headers(first,**{"Idempotency-Key":key()}),data={"purpose":"AVATAR"},files={"file":("bad.jpg",b"\xff\xd8\xffnot-an-image","image/jpeg")})
    assert corrupt.status_code==415
    assert client.post("/attachments",headers=headers(first,**{"Idempotency-Key":key()}),data={"purpose":"AVATAR"},files={"file":("x.txt",b"hello","text/plain")}).status_code==400

def test_friendship_requires_recipient_acceptance_and_filters_friend_ranking(api):
    client,_=api
    first,second=signup(client,"First"),signup(client,"Second")
    friendship=client.post("/friendships",headers=headers(first,**{"Idempotency-Key":key()}),json={"recipientProfileId":second["userId"]})
    assert friendship.status_code==201,friendship.text
    item=friendship.json()["id"]
    route="/friendships/"+item
    assert client.patch(route,headers=headers(first,**{"If-Match":"1"}),json={"decision":"ACCEPT"}).status_code==404
    before=client.get("/rankings",headers=headers(first),params={"metric":"DAILY_XP","scope":"FRIENDS"}).json()
    assert len(before["entries"])==1
    accepted=client.patch(route,headers=headers(second,**{"If-Match":"1"}),json={"decision":"ACCEPT"})
    assert accepted.status_code==200,accepted.text
    after=client.get("/rankings",headers=headers(first),params={"metric":"STREAK","scope":"FRIENDS"}).json()
    assert len(after["entries"])==2
    assert after["timezone"]=="Asia/Ho_Chi_Minh"
    assert all(row["value"]==0 for row in after["entries"])

def test_event_ownership_never_broadcasts_private_chat_content(api):
    client,app=api
    first,second=signup(client,"First"),signup(client,"Second")
    project=client.post("/projects",headers=headers(first,**{"Idempotency-Key":key()}),json={"title":"Riêng tư"}).json()
    with app.state.db.read() as db:
        private=db.execute("SELECT * FROM events WHERE resource_id=?",(project["id"],)).fetchall()
        assert private and all(row["owner"]==int(first["userId"]) for row in private)
        other_visible=db.execute("SELECT resource_id FROM events WHERE owner=? OR owner IS NULL",(int(second["userId"]),)).fetchall()
        assert project["id"] not in [r[0] for r in other_visible]
        assert set(private[0].keys())=={"seq","owner","resource_type","resource_id","revision","operation","created_at"}

def test_health_does_not_claim_unimplemented_learning_or_six_other_subjects(api):
    client,_=api
    result=client.get("/health").json()
    assert result["learningEngineReady"] is True
    assert len(result["publishedCourses"]) == 3
    assert set(result["activeLessonSubjects"])=={"biology","chemistry","physics"}
    assert len(result["frameworkOnlySubjects"])==6

