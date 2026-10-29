"""Isolated API acceptance, distinct from browser and physical-device E2E."""
from verify_api_acceptance import run_tests


def test_isolated_api_acceptance():
    run_tests()
