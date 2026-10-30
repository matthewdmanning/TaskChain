"""Check TaskChain ownership imports, resource ownership and cycles in main and test sources without Android or third-party tools."""

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


# Feature-owned value resource files; every other values file is shared Core configuration.
RESOURCE_OWNERS = {
    "authoring_strings.xml": "features.authoring",
    "subtask_authoring.xml": "features.authoring",
    "keep_screen_on_strings.xml": "features.keepscreenon",
    "notification_strings.xml": "features.notifications",
    "progress_strings.xml": "features.progress",
    "settings_strings.xml": "features.settings",
    "run_strings.xml": "features.running",
    "runner_gesture_strings.xml": "features.running",
    "runner_micro_animation_strings.xml": "features.running",
    "runner_next_up_strings.xml": "features.running",
    "subtask_runner.xml": "features.running",
}
SOURCE_SETS = ("main", "test", "androidTest")
# Library theme values and the Material type scale that features must reach through core/designsystem tokens.
THEME_BYPASS = re.compile(r"\b(?:CyberTheme|CyberPrimitives)\.|\bMaterialTheme\s*\.\s*typography\b")
STRIPPED = re.compile(r'/\*.*?\*/|//[^\n]*|""".*?"""|"(?:\\.|[^"\\])*"' + r"|'(?:\\.|[^'\\])+'", re.S)


def resource_owners(root):
    """Use this function to map each feature-owned value resource to its owner; dependencies: RESOURCE_OWNERS."""
    owners = {}
    for file in sorted((root / "app/src/main/res").glob("values*/*.xml")):
        resource_owner = RESOURCE_OWNERS.get(file.name)
        if resource_owner is None:
            continue
        for kind, name in re.findall(r'<(string|plurals|integer|dimen|color|bool|string-array|integer-array)\b[^>]*\bname="([^"]+)"', file.read_text(encoding="utf-8-sig")):
            kind = "array" if kind.endswith("-array") else kind
            owners[(kind, name.replace(".", "_"))] = resource_owner
    return owners


def check(root):
    """Use this function to inspect Kotlin packages, imports and resource use in all source sets under a repository root; dependencies: owner, forbidden, cycle and resource_owners."""
    graph, errors, files = {}, [], []
    resources = resource_owners(root)
    for source_set in SOURCE_SETS:
        source_root = root / "app/src" / source_set / "java"
        set_files = sorted(source_root.rglob("*.kt"))
        if source_set == "main" and not set_files:
            errors.append(f"No Kotlin sources found under {source_root}")
        files += set_files
        for file in set_files:
            check_file(root, source_root, source_set, file, graph, resources, errors)
    found = cycle(graph)
    if found:
        errors.append("Ownership cycle: " + " -> ".join(found))
    return files, graph, errors


def check_file(root, source_root, source_set, file, graph, resources, errors):
    """Use this function to check one Kotlin file; main files build the owner graph, root-package tests are system tests that may cross features, and feature files may not read library theme values or Material typography; dependencies: owner, forbidden and THEME_BYPASS."""
    text = file.read_text(encoding="utf-8-sig")
    package_match = re.search(r"^package\s+([\w.]+)", text, re.M)
    if not package_match:
        errors.append(f"{file.relative_to(root)}: missing package")
        return
    package = package_match[1]
    source = owner(package)
    if source is None:
        errors.append(f"{file.relative_to(root)}: unexpected package {package}")
        return
    if file.parent.relative_to(source_root).as_posix() != package.replace(".", "/"):
        errors.append(f"{file.relative_to(root)}: path does not match package {package}")
    if source_set == "main":
        if source == "root" and package not in ("com.taskchain", "com.taskchain.reminder"):
            errors.append(f"{file.relative_to(root)}: legacy package {package}")
        if package == "com.taskchain.reminder" and file.name != "LegacyReceivers.kt":
            errors.append(f"{file.relative_to(root)}: only compatibility receiver adapters belong in the legacy package")
        graph.setdefault(source, set())
    # Inspect imports and explicit qualified references; omit comments, string and char literals first.
    code = STRIPPED.sub("", text)
    for reference in set(re.findall(r"\bcom\.taskchain\.[\w.*]+", code)):
        if reference == package:
            continue
        if reference in ("com.taskchain.R", "com.taskchain.BuildConfig") or reference.startswith(("com.taskchain.R.", "com.taskchain.BuildConfig.")):
            continue  # Generated :app classes; resource ownership is checked below.
        if "*" in reference:
            errors.append(f"{file.relative_to(root)}: wildcard project import {reference}")
            continue
        target = owner(reference)
        if target is None or target == source:
            continue
        if source_set == "main":
            graph[source].add(target)
        if forbidden(source, target):
            errors.append(f"{file.relative_to(root)}: forbidden {source} -> {target}: {reference}")
    if source.startswith("features."):
        for bypass in sorted(set(match.group(0) for match in THEME_BYPASS.finditer(code))):
            errors.append(f"{file.relative_to(root)}: theme bypass {bypass} in {source}; use TaskChainDesignSystem tokens")
    for kind, name in set(re.findall(r"(?<![\w.])R\.(\w+)\.(\w+)", code)):
        target = resources.get((kind, name))
        if target and forbidden(source, target):
            errors.append(f"{file.relative_to(root)}: forbidden {source} -> {target} resource R.{kind}.{name}")


def self_test():
    """Use this function to verify import direction, cycle detection and feature theme-bypass rules; dependencies: owner, forbidden, cycle and check."""
    assert owner("com.taskchain.features.running.ui") == "features.running"
    assert owner("com.taskchain.core.data") == "core.data"
    assert forbidden("core.data", "features.running")
    assert forbidden("features.running", "root")
    assert forbidden("features.settings", "features.authoring")
    assert forbidden("features.running", "features.notifications")
    assert forbidden("features.notifications", "features.running")
    assert forbidden("features.reminders", "features.notifications")
    assert forbidden("features.notifications", "features.reminders")
    assert not forbidden("root", "features.running")
    assert not forbidden("features.running", "core.data")
    assert not forbidden("features.running", "features.running")
    assert cycle({"a": {"b"}, "b": set()}) is None
    assert cycle({"a": {"b"}, "b": {"a"}}) == ["a", "b", "a"]
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        file = root / "app/src/main/java/com/taskchain/core/data/Example.kt"
        file.parent.mkdir(parents=True)
        file.write_text(
            'package com.taskchain.core.data\nimport com.taskchain.R\n'
            '// com.taskchain.AppContainer\nval example = "com.taskchain.features.running"\n',
            encoding="utf-8",
        )
        assert not check(root)[2], "Comments, strings and generated resources are not source dependencies"
        file.write_text(file.read_text() + "import com.taskchain.AppContainer as Container\n", encoding="utf-8")
        assert any("forbidden core.data -> root" in error for error in check(root)[2])
        file.write_text("package com.taskchain.core.data\nval q = '\"'\nval r = com.taskchain.features.running.Example\n", encoding="utf-8")
        assert any("forbidden core.data -> features.running" in error for error in check(root)[2]), "Char literals hide no references"
        file.write_text("package com.taskchain.core.data\n", encoding="utf-8")
        values = root / "app/src/main/res/values/run_strings.xml"
        values.parent.mkdir(parents=True)
        values.write_text('<resources><string name="run_only">Run</string></resources>', encoding="utf-8")
        screen = root / "app/src/main/java/com/taskchain/features/scheduling/Screen.kt"
        screen.parent.mkdir(parents=True)
        screen.write_text("package com.taskchain.features.scheduling\nval id = R.string.run_only\n", encoding="utf-8")
        assert any("resource R.string.run_only" in error for error in check(root)[2]), "Feature resources stay with their owner"
        screen.unlink()
        test = root / "app/src/test/java/com/taskchain/features/scheduling/ScreenTest.kt"
        test.parent.mkdir(parents=True)
        test.write_text("package com.taskchain.features.scheduling\nimport com.taskchain.features.running.Engine\n", encoding="utf-8")
        assert any("forbidden features.scheduling -> features.running" in error for error in check(root)[2]), "Feature tests stay inside their owner"
        test.unlink()
        system = root / "app/src/test/java/com/taskchain/JourneyTest.kt"
        system.write_text("package com.taskchain\nimport com.taskchain.features.running.Engine\n", encoding="utf-8")
        assert not check(root)[2], "Root system tests may cross features"
        screen.write_text(
            "package com.taskchain.features.scheduling\n"
            "// CyberTheme.colors.primary\nval label = \"MaterialTheme.typography.titleLarge\"\n"
            "val gap = TaskChainDesignSystem.Space.small\n",
            encoding="utf-8",
        )
        assert not check(root)[2], "Comments, strings and semantic tokens are not theme bypasses"
        for bypass in ("CyberTheme.colors.primary", "CyberPrimitives.Spacing.dp8", "MaterialTheme.typography.titleLarge"):
            screen.write_text(f"package com.taskchain.features.scheduling\nval value = {bypass}\n", encoding="utf-8")
            assert any("theme bypass" in error for error in check(root)[2]), f"Features may not read {bypass}"
        core_theme = root / "app/src/main/java/com/taskchain/core/designsystem/Theme.kt"
        core_theme.parent.mkdir(parents=True)
        core_theme.write_text("package com.taskchain.core.designsystem\nval value = CyberTheme.colors.primary\n", encoding="utf-8")
        screen.unlink()
        assert not check(root)[2], "The Core theme boundary may read library tokens"


if __name__ == "__main__":
    self_test()
    files, graph, errors = check(Path(__file__).resolve().parents[1])
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    print(f"Feature boundaries passed: {len(files)} Kotlin files (main and test), {len(graph)} owners, no cycles.")
