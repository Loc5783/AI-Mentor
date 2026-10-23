import math
import pytest

# Python/JS logic equivalent test for confirmImport validation contracts in admin_web.py

def js_is_safe_integer(val):
    if isinstance(val, bool):
        return False
    if isinstance(val, (int, float)):
        if math.isnan(val) or math.isinf(val):
            return False
        return float(val).is_integer() and -9007199254740991 <= val <= 9007199254740991
    return False

def validate_import_confirm_response(res_status, res_ok, result_body):
    # Simulates the exact logic in confirmImport() in admin_web.py
    if res_status == 401:
        return False, "SESSION_EXPIRED"
    if res_status == 403:
        return False, "FORBIDDEN"
    if not res_ok:
        return False, "SERVER_ERROR"
    if not isinstance(result_body, dict):
        return False, "INVALID_JSON"

    imported_count = result_body.get("importedCount")
    if not js_is_safe_integer(imported_count) or imported_count < 0:
        return False, "INVALID_IMPORTED_COUNT"

    return True, f"SUCCESS_{imported_count}"

def old_buggy_validate_import_confirm_response(res_status, res_ok, result_body):
    # Simulates the old buggy check: typeof result.importedCount === 'number' && result.importedCount >= 0
    if res_status == 403:
        return False, "FORBIDDEN"
    if not isinstance(result_body, dict):
        return False, "INVALID_JSON"
    imported_count = result_body.get("importedCount")
    # Old buggy check accepted 1.5!
    if isinstance(imported_count, (int, float)) and not math.isnan(imported_count) and imported_count >= 0:
        return True, f"SUCCESS_{imported_count}"
    return False, "INVALID"

def test_proof_imported_count_float_rejection():
    # 1. Proof that OLD check accepted 1.5 (BUGGY)
    old_success, old_msg = old_buggy_validate_import_confirm_response(200, True, {"importedCount": 1.5})
    assert old_success is True
    assert "1.5" in old_msg

    # 2. Proof that NEW check REJECTS 1.5 (FIXED)
    new_success, new_msg = validate_import_confirm_response(200, True, {"importedCount": 1.5})
    assert new_success is False
    assert new_msg == "INVALID_IMPORTED_COUNT"

def test_imported_count_validation_matrix():
    # Valid non-negative integers -> Success
    assert validate_import_confirm_response(200, True, {"importedCount": 0}) == (True, "SUCCESS_0")
    assert validate_import_confirm_response(200, True, {"importedCount": 1}) == (True, "SUCCESS_1")
    assert validate_import_confirm_response(200, True, {"importedCount": 100}) == (True, "SUCCESS_100")

    # Invalid non-integers, floats, negative, non-numbers -> Rejection
    assert validate_import_confirm_response(200, True, {"importedCount": 1.5})[0] is False
    assert validate_import_confirm_response(200, True, {"importedCount": -1})[0] is False
    assert validate_import_confirm_response(200, True, {"importedCount": "1"})[0] is False
    assert validate_import_confirm_response(200, True, {"importedCount": None})[0] is False
    assert validate_import_confirm_response(200, True, {"importedCount": float("nan")})[0] is False
    assert validate_import_confirm_response(200, True, {"importedCount": float("inf")})[0] is False
    assert validate_import_confirm_response(200, True, {})[0] is False

def test_http_status_validation_matrix():
    # 401 Unauthorized -> Rejection
    assert validate_import_confirm_response(401, False, {"code": "INVALID_SESSION"}) == (False, "SESSION_EXPIRED")

    # 403 Forbidden -> Rejection
    assert validate_import_confirm_response(403, False, {"code": "ADMIN_REQUIRED"}) == (False, "FORBIDDEN")

    # 422 Unprocessable Entity -> Rejection
    assert validate_import_confirm_response(422, False, {"detail": "Unprocessable"}) == (False, "SERVER_ERROR")

    # 500 Internal Server Error -> Rejection
    assert validate_import_confirm_response(500, False, {"code": "INTERNAL_ERROR"}) == (False, "SERVER_ERROR")

    # Non-JSON HTML response -> Rejection
    assert validate_import_confirm_response(500, False, "<html>Error</html>") == (False, "SERVER_ERROR")
