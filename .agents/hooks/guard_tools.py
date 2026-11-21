#!/usr/bin/env python3
"""PreToolUse safety guard for Antigravity.

Enforces:
1. Hard-denies any `run_command` invocation that calls `git`.
2. Forces explicit user confirmation (`force_ask`) for every other `run_command`.
3. Hard-denies any file/path tool targeting a path outside the workspace directory.
4. Forces explicit user confirmation (`force_ask`) for any file-modifying tool.
"""

import json
import os
import re
import sys
from pathlib import Path

FILE_MUTATION_TOOLS = {
    "write_file",
    "edit_file",
    "write_to_file",
    "replace_file_content",
    "multi_replace_file_content",
    "delete_file",
    "create_file",
    "patch_file",
}

PATH_ARG_KEYS = {
    "Path",
    "path",
    "TargetFile",
    "AbsolutePath",
    "DirectoryPath",
    "SearchPath",
    "SearchDirectory",
    "Cwd",
    "cwd",
    "ScriptPath",
    "Destination",
    "Source",
}


def emit(decision: str, reason: str | None = None) -> None:
    response: dict[str, str] = {"decision": decision}
    if reason:
        response["reason"] = reason
    sys.stdout.write(json.dumps(response))
    sys.stdout.flush()


def get_workspace_roots(payload: dict) -> list[Path]:
    roots: list[Path] = []
    for raw in payload.get("workspacePaths") or []:
        if isinstance(raw, str) and raw.strip():
            roots.append(Path(raw).expanduser().resolve())
    if not roots:
        cwd = payload.get("cwd")
        if isinstance(cwd, str) and cwd.strip():
            roots.append(Path(cwd).expanduser().resolve())
    if not roots:
        # <workspace>/.agents/hooks/guard_tools.py -> parents[2] is <workspace>
        roots.append(Path(__file__).resolve().parents[2])
    return roots


def contains_git_command(command_line: str) -> bool:
    tokens = re.split(r"[\s;&|()`\n\r]+", command_line)
    for token in tokens:
        cleaned = token.strip("\"'")
        if not cleaned:
            continue
        if cleaned == "git" or cleaned.endswith("/git"):
            return True
    return False


def is_within_workspace(raw_path: str, roots: list[Path]) -> bool:
    expanded = os.path.expanduser(raw_path)
    candidate = Path(expanded)
    if not candidate.is_absolute():
        candidate = roots[0] / candidate
    resolved = candidate.resolve()
    return any(resolved == root or root in resolved.parents for root in roots)


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except Exception:
        emit(
            "ask",
            "Safety guard could not parse hook payload; requesting confirmation.",
        )
        return

    tool_call = payload.get("toolCall") or {}
    tool_name = str(tool_call.get("name") or "").lower()
    args = tool_call.get("args") or {}
    roots = get_workspace_roots(payload)

    if tool_name == "run_command":
        cmd = str(args.get("CommandLine") or "")
        if contains_git_command(cmd):
            emit(
                "deny",
                "Blocked by project safety rules: git commands are strictly forbidden.",
            )
            return
        emit(
            "force_ask",
            "Project safety rules require explicit user permission before running any command.",
        )
        return

    if isinstance(args, dict):
        for key, value in args.items():
            if key in PATH_ARG_KEYS and isinstance(value, str) and value.strip():
                if not is_within_workspace(value, roots):
                    emit(
                        "deny",
                        f"Blocked by project safety rules: path '{value}' is outside the project directory.",
                    )
                    return

    if tool_name in FILE_MUTATION_TOOLS:
        emit(
            "force_ask",
            "Project safety rules require explicit user permission before changing any file.",
        )
        return

    emit("allow")


if __name__ == "__main__":
    main()
