#!/usr/bin/env python3
import os
import subprocess
import sys
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/build.yml"


def job_run(job):
    return "\n".join(step.get("run", "") for step in job["steps"])


def artifact_names(job):
    return [
        step.get("with", {}).get("name", "")
        for step in job["steps"]
        if step.get("uses", "").startswith("actions/upload-artifact@")
    ]


def expect(condition, message):
    if not condition:
        print(f"FAIL {message}")
        return 1
    print(f"ok   {message}")
    return 0


def final_gate_status(script, changes, code_changed, results):
    environment = os.environ | {
        "CHANGES": changes,
        "CODE_CHANGED": code_changed,
        "RESULTS": results,
    }
    return subprocess.run(
        ["bash", "-c", script],
        env=environment,
        check=False,
        capture_output=True,
    ).returncode


def main():
    workflow = yaml.safe_load(WORKFLOW.read_text())
    jobs = workflow["jobs"]
    failures = 0

    failures += expect(":core:model:test" in job_run(jobs["unit-core"]), "core model tests run")
    failures += expect("python3 tools/ci/test-build-workflow.py" in job_run(jobs["static"]), "static checks test workflow")

    expected_matrix = {"fail-fast": False, "matrix": {"flavor": ["Demo", "Prod"]}}
    for name, task in (("unit-app", ":app:test${{ matrix.flavor }}DebugUnitTest"), ("assemble", "assemble${{ matrix.flavor }}Debug")):
        job = jobs[name]
        failures += expect(job["strategy"] == expected_matrix, f"{name} has both flavors")
        failures += expect(task in job_run(job), f"{name} uses flavor task")
        failures += expect(
            artifact_names(job) == [f"reports-{name}-${{{{ matrix.flavor }}}}"],
            f"{name} names artifacts by flavor",
        )

    assemble_run = job_run(jobs["assemble"])
    failures += expect("assemble${{ matrix.flavor }}Release" in assemble_run, "assemble builds release APKs")
    failures += expect(":app:dependencyGuard" not in assemble_run, "assemble does not repeat dependency guard")

    release_config = jobs["release-config"]
    release_run = job_run(release_config)
    failures += expect("tools/ci/test-release-config.sh" in release_run, "release configuration runs checks")
    failures += expect(":app:dependencyGuard -PwarningsAsErrors=true" in release_run, "release configuration verifies dependency guard")
    failures += expect("org.gradle.daemon=true" in release_run, "release configuration enables daemon")
    failures += expect("./gradlew --stop" in release_run, "release configuration stops daemon")
    stop_step = next(step for step in release_config["steps"] if step["name"] == "Stop Gradle daemon")
    failures += expect(release_config["needs"] == "changes", "release configuration runs in parallel")
    failures += expect(stop_step["if"] == "always()", "release configuration always stops daemon")
    failures += expect(WORKFLOW.read_text().count("org.gradle.daemon=true") == 1, "daemon override is release configuration only")

    final_gate = jobs["build"]
    failures += expect(final_gate["if"] == "always()", "final gate always runs")
    failures += expect(
        set(final_gate["needs"]) == set(jobs) - {"policy", "build"},
        "final gate requires every build shard",
    )
    final_script = job_run(final_gate)
    failures += expect(final_gate_status(final_script, "success", "true", "success success success") == 0, "final gate accepts passing code checks")
    for result in ("failure", "cancelled", "skipped"):
        failures += expect(
            final_gate_status(final_script, "success", "true", f"success {result} success") != 0,
            f"final gate rejects {result} code shard",
        )
    failures += expect(final_gate_status(final_script, "failure", "", "") != 0, "final gate rejects failed detector")
    failures += expect(final_gate_status(final_script, "success", "false", "skipped skipped success") == 0, "final gate accepts docs-only skips")
    failures += expect(final_gate_status(final_script, "skipped", "", "") == 0, "final gate accepts edited event")

    return failures


if __name__ == "__main__":
    sys.exit(main())
