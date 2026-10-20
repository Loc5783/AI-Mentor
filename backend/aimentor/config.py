from dataclasses import dataclass
from pathlib import Path
import os

@dataclass(frozen=True)
class Settings:
    data_dir: Path
    demo_accounts: bool = False
    access_seconds: int = 900
    refresh_seconds: int = 2592000
    learning_timezone: str = "Asia/Ho_Chi_Minh"
    curriculum_dir: Path|None = None

    @classmethod
    def from_env(cls):
        base = Path(__file__).resolve().parents[1]
        return cls(Path(os.environ.get("MENTOR_DATA_DIR", base / "data")).resolve(),
                   os.environ.get("MENTOR_DEMO_MODE", "0") == "1")

