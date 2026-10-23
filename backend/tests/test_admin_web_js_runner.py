import subprocess
import sys
from pathlib import Path

def test_product_javascript_via_node():
    node_script = Path(__file__).parent / "test_admin_web_js_node.js"
    res = subprocess.run(["node", str(node_script)], capture_output=True, text=True)
    assert res.returncode == 0, f"Node.js JS test failed:\nStdout:\n{res.stdout}\nStderr:\n{res.stderr}"
    assert "ALL PRODUCT JAVASCRIPT TESTS PASSED SUCCESSFULLY!" in res.stdout
