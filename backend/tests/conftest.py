import socket
import sys

# Windows Python 3.14 compatibility: binding to port 0 (ephemeral allocation)
# can raise [WinError 10013] under restricted socket permissions.
# This fixture ensures socket.socketpair falls back to available local ports.
_orig_socketpair = getattr(socket, "socketpair", None)


def _safe_socketpair(family=socket.AF_INET, type=socket.SOCK_STREAM, proto=0):
    try:
        if _orig_socketpair is not None:
            return _orig_socketpair(family, type, proto)
    except OSError as err:
        if getattr(err, "winerror", None) != 10013 and "[WinError 10013]" not in str(err):
            raise

    for port in range(52000, 58000):
        lsock = socket.socket(family, type, proto)
        lsock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            lsock.bind(("127.0.0.1", port))
            lsock.listen(1)
            csock = socket.socket(family, type, proto)
            csock.connect(("127.0.0.1", port))
            ssock, _ = lsock.accept()
            lsock.close()
            return ssock, csock
        except OSError:
            lsock.close()
            continue
    raise RuntimeError("No available port for socketpair in 52000-58000")


if sys.platform == "win32":
    socket.socketpair = _safe_socketpair
