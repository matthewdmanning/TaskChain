"""Check TaskChain ownership imports and cycles without Android or third-party tools."""

import re
import sys
import tempfile
from pathlib import Path


def owner(package):
    """Use this function to map a TaskChain package to its Core area, feature or root wiring; dependencies: none."""
    parts = package.split(".")
    if parts[:2] != ["com", "taskchain"]:
        return None
    if len(parts) >= 4 and parts[2] in ("core", "features"):
        return ".".join(parts[2:4])
    return "root"


def forbidden(source, target):
    """Use this function to reject imports from reusable owners into root or other features; dependencies: none."""
    if source == "root" or source == target:
        return False
    return target == "root" or (target.startswith("features.") and source != target)


def cycle(graph):
    """Use this function to find one directed cycle in the ownership adjacency map; dependencies: none."""
    visited, active, path = set(), set(), []

    def visit(node):
        """Use this function to visit one owner during cycle detection; dependencies: graph, visited, active and path."""
        if node in active:
            return path[path.index(node):] + [node]
        if node in visited:
            return None
        active.add(node)
        path.append(node)
        for target in sorted(graph.get(node, ())):
            found = visit(target)
            if found:
                return found
        path.pop()
        active.remove(node)
        visited.add(node)
        return None

    for node in sorted(graph):
        found = visit(node)
        if found:
            return found
    return None


def check(root):
    """Use this function to inspect main Kotlin packages/imports under a repository root; dependencies: owner, forbidden and cycle."""
    source_root = root / "app/src/main/java"
    graph, errors = {}, []
    files = sorted(source_root.rglob("*.kt"))
    if not files:
        errors.append(f"No Kotlin sources found under {source_root}")
    for file in files:
        text = file.read_text(encoding="utf-8-sig")
        package_match = re.search(r"^package\s+([\w.]+)", text, re.M)
        if not package_match:
            errors.append(f"{file.relative_to(root)}: missing package")
            continue
        package = package_match[1]
        source = owner(package)
        if source is None:
            errors.append(f"{file.relative_to(root)}: unexpected package {package}")
            continue
        if file.parent.relative_to(source_root).as_posix() != package.replace(".", "/"):
            errors.append(f"{file.relative_to(root)}: path does not match package {package}")
        if source == "root" and package not in ("com.taskchain", "com.taskchain.reminder"):
            errors.append(f"{file.relative_to(root)}: legacy package {package}")
        if package == "com.taskchain.reminder" and file.name != "LegacyReceivers.kt":
            errors.append(f"{file.relative_to(root)}: only compatibility receiver adapters belong in the legacy package")
        graph.setdefault(source, set())
        # Inspect imports and explicit qualified references; omit comments and string literals first.
        code = re.sub(r'/\*.*?\*/|//[^\n]*|""".*?"""|"(?:\\.|[^"\\])*"', "", text, flags=re.S)
        for reference in set(re.findall(r"\bcom\.taskchain\.[\w.*]+", code)):
            if reference == package:
                continue
            if reference in ("com.taskchain.R", "com.taskchain.BuildConfig") or reference.startswith(("com.taskchain.R.", "com.taskchain.BuildConfig.")):
                continue  # Generated :app resources are shared across all ownership areas.
            if "*" in reference:
                errors.append(f"{file.relative_to(root)}: wildcard project import {reference}")
                continue
            target = owner(reference)
            if target is None or target == source:
                continue
            graph[source].add(target)
            if forbidden(source, target):
                errors.append(f"{file.relative_to(root)}: forbidden {source} -> {target}: {reference}")
    found = cycle(graph)
    if found:
        errors.append("Ownership cycle: " + " -> ".join(found))
    return files, graph, errors


def self_test():
    """Use this function to verify import direction and cycle detection rules; dependencies: owner, forbidden and cycle."""
    assert owner("com.taskchain.features.execution.ui") == "features.execution"
    assert owner("com.taskchain.core.data") == "core.data"
    assert forbidden("core.data", "features.execution")
    assert forbidden("features.execution", "root")
    assert forbidden("features.settings", "features.authoring")
    assert not forbidden("root", "features.execution")
    assert not forbidden("features.execution", "core.data")
    assert not forbidden("features.execution", "features.execution")
    assert cycle({"a": {"b"}, "b": set()}) is None
    assert cycle({"a": {"b"}, "b": {"a"}}) == ["a", "b", "a"]
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        file = root / "app/src/main/java/com/taskchain/core/data/Example.kt"
        file.parent.mkdir(parents=True)
        file.write_text(
            'package com.taskchain.core.data\nimport com.taskchain.R\n'
            '// com.taskchain.AppContainer\nval example = "com.taskchain.features.execution"\n',
            encoding="utf-8",
        )
        assert not check(root)[2], "Comments, strings and generated resources are not source dependencies"
        file.write_text(file.read_text() + "import com.taskchain.AppContainer as Container\n", encoding="utf-8")
        assert any("forbidden core.data -> root" in error for error in check(root)[2])


if __name__ == "__main__":
    self_test()
    files, graph, errors = check(Path(__file__).resolve().parents[1])
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    print(f"Feature boundaries passed: {len(files)} Kotlin files, {len(graph)} owners, no cycles.")
