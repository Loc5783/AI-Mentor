"""HTTP adaptation only; learning rules live in LearningService."""
from fastapi import APIRouter, Depends, Header, Query
from . import models
from .common import ACTIVE_SUBJECTS, ALL_SUBJECTS


def learning_router(service, current):
    router = APIRouter(tags=["Learning"])

    @router.get("/subjects")
    def subjects(user=Depends(current)):
        return {"items": [{"id": subject, "implementation": "ACTIVE" if subject in ACTIVE_SUBJECTS else "FRAMEWORK",
                           "availableGrades": [grade for grade in (10, 11, 12) if (subject, grade) in service.catalog.courses]}
                          for subject in ALL_SUBJECTS]}

    @router.get("/enrollments")
    def enrollments(user=Depends(current)):
        return service.list_enrollments(user["id"])

    @router.post("/enrollments", status_code=201)
    def enroll(body: models.EnrollmentCreate, user=Depends(current), key: str | None=Header(None, alias="Idempotency-Key")):
        return service.enroll(user["id"], body.model_dump(), key)

    @router.get("/enrollments/{item}/path")
    def path(item: str, user=Depends(current)):
        return service.path(user["id"], item)

    @router.post("/placements", status_code=201)
    def placement(body: models.PlacementCreate, user=Depends(current), key: str | None=Header(None, alias="Idempotency-Key")):
        return service.start(user["id"], body.model_dump(), key, placement=True)

    @router.post("/attempts", status_code=201)
    def attempt(body: models.AttemptCreate, user=Depends(current), key: str | None=Header(None, alias="Idempotency-Key")):
        return service.start(user["id"], body.model_dump(), key)

    @router.get("/attempts/{item}")
    def get_attempt(item: str, user=Depends(current)):
        return service.get_attempt(user["id"], item)

    @router.patch("/attempts/{item}")
    def save(item: str, body: models.AttemptPatch, user=Depends(current), revision: str | None=Header(None, alias="If-Match")):
        return service.save(user["id"], item, body.model_dump(), revision)

    @router.post("/completions", status_code=201)
    def complete(body: models.CompletionCreate, user=Depends(current), key: str | None=Header(None, alias="Idempotency-Key")):
        return service.complete(user["id"], body.model_dump(), key)

    @router.post("/attempts/{item}/handoffs", status_code=201)
    def handoff(item: str, body: models.HandoffCreate, user=Depends(current), key: str | None=Header(None, alias="Idempotency-Key")):
        return service.handoff(user["id"], item, body.model_dump(), key)

    @router.get("/lesson-handoffs/{item}/resume")
    def resume(item: str, user=Depends(current)):
        return service.resume(user["id"], item)

    @router.get("/question-bank")
    def bank(enrollmentId: str=Query(...), user=Depends(current)):
        return service.bank(user["id"], enrollmentId)

    return router
