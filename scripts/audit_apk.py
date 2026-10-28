"""Inspect a built APK using SDK tools and local ZIP/DEX inspection. No uploads."""
import argparse
import base64
import hashlib
import json
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

def run(command):
    result = subprocess.run([str(x) for x in command], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode:
        raise RuntimeError(f"SDK verification failed ({result.returncode}): {result.stdout}\n{result.stderr}")
    return result.stdout + result.stderr

def dex_strings(data):
    count, offset = struct.unpack_from("<II", data, 56)
    for index in range(count):
        cursor, = struct.unpack_from("<I", data, offset + index * 4)
        while data[cursor] & 128:
            cursor += 1
        cursor += 1
        end = data.index(b"\0", cursor)
        yield data[cursor:end].decode("utf-8", errors="replace")

def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--sdk", required=True, type=Path)
    parser.add_argument("--java-home", required=True, type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    output = root / "security/evidence"
    output.mkdir(parents=True, exist_ok=True)
    tools = args.sdk / "build-tools/35.0.0"
    manifest = run([tools / "aapt2.exe", "dump", "xmltree", args.apk, "--file", "AndroidManifest.xml"])
    permissions_text = run([tools / "aapt2.exe", "dump", "permissions", args.apk])
    signature = run([args.java_home / "bin/java.exe", "-jar", tools / "lib/apksigner.jar", "verify", "--verbose", "--print-certs", args.apk])
    alignment = run([tools / "zipalign.exe", "-c", "-P", "16", "-v", "4", args.apk])
    for filename, content in [("release-manifest.txt", manifest), ("release-permissions.txt", permissions_text), ("release-signature.txt", signature), ("release-alignment.txt", alignment)]:
        (output / filename).write_text(content, encoding="utf-8")
    permissions = sorted(set(re.findall(r"uses-permission: name='([^']+)'", permissions_text)))
    expected = {
        "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.FOREGROUND_SERVICE", "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
        "android.permission.WAKE_LOCK", "android.permission.MODIFY_AUDIO_SETTINGS",
        "com.coelho.music.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    }
    assert set(permissions) == expected, f"Unexpected/missing permissions: {set(permissions) ^ expected}"
    assert not re.search(r"android:debuggable\([^)]*\)=(?:true|0xffffffff)", manifest), "Debuggable release"
    assert "PreviewActivity" not in manifest, "Preview tooling present in release"
    assert re.search(r"android:usesCleartextTraffic\([^)]*\)=false", manifest), "Cleartext must be disabled"
    components = []
    lines = manifest.splitlines()
    for index, line in enumerate(lines):
        match = re.match(r"\s+E: (activity|service|receiver|provider) ", line)
        if match:
            attributes = []
            for following in lines[index+1:]:
                if "E: " in following:
                    break
                attributes.append(following)
            block = "\n".join(attributes)
            name_match = re.search(r'android:name\([^)]*\)="([^"]+)"', block)
            component = dict(kind=match[1], name=name_match[1] if name_match else "", exported="android:exported" in block and bool(re.search(r"android:exported\([^)]*\)=true", block)))
            components.append(component)
            if component["name"].endswith("PlaybackService"):
                assert re.search(r"android:exported\([^)]*\)=false", block), "Playback service must be private"
            if component["exported"] and component["name"] != "com.coelho.music.MainActivity":
                assert any(permission in block for permission in ["android.permission.DUMP", "android.permission.BLUETOOTH_PRIVILEGED"]), f"Unprotected exported component: {component}"
    assert any(c["name"] == "com.coelho.music.player.service.PlaybackService" and not c["exported"] for c in components)
    assert "Android Debug" not in signature, "Debug signing certificate"
    assert "Verified using v2 scheme (APK Signature Scheme v2): true" in signature
    assert "Verified using v3 scheme (APK Signature Scheme v3): true" in signature
    pem = (root / "security/release-certificate.pem").read_text()
    der = base64.b64decode("".join(line for line in pem.splitlines() if not line.startswith("---")))
    certificate_digest = hashlib.sha256(der).hexdigest()
    assert f"Signer #1 certificate SHA-256 digest: {certificate_digest}" in signature, "APK does not match project's public certificate"
    native_origins = {}
    artifact_list = root / ".tools/artifact-files.json"
    if artifact_list.exists():
        for artifact in json.loads(artifact_list.read_text()):
            path = Path(artifact["file"])
            if path.suffix == ".aar":
                with zipfile.ZipFile(path) as archive:
                    for entry in archive.namelist():
                        if entry.endswith(".so"):
                            native_origins[hashlib.sha256(archive.read(entry)).hexdigest()] = dict(coordinate=artifact["coordinate"], entry=entry)
    with zipfile.ZipFile(args.apk) as archive:
        assert archive.testzip() is None, "Corrupt ZIP entry"
        names = archive.namelist()
        forbidden_files = [n for n in names if n.lower().endswith((".apk", ".exe", ".dll", ".bat", ".ps1", ".sh", ".jar", ".jks", ".p12", ".keystore"))]
        assert not forbidden_files, f"Unexpected embedded executables or keys: {forbidden_files}"
        dex = [n for n in names if n.endswith(".dex")]
        assert all(re.fullmatch(r"classes(?:[0-9]+)?\.dex", n) for n in dex), "Unexpected secondary code asset"
        strings = {s for n in dex for s in dex_strings(archive.read(n))}
        assert "Lcom/coelho/music/MainActivity;" in strings, "Expected readable application class missing"
        native = []
        for name in names:
            if name.endswith(".so"):
                binary = archive.read(name)
                digest = hashlib.sha256(binary).hexdigest()
                origin = native_origins.get(digest)
                assert origin, f"Native binary not matched to a resolved AAR: {name}"
                alignments = []
                if binary[:5] == b"\x7fELF\x02":
                    phoff, = struct.unpack_from("<Q", binary, 32)
                    entry_size, count = struct.unpack_from("<HH", binary, 54)
                    for n in range(count):
                        offset = phoff + n * entry_size
                        kind, = struct.unpack_from("<I", binary, offset)
                        if kind == 1:
                            alignment, = struct.unpack_from("<Q", binary, offset + 48)
                            alignments.append(alignment)
                    assert alignments and min(alignments) >= 16384, f"ELF 64-bit not aligned for 16 KB: {name}"
                native.append(dict(entry=name, sha256=digest, origin=origin, elf64_load_alignments=alignments))
        code_references = sorted(s for s in strings if s in {
            "Ldalvik/system/DexClassLoader;", "Ldalvik/system/InMemoryDexClassLoader;",
            "Ljava/lang/ProcessBuilder;", "Landroid/app/DownloadManager;", "Landroid/webkit/WebView;",
            "Ljava/io/ObjectInputStream;", "Ljava/net/Socket;", "Ljava/net/HttpURLConnection;",
        })
    source = "\n".join(p.read_text(encoding="utf-8") for p in (root / "app/src/main/java").rglob("*.kt"))
    suspicious = re.findall(r"DexClassLoader|InMemoryDexClassLoader|ProcessBuilder|Runtime\.getRuntime\(\)\.exec|DownloadManager|ObjectInputStream|System\.load(?:Library)?\s*\(|WebView|https?://", source)
    assert not suspicious, f"Review dangerous application-source references: {suspicious}"
    report = dict(apk=args.apk.name, sha256=hashlib.sha256(args.apk.read_bytes()).hexdigest(), size=args.apk.stat().st_size, certificate_sha256=certificate_digest,
                  permissions=permissions, components=components, dex_files=dex, native_binaries=native,
                  library_dex_references_requiring_context=code_references, app_source_pattern_matches=suspicious,
                  note="Static checks and valid signatures do not establish absence of malware. Framework references need contextual review.")
    (output / "apk-audit.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))

if __name__ == "__main__":
    main()
