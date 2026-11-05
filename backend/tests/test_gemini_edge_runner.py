from pathlib import Path
import shutil
import subprocess
import pytest


def test_gemini_edge_pool_and_handler():
    node=shutil.which('node')
    if not node:pytest.skip('Node.js is not installed')
    folder=Path(__file__).parent
    result=subprocess.run([node,'--test',str(folder/'test_gemini_key_pool.mjs'),str(folder/'test_mentor_ai_handler.mjs')],
                          capture_output=True,text=True,timeout=30)
    assert result.returncode==0,result.stdout+result.stderr
