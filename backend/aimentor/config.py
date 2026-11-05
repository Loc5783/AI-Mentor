from dataclasses import dataclass, field
from pathlib import Path
import os
import json

@dataclass(frozen=True)
class Settings:
    data_dir: Path
    demo_accounts: bool = False
    access_seconds: int = 900
    refresh_seconds: int = 2592000
    learning_timezone: str = "Asia/Ho_Chi_Minh"
    curriculum_dir: Path|None = None

    gemini_api_keys: tuple[str, ...] = field(default=(), repr=False)
    gemini_model: str = ""
    admin_catalog: str = "sqlite"
    admin_auth_provider: str = "sqlite"
    supabase_url: str = ""
    supabase_server_key: str = field(default="", repr=False)

    @classmethod
    def from_env(cls):
        base = Path(__file__).resolve().parents[1]
        values = {}
        for filename in (".env.supabase.local", ".env.gemini.local"):
            local=base/filename
            if not local.exists():continue
            for line in local.read_text(encoding="utf-8-sig").splitlines():
                if "=" not in line or line.lstrip().startswith("#"):continue
                name,value=line.split("=",1)
                if name.strip() in ("SUPABASE_URL","SUPABASE_SERVICE_ROLE_KEY","SUPABASE_SECRET_KEY",
                                    "MENTOR_ADMIN_CATALOG","MENTOR_ADMIN_AUTH_PROVIDER","GEMINI_API_KEYS","GEMINI_MODEL"):
                    values[name.strip()]=value.strip().strip("\"'")
        def setting(name, default=""):
            return os.environ.get(name, values.get(name, default)).strip()
        url = setting("SUPABASE_URL")
        key = setting("SUPABASE_SECRET_KEY") or setting("SUPABASE_SERVICE_ROLE_KEY")
        mode = setting("MENTOR_ADMIN_CATALOG", "supabase" if key else "sqlite").lower()
        if mode not in ("sqlite", "supabase"):
            raise ValueError("MENTOR_ADMIN_CATALOG must be sqlite or supabase")
        if mode == "supabase" and not (url and key):
            raise ValueError("Supabase catalog requires a URL and server secret")
        auth_provider = setting("MENTOR_ADMIN_AUTH_PROVIDER", "supabase" if mode == "supabase" else "sqlite").lower()
        if auth_provider not in ("sqlite", "supabase"):
            raise ValueError("MENTOR_ADMIN_AUTH_PROVIDER must be sqlite or supabase")
        if auth_provider == "supabase" and not (url and key):
            raise ValueError("Supabase admin authentication requires a URL and server secret")
        packed=setting("GEMINI_API_KEYS")
        try: gemini_keys=json.loads(packed) if packed else []
        except ValueError: raise ValueError("GEMINI_API_KEYS must be a JSON array") from None
        if not isinstance(gemini_keys,list) or len(gemini_keys)>16 or any(not isinstance(k,str) for k in gemini_keys):
            raise ValueError("GEMINI_API_KEYS must contain at most 16 string keys")
        gemini_keys=tuple(dict.fromkeys(k.strip() for k in gemini_keys if k.strip()))
        return cls(Path(os.environ.get("MENTOR_DATA_DIR", base / "data")).resolve(),
                   os.environ.get("MENTOR_DEMO_MODE", "0") == "1",
                   admin_catalog=mode, admin_auth_provider=auth_provider, supabase_url=url, supabase_server_key=key,
                   gemini_api_keys=gemini_keys,gemini_model=setting("GEMINI_MODEL"))
