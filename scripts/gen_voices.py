"""
Generate the trainers' voice clips with Piper TTS (offline neural voices).

Usage:  python scripts/gen_voices.py <piper_dir_with_piper.exe_and_voices>

Writes app/src/main/assets/voice/<trainer>/<key>.ogg. Voices are public domain / CC0:
  male   -> en_US-joe-medium   (CC0, OHF-Voice datasets)
  female -> en_US-kristin-medium (public domain, LibriVox)
"""
import json, os, re, subprocess, sys, tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PIPER_DIR = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser("~/android-dev/voice")
VOICES = {"male": "en_US-joe-medium", "female": "en_US-kristin-medium"}
NAMES = {"male": "Alex", "female": "Maya"}


def phrases(trainer):
    lib = open(os.path.join(ROOT, "app/src/main/java/com/ownfit/app/data/ExerciseLibrary.kt"), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'(?:ex|breath)\(\s*"([a-z0-9_]+)",\s*"([^"]+)"', lib):
        out["ex_" + m.group(1)] = m.group(2) + "."
    for n in range(1, 61):
        out[f"reps_{n}"] = f"{n} reps."
    for n in sorted(set(list(range(5, 61, 5)) + list(range(60, 301, 15)))):
        out[f"sec_{n}"] = f"{n} seconds." if n < 60 or n % 60 else (f"{n // 60} minute." if n == 60 else f"{n // 60} minutes.")
    out.update({
        "get_ready": "Get ready.",
        "first_up": "First up,",
        "next": "Next up,",
        "rest": "Rest.",
        "first_side": "First side.",
        "other_side": "Switch sides.",
        "each_side": "Each side.",
        "go": "Go!",
        "three": "Three.", "two": "Two.", "one": "One.",
        "halfway": "Halfway there. Keep going!",
        "keep_going": "Keep it up, you're doing great!",
        "almost": "Almost there!",
        "last_set": "Last set!",
        "done": "Workout complete. Great job!",
        "breathe_in": "Breathe in.",
        "hold": "Hold.",
        "breathe_out": "Breathe out.",
        "intro": f"Hi, I'm {NAMES[trainer]}, your trainer. Let's get to work!",
    })
    return out


def main():
    piper = os.path.join(PIPER_DIR, "piper", "piper.exe")
    for trainer, voice in VOICES.items():
        dest = os.path.join(ROOT, "app/src/main/assets/voice", trainer)
        os.makedirs(dest, exist_ok=True)
        items = phrases(trainer)
        with tempfile.TemporaryDirectory() as tmp:
            lines = [json.dumps({"text": t, "output_file": os.path.join(tmp, k + ".wav")}) for k, t in items.items()]
            subprocess.run(
                [piper, "--model", os.path.join(PIPER_DIR, voice + ".onnx"), "--json-input",
                 "--length_scale", "0.92", "--sentence_silence", "0.05"],
                input="\n".join(lines).encode(), check=True, capture_output=True,
            )
            for k in items:
                subprocess.run(
                    ["ffmpeg", "-y", "-v", "error", "-i", os.path.join(tmp, k + ".wav"),
                     "-af", "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
                            "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
                            "loudnorm=I=-16:TP=-1.5",
                     "-ac", "1", "-ar", "22050", "-c:a", "libvorbis", "-q:a", "2",
                     os.path.join(dest, k + ".ogg")],
                    check=True,
                )
        size = sum(os.path.getsize(os.path.join(dest, f)) for f in os.listdir(dest))
        print(f"{trainer}: {len(items)} clips, {size / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
