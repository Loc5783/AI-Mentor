from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator
import re

class Input(BaseModel):
    model_config=ConfigDict(extra="forbid")

class SignIn(Input):
    identifier: str=Field(min_length=1,max_length=254)
    password: str=Field(min_length=1,max_length=128)

class SignUp(Input):
    displayName: str=Field(min_length=2,max_length=80)
    email: str=Field(min_length=3,max_length=254)
    password: str=Field(min_length=8,max_length=128)
    acceptedTermsVersion: str=Field(min_length=1,max_length=50)
    @field_validator("displayName")
    @classmethod
    def name_format(cls,value):
        value=value.strip()
        if len(value)<2:raise ValueError("Invalid display name")
        return value
    @field_validator("email")
    @classmethod
    def email_format(cls,value):
        value=value.strip().lower()
        if not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+",value):
            raise ValueError("Invalid email")
        return value

class Refresh(Input):
    refreshToken: str=Field(min_length=20,max_length=256)

class ProfilePatch(Input):
    nickname: str|None=Field(default=None,min_length=2,max_length=80)
    bio: str|None=Field(default=None,max_length=280)
    avatarAttachmentId: str|None=None

class LearningPatch(Input):
    grade: Literal[10,11,12]|None=None
    displayName: str|None=Field(default=None,min_length=2,max_length=80)

class Named(Input):
    title: str=Field(min_length=1,max_length=100)
    @field_validator("title")
    @classmethod
    def not_blank(cls,value):
        value=value.strip()
        if not value:raise ValueError("Empty title")
        return value

class ConversationCreate(Input):
    title: str=Field(default="Chat mới",min_length=1,max_length=120)
    projectId: str|None=None
    grade: Literal[10,11,12]
    handoff: dict|None=None

class ConversationPatch(Input):
    title: str|None=Field(default=None,min_length=1,max_length=120)
    projectId: str|None=None

class MessageCreate(Input):
    clientMessageId: str=Field(min_length=1,max_length=128)
    role: Literal["USER","ASSISTANT"]
    text: str=Field(default="",max_length=30000)
    attachmentIds: list[str]=Field(default_factory=list,max_length=4)
    replyToMessageId: str|None=None
    truncated: bool=False

class EnrollmentCreate(Input):
    subjectId: Literal["math","physics","chemistry","biology","literature","english","geography","history","economics_law"]
    grade: Literal[10,11,12]

class AttemptCreate(Input):
    enrollmentId: str
    lessonId: str
    curriculumVersion: str

class Answer(Input):
    questionId: str
    selectedOptionId: str=Field(min_length=1,max_length=128)
    submissionId: str=Field(min_length=1,max_length=128)

class AttemptPatch(Input):
    step: int=Field(ge=0,le=10000)
    answers: list[Answer]=Field(default_factory=list,max_length=100)

class CompletionCreate(Input):
    attemptId: str
    clientCompletedAt: str|None=None

class PlacementCreate(Input):
    enrollmentId: str

class HandoffCreate(Input):
    questionId: str

class FriendCreate(Input):
    recipientProfileId: str

class QrConnect(Input):
    qrToken: str=Field(min_length=8,max_length=128)

class FriendPatch(Input):
    decision: Literal["ACCEPT","DECLINE"]

class ChangePassword(Input):
    currentPassword: str=Field(min_length=1,max_length=128)
    newPassword: str=Field(min_length=8,max_length=128)

