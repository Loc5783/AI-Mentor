"""Validate import-ready question CSVs without changing a database.

This checks structural errors and duplicates. It cannot prove that a scientific,
historical, or language answer is factually correct; that needs source review.
"""

import argparse
import csv
import re
import unicodedata
from collections import Counter
from pathlib import Path

SUBJECTS = {
    "math", "physics", "chemistry", "biology", "literature", "english",
    "history", "geography", "economics_law",
}


def normalized(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", value).casefold()).strip()


def validate(paths: list[Path]) -> tuple[Counter, list[str]]:
    counts: Counter = Counter()
    problems: list[str] = []
    codes: set[str] = set()
    prompts: set[tuple[str, str, str]] = set()
    for path in paths:
        with path.open(encoding="utf-8-sig", newline="") as stream:
            for line, row in enumerate(csv.DictReader(stream), start=2):
                where = f"{path}:{line}"
                status = row.get("status", "").strip().upper()
                if status not in {"DRAFT", "PUBLISHED"}:
                    problems.append(f"{where}: invalid status")
                    continue
                grade = row.get("grade", "").strip()
                subject = row.get("subjectId", "").strip()
                code = row.get("code", "").strip()
                prompt = row.get("prompt", "").strip()
                if grade not in {"10", "11", "12"} or subject not in SUBJECTS:
                    problems.append(f"{where}: invalid grade or subject")
                if not code or code in codes:
                    problems.append(f"{where}: empty or duplicate code")
                codes.add(code)
                key = (grade, subject, normalized(prompt))
                if not prompt:
                    problems.append(f"{where}: empty prompt")
                if status == "DRAFT":
                    counts[(grade, subject, status)] += 1
                    continue
                if key in prompts:
                    problems.append(f"{where}: duplicate published prompt in grade/subject")
                prompts.add(key)
                choices = [row.get(f"option{letter}", "").strip() for letter in "ABCD"]
                answer = row.get("correctOption", "").strip().upper()
                if any(not choice for choice in choices) or len({normalized(c) for c in choices}) != 4:
                    problems.append(f"{where}: published question needs four distinct choices")
                if answer not in "ABCD" or len(answer) != 1:
                    problems.append(f"{where}: invalid correct option")
                for field in ("chapter", "topic", "explanation", "source", "license"):
                    if not row.get(field, "").strip():
                        problems.append(f"{where}: missing {field}")
                counts[(grade, subject, status)] += 1
    return counts, problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()
    paths = [csv_path for path in args.paths for csv_path in
             (sorted(path.glob("*.csv")) if path.is_dir() else [path])]
    counts, problems = validate(paths)
    for (grade, subject, status), count in sorted(counts.items()):
        print(f"grade={grade} subject={subject} status={status} count={count}")
    for problem in problems:
        print(problem)
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
