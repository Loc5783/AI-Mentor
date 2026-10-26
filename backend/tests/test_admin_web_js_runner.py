import shutil
import subprocess
import sys
from pathlib import Path
import pytest

def test_product_javascript_via_node():
    node_bin = shutil.which("node")
    if not node_bin and Path(r"C:\Program Files\nodejs\node.exe").is_file():
        node_bin = str(Path(r"C:\Program Files\nodejs\node.exe"))
    if not node_bin:
        pytest.skip("Node.js is not installed on this system. Skipping node test runner.")
    node_script = Path(__file__).parent / "test_admin_web_js_node.js"
    res = subprocess.run([node_bin, str(node_script)], capture_output=True, text=True)
    assert res.returncode == 0, f"Node.js JS test failed:\nStdout:\n{res.stdout}\nStderr:\n{res.stderr}"
    assert "ALL PRODUCT JAVASCRIPT TESTS PASSED SUCCESSFULLY!" in res.stdout

def test_real_browser_via_chrome_cdp():
    node_bin = shutil.which("node")
    if not node_bin and Path(r"C:\Program Files\nodejs\node.exe").is_file():
        node_bin = str(Path(r"C:\Program Files\nodejs\node.exe"))
    if not node_bin:
        pytest.skip("Node.js is not installed.")
    chrome_bin = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")
    if not chrome_bin.is_file():
        pytest.skip("Google Chrome is not installed.")
    real_browser_script = Path(__file__).parent / "test_admin_web_real_browser.js"
    res = subprocess.run([node_bin, str(real_browser_script)], capture_output=True, text=True, timeout=40)
    assert res.returncode == 0, f"Real browser test failed:\nStdout:\n{res.stdout}\nStderr:\n{res.stderr}"
    assert "ALL REAL BROWSER CDP TESTS PASSED SUCCESSFULLY!" in res.stdout
