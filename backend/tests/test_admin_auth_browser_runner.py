import shutil
import subprocess
from pathlib import Path
import pytest


def test_actual_browser_auth_javascript():
    node = shutil.which('node')
    fallback = Path(r'C:\Program Files\nodejs\node.exe')
    if not node and fallback.is_file():
        node = str(fallback)
    if not node:
        pytest.skip('Node.js is not installed')
    result = subprocess.run([node, str(Path(__file__).with_name('test_admin_auth_browser.js'))],
                            capture_output=True, text=True, timeout=20)
    assert result.returncode == 0, result.stdout + result.stderr
    assert 'Admin auth browser tests passed:' in result.stdout
