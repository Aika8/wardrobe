#!/usr/bin/env python3
"""The backlog run's rules as code: which task is next, which paths a PR may touch, which proposals enter the
queue, when a PR is merged. Agents call it; GitHub Actions call it; nobody has to trust a prompt for these rules.
Copied from Aika8/learning-exchange (DECISIONS.md: a shared library later); config adapted for the wardrobe.
See docs/backlog/README.md. Standard library only (runs in the cloud VM and on ubuntu-latest as is).

Pattern: Policy as code — the merge gate and the path rules live in one tested module, not in prompts.

Commands
  lint                         check docs/backlog/QUEUE.md (format, ids, depends, description size)
  show ID                      print one task
  next [--state FILE]          print the next ready task (state from GitHub via `gh api`, or a JSON file)
  paused                       exit 0 when docs/backlog/PAUSE exists (agents stop), 1 otherwise
  check-paths --branch B (--base REF | --files FILE)
                               exit 0 when the PR's changed files are allowed for its branch, 1 with reasons
  intake [--date YYYY-MM-DD]   move at most 5 small, checkable proposals of that day into QUEUE.md
  automerge [--dry-run]        merge every open agent PR that passed all gates (GitHub Actions only)
  plan                         the executor's move for this run: PAUSED | PARK | FIX | REBASE | CI-RED |
                               WIP-LIMIT | NOTHING | NEW <task>
  review-queue                 the reviewer's move: PAUSED | REVIEW #n … | NOTHING
  prod-changes [--base REF]    `added <path>` / `changed <path>` for non-test, non-doc files (the reviewer's
                               "tests in a vacuum" check)
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
QUEUE = Path("docs/backlog/QUEUE.md")
PAUSE = Path("docs/backlog/PAUSE")
PROPOSALS_DIR = Path("docs/backlog/proposals")
INTAKE_LOG = Path("docs/backlog/intake-log.md")
DEFAULT_REPO = "Aika8/wardrobe"
MAX_DESCRIPTION_CHARS = 6000  # ≈ 1500 tokens
MAX_INTAKE_PER_DAY = 5
MAX_OPEN_AGENT_PRS = 3

# Nobody but Aika changes these (an agent PR touching one is not merged, whatever its task says).
PROTECTED = [
    ".github/**",
    "Makefile",
    "**/CLAUDE.md",
    "CLAUDE.md",
    "scripts/backlog/**",
    "scripts/cloud/**",
    "docs/backlog/README.md",
    "docs/backlog/QUEUE.md",
    "docs/backlog/PROPOSALS.md",
    "docs/backlog/QUESTIONS-BEFORE-LEAVE.md",
    "docs/backlog/prompts/**",
    "docs/backlog/PAUSE",
    "docs/backlog/intake-log.md",
    "docs/backlog/TECH-DEBT.md",
    "docs/backlog/experience/README.md",
    "gradle/wrapper/**",
    "docs/adr/0000-template.md",
    "gradlew",
    "gradlew.bat",
    ".env*",
    "**/.env*",
    ".claude/**",
    "**/.claude/**",
    ".mcp.json",
    "**/AGENTS.md",
    "settings.gradle",
    "gradle.properties",
    "**/gradle.properties",
    "docs/backlog/UNATTENDED",
    # Test-engine configuration could switch tests off without a skip marker.
    "**/junit-platform.properties",
    "**/META-INF/**",
    "**/.npmrc",
]
# Approved designs are Aika's: once she has looked at a screen or the avatar and approved it, the reference goes
# here and no cloud agent changes it. Drafts of screens and the avatar (client/, avatar logic) are not visual in
# this sense: agents build them, Aika checks their look after the week (tasks marked «вид проверяет Aika»).
VISUAL = [
    "docs/design/approved/**",
]
NOT_VISUAL: list[str] = []
# Proposals from the analyst/user runs may not reach build or test configuration (code would merge a test exclusion).
INTAKE_DENY = [
    "**/build.gradle",
    "build.gradle",
    "**/package.json",
    "**/package-lock.json",
    "**/*.config.*",
    "**/tsconfig*.json",
    "docs/adr/**",
    "api/**",
    "intent/**",
]
# Tests and quality gates of earlier work: new files may be added, existing ones are never changed or removed.
APPEND_ONLY = [
    "**/src/test/**",
    "client/test/**",
    "client/**/*.test.ts",
    "client/**/*.test.tsx",
    "client/**/*.test.mjs",
    "**/quality-gates/**",
    "**/test_*.py",
    "docs/backlog/tech-debt/**",
    "docs/backlog/experience/**",
]
# Notes, not tests: append-only, but a note may quote `@Disabled` or `test.skip(` when it reports one.
NOTES = ["docs/backlog/tech-debt/**", "docs/backlog/experience/**"]
# Every task PR may add these (new files only — APPEND_ONLY covers the two folders).
ALWAYS_ALLOWED_FOR_TASKS = ["DECISIONS.md", "docs/backlog/tech-debt/*.md", "docs/backlog/experience/*.md"]
SKIP_MARKERS = re.compile(r"@Disabled|@Ignore|\btest\.skip\(|\bit\.skip\(|\bdescribe\.skip\(|\btest\.todo\(|"
                          r"\bpending\(|assumeTrue\(false|\bskip\s*:\s*true|func\s+skip_", re.I)
ADR_FILE = re.compile(r"^docs/adr/\d{4}-.*\.md$")


# ---------------------------------------------------------------- globs

def glob_re(pattern: str) -> re.Pattern:
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out += "(?:.*/)?"; i += 3
        elif pattern.startswith("**", i):
            out += ".*"; i += 2
        elif pattern[i] == "*":
            out += "[^/]*"; i += 1
        elif pattern[i] == "?":
            out += "[^/]"; i += 1
        else:
            out += re.escape(pattern[i]); i += 1
    return re.compile("^" + out + "$")


def matches(path: str, patterns: list[str]) -> bool:
    return any(glob_re(p).match(path) for p in patterns)


def narrow(path: str) -> bool:
    """A proposal's glob names at least three folders before any wildcard: `domain/src/main/**` yes, `app/src/**` no
    (here modules sit at the top level, so two folders would still reach test resources and configuration)."""
    head = re.split(r"[*?]", path, 1)[0]
    return head == path or head.count("/") >= 3


def is_production(path: str) -> bool:
    """Code whose behaviour tests pin: not a test, not docs, not the backlog's own files."""
    return not (matches(path, APPEND_ONLY) or path.endswith(".md") or path.startswith("docs/"))


def visual(path: str) -> bool:
    return matches(path, VISUAL) and not matches(path, NOT_VISUAL)


# ---------------------------------------------------------------- queue

@dataclass
class Task:
    id: str
    title: str
    fields: dict = field(default_factory=dict)
    description: str = ""
    line: int = 0

    @property
    def status(self) -> str:
        return self.fields.get("status", "todo")

    @property
    def depends(self) -> list[str]:
        raw = self.fields.get("depends", "")
        return [d.strip() for d in re.split(r"[,\s]+", raw) if re.match(r"^[A-Z]+-[\w-]+$", d.strip())]

    @property
    def paths(self) -> list[str]:
        return re.findall(r"`([^`]+)`", self.fields.get("paths", ""))

    @property
    def needs(self) -> list[str]:
        return [n.strip() for n in self.fields.get("needs", "").split(",") if n.strip()]

    @property
    def branch(self) -> str:
        return "claude/" + self.id.lower()

    def text(self) -> str:
        head = f"### {self.id} — {self.title}\n" + "".join(f"- {k}: {v}\n" for k, v in self.fields.items())
        return head + "\n" + self.description.strip() + "\n"


TASK_HEAD = re.compile(r"^### ([A-Z]+-[\w-]+) — (.+)$")
FIELD = re.compile(r"^- ([a-z]+): (.*)$")
REQUIRED = ["status", "size", "paths", "verify", "expect"]
STATUSES = {"todo", "waiting", "done", "dropped"}
TOOLS = {"docker"}  # tools the cloud image may lack; a task that needs a missing one is not picked


def parse_tasks(text: str) -> list[Task]:
    tasks: list[Task] = []
    cur: Task | None = None
    in_fields = False
    for n, line in enumerate(text.splitlines(), 1):
        m = TASK_HEAD.match(line)
        if m:
            cur = Task(m.group(1), m.group(2).strip(), line=n)
            tasks.append(cur)
            in_fields = True
            continue
        if cur is None:
            continue
        if line.startswith("## ") or line.startswith("# "):
            cur = None
            continue
        f = FIELD.match(line)
        if in_fields and f:
            cur.fields[f.group(1)] = f.group(2).strip()
            continue
        if in_fields and line.strip() == "":
            in_fields = False
            continue
        in_fields = False
        cur.description += line + "\n"
    return tasks


def lint(tasks: list[Task]) -> list[str]:
    errors, seen = [], set()
    ids = {t.id for t in tasks}
    for t in tasks:
        where = f"{t.id} (line {t.line})"
        if t.id in seen:
            errors.append(f"{where}: duplicate id")
        seen.add(t.id)
        for r in REQUIRED:
            if not t.fields.get(r):
                errors.append(f"{where}: missing '{r}'")
        if t.status not in STATUSES:
            errors.append(f"{where}: status '{t.status}' not in {sorted(STATUSES)}")
        if t.status == "waiting" and not t.fields.get("waits"):
            errors.append(f"{where}: a waiting task says what it waits for in 'waits'")
        for d in t.depends:
            if d not in ids:
                errors.append(f"{where}: depends on unknown {d}")
        if len(t.description) > MAX_DESCRIPTION_CHARS:
            errors.append(f"{where}: description {len(t.description)} chars > {MAX_DESCRIPTION_CHARS}")
        if not t.description.strip():
            errors.append(f"{where}: empty description")
        if not t.paths:
            errors.append(f"{where}: no paths in backticks")
        for p in t.paths:
            if matches(p, PROTECTED) or visual(p):
                errors.append(f"{where}: path {p} is protected or visual")
        if not re.search(r"`[^`]+`", t.fields.get("verify", "")):
            errors.append(f"{where}: verify must hold a command in backticks")
        for n in t.needs:
            if n not in TOOLS:
                errors.append(f"{where}: needs '{n}' is not one of {sorted(TOOLS)}")
    return errors


def load_tasks(root: Path = ROOT) -> list[Task]:
    return parse_tasks((root / QUEUE).read_text(encoding="utf-8"))


# ---------------------------------------------------------------- GitHub state

def repo_slug() -> str:
    if os.environ.get("GITHUB_REPOSITORY"):
        return os.environ["GITHUB_REPOSITORY"]
    try:
        url = subprocess.run(["git", "-C", str(ROOT), "remote", "get-url", "origin"],
                             capture_output=True, text=True, check=True).stdout.strip()
        m = re.search(r"([\w.-]+)/([\w.-]+?)(?:\.git)?/?$", url)
        if m:
            return f"{m.group(1)}/{m.group(2)}"
    except Exception:
        pass
    return DEFAULT_REPO


def gh(*args: str, method: str = "GET", fields: dict | None = None, paginate: bool = False):
    cmd = ["gh", "api", "-X", method]
    if paginate:
        cmd.append("--paginate")
    for k, v in (fields or {}).items():
        cmd += ["-f" if isinstance(v, str) else "-F", f"{k}={json.dumps(v) if isinstance(v, bool) else v}"]
    cmd += list(args)
    out = subprocess.run(cmd, capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(f"gh api {' '.join(args)}: {out.stderr.strip() or out.stdout.strip()}")
    if not out.stdout.strip():
        return None
    if paginate:  # --paginate prints one JSON array per page, back to back
        items, dec, s, i = [], json.JSONDecoder(), out.stdout, 0
        while i < len(s):
            while i < len(s) and s[i].isspace():
                i += 1
            if i >= len(s):
                break
            page, i = dec.raw_decode(s, i)
            items.extend(page if isinstance(page, list) else [page])
        return items
    return json.loads(out.stdout)


STALE_BRANCH_HOURS = 3


def fetch_state(repo: str, tasks: list[Task] | None = None) -> dict:
    prs = gh(f"repos/{repo}/pulls?state=all&per_page=100", paginate=True) or []
    parked = gh(f"repos/{repo}/issues?state=open&labels=parked&per_page=100", paginate=True) or []
    with_pr = {p["head"]["ref"] for p in prs}
    branches = {}
    for b in gh(f"repos/{repo}/branches?per_page=100", paginate=True) or []:
        name = b["name"]
        if name.startswith("claude/") and name not in with_pr and tasks and any(t.branch == name for t in tasks):
            c = gh(f"repos/{repo}/commits/{b['commit']['sha']}")
            branches[name] = c["commit"]["committer"]["date"]
    return {
        "branches": branches,
        "prs": [{"number": p["number"], "head": p["head"]["ref"], "state": p["state"],
                 "merged": bool(p.get("merged_at")), "labels": [l["name"] for l in p.get("labels", [])],
                 "draft": p.get("draft", False)} for p in prs],
        "parked_issues": [i["title"] for i in parked if "pull_request" not in i],
    }


def task_states(tasks: list[Task], state: dict) -> dict[str, str]:
    """done | open | parked | ready | blocked | waiting | dropped for each task."""
    by_branch: dict[str, list[dict]] = {}
    for p in state.get("prs", []):
        by_branch.setdefault(p["head"], []).append(p)
    parked_ids = {m.group(1) for t in state.get("parked_issues", [])
                  for m in [re.search(r"\b([A-Z]+-[\w-]+)\b", t)] if m}
    out: dict[str, str] = {}
    for t in tasks:
        prs = by_branch.get(t.branch, [])
        if t.status == "done" or any(p["merged"] for p in prs):
            out[t.id] = "done"
        elif t.status in ("dropped", "waiting"):
            out[t.id] = t.status
        elif t.id in parked_ids or any("parked" in p["labels"] for p in prs):
            out[t.id] = "parked"
        elif any(p["state"] == "open" for p in prs):
            out[t.id] = "open"
        elif t.branch in state.get("branches", {}) and _fresh(state["branches"][t.branch], state.get("now")):
            out[t.id] = "open"  # a run started it (pushed the branch) and has not opened its PR yet
        else:
            out[t.id] = "pending"
    for t in tasks:
        if out[t.id] == "pending":
            out[t.id] = "ready" if all(out.get(d) == "done" for d in t.depends) else "blocked"
    return out


def _fresh(iso: str, now: str | None) -> bool:
    t = dt.datetime.fromisoformat(iso.replace("Z", "+00:00"))
    n = dt.datetime.fromisoformat(now.replace("Z", "+00:00")) if now else dt.datetime.now(dt.timezone.utc)
    return (n - t) < dt.timedelta(hours=STALE_BRANCH_HOURS)


def tool_available(name: str) -> bool:
    import shutil
    if not shutil.which(name):
        return False
    if name == "docker":
        return subprocess.run(["docker", "info"], capture_output=True).returncode == 0
    return True


def next_task(tasks: list[Task], state: dict, has_tool=tool_available) -> Task | None:
    st = task_states(tasks, state)
    return next((t for t in tasks if st[t.id] == "ready" and all(has_tool(n) for n in t.needs)), None)


def open_agent_prs(state: dict) -> list[dict]:
    return [p for p in state.get("prs", []) if p["state"] == "open" and p["head"].startswith("claude/")
            and "parked" not in p["labels"]]


# ---------------------------------------------------------------- paths

@dataclass
class Change:
    status: str  # added | modified | removed | renamed
    path: str
    previous: str | None = None
    added_text: str = ""


def branch_kind(branch: str, tasks: list[Task]) -> tuple[str, Task | None]:
    for t in tasks:
        if branch == t.branch:
            return "task", t
    if re.match(r"^claude/proposals-(analyst|user)-\d{4}-\d{2}-\d{2}$", branch):
        return "proposals", None
    if re.match(r"^claude/summary-\d{4}-\d{2}-\d{2}$", branch):
        return "summary", None
    return "unknown", None


def adr_is_open(path: str, root: Path) -> bool:
    """An ADR may be edited only while it is Proposed (CLAUDE.md: never edit an Accepted ADR)."""
    f = root / path
    if not f.exists():
        return True
    head = f.read_text(encoding="utf-8")[:2000]
    m = re.search(r"(?im)^\s*(?:[-*]\s*)?\**status\**\s*:?\**\s*:?\s*\**\s*([A-Za-z]+)", head)
    return bool(m) and m.group(1).lower() == "proposed"


def check_paths(branch: str, changes: list[Change], tasks: list[Task], root: Path = ROOT) -> list[str]:
    kind, task = branch_kind(branch, tasks)
    problems: list[str] = []
    if kind == "unknown":
        return [f"branch {branch} is not an agent branch of a queued task, proposals or summary"]
    if not changes:
        return ["the PR changes nothing"]
    for c in changes:
        paths = [c.path] + ([c.previous] if c.previous else [])
        for p in paths:
            if matches(p, PROTECTED):
                problems.append(f"{p}: protected (only Aika changes it)")
            if visual(p):
                problems.append(f"{p}: an approved design (look) is Aika's; cloud agents do not change it")
            if c.status != "added" and matches(p, APPEND_ONLY):
                problems.append(f"{p}: an existing test or quality gate is {c.status}; add a new file instead")
            if c.status != "added" and ADR_FILE.match(p) and not adr_is_open(p, root):
                problems.append(f"{p}: an accepted ADR is never edited; supersede it with a new ADR")
        if c.status == "added" and matches(c.path, APPEND_ONLY) and not matches(c.path, NOTES) \
                and SKIP_MARKERS.search(c.added_text):
            problems.append(f"{c.path}: a new test file skips or disables tests")
        if kind == "task":
            if not matches(c.path, task.paths + ALWAYS_ALLOWED_FOR_TASKS):
                problems.append(f"{c.path}: outside the paths of {task.id} ({', '.join(task.paths)})")
        elif kind == "proposals":
            role, day = re.match(r"^claude/proposals-(\w+)-(.+)$", branch).groups()
            if not (c.status == "added" and c.path == f"{PROPOSALS_DIR}/{day}-{role}.md"):
                problems.append(f"{c.path}: a proposals PR adds only {PROPOSALS_DIR}/{day}-{role}.md")
        elif kind == "summary":
            day = branch.rsplit("-", 3)[-3:]
            want = f"docs/backlog/summaries/{'-'.join(day)}.md"
            if c.path != want:
                problems.append(f"{c.path}: a summary PR changes only {want}")
    return sorted(set(problems))


def changes_from_git(base: str) -> list[Change]:
    out = subprocess.run(["git", "-C", str(ROOT), "diff", "--name-status", "-M", f"{base}...HEAD"],
                         capture_output=True, text=True, check=True).stdout
    changes = []
    for line in out.splitlines():
        parts = line.split("\t")
        code = parts[0][0]
        if code == "R":
            changes.append(Change("renamed", parts[2], parts[1]))
        else:
            status = {"A": "added", "D": "removed"}.get(code, "modified")
            text = ""
            if status == "added":
                text = subprocess.run(["git", "-C", str(ROOT), "show", f"HEAD:{parts[1]}"],
                                      capture_output=True, text=True).stdout
            changes.append(Change(status, parts[1], None, text))
    return changes


def changes_from_api(files: list[dict]) -> list[Change]:
    out = []
    for f in files:
        added = "\n".join(l[1:] for l in (f.get("patch") or "").splitlines() if l.startswith("+"))
        out.append(Change(f["status"] if f["status"] in ("added", "removed", "renamed") else "modified",
                          f["filename"], f.get("previous_filename"), added))
    return out


# ---------------------------------------------------------------- proposals → queue

PROPOSAL_HEAD = re.compile(r"^### (.+)$")


def parse_proposals(text: str) -> list[Task]:
    """A proposal looks like a task without an id: '### title', fields, description."""
    numbered, n = [], 0
    for line in text.splitlines():
        if PROPOSAL_HEAD.match(line) and not TASK_HEAD.match(line):
            n += 1
            numbered.append(f"### PROP-{n} — {PROPOSAL_HEAD.match(line).group(1)}")
        else:
            numbered.append(line)
    return parse_tasks("\n".join(numbered))


def proposal_problems(p: Task, existing: list[Task]) -> list[str]:
    probs = []
    if p.fields.get("size", "").upper() != "S":
        probs.append("not size S")
    for r in ("paths", "verify", "expect"):
        if not p.fields.get(r):
            probs.append(f"no {r}")
    if not re.search(r"`[^`]+`", p.fields.get("verify", "")):
        probs.append("verify has no command in backticks")
    for path in p.paths:
        if matches(path, PROTECTED) or visual(path) or matches(path, INTAKE_DENY):
            probs.append(f"path {path} is protected, visual, or build/test/contract configuration")
        if not narrow(path):
            probs.append(f"path {path} is not narrowed (a ** must sit at least two folders deep)")
    for d in re.split(r"[,\s]+", p.fields.get("depends", "")):
        if d and not any(t.id == d for t in existing):
            probs.append(f"depends on unknown {d}")
    for n in p.needs:
        if n not in TOOLS:
            probs.append(f"needs '{n}' unknown")
    if not p.paths:
        probs.append("no paths in backticks")
    if len(p.description) > MAX_DESCRIPTION_CHARS or not p.description.strip():
        probs.append("description empty or longer than ~1500 tokens")
    if any(t.title.strip().lower() == p.title.strip().lower() for t in existing):
        probs.append("same title as a queued task")
    return probs


def intake(day: str, root: Path = ROOT) -> tuple[list[str], list[str]]:
    queue_file = root / QUEUE
    tasks = parse_tasks(queue_file.read_text(encoding="utf-8"))
    taken_sources = {t.fields.get("source") for t in tasks}
    already_today = sum(1 for t in tasks if t.fields.get("added") == day and t.fields.get("source"))
    accepted, rejected, new_text = [], [], ""
    for f in sorted((root / PROPOSALS_DIR).glob(f"{day}-*.md")):
        for p in parse_proposals(f.read_text(encoding="utf-8")):
            src = f"{f.relative_to(root)}#{p.id.split('-')[1]}"
            if src in taken_sources:
                continue
            probs = proposal_problems(p, tasks)
            if not probs and already_today + len(accepted) >= MAX_INTAKE_PER_DAY:
                probs = [f"daily cap of {MAX_INTAKE_PER_DAY} reached"]
            if probs:
                rejected.append(f"{src} «{p.title}»: {'; '.join(probs)}")
                continue
            new_id = f"AUTO-{day.replace('-', '')}-{already_today + len(accepted) + 1}"
            fields = {"status": "todo", "size": "S", "paths": p.fields["paths"],
                      "verify": p.fields["verify"], "expect": p.fields["expect"], "source": src, "added": day}
            for extra in ("depends", "needs"):
                if p.fields.get(extra):
                    fields[extra] = p.fields[extra]
            t = Task(new_id, p.title, fields,
                     (f"Why: {p.fields['why']}\n\n" if p.fields.get("why") else "") + p.description)
            accepted.append(f"{new_id} ← {src} «{p.title}»")
            new_text += "\n" + t.text()
            tasks.append(t)
    if new_text:
        body = queue_file.read_text(encoding="utf-8").rstrip("\n") + "\n"
        if "## Added by intake" not in body:
            body += "\n## Added by intake\n\nAppended by `scripts/backlog/backlog.py intake`, at most "\
                    f"{MAX_INTAKE_PER_DAY} a day.\n"
        queue_file.write_text(body + new_text, encoding="utf-8")
    if accepted or rejected:
        log = root / INTAKE_LOG
        prev = log.read_text(encoding="utf-8") if log.exists() else "# Intake log\n"
        entry = f"\n## {day}\n" + "".join(f"- accepted: {a}\n" for a in accepted) + \
                "".join(f"- left for Aika: {r}\n" for r in rejected)
        if f"\n## {day}\n" not in prev:
            log.write_text(prev.rstrip("\n") + "\n" + entry, encoding="utf-8")
    return accepted, rejected


# ---------------------------------------------------------------- automerge (GitHub Actions)

AUX_CHECKS = {"merge", "reset-review", "intake", "guard"}


def ci_state(runs: list[dict]) -> str:
    """green | red | pending | none (no `test` check run on this commit yet)."""
    if any(r["status"] == "completed" and r["conclusion"] in ("failure", "cancelled", "timed_out")
           for r in runs if r["name"] not in AUX_CHECKS):
        return "red"
    tests = [r for r in runs if r["name"] == "test"]
    if not tests:
        return "none"
    if any(r["status"] != "completed" for r in tests):
        return "pending"
    latest = max(tests, key=lambda r: r.get("completed_at") or "")
    return "green" if latest["conclusion"] == "success" else "red"


def ci_green(repo: str, sha: str) -> str:
    return ci_state(gh(f"repos/{repo}/commits/{sha}/check-runs?per_page=100")["check_runs"])


def comment_once(repo: str, number: int, marker: str, body: str):
    comments = gh(f"repos/{repo}/issues/{number}/comments?per_page=100", paginate=True) or []
    if not any(marker in c.get("body", "") for c in comments):
        gh(f"repos/{repo}/issues/{number}/comments", method="POST", fields={"body": f"{body}\n\n{marker}"})


def add_label(repo: str, number: int, label: str):
    gh(f"repos/{repo}/issues/{number}/labels", method="POST", fields={"labels[]": label})


REVIEW_RE = re.compile(r"<!-- backlog-review round=(\d+) verdict=(pass|fail) sha=([0-9a-f]{7,40}) -->")


def last_review(bodies: list[str]) -> tuple[str, str] | None:
    """(verdict, sha) of the latest reviewer marker, or None."""
    found = [m for b in bodies for m in REVIEW_RE.finditer(b)]
    return (found[-1].group(2), found[-1].group(3)) if found else None


def is_main_update(c: dict) -> bool:
    """A merge of main into the branch made by GitHub itself (automerge's update-branch), verified."""
    return (len(c.get("parents", [])) == 2 and (c.get("committer") or {}).get("login") == "web-flow"
            and bool(c["commit"].get("verification", {}).get("verified"))
            and c["commit"]["message"].startswith("Merge branch 'main' into"))


def review_covers_head(repo: str, bodies: list[str], head: str, max_steps: int = 20) -> bool:
    """The latest review passed, and from the head back to the reviewed commit (first parents) there are only
    GitHub's own merges of main."""
    r = last_review(bodies)
    if not r or r[0] != "pass":
        return False
    sha = head
    for _ in range(max_steps):
        if sha.startswith(r[1]) or r[1].startswith(sha):
            return True
        c = gh(f"repos/{repo}/commits/{sha}")
        if not is_main_update(c):
            return False
        sha = c["parents"][0]["sha"]
    return False


POLL_DELAY = 5
POLL_TRIES = 12


def wait_new_head(repo: str, n: int, old_sha: str) -> str | None:
    import time
    for _ in range(POLL_TRIES):
        time.sleep(POLL_DELAY)
        sha = gh(f"repos/{repo}/pulls/{n}")["head"]["sha"]
        if sha != old_sha:
            return sha
    return None


def automerge_one(repo: str, pr: dict, tasks: list[Task], dry_run: bool, say) -> bool:
    """Look at one PR; True when it was merged."""
    n, branch, sha = pr["number"], pr["head"]["ref"], pr["head"]["sha"]
    labels = {l["name"] for l in pr.get("labels", [])}
    kind, task = branch_kind(branch, tasks)
    if kind == "unknown" or pr.get("draft"):
        say("not an agent PR, left alone"); return False
    if labels & {"parked", "hold", "needs-fix", "blocked-paths", "needs-rebase"}:
        say(f"skipped: labels {sorted(labels)}"); return False
    files = gh(f"repos/{repo}/pulls/{n}/files?per_page=100", paginate=True) or []
    problems = check_paths(branch, changes_from_api(files), tasks)
    if problems:
        say("paths not allowed: " + "; ".join(problems))
        if not dry_run:
            comment_once(repo, n, "<!-- backlog-paths -->",
                         "Not merged automatically, the PR touches paths it may not:\n" +
                         "\n".join(f"- {p}" for p in problems))
            add_label(repo, n, "blocked-paths")
        return False
    cmp = gh(f"repos/{repo}/compare/main...{sha}")
    behind = cmp.get("behind_by", 0) > 0
    ci = ci_green(repo, sha)
    if ci == "none" or (ci == "green" and behind):
        # No CI on this head (a conflicted PR, or a head made by update-branch): bring it up to date, or run CI.
        if behind:
            say(f"behind main by {cmp['behind_by']}: updating the branch and running CI on it")
            if not dry_run:
                try:
                    gh(f"repos/{repo}/pulls/{n}/update-branch", method="PUT", fields={"expected_head_sha": sha})
                except RuntimeError as e:
                    say(f"update failed ({e}); the executor merges main into it")
                    add_label(repo, n, "needs-rebase")
                    return False
                # The update is asynchronous and starts no workflow: wait for the new head, then run CI on it.
                if wait_new_head(repo, n, sha):
                    gh(f"repos/{repo}/actions/workflows/ci.yml/dispatches", method="POST", fields={"ref": branch})
        else:
            say("no CI on the head commit: dispatching CI")
            if not dry_run:
                gh(f"repos/{repo}/actions/workflows/ci.yml/dispatches", method="POST", fields={"ref": branch})
        return False
    if ci != "green":
        say(f"CI {ci}"); return False
    if kind == "task":
        comments = gh(f"repos/{repo}/issues/{n}/comments?per_page=100", paginate=True) or []
        if "review:pass" not in labels:
            say("waits for the independent review"); return False
        if not review_covers_head(repo, [c.get("body", "") for c in comments], sha):
            say("the review does not cover this commit: asking for a new review")
            if not dry_run:
                gh(f"repos/{repo}/issues/{n}/labels/review:pass", method="DELETE")
            return False
    say("all gates passed: merging")
    if dry_run:
        return False
    gh(f"repos/{repo}/pulls/{n}/merge", method="PUT",
       fields={"merge_method": "squash", "sha": sha, "commit_title": f"{pr['title']} (#{n})"})
    # Merges made with GITHUB_TOKEN start no workflow: run CI on main explicitly.
    gh(f"repos/{repo}/actions/workflows/ci.yml/dispatches", method="POST", fields={"ref": "main"})
    return True


def automerge(dry_run: bool = False) -> int:
    repo = repo_slug()
    if (ROOT / PAUSE).exists():
        print("automerge: docs/backlog/PAUSE exists on main — nothing is merged")
        return 0
    tasks = load_tasks()
    prs = gh(f"repos/{repo}/pulls?state=open&base=main&per_page=100", paginate=True) or []
    merged_one = False
    for pr in sorted(prs, key=lambda p: p["number"]):
        say = lambda msg, pr=pr: print(f"#{pr['number']} {pr['head']['ref']}: {msg}")
        if merged_one:
            say("main moved in this sweep; looked at again next sweep"); continue
        try:
            merged_one = automerge_one(repo, pr, tasks, dry_run, say)
        except Exception as e:  # one bad PR never blocks the others
            say(f"error, skipped: {e}")
    return 0


# ---------------------------------------------------------------- what a run should do (agents call these)

FIX_MARK = "<!-- backlog-fix"
REVIEW_MARK = "<!-- backlog-review"
MAX_FIX_ROUNDS = 2


def pr_snapshot(repo: str, pr: dict) -> dict:
    comments = gh(f"repos/{repo}/issues/{pr['number']}/comments?per_page=100", paginate=True) or []
    bodies = [c.get("body", "") for c in comments]
    return {"number": pr["number"], "head": pr["head"]["ref"], "sha": pr["head"]["sha"],
            "labels": {l["name"] for l in pr.get("labels", [])}, "draft": pr.get("draft", False),
            "fixes": sum(FIX_MARK in b for b in bodies), "reviews": sum(REVIEW_MARK in b for b in bodies),
            "ci": ci_green(repo, pr["head"]["sha"])}


def decide_executor(prs: list[dict], tasks: list[Task], state: dict, has_tool=None) -> tuple[str, str]:
    """The executor's next move, in priority order: finish open PRs before starting anything new."""
    has_tool = has_tool or tool_available
    live = []
    for p in sorted(prs, key=lambda p: p["number"]):
        if p["draft"] or not p["head"].startswith("claude/") or branch_kind(p["head"], tasks)[0] == "unknown":
            continue
        if p["labels"] & {"parked", "hold", "blocked-paths"}:
            continue
        live.append(p)
        broken = "needs-fix" in p["labels"] or p["ci"] == "red"
        if broken and p["fixes"] >= MAX_FIX_ROUNDS:
            return "PARK", f"#{p['number']} {p['head']}: {p['fixes']} fix rounds used; label it parked, say why, stop"
        if "needs-fix" in p["labels"]:
            return "FIX", f"#{p['number']} {p['head']}: answer the reviewer's questions (fix round {p['fixes'] + 1})"
        if "needs-rebase" in p["labels"]:
            return "REBASE", f"#{p['number']} {p['head']}: merge origin/main into it, resolve, run verify, push"
        if p["ci"] == "red":
            return "CI-RED", f"#{p['number']} {p['head']}: CI is red; fix it (fix round {p['fixes'] + 1})"
    if len(live) >= MAX_OPEN_AGENT_PRS:
        return "WIP-LIMIT", f"{len(live)} agent PRs are open and waiting for review or CI; start nothing new"
    t = next_task(tasks, state, has_tool)
    if not t:
        return "NOTHING", "no task is ready (all done, open, parked, waiting, blocked or needing a missing tool)"
    return "NEW", t.id


def decide_reviewer(prs: list[dict], tasks: list[Task]) -> tuple[str, str]:
    for p in sorted(prs, key=lambda p: p["number"]):
        kind, task = branch_kind(p["head"], tasks)
        if kind != "task" or p["draft"]:
            continue
        if p["labels"] & {"review:pass", "needs-fix", "needs-rebase", "parked", "hold", "blocked-paths"}:
            continue
        if p["ci"] != "green":
            continue
        return "REVIEW", (f"#{p['number']} {p['head']} {task.id} round={p['reviews'] + 1} fixes={p['fixes']} "
                          f"sha={p['sha']}")
    return "NOTHING", "no green agent PR waits for a review"


# ---------------------------------------------------------------- CLI

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("lint")
    s = sub.add_parser("show"); s.add_argument("id")
    s = sub.add_parser("next"); s.add_argument("--state")
    sub.add_parser("paused")
    s = sub.add_parser("check-paths"); s.add_argument("--branch", required=True)
    g = s.add_mutually_exclusive_group(required=True); g.add_argument("--base"); g.add_argument("--files")
    s = sub.add_parser("intake"); s.add_argument("--date", default=dt.date.today().isoformat())
    s = sub.add_parser("automerge"); s.add_argument("--dry-run", action="store_true")
    sub.add_parser("plan", help="executor: what to do in this run (one line: MOVE detail)")
    s = sub.add_parser("prod-changes", help="reviewer: changed files that are not tests or docs (for the vacuum check)")
    s.add_argument("--base", default="origin/main")
    sub.add_parser("review-queue", help="reviewer: which PR to review in this run")
    a = ap.parse_args(argv)

    if a.cmd == "paused":
        if (ROOT / PAUSE).exists():
            print("PAUSED: docs/backlog/PAUSE exists on main. Stop now, do nothing else.")
            return 0
        return 1
    if a.cmd == "automerge":
        return automerge(a.dry_run)
    if a.cmd == "intake":
        acc, rej = intake(a.date)
        print("\n".join([f"accepted: {x}" for x in acc] + [f"left for Aika: {x}" for x in rej]) or "nothing")
        return 0

    if a.cmd == "prod-changes":
        for c in changes_from_git(a.base):
            if c.status != "removed" and is_production(c.path):
                print(("added " if c.status in ("added", "renamed") else "changed ") + c.path)  # a renamed path is new on main
        return 0
    tasks = load_tasks()
    if a.cmd in ("plan", "review-queue"):
        if (ROOT / PAUSE).exists():
            print("PAUSED docs/backlog/PAUSE exists on main: stop now"); return 0
        repo = repo_slug()
        open_prs = gh(f"repos/{repo}/pulls?state=open&base=main&per_page=100", paginate=True) or []
        snaps = [pr_snapshot(repo, p) for p in open_prs if p["head"]["ref"].startswith("claude/")]
        if a.cmd == "plan":
            move, detail = decide_executor(snaps, tasks, fetch_state(repo, tasks))
        else:
            move, detail = decide_reviewer(snaps, tasks)
        print(f"{move} {detail}")
        if move == "NEW":
            t = next(t for t in tasks if t.id == detail)
            print(f"BRANCH: {t.branch}\n"); print(t.text())
        return 0
    if a.cmd == "lint":
        errs = lint(tasks)
        print("\n".join(errs) if errs else f"QUEUE.md ok: {len(tasks)} tasks")
        return 1 if errs else 0
    if a.cmd == "show":
        t = next((t for t in tasks if t.id == a.id), None)
        if not t:
            print(f"no task {a.id}"); return 1
        print(t.text()); return 0
    if a.cmd == "next":
        state = json.loads(Path(a.state).read_text()) if a.state else fetch_state(repo_slug(), tasks)
        opened = open_agent_prs(state)
        if len(opened) >= MAX_OPEN_AGENT_PRS:
            print(f"WIP-LIMIT: {len(opened)} agent PRs are open ({', '.join(p['head'] for p in opened)}). "
                  "Work on them, start nothing new.")
            return 3
        t = next_task(tasks, state)
        if not t:
            print("NOTHING-READY: no task is ready (all done, open, parked, waiting or blocked).")
            return 2
        print(f"BRANCH: {t.branch}\n"); print(t.text()); return 0
    if a.cmd == "check-paths":
        changes = (changes_from_api(json.loads(Path(a.files).read_text())) if a.files
                   else changes_from_git(a.base))
        probs = check_paths(a.branch, changes, tasks)
        print("\n".join(probs) if probs else "paths ok")
        return 1 if probs else 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
